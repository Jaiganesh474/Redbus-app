package com.redbus.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelayPredictionDto {
    private Long scheduleId;
    private String routeName;
    private Integer predictedDepartureDelayMinutes;
    private Integer predictedArrivalDelayMinutes;
    private Double onTimeProbability; // e.g., 0.96 (96%)
    private String punctualityGrade; // "EXCELLENT", "GOOD", "MODERATE_RISK"
    private String trafficCondition; // "SMOOTH", "MODERATE_PEAK", "HEAVY_HIGHWAY"
    private String weatherRisk; // "CLEAR", "MIST_FOG", "RAIN"
    private String confidenceScore; // "94% Confidence"
    private String aiExplanation;
}
