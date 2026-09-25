package com.redbus.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityDto {
    private Long id;
    private Long userId;
    private String userEmail;
    private String sessionId;
    private String ipAddress;
    private String userAgent;
    private String actionType;
    private Long routeId;
    private Long busId;
    private Long scheduleId;
    private String metadataJson;
    private Integer riskScore;
    private Boolean isBot;
    private LocalDateTime createdAt;
}
