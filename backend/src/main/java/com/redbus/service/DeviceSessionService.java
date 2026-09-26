package com.redbus.service;

import com.redbus.dto.UserDeviceSessionDto;
import com.redbus.entity.User;
import com.redbus.entity.UserDeviceSession;
import com.redbus.repository.UserDeviceSessionRepository;
import com.redbus.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceSessionService {

    private final UserDeviceSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserDeviceSession recordSession(User user, HttpServletRequest request, String token) {
        if (user == null || request == null) return null;

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) userAgent = "Unknown Browser";

        String ip = extractClientIp(request);
        String browser = detectBrowser(userAgent);
        String os = detectOs(userAgent);
        String deviceName = browser + " on " + os;
        String location = determineLocationFromIp(ip);
        String sessionToken = token != null ? token : UUID.randomUUID().toString();

        // Check if session for this user with same IP and UserAgent already exists
        List<UserDeviceSession> existingSessions = sessionRepository.findByUserAndIsActiveTrue(user);
        for (UserDeviceSession s : existingSessions) {
            if (ip.equals(s.getIpAddress()) && userAgent.equals(s.getUserAgent())) {
                s.setLastActive(LocalDateTime.now());
                s.setSessionToken(sessionToken);
                return sessionRepository.save(s);
            }
        }

        UserDeviceSession newSession = UserDeviceSession.builder()
                .user(user)
                .sessionToken(sessionToken)
                .deviceName(deviceName)
                .browser(browser)
                .operatingSystem(os)
                .ipAddress(ip)
                .location(location)
                .userAgent(userAgent)
                .isActive(true)
                .lastActive(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        return sessionRepository.save(newSession);
    }

    @Transactional(readOnly = true)
    public List<UserDeviceSessionDto> getUserSessions(String userEmail, HttpServletRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        String currentIp = extractClientIp(request);
        String currentUa = request.getHeader("User-Agent");

        List<UserDeviceSession> sessions = sessionRepository.findByUserAndIsActiveTrueOrderByLastActiveDesc(user);

        // If no sessions yet (e.g. user created before table), auto seed current session
        if (sessions.isEmpty()) {
            UserDeviceSession current = recordSession(user, request, "active-current-session");
            if (current != null) {
                sessions = List.of(current);
            }
        }

        boolean currentMarked = false;
        List<UserDeviceSessionDto> dtoList = new java.util.ArrayList<>();

        for (UserDeviceSession s : sessions) {
            boolean isCurrent = false;
            if (!currentMarked && currentIp.equals(s.getIpAddress()) && (currentUa == null || currentUa.equals(s.getUserAgent()))) {
                isCurrent = true;
                currentMarked = true;
            }

            dtoList.add(UserDeviceSessionDto.builder()
                    .id(s.getId())
                    .deviceName(s.getDeviceName())
                    .browser(s.getBrowser())
                    .operatingSystem(s.getOperatingSystem())
                    .ipAddress(s.getIpAddress())
                    .location(s.getLocation())
                    .isCurrent(isCurrent)
                    .lastActive(s.getLastActive())
                    .createdAt(s.getCreatedAt())
                    .build());
        }

        // If no match directly, mark first as current
        if (!currentMarked && !dtoList.isEmpty()) {
            dtoList.get(0).setIsCurrent(true);
        }

        return dtoList;
    }

    @Transactional
    public void revokeSession(String userEmail, Long sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        UserDeviceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to revoke this session");
        }

        session.setIsActive(false);
        sessionRepository.save(session);
        log.info("Revoked session ID {} for user {}", sessionId, userEmail);
    }

    @Transactional
    public void revokeAllOtherSessions(String userEmail, HttpServletRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        String currentIp = extractClientIp(request);
        String currentUa = request.getHeader("User-Agent");

        List<UserDeviceSession> sessions = sessionRepository.findByUserAndIsActiveTrue(user);
        for (UserDeviceSession s : sessions) {
            boolean isCurrent = currentIp.equals(s.getIpAddress()) && (currentUa == null || currentUa.equals(s.getUserAgent()));
            if (!isCurrent) {
                s.setIsActive(false);
                sessionRepository.save(s);
            }
        }
        log.info("Revoked all other sessions for user {}", userEmail);
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        String remote = request.getRemoteAddr();
        return (remote != null && !remote.isBlank()) ? remote : "127.0.0.1";
    }

    private String detectBrowser(String ua) {
        if (ua == null) return "Unknown Browser";
        String lower = ua.toLowerCase(Locale.ROOT);
        if (lower.contains("edg/")) return "Microsoft Edge";
        if (lower.contains("chrome") && !lower.contains("edg")) return "Google Chrome";
        if (lower.contains("firefox")) return "Mozilla Firefox";
        if (lower.contains("safari") && !lower.contains("chrome")) return "Apple Safari";
        if (lower.contains("opera") || lower.contains("opr/")) return "Opera";
        return "Standard Web Browser";
    }

    private String detectOs(String ua) {
        if (ua == null) return "Unknown OS";
        String lower = ua.toLowerCase(Locale.ROOT);
        if (lower.contains("windows nt 10.0")) return "Windows 10/11";
        if (lower.contains("windows nt 6.3")) return "Windows 8.1";
        if (lower.contains("windows nt 6.1")) return "Windows 7";
        if (lower.contains("windows")) return "Windows";
        if (lower.contains("macintosh") || lower.contains("mac os x")) return "macOS";
        if (lower.contains("android")) return "Android";
        if (lower.contains("iphone") || lower.contains("ipad") || lower.contains("ios")) return "iOS";
        if (lower.contains("linux")) return "Linux";
        return "Desktop/Mobile OS";
    }

    private String determineLocationFromIp(String ip) {
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return "Localhost / India Network";
        }
        return "India (Secure Cloud)";
    }
}
