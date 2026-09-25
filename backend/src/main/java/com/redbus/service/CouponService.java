package com.redbus.service;

import com.redbus.dto.CouponDto;
import com.redbus.dto.CreateCouponRequest;
import com.redbus.dto.CouponValidationResponse;
import com.redbus.entity.Coupon;
import com.redbus.entity.User;
import com.redbus.exception.BadRequestException;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.repository.CouponRepository;
import com.redbus.repository.UserRepository;
import com.redbus.repository.OperatorRepository;
import com.redbus.entity.Operator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final OperatorRepository operatorRepository;

    @Transactional
    public CouponDto createCoupon(CreateCouponRequest request, Long operatorUserId) {
        String cleanCode = request.getCode().trim().toUpperCase();
        if (couponRepository.findByCodeIgnoreCase(cleanCode).isPresent()) {
            throw new BadRequestException("Coupon code '" + cleanCode + "' already exists.");
        }

        String operatorName = null;
        Long operatorId = null;
        if (operatorUserId != null) {
            Operator operator = operatorRepository.findByUserId(operatorUserId).orElse(null);
            if (operator != null) {
                operatorId = operator.getId();
                operatorName = operator.getCompanyName();
            } else {
                User user = userRepository.findById(operatorUserId).orElse(null);
                if (user != null) {
                    operatorId = user.getId();
                    operatorName = user.getName();
                }
            }
        }

        LocalDate validFrom = request.getValidFrom() != null ? request.getValidFrom() : LocalDate.now();
        LocalDate validTo = request.getValidTo() != null ? request.getValidTo() : LocalDate.now().plusMonths(3);

        if (validTo.isBefore(validFrom)) {
            throw new BadRequestException("Valid to date cannot be before valid from date.");
        }

        Coupon coupon = Coupon.builder()
                .code(cleanCode)
                .discountPercentage(request.getDiscountPercentage())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minBookingAmount(request.getMinBookingAmount() != null ? request.getMinBookingAmount() : BigDecimal.ZERO)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .validFrom(validFrom)
                .validTo(validTo)
                .usageLimit(request.getUsageLimit() != null ? request.getUsageLimit() : 1000)
                .timesUsed(0)
                .isActive(true)
                .build();

        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon '{}' created with {}% discount by operator '{}' (ID: {})",
                saved.getCode(), saved.getDiscountPercentage(), operatorName, operatorId);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CouponDto> getOperatorCoupons(Long operatorUserId) {
        if (operatorUserId == null) {
            return couponRepository.findAllActiveCoupons().stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }
        User user = userRepository.findById(operatorUserId).orElse(null);
        if (user != null && ("ROLE_ADMIN".equals(user.getRole()) || "ADMIN".equals(user.getRole()))) {
            return couponRepository.findAll().stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        Long opEntityId = operatorRepository.findByUserId(operatorUserId)
                .map(Operator::getId)
                .orElse(operatorUserId);

        List<Coupon> list = couponRepository.findByOperatorIdOrUserIdOrderByCreatedAtDesc(opEntityId, operatorUserId);
        if (list.isEmpty()) {
            list = couponRepository.findAllActiveCoupons();
        }

        return list.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CouponDto> getAvailableCoupons(Long operatorId) {
        List<Coupon> list = (operatorId != null)
                ? couponRepository.findAvailableCouponsForOperator(operatorId)
                : couponRepository.findAllActiveCoupons();
        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, Long operatorId, BigDecimal bookingAmount) {
        if (code == null || code.isBlank()) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .message("Please enter a coupon code.")
                    .build();
        }

        String cleanCode = code.trim().toUpperCase();
        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(cleanCode)
                .orElse(null);

        if (coupon == null) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .message("Invalid or expired coupon code: " + cleanCode)
                    .build();
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(coupon.getValidFrom()) || today.isAfter(coupon.getValidTo())) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .message("Coupon " + cleanCode + " has expired.")
                    .build();
        }

        if (coupon.getTimesUsed() >= coupon.getUsageLimit()) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .message("Coupon " + cleanCode + " has reached its maximum usage limit.")
                    .build();
        }

        // Check operator restriction
        if (coupon.getOperatorId() != null && operatorId != null && !coupon.getOperatorId().equals(operatorId)) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .message("Coupon " + cleanCode + " is valid only for buses operated by " + (coupon.getOperatorName() != null ? coupon.getOperatorName() : "its issuing operator") + ".")
                    .build();
        }

        BigDecimal minAmount = coupon.getMinBookingAmount() != null ? coupon.getMinBookingAmount() : BigDecimal.ZERO;
        if (bookingAmount.compareTo(minAmount) < 0) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .message("Minimum booking amount of ₹" + minAmount + " required to use coupon " + cleanCode + ".")
                    .build();
        }

        // Calculate discount percentage
        BigDecimal discount = bookingAmount.multiply(coupon.getDiscountPercentage())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }

        BigDecimal finalPayable = bookingAmount.subtract(discount).max(BigDecimal.ZERO);

        return CouponValidationResponse.builder()
                .valid(true)
                .code(cleanCode)
                .discountPercentage(coupon.getDiscountPercentage())
                .discountAmount(discount)
                .finalPayableAmount(finalPayable)
                .message("🎉 Coupon '" + cleanCode + "' applied! You saved ₹" + discount + " (" + coupon.getDiscountPercentage() + "% OFF).")
                .build();
    }

    @Transactional
    public void incrementCouponUsage(String code) {
        if (code != null && !code.isBlank()) {
            couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim().toUpperCase())
                    .ifPresent(c -> {
                        c.setTimesUsed(c.getTimesUsed() + 1);
                        couponRepository.save(c);
                    });
        }
    }

    @Transactional
    public void deleteCoupon(Long couponId, Long operatorUserId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + couponId));

        if (operatorUserId != null) {
            Long opEntityId = operatorRepository.findByUserId(operatorUserId)
                    .map(Operator::getId)
                    .orElse(operatorUserId);

            User user = userRepository.findById(operatorUserId).orElse(null);
            boolean isAdmin = user != null && ("ROLE_ADMIN".equals(user.getRole()) || "ADMIN".equals(user.getRole()));

            if (!isAdmin && coupon.getOperatorId() != null
                    && !coupon.getOperatorId().equals(operatorUserId)
                    && !coupon.getOperatorId().equals(opEntityId)) {
                throw new BadRequestException("You are not authorized to delete this coupon.");
            }
        }

        couponRepository.delete(coupon);
    }

    public CouponDto mapToDto(Coupon c) {
        return CouponDto.builder()
                .id(c.getId())
                .code(c.getCode())
                .discountPercentage(c.getDiscountPercentage())
                .maxDiscountAmount(c.getMaxDiscountAmount())
                .minBookingAmount(c.getMinBookingAmount())
                .operatorId(c.getOperatorId())
                .operatorName(c.getOperatorName())
                .validFrom(c.getValidFrom())
                .validTo(c.getValidTo())
                .usageLimit(c.getUsageLimit())
                .timesUsed(c.getTimesUsed())
                .isActive(c.getIsActive())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
