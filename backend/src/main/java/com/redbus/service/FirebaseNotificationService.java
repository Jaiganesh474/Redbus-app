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
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FirebaseNotificationService {

    private final RestTemplate restTemplate;

    @Value("${app.firebase.server-key:${FIREBASE_SERVER_KEY:}}")
    private String firebaseServerKey;

    @Value("${app.firebase.project-id:${FIREBASE_PROJECT_ID:redbus-clone-app}}")
    private String firebaseProjectId;

    public FirebaseNotificationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Sends a 1-hour pre-journey reminder push notification through Firebase Cloud Messaging (FCM).
     */
    public boolean sendJourneyReminder(Booking booking) {
        if (booking == null) {
            return false;
        }

        String pnr = booking.getPnr();
        String operatorName = (booking.getRoute() != null && booking.getRoute().getBus() != null)
                ? booking.getRoute().getBus().getOperatorName()
                : "redBus Express";
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

        String title = "🚌 Bus Departing in 1 Hour (" + depTimeStr + ") - PNR: " + pnr;
        String body = String.format("Your %s bus to %s departs at %s from %s. Seat(s): %s. Tap to view live ticket.",
                operatorName, destination, depTimeStr, boardingPoint, seats);

        Map<String, String> dataPayload = new HashMap<>();
        dataPayload.put("type", "JOURNEY_REMINDER_1HR");
        dataPayload.put("pnr", pnr);
        dataPayload.put("departureTime", depTimeStr);
        dataPayload.put("boardingPoint", boardingPoint);
        dataPayload.put("seats", seats);
        dataPayload.put("operator", operatorName);
        dataPayload.put("click_action", "/booking/success?pnr=" + pnr);

        // Topic name for passenger or direct FCM dispatch
        String topic = "booking_" + pnr;

        boolean dispatched = dispatchFcmTopicNotification(topic, title, body, dataPayload);

        log.info("🔥 [FIREBASE CLOUD NOTIFICATION] PNR: {} | Topic: {} | Title: {} | FCM Sent: {}",
                pnr, topic, title, dispatched);

        return true;
    }

    /**
     * Dispatches notification to a specific FCM Topic or Device Token via Firebase Cloud Messaging Legacy/HTTP API.
     */
    public boolean dispatchFcmTopicNotification(String topicOrToken, String title, String body, Map<String, String> data) {
        if (firebaseServerKey == null || firebaseServerKey.isBlank() || firebaseServerKey.startsWith("your_") || "mock-key".equalsIgnoreCase(firebaseServerKey)) {
            log.info("🔥 [FIREBASE FCM SIMULATOR] Recipient: {} | Title: '{}' | Body: '{}' | Data: {}",
                    topicOrToken, title, body, data);
            return false;
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
                log.info("✅ Firebase FCM message delivered successfully to {} | Response: {}", topicOrToken, response.getBody());
                return true;
            } else {
                log.warn("Firebase FCM response: {}", response.getBody());
            }
        } catch (Exception e) {
            log.warn("Firebase FCM dispatch failed: {}", e.getMessage());
        }

        return false;
    }
}
