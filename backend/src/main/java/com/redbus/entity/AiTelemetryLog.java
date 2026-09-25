package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_telemetry_logs", indexes = {
    @Index(name = "idx_ai_telemetry_session", columnList = "session_id"),
    @Index(name = "idx_ai_telemetry_type", columnList = "request_type"),
    @Index(name = "idx_ai_telemetry_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTelemetryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "request_type", nullable = false, length = 50)
    private String requestType; // CHAT, NLP_QUERY_PARSE, SUPPORT_RAG, RECOMMENDATION, IMAGE_GENERATION

    @Column(name = "query_text", columnDefinition = "TEXT")
    private String queryText;

    @Column(name = "response_summary", length = 500)
    private String responseSummary;

    @Column(name = "latency_ms", nullable = false)
    @Builder.Default
    private Long latencyMs = 0L;

    @Column(name = "tokens_used")
    @Builder.Default
    private Integer tokensUsed = 0;

    @Column(name = "model_used", length = 50)
    private String modelUsed;

    @Column(name = "confidence_score")
    @Builder.Default
    private Double confidenceScore = 0.95;

    @Column(length = 30)
    @Builder.Default
    private String sentiment = "NEUTRAL"; // POSITIVE, NEUTRAL, FRUSTRATED, URGENT

    @Column(length = 50)
    @Builder.Default
    private String intent = "GENERAL_QUERY"; // BOOKING_SEARCH, PNR_LOOKUP, CANCELLATION, POLICY_FAQ, PRICE_ESTIMATE

    @Column(name = "is_fallback")
    @Builder.Default
    private Boolean isFallback = false;

    @Column(name = "is_anomaly")
    @Builder.Default
    private Boolean isAnomaly = false;

    @Column(name = "safety_flag", length = 50)
    @Builder.Default
    private String safetyFlag = "CLEAN"; // CLEAN, SUSPICIOUS_INJECTION, HIGH_RATE

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
