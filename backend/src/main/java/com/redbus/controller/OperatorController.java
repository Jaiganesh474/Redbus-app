package com.redbus.controller;

import com.redbus.dto.*;
import com.redbus.entity.Operator;
import com.redbus.entity.User;
import com.redbus.exception.BadRequestException;
import com.redbus.service.AuthService;
import com.redbus.service.OperatorAnalyticsService;
import com.redbus.service.OperatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/operator", "/api/operator"})
@RequiredArgsConstructor
public class OperatorController {

    private final AuthService authService;
    private final OperatorService operatorService;
    private final OperatorAnalyticsService operatorAnalyticsService;

    private Operator getAuthenticatedOperator() {
        User user = authService.getAuthenticatedUser();
        return operatorService.getOrCreateOperatorForUser(user);
    }

    @GetMapping("/me")
    public ResponseEntity<OperatorDto> getProfile() {
        Operator op = getAuthenticatedOperator();
        return ResponseEntity.ok(OperatorDto.builder()
                .id(op.getId())
                .userId(op.getUser().getId())
                .companyName(op.getCompanyName())
                .contactPerson(op.getContactPerson())
                .email(op.getEmail())
                .phone(op.getPhone())
                .commissionRate(op.getCommissionRate())
                .status(op.getStatus())
                .createdAt(op.getCreatedAt())
                .build());
    }

    @GetMapping("/buses")
    public ResponseEntity<List<BusResponseDto>> getBuses() {
        Operator op = getAuthenticatedOperator();
        return ResponseEntity.ok(operatorService.getOperatorBuses(op.getId()));
    }

    @PostMapping("/buses")
    public ResponseEntity<BusResponseDto> createBus(@Valid @RequestBody BusCreateRequest req) {
        Operator op = getAuthenticatedOperator();
        BusResponseDto created = operatorService.createBus(op.getId(), req);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping(value = "/buses/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadBusImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Please select an image file to upload");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            throw new BadRequestException("Only image files (JPEG, PNG, WEBP) are allowed");
        }

        try {
            Path uploadDir = Paths.get("uploads", "buses");
            File dir = uploadDir.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String cleanFileName = "bus_" + UUID.randomUUID().toString().substring(0, 10) + extension;
            Path targetPath = uploadDir.resolve(cleanFileName);
            Files.copy(file.getInputStream(), targetPath);

            String fileUrl = "/uploads/buses/" + cleanFileName;
            log.info("Successfully uploaded bus image: {}", fileUrl);

            return ResponseEntity.ok(Map.of(
                    "imageUrl", fileUrl,
                    "fileName", cleanFileName,
                    "message", "Image uploaded successfully"
            ));
        } catch (IOException e) {
            log.error("Failed to upload bus image", e);
            throw new RuntimeException("Failed to upload image. Please try again.");
        }
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleResponseDto>> getSchedules() {
        Operator op = getAuthenticatedOperator();
        return ResponseEntity.ok(operatorService.getOperatorSchedules(op.getId()));
    }

    @PostMapping("/schedules")
    public ResponseEntity<ScheduleResponseDto> createSchedule(@Valid @RequestBody ScheduleCreateRequest req) {
        Operator op = getAuthenticatedOperator();
        ScheduleResponseDto created = operatorService.createSchedule(op.getId(), req);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/analytics/overview")
    public ResponseEntity<OperatorAnalyticsDto> getAnalytics() {
        Operator op = getAuthenticatedOperator();
        return ResponseEntity.ok(operatorAnalyticsService.getAnalyticsOverview(op));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<OperatorBookingDto>> getBookings() {
        Operator op = getAuthenticatedOperator();
        return ResponseEntity.ok(operatorAnalyticsService.getOperatorBookings(op));
    }

    @PutMapping("/buses/{id}/photos")
    public ResponseEntity<BusResponseDto> updateBusPhotos(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload
    ) {
        Operator op = getAuthenticatedOperator();
        String photoUrls = payload.get("photoUrls");
        BusResponseDto updated = operatorService.updateBusPhotos(op.getId(), id, photoUrls);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/buses/{id}")
    public ResponseEntity<BusResponseDto> updateBus(
            @PathVariable Long id,
            @RequestBody BusCreateRequest req
    ) {
        Operator op = getAuthenticatedOperator();
        BusResponseDto updated = operatorService.updateBus(op.getId(), id, req);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/buses/{id}")
    public ResponseEntity<Void> deleteBus(@PathVariable Long id) {
        Operator op = getAuthenticatedOperator();
        operatorService.deleteBus(op.getId(), id);
        return ResponseEntity.noContent().build();
    }
}

