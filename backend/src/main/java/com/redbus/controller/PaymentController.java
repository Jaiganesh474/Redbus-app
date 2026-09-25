package com.redbus.controller;

import com.redbus.dto.CreatePaymentOrderRequest;
import com.redbus.dto.PaymentOrderResponse;
import com.redbus.dto.VerifyPaymentRequest;
import com.redbus.dto.VerifyPaymentResponse;
import com.redbus.entity.Booking;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.repository.BookingRepository;
import com.redbus.service.PaymentService;
import com.redbus.service.PdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/payments", "/api/v1/payments"})
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PdfService pdfService;
    private final BookingRepository bookingRepository;

    @PostMapping("/create-order")
    public ResponseEntity<PaymentOrderResponse> createOrder(@Valid @RequestBody CreatePaymentOrderRequest request) {
        PaymentOrderResponse response = paymentService.createOrder(request.getPnr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyPaymentResponse> verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        VerifyPaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        log.info("Received Razorpay webhook event with signature: {}", signature);
        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
