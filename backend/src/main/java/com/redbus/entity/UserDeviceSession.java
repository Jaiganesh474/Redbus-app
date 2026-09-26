package com.redbus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_device_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeviceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_name", length = 150)
    private String deviceName;

    @Column(name = "device_type", length = 50)
    @Builder.Default
    private String deviceType = "Desktop";

    @Column(length = 100)
    private String browser;

    @Column(length = 100)
    private String os;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(length = 150)
    @Builder.Default
    private String location = "India";

    @Column(name = "last_active_at")
    @Builder.Default
    private LocalDateTime lastActiveAt = LocalDateTime.now();

    @Column(name = "is_current_session")
    @Builder.Default
    private Boolean isCurrentSession = false;

    @Column(name = "session_token")
    private String sessionToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
