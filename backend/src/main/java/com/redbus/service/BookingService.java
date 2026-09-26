package com.redbus.service;

import com.redbus.dto.*;
import com.redbus.entity.*;
import com.redbus.exception.BadRequestException;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.exception.SeatLockException;
import com.redbus.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RouteRepository routeRepository;
    private final SeatRepository seatRepository;
    private final RouteSeatRepository routeSeatRepository;
    private final UserRepository userRepository;
    private final OperatorRepository operatorRepository;
    private final OperatorWalletTransactionRepository operatorWalletTransactionRepository;
    private final CouponService couponService;
    private final EmailService emailService;

    private static final String PNR_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public BookingResponseDto createBooking(CreateBookingRequest request, Long currentUserId) {
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + request.getRouteId()));

        User user;
        if (currentUserId != null) {
            user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));
        } else {
            // Find or create guest user with the provided email
            user = userRepository.findByEmail(request.getContactEmail().trim().toLowerCase())
                    .orElseGet(() -> {
                        User guest = User.builder()
                                .name(request.getPassengers().get(0).getName())
                                .email(request.getContactEmail().trim().toLowerCase())
                                .phone(request.getContactPhone().trim())
                                .passwordHash("$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW")
                                .role("ROLE_USER")
                                .build();
                        return userRepository.save(guest);
                    });
        }

        // Restriction: Bus Operators cannot book tickets on the platform
        if (user.getRole() != null && ("ROLE_OPERATOR".equalsIgnoreCase(user.getRole()) || "OPERATOR".equalsIgnoreCase(user.getRole()))) {
            throw new BadRequestException("Bus Operator accounts are restricted from booking passenger tickets. Please log in with a passenger account.");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Seat> validatedSeats = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Validate seats and calculate base price
        for (PassengerDto p : request.getPassengers()) {
            RouteSeat rs = routeSeatRepository.findByRouteIdAndSeatId(route.getId(), p.getSeatId())
                    .orElseThrow(() -> new BadRequestException("Seat " + p.getSeatNumber() + " does not belong to this route"));

            if ("BOOKED".equalsIgnoreCase(rs.getStatus())) {
                throw new SeatLockException("Seat " + p.getSeatNumber() + " is already booked.");
            }

            // If locked, check if locked by this user
            if ("LOCKED".equalsIgnoreCase(rs.getStatus()) && rs.getLockExpiry() != null && rs.getLockExpiry().isAfter(now)) {
                if (rs.getLockedByUserId() != null && !rs.getLockedByUserId().equals(user.getId())) {
                    throw new SeatLockException("Seat " + p.getSeatNumber() + " is locked by another session.");
                }
            }

            // Check female-only adjacent restriction
            if ("FEMALE".equalsIgnoreCase(rs.getGenderRestriction()) && !"FEMALE".equalsIgnoreCase(p.getGender())) {
                throw new BadRequestException("Seat " + p.getSeatNumber() + " is next to a female passenger. Only female travelers can book this seat.");
            }

            BigDecimal seatPrice = rs.getPriceOverride() != null ? rs.getPriceOverride() : route.getBasePrice();
            totalAmount = totalAmount.add(seatPrice);
            validatedSeats.add(rs.getSeat());
        }

        BigDecimal baseFareAmount = totalAmount;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedCouponCode = null;

        // Apply coupon if provided
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            CouponValidationResponse couponRes = couponService.validateCoupon(
                    request.getCouponCode(),
                    route.getOperatorId(),
                    baseFareAmount
            );
            if (!couponRes.isValid()) {
                throw new BadRequestException(couponRes.getMessage());
            }
            discountAmount = couponRes.getDiscountAmount();
            appliedCouponCode = couponRes.getCode();
            couponService.incrementCouponUsage(appliedCouponCode);
        }

        // Free Cancellation Add-on (₹21 per passenger)
        boolean hasFreeCancellation = Boolean.TRUE.equals(request.getHasFreeCancellation());
        BigDecimal freeCancellationFee = BigDecimal.ZERO;
        if (hasFreeCancellation) {
            freeCancellationFee = new BigDecimal("21.00").multiply(BigDecimal.valueOf(request.getPassengers().size()));
        }

        // Trip Guarantee Add-on (₹21 per passenger)
        boolean hasTripGuarantee = Boolean.TRUE.equals(request.getHasTripGuarantee());
        BigDecimal tripGuaranteeFee = BigDecimal.ZERO;
        if (hasTripGuarantee) {
            tripGuaranteeFee = new BigDecimal("21.00").multiply(BigDecimal.valueOf(request.getPassengers().size()));
        }

        // AI Dynamic Operator Service Fee (₹5 - ₹15)
        BigDecimal serviceFee = request.getServiceFee() != null ? request.getServiceFee() : BigDecimal.ZERO;

        BigDecimal subtotalAmount = baseFareAmount.subtract(discountAmount)
                .add(freeCancellationFee)
                .add(tripGuaranteeFee)
                .add(serviceFee)
                .max(BigDecimal.ZERO);
        
        // Check and apply wallet balance deduction if requested
        BigDecimal walletAmountUsed = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(request.getUseWalletBalance()) && user.getWalletBalance() != null && user.getWalletBalance().compareTo(BigDecimal.ZERO) > 0) {
            walletAmountUsed = user.getWalletBalance().min(subtotalAmount);
            BigDecimal newWalletBalance = user.getWalletBalance().subtract(walletAmountUsed);
            user.setWalletBalance(newWalletBalance);
            userRepository.save(user);
            log.info("Applied wallet balance ₹{} for user ID {}. Remaining wallet: ₹{}", walletAmountUsed, user.getId(), newWalletBalance);
        }

        BigDecimal finalTotalAmount = subtotalAmount.subtract(walletAmountUsed).max(BigDecimal.ZERO);
        boolean isFullyPaidByWallet = walletAmountUsed.compareTo(BigDecimal.ZERO) > 0 && finalTotalAmount.compareTo(BigDecimal.ZERO) == 0;
        String initialStatus = isFullyPaidByWallet ? "CONFIRMED" : "PENDING_PAYMENT";
        String pnr = generatePnr();

        Long bookingOperatorId = route.getOperatorId();
        if (bookingOperatorId == null && route.getBus() != null) {
            bookingOperatorId = route.getBus().getOperatorId();
        }
        if (bookingOperatorId == null) {
            bookingOperatorId = 1L;
        }

        Booking booking = Booking.builder()
                .user(user)
                .route(route)
                .operatorId(bookingOperatorId)
                .commissionAmount(baseFareAmount.multiply(new BigDecimal("0.10")))
                .pnr(pnr)
                .totalAmount(finalTotalAmount)
                .walletAmountUsed(walletAmountUsed)
                .couponCode(appliedCouponCode)
                .discountAmount(discountAmount)
                .hasFreeCancellation(hasFreeCancellation)
                .freeCancellationFee(freeCancellationFee)
                .hasTripGuarantee(hasTripGuarantee)
                .tripGuaranteeFee(tripGuaranteeFee)
                .serviceFee(serviceFee)
                .status(initialStatus)
                .refundStatus("NONE")
                .refundDestination("WALLET")
                .refundStage("NONE")
                .boardingPoint(request.getBoardingPoint())
                .droppingPoint(request.getDroppingPoint())
                .contactEmail(request.getContactEmail().trim())
                .contactPhone(request.getContactPhone().trim())
                .build();

        List<BookingPassenger> passengerEntities = new ArrayList<>();
        for (int i = 0; i < request.getPassengers().size(); i++) {
            PassengerDto p = request.getPassengers().get(i);
            Seat s = validatedSeats.get(i);
            BookingPassenger bp = BookingPassenger.builder()
                    .booking(booking)
                    .seat(s)
                    .name(p.getName().trim())
                    .age(p.getAge())
                    .gender(p.getGender().toUpperCase())
                    .seatNumber(s.getSeatNumber())
                    .build();
            passengerEntities.add(bp);
        }
        booking.setPassengers(passengerEntities);

        Booking saved = bookingRepository.save(booking);

        // If booking is 100% paid by wallet balance, immediately mark seats BOOKED and credit operator wallet
        if (isFullyPaidByWallet) {
            for (BookingPassenger bp : passengerEntities) {
                routeSeatRepository.findByRouteIdAndSeatId(route.getId(), bp.getSeat().getId())
                        .ifPresent(rs -> {
                            rs.setStatus("BOOKED");
                            rs.setBookedGender(bp.getGender() != null ? bp.getGender().toUpperCase() : "MALE");
                            rs.setLockExpiry(null);
                            rs.setLockedByUserId(null);
                            routeSeatRepository.save(rs);

                            if ("FEMALE".equalsIgnoreCase(bp.getGender())) {
                                markAdjacentSeatFemaleRestricted(route.getId(), rs.getSeat());
                            }
                        });
            }

            creditOperatorWalletForBooking(saved);

            try {
                emailService.sendBookingConfirmationEmail(saved, null);
            } catch (Exception e) {
                log.warn("Automatic e-ticket email dispatch failed for 100% wallet booking PNR {}: {}", saved.getPnr(), e.getMessage());
            }
        }

        return mapToDto(saved);
    }

    @Transactional
    public void creditOperatorWalletForBooking(Booking booking) {
        if (booking == null || booking.getOperatorId() == null) return;
        Long opId = booking.getOperatorId();

        Operator op = operatorRepository.findById(opId).orElse(null);
        if (op == null) {
            // Find first operator or by company name
            op = operatorRepository.findAll().stream().findFirst().orElse(null);
        }

        if (op != null && op.getUser() != null) {
            User opUser = op.getUser();
            BigDecimal netEarnings = booking.getTotalAmount().add(booking.getWalletAmountUsed() != null ? booking.getWalletAmountUsed() : BigDecimal.ZERO)
                    .subtract(booking.getCommissionAmount() != null ? booking.getCommissionAmount() : BigDecimal.ZERO)
                    .max(BigDecimal.ZERO);

            BigDecimal currentBal = opUser.getWalletBalance() != null ? opUser.getWalletBalance() : BigDecimal.ZERO;
            BigDecimal newBal = currentBal.add(netEarnings);
            opUser.setWalletBalance(newBal);
            userRepository.save(opUser);

            OperatorWalletTransaction tx = OperatorWalletTransaction.builder()
                    .operatorId(op.getId())
                    .bookingId(booking.getId())
                    .pnr(booking.getPnr())
                    .type("CREDIT_TICKET_FARE")
                    .amount(netEarnings)
                    .balanceAfter(newBal)
                    .description("Ticket fare credited for PNR " + booking.getPnr() + " (Gross: ₹" + (booking.getTotalAmount().add(booking.getWalletAmountUsed())) + " less 10% commission ₹" + booking.getCommissionAmount() + ")")
                    .build();
            operatorWalletTransactionRepository.save(tx);

            log.info("Credited Operator ID {} wallet with ₹{}. New wallet balance: ₹{}", op.getId(), netEarnings, newBal);
        }
    }

    @Transactional(readOnly = true)
    public BookingResponseDto getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr));
        return mapToDto(booking);
    }

    @Transactional
    public Page<BookingResponseDto> getUserBookings(Long userId, Pageable pageable) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        
        // Auto-expire any pending payment bookings created more than 10 minutes ago
        List<Booking> expiredPending = bookingRepository.findByStatusAndCreatedAtBefore("PENDING_PAYMENT", threshold);
        if (!expiredPending.isEmpty()) {
            for (Booking b : expiredPending) {
                b.setStatus("EXPIRED");
                bookingRepository.save(b);
                for (BookingPassenger bp : b.getPassengers()) {
                    routeSeatRepository.findByRouteIdAndSeatId(b.getRoute().getId(), bp.getSeat().getId()).ifPresent(rs -> {
                        if ("LOCKED".equalsIgnoreCase(rs.getStatus())) {
                            rs.setStatus("AVAILABLE");
                            rs.setLockExpiry(null);
                            rs.setLockedByUserId(null);
                            routeSeatRepository.save(rs);
                        }
                    });
                }
            }
        }

        return bookingRepository.findValidUserBookings(userId, threshold, pageable)
                .map(this::mapToDto);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredPendingBookingsJob() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        List<Booking> expiredPending = bookingRepository.findByStatusAndCreatedAtBefore("PENDING_PAYMENT", threshold);
        if (!expiredPending.isEmpty()) {
            for (Booking b : expiredPending) {
                b.setStatus("EXPIRED");
                bookingRepository.save(b);
                for (BookingPassenger bp : b.getPassengers()) {
                    routeSeatRepository.findByRouteIdAndSeatId(b.getRoute().getId(), bp.getSeat().getId()).ifPresent(rs -> {
                        if ("LOCKED".equalsIgnoreCase(rs.getStatus())) {
                            rs.setStatus("AVAILABLE");
                            rs.setLockExpiry(null);
                            rs.setLockedByUserId(null);
                            routeSeatRepository.save(rs);
                        }
                    });
                }
            }
            log.info("Automatically cleaned up {} pending payment bookings older than 10 minutes", expiredPending.size());
        }
    }

    @Transactional
    public CancelBookingResponse cancelBooking(String pnr, String reason, String refundDestination, Long currentUserId) {
        Booking booking = bookingRepository.findByPnr(pnr.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr));

        if (currentUserId != null && !booking.getUser().getId().equals(currentUserId)) {
            User requestingUser = userRepository.findById(currentUserId).orElse(null);
            if (requestingUser == null || (!"ROLE_ADMIN".equals(requestingUser.getRole()) && !"ROLE_OPERATOR".equals(requestingUser.getRole()))) {
                throw new BadRequestException("You are not authorized to cancel this booking");
            }
        }

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus()) || "REFUNDED".equalsIgnoreCase(booking.getStatus())) {
            throw new BadRequestException("This booking is already cancelled.");
        }

        Route route = booking.getRoute();
        LocalDateTime departureDateTime = LocalDateTime.of(route.getTravelDate(), route.getDepartureTime());
        LocalDateTime now = LocalDateTime.now();

        BigDecimal refundAmount;
        BigDecimal originalPaidTotal = booking.getTotalAmount().add(booking.getWalletAmountUsed() != null ? booking.getWalletAmountUsed() : BigDecimal.ZERO);
        BigDecimal refundableBase = originalPaidTotal;

        if (Boolean.TRUE.equals(booking.getHasFreeCancellation())) {
            if (booking.getFreeCancellationFee() != null) {
                refundableBase = refundableBase.subtract(booking.getFreeCancellationFee());
            }
            refundAmount = refundableBase.max(BigDecimal.ZERO);
        } else {
            Duration duration = Duration.between(now, departureDateTime);
            long hoursUntilDeparture = duration.toHours();

            BigDecimal refundPercentage;
            if (hoursUntilDeparture >= 24) {
                refundPercentage = new BigDecimal("0.90"); // 90% refund (> 24 hours before trip)
            } else if (hoursUntilDeparture >= 12) {
                refundPercentage = new BigDecimal("0.75"); // 75% refund (12 to 24 hours before trip)
            } else if (hoursUntilDeparture >= 2) {
                refundPercentage = new BigDecimal("0.50"); // 50% refund (2 to 12 hours before trip)
            } else {
                refundPercentage = new BigDecimal("0.25"); // 25% refund (< 2 hours before trip)
            }

            refundAmount = refundableBase.multiply(refundPercentage).setScale(2, RoundingMode.HALF_UP);
        }

        String destination = (refundDestination != null && refundDestination.equalsIgnoreCase("ORIGINAL_PAYMENT"))
                ? "ORIGINAL_PAYMENT"
                : "WALLET";

        booking.setStatus("CANCELLED");
        booking.setCancellationReason(reason != null ? reason : "Cancelled by customer");
        booking.setRefundAmount(refundAmount);
        booking.setRefundStatus("REQUESTED");
        booking.setRefundDestination(destination);
        booking.setRefundStage("OPERATOR_AUDIT");
        booking.setRefundRequestedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Free up the route seats immediately so other travelers can book
        for (BookingPassenger passenger : booking.getPassengers()) {
            routeSeatRepository.findByRouteIdAndSeatId(route.getId(), passenger.getSeat().getId())
                    .ifPresent(rs -> {
                        rs.setStatus("AVAILABLE");
                        rs.setLockExpiry(null);
                        rs.setLockedByUserId(null);
                        routeSeatRepository.save(rs);
                    });
        }

        // Send cancellation request notification
        try {
            emailService.sendBookingCancellationEmail(booking, refundAmount);
        } catch (Exception e) {
            log.warn("Failed to dispatch booking cancellation email for PNR {}: {}", booking.getPnr(), e.getMessage());
        }

        String destLabel = destination.equals("WALLET") ? "redBus Wallet (Instant upon audit)" : "Original Payment Method (3-5 business days)";

        return CancelBookingResponse.builder()
                .pnr(booking.getPnr())
                .status("CANCELLED")
                .refundStatus("REQUESTED")
                .refundDestination(destination)
                .refundStage("OPERATOR_AUDIT")
                .refundAmount(refundAmount)
                .walletBalance(booking.getUser().getWalletBalance() != null ? booking.getUser().getWalletBalance() : BigDecimal.ZERO)
                .message("Cancellation initiated successfully! Refund of ₹" + refundAmount + " is queued for operator audit and will be disbursed to your " + destLabel + ".")
                .build();
    }

    @Transactional
    public CancelBookingResponse approveOperatorRefund(String pnr, Operator operator) {
        Booking booking = bookingRepository.findByPnr(pnr.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr));

        BigDecimal refundAmount = booking.getRefundAmount() != null ? booking.getRefundAmount() : BigDecimal.ZERO;
        String destination = booking.getRefundDestination() != null ? booking.getRefundDestination() : "WALLET";

        // 1. Debit the refund amount from operator's redBus wallet
        if (operator != null && operator.getUser() != null) {
            User opUser = operator.getUser();
            BigDecimal currentOpWallet = opUser.getWalletBalance() != null ? opUser.getWalletBalance() : BigDecimal.ZERO;
            BigDecimal newOpWallet = currentOpWallet.subtract(refundAmount);
            opUser.setWalletBalance(newOpWallet);
            userRepository.save(opUser);

            OperatorWalletTransaction debitTx = OperatorWalletTransaction.builder()
                    .operatorId(operator.getId())
                    .bookingId(booking.getId())
                    .pnr(booking.getPnr())
                    .type("DEBIT_REFUND_AUDIT")
                    .amount(refundAmount)
                    .balanceAfter(newOpWallet)
                    .description("Refund disbursed for PNR " + booking.getPnr() + " to passenger " + destination)
                    .build();
            operatorWalletTransactionRepository.save(debitTx);
            log.info("Debited Operator ID {} wallet ₹{} for refund of PNR {}", operator.getId(), refundAmount, booking.getPnr());
        }

        // 2. Disburse refund to passenger if destination is WALLET
        BigDecimal passengerWalletBalance = BigDecimal.ZERO;
        User passenger = booking.getUser();
        if ("WALLET".equalsIgnoreCase(destination) && passenger != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal passCurBal = passenger.getWalletBalance() != null ? passenger.getWalletBalance() : BigDecimal.ZERO;
            BigDecimal passNewBal = passCurBal.add(refundAmount);
            passenger.setWalletBalance(passNewBal);
            userRepository.save(passenger);
            passengerWalletBalance = passNewBal;
            log.info("Credited passenger User ID {} redBus wallet with ₹{}. New balance: ₹{}", passenger.getId(), refundAmount, passNewBal);
        } else if (passenger != null) {
            passengerWalletBalance = passenger.getWalletBalance() != null ? passenger.getWalletBalance() : BigDecimal.ZERO;
        }

        // 3. Mark booking stage as COMPLETED / REFUNDED
        booking.setStatus("REFUNDED");
        booking.setRefundStatus("REFUNDED");
        booking.setRefundStage("COMPLETED");
        booking.setRefundApprovedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        return CancelBookingResponse.builder()
                .pnr(booking.getPnr())
                .status("REFUNDED")
                .refundStatus("REFUNDED")
                .refundDestination(destination)
                .refundStage("COMPLETED")
                .refundAmount(refundAmount)
                .walletBalance(passengerWalletBalance)
                .message("Refund of ₹" + refundAmount + " approved and disbursed successfully to " + destination + "!")
                .build();
    }

    private void markAdjacentSeatFemaleRestricted(Long routeId, Seat seat) {
        List<RouteSeat> allRouteSeats = routeSeatRepository.findByRouteId(routeId);
        for (RouteSeat other : allRouteSeats) {
            Seat otherSeat = other.getSeat();
            if (otherSeat.getDeck().equalsIgnoreCase(seat.getDeck())
                    && otherSeat.getRowNum().equals(seat.getRowNum())
                    && !otherSeat.getId().equals(seat.getId())) {
                if (Math.abs(otherSeat.getColNum() - seat.getColNum()) == 1) {
                    if ("AVAILABLE".equalsIgnoreCase(other.getStatus())) {
                        other.setGenderRestriction("FEMALE");
                        routeSeatRepository.save(other);
                    }
                }
            }
        }
    }

    private String generatePnr() {
        StringBuilder sb = new StringBuilder("RB-2026-");
        for (int i = 0; i < 6; i++) {
            sb.append(PNR_CHARS.charAt(random.nextInt(PNR_CHARS.length())));
        }
        return sb.toString();
    }

    public BookingResponseDto mapToDto(Booking booking) {
        Route route = booking.getRoute();
        List<PassengerDto> passengerDtos = booking.getPassengers().stream().map(bp ->
                PassengerDto.builder()
                        .seatId(bp.getSeat().getId())
                        .seatNumber(bp.getSeatNumber())
                        .name(bp.getName())
                        .age(bp.getAge())
                        .gender(bp.getGender())
                        .build()
        ).collect(Collectors.toList());

        return BookingResponseDto.builder()
                .id(booking.getId())
                .pnr(booking.getPnr())
                .routeId(route.getId())
                .sourceCity(route.getSourceCity())
                .destinationCity(route.getDestinationCity())
                .travelDate(route.getTravelDate())
                .departureTime(route.getDepartureTime())
                .arrivalTime(route.getArrivalTime())
                .operatorName(route.getBus().getOperatorName())
                .busType(route.getBus().getBusType())
                .totalAmount(booking.getTotalAmount())
                .walletAmountUsed(booking.getWalletAmountUsed())
                .couponCode(booking.getCouponCode())
                .discountAmount(booking.getDiscountAmount())
                .hasFreeCancellation(booking.getHasFreeCancellation())
                .freeCancellationFee(booking.getFreeCancellationFee())
                .hasTripGuarantee(booking.getHasTripGuarantee())
                .tripGuaranteeFee(booking.getTripGuaranteeFee())
                .serviceFee(booking.getServiceFee())
                .status(booking.getStatus())
                .refundStatus(booking.getRefundStatus())
                .refundDestination(booking.getRefundDestination())
                .refundStage(booking.getRefundStage())
                .refundRequestedAt(booking.getRefundRequestedAt())
                .refundApprovedAt(booking.getRefundApprovedAt())
                .boardingPoint(booking.getBoardingPoint())
                .droppingPoint(booking.getDroppingPoint())
                .contactEmail(booking.getContactEmail())
                .contactPhone(booking.getContactPhone())
                .cancellationReason(booking.getCancellationReason())
                .refundAmount(booking.getRefundAmount())
                .createdAt(booking.getCreatedAt())
                .passengers(passengerDtos)
                .build();
    }
}
