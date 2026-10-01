package com.redbus.service;

import com.redbus.entity.Booking;
import com.redbus.entity.Route;
import com.redbus.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JourneyReminderAutomationService {

    private final BookingRepository bookingRepository;
    private final FirebaseNotificationService firebaseNotificationService;
    private final SmsService smsService;

    @Value("${app.automation.journey-reminder-enabled:true}")
    private boolean isReminderAutomationEnabled;

    @Value("${app.automation.reminder-window-minutes:60}")
    private int reminderWindowMinutes;

    /**
     * Automatic Cron / Background Task: Runs every 60 seconds (1 minute).
     * Scans for confirmed bookings whose departure is within 1 hour (~60 mins) and dispatches alerts.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    @Transactional
    public void scheduleJourneyDepartureReminders() {
        if (!isReminderAutomationEnabled) {
            return;
        }

        executeReminderScan();
    }

    private static final java.time.ZoneId IST_ZONE = java.time.ZoneId.of("Asia/Kolkata");

    /**
     * Core scan and dispatch routine using Indian Standard Time (IST).
     */
    @Transactional
    public Map<String, Object> executeReminderScan() {
        LocalDateTime nowIst = LocalDateTime.now(IST_ZONE);
        LocalDate todayIst = nowIst.toLocalDate();
        LocalDate yesterdayIst = todayIst.minusDays(1);
        LocalDate tomorrowIst = todayIst.plusDays(1);

        log.info("🔍 [JOURNEY REMINDER SCAN] Current IST: {} | Checking window <= {} minutes", 
                nowIst, reminderWindowMinutes);

        List<Booking> pendingBookings = bookingRepository.findPendingDepartureReminders(
                List.of(yesterdayIst, todayIst, tomorrowIst)
        );

        int matchedCount = 0;
        int dispatchedCount = 0;

        for (Booking booking : pendingBookings) {
            Route route = booking.getRoute();
            if (route == null || route.getTravelDate() == null || route.getDepartureTime() == null) {
                continue;
            }

            LocalDateTime departureDateTime = LocalDateTime.of(route.getTravelDate(), route.getDepartureTime());
            long minutesUntilDeparture = Duration.between(nowIst, departureDateTime).toMinutes();

            log.debug("Checking PNR: {} | Travel Date: {} | Dep: {} | Minutes to departure: {}",
                    booking.getPnr(), route.getTravelDate(), route.getDepartureTime(), minutesUntilDeparture);

            if (minutesUntilDeparture <= reminderWindowMinutes && minutesUntilDeparture >= -15) {
                matchedCount++;
                try {
                    log.info("⏰ [JOURNEY REMINDER TRIGGERED] PNR: {} | Departs at: {} (in {} mins) | Recipient: {}",
                            booking.getPnr(), departureDateTime, minutesUntilDeparture, booking.getContactPhone());

                    // 1. Dispatch SMS
                    try {
                        smsService.sendJourneyReminderSms(booking);
                    } catch (Exception e) {
                        log.warn("SMS dispatch attempt note for PNR {}: {}", booking.getPnr(), e.getMessage());
                    }

                    // 2. Dispatch Firebase Cloud Messaging (FCM HTTP v1)
                    boolean fcmSent = firebaseNotificationService.sendJourneyReminder(booking);

                    // 3. Mark booking as reminder sent
                    booking.setDepartureReminderSent(true);
                    booking.setDepartureReminderSentAt(LocalDateTime.now());
                    bookingRepository.save(booking);

                    dispatchedCount++;
                } catch (Exception e) {
                    log.error("Failed to process journey reminder for PNR {}: {}", booking.getPnr(), e.getMessage(), e);
                }
            }
        }

        if (dispatchedCount > 0) {
            log.info("🚀 [JOURNEY REMINDER AUTOMATION COMPLETED] Successfully sent 1-hr alerts for {} ticket(s)", dispatchedCount);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("scannedPendingCount", pendingBookings.size());
        result.put("matchedInWindowCount", matchedCount);
        result.put("dispatchedCount", dispatchedCount);
        result.put("timestamp", LocalDateTime.now());
        return result;
    }

    /**
     * Forces immediate dispatch of 1-hour ticket reminder for a specific PNR.
     */
    @Transactional
    public Map<String, Object> sendImmediateReminderForPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with PNR: " + pnr));

        boolean smsSuccess = false;
        try {
            smsSuccess = smsService.sendJourneyReminderSms(booking);
        } catch (Exception ignored) {}

        boolean firebaseSuccess = firebaseNotificationService.sendJourneyReminder(booking);

        booking.setDepartureReminderSent(true);
        booking.setDepartureReminderSentAt(LocalDateTime.now());
        bookingRepository.save(booking);

        Map<String, Object> response = new HashMap<>();
        response.put("pnr", booking.getPnr());
        response.put("smsSent", smsSuccess);
        response.put("firebaseSent", firebaseSuccess);
        response.put("phone", booking.getContactPhone());
        response.put("departureReminderSentAt", booking.getDepartureReminderSentAt());
        response.put("message", "Journey reminder dispatched successfully.");
        return response;
    }
}
