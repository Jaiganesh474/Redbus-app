package com.redbus.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicPricePredictionDto {
    private Long scheduleId;
    private String sourceCity;
    private String destinationCity;
    private BigDecimal basePrice;
    private BigDecimal currentDynamicPrice;
    private Double surgeMultiplier;
    private String demandLevel; // NORMAL, MODERATE, HIGH, PEAK_FESTIVE
    private Double occupancyPercentage;
    private Integer hoursUntilDeparture;
    private String reason;
    private List<PricePointDto> priceTrajectory;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PricePointDto {
        private String timeLabel;
        private BigDecimal price;
        private Double demandIndex;
    }
}
