package com.redbus.controller;

import com.redbus.dto.*;
import com.redbus.entity.Operator;
import com.redbus.entity.User;
import com.redbus.exception.BadRequestException;
import com.redbus.service.AuthService;
import com.redbus.service.OperatorAnalyticsService;
import com.redbus.service.OperatorService;
import com.redbus.service.PdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
import java.time.LocalDate;
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
    private final PdfService pdfService;

    private Operator getAuthenticatedOperator() {
        User user = authService.getAuthenticatedUser();
        return operatorService.getOrCreateOperatorForUser(user);
    }

    @GetMapping({"/me", "/profile"})
    public ResponseEntity<OperatorDto> getProfile() {
        Operator op = getAuthenticatedOperator();
        return ResponseEntity.ok(operatorService.mapToOperatorDto(op));
    }

    @PutMapping({"/me", "/profile"})
    public ResponseEntity<OperatorDto> updateProfile(@Valid @RequestBody UpdateOperatorProfileRequest req) {
        Operator op = getAuthenticatedOperator();
        OperatorDto updated = operatorService.updateProfile(op.getId(), req);
        return ResponseEntity.ok(updated);
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

    @GetMapping("/manifest")
    public ResponseEntity<List<OperatorPassengerManifestDto>> getPassengerManifest(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long busId,
            @RequestParam(required = false) Long scheduleId
    ) {
        Operator op = getAuthenticatedOperator();
        LocalDate travelDate = date != null ? date : LocalDate.now();
        List<OperatorPassengerManifestDto> manifest = operatorAnalyticsService.getPassengerManifest(op, travelDate, busId, scheduleId);
        return ResponseEntity.ok(manifest);
    }

    @GetMapping("/manifest/pdf")
    public ResponseEntity<byte[]> downloadPassengerManifestPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long busId,
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) String routeName,
            @RequestParam(required = false) String busRegNo,
            @RequestParam(required = false) String departureTime
    ) {
        Operator op = getAuthenticatedOperator();
        LocalDate travelDate = date != null ? date : LocalDate.now();
        List<OperatorPassengerManifestDto> manifest = operatorAnalyticsService.getPassengerManifest(op, travelDate, busId, scheduleId);

        byte[] pdfBytes = pdfService.generatePassengerManifestPdf(
                manifest,
                op.getCompanyName() != null ? op.getCompanyName() : op.getContactPerson(),
                routeName,
                travelDate,
                busRegNo,
                departureTime
        );

        String dateStr = travelDate.toString();
        String safeRoute = routeName != null ? routeName.replaceAll("[^a-zA-Z0-9]", "_") : "bus_manifest";
        String filename = "passenger_manifest_" + safeRoute + "_" + dateStr + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
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
