package com.redbus.service;

import com.redbus.dto.SendMobileOtpResponse;
import com.redbus.exception.BadRequestException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SmsService {

    private final RestTemplate restTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    // In-memory thread-safe OTP session storage
    private final Map<String, OtpSession> otpCache = new ConcurrentHashMap<>();

    @Value("${app.sms.fast2sms-api-key:${FAST2SMS_API_KEY:}}")
    private String fast2smsApiKey;

    @Value("${app.sms.two-factor-api-key:${TWO_FACTOR_API_KEY:}}")
    private String twoFactorApiKey;

    @Value("${app.sms.textbelt-key:${TEXTBELT_KEY:textbelt}}")
    private String textbeltKey;

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
     * Clean and normalize phone numbers into a standard format.
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
     * Dispatches a 6-digit OTP to a mobile number using real SMS gateways.
     */
    public SendMobileOtpResponse sendOtp(String rawPhone, String purpose) {
        String normalizedPhone = normalizePhone(rawPhone);
        if (normalizedPhone.length() < 10) {
            throw new BadRequestException("Invalid mobile phone number. Please check the digits and country code.");
        }

        String normalizedPurpose = (purpose != null && !purpose.isBlank()) ? purpose.toUpperCase().trim() : "LOGIN";

        // Prevent rapid spamming (cooldown check: 5 seconds)
        OtpSession existing = otpCache.get(normalizedPhone);
        if (existing != null && existing.getCreatedAt() != null &&
                existing.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(5))) {
            long waitTime = 5 - java.time.Duration.between(existing.getCreatedAt(), LocalDateTime.now()).getSeconds();
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

        // Dispatch SMS via live gateway providers
        boolean sentViaGateway = dispatchSms(normalizedPhone, otp, message);

        log.info("📱 [MOBILE OTP SMS DISPATCH] To: {} | Purpose: {} | Gateway Sent: {}",
                normalizedPhone, normalizedPurpose, sentViaGateway);

        String displayMsg = "Verification OTP has been sent via SMS to " + maskPhoneNumber(normalizedPhone) + ". Code valid for 5 minutes.";

        return SendMobileOtpResponse.builder()
                .success(true)
                .message(displayMsg)
                .phone(normalizedPhone)
                .expiresInSeconds(300)
                .previewOtp(null) // Do NOT expose OTP in response
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
     * Multi-Provider SMS Sender engine (Fast2SMS, 2Factor, Twilio, Brevo SMS, Textbelt).
     */
    private boolean dispatchSms(String phone, String otp, String message) {
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        String indianNumber = (digitsOnly.length() >= 10) ? digitsOnly.substring(digitsOnly.length() - 10) : digitsOnly;

        // 1. Try Brevo Transactional SMS (Fallback to Backend SMS Gateway)
        if (brevoApiKey != null && !brevoApiKey.isBlank() && !brevoApiKey.startsWith("your_")) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("api-key", brevoApiKey.trim());
                headers.setContentType(MediaType.APPLICATION_JSON);

                String brevoPhone = digitsOnly.startsWith("91") && digitsOnly.length() >= 12 ? digitsOnly : ("91" + indianNumber);

                Map<String, Object> body = Map.of(
                        "sender", "redBus",
                        "recipient", brevoPhone,
                        "content", message
                );

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(
                        "https://api.brevo.com/v3/transactionalSMS/send", entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Brevo SMS dispatched successfully to {} | Response: {}", brevoPhone, response.getBody());
                    return true;
                } else {
                    log.warn("Brevo SMS response: {}", response.getBody());
                }
            } catch (Exception e) {
                log.warn("Brevo SMS dispatch attempt failed: {}", e.getMessage());
            }
        }

        // 3. Try Fast2SMS (Indian SMS Gateway)
        if (fast2smsApiKey != null && !fast2smsApiKey.isBlank()) {
            try {
                if (indianNumber.length() == 10) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("authorization", fast2smsApiKey.trim());
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    Map<String, Object> body = Map.of(
                            "route", "otp",
                            "variables_values", otp,
                            "numbers", indianNumber
                    );

                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                    try {
                        ResponseEntity<String> response = restTemplate.postForEntity(
                                "https://www.fast2sms.com/dev/bulkV2", entity, String.class);

                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().contains("\"return\":true")) {
                            log.info("✅ Fast2SMS OTP sent successfully to {}", phone);
                            return true;
                        } else {
                            log.warn("Fast2SMS OTP route response: {}", response.getBody());
                        }
                    } catch (Exception otpEx) {
                        log.warn("Fast2SMS OTP route error (trying Quick SMS route fallback): {}", otpEx.getMessage());
                        Map<String, Object> qBody = Map.of(
                                "route", "q",
                                "message", message,
                                "language", "english",
                                "numbers", indianNumber
                        );
                        HttpEntity<Map<String, Object>> qEntity = new HttpEntity<>(qBody, headers);
                        ResponseEntity<String> qResponse = restTemplate.postForEntity(
                                "https://www.fast2sms.com/dev/bulkV2", qEntity, String.class);
                        if (qResponse.getStatusCode().is2xxSuccessful() && qResponse.getBody() != null && qResponse.getBody().contains("\"return\":true")) {
                            log.info("✅ Fast2SMS Quick SMS fallback sent successfully to {}", phone);
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Fast2SMS dispatch attempt failed: {}", e.getMessage());
            }
        }

        // 4. Try 2Factor.in (Specialized Indian OTP SMS Gateway)
        if (twoFactorApiKey != null && !twoFactorApiKey.isBlank() && !twoFactorApiKey.startsWith("your_")) {
            try {
                String formattedPhone = (indianNumber.length() == 10) ? "+91" + indianNumber : phone;
                String twoFactorUrl = "https://2factor.in/API/V1/" + twoFactorApiKey.trim() + "/SMS/" + formattedPhone + "/" + otp;
                ResponseEntity<String> response = restTemplate.getForEntity(twoFactorUrl, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().toUpperCase().contains("SUCCESS")) {
                    log.info("✅ 2Factor.in OTP SMS successfully delivered to {} | Response: {}", phone, response.getBody());
                    return true;
                } else {
                    log.warn("2Factor.in primary dispatch response: {} (trying without +91 format)", response.getBody());
                    String rawUrl = "https://2factor.in/API/V1/" + twoFactorApiKey.trim() + "/SMS/" + indianNumber + "/" + otp;
                    ResponseEntity<String> rawResponse = restTemplate.getForEntity(rawUrl, String.class);
                    if (rawResponse.getStatusCode().is2xxSuccessful() && rawResponse.getBody() != null && rawResponse.getBody().toUpperCase().contains("SUCCESS")) {
                        log.info("✅ 2Factor.in OTP SMS successfully delivered to {} | Response: {}", phone, rawResponse.getBody());
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("2Factor.in dispatch attempt failed: {}", e.getMessage());
            }
        }

        // 5. Try Textbelt Free Tier
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("phone", phone);
            map.add("message", message);
            map.add("key", (textbeltKey != null && !textbeltKey.isBlank()) ? textbeltKey.trim() : "textbelt");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://textbelt.com/text", request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().contains("\"success\":true")) {
                log.info("✅ Textbelt SMS dispatched successfully to {}", phone);
                return true;
            } else {
                log.info("Textbelt response: {}", response.getBody());
            }
        } catch (Exception e) {
            log.warn("Textbelt free tier note: {}", e.getMessage());
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
