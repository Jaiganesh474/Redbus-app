package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorWalletLedgerDto {
    private Long operatorId;
    private String companyName;
    private BigDecimal currentWalletBalance;
    private BigDecimal totalEarningsCredited;
    private BigDecimal totalRefundsDebited;
    private BigDecimal totalPlatformCommissionPaid;
    private List<WalletTransactionItem> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletTransactionItem {
        private Long id;
        private String pnr;
        private String type; // 'CREDIT_TICKET_FARE', 'DEBIT_REFUND_AUDIT', 'PAYOUT_WITHDRAWAL'
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String description;
        private LocalDateTime createdAt;
    }
}
