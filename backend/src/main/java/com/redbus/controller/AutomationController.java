package com.redbus.controller;

import com.redbus.service.JourneyReminderAutomationService;
import com.redbus.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping({"/api/automation", "/api/v1/automation"})
@RequiredArgsConstructor
public class AutomationController {

    private final JourneyReminderAutomationService journeyReminderAutomationService;
    private final SmsService smsService;

    /**
     * Triggers an immediate scan and dispatch of 1-hour pre-journey reminders.
     */
    @PostMapping("/scan-reminders")
    public ResponseEntity<Map<String, Object>> triggerReminderScan() {
        Map<String, Object> result = journeyReminderAutomationService.executeReminderScan();
        return ResponseEntity.ok(result);
    }

    /**
     * Checks automation background status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAutomationStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "ACTIVE",
                "scheduler", "RUNNING_EVERY_60_SECONDS",
                "targetJourneyWindow", "Within 1 Hour (60 Minutes) Before Departure",
                "smsProvider", "Brevo Transactional SMS + E.164 Engine",
                "pushProvider", "Firebase Cloud Messaging (FCM)",
                "currentTime", LocalDateTime.now()
        ));
    }

    /**
     * Test direct SMS sending to a given phone number.
     */
    @PostMapping("/test-sms")
    public ResponseEntity<Map<String, Object>> testSms(
            @RequestParam String phone,
            @RequestParam(defaultValue = "redBus Journey Alert Test: Your bus to Destination departs in 1 hour. PNR: TEST1234, Seats: A1, A2. Have a safe journey!") String message
    ) {
        boolean sent = smsService.sendDirectSms(phone, message);
        return ResponseEntity.ok(Map.of(
                "success", sent,
                "recipient", smsService.normalizePhone(phone),
                "message", message
        ));
    }
}
