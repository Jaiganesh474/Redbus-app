package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorAiPriceIntelligenceDto {
    private String corridor; // e.g. "Bangalore ➔ Chennai"
    private Long myRouteId;
    private String myBusType;
    private BigDecimal myCurrentPrice;
    private BigDecimal marketAveragePrice;
    private BigDecimal marketLowestPrice;
    private BigDecimal marketHighestPrice;
    private BigDecimal priceDifferencePercentage; // e.g. -7.5% (cheaper) or +12.0% (premium)
    private String priceCompetitiveness; // "HIGHLY_COMPETITIVE", "OPTIMAL", "PREMIUM", "OVERPRICED"
    private String aiRecommendation;
    private String suggestedPromoCode;
    private BigDecimal suggestedPromoDiscount;
    private Integer predictedDemandOccupancy; // e.g. 88%
    private List<CompetitorBenchmark> competitorBenchmarks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompetitorBenchmark {
        private String operatorName;
        private String busType;
        private BigDecimal price;
        private BigDecimal rating;
        private BigDecimal differenceFromMe;
    }
}
