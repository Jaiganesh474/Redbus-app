package com.redbus.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSeatRecommendationDto {
    private Long scheduleId;
    private List<RecommendedSeatItem> recommendedSeats;
    private List<String> femaleSafeSeatNumbers;
    private List<String> quietZoneSeatNumbers;
    private List<String> panoramicWindowSeatNumbers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendedSeatItem {
        private String seatNumber;
        private Double matchScore; // e.g. 0.98
        private String badge; // "Top Comfort", "Solo Female Safe", "Panoramic View", "Easy Boarding"
        private String reason;
    }
}
