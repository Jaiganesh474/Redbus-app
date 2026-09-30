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

    @Value("${app.sms.textbelt-key:${TEXTBELT_KEY:textbelt}}")
    private String textbeltKey;

    @Value("${app.sms.twilio-account-sid:${TWILIO_ACCOUNT_SID:}}")
    private String twilioAccountSid;

    @Value("${app.sms.twilio-auth-token:${TWILIO_AUTH_TOKEN:}}")
    private String twilioAuthToken;

    @Value("${app.sms.twilio-from-number:${TWILIO_FROM_NUMBER:}}")
    private String twilioFromNumber;

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
     * Dispatches a 6-digit OTP to any mobile number using a free / multi-provider SMS framework.
     */
    public SendMobileOtpResponse sendOtp(String rawPhone, String purpose) {
        String normalizedPhone = normalizePhone(rawPhone);
        if (normalizedPhone.length() < 10) {
            throw new BadRequestException("Invalid mobile phone number. Please check the digits and country code.");
        }

        String normalizedPurpose = (purpose != null && !purpose.isBlank()) ? purpose.toUpperCase().trim() : "LOGIN";

        // Prevent rapid spamming (cooldown check: 30 seconds)
        OtpSession existing = otpCache.get(normalizedPhone);
        if (existing != null && existing.getCreatedAt() != null &&
                existing.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(30))) {
            long waitTime = 30 - java.time.Duration.between(existing.getCreatedAt(), LocalDateTime.now()).getSeconds();
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

        // Dispatch SMS via free SMS framework
        boolean sentViaGateway = dispatchSms(normalizedPhone, otp, normalizedPurpose);

        log.info("======================================================================");
        log.info("📱 [MOBILE OTP DISPATCH] To: {} | Purpose: {} | Code: {} | Gateway Sent: {}",
                normalizedPhone, normalizedPurpose, otp, sentViaGateway);
        log.info("======================================================================");

        String displayMsg = "Verification OTP has been dispatched to " + maskPhoneNumber(normalizedPhone) + ". Code valid for 5 minutes.";

        return SendMobileOtpResponse.builder()
                .success(true)
                .message(displayMsg)
                .phone(normalizedPhone)
                .expiresInSeconds(300)
                .previewOtp(otp) // Included for seamless testing/preview in demo environments
                .build();
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
     * Multi-Provider SMS Sender engine (Textbelt free tier, Fast2SMS, Twilio).
     */
    private boolean dispatchSms(String phone, String otp, String purpose) {
        String message = String.format("Your redBus verification code is %s for %s. Valid for 5 minutes. Do not share with anyone.",
                otp, purpose.replace("_", " ").toLowerCase());

        // 1. Try Fast2SMS if API key is provided
        if (fast2smsApiKey != null && !fast2smsApiKey.isBlank()) {
            try {
                String indianNumber = phone.replace("+91", "").replaceAll("[^0-9]", "");
                if (indianNumber.length() == 10) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("authorization", fast2smsApiKey);
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    Map<String, Object> body = Map.of(
                            "route", "otp",
                            "variables_values", otp,
                            "numbers", indianNumber
                    );

                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                    ResponseEntity<String> response = restTemplate.postForEntity(
                            "https://www.fast2sms.com/dev/bulkV2", entity, String.class);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Fast2SMS OTP sent successfully to {}", phone);
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("Fast2SMS dispatch attempt failed: {}", e.getMessage());
            }
        }

        // 2. Try Textbelt Free Open Tier (Sends 1 free SMS per day per IP/Key without setup to any global number)
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("phone", phone);
            map.add("message", message);
            map.add("key", (textbeltKey != null && !textbeltKey.isBlank()) ? textbeltKey : "textbelt");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://textbelt.com/text", request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().contains("\"success\":true")) {
                log.info("Textbelt free SMS dispatched successfully to {}", phone);
                return true;
            }
        } catch (Exception e) {
            log.warn("Textbelt free tier dispatch note: {}", e.getMessage());
        }

        // 3. Try Twilio if credentials configured
        if (twilioAccountSid != null && !twilioAccountSid.isBlank() && twilioAuthToken != null && !twilioAuthToken.isBlank()) {
            try {
                String twilioUrl = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json";
                HttpHeaders headers = new HttpHeaders();
                headers.setBasicAuth(twilioAccountSid, twilioAuthToken);
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
                map.add("To", phone);
                map.add("From", twilioFromNumber);
                map.add("Body", message);

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(twilioUrl, request, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Twilio SMS dispatched successfully to {}", phone);
                    return true;
                }
            } catch (Exception e) {
                log.warn("Twilio SMS dispatch attempt failed: {}", e.getMessage());
            }
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
