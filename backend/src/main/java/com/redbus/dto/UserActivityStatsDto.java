package com.redbus.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityStatsDto {
    private long totalEvents;
    private long activeUsersToday;
    private long botAttemptsBlocked;
    private double botTrafficPercentage;
    private double averageRiskScore;
    private List<Map<String, Object>> funnelMetrics;
    private List<Map<String, Object>> topActionsDistribution;
    private List<UserActivityDto> recentActivities;
    private List<UserActivityDto> highRiskActivities;
}
