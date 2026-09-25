package com.redbus.controller;

import com.redbus.config.JwtUtil;
import com.redbus.dto.BookingResponseDto;
import com.redbus.dto.CancelBookingRequest;
import com.redbus.dto.CancelBookingResponse;
import com.redbus.dto.CreateBookingRequest;
import com.redbus.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/bookings", "/api/v1/bookings"})
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final JwtUtil jwtUtil;
    private final com.redbus.service.EmailService emailService;
    private final com.redbus.service.PdfService pdfService;
    private final com.redbus.repository.BookingRepository bookingRepository;

    @GetMapping("/{pnr}/ticket-pdf")
    public ResponseEntity<byte[]> downloadTicketPdf(@PathVariable String pnr) {
        com.redbus.entity.Booking booking = bookingRepository.findByPnr(pnr.trim().toUpperCase())
                .orElseThrow(() -> new com.redbus.exception.ResourceNotFoundException("Booking not found with PNR: " + pnr));

        byte[] pdfBytes = pdfService.generateTicketPdf(booking);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "redbus_ticket_" + booking.getPnr() + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = extractUserIdFromHeader(authHeader);
        BookingResponseDto booking = bookingService.createBooking(request, userId);
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    @GetMapping("/{pnr}")
    public ResponseEntity<BookingResponseDto> getBooking(@PathVariable String pnr) {
        return ResponseEntity.ok(bookingService.getBookingByPnr(pnr));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<Page<BookingResponseDto>> getMyBookings(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = extractUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Page<BookingResponseDto> bookings = bookingService.getUserBookings(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/{pnr}/send-email")
    public ResponseEntity<java.util.Map<String, Object>> sendTicketEmail(
            @PathVariable String pnr,
            @RequestParam(required = false) String email
    ) {
        com.redbus.entity.Booking booking = bookingRepository.findByPnr(pnr.trim().toUpperCase())
                .orElseThrow(() -> new com.redbus.exception.ResourceNotFoundException("Booking not found with PNR: " + pnr));
        String target = (email != null && !email.isBlank()) ? email.trim() : booking.getContactEmail();
        boolean sent;
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus()) || "REFUNDED".equalsIgnoreCase(booking.getStatus())) {
            sent = emailService.sendBookingCancellationEmail(booking, booking.getRefundAmount());
        } else {
            sent = emailService.sendBookingConfirmationEmail(booking, target);
        }
        return ResponseEntity.ok(java.util.Map.of(
                "success", sent,
                "pnr", booking.getPnr(),
                "status", booking.getStatus(),
                "recipient", target,
                "message", "Ticket document with PDF attachment dispatched to " + target
        ));
    }

    @PostMapping("/{pnr}/cancel")
    public ResponseEntity<CancelBookingResponse> cancelBooking(
            @PathVariable String pnr,
            @RequestBody(required = false) CancelBookingRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = extractUserIdFromHeader(authHeader);
        String reason = request != null ? request.getReason() : "Customer cancelled online";
        CancelBookingResponse response = bookingService.cancelBooking(pnr, reason, userId);
        return ResponseEntity.ok(response);
    }

    private Long extractUserIdFromHeader(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                return jwtUtil.extractUserId(token);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
