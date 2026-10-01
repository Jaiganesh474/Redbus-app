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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WhatsAppService {

    private final RestTemplate restTemplate;

    // Meta WhatsApp Cloud API Properties
    @Value("${app.whatsapp.api-token:${WHATSAPP_API_TOKEN:}}")
    private String whatsappApiToken;

    @Value("${app.whatsapp.phone-number-id:${WHATSAPP_PHONE_NUMBER_ID:}}")
    private String whatsappPhoneNumberId;

    @Value("${app.whatsapp.business-account-id:${WHATSAPP_BUSINESS_ACCOUNT_ID:}}")
    private String whatsappBusinessAccountId;

    // Twilio WhatsApp Properties (Instant Sandbox without Business Verification)
    @Value("${app.twilio.account-sid:${TWILIO_ACCOUNT_SID:}}")
    private String twilioAccountSid;

    @Value("${app.twilio.auth-token:${TWILIO_AUTH_TOKEN:}}")
    private String twilioAuthToken;

    @Value("${app.twilio.whatsapp-from:${TWILIO_WHATSAPP_FROM:whatsapp:+14155238886}}")
    private String twilioWhatsAppFrom;

    // UltraMsg Properties (Instant QR-based Gateway to ANY number worldwide)
    @Value("${app.ultramsg.instance-id:${ULTRAMSG_INSTANCE_ID:}}")
    private String ultramsgInstanceId;

    @Value("${app.ultramsg.token:${ULTRAMSG_TOKEN:}}")
    private String ultramsgToken;

    @Value("${app.whatsapp.enabled:true}")
    private boolean isWhatsAppEnabled;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendBaseUrl;

    public WhatsAppService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Clean and format phone number for WhatsApp (e.g., "918939129572").
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
        boolean textSent = sendTextMessage(recipientPhone, sb.toString());

        // Automatically dispatch PDF Document attachment into WhatsApp chat
        try {
            sendUltraMsgDocument(recipientPhone, ticketPdfUrl, "redBus_Ticket_" + booking.getPnr() + ".pdf", "📄 Official E-Ticket PDF: " + booking.getPnr());
        } catch (Exception ignored) {}

        return textSent;
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
        boolean textSent = sendTextMessage(recipientPhone, sb.toString());

        // Automatically dispatch PDF Document attachment into WhatsApp chat
        try {
            sendUltraMsgDocument(recipientPhone, pdfUrl, "redBus_Ticket_" + booking.getPnr() + ".pdf", "📄 Official E-Ticket PDF: " + booking.getPnr());
        } catch (Exception ignored) {}

        return textSent;
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
        boolean textSent = sendTextMessage(recipientPhone, sb.toString());

        // Automatically dispatch Updated Cancellation Receipt PDF Document into WhatsApp chat
        try {
            sendUltraMsgDocument(recipientPhone, pdfUrl, "redBus_Cancellation_" + pnr + ".pdf", "📄 Cancellation Receipt PDF: " + pnr);
        } catch (Exception ignored) {}

        return textSent;
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
     * Generic text dispatcher: Dispatches via Twilio WhatsApp API (if configured) or Meta Cloud API.
     */
    public boolean sendTextMessage(String phone, String text) {
        String normalizedPhone = normalizeWhatsAppPhone(phone);
        if (normalizedPhone.length() < 10) {
            return false;
        }

        // 1. Try UltraMsg QR-Gateway first (Dispatches to ANY number without business verification)
        if (ultramsgInstanceId != null && !ultramsgInstanceId.isBlank() && ultramsgToken != null && !ultramsgToken.isBlank()) {
            boolean ultraSent = sendViaUltraMsg(normalizedPhone, text);
            if (ultraSent) return true;
        }

        // 2. Try Twilio WhatsApp API (Sandbox)
        if (twilioAccountSid != null && !twilioAccountSid.isBlank() && twilioAuthToken != null && !twilioAuthToken.isBlank()) {
            boolean twilioSent = sendViaTwilioWhatsApp(normalizedPhone, text);
            if (twilioSent) return true;
        }

        // 3. Try Meta WhatsApp Cloud API (Graph API)
        if (whatsappApiToken != null && !whatsappApiToken.isBlank() && whatsappPhoneNumberId != null && !whatsappPhoneNumberId.isBlank() && !"mock-token".equalsIgnoreCase(whatsappApiToken)) {
            boolean metaSent = sendViaMetaGraphApi(normalizedPhone, text);
            if (metaSent) return true;
        }

        // 4. Fallback: Simulator console output
        log.info("🟢 [WHATSAPP SIMULATOR / CONSOLE DISPATCH]\nTo: {}\nMessage:\n{}\n", normalizedPhone, text);
        return true;
    }

    /**
     * Dispatch WhatsApp message using UltraMsg QR-Code Gateway (delivers to any number worldwide).
     */
    private boolean sendViaUltraMsg(String normalizedPhone, String bodyText) {
        try {
            String url = String.format("https://api.ultramsg.com/%s/messages/chat", ultramsgInstanceId.trim());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("token", ultramsgToken.trim());
            map.add("to", "+" + normalizedPhone);
            map.add("body", bodyText);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [ULTRAMSG WHATSAPP GATEWAY] Message sent to +{}. Response: {}", normalizedPhone, response.getBody());
                return true;
            } else {
                log.warn("⚠️ [ULTRAMSG WHATSAPP GATEWAY] Response status {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("❌ [ULTRAMSG HTTP ERROR] Status: {} | Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ [ULTRAMSG FAILED] Error: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Dispatch WhatsApp PDF Document attachment using UltraMsg.
     */
    public boolean sendUltraMsgDocument(String normalizedPhone, String documentUrl, String filename, String caption) {
        if (ultramsgInstanceId == null || ultramsgInstanceId.isBlank() || ultramsgToken == null || ultramsgToken.isBlank()) {
            return false;
        }

        try {
            String url = String.format("https://api.ultramsg.com/%s/messages/document", ultramsgInstanceId.trim());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("token", ultramsgToken.trim());
            map.add("to", "+" + normalizedPhone);
            map.add("filename", (filename != null && !filename.isBlank()) ? filename : "redBus_Ticket.pdf");
            map.add("document", documentUrl);
            if (caption != null && !caption.isBlank()) {
                map.add("caption", caption);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [ULTRAMSG PDF DOCUMENT ATTACHED] Sent to +{}. Response: {}", normalizedPhone, response.getBody());
                return true;
            } else {
                log.warn("⚠️ [ULTRAMSG PDF DOCUMENT ATTEMPT] Status {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.warn("UltraMsg PDF document dispatch note: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Dispatch WhatsApp message using Twilio WhatsApp API.
     */
    private boolean sendViaTwilioWhatsApp(String normalizedPhone, String bodyText) {
        try {
            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", twilioAccountSid.trim());

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(twilioAccountSid.trim(), twilioAuthToken.trim());
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String formattedTo = "whatsapp:+" + normalizedPhone;
            String formattedFrom = (twilioWhatsAppFrom != null && !twilioWhatsAppFrom.isBlank())
                    ? twilioWhatsAppFrom.trim()
                    : "whatsapp:+14155238886";

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("From", formattedFrom);
            map.add("To", formattedTo);
            map.add("Body", bodyText);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [TWILIO WHATSAPP API] Message successfully sent to {}. Response: {}", formattedTo, response.getBody());
                return true;
            } else {
                log.warn("⚠️ [TWILIO WHATSAPP API] Response status {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("❌ [TWILIO WHATSAPP HTTP ERROR] Status: {} | Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ [TWILIO WHATSAPP FAILED] Error: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Dispatch WhatsApp message using Meta WhatsApp Cloud API.
     */
    private boolean sendViaMetaGraphApi(String normalizedPhone, String text) {
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
            }
        } catch (Exception e) {
            log.error("❌ [WHATSAPP CLOUD API FAILED] Error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Send pre-approved Meta WhatsApp Template message.
     */
    public boolean sendTemplateMessage(String phone, String templateName, String languageCode) {
        String normalizedPhone = normalizeWhatsAppPhone(phone);
        if (normalizedPhone.length() < 10) {
            return false;
        }

        if (whatsappApiToken == null || whatsappApiToken.isBlank() || whatsappPhoneNumberId == null || whatsappPhoneNumberId.isBlank() || "mock-token".equalsIgnoreCase(whatsappApiToken)) {
            log.info("🟢 [WHATSAPP TEMPLATE SIMULATOR] To: {} | Template: {} | Lang: {}", normalizedPhone, templateName, languageCode);
            return true;
        }

        try {
            String url = String.format("https://graph.facebook.com/v18.0/%s/messages", whatsappPhoneNumberId.trim());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(whatsappApiToken.trim());
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> langMap = new HashMap<>();
            langMap.put("code", (languageCode != null && !languageCode.isBlank()) ? languageCode : "en_US");

            Map<String, Object> templateMap = new HashMap<>();
            templateMap.put("name", (templateName != null && !templateName.isBlank()) ? templateName : "hello_world");
            templateMap.put("language", langMap);

            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("recipient_type", "individual");
            payload.put("to", normalizedPhone);
            payload.put("type", "template");
            payload.put("template", templateMap);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [WHATSAPP CLOUD API TEMPLATE] Template '{}' sent successfully to {}. Response: {}", 
                        templateName, normalizedPhone, response.getBody());
                return true;
            }
        } catch (Exception e) {
            log.error("❌ [WHATSAPP TEMPLATE FAILED] Error: {}", e.getMessage());
        }

        return false;
    }
}
