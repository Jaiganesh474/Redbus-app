package com.redbus.controller;

import com.redbus.dto.UserDeviceSessionDto;
import com.redbus.service.DeviceSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/users/me/sessions", "/api/v1/users/me/sessions"})
@RequiredArgsConstructor
@Slf4j
public class DeviceSessionController {

    private final DeviceSessionService deviceSessionService;

    @GetMapping
    public ResponseEntity<List<UserDeviceSessionDto>> getMySessions(
            Authentication authentication,
            HttpServletRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        try {
            List<UserDeviceSessionDto> sessions = deviceSessionService.getUserSessions(email, request);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            log.error("Failed to fetch user device sessions for {}: {}", email, e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> revokeSession(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        try {
            deviceSessionService.revokeSession(email, id);
            return ResponseEntity.ok(Map.of("message", "Device session revoked successfully", "sessionId", id));
        } catch (Exception e) {
            log.error("Failed to revoke session ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to revoke session"));
        }
    }

    @PostMapping("/revoke-others")
    public ResponseEntity<Map<String, Object>> revokeAllOtherSessions(
            Authentication authentication,
            HttpServletRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        try {
            deviceSessionService.revokeAllOtherSessions(email, request);
            return ResponseEntity.ok(Map.of("message", "All other device sessions logged out successfully"));
        } catch (Exception e) {
            log.error("Failed to revoke other sessions for user {}: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to revoke sessions"));
        }
    }
}
