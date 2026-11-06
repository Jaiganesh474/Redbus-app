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
public class AdminStatsDto {
    private long totalBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private BigDecimal totalRevenue;
    private double occupancyRate;
    private long totalRoutes;
    private long totalBuses;
    private long primoOperatorsCount;
    private long primoBusesCount;
    private long activeSchedulesCount;
    private long approvedOperatorsCount;
    private List<CityPairDto> topRoutes;
}
