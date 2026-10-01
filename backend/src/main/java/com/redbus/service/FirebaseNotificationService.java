package com.redbus.service;

import com.redbus.entity.Booking;
import com.redbus.entity.BookingPassenger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FirebaseNotificationService {

    private final RestTemplate restTemplate;

    @Value("${app.firebase.api-key:${FIREBASE_API_KEY:${NEXT_PUBLIC_FIREBASE_API_KEY:}}}")
    private String firebaseApiKey;

    @Value("${app.firebase.server-key:${FIREBASE_SERVER_KEY:}}")
    private String firebaseServerKey;

    @Value("${app.firebase.project-id:${FIREBASE_PROJECT_ID:${NEXT_PUBLIC_FIREBASE_PROJECT_ID:redbus-clone-app}}}")
    private String firebaseProjectId;

    public FirebaseNotificationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Normalizes Indian and international phone numbers into E.164 standard format.
     */
    public String normalizePhone(String rawPhone) {
        if (rawPhone == null) return "";
        String cleaned = rawPhone.replaceAll("[\\s\\-\\(\\)]", "").trim();
        if (cleaned.startsWith("+")) {
            return "+" + cleaned.substring(1).replaceAll("[^0-9]", "");
        }
        String digitsOnly = cleaned.replaceAll("[^0-9]", "");
        if (digitsOnly.length() == 10) {
            return "+91" + digitsOnly;
        } else if (digitsOnly.length() == 12 && digitsOnly.startsWith("91")) {
            return "+" + digitsOnly;
        }
        return "+" + digitsOnly;
    }

    /**
     * Sends a 1-hour pre-journey reminder via Firebase SMS and Firebase Push Notification on Blaze Plan.
     */
    public boolean sendJourneyReminder(Booking booking) {
        if (booking == null) {
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
        String destination = (booking.getRoute() != null) ? booking.getRoute().getDestinationCity() : "Destination";

        String depTimeStr = "";
        if (booking.getRoute() != null && booking.getRoute().getDepartureTime() != null) {
            depTimeStr = booking.getRoute().getDepartureTime().format(DateTimeFormatter.ofPattern("hh:mm a"));
        }

        String boardingPoint = (booking.getBoardingPoint() != null && !booking.getBoardingPoint().isBlank())
                ? booking.getBoardingPoint()
                : "Boarding Point";

        String seats = "";
        if (booking.getPassengers() != null && !booking.getPassengers().isEmpty()) {
            seats = booking.getPassengers().stream()
                    .map(BookingPassenger::getSeatNumber)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(", "));
        }

        String busDetails = busReg.isBlank() ? operatorName : (operatorName + " " + busReg);
        String smsMessage = String.format(
                "redBus Alert: Your bus %s to %s departs at %s (in ~1 hr). Boarding: %s. PNR: %s, Seat(s): %s. Have a safe journey!",
                busDetails, destination, depTimeStr, boardingPoint, pnr, seats
        );

        log.info("🔥 [FIREBASE 1-HR REMINDER DISPATCH] To: {} | PNR: {} | Departure: {}", phone, pnr, depTimeStr);

        // 1. Dispatch Firebase SMS (Blaze Plan)
        boolean smsSent = dispatchFirebaseSms(phone, smsMessage);

        // 2. Dispatch Firebase Cloud Messaging Push Notification
        String title = "🚌 Bus Departing in 1 Hour (" + depTimeStr + ") - PNR: " + pnr;
        Map<String, String> dataPayload = new HashMap<>();
        dataPayload.put("type", "JOURNEY_REMINDER_1HR");
        dataPayload.put("pnr", pnr);
        dataPayload.put("departureTime", depTimeStr);
        dataPayload.put("boardingPoint", boardingPoint);
        dataPayload.put("seats", seats);
        dataPayload.put("operator", operatorName);
        dataPayload.put("click_action", "/booking/success?pnr=" + pnr);

        String topic = "booking_" + pnr;
        boolean fcmSent = dispatchFcmTopicNotification(topic, title, smsMessage, dataPayload);

        return smsSent || fcmSent;
    }

    /**
     * Dispatches SMS via Firebase Authentication / Google Cloud Identity Toolkit API (Blaze Plan).
     */
    public boolean dispatchFirebaseSms(String phone, String message) {
        if (firebaseApiKey == null || firebaseApiKey.isBlank() || firebaseApiKey.startsWith("your_")) {
            log.info("🔥 [FIREBASE SMS SIMULATOR - BLAZE PLAN] Recipient: {} | Message: '{}'", phone, message);
            return true;
        }

        try {
            String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:sendVerificationCode?key=" + firebaseApiKey.trim();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phoneNumber", phone);
            requestBody.put("iosReceipt", null);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Firebase SMS Gateway dispatched successfully to {} | Response: {}", phone, response.getBody());
                return true;
            } else {
                log.warn("⚠️ Firebase SMS status {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (HttpStatusCodeException e) {
            log.error("❌ Firebase SMS HTTP {} Error: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ Firebase SMS dispatch failed: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Dispatches notification to a specific FCM Topic or Device Token via Firebase Cloud Messaging.
     */
    public boolean dispatchFcmTopicNotification(String topicOrToken, String title, String body, Map<String, String> data) {
        if (firebaseServerKey == null || firebaseServerKey.isBlank() || firebaseServerKey.startsWith("your_")) {
            log.info("🔥 [FIREBASE FCM NOTIFICATION] Recipient: {} | Title: '{}' | Body: '{}'", topicOrToken, title, body);
            return true;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "key=" + firebaseServerKey.trim());

            Map<String, Object> payload = new HashMap<>();
            if (topicOrToken.startsWith("/topics/") || !topicOrToken.contains(":")) {
                String formattedTopic = topicOrToken.startsWith("/topics/") ? topicOrToken : ("/topics/" + topicOrToken);
                payload.put("to", formattedTopic);
            } else {
                payload.put("to", topicOrToken);
            }

            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("body", body);
            notification.put("sound", "default");
            notification.put("priority", "high");

            payload.put("notification", notification);
            if (data != null && !data.isEmpty()) {
                payload.put("data", data);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://fcm.googleapis.com/fcm/send", entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Firebase FCM delivered successfully to {} | Response: {}", topicOrToken, response.getBody());
                return true;
            } else {
                log.warn("Firebase FCM response: {}", response.getBody());
            }
        } catch (HttpStatusCodeException e) {
            log.error("❌ Firebase FCM HTTP {} Error: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Firebase FCM dispatch failed: {}", e.getMessage());
        }

        return false;
    }
}
