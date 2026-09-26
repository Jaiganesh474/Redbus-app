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
public class AdminOperatorEarningsDto {
    private BigDecimal systemGrossRevenue;
    private BigDecimal systemNetOperatorPayouts;
    private BigDecimal systemCommissionsCollected;
    private BigDecimal systemTotalRefundsProcessed;
    private List<OperatorEarningItem> operatorEarnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperatorEarningItem {
        private Long operatorId;
        private String companyName;
        private String contactPerson;
        private String email;
        private String phone;
        private String status;
        private int totalBuses;
        private int totalRoutes;
        private long totalConfirmedBookings;
        private long totalCancelledBookings;
        private BigDecimal grossRevenue;
        private BigDecimal commissionPaid;
        private BigDecimal netEarnings;
        private BigDecimal walletBalance;
        private BigDecimal totalRefundsApproved;
    }
}
