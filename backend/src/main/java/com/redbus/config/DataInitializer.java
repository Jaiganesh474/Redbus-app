package com.redbus.config;

import com.redbus.entity.User;
import com.redbus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Ensure default verified administrator is always initialized with password '123456'
        String adminEmail = "redbus@admin.in";
        User admin = userRepository.findByEmail(adminEmail).orElseGet(() -> {
            log.info("Creating default administrator account: {}", adminEmail);
            return User.builder()
                    .name("RedBus Administrator")
                    .email(adminEmail)
                    .phone("+91 9999999999")
                    .role("ROLE_ADMIN")
                    .emailVerified(true)
                    .avatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150")
                    .gender("MALE")
                    .passwordHash(passwordEncoder.encode("123456"))
                    .build();
        });

        // Ensure role, password, and email_verified are always strictly verified
        admin.setRole("ROLE_ADMIN");
        admin.setEmailVerified(true);
        admin.setPasswordHash(passwordEncoder.encode("123456"));
        userRepository.save(admin);
        log.info("Default RedBus Admin account verified and active: email='{}', role='{}'", adminEmail, admin.getRole());
    }
}
