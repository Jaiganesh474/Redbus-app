package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity_logs", indexes = {
    @Index(name = "idx_activity_user", columnList = "user_id"),
    @Index(name = "idx_activity_session", columnList = "session_id"),
    @Index(name = "idx_activity_action", columnList = "action_type"),
    @Index(name = "idx_activity_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // SEARCH, VIEW_BUS, SEAT_SELECT, LOCK_SEAT, CHECKOUT_INITIATE, PAYMENT_ATTEMPT, PAYMENT_SUCCESS, BOOKING_CANCEL, CHATBOT_QUERY

    @Column(name = "route_id")
    private Long routeId;

    @Column(name = "bus_id")
    private Long busId;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "risk_score")
    @Builder.Default
    private Integer riskScore = 0; // 0 - 100

    @Column(name = "is_bot")
    @Builder.Default
    private Boolean isBot = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
