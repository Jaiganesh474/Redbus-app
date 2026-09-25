package com.redbus.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMonitoringStatsDto {
    private long totalQueries;
    private long activeToday;
    private double avgLatencyMs;
    private long p50LatencyMs;
    private long p95LatencyMs;
    private long p99LatencyMs;
    private double successRate;
    private double fallbackRate;
    private double avgConfidence;
    private long totalTokensConsumed;
    private double estimatedCostUsd;
    private long anomalyCount;
    private String primaryModel;
    private List<Map<String, Object>> intentDistribution;
    private List<Map<String, Object>> sentimentDistribution;
    private List<AiTelemetryDto> recentLogs;
}
