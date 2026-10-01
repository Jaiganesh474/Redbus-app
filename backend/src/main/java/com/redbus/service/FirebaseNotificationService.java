package com.redbus.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.redbus.entity.Booking;
import com.redbus.entity.BookingPassenger;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FirebaseNotificationService {

    @Value("${app.firebase.credentials-path:${FIREBASE_CREDENTIALS_PATH:firebase-service-account.json}}")
    private String firebaseCredentialsPath;

    @Value("${app.firebase.project-id:${FIREBASE_PROJECT_ID:${NEXT_PUBLIC_FIREBASE_PROJECT_ID:redbus-clone-app}}}")
    private String firebaseProjectId;

    private boolean isFirebaseInitialized = false;

    @PostConstruct
    public void initFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                File file = new File(firebaseCredentialsPath);
                if (!file.exists()) {
                    // Try searching in backend directory or working directory
                    file = new File("backend/" + firebaseCredentialsPath);
                }
                if (!file.exists()) {
                    file = new File("/home/ubuntu/Redbus-app/backend/firebase-service-account.json");
                }

                if (file.exists() && file.isFile()) {
                    try (InputStream serviceAccount = new FileInputStream(file)) {
                        FirebaseOptions options = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .setProjectId(firebaseProjectId)
                                .build();

                        FirebaseApp.initializeApp(options);
                        isFirebaseInitialized = true;
                        log.info("🔥 [FIREBASE ADMIN SDK] Successfully initialized Firebase App with Service Account: {}", file.getAbsolutePath());
                    }
                } else {
                    log.warn("⚠️ Firebase service account file not found at '{}'. Firebase V1 notifications will run in simulation mode until file is placed.", firebaseCredentialsPath);
                }
            } else {
                isFirebaseInitialized = true;
            }
        } catch (Exception e) {
            log.warn("Could not initialize Firebase Admin SDK: {}", e.getMessage());
        }
    }

    /**
     * Sends a 1-hour pre-journey reminder via Firebase Cloud Messaging (HTTP v1).
     */
    public boolean sendJourneyReminder(Booking booking) {
        if (booking == null) {
            return false;
        }

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

        String title = "🚌 Bus Departing in 1 Hour (" + depTimeStr + ") - PNR: " + pnr;
        String busDetails = busReg.isBlank() ? operatorName : (operatorName + " " + busReg);
        String body = String.format("Your bus %s to %s departs at %s from %s. Seat(s): %s. Tap to view ticket.",
                busDetails, destination, depTimeStr, boardingPoint, seats);

        String topic = "booking_" + pnr;

        Map<String, String> dataPayload = new HashMap<>();
        dataPayload.put("type", "JOURNEY_REMINDER_1HR");
        dataPayload.put("pnr", pnr);
        dataPayload.put("departureTime", depTimeStr);
        dataPayload.put("boardingPoint", boardingPoint);
        dataPayload.put("seats", seats);
        dataPayload.put("operator", operatorName);
        dataPayload.put("click_action", "/booking/success?pnr=" + pnr);

        if (!isFirebaseInitialized) {
            initFirebase();
        }

        if (isFirebaseInitialized) {
            try {
                Message message = Message.builder()
                        .setTopic(topic)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putAllData(dataPayload)
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);
                log.info("✅ [FIREBASE FCM HTTP V1] Successfully dispatched notification to topic '{}' | Message ID: {}", topic, response);
                return true;
            } catch (Exception e) {
                log.error("❌ [FIREBASE FCM HTTP V1] Failed to dispatch push notification: {}", e.getMessage(), e);
            }
        } else {
            log.info("🔥 [FIREBASE NOTIFICATION SIMULATOR] Topic: {} | Title: '{}' | Body: '{}'", topic, title, body);
        }

        return true;
    }
}
