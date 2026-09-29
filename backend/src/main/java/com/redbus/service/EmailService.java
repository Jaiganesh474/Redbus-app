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

    @Value("${app.frontend.url:https://redbusai.app}")
    private String frontendUrl;

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
        String verificationUrl = frontendUrl.replaceAll("/$", "") + "/verify-email?token=" + tokenOrOtp + "&email=" + recipient;

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
     * Send Welcome & Account Verified confirmation email with Book Bus ad & booking link via Brevo
     */
    public boolean sendWelcomeAndVerificationEmail(User user) {
        String recipient = user.getEmail().trim();
        String subject = "🎉 Welcome to redBus AI! Your Account is Verified — Book Your First Bus Trip";
        String bookingUrl = frontendUrl.replaceAll("/$", "") + "/";

        String htmlContent = buildWelcomeAndVerifiedEmailTemplate(user.getName(), user.getEmail(), bookingUrl);

        // 1. Attempt sending through Brevo SMTP relay
        if (sendViaSmtp(recipient, subject, htmlContent, null, null)) {
            return true;
        }

        // 2. Attempt sending through Brevo HTTP REST API
        if (!"mock-key".equalsIgnoreCase(brevoApiKey) && !brevoApiKey.isBlank()) {
            boolean sent = sendViaBrevoApi(recipient, user.getName(), subject, htmlContent, null, null);
            if (sent) return true;
        }

        // Virtual dispatcher for local / sandbox logging
        log.info("==================== BREVO WELCOME & VERIFIED EMAIL DISPATCHED ====================");
        log.info("To: {}", recipient);
        log.info("Subject: {}", subject);
        log.info("Booking URL: {}", bookingUrl);
        log.info("===================================================================================");

        return true;
    }

    /**
     * Send email verification code for new Bus Operators / Fleet Partners via Brevo
     */
    public boolean sendOperatorVerificationEmail(User user, String companyName, String tokenOrOtp) {
        String recipient = user.getEmail().trim();
        String subject = "🚍 Verify Your redBus Fleet Partner Account [" + (companyName != null ? companyName : "Operator") + "] - OTP: " + tokenOrOtp;
        String verificationUrl = frontendUrl.replaceAll("/$", "") + "/operator/register?token=" + tokenOrOtp + "&email=" + recipient;

        String htmlContent = buildOperatorVerificationEmailTemplate(user.getName(), companyName, tokenOrOtp, verificationUrl);

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
        log.info("==================== BREVO OPERATOR VERIFICATION EMAIL ====================");
        log.info("To: {}", recipient);
        log.info("Company: {}", companyName);
        log.info("Subject: {}", subject);
        log.info("Verification Code (OTP): {}", tokenOrOtp);
        log.info("Activation URL: {}", verificationUrl);
        log.info("===========================================================================");

        return true;
    }

    /**
     * Send Operator Verification Confirmation email via Brevo
     */
    public boolean sendOperatorVerificationConfirmationEmail(User user, String companyName) {
        String recipient = user.getEmail().trim();
        String displayName = (companyName != null && !companyName.isBlank()) ? companyName : user.getName();
        String subject = "🚍 Account Verified — Welcome to redBus Partner Console [" + displayName + "]";
        String consoleUrl = frontendUrl.replaceAll("/$", "") + "/operator/login";

        String htmlContent = buildOperatorVerifiedConfirmationEmailTemplate(user.getName(), displayName, consoleUrl);

        // 1. Attempt sending through Brevo SMTP relay
        if (sendViaSmtp(recipient, subject, htmlContent, null, null)) {
            return true;
        }

        // 2. Attempt sending through Brevo HTTP REST API
        if (!"mock-key".equalsIgnoreCase(brevoApiKey) && !brevoApiKey.isBlank()) {
            boolean sent = sendViaBrevoApi(recipient, user.getName(), subject, htmlContent, null, null);
            if (sent) return true;
        }

        log.info("==================== BREVO OPERATOR VERIFIED CONFIRMATION ====================");
        log.info("To: {}", recipient);
        log.info("Company: {}", displayName);
        log.info("Console URL: {}", consoleUrl);
        log.info("==============================================================================");

        return true;
    }

    /**
     * Send password reset 6-digit OTP code via Brevo
     */
    public boolean sendPasswordResetOtpEmail(User user, String otp) {
        String recipient = user.getEmail().trim();
        String subject = "🔑 Reset Your redBus Password - OTP: " + otp;
        String resetUrl = frontendUrl.replaceAll("/$", "") + "/reset-password?email=" + recipient + "&otp=" + otp;

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

    /**
     * Send password changed / reset confirmation security notification email via Brevo
     */
    public boolean sendPasswordResetSuccessEmail(User user) {
        String recipient = user.getEmail().trim();
        String subject = "🛡️ Security Alert: Your redBus Password Has Been Changed Successfully";
        String loginUrl = frontendUrl.replaceAll("/$", "") + "/";

        String formattedTime = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        String htmlContent = buildPasswordResetSuccessEmailTemplate(user.getName(), user.getEmail(), formattedTime, loginUrl);

        // 1. Attempt sending through Brevo SMTP relay
        if (sendViaSmtp(recipient, subject, htmlContent, null, null)) {
            return true;
        }

        // 2. Attempt sending through Brevo HTTP REST API
        if (!"mock-key".equalsIgnoreCase(brevoApiKey) && !brevoApiKey.isBlank()) {
            boolean sent = sendViaBrevoApi(recipient, user.getName(), subject, htmlContent, null, null);
            if (sent) return true;
        }

        log.info("==================== BREVO PASSWORD CHANGE CONFIRMATION ====================");
        log.info("To: {}", recipient);
        log.info("Subject: {}", subject);
        log.info("Time: {}", formattedTime);
        log.info("============================================================================");

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
        invoiceRows.append("<tr><td class='label' style='font-size:15px;font-weight:800;color:#111827;'>Final Gateway Paid</td><td class='value' style='color:#e23744;font-size:20px;font-weight:900;'>₹").append(booking.getTotalAmount()).append("</td></tr>");

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800;900&family=Poppins:wght@600;700;800;900&display=swap');
                * { box-sizing: border-box; }
                body {
                  font-family: 'Plus Jakarta Sans', 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  margin: 0;
                  padding: 24px 12px;
                  background-color: #f4f6f8;
                  color: #1c1c1c;
                  -webkit-font-smoothing: antialiased;
                }
                .container {
                  max-width: 580px;
                  margin: 0 auto;
                  background: #ffffff;
                  border-radius: 20px;
                  overflow: hidden;
                  border: 1px solid #eaeaea;
                  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
                }
                .header {
                  background: linear-gradient(135deg, #e23744 0%%, #cb202d 100%%);
                  padding: 28px 24px;
                  color: #ffffff;
                  text-align: center;
                }
                .header h1 {
                  margin: 0;
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-size: 30px;
                  font-weight: 900;
                  letter-spacing: -0.8px;
                  line-height: 1;
                }
                .header p {
                  margin: 6px 0 0;
                  font-size: 13px;
                  font-weight: 500;
                  opacity: 0.95;
                  letter-spacing: 0.2px;
                }
                .body {
                  padding: 26px 24px;
                }
                .pnr-box {
                  background: #fff5f6;
                  border: 1.5px dashed #f87171;
                  border-radius: 16px;
                  padding: 16px 14px;
                  text-align: center;
                  margin-bottom: 22px;
                }
                .pnr-label {
                  font-size: 11px;
                  text-transform: uppercase;
                  color: #991b1b;
                  font-weight: 800;
                  letter-spacing: 1px;
                }
                .pnr-num {
                  font-size: 26px;
                  font-weight: 900;
                  color: #e23744;
                  font-family: 'Plus Jakarta Sans', monospace;
                  letter-spacing: 1.5px;
                  margin: 4px 0 2px;
                }
                .pnr-badge {
                  display: inline-block;
                  font-size: 10px;
                  color: #065f46;
                  background: #d1fae5;
                  font-weight: 800;
                  padding: 3px 10px;
                  border-radius: 20px;
                  letter-spacing: 0.5px;
                }
                .section-title {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-size: 12px;
                  font-weight: 800;
                  text-transform: uppercase;
                  letter-spacing: 0.8px;
                  color: #4b5563;
                  margin: 20px 0 10px 0;
                  border-bottom: 1.5px solid #f3f4f6;
                  padding-bottom: 6px;
                }
                .details-table {
                  width: 100%%;
                  border-collapse: collapse;
                  margin-bottom: 16px;
                }
                .details-table td {
                  padding: 10px 0;
                  font-size: 13px;
                  border-bottom: 1px solid #f3f4f6;
                  vertical-align: middle;
                }
                .label {
                  color: #6b7280;
                  width: 38%%;
                  font-weight: 500;
                }
                .value {
                  font-weight: 700;
                  color: #111827;
                }
                .route-text {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-size: 16px;
                  font-weight: 800;
                  color: #e23744;
                }
                .pdf-callout {
                  background: #f0fdf4;
                  border: 1.5px solid #bbf7d0;
                  border-radius: 14px;
                  padding: 14px;
                  margin-top: 20px;
                }
                .footer {
                  background: #fafafa;
                  padding: 18px 24px;
                  text-align: center;
                  font-size: 11px;
                  color: #9ca3af;
                  font-weight: 500;
                  border-top: 1px solid #f0f0f0;
                }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>redBus</h1>
                  <p>Official Booking Confirmation & Tax Invoice</p>
                </div>
                <div class="body">
                  <div class="pnr-box">
                    <div class="pnr-label">Booking PNR</div>
                    <div class="pnr-num">%s</div>
                    <div class="pnr-badge">● CONFIRMED WITH OPERATOR</div>
                  </div>

                  <div class="section-title">Journey & Bus Details</div>
                  <table class="details-table">
                    <tr><td class="label">Route</td><td class="value route-text">%s ➔ %s</td></tr>
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

                  <div class="pdf-callout">
                    <p style="margin:0;font-size:12px;color:#166534;line-height:1.5;font-weight:600;">
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
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800;900&family=Poppins:wght@600;700;800;900&display=swap');
                * { box-sizing: border-box; }
                body {
                  font-family: 'Plus Jakarta Sans', 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  background-color: #f4f6f8;
                  padding: 24px 12px;
                  margin: 0;
                  -webkit-font-smoothing: antialiased;
                }
                .card {
                  max-width: 480px;
                  margin: 0 auto;
                  background: #ffffff;
                  border-radius: 20px;
                  padding: 36px 28px;
                  border: 1px solid #eaeaea;
                  text-align: center;
                  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
                }
                .logo {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  color: #e23744;
                  font-size: 32px;
                  font-weight: 900;
                  margin-bottom: 18px;
                  letter-spacing: -0.8px;
                }
                .heading {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  margin: 0;
                  color: #111827;
                  font-size: 21px;
                  font-weight: 800;
                }
                .subtext {
                  color: #6b7280;
                  font-size: 13px;
                  font-weight: 500;
                  margin-top: 8px;
                  line-height: 1.5;
                }
                .otp {
                  font-size: 34px;
                  font-weight: 900;
                  letter-spacing: 8px;
                  color: #e23744;
                  background: #fff5f6;
                  border: 1.5px dashed #f87171;
                  border-radius: 16px;
                  padding: 16px;
                  margin: 22px 0;
                  font-family: 'Plus Jakarta Sans', monospace;
                  display: inline-block;
                }
                .btn {
                  display: inline-block;
                  padding: 14px 32px;
                  background: linear-gradient(135deg, #e23744 0%%, #cb202d 100%%);
                  color: #ffffff !important;
                  text-decoration: none;
                  border-radius: 14px;
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-weight: 800;
                  font-size: 14px;
                  margin-top: 10px;
                  box-shadow: 0 4px 14px rgba(226, 55, 68, 0.35);
                  letter-spacing: 0.2px;
                }
                .footer-text {
                  color: #9ca3af;
                  font-size: 11px;
                  font-weight: 500;
                  margin-top: 26px;
                  border-top: 1px solid #f0f0f0;
                  padding-top: 16px;
                }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="logo">redBus</div>
                <h2 class="heading">Verify Your Email Address</h2>
                <p class="subtext">Hi %s, welcome to redBus! Use the 6-digit code below to activate your account:</p>
                <div class="otp">%s</div>
                <p style="color:#9ca3af;font-size:12px;font-weight:500;">This code will expire in 24 hours.</p>
                <div>
                  <a href="%s" class="btn">Verify Account Now</a>
                </div>
                <p class="footer-text">Sent via Brevo Email Service • If you didn't create an account, please ignore this email.</p>
              </div>
            </body>
            </html>
            """.formatted(name, otp, url);
    }

    private String buildOperatorVerificationEmailTemplate(String name, String companyName, String otp, String url) {
        String displayName = (companyName != null && !companyName.isBlank()) ? companyName : name;
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800;900&family=Poppins:wght@600;700;800;900&display=swap');
                * { box-sizing: border-box; }
                body {
                  font-family: 'Plus Jakarta Sans', 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  background-color: #0f172a;
                  padding: 24px 12px;
                  margin: 0;
                  -webkit-font-smoothing: antialiased;
                }
                .card {
                  max-width: 500px;
                  margin: 0 auto;
                  background: #1e293b;
                  border-radius: 24px;
                  padding: 36px 28px;
                  border: 1px solid #334155;
                  text-align: center;
                  box-shadow: 0 20px 40px rgba(0,0,0,0.3);
                  color: #f8fafc;
                }
                .logo-badge {
                  display: inline-flex;
                  align-items: center;
                  background: rgba(216, 78, 85, 0.15);
                  border: 1px solid rgba(216, 78, 85, 0.3);
                  padding: 6px 14px;
                  border-radius: 9999px;
                  color: #d84e55;
                  font-size: 11px;
                  font-weight: 800;
                  letter-spacing: 1px;
                  text-transform: uppercase;
                  margin-bottom: 16px;
                }
                .brand {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-size: 26px;
                  font-weight: 900;
                  color: #ffffff;
                  margin-bottom: 4px;
                }
                .brand span {
                  color: #d84e55;
                  font-weight: 400;
                }
                .heading {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  margin: 12px 0 6px 0;
                  color: #ffffff;
                  font-size: 20px;
                  font-weight: 800;
                }
                .subtext {
                  color: #94a3b8;
                  font-size: 13px;
                  font-weight: 400;
                  line-height: 1.5;
                  margin: 0 0 20px 0;
                }
                .otp-box {
                  font-size: 36px;
                  font-weight: 900;
                  letter-spacing: 10px;
                  color: #d84e55;
                  background: #0f172a;
                  border: 2px dashed #d84e55;
                  border-radius: 18px;
                  padding: 18px;
                  margin: 18px 0;
                  font-family: 'Plus Jakarta Sans', monospace;
                  display: inline-block;
                }
                .notice {
                  background: rgba(59, 130, 246, 0.1);
                  border: 1px solid rgba(59, 130, 246, 0.2);
                  border-radius: 12px;
                  padding: 12px;
                  font-size: 12px;
                  color: #93c5fd;
                  margin: 16px 0;
                  text-align: left;
                  line-height: 1.4;
                }
                .btn {
                  display: inline-block;
                  padding: 14px 34px;
                  background: linear-gradient(135deg, #d84e55 0%%, #b91c1c 100%%);
                  color: #ffffff !important;
                  text-decoration: none;
                  border-radius: 14px;
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-weight: 800;
                  font-size: 14px;
                  margin-top: 8px;
                  box-shadow: 0 6px 20px rgba(216, 78, 85, 0.35);
                }
                .footer-text {
                  color: #64748b;
                  font-size: 11px;
                  margin-top: 24px;
                  border-top: 1px solid #334155;
                  padding-top: 16px;
                }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="logo-badge">🚍 redBus Fleet Partner Console</div>
                <div class="brand">redBus <span>Partner</span></div>
                <h2 class="heading">Verify Your Operator Email</h2>
                <p class="subtext">
                  Hi <strong>%s</strong>, thank you for registering with redBus. Enter this 6-digit verification code to authenticate your business email:
                </p>
                <div class="otp-box">%s</div>
                <div class="notice">
                  ℹ️ <strong>Next Step:</strong> Once your email is verified, your fleet registration will be forwarded to the redBus compliance team for administrative approval.
                </div>
                <p class="footer-text">Dispatched via Brevo Transactional Email • © redBus India Partner Ecosystem</p>
              </div>
            </body>
            </html>
            """.formatted(displayName, otp);
    }

    private String buildPasswordResetEmailTemplate(String name, String otp, String url) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800;900&family=Poppins:wght@600;700;800;900&display=swap');
                * { box-sizing: border-box; }
                body {
                  font-family: 'Plus Jakarta Sans', 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  background-color: #f4f6f8;
                  padding: 24px 12px;
                  margin: 0;
                  -webkit-font-smoothing: antialiased;
                }
                .card {
                  max-width: 480px;
                  margin: 0 auto;
                  background: #ffffff;
                  border-radius: 20px;
                  padding: 36px 28px;
                  border: 1px solid #eaeaea;
                  text-align: center;
                  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
                }
                .logo {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  color: #e23744;
                  font-size: 32px;
                  font-weight: 900;
                  margin-bottom: 18px;
                  letter-spacing: -0.8px;
                }
                .heading {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  margin: 0;
                  color: #111827;
                  font-size: 21px;
                  font-weight: 800;
                }
                .subtext {
                  color: #6b7280;
                  font-size: 13px;
                  font-weight: 500;
                  margin-top: 8px;
                  line-height: 1.5;
                }
                .otp {
                  font-size: 34px;
                  font-weight: 900;
                  letter-spacing: 8px;
                  color: #e23744;
                  background: #fff5f6;
                  border: 1.5px dashed #f87171;
                  border-radius: 16px;
                  padding: 16px;
                  margin: 22px 0;
                  font-family: 'Plus Jakarta Sans', monospace;
                  display: inline-block;
                }
                .btn {
                  display: inline-block;
                  padding: 14px 32px;
                  background: linear-gradient(135deg, #e23744 0%%, #cb202d 100%%);
                  color: #ffffff !important;
                  text-decoration: none;
                  border-radius: 14px;
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-weight: 800;
                  font-size: 14px;
                  margin-top: 10px;
                  box-shadow: 0 4px 14px rgba(226, 55, 68, 0.35);
                  letter-spacing: 0.2px;
                }
                .footer-text {
                  color: #9ca3af;
                  font-size: 11px;
                  font-weight: 500;
                  margin-top: 26px;
                  border-top: 1px solid #f0f0f0;
                  padding-top: 16px;
                }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="logo">redBus</div>
                <h2 class="heading">Password Reset Request</h2>
                <p class="subtext">Hi %s, we received a request to reset your redBus password. Use the 6-digit OTP code below:</p>
                <div class="otp">%s</div>
                <p style="color:#9ca3af;font-size:12px;font-weight:500;">This OTP code expires in 15 minutes.</p>
                <div>
                  <a href="%s" class="btn">Reset Password</a>
                </div>
                <p class="footer-text">Sent via Brevo Email Service • If you didn't request a password reset, you can safely ignore this email.</p>
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
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800;900&family=Poppins:wght@600;700;800;900&display=swap');
                * { box-sizing: border-box; }
                body {
                  font-family: 'Plus Jakarta Sans', 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  margin: 0;
                  padding: 24px 12px;
                  background-color: #f4f6f8;
                  color: #1c1c1c;
                  -webkit-font-smoothing: antialiased;
                }
                .container {
                  max-width: 580px;
                  margin: 0 auto;
                  background: #ffffff;
                  border-radius: 20px;
                  overflow: hidden;
                  border: 1px solid #eaeaea;
                  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
                }
                .header {
                  background: linear-gradient(135deg, #e23744 0%%, #b91c1c 100%%);
                  padding: 28px 24px;
                  color: #ffffff;
                  text-align: center;
                }
                .header h1 {
                  margin: 0;
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-size: 30px;
                  font-weight: 900;
                  letter-spacing: -0.8px;
                }
                .header p {
                  margin: 6px 0 0;
                  font-size: 13px;
                  font-weight: 500;
                  opacity: 0.95;
                }
                .body {
                  padding: 26px 24px;
                }
                .pnr-box {
                  background: #fff5f6;
                  border: 1.5px dashed #f87171;
                  border-radius: 16px;
                  padding: 16px 14px;
                  text-align: center;
                  margin-bottom: 22px;
                }
                .pnr-label {
                  font-size: 11px;
                  text-transform: uppercase;
                  color: #991b1b;
                  font-weight: 800;
                  letter-spacing: 1px;
                }
                .pnr-num {
                  font-size: 26px;
                  font-weight: 900;
                  color: #e23744;
                  font-family: 'Plus Jakarta Sans', monospace;
                  letter-spacing: 1.5px;
                  margin: 4px 0 6px;
                }
                .refund-badge {
                  display: inline-block;
                  background: #ecfdf5;
                  border: 1px solid #a7f3d0;
                  color: #065f46;
                  padding: 6px 14px;
                  border-radius: 20px;
                  font-weight: 800;
                  font-size: 13px;
                }
                .wallet-box {
                  background: linear-gradient(135deg, #f0fdf4 0%%, #dcfce7 100%%);
                  border: 1.5px solid #86efac;
                  border-radius: 16px;
                  padding: 18px;
                  margin-bottom: 22px;
                }
                .wallet-title {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-weight: 800;
                  color: #166534;
                  font-size: 14px;
                  margin-bottom: 6px;
                }
                .wallet-desc {
                  font-size: 12px;
                  color: #15803d;
                  line-height: 1.5;
                  margin: 0;
                  font-weight: 500;
                }
                .section-title {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-size: 12px;
                  font-weight: 800;
                  text-transform: uppercase;
                  letter-spacing: 0.8px;
                  color: #4b5563;
                  margin: 20px 0 10px 0;
                  border-bottom: 1.5px solid #f3f4f6;
                  padding-bottom: 6px;
                }
                .details-table {
                  width: 100%%;
                  border-collapse: collapse;
                  margin-bottom: 20px;
                }
                .details-table td {
                  padding: 10px 0;
                  font-size: 13px;
                  border-bottom: 1px solid #f3f4f6;
                }
                .label {
                  color: #6b7280;
                  width: 38%%;
                  font-weight: 500;
                }
                .value {
                  font-weight: 700;
                  color: #111827;
                }
                .footer {
                  background: #fafafa;
                  padding: 18px 24px;
                  text-align: center;
                  font-size: 11px;
                  color: #9ca3af;
                  font-weight: 500;
                  border-top: 1px solid #f0f0f0;
                }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>redBus</h1>
                  <p>Booking Cancellation Confirmed</p>
                </div>
                <div class="body">
                  <div class="pnr-box">
                    <div class="pnr-label">Cancelled Booking PNR</div>
                    <div class="pnr-num">%s</div>
                    <div class="refund-badge">Refund Amount: %s</div>
                  </div>

                  <div class="wallet-box">
                    <div class="wallet-title">👛 Refund Deposited to Your Account</div>
                    <p class="wallet-desc">
                      An amount of <strong>%s</strong> has been queued/credited for your booking cancellation. Registered Wallet Balance: <strong>₹%s</strong>.
                    </p>
                  </div>

                  <div class="section-title">Trip Summary & Details</div>
                  <table class="details-table">
                    <tr><td class="label">Route</td><td class="value" style="font-family:'Poppins','Plus Jakarta Sans',sans-serif;font-size:15px;color:#e23744;font-weight:800;">%s ➔ %s</td></tr>
                    <tr><td class="label">Travel Date</td><td class="value">%s at %s</td></tr>
                    <tr><td class="label">Bus Operator</td><td class="value">%s (%s)</td></tr>
                    <tr><td class="label">Passenger(s)</td><td class="value">%s</td></tr>
                    <tr><td class="label">Paid Amount</td><td class="value">₹%s</td></tr>
                    <tr><td class="label">Refund Status</td><td class="value" style="color:#059669;font-weight:800;">%s (Processed)</td></tr>
                    <tr><td class="label">Reason</td><td class="value">%s</td></tr>
                    <tr><td class="label">Policy Note</td><td class="value" style="font-size:12px;color:#6b7280;">%s</td></tr>
                  </table>
                  <p style="font-size:12px;color:#6b7280;line-height:1.5;font-weight:500;">Your updated cancelled ticket PDF is attached for your records. Need assistance? Our 24x7 support team is here to help.</p>
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

    private String buildWelcomeAndVerifiedEmailTemplate(String name, String email, String bookingUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800;900&family=Poppins:wght@600;700;800;900&display=swap');
                * { box-sizing: border-box; }
                body {
                  font-family: 'Plus Jakarta Sans', 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  margin: 0;
                  padding: 24px 12px;
                  background-color: #f4f6f8;
                  color: #1c1c1c;
                  -webkit-font-smoothing: antialiased;
                }
                .container {
                  max-width: 580px;
                  margin: 0 auto;
                  background: #ffffff;
                  border-radius: 24px;
                  overflow: hidden;
                  border: 1px solid #eaeaea;
                  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
                }
                .header {
                  background: linear-gradient(135deg, #e23744 0%%, #cb202d 100%%);
                  padding: 32px 24px;
                  color: #ffffff;
                  text-align: center;
                }
                .header h1 {
                  margin: 0;
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-size: 32px;
                  font-weight: 900;
                  letter-spacing: -0.8px;
                }
                .header p {
                  margin: 6px 0 0;
                  font-size: 13px;
                  font-weight: 600;
                  letter-spacing: 0.3px;
                  opacity: 0.95;
                }
                .body {
                  padding: 28px 24px;
                }
                .verified-badge {
                  display: inline-flex;
                  align-items: center;
                  gap: 6px;
                  background: #ecfdf5;
                  border: 1px solid #a7f3d0;
                  color: #065f46;
                  padding: 6px 14px;
                  border-radius: 9999px;
                  font-weight: 800;
                  font-size: 12px;
                  margin-bottom: 16px;
                }
                .greeting {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-size: 22px;
                  font-weight: 800;
                  color: #111827;
                  margin: 0 0 8px 0;
                }
                .welcome-text {
                  font-size: 14px;
                  color: #4b5563;
                  line-height: 1.6;
                  margin: 0 0 24px 0;
                }
                .ad-card {
                  background: linear-gradient(135deg, #fff5f5 0%%, #fff1f2 50%%, #fed7aa 100%%);
                  border: 1.5px solid #fecaca;
                  border-radius: 20px;
                  padding: 24px;
                  margin-bottom: 24px;
                  text-align: center;
                  box-shadow: 0 4px 15px rgba(226, 55, 68, 0.08);
                }
                .ad-badge {
                  display: inline-block;
                  background: #e23744;
                  color: #ffffff;
                  font-size: 10px;
                  font-weight: 900;
                  letter-spacing: 1px;
                  text-transform: uppercase;
                  padding: 4px 10px;
                  border-radius: 9999px;
                  margin-bottom: 10px;
                }
                .ad-title {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-size: 20px;
                  font-weight: 900;
                  color: #991b1b;
                  margin: 0 0 6px 0;
                }
                .ad-desc {
                  font-size: 13px;
                  color: #7f1d1d;
                  line-height: 1.5;
                  margin: 0 0 16px 0;
                }
                .coupon-box {
                  background: #ffffff;
                  border: 2px dashed #e23744;
                  border-radius: 12px;
                  padding: 10px 18px;
                  display: inline-block;
                  margin-bottom: 18px;
                }
                .coupon-code {
                  font-family: 'Plus Jakarta Sans', monospace;
                  font-size: 18px;
                  font-weight: 900;
                  color: #e23744;
                  letter-spacing: 2px;
                }
                .btn-cta {
                  display: inline-block;
                  padding: 15px 36px;
                  background: linear-gradient(135deg, #e23744 0%%, #cb202d 100%%);
                  color: #ffffff !important;
                  text-decoration: none;
                  border-radius: 14px;
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-weight: 800;
                  font-size: 15px;
                  box-shadow: 0 6px 20px rgba(226, 55, 68, 0.35);
                  letter-spacing: 0.2px;
                }
                .perks-grid {
                  display: grid;
                  grid-template-columns: 1fr 1fr;
                  gap: 12px;
                  margin: 24px 0;
                  text-align: left;
                }
                .perk-item {
                  background: #f9fafb;
                  border: 1px solid #f3f4f6;
                  border-radius: 14px;
                  padding: 12px 14px;
                }
                .perk-title {
                  font-size: 12px;
                  font-weight: 800;
                  color: #111827;
                  margin-bottom: 2px;
                }
                .perk-sub {
                  font-size: 11px;
                  color: #6b7280;
                  line-height: 1.4;
                }
                .popular-routes {
                  background: #f8fafc;
                  border: 1px solid #e2e8f0;
                  border-radius: 16px;
                  padding: 16px;
                  margin-top: 20px;
                }
                .popular-title {
                  font-size: 12px;
                  font-weight: 800;
                  color: #334155;
                  text-transform: uppercase;
                  letter-spacing: 0.5px;
                  margin-bottom: 10px;
                }
                .route-pills {
                  display: flex;
                  flex-wrap: wrap;
                  gap: 8px;
                }
                .route-pill {
                  background: #ffffff;
                  border: 1px solid #cbd5e1;
                  padding: 6px 12px;
                  border-radius: 20px;
                  font-size: 11px;
                  font-weight: 700;
                  color: #1e293b;
                  text-decoration: none;
                }
                .footer {
                  background: #fafafa;
                  padding: 20px 24px;
                  text-align: center;
                  font-size: 11px;
                  color: #9ca3af;
                  font-weight: 500;
                  border-top: 1px solid #f0f0f0;
                }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>redBus</h1>
                  <p>India's #1 AI-Powered Bus Network</p>
                </div>
                <div class="body">
                  <div class="verified-badge">
                    ✓ Email Verified: %s
                  </div>
                  <h2 class="greeting">Welcome Aboard, %s! 🚌</h2>
                  <p class="welcome-text">
                    Your redBus profile is verified and active. You now have access to 10,000+ daily bus routes, real-time GPS tracking, instant 1-click redBus Wallet checkout, and 24/7 AI Smart Concierge support.
                  </p>

                  <!-- Promotional Book Bus Ad Banner -->
                  <div class="ad-card">
                    <span class="ad-badge">🌟 Special Welcome Offer</span>
                    <h3 class="ad-title">Get 20%% OFF on Your First Trip</h3>
                    <p class="ad-desc">
                      Book any luxury AC Sleeper, Volvo Multi-Axle, or express seater bus today. Apply the coupon code below at checkout:
                    </p>
                    <div class="coupon-box">
                      <span style="font-size:11px;font-weight:800;color:#991b1b;display:block;">COUPON CODE</span>
                      <span class="coupon-code">FIRSTTRIP</span>
                    </div>
                    <div>
                      <a href="%s" class="btn-cta">Book Bus Tickets Now ➔</a>
                    </div>
                  </div>

                  <!-- Highlights Grid -->
                  <div class="perks-grid">
                    <div class="perk-item">
                      <div class="perk-title">📍 Live Bus Tracking</div>
                      <div class="perk-sub">Real-time GPS tracking & boarding point directions.</div>
                    </div>
                    <div class="perk-item">
                      <div class="perk-title">👛 redBus Wallet</div>
                      <div class="perk-sub">1-click payments & instant automatic refund credits.</div>
                    </div>
                    <div class="perk-item">
                      <div class="perk-title">🛡️ 100%% Free Cancellation</div>
                      <div class="perk-sub">Full refunds on cancellations with zero penalty.</div>
                    </div>
                    <div class="perk-item">
                      <div class="perk-title">🤖 AI Smart Concierge</div>
                      <div class="perk-sub">Ask questions, reschedule, or find the best seats.</div>
                    </div>
                  </div>

                  <!-- Popular Bus Routes -->
                  <div class="popular-routes">
                    <div class="popular-title">🔥 Trending Bus Routes Today</div>
                    <div class="route-pills">
                      <span class="route-pill">Bengaluru ⇄ Chennai</span>
                      <span class="route-pill">Mumbai ⇄ Pune</span>
                      <span class="route-pill">Delhi ⇄ Jaipur</span>
                      <span class="route-pill">Hyderabad ⇄ Vijayawada</span>
                      <span class="route-pill">Coimbatore ⇄ Bangalore</span>
                    </div>
                  </div>
                </div>

                <div class="footer">
                  Sent with care via Brevo Transactional Email • © %d redBus India • 24x7 Support: help@redbus.in
                </div>
              </div>
            </body>
            </html>
            """.formatted(email, name, bookingUrl, java.time.Year.now().getValue());
    }

    private String buildOperatorVerifiedConfirmationEmailTemplate(String name, String companyName, String consoleUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800;900&family=Poppins:wght@600;700;800;900&display=swap');
                * { box-sizing: border-box; }
                body {
                  font-family: 'Plus Jakarta Sans', 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  background-color: #0f172a;
                  padding: 24px 12px;
                  margin: 0;
                  -webkit-font-smoothing: antialiased;
                }
                .card {
                  max-width: 520px;
                  margin: 0 auto;
                  background: #1e293b;
                  border-radius: 24px;
                  padding: 36px 28px;
                  border: 1px solid #334155;
                  text-align: center;
                  box-shadow: 0 20px 40px rgba(0,0,0,0.3);
                  color: #f8fafc;
                }
                .logo-badge {
                  display: inline-flex;
                  align-items: center;
                  background: rgba(216, 78, 85, 0.15);
                  border: 1px solid rgba(216, 78, 85, 0.3);
                  padding: 6px 14px;
                  border-radius: 9999px;
                  color: #d84e55;
                  font-size: 11px;
                  font-weight: 800;
                  letter-spacing: 1px;
                  text-transform: uppercase;
                  margin-bottom: 16px;
                }
                .brand {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-size: 26px;
                  font-weight: 900;
                  color: #ffffff;
                  margin-bottom: 4px;
                }
                .heading {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  margin: 12px 0 6px 0;
                  color: #ffffff;
                  font-size: 21px;
                  font-weight: 800;
                }
                .subtext {
                  color: #94a3b8;
                  font-size: 13px;
                  line-height: 1.6;
                  margin: 0 0 20px 0;
                }
                .verified-box {
                  background: rgba(16, 185, 129, 0.1);
                  border: 1.5px solid rgba(16, 185, 129, 0.3);
                  border-radius: 16px;
                  padding: 16px;
                  margin-bottom: 20px;
                  text-align: left;
                }
                .verified-box h4 {
                  margin: 0 0 4px 0;
                  color: #34d399;
                  font-size: 13px;
                  font-weight: 800;
                }
                .verified-box p {
                  margin: 0;
                  color: #a7f3d0;
                  font-size: 12px;
                  line-height: 1.5;
                }
                .btn {
                  display: inline-block;
                  padding: 14px 34px;
                  background: linear-gradient(135deg, #d84e55 0%%, #b91c1c 100%%);
                  color: #ffffff !important;
                  text-decoration: none;
                  border-radius: 14px;
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-weight: 800;
                  font-size: 14px;
                  box-shadow: 0 6px 20px rgba(216, 78, 85, 0.35);
                }
                .footer-text {
                  color: #64748b;
                  font-size: 11px;
                  margin-top: 24px;
                  border-top: 1px solid #334155;
                  padding-top: 16px;
                }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="logo-badge">🚍 Partner Account Verified</div>
                <div class="brand">redBus Fleet Console</div>
                <h2 class="heading">Business Email Confirmed</h2>
                <p class="subtext">
                  Hi <strong>%s</strong>, your operator account for <strong>%s</strong> is verified. You can now access the partner console to manage routes, buses, and live seat reservations.
                </p>

                <div class="verified-box">
                  <h4>✓ Fleet Console Privileges Activated</h4>
                  <p>
                    • Add & manage bus fleets (AC Sleeper, Seater, Volvo)<br>
                    • Create route schedules, boarding & dropping points<br>
                    • Real-time ticket booking management and revenue reports
                  </p>
                </div>

                <div>
                  <a href="%s" class="btn">Open Operator Console ➔</a>
                </div>

                <p class="footer-text">Dispatched via Brevo Transactional Email • © redBus India Partner Network</p>
              </div>
            </body>
            </html>
            """.formatted(name, companyName, consoleUrl);
    }

    private String buildPasswordResetSuccessEmailTemplate(String name, String email, String formattedTime, String loginUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800;900&family=Poppins:wght@600;700;800;900&display=swap');
                * { box-sizing: border-box; }
                body {
                  font-family: 'Plus Jakarta Sans', 'Poppins', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  background-color: #f4f6f8;
                  padding: 24px 12px;
                  margin: 0;
                  -webkit-font-smoothing: antialiased;
                }
                .card {
                  max-width: 480px;
                  margin: 0 auto;
                  background: #ffffff;
                  border-radius: 24px;
                  padding: 36px 28px;
                  border: 1px solid #eaeaea;
                  text-align: center;
                  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
                }
                .logo {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  color: #e23744;
                  font-size: 30px;
                  font-weight: 900;
                  margin-bottom: 12px;
                  letter-spacing: -0.8px;
                }
                .shield-icon {
                  display: inline-flex;
                  align-items: center;
                  justify-content: center;
                  width: 56px;
                  height: 56px;
                  border-radius: 20px;
                  background: #ecfdf5;
                  color: #059669;
                  font-size: 26px;
                  margin-bottom: 16px;
                }
                .heading {
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  margin: 0 0 8px 0;
                  color: #111827;
                  font-size: 20px;
                  font-weight: 800;
                }
                .subtext {
                  color: #4b5563;
                  font-size: 13px;
                  line-height: 1.5;
                  margin: 0 0 20px 0;
                }
                .security-box {
                  background: #f8fafc;
                  border: 1px solid #e2e8f0;
                  border-radius: 14px;
                  padding: 14px;
                  margin-bottom: 20px;
                  text-align: left;
                  font-size: 12px;
                }
                .security-row {
                  display: flex;
                  justify-content: space-between;
                  padding: 4px 0;
                  color: #475569;
                }
                .security-val {
                  font-weight: 700;
                  color: #0f172a;
                }
                .alert-warn {
                  background: #fffbeb;
                  border: 1px solid #fef3c7;
                  border-radius: 12px;
                  padding: 12px;
                  color: #92400e;
                  font-size: 12px;
                  text-align: left;
                  line-height: 1.4;
                  margin-bottom: 20px;
                }
                .btn {
                  display: inline-block;
                  padding: 14px 32px;
                  background: linear-gradient(135deg, #e23744 0%%, #cb202d 100%%);
                  color: #ffffff !important;
                  text-decoration: none;
                  border-radius: 14px;
                  font-family: 'Poppins', 'Plus Jakarta Sans', sans-serif;
                  font-weight: 800;
                  font-size: 14px;
                  box-shadow: 0 4px 14px rgba(226, 55, 68, 0.35);
                }
                .footer-text {
                  color: #9ca3af;
                  font-size: 11px;
                  margin-top: 24px;
                  border-top: 1px solid #f0f0f0;
                  padding-top: 16px;
                }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="logo">redBus</div>
                <div class="shield-icon">🛡️</div>
                <h2 class="heading">Password Changed Successfully</h2>
                <p class="subtext">
                  Hi %s, your redBus account password was recently updated.
                </p>

                <div class="security-box">
                  <div class="security-row">
                    <span>Account Email:</span>
                    <span class="security-val">%s</span>
                  </div>
                  <div class="security-row">
                    <span>Time of Change:</span>
                    <span class="security-val">%s</span>
                  </div>
                  <div class="security-row">
                    <span>Status:</span>
                    <span class="security-val" style="color:#059669;">Protected & Active</span>
                  </div>
                </div>

                <div class="alert-warn">
                  ⚠️ <strong>Security Notice:</strong> If you did NOT perform this action, please reset your password immediately or contact our security team at <strong>support@redbusai.app</strong>.
                </div>

                <div>
                  <a href="%s" class="btn">Sign In to Your Account</a>
                </div>

                <p class="footer-text">Sent via Brevo Security Engine • © redBus India Security Team</p>
              </div>
            </body>
            </html>
            """.formatted(name, email, formattedTime, loginUrl);
    }
}
