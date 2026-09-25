package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String role = "ROLE_USER";

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "verification_token", length = 100)
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(length = 20)
    private String gender;

    @Column(name = "password_reset_token", length = 20)
    private String passwordResetToken;

    @Column(name = "password_reset_expiry")
    private LocalDateTime passwordResetExpiry;

    @Column(name = "wallet_balance", precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal walletBalance = java.math.BigDecimal.ZERO;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
