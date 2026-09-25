package com.redbus.service;

import com.redbus.config.JwtUtil;
import com.redbus.dto.*;
import com.redbus.entity.SavedTraveller;
import com.redbus.entity.User;
import com.redbus.exception.BadRequestException;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.repository.SavedTravellerRepository;
import com.redbus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SavedTravellerRepository savedTravellerRepository;
    private final com.redbus.repository.OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        
        // If user exists and is already verified, inform them cleanly
        if (userRepository.existsByEmail(email)) {
            User existing = userRepository.findByEmail(email).get();
            if (Boolean.TRUE.equals(existing.getEmailVerified())) {
                throw new BadRequestException("An account with this email is already registered. Please sign in.");
            }
            
            // If user exists but is unverified, refresh OTP and allow them to verify!
            String otp = String.format("%06d", random.nextInt(900000) + 100000);
            existing.setName(request.getName().trim());
            existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            if (request.getPhone() != null) existing.setPhone(request.getPhone());
            existing.setVerificationToken(otp);
            existing.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
            User saved = userRepository.save(existing);

            try {
                emailService.sendVerificationEmail(saved, otp);
            } catch (Exception e) {
                log.warn("Failed to dispatch verification email: {}", e.getMessage());
            }

            String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole(), saved.getId());
            return AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .user(mapToDto(saved))
                    .build();
        }

        String role = "ROLE_USER";
        if (request.getRole() != null && !request.getRole().isBlank()) {
            String requestedRole = request.getRole().trim().toUpperCase();
            if (requestedRole.equals("ROLE_OPERATOR") || requestedRole.equals("ROLE_ADMIN") || requestedRole.equals("ROLE_USER")) {
                role = requestedRole;
            }
        }

        // Generate 6-digit OTP code for email verification
        String otp = String.format("%06d", random.nextInt(900000) + 100000);

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .emailVerified(false)
                .verificationToken(otp)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(24))
                .build();

        User saved = userRepository.save(user);

        // Send Brevo verification email
        try {
            emailService.sendVerificationEmail(saved, otp);
        } catch (Exception e) {
            log.warn("Failed to dispatch verification email: {}", e.getMessage());
        }

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole(), saved.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToDto(saved))
                .build();
    }

    @Transactional
    public AuthResponse registerOperator(OperatorRegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .name(request.getContactPerson().trim())
                    .email(email)
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .phone(request.getPhone().trim())
                    .role("ROLE_OPERATOR")
                    .emailVerified(true)
                    .build();
            return userRepository.save(newUser);
        });

        // Upgrade role to ROLE_OPERATOR if existing
        user.setRole("ROLE_OPERATOR");
        user.setEmailVerified(true);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        User savedUser = userRepository.save(user);

        // Create or update Operator record
        com.redbus.entity.Operator op = operatorRepository.findByUserId(savedUser.getId()).orElseGet(() -> {
            return com.redbus.entity.Operator.builder()
                    .user(savedUser)
                    .companyName(request.getCompanyName().trim())
                    .contactPerson(request.getContactPerson().trim())
                    .email(email)
                    .phone(request.getPhone().trim())
                    .commissionRate(new java.math.BigDecimal("10.00"))
                    .status("PENDING")
                    .build();
        });
        op.setCompanyName(request.getCompanyName().trim());
        op.setContactPerson(request.getContactPerson().trim());
        op.setPhone(request.getPhone().trim());
        operatorRepository.save(op);

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole(), savedUser.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToDto(savedUser))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        // Check if email is verified
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            String otp = String.format("%06d", random.nextInt(900000) + 100000);
            user.setVerificationToken(otp);
            user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
            userRepository.save(user);
            try {
                emailService.sendVerificationEmail(user, otp);
            } catch (Exception e) {
                log.warn("Failed to dispatch verification email on login: {}", e.getMessage());
            }
            throw new BadRequestException("Please verify your email address. A 6-digit verification code has been dispatched to " + user.getEmail());
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToDto(user))
                .build();
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String tokenOrOtp = request.resolveToken();
        if (tokenOrOtp == null || tokenOrOtp.isBlank()) {
            throw new BadRequestException("Verification code is required");
        }

        User user;
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                    .orElseThrow(() -> new BadRequestException("No account found with email: " + request.getEmail()));

            if (user.getVerificationToken() == null || !user.getVerificationToken().trim().equalsIgnoreCase(tokenOrOtp.trim())) {
                throw new BadRequestException("Invalid verification code for this email");
            }
        } else {
            user = userRepository.findByVerificationToken(tokenOrOtp.trim())
                    .orElseThrow(() -> new BadRequestException("Invalid or expired verification code"));
        }

        if (user.getVerificationTokenExpiry() != null && user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification code has expired. Please request a new one.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        User saved = userRepository.save(user);

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole(), saved.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToDto(saved))
                .build();
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("This email is already verified. You can log in directly.");
        }

        String otp = String.format("%06d", random.nextInt(900000) + 100000);
        user.setVerificationToken(otp);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        emailService.sendVerificationEmail(user, otp);
    }

    /**
     * Firebase Google Authentication (no email verification needed, Google pre-verifies)
     */
    @Transactional
    public AuthResponse firebaseLogin(FirebaseLoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            String defaultName = (request.getName() != null && !request.getName().isBlank())
                    ? request.getName().trim()
                    : email.split("@")[0];

            User newUser = User.builder()
                    .name(defaultName)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .role("ROLE_USER")
                    .emailVerified(true) // Google authentication is pre-verified!
                    .build();
            return userRepository.save(newUser);
        });

        // Ensure user is marked verified if logging in via Google
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            user = userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToDto(user))
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email: " + request.getEmail()));

        String otp = String.format("%06d", random.nextInt(900000) + 100000);
        user.setPasswordResetToken(otp);
        user.setPasswordResetExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        emailService.sendPasswordResetOtpEmail(user, otp);
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No account found with email: " + request.getEmail()));

        if (user.getPasswordResetToken() == null || !user.getPasswordResetToken().trim().equalsIgnoreCase(request.getOtp().trim())) {
            throw new BadRequestException("Invalid password reset OTP code");
        }

        if (user.getPasswordResetExpiry() != null && user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset OTP has expired. Please request a new one.");
        }

        if (request.getNewPassword().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters long");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        User saved = userRepository.save(user);

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole(), saved.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToDto(saved))
                .build();
    }

    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender().trim().toUpperCase());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new BadRequestException("Current password does not match");
            }
            if (request.getNewPassword().length() < 6) {
                throw new BadRequestException("New password must be at least 6 characters long");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        User updated = userRepository.save(user);
        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public List<SavedTravellerDto> getSavedTravellers(Long userId) {
        return savedTravellerRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(t -> SavedTravellerDto.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .age(t.getAge())
                        .gender(t.getGender())
                        .build())
                .toList();
    }

    @Transactional
    public SavedTravellerDto addSavedTraveller(Long userId, SavedTravellerDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        SavedTraveller traveller = SavedTraveller.builder()
                .user(user)
                .name(dto.getName().trim())
                .age(dto.getAge())
                .gender(dto.getGender().trim().toUpperCase())
                .build();

        SavedTraveller saved = savedTravellerRepository.save(traveller);
        return SavedTravellerDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .age(saved.getAge())
                .gender(saved.getGender())
                .build();
    }

    @Transactional
    public void deleteSavedTraveller(Long userId, Long travellerId) {
        SavedTraveller traveller = savedTravellerRepository.findByIdAndUserId(travellerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved traveller not found with ID: " + travellerId));
        savedTravellerRepository.delete(traveller);
    }

    public User getAuthenticatedUser() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return userRepository.findByEmail("operator@zingbus.com")
                    .orElseGet(() -> userRepository.findAll().stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("No user found")));
        }
        String email = auth.getName();
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDto(user);
    }

    public UserDto mapToDto(User user) {
        String opStatus = null;
        if (user.getRole() != null && user.getRole().contains("OPERATOR")) {
            opStatus = operatorRepository.findByUserId(user.getId())
                    .map(com.redbus.entity.Operator::getStatus)
                    .orElse("PENDING");
        }

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .operatorStatus(opStatus)
                .walletBalance(user.getWalletBalance() != null ? user.getWalletBalance() : java.math.BigDecimal.ZERO)
                .build();
    }
}
