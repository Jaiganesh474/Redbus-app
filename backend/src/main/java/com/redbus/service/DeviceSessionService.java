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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceSessionService {

    private final UserDeviceSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserDeviceSession recordSession(User user, HttpServletRequest request, String token) {
        if (user == null) return null;

        try {
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            if (userAgent == null || userAgent.isBlank()) userAgent = "Web Browser";

            String ip = extractClientIp(request);
            String browser = detectBrowser(userAgent);
            String os = detectOs(userAgent);
            String deviceName = browser + " on " + os;
            String deviceType = detectDeviceType(userAgent, os);
            String location = determineLocationFromIp(ip);
            String sessionToken = token != null ? token : UUID.randomUUID().toString();

            // Check if session for this user with same IP, browser, and OS already exists
            Optional<UserDeviceSession> existingSessionOpt = sessionRepository.findByUserAndIpAddressAndBrowserAndOs(user, ip, browser, os);
            if (existingSessionOpt.isPresent()) {
                UserDeviceSession s = existingSessionOpt.get();
                s.setIsActive(true);
                s.setLastActiveAt(LocalDateTime.now());
                s.setSessionToken(sessionToken);
                s.setDeviceName(deviceName);
                s.setDeviceType(deviceType);
                s.setLocation(location);
                return sessionRepository.save(s);
            }

            UserDeviceSession newSession = UserDeviceSession.builder()
                    .user(user)
                    .sessionToken(sessionToken)
                    .deviceName(deviceName)
                    .deviceType(deviceType)
                    .browser(browser)
                    .os(os)
                    .ipAddress(ip)
                    .location(location)
                    .lastActiveAt(LocalDateTime.now())
                    .isCurrentSession(true)
                    .isActive(true)
                    .build();

            return sessionRepository.save(newSession);
        } catch (Exception e) {
            log.warn("Could not record device session: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public List<UserDeviceSessionDto> getUserSessions(String userEmail, HttpServletRequest request) {
        if (userEmail == null || userEmail.isBlank()) {
            return List.of();
        }

        Optional<User> userOpt = userRepository.findByEmail(userEmail.trim());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(userEmail.trim().toLowerCase(Locale.ROOT));
        }
        if (userOpt.isEmpty()) {
            log.warn("User not found for session query: {}", userEmail);
            return List.of();
        }

        User user = userOpt.get();
        String currentIp = extractClientIp(request);
        String currentUa = request != null ? request.getHeader("User-Agent") : null;
        String currentBrowser = detectBrowser(currentUa);
        String currentOs = detectOs(currentUa);

        List<UserDeviceSession> sessions;
        try {
            sessions = sessionRepository.findByUserOrderByLastActiveAtDesc(user);
        } catch (Exception e) {
            log.warn("Error querying device sessions: {}", e.getMessage());
            sessions = new ArrayList<>();
        }

        // If no sessions yet (e.g. user created before session logging was active), auto seed current session
        if (sessions.isEmpty() && request != null) {
            try {
                UserDeviceSession current = recordSession(user, request, "active-current-session");
                if (current != null) {
                    sessions = List.of(current);
                }
            } catch (Exception e) {
                log.warn("Could not auto-seed session: {}", e.getMessage());
            }
        }

        boolean currentMarked = false;
        List<UserDeviceSessionDto> dtoList = new ArrayList<>();

        for (UserDeviceSession s : sessions) {
            boolean isCurrent = false;
            boolean isActive = s.getIsActive() != null ? s.getIsActive() : true;

            if (isActive && !currentMarked && currentIp.equals(s.getIpAddress()) && currentBrowser.equals(s.getBrowser()) && currentOs.equals(s.getOs())) {
                isCurrent = true;
                currentMarked = true;
            }

            dtoList.add(UserDeviceSessionDto.builder()
                    .id(s.getId())
                    .deviceName(s.getDeviceName() != null ? s.getDeviceName() : "Web Browser Session")
                    .deviceType(s.getDeviceType() != null ? s.getDeviceType() : "Desktop")
                    .browser(s.getBrowser() != null ? s.getBrowser() : "Browser")
                    .operatingSystem(s.getOs() != null ? s.getOs() : "OS")
                    .ipAddress(s.getIpAddress() != null ? s.getIpAddress() : "127.0.0.1")
                    .maskedIp(maskIpAddress(s.getIpAddress()))
                    .location(s.getLocation() != null ? s.getLocation() : "India (Secure Cloud)")
                    .isCurrent(isCurrent)
                    .isActive(isActive)
                    .lastActive(s.getLastActiveAt() != null ? s.getLastActiveAt() : LocalDateTime.now())
                    .createdAt(s.getCreatedAt() != null ? s.getCreatedAt() : LocalDateTime.now())
                    .build());
        }

        // If no active session was directly matched as current, mark the first active session as current
        if (!currentMarked && !dtoList.isEmpty()) {
            for (UserDeviceSessionDto d : dtoList) {
                if (Boolean.TRUE.equals(d.getIsActive())) {
                    d.setIsCurrent(true);
                    break;
                }
            }
        }

        return dtoList;
    }

    @Transactional
    public void revokeSession(String userEmail, Long sessionId) {
        if (userEmail == null || sessionId == null) return;

        User user = userRepository.findByEmail(userEmail.trim())
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        UserDeviceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to revoke this session");
        }

        // Mark as inactive (previous session) rather than hard deleting so history is retained
        session.setIsActive(false);
        session.setSessionToken(null);
        session.setLastActiveAt(LocalDateTime.now());
        sessionRepository.save(session);
        log.info("Revoked session ID {} for user {}", sessionId, userEmail);
    }

    @Transactional
    public void deleteSessionPermanently(String userEmail, Long sessionId) {
        if (userEmail == null || sessionId == null) return;

        User user = userRepository.findByEmail(userEmail.trim())
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        UserDeviceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to delete this session");
        }

        sessionRepository.delete(session);
        log.info("Permanently deleted session history ID {} for user {}", sessionId, userEmail);
    }

    @Transactional
    public void revokeAllOtherSessions(String userEmail, HttpServletRequest request) {
        if (userEmail == null) return;

        User user = userRepository.findByEmail(userEmail.trim())
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        String currentIp = extractClientIp(request);
        String currentUa = request != null ? request.getHeader("User-Agent") : null;
        String currentBrowser = detectBrowser(currentUa);
        String currentOs = detectOs(currentUa);

        Optional<UserDeviceSession> currentSessionOpt = sessionRepository.findByUserAndIpAddressAndBrowserAndOs(user, currentIp, currentBrowser, currentOs);
        if (currentSessionOpt.isPresent()) {
            sessionRepository.deactivateAllByUserExceptCurrent(user, currentSessionOpt.get().getId());
        } else if (request != null) {
            UserDeviceSession current = recordSession(user, request, UUID.randomUUID().toString());
            if (current != null && current.getId() != null) {
                sessionRepository.deactivateAllByUserExceptCurrent(user, current.getId());
            }
        }
        log.info("Deactivated all other sessions for user {}", userEmail);
    }

    public String maskIpAddress(String ip) {
        if (ip == null || ip.isBlank()) return "Protected IP";
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            return "127.0.0.1 (Localhost)";
        }
        // IPv4 format: xxx.xxx.xxx.xxx -> xxx.xxx.•••.•••
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".•••.•••";
            }
        }
        // IPv6 or short fallback
        if (ip.length() > 6) {
            return ip.substring(0, 4) + "••••" + ip.substring(ip.length() - 2);
        }
        return "Protected IP";
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
        if (lower.contains("android")) return "Android";
        if (lower.contains("iphone") || lower.contains("ipad") || lower.contains("ios")) return "iOS";
        if (lower.contains("macintosh") || lower.contains("mac os x")) return "macOS";
        if (lower.contains("linux")) return "Linux";
        return "Desktop/Mobile OS";
    }

    private String detectDeviceType(String ua, String os) {
        if (os.contains("Android") || os.contains("iOS") || (ua != null && (ua.contains("Mobile") || ua.contains("Phone")))) {
            return "Mobile";
        }
        if (ua != null && (ua.contains("Tablet") || ua.contains("iPad"))) {
            return "Tablet";
        }
        return "Desktop";
    }

    private String determineLocationFromIp(String ip) {
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return "Localhost / India Network";
        }
        return "India (Secure Cloud)";
    }
}
