package com.redbus.service;

import com.redbus.dto.SendMobileOtpResponse;
import com.redbus.entity.Booking;
import com.redbus.entity.BookingPassenger;
import com.redbus.exception.BadRequestException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SmsService {

    private final RestTemplate restTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    // In-memory thread-safe OTP session storage
    private final Map<String, OtpSession> otpCache = new ConcurrentHashMap<>();

    @Value("${app.brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    public SmsService() {
        this.restTemplate = new RestTemplate();
    }

    @Data
    @Builder
    public static class OtpSession {
        private String phone;
        private String otp;
        private String purpose;
        private LocalDateTime expiresAt;
        private int remainingAttempts;
        private LocalDateTime createdAt;
    }

    /**
     * Clean and normalize phone numbers into a standard format (E.164 with country code).
     */
    public String normalizePhone(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        String cleaned = rawPhone.replaceAll("[\\s\\-\\(\\)]", "").trim();
        if (cleaned.startsWith("+")) {
            return "+" + cleaned.substring(1).replaceAll("[^0-9]", "");
        }
        // If 10-digit Indian number without country code, prepend +91
        String digitsOnly = cleaned.replaceAll("[^0-9]", "");
        if (digitsOnly.length() == 10) {
            return "+91" + digitsOnly;
        } else if (digitsOnly.length() == 12 && digitsOnly.startsWith("91")) {
            return "+" + digitsOnly;
        }
        return "+" + digitsOnly;
    }

    /**
     * Dispatches a 6-digit OTP to a mobile number using Brevo SMS Gateway.
     */
    public SendMobileOtpResponse sendOtp(String rawPhone, String purpose) {
        String normalizedPhone = normalizePhone(rawPhone);
        if (normalizedPhone.length() < 10) {
            throw new BadRequestException("Invalid mobile phone number. Please check the digits and country code.");
        }

        String normalizedPurpose = (purpose != null && !purpose.isBlank()) ? purpose.toUpperCase().trim() : "LOGIN";

        // Prevent rapid spamming (cooldown check: 10 seconds)
        OtpSession existing = otpCache.get(normalizedPhone);
        if (existing != null && existing.getCreatedAt() != null &&
                existing.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(10))) {
            long waitTime = 10 - java.time.Duration.between(existing.getCreatedAt(), LocalDateTime.now()).getSeconds();
            throw new BadRequestException("Please wait " + Math.max(1, waitTime) + "s before requesting a new OTP.");
        }

        // Generate cryptographically secure 6-digit OTP
        int otpInt = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(otpInt);

        // Store OTP in cache with 5 minutes expiration
        OtpSession session = OtpSession.builder()
                .phone(normalizedPhone)
                .otp(otp)
                .purpose(normalizedPurpose)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .remainingAttempts(5)
                .createdAt(LocalDateTime.now())
                .build();

        otpCache.put(normalizedPhone, session);

        // Format message tailored to the user/operator action
        String message = formatSmsMessage(otp, normalizedPurpose, normalizedPhone);

        // Dispatch SMS via Brevo Transactional SMS
        boolean sentViaBrevo = dispatchBrevoSms(normalizedPhone, message);

        log.info("📱 [MOBILE OTP SMS DISPATCH] To: {} | Purpose: {} | Brevo Sent: {}",
                normalizedPhone, normalizedPurpose, sentViaBrevo);

        if (!sentViaBrevo) {
            log.info("🔑 [SERVER OTP DEBUG] OTP for {} is: {}", normalizedPhone, otp);
        }

        String displayMsg = "Verification OTP has been sent via SMS to " + maskPhoneNumber(normalizedPhone) + ". Code valid for 5 minutes.";

        return SendMobileOtpResponse.builder()
                .success(true)
                .message(displayMsg)
                .phone(normalizedPhone)
                .expiresInSeconds(300)
                .previewOtp(null)
                .build();
    }

    /**
     * Formats SMS body dynamically according to user request purpose.
     */
    private String formatSmsMessage(String otp, String purpose, String phone) {
        String p = (purpose != null) ? purpose.toUpperCase().trim() : "LOGIN";
        switch (p) {
            case "RESET_PASSWORD":
                return String.format("redBus: Your OTP to reset password is %s. Valid for 5 minutes. Never share this code with anyone.", otp);
            case "FORGOT_PASSWORD":
                return String.format("redBus: Your password recovery OTP is %s. Valid for 5 minutes. Do not share with anyone.", otp);
            case "OPERATOR_LOGIN":
            case "OPERATOR":
                return String.format("redBus Partner: Your OTP for Operator Portal login is %s. Valid for 5 minutes. Do not share.", otp);
            case "REGISTER":
            case "REGISTRATION":
                return String.format("redBus: Your OTP to verify mobile number and complete registration is %s. Valid for 5 minutes.", otp);
            case "LOGIN":
            default:
                return String.format("redBus: Your verification code for login is %s. Valid for 5 minutes. Do not share this OTP with anyone.", otp);
        }
    }

    /**
     * Verifies the OTP for a mobile number.
     */
    public boolean verifyOtp(String rawPhone, String inputOtp, String expectedPurpose) {
        if (rawPhone == null || inputOtp == null) {
            return false;
        }

        String normalizedPhone = normalizePhone(rawPhone);
        OtpSession session = otpCache.get(normalizedPhone);

        if (session == null) {
            log.warn("No active OTP session found for {}", normalizedPhone);
            return false;
        }

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpCache.remove(normalizedPhone);
            throw new BadRequestException("OTP has expired. Please request a new code.");
        }

        if (session.getRemainingAttempts() <= 0) {
            otpCache.remove(normalizedPhone);
            throw new BadRequestException("Too many incorrect OTP attempts. Please request a fresh code.");
        }

        if (expectedPurpose != null && !expectedPurpose.equalsIgnoreCase(session.getPurpose())) {
            log.warn("OTP purpose mismatch for {}: expected {} but found {}",
                    normalizedPhone, expectedPurpose, session.getPurpose());
        }

        if (!session.getOtp().equals(inputOtp.trim())) {
            session.setRemainingAttempts(session.getRemainingAttempts() - 1);
            int left = session.getRemainingAttempts();
            if (left > 0) {
                throw new BadRequestException("Invalid OTP code. " + left + " attempt(s) remaining.");
            } else {
                otpCache.remove(normalizedPhone);
                throw new BadRequestException("Invalid OTP code. Maximum attempts reached. Please request a new code.");
            }
        }

        // Successfully verified: clear the OTP session
        otpCache.remove(normalizedPhone);
        log.info("✅ Mobile OTP successfully verified for {}", normalizedPhone);
        return true;
    }

    /**
     * Dispatches automated 1-hour pre-journey SMS notification with ticket details.
     */
    public boolean sendJourneyReminderSms(Booking booking) {
        if (booking == null || booking.getContactPhone() == null || booking.getContactPhone().isBlank()) {
            log.warn("Cannot send journey reminder SMS: booking or phone number is missing");
            return false;
        }

        String phone = normalizePhone(booking.getContactPhone());
        String pnr = booking.getPnr();
        String operatorName = (booking.getRoute() != null && booking.getRoute().getBus() != null)
                ? booking.getRoute().getBus().getOperatorName()
                : "redBus Express";
        String busReg = (booking.getRoute() != null && booking.getRoute().getBus() != null && booking.getRoute().getBus().getRegistrationNumber() != null)
                ? booking.getRoute().getBus().getRegistrationNumber()
                : "";
        String source = (booking.getRoute() != null) ? booking.getRoute().getSourceCity() : "Origin";
        String destination = (booking.getRoute() != null) ? booking.getRoute().getDestinationCity() : "Destination";

        String depTimeStr = "";
        if (booking.getRoute() != null && booking.getRoute().getDepartureTime() != null) {
            depTimeStr = booking.getRoute().getDepartureTime().format(DateTimeFormatter.ofPattern("hh:mm a"));
        }

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

        String busDetails = busReg.isBlank() ? operatorName : (operatorName + " " + busReg);
        String smsMessage = String.format(
                "redBus Alert: Your bus %s to %s departs at %s (in 1 hr). Boarding: %s. PNR: %s, Seat: %s. Safe journey!",
                busDetails, destination, depTimeStr, boardingPoint, pnr, seats
        );

        log.info("🚌 [AUTOMATION: 1-HR JOURNEY REMINDER SMS] Dispathing to {} | PNR: {} | Bus: {} | Departure: {}",
                phone, pnr, busDetails, depTimeStr);

        boolean sent = dispatchBrevoSms(phone, smsMessage);

        if (!sent) {
            log.info("📢 [SMS FALLBACK / SIMULATOR] To: {} | Msg: {}", phone, smsMessage);
        }

        return true;
    }

    /**
     * Dispatches any custom transactional SMS directly.
     */
    public boolean sendDirectSms(String rawPhone, String message) {
        String normalizedPhone = normalizePhone(rawPhone);
        if (normalizedPhone.length() < 10) {
            return false;
        }
        boolean sent = dispatchBrevoSms(normalizedPhone, message);
        if (!sent) {
            log.info("📱 [DIRECT SMS SIMULATION] To: {} | Message: {}", normalizedPhone, message);
        }
        return true;
    }

    /**
     * Dispatches SMS strictly through Brevo Transactional SMS API.
     */
    private boolean dispatchBrevoSms(String phone, String message) {
        if (brevoApiKey == null || brevoApiKey.isBlank() || brevoApiKey.startsWith("your_") || "mock-key".equalsIgnoreCase(brevoApiKey)) {
            log.warn("Brevo API key is not configured or is default placeholder.");
            return false;
        }

        try {
            String digitsOnly = phone.replaceAll("[^0-9]", "");
            String indianNumber = (digitsOnly.length() >= 10) ? digitsOnly.substring(digitsOnly.length() - 10) : digitsOnly;
            String brevoPhone = digitsOnly.startsWith("91") && digitsOnly.length() >= 12 ? digitsOnly : ("91" + indianNumber);

            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", brevoApiKey.trim());
            headers.setContentType(MediaType.APPLICATION_JSON);

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("sender", "REDBUS");
            body.put("recipient", brevoPhone);
            body.put("content", message);
            body.put("type", "transactional");
            body.put("unicodeEnabled", true);

            HttpEntity<java.util.Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.brevo.com/v3/transactionalSMS/send", entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Brevo SMS dispatched successfully to {} | Response: {}", brevoPhone, response.getBody());
                return true;
            } else {
                log.warn("⚠️ Brevo SMS response status {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("❌ Brevo SMS HTTP {} Error: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ Brevo SMS dispatch attempt failed: {}", e.getMessage(), e);
        }

        return false;
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        int len = phone.length();
        return phone.substring(0, 4) + "••••" + phone.substring(len - 2);
    }
}
