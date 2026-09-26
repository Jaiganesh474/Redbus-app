package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operator_wallet_transactions", indexes = {
    @Index(name = "idx_op_wallet_op", columnList = "operator_id"),
    @Index(name = "idx_op_wallet_pnr", columnList = "pnr")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorWalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(length = 50)
    private String pnr;

    @Column(nullable = false, length = 30)
    private String type; // 'CREDIT_TICKET_FARE', 'DEBIT_REFUND_AUDIT', 'PAYOUT_WITHDRAWAL'

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
