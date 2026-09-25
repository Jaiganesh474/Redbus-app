package com.redbus.controller;

import com.redbus.config.JwtUtil;
import com.redbus.dto.CouponDto;
import com.redbus.dto.CouponValidationResponse;
import com.redbus.dto.CreateCouponRequest;
import com.redbus.dto.ValidateCouponRequest;
import com.redbus.entity.User;
import com.redbus.service.AuthService;
import com.redbus.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/coupons", "/api/v1/coupons", "/api/operator/coupons", "/api/v1/operator/coupons"})
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping({"", "/operator"})
    public ResponseEntity<CouponDto> createCoupon(
            @Valid @RequestBody CreateCouponRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long operatorId = resolveUserId(authHeader);
        CouponDto created = couponService.createCoupon(request, operatorId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping({"", "/operator"})
    public ResponseEntity<List<CouponDto>> getOperatorCoupons(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long operatorId = resolveUserId(authHeader);
        return ResponseEntity.ok(couponService.getOperatorCoupons(operatorId));
    }

    @GetMapping("/available")
    public ResponseEntity<List<CouponDto>> getAvailableCoupons(
            @RequestParam(required = false) Long operatorId
    ) {
        return ResponseEntity.ok(couponService.getAvailableCoupons(operatorId));
    }

    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @Valid @RequestBody ValidateCouponRequest request
    ) {
        return ResponseEntity.ok(couponService.validateCoupon(
                request.getCode(),
                request.getOperatorId(),
                request.getBookingAmount()
        ));
    }

    @DeleteMapping({"/{id}", "/operator/{id}"})
    public ResponseEntity<Void> deleteCoupon(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long operatorId = resolveUserId(authHeader);
        couponService.deleteCoupon(id, operatorId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (!jwtUtil.isTokenExpired(token)) {
                    return jwtUtil.extractUserId(token);
                }
            } catch (Exception ignored) {
            }
        }
        try {
            User user = authService.getAuthenticatedUser();
            if (user != null) {
                return user.getId();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
