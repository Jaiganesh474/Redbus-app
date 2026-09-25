package com.redbus.controller;

import com.redbus.dto.*;
import com.redbus.service.AiService;
import com.redbus.service.MlService;
import com.redbus.service.UserActivityService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api", "/api/v1"})
@RequiredArgsConstructor
public class MlController {

    private final MlService mlService;
    private final UserActivityService userActivityService;
    private final AiService aiService;

    // 1. Client User Activity Logging
    @PostMapping("/activity/track")
    public ResponseEntity<UserActivityDto> trackActivity(
            @RequestBody UserActivityDto request,
            HttpServletRequest httpRequest
    ) {
        if (request.getIpAddress() == null || request.getIpAddress().isBlank()) {
            request.setIpAddress(httpRequest.getRemoteAddr());
        }
        if (request.getUserAgent() == null || request.getUserAgent().isBlank()) {
            request.setUserAgent(httpRequest.getHeader("User-Agent"));
        }
        UserActivityDto response = userActivityService.logActivity(request);
        return ResponseEntity.ok(response);
    }

    // 2. Bus Delay & Punctuality ETA Predictor ML
    @GetMapping("/ml/predict-delay")
    public ResponseEntity<DelayPredictionDto> predictDelay(
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String departureTime
    ) {
        DelayPredictionDto response = mlService.predictBusDelay(scheduleId, source, destination, departureTime);
        return ResponseEntity.ok(response);
    }

    // 3. Dynamic Surge Pricing Predictor ML
    @GetMapping("/ml/dynamic-price")
    public ResponseEntity<DynamicPricePredictionDto> getDynamicPrice(
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) BigDecimal basePrice
    ) {
        DynamicPricePredictionDto response = mlService.calculateDynamicPricing(scheduleId, source, destination, basePrice);
        return ResponseEntity.ok(response);
    }

    // 4. Smart Seat Recommendations ML
    @GetMapping("/ml/smart-seat-recommendations")
    public ResponseEntity<SmartSeatRecommendationDto> getSmartSeatRecommendations(
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "ANY") String gender
    ) {
        SmartSeatRecommendationDto response = mlService.getSmartSeatRecommendations(scheduleId, userId, gender);
        return ResponseEntity.ok(response);
    }

    // 5. Operator / Fleet AI Bus Photos
    @GetMapping("/ml/bus-photos")
    public ResponseEntity<List<AiBusPhotoDto>> getAiBusPhotos(
            @RequestParam(defaultValue = "AC_SLEEPER") String busType,
            @RequestParam(defaultValue = "Royal Express Luxury Coach") String busName,
            @RequestParam(required = false) String category
    ) {
        List<AiBusPhotoDto> photos = mlService.generateAiBusPhotos(busType, busName, category);
        return ResponseEntity.ok(photos);
    }

    // 6. Admin AI & MLOps Monitoring Dashboard
    @GetMapping("/admin/ml/ai-monitoring")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<AiMonitoringStatsDto> getAiMonitoringStats() {
        AiMonitoringStatsDto stats = aiService.getAiMonitoringStats();
        return ResponseEntity.ok(stats);
    }

    // 7. Admin User Activity & Fraud Shield Dashboard
    @GetMapping("/admin/ml/user-activity")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<UserActivityStatsDto> getUserActivityStats() {
        UserActivityStatsDto stats = userActivityService.getUserActivityStats();
        return ResponseEntity.ok(stats);
    }

    // 8. Admin AI Simulation Playground
    @PostMapping("/admin/ml/simulate-query")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> simulateAiQuery(@RequestBody Map<String, String> payload) {
        String testQuery = payload.getOrDefault("query", "AC sleeper bus from Bangalore to Chennai tomorrow");
        Map<String, Object> result = aiService.simulateAiQuery(testQuery);
        return ResponseEntity.ok(result);
    }
}
