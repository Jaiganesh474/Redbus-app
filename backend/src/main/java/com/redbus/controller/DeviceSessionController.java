package com.redbus.controller;

import com.redbus.dto.UserDeviceSessionDto;
import com.redbus.service.DeviceSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/users/me/sessions", "/api/v1/users/me/sessions"})
@RequiredArgsConstructor
public class DeviceSessionController {

    private final DeviceSessionService deviceSessionService;

    @GetMapping
    public ResponseEntity<List<UserDeviceSessionDto>> getMySessions(
            Authentication authentication,
            HttpServletRequest request) {
        String email = authentication != null ? authentication.getName() : "user@redbus.com";
        List<UserDeviceSessionDto> sessions = deviceSessionService.getUserSessions(email, request);
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> revokeSession(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "user@redbus.com";
        deviceSessionService.revokeSession(email, id);
        return ResponseEntity.ok(Map.of("message", "Device session revoked successfully", "sessionId", id));
    }

    @PostMapping("/revoke-others")
    public ResponseEntity<Map<String, Object>> revokeAllOtherSessions(
            Authentication authentication,
            HttpServletRequest request) {
        String email = authentication != null ? authentication.getName() : "user@redbus.com";
        deviceSessionService.revokeAllOtherSessions(email, request);
        return ResponseEntity.ok(Map.of("message", "All other device sessions logged out successfully"));
    }
}
