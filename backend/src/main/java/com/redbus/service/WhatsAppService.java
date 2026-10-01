package com.redbus.service;

import com.redbus.entity.Booking;
import com.redbus.entity.BookingPassenger;
import com.redbus.entity.Route;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WhatsAppService {

    private final RestTemplate restTemplate;

    @Value("${app.whatsapp.api-token:${WHATSAPP_API_TOKEN:}}")
    private String whatsappApiToken;

    @Value("${app.whatsapp.phone-number-id:${WHATSAPP_PHONE_NUMBER_ID:}}")
    private String whatsappPhoneNumberId;

    @Value("${app.whatsapp.business-account-id:${WHATSAPP_BUSINESS_ACCOUNT_ID:}}")
    private String whatsappBusinessAccountId;

    @Value("${app.whatsapp.enabled:true}")
    private boolean isWhatsAppEnabled;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendBaseUrl;

    public WhatsAppService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Clean and format phone number for WhatsApp Cloud API (e.g., "918939129572").
     */
    public String normalizeWhatsAppPhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return "";
        }
        String digits = rawPhone.replaceAll("[^0-9]", "").trim();
        if (digits.length() == 10) {
            return "91" + digits;
        } else if (digits.length() == 12 && digits.startsWith("91")) {
            return digits;
        } else if (digits.startsWith("0") && digits.length() == 11) {
            return "91" + digits.substring(1);
        }
        return digits;
    }

    /**
     * 1. Automated 1-Hour Journey Departure Reminder via WhatsApp.
     */
    public boolean sendJourneyReminder(Booking booking) {
        if (!isWhatsAppEnabled || booking == null || booking.getContactPhone() == null) {
            return false;
        }

        String recipientPhone = normalizeWhatsAppPhone(booking.getContactPhone());
        if (recipientPhone.length() < 10) {
            log.warn("Cannot send WhatsApp journey reminder: invalid phone {}", booking.getContactPhone());
            return false;
        }

        Route route = booking.getRoute();
        String operatorName = (route != null && route.getBus() != null)
                ? route.getBus().getOperatorName()
                : "redBus Express";
        String busNumber = (route != null && route.getBus() != null && route.getBus().getRegistrationNumber() != null)
                ? route.getBus().getRegistrationNumber()
                : "";
        String source = (route != null) ? route.getSourceCity() : "Origin";
        String destination = (route != null) ? route.getDestinationCity() : "Destination";
        String depTime = (route != null && route.getDepartureTime() != null)
                ? route.getDepartureTime().format(DateTimeFormatter.ofPattern("hh:mm a"))
                : "Scheduled Time";
        String travelDateStr = (route != null && route.getTravelDate() != null)
                ? route.getTravelDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "Today";

        String boardingPoint = (booking.getBoardingPoint() != null && !booking.getBoardingPoint().isBlank())
                ? booking.getBoardingPoint()
                : source;

        String seats = "";
        if (booking.getPassengers() != null && !booking.getPassengers().isEmpty()) {
            seats = booking.getPassengers().stream()
                    .map(BookingPassenger::getSeatNumber)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(", "));
        }

        String trackingUrl = frontendBaseUrl + "/tracking?pnr=" + booking.getPnr();
        String ticketPdfUrl = frontendBaseUrl + "/api/v1/bookings/" + booking.getPnr() + "/ticket-pdf";

        StringBuilder sb = new StringBuilder();
        sb.append("🚨 *redBus Journey Alert: 1 Hour to Departure!* ⏰\n\n");
        sb.append("Dear Traveler,\n");
        sb.append("Your bus is scheduled to depart in approximately *1 hour*. Please reach your boarding point on time.\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("📋 *Trip Summary*\n");
        sb.append("• *PNR:* `").append(booking.getPnr()).append("`\n");
        sb.append("• *Bus:* ").append(operatorName).append(busNumber.isBlank() ? "" : " (" + busNumber + ")").append("\n");
        sb.append("• *Route:* ").append(source).append(" ➔ ").append(destination).append("\n");
        sb.append("• *Date & Time:* ").append(travelDateStr).append(" at *").append(depTime).append("*\n");
        sb.append("• *Boarding Point:* ").append(boardingPoint).append("\n");
        sb.append("• *Seat(s):* ").append(seats.isBlank() ? "Confirmed" : seats).append("\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("📍 *Live GPS Tracking:*\n").append(trackingUrl).append("\n\n");
        sb.append("📄 *Download E-Ticket PDF:*\n").append(ticketPdfUrl).append("\n\n");
        sb.append("Have a safe and pleasant journey! 🚌✨\n");
        sb.append("_redBus Customer Care: 1800-102-1234_");

        log.info("📲 [WHATSAPP API: 1-HR JOURNEY REMINDER] Sending to {} | PNR: {}", recipientPhone, booking.getPnr());
        return sendTextMessage(recipientPhone, sb.toString());
    }

    /**
     * 2. Ticket Booking Confirmation Notification with PDF Link via WhatsApp.
     */
    public boolean sendBookingConfirmation(Booking booking, String customPdfUrl) {
        if (!isWhatsAppEnabled || booking == null || booking.getContactPhone() == null) {
            return false;
        }

        String recipientPhone = normalizeWhatsAppPhone(booking.getContactPhone());
        if (recipientPhone.length() < 10) {
            return false;
        }

        Route route = booking.getRoute();
        String operatorName = (route != null && route.getBus() != null)
                ? route.getBus().getOperatorName()
                : "redBus Partner";
        String busType = (route != null && route.getBus() != null)
                ? route.getBus().getBusType()
                : "AC Sleeper";
        String source = (route != null) ? route.getSourceCity() : "Origin";
        String destination = (route != null) ? route.getDestinationCity() : "Destination";
        String depTime = (route != null && route.getDepartureTime() != null)
                ? route.getDepartureTime().format(DateTimeFormatter.ofPattern("hh:mm a"))
                : "Scheduled Time";
        String travelDateStr = (route != null && route.getTravelDate() != null)
                ? route.getTravelDate().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))
                : "Journey Date";

        String boardingPoint = (booking.getBoardingPoint() != null && !booking.getBoardingPoint().isBlank())
                ? booking.getBoardingPoint()
                : source;
        String droppingPoint = (booking.getDroppingPoint() != null && !booking.getDroppingPoint().isBlank())
                ? booking.getDroppingPoint()
                : destination;

        String seats = "";
        if (booking.getPassengers() != null && !booking.getPassengers().isEmpty()) {
            seats = booking.getPassengers().stream()
                    .map(BookingPassenger::getSeatNumber)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(", "));
        }

        String pdfUrl = (customPdfUrl != null && !customPdfUrl.isBlank())
                ? customPdfUrl
                : frontendBaseUrl + "/api/v1/bookings/" + booking.getPnr() + "/ticket-pdf";
        String trackingUrl = frontendBaseUrl + "/tracking?pnr=" + booking.getPnr();
        String manageBookingUrl = frontendBaseUrl + "/my-bookings";

        StringBuilder sb = new StringBuilder();
        sb.append("🎉 *redBus: Booking Confirmed!* 🎫\n\n");
        sb.append("Dear Traveler,\n");
        sb.append("Thank you for booking with redBus. Your e-ticket has been successfully generated!\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🏷️ *PNR:* `").append(booking.getPnr()).append("`\n");
        sb.append("🚌 *Operator:* ").append(operatorName).append(" (").append(busType).append(")\n");
        sb.append("🛣️ *Route:* ").append(source).append(" ➔ ").append(destination).append("\n");
        sb.append("📅 *Travel Date:* ").append(travelDateStr).append("\n");
        sb.append("⏰ *Departure:* ").append(depTime).append("\n");
        sb.append("📍 *Boarding:* ").append(boardingPoint).append("\n");
        sb.append("🏁 *Dropping:* ").append(droppingPoint).append("\n");
        sb.append("💺 *Seat(s):* ").append(seats.isBlank() ? "Confirmed" : seats).append("\n");
        sb.append("💰 *Total Fare Paid:* ₹").append(booking.getTotalAmount() != null ? booking.getTotalAmount() : "0.00").append("\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("📄 *Download Official E-Ticket PDF:*\n").append(pdfUrl).append("\n\n");
        sb.append("📍 *Live GPS Bus Tracking:*\n").append(trackingUrl).append("\n\n");
        sb.append("📱 *Manage Booking / Cancel Ticket:*\n").append(manageBookingUrl).append("\n\n");
        sb.append("Wish you a pleasant trip! 🌟\n");
        sb.append("_Need help? Reply HELP or visit redbus.in/support_");

        log.info("📲 [WHATSAPP API: BOOKING CONFIRMATION] Sending to {} | PNR: {}", recipientPhone, booking.getPnr());
        return sendTextMessage(recipientPhone, sb.toString());
    }

    /**
     * 3. Ticket Cancellation Notification with Refund Details & PDF via WhatsApp.
     */
    public boolean sendBookingCancellation(Booking booking, BigDecimal refundAmount, String destination) {
        if (!isWhatsAppEnabled || booking == null || booking.getContactPhone() == null) {
            return false;
        }

        String recipientPhone = normalizeWhatsAppPhone(booking.getContactPhone());
        if (recipientPhone.length() < 10) {
            return false;
        }

        Route route = booking.getRoute();
        String source = (route != null) ? route.getSourceCity() : "Origin";
        String dest = (destination != null && !destination.isBlank())
                ? destination
                : ((route != null) ? route.getDestinationCity() : "Destination");
        String pnr = booking.getPnr();
        BigDecimal refund = (refundAmount != null) ? refundAmount : BigDecimal.ZERO;

        String pdfUrl = frontendBaseUrl + "/api/v1/bookings/" + pnr + "/ticket-pdf";

        StringBuilder sb = new StringBuilder();
        sb.append("❌ *redBus: Ticket Cancellation & Refund Confirmed*\n\n");
        sb.append("Dear Traveler,\n");
        sb.append("Your ticket under PNR `").append(pnr).append("` for journey *").append(source).append(" ➔ ").append(dest).append("* has been successfully cancelled.\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("💵 *Refund Amount:* ₹").append(refund).append("\n");
        sb.append("💳 *Refund Mode:* Original Payment Method / redBus Wallet\n");
        sb.append("⏳ *Status:* Processed (credited within 1-3 business days)\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("📄 *Download Updated Cancellation Receipt PDF:*\n").append(pdfUrl).append("\n\n");
        sb.append("For any assistance, please reach out to 24/7 redBus Support.\n");
        sb.append("_Thank you for choosing redBus._");

        log.info("📲 [WHATSAPP API: CANCELLATION NOTIFICATION] Sending to {} | PNR: {} | Refund: ₹{}", recipientPhone, pnr, refund);
        return sendTextMessage(recipientPhone, sb.toString());
    }

    /**
     * 4. Password Reset OTP / Security Notification via WhatsApp.
     */
    public boolean sendPasswordResetOtp(String rawPhone, String otp) {
        if (!isWhatsAppEnabled || rawPhone == null || rawPhone.isBlank() || otp == null) {
            return false;
        }

        String recipientPhone = normalizeWhatsAppPhone(rawPhone);
        if (recipientPhone.length() < 10) {
            return false;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🔒 *redBus Security: Password Reset Code*\n\n");
        sb.append("Your One-Time Password (OTP) to reset your redBus account password is:\n\n");
        sb.append("🔑 *").append(otp).append("*\n\n");
        sb.append("⏱️ This code is valid for *5 minutes*. For security reasons, please do not share this OTP with anyone.\n\n");
        sb.append("If you did not request this, please secure your account immediately.\n");
        sb.append("_redBus Security Team_");

        log.info("📲 [WHATSAPP API: PASSWORD RESET OTP] Sending to {}", recipientPhone);
        return sendTextMessage(recipientPhone, sb.toString());
    }

    /**
     * 5. Account Registration & Welcome Notification via WhatsApp.
     */
    public boolean sendWelcomeMessage(String rawPhone, String userName) {
        if (!isWhatsAppEnabled || rawPhone == null || rawPhone.isBlank()) {
            return false;
        }

        String recipientPhone = normalizeWhatsAppPhone(rawPhone);
        if (recipientPhone.length() < 10) {
            return false;
        }

        String displayName = (userName != null && !userName.isBlank()) ? userName.trim() : "Traveler";
        String exploreUrl = frontendBaseUrl + "/search";

        StringBuilder sb = new StringBuilder();
        sb.append("🎊 *Welcome to redBus, ").append(displayName).append("!* 🚌\n\n");
        sb.append("Your redBus account has been successfully created and verified.\n\n");
        sb.append("✨ *Why Book with redBus?*\n");
        sb.append("• 30,000+ routes across India\n");
        sb.append("• Primo top-rated buses with on-time guarantee\n");
        sb.append("• Live GPS bus tracking on WhatsApp & App\n");
        sb.append("• Instant refunds & Free cancellation options\n\n");
        sb.append("🔍 *Book your first trip today:*\n").append(exploreUrl).append("\n\n");
        sb.append("Happy Travelling! 🌟\n");
        sb.append("_The redBus Team_");

        log.info("📲 [WHATSAPP API: WELCOME NOTIFICATION] Sending to {}", recipientPhone);
        return sendTextMessage(recipientPhone, sb.toString());
    }

    /**
     * Generic text dispatcher via WhatsApp Cloud API (Meta Graph API).
     */
    public boolean sendTextMessage(String phone, String text) {
        String normalizedPhone = normalizeWhatsAppPhone(phone);
        if (normalizedPhone.length() < 10) {
            return false;
        }

        // If credentials are not configured, simulate smoothly in server logs
        if (whatsappApiToken == null || whatsappApiToken.isBlank() || whatsappPhoneNumberId == null || whatsappPhoneNumberId.isBlank() || "mock-token".equalsIgnoreCase(whatsappApiToken)) {
            log.info("🟢 [WHATSAPP SIMULATOR / CONSOLE DISPATCH]\nTo: {}\nMessage:\n{}\n", normalizedPhone, text);
            return true;
        }

        try {
            String url = String.format("https://graph.facebook.com/v18.0/%s/messages", whatsappPhoneNumberId.trim());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(whatsappApiToken.trim());
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> textBody = new HashMap<>();
            textBody.put("preview_url", true);
            textBody.put("body", text);

            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("recipient_type", "individual");
            payload.put("to", normalizedPhone);
            payload.put("type", "text");
            payload.put("text", textBody);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [WHATSAPP CLOUD API] Message sent successfully to {}. Response: {}", normalizedPhone, response.getBody());
                return true;
            } else {
                log.warn("⚠️ [WHATSAPP CLOUD API] Response status {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("❌ [WHATSAPP CLOUD API HTTP ERROR] Status: {} | Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ [WHATSAPP DISPATCH FAILED] Error: {}", e.getMessage(), e);
        }

        return false;
    }
}
