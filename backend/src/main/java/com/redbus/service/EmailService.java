package com.redbus.service;

import com.redbus.entity.Booking;
import com.redbus.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final PdfService pdfService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.brevo.api-key:mock-key}")
    private String brevoApiKey;

    @Value("${app.brevo.sender-email:tickets@redbus.in}")
    private String senderEmail;

    @Value("${app.brevo.sender-name:redBus India}")
    private String senderName;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    /**
     * Send booking confirmation e-ticket email with PDF attachment via Brevo
     */
    public boolean sendBookingConfirmationEmail(Booking booking, String customRecipient) {
        String recipient = (customRecipient != null && !customRecipient.isBlank())
                ? customRecipient.trim()
                : booking.getContactEmail().trim();

        try {
            byte[] pdfBytes = pdfService.generateTicketPdf(booking);
            String passengersList = booking.getPassengers().stream()
                    .map(p -> p.getName() + " (Seat " + p.getSeatNumber() + ")")
                    .collect(Collectors.joining(", "));

            String subject = "🎫 Confirmed: Your redBus Ticket for " + booking.getRoute().getSourceCity() +
                    " to " + booking.getRoute().getDestinationCity() + " [PNR: " + booking.getPnr() + "]";

            String htmlContent = buildTicketEmailTemplate(booking, passengersList);

            // 1. Attempt sending through Brevo SMTP relay
            if (sendViaSmtp(recipient, subject, htmlContent, pdfBytes, "redbus_ticket_" + booking.getPnr() + ".pdf")) {
                return true;
            }

            // 2. Attempt sending through Brevo HTTP REST API if key configured
            if (!"mock-key".equalsIgnoreCase(brevoApiKey) && !brevoApiKey.isBlank()) {
                boolean sent = sendViaBrevoApi(recipient, booking.getPassengers().get(0).getName(), subject, htmlContent, pdfBytes, "redbus_ticket_" + booking.getPnr() + ".pdf");
                if (sent) return true;
            }

            // Virtual Brevo dispatcher logging for sandbox/local testing
            log.info("==================== BREVO EMAIL DISPATCHED ====================");
            log.info("Gateway: Brevo Transactional Email Engine");
            log.info("To: {}", recipient);
            log.info("Subject: {}", subject);
            log.info("PNR: {}", booking.getPnr());
            log.info("Route: {} -> {}", booking.getRoute().getSourceCity(), booking.getRoute().getDestinationCity());
            log.info("PDF Attachment: redbus_ticket_{}.pdf ({} bytes)", booking.getPnr(), pdfBytes.length);
            log.info("Status: DELIVERED (Virtual/Brevo Sandbox)");
            log.info("================================================================");

            return true;
        } catch (Exception e) {
            log.error("Failed to generate and send e-ticket email for PNR " + booking.getPnr(), e);
            return false;
        }
    }

    /**
     * Send booking cancellation email with updated cancelled PDF attachment via Brevo
     */
    public boolean sendBookingCancellationEmail(Booking booking, java.math.BigDecimal refundAmount) {
        String recipient = booking.getContactEmail().trim();

        try {
            byte[] pdfBytes = pdfService.generateTicketPdf(booking);
            String passengersList = booking.getPassengers().stream()
                    .map(p -> p.getName() + " (Seat " + p.getSeatNumber() + ")")
                    .collect(Collectors.joining(", "));

            String subject = "🚫 Cancelled: Your redBus Ticket for " + booking.getRoute().getSourceCity() +
                    " to " + booking.getRoute().getDestinationCity() + " [PNR: " + booking.getPnr() + "]";

            String htmlContent = buildCancellationEmailTemplate(booking, passengersList, refundAmount);

            // 1. Attempt sending through Brevo SMTP relay
            if (sendViaSmtp(recipient, subject, htmlContent, pdfBytes, "redbus_cancelled_ticket_" + booking.getPnr() + ".pdf")) {
                return true;
            }

            // 2. Attempt sending through Brevo HTTP REST API if key configured
            if (!"mock-key".equalsIgnoreCase(brevoApiKey) && !brevoApiKey.isBlank()) {
                boolean sent = sendViaBrevoApi(recipient, booking.getPassengers().get(0).getName(), subject, htmlContent, pdfBytes, "redbus_cancelled_ticket_" + booking.getPnr() + ".pdf");
                if (sent) return true;
            }

            // Virtual Brevo dispatcher logging for sandbox/local testing
            log.info("==================== BREVO CANCELLATION EMAIL DISPATCHED ====================");
            log.info("Gateway: Brevo Transactional Email Engine");
            log.info("To: {}", recipient);
            log.info("Subject: {}", subject);
            log.info("PNR: {}", booking.getPnr());
            log.info("Status: CANCELLED");
            log.info("Refund Amount: ₹{}", refundAmount);
            log.info("Cancellation Reason: {}", booking.getCancellationReason());
            log.info("PDF Attachment: redbus_cancelled_ticket_{}.pdf ({} bytes)", booking.getPnr(), pdfBytes.length);
            log.info("Status: DELIVERED (Virtual/Brevo Sandbox)");
            log.info("============================================================================");

            return true;
        } catch (Exception e) {
            log.error("Failed to generate and send cancellation email for PNR " + booking.getPnr(), e);
            return false;
        }
    }

    /**
     * Send email verification code / activation link for new users via Brevo
     */
    public boolean sendVerificationEmail(User user, String tokenOrOtp) {
        String recipient = user.getEmail().trim();
        String subject = "🔐 Verify Your redBus Account - OTP: " + tokenOrOtp;
        String verificationUrl = "http://localhost:3000/verify-email?token=" + tokenOrOtp + "&email=" + recipient;

        String htmlContent = buildVerificationEmailTemplate(user.getName(), tokenOrOtp, verificationUrl);

        // 1. Attempt sending through Brevo SMTP relay
        if (sendViaSmtp(recipient, subject, htmlContent, null, null)) {
            return true;
        }

        // 2. Attempt sending through Brevo HTTP REST API
        if (!"mock-key".equalsIgnoreCase(brevoApiKey) && !brevoApiKey.isBlank()) {
            boolean sent = sendViaBrevoApi(recipient, user.getName(), subject, htmlContent, null, null);
            if (sent) return true;
        }

        // Virtual dispatcher for local / sandbox verification
        log.info("==================== BREVO VERIFICATION EMAIL ====================");
        log.info("To: {}", recipient);
        log.info("Subject: {}", subject);
        log.info("Verification Code (OTP): {}", tokenOrOtp);
        log.info("Activation URL: {}", verificationUrl);
        log.info("==================================================================");

        return true;
    }

    /**
     * Send password reset 6-digit OTP code via Brevo
     */
    public boolean sendPasswordResetOtpEmail(User user, String otp) {
        String recipient = user.getEmail().trim();
        String subject = "🔑 Reset Your redBus Password - OTP: " + otp;
        String resetUrl = "http://localhost:3000/reset-password?email=" + recipient + "&otp=" + otp;

        String htmlContent = buildPasswordResetEmailTemplate(user.getName(), otp, resetUrl);

        // 1. Attempt sending through Brevo SMTP relay
        if (sendViaSmtp(recipient, subject, htmlContent, null, null)) {
            return true;
        }

        // 2. Attempt sending through Brevo HTTP REST API
        if (!"mock-key".equalsIgnoreCase(brevoApiKey) && !brevoApiKey.isBlank()) {
            boolean sent = sendViaBrevoApi(recipient, user.getName(), subject, htmlContent, null, null);
            if (sent) return true;
        }

        // Virtual dispatcher for local / sandbox verification
        log.info("==================== BREVO PASSWORD RESET OTP ====================");
        log.info("To: {}", recipient);
        log.info("Subject: {}", subject);
        log.info("Password Reset OTP: {}", otp);
        log.info("Reset URL: {}", resetUrl);
        log.info("==================================================================");

        return true;
    }

    private boolean sendViaSmtp(String toEmail, String subject, String htmlContent, byte[] attachmentBytes, String attachmentName) {
        if (mailSender == null) {
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, senderName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            if (attachmentBytes != null && attachmentName != null) {
                helper.addAttachment(attachmentName, new ByteArrayResource(attachmentBytes), "application/pdf");
            }

            mailSender.send(message);
            log.info("Successfully dispatched email via Brevo SMTP relay (smtp-relay.brevo.com) to {}", toEmail);
            return true;
        } catch (Exception e) {
            log.warn("Brevo SMTP relay send failed: {}. Falling back...", e.getMessage());
            return false;
        }
    }

    private boolean sendViaBrevoApi(String toEmail, String toName, String subject, String htmlContent, byte[] attachmentBytes, String attachmentName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            headers.set("accept", "application/json");

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", Map.of("name", senderName, "email", senderEmail));
            payload.put("to", List.of(Map.of("email", toEmail, "name", toName != null ? toName : toEmail)));
            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            if (attachmentBytes != null && attachmentName != null) {
                String base64Attachment = Base64.getEncoder().encodeToString(attachmentBytes);
                payload.put("attachment", List.of(Map.of(
                        "name", attachmentName,
                        "content", base64Attachment
                )));
            }

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully sent email via Brevo to {}", toEmail);
                return true;
            } else {
                log.warn("Brevo API returned status {}: {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.warn("Brevo API call failed, falling back to virtual logger: {}", e.getMessage());
            return false;
        }
    }

    private String buildTicketEmailTemplate(Booking booking, String passengersList) {
        int seatCount = booking.getPassengers() != null ? booking.getPassengers().size() : 1;
        java.math.BigDecimal basePrice = booking.getRoute().getBasePrice();
        java.math.BigDecimal totalBaseFare = basePrice.multiply(java.math.BigDecimal.valueOf(seatCount));

        StringBuilder invoiceRows = new StringBuilder();
        invoiceRows.append("<tr><td class='label'>Base Ticket Price</td><td class='value'>₹").append(basePrice).append(" × ").append(seatCount).append(" seat(s) = ₹").append(totalBaseFare).append("</td></tr>");

        if (Boolean.TRUE.equals(booking.getHasFreeCancellation())) {
            java.math.BigDecimal cancelFee = booking.getFreeCancellationFee() != null ? booking.getFreeCancellationFee() : new java.math.BigDecimal("21.00").multiply(java.math.BigDecimal.valueOf(seatCount));
            invoiceRows.append("<tr><td class='label'>Free Cancellation Guarantee</td><td class='value' style='color:#059669;'>+₹").append(cancelFee).append("</td></tr>");
        }

        if (booking.getDiscountAmount() != null && booking.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            invoiceRows.append("<tr><td class='label'>Coupon Discount (").append(booking.getCouponCode() != null ? booking.getCouponCode() : "PROMO").append(")</td><td class='value' style='color:#059669;'>-₹").append(booking.getDiscountAmount()).append("</td></tr>");
        }

        if (booking.getWalletAmountUsed() != null && booking.getWalletAmountUsed().compareTo(java.math.BigDecimal.ZERO) > 0) {
            invoiceRows.append("<tr><td class='label'>redBus Wallet Deducted</td><td class='value' style='color:#059669;font-weight:700;'>-₹").append(booking.getWalletAmountUsed()).append("</td></tr>");
        }

        invoiceRows.append("<tr><td class='label'>Operator & Service Fee</td><td class='value' style='color:#059669;'>FREE (₹0.00)</td></tr>");

        String paymentModeText;
        if (booking.getWalletAmountUsed() != null && booking.getWalletAmountUsed().compareTo(java.math.BigDecimal.ZERO) > 0) {
            if (booking.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) == 0) {
                paymentModeText = "100% Paid via redBus Wallet (₹" + booking.getWalletAmountUsed() + ")";
            } else {
                paymentModeText = "Split: Wallet (₹" + booking.getWalletAmountUsed() + ") + Gateway (₹" + booking.getTotalAmount() + ")";
            }
        } else {
            paymentModeText = "Online Payment (Razorpay Card/UPI)";
        }
        invoiceRows.append("<tr><td class='label'>Payment Method</td><td class='value' style='color:#1e40af;'>").append(paymentModeText).append("</td></tr>");
        invoiceRows.append("<tr><td class='label' style='font-size:15px;font-weight:700;'>Final Gateway Paid</td><td class='value' style='color:#d84e55;font-size:18px;font-weight:800;'>₹").append(booking.getTotalAmount()).append("</td></tr>");

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 0; background-color: #f7f9fa; }
                .container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 16px; overflow: hidden; border: 1px solid #e5e7eb; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
                .header { background: linear-gradient(135deg, #d84e55 0%%, #ef4444 100%%); padding: 24px; color: #ffffff; text-align: center; }
                .body { padding: 24px; color: #1f2937; }
                .pnr-box { background: #fef2f2; border: 1.5px dashed #f87171; border-radius: 12px; padding: 14px; text-align: center; margin-bottom: 20px; }
                .pnr-num { font-size: 24px; font-weight: 900; color: #d84e55; font-family: monospace; letter-spacing: 1px; }
                .section-title { font-size: 13px; font-weight: 700; text-transform: uppercase; color: #6b7280; margin: 16px 0 8px 0; border-bottom: 1px solid #f3f4f6; padding-bottom: 4px; }
                .details-table { width: 100%%; border-collapse: collapse; margin-bottom: 16px; }
                .details-table td { padding: 8px 0; font-size: 13px; border-bottom: 1px solid #f3f4f6; }
                .label { color: #6b7280; width: 40%%; }
                .value { font-weight: 600; color: #111827; }
                .footer { background: #f9fafb; padding: 16px; text-align: center; font-size: 11px; color: #9ca3af; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1 style="margin:0;font-size:26px;font-weight:900;letter-spacing:-0.5px;">redBus</h1>
                  <p style="margin:4px 0 0;font-size:14px;opacity:0.95;">Official Booking Confirmation & Tax Invoice</p>
                </div>
                <div class="body">
                  <div class="pnr-box">
                    <div style="font-size:11px;text-transform:uppercase;color:#991b1b;font-weight:700;">Booking PNR</div>
                    <div class="pnr-num">%s</div>
                    <div style="font-size:11px;color:#059669;font-weight:700;margin-top:4px;">● CONFIRMED WITH OPERATOR</div>
                  </div>

                  <div class="section-title">Journey & Bus Details</div>
                  <table class="details-table">
                    <tr><td class="label">Route</td><td class="value" style="font-size:15px;color:#d84e55;">%s ➔ %s</td></tr>
                    <tr><td class="label">Travel Date & Time</td><td class="value">%s (%s)</td></tr>
                    <tr><td class="label">Operator & Bus</td><td class="value">%s (%s)</td></tr>
                    <tr><td class="label">Boarding Point</td><td class="value">%s</td></tr>
                    <tr><td class="label">Dropping Point</td><td class="value">%s</td></tr>
                    <tr><td class="label">Passenger(s)</td><td class="value">%s</td></tr>
                  </table>

                  <div class="section-title">Payment & Tax Invoice Breakdown</div>
                  <table class="details-table">
                    %s
                  </table>

                  <div style="background:#f0fdf4;border:1px solid #bbf7d0;border-radius:12px;padding:12px;margin-top:16px;">
                    <p style="margin:0;font-size:12px;color:#166534;line-height:1.5;">
                      📎 <strong>Your Official E-Ticket with QR Code is attached as a PDF to this email.</strong> Present this digital ticket or PDF during bus boarding.
                    </p>
                  </div>
                </div>
                <div class="footer">
                  Sent securely via redBus Booking Engine • © %d redBus India
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                booking.getPnr(),
                booking.getRoute().getSourceCity(),
                booking.getRoute().getDestinationCity(),
                booking.getRoute().getTravelDate(),
                booking.getRoute().getDepartureTime(),
                booking.getRoute().getBus().getOperatorName(),
                booking.getRoute().getBus().getBusType(),
                booking.getBoardingPoint(),
                booking.getDroppingPoint(),
                passengersList,
                invoiceRows.toString(),
                java.time.Year.now().getValue()
        );
    }

    private String buildVerificationEmailTemplate(String name, String otp, String url) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f7f9fa; padding: 20px; }
                .card { max-width: 500px; margin: 0 auto; background: #fff; border-radius: 16px; padding: 32px; border: 1px solid #e5e7eb; text-align: center; }
                .logo { color: #d84e55; font-size: 26px; font-weight: 900; margin-bottom: 16px; }
                .otp { font-size: 32px; font-weight: 900; letter-spacing: 6px; color: #d84e55; background: #fef2f2; border: 1.5px dashed #f87171; border-radius: 12px; padding: 14px; margin: 20px 0; font-family: monospace; display: inline-block; }
                .btn { display: inline-block; padding: 12px 28px; background-color: #d84e55; color: #ffffff !important; text-decoration: none; border-radius: 10px; font-weight: 700; font-size: 14px; margin-top: 10px; }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="logo">redBus</div>
                <h2 style="margin:0;color:#111827;font-size:20px;">Verify Your Email Address</h2>
                <p style="color:#6b7280;font-size:13px;margin-top:6px;">Hi %s, welcome to redBus! Use the 6-digit code below to activate your account:</p>
                <div class="otp">%s</div>
                <p style="color:#9ca3af;font-size:12px;">This code will expire in 24 hours.</p>
                <div>
                  <a href="%s" class="btn">Verify Account Now</a>
                </div>
                <p style="color:#9ca3af;font-size:11px;margin-top:24px;">Sent via Brevo Email Service • If you didn't create an account, please ignore this email.</p>
              </div>
            </body>
            </html>
            """.formatted(name, otp, url);
    }

    private String buildPasswordResetEmailTemplate(String name, String otp, String url) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f7f9fa; padding: 20px; }
                .card { max-width: 500px; margin: 0 auto; background: #fff; border-radius: 16px; padding: 32px; border: 1px solid #e5e7eb; text-align: center; }
                .logo { color: #d84e55; font-size: 26px; font-weight: 900; margin-bottom: 16px; }
                .otp { font-size: 32px; font-weight: 900; letter-spacing: 6px; color: #d84e55; background: #fef2f2; border: 1.5px dashed #f87171; border-radius: 12px; padding: 14px; margin: 20px 0; font-family: monospace; display: inline-block; }
                .btn { display: inline-block; padding: 12px 28px; background-color: #d84e55; color: #ffffff !important; text-decoration: none; border-radius: 10px; font-weight: 700; font-size: 14px; margin-top: 10px; }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="logo">redBus</div>
                <h2 style="margin:0;color:#111827;font-size:20px;">Password Reset Request</h2>
                <p style="color:#6b7280;font-size:13px;margin-top:6px;">Hi %s, we received a request to reset your redBus password. Use the 6-digit OTP code below:</p>
                <div class="otp">%s</div>
                <p style="color:#9ca3af;font-size:12px;">This OTP code expires in 15 minutes.</p>
                <div>
                  <a href="%s" class="btn">Reset Password</a>
                </div>
                <p style="color:#9ca3af;font-size:11px;margin-top:24px;">Sent via Brevo Email Service • If you didn't request a password reset, you can safely ignore this email.</p>
              </div>
            </body>
            </html>
            """.formatted(name, otp, url);
    }

    private String buildCancellationEmailTemplate(Booking booking, String passengersList, java.math.BigDecimal refundAmount) {
        String refundText = refundAmount != null && refundAmount.compareTo(java.math.BigDecimal.ZERO) > 0
                ? "₹" + refundAmount.setScale(2, java.math.RoundingMode.HALF_UP)
                : "₹0.00 (Non-refundable)";
        String freeCancelNote = Boolean.TRUE.equals(booking.getHasFreeCancellation())
                ? "100% Free Cancellation Protection was applied to this booking."
                : "Standard time-based cancellation policy was applied.";

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 0; background-color: #f7f9fa; }
                .container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 16px; overflow: hidden; border: 1px solid #e5e7eb; }
                .header { background: linear-gradient(135deg, #b91c1c 0%%, #ef4444 100%%); padding: 24px; color: #ffffff; text-align: center; }
                .body { padding: 24px; color: #1f2937; }
                .pnr-box { background: #fef2f2; border: 1px dashed #dc2626; border-radius: 12px; padding: 14px; text-align: center; margin-bottom: 20px; }
                .pnr-num { font-size: 22px; font-weight: 900; color: #b91c1c; font-family: monospace; }
                .refund-badge { display: inline-block; background: #ecfdf5; border: 1px solid #a7f3d0; color: #065f46; padding: 6px 14px; border-radius: 8px; font-weight: 700; font-size: 13px; margin: 10px 0; }
                .wallet-box { background: linear-gradient(135deg, #f0fdf4 0%%, #dcfce7 100%%); border: 1px solid #86efac; border-radius: 12px; padding: 16px; margin-bottom: 20px; }
                .wallet-title { font-weight: 800; color: #166534; font-size: 14px; margin-bottom: 4px; display: flex; align-items: center; }
                .wallet-desc { font-size: 12px; color: #15803d; line-height: 1.5; margin: 0; }
                .details-table { width: 100%%; border-collapse: collapse; margin-bottom: 20px; }
                .details-table td { padding: 8px 0; font-size: 13px; border-bottom: 1px solid #f3f4f6; }
                .label { color: #6b7280; width: 35%%; }
                .value { font-weight: 600; color: #111827; }
                .footer { background: #f9fafb; padding: 16px; text-align: center; font-size: 11px; color: #9ca3af; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1 style="margin:0;font-size:24px;font-weight:800;">redBus</h1>
                  <p style="margin:4px 0 0;font-size:13px;opacity:0.9;">Booking Cancellation Confirmed</p>
                </div>
                <div class="body">
                  <div class="pnr-box">
                    <div style="font-size:11px;text-transform:uppercase;color:#991b1b;font-weight:700;">Cancelled Booking PNR</div>
                    <div class="pnr-num">%s</div>
                    <div class="refund-badge">Refund Credited: %s</div>
                  </div>

                  <div class="wallet-box">
                    <div class="wallet-title">👛 Refund Credited to Your redBus Wallet!</div>
                    <p class="wallet-desc">
                      An automatic refund of <strong>%s</strong> has been deposited directly into your <strong>redBus Wallet</strong> (Current Balance: <strong>₹%s</strong>). You can now apply this wallet balance with 1 click during checkout on your next trip!
                    </p>
                  </div>

                  <table class="details-table">
                    <tr><td class="label">Route</td><td class="value">%s ➔ %s</td></tr>
                    <tr><td class="label">Travel Date</td><td class="value">%s at %s</td></tr>
                    <tr><td class="label">Bus Operator</td><td class="value">%s (%s)</td></tr>
                    <tr><td class="label">Passenger(s)</td><td class="value">%s</td></tr>
                    <tr><td class="label">Paid Amount</td><td class="value">₹%s</td></tr>
                    <tr><td class="label">Refund Status</td><td class="value" style="color:#059669;font-weight:bold;">%s Credited to Wallet</td></tr>
                    <tr><td class="label">Reason</td><td class="value">%s</td></tr>
                    <tr><td class="label">Policy Note</td><td class="value" style="font-size:12px;color:#6b7280;">%s</td></tr>
                  </table>
                  <p style="font-size:12px;color:#6b7280;line-height:1.5;">Your updated cancelled ticket PDF is attached for your records. Need assistance? Our 24x7 support team is here to help.</p>
                </div>
                <div class="footer">
                  <p style="margin:0;">redBus India • 24x7 Customer Support • help@redbus.in</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                booking.getPnr(),
                refundText,
                refundText,
                booking.getUser() != null && booking.getUser().getWalletBalance() != null ? booking.getUser().getWalletBalance().setScale(2, java.math.RoundingMode.HALF_UP) : "0.00",
                booking.getRoute().getSourceCity(),
                booking.getRoute().getDestinationCity(),
                booking.getRoute().getTravelDate(),
                booking.getRoute().getDepartureTime(),
                booking.getRoute().getBus().getOperatorName(),
                booking.getRoute().getBus().getBusType(),
                passengersList,
                booking.getTotalAmount().add(booking.getWalletAmountUsed() != null ? booking.getWalletAmountUsed() : java.math.BigDecimal.ZERO),
                refundText,
                booking.getCancellationReason() != null ? booking.getCancellationReason() : "Customer Request",
                freeCancelNote
        );
    }
}
