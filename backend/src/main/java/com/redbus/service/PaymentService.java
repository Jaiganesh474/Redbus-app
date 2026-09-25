package com.redbus.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.redbus.dto.PaymentOrderResponse;
import com.redbus.dto.VerifyPaymentRequest;
import com.redbus.dto.VerifyPaymentResponse;
import com.redbus.entity.*;
import com.redbus.exception.BadRequestException;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.repository.BookingRepository;
import com.redbus.repository.PaymentRepository;
import com.redbus.repository.RouteSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RouteSeatRepository routeSeatRepository;
    private final EmailService emailService;

    @Value("${app.razorpay.key-id:rzp_test_redbusKeyMock}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret:redbusSecretKeyMock}")
    private String razorpayKeySecret;

    @Transactional
    public PaymentOrderResponse createOrder(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr));

        if ("CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new BadRequestException("Booking is already confirmed");
        }

        long amountInPaise = booking.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        String orderId;

        // Create real order via Razorpay SDK with configured Test / Live credentials
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", booking.getPnr());
            orderRequest.put("payment_capture", 1);

            Order order = client.orders.create(orderRequest);
            orderId = order.get("id");
            log.info("Razorpay order created successfully: {} for PNR: {}", orderId, booking.getPnr());
        } catch (Exception e) {
            log.error("Razorpay API order creation failed: {}", e.getMessage(), e);
            throw new BadRequestException("Razorpay order creation failed: " + e.getMessage() + 
                    ". Please make sure valid RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET are set in the backend .env");
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .status("INITIATED")
                .method("RAZORPAY")
                .razorpayOrderId(orderId)
                .build();
        paymentRepository.save(payment);

        return PaymentOrderResponse.builder()
                .orderId(orderId)
                .currency("INR")
                .amountInPaise(amountInPaise)
                .amount(booking.getTotalAmount())
                .keyId(razorpayKeyId)
                .pnr(booking.getPnr())
                .build();
    }

    @Transactional
    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {
        Booking booking = bookingRepository.findByPnr(request.getPnr().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + request.getPnr()));

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseGet(() -> paymentRepository.findByBookingId(booking.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for order: " + request.getRazorpayOrderId())));

        boolean isValidSignature = verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                razorpayKeySecret
        );

        if (!isValidSignature) {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            throw new BadRequestException("Invalid payment signature. Verification failed.");
        }

        // Payment verified successfully
        payment.setStatus("SUCCESS");
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        paymentRepository.save(payment);

        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);

        // Automatically dispatch confirmation e-ticket email with PDF attachment
        try {
            emailService.sendBookingConfirmationEmail(booking, null);
        } catch (Exception e) {
            log.warn("Automatic e-ticket email dispatch failed: {}", e.getMessage());
        }

        // Update all passenger seats to BOOKED
        for (BookingPassenger passenger : booking.getPassengers()) {
            routeSeatRepository.findByRouteIdAndSeatId(booking.getRoute().getId(), passenger.getSeat().getId())
                    .ifPresent(rs -> {
                        rs.setStatus("BOOKED");
                        rs.setBookedGender(passenger.getGender() != null ? passenger.getGender().toUpperCase() : "MALE");
                        rs.setLockExpiry(null);
                        rs.setLockedByUserId(null);
                        routeSeatRepository.save(rs);

                        if ("FEMALE".equalsIgnoreCase(passenger.getGender())) {
                            markAdjacentSeatFemaleRestricted(booking.getRoute().getId(), rs.getSeat());
                        }
                    });
        }

        return VerifyPaymentResponse.builder()
                .success(true)
                .pnr(booking.getPnr())
                .bookingStatus("CONFIRMED")
                .message("Payment verified and booking confirmed successfully!")
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

    private boolean verifySignature(String orderId, String paymentId, String signature, String secret) {
        if (orderId == null || paymentId == null || signature == null || secret == null) {
            return false;
        }

        try {
            String data = orderId + "|" + paymentId;
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(generatedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Signature verification error", e);
            return false;
        }
    }
}
