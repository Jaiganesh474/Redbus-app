package com.redbus.controller;

import com.redbus.dto.*;
import com.redbus.exception.BadRequestException;
import com.redbus.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/operator/register")
    public ResponseEntity<AuthResponse> registerOperator(@Valid @RequestBody OperatorRegisterRequest request) {
        AuthResponse response = authService.registerOperator(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        AuthResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.ok(Map.of("message", "Verification OTP sent successfully to your email"));
    }

    @PostMapping("/firebase-login")
    public ResponseEntity<AuthResponse> firebaseLogin(@Valid @RequestBody FirebaseLoginRequest request) {
        AuthResponse response = authService.firebaseLogin(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto currentUser = authService.getCurrentUser(authentication.getName());
        UserDto updated = authService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping(value = {"/profile/upload-avatar", "/upload-avatar"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Please select an image file to upload");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            throw new BadRequestException("Only image files (JPEG, PNG, WEBP, GIF) are allowed");
        }

        try {
            Path uploadDir = Paths.get("uploads", "avatars");
            File dir = uploadDir.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String cleanFileName = "avatar_" + UUID.randomUUID().toString().substring(0, 12) + extension;
            Path targetPath = uploadDir.resolve(cleanFileName);
            Files.copy(file.getInputStream(), targetPath);

            String fileUrl = "/uploads/avatars/" + cleanFileName;
            log.info("Successfully uploaded profile avatar: {}", fileUrl);

            return ResponseEntity.ok(Map.of(
                    "avatarUrl", fileUrl,
                    "fileName", cleanFileName,
                    "message", "Avatar image uploaded successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to upload avatar image: {}", e.getMessage());
            throw new BadRequestException("Failed to upload avatar image: " + e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset OTP sent to your email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        AuthResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/saved-travellers")
    public ResponseEntity<List<SavedTravellerDto>> getSavedTravellers(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto currentUser = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(authService.getSavedTravellers(currentUser.getId()));
    }

    @PostMapping("/saved-travellers")
    public ResponseEntity<SavedTravellerDto> addSavedTraveller(
            Authentication authentication,
            @Valid @RequestBody SavedTravellerDto request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto currentUser = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(authService.addSavedTraveller(currentUser.getId(), request));
    }

    @DeleteMapping("/saved-travellers/{id}")
    public ResponseEntity<Map<String, String>> deleteSavedTraveller(
            Authentication authentication,
            @PathVariable Long id) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto currentUser = authService.getCurrentUser(authentication.getName());
        authService.deleteSavedTraveller(currentUser.getId(), id);
        return ResponseEntity.ok(Map.of("message", "Saved traveller removed successfully"));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, String>> deactivateAccount(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.deactivateAccount(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Your account has been deactivated successfully"));
    }

    @PostMapping("/delete-account")
    public ResponseEntity<Map<String, String>> deleteAccount(
            Authentication authentication,
            @RequestBody(required = false) Map<String, String> body) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String password = body != null ? body.get("password") : null;
        authService.deleteAccount(authentication.getName(), password);
        return ResponseEntity.ok(Map.of("message", "Your account has been permanently deleted"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<Map<String, String>> deleteAccountRest(
            Authentication authentication,
            @RequestBody(required = false) Map<String, String> body) {
        return deleteAccount(authentication, body);
    }
}

