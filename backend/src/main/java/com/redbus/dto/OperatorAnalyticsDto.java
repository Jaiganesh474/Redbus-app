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
public class OperatorAnalyticsDto {
    private String operatorName;
    private String companyName;
    private BigDecimal totalRevenue;
    private BigDecimal netEarnings;
    private BigDecimal commissionPaid;
    private Long ticketsSold;
    private Long activeFleetCount;
    private Long activeSchedulesCount;
    private Long totalTripsRun;
    private BigDecimal averageOccupancyPercentage;

    private List<RouteStat> routePerformance;
    private List<BusStat> busPerformance;
    private List<DailyTimelineStat> dailyTimeline;
    private List<OperatorBookingDto> recentBookings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteStat {
        private String routeName;
        private Long bookingCount;
        private Long ticketsSold;
        private BigDecimal revenue;
        private BigDecimal occupancyPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusStat {
        private String registrationNumber;
        private String busType;
        private Long tripsCount;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTimelineStat {
        private String date;
        private BigDecimal revenue;
        private Long ticketsSold;
    }
}
