package com.redbus.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTelemetryDto {
    private Long id;
    private String sessionId;
    private Long userId;
    private String requestType;
    private String queryText;
    private String responseSummary;
    private Long latencyMs;
    private Integer tokensUsed;
    private String modelUsed;
    private Double confidenceScore;
    private String sentiment;
    private String intent;
    private Boolean isFallback;
    private Boolean isAnomaly;
    private String safetyFlag;
    private LocalDateTime createdAt;
}
