package com.redbus.config;

import com.redbus.entity.Operator;
import com.redbus.entity.User;
import com.redbus.repository.BusRepository;
import com.redbus.repository.OperatorRepository;
import com.redbus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OperatorRepository operatorRepository;
    private final BusRepository busRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // 1. Ensure primo_certified column exists on operators table
        try {
            jdbcTemplate.execute("ALTER TABLE operators ADD COLUMN IF NOT EXISTS primo_certified BOOLEAN DEFAULT FALSE");
        } catch (Exception e) {
            log.warn("Could not alter operators table for primo_certified: {}", e.getMessage());
        }

        // 2. Ensure default verified administrator is always initialized with password '123456'
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

        admin.setRole("ROLE_ADMIN");
        admin.setEmailVerified(true);
        admin.setPasswordHash(passwordEncoder.encode("123456"));
        userRepository.save(admin);

        // 3. Remove mocked / suspended operators with 0 buses (e.g. Zingbus Mobility, RedBus Administrator Travels)
        try {
            List<Operator> allOps = operatorRepository.findAll();
            for (Operator op : allOps) {
                int busCount = busRepository.findByOperatorId(op.getId()).size();
                boolean isMockZingbus = (op.getEmail() != null && op.getEmail().equalsIgnoreCase("operator@zingbus.com"))
                        || (op.getCompanyName() != null && op.getCompanyName().toLowerCase().contains("zingbus"));
                boolean isAdminAutoOp = (op.getEmail() != null && op.getEmail().equalsIgnoreCase("redbus@admin.in"))
                        || (op.getCompanyName() != null && op.getCompanyName().toLowerCase().contains("redbus administrator"));
                boolean isMockSuspended = "SUSPENDED".equalsIgnoreCase(op.getStatus()) && busCount == 0;

                if ((isMockZingbus || isAdminAutoOp || isMockSuspended) && busCount == 0) {
                    operatorRepository.delete(op);
                    log.info("Removed mocked/suspended operator from DB: ID={}, Name={}", op.getId(), op.getCompanyName());
                } else if (Boolean.TRUE.equals(op.getPrimoCertified()) && (op.getCommissionRate() == null || op.getCommissionRate().compareTo(BigDecimal.ZERO) > 0)) {
                    op.setCommissionRate(BigDecimal.ZERO);
                    operatorRepository.save(op);
                }
            }

            // Ensure at least one approved operator is Primo Certified if none are set yet, so search shows mixed Primo & normal buses
            List<Operator> remainingOps = operatorRepository.findAll();
            boolean anyPrimo = remainingOps.stream().anyMatch(op -> Boolean.TRUE.equals(op.getPrimoCertified()));
            if (!anyPrimo && !remainingOps.isEmpty()) {
                Operator targetPrimo = remainingOps.stream()
                        .filter(op -> "APPROVED".equalsIgnoreCase(op.getStatus()) && op.getCompanyName() != null && op.getCompanyName().toLowerCase().contains("intercity"))
                        .findFirst()
                        .orElseGet(() -> remainingOps.stream()
                                .filter(op -> "APPROVED".equalsIgnoreCase(op.getStatus()))
                                .findFirst()
                                .orElse(remainingOps.get(0)));
                targetPrimo.setPrimoCertified(true);
                targetPrimo.setCommissionRate(BigDecimal.ZERO);
                operatorRepository.save(targetPrimo);
                log.info("Initialized default Primo Bus Operator (0% Commission): {}", targetPrimo.getCompanyName());
            }
        } catch (Exception e) {
            log.warn("Cleanup of mocked operators encountered warning: {}", e.getMessage());
        }

        log.info("Default RedBus Admin account verified and active: email='{}', role='{}'", adminEmail, admin.getRole());
    }
}
