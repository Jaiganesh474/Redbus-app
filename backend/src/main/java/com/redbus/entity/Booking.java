package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_bookings_user", columnList = "user_id"),
    @Index(name = "idx_bookings_pnr", columnList = "pnr"),
    @Index(name = "idx_bookings_operator", columnList = "operator_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id")
    private Long operatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(nullable = false, unique = true, length = 50)
    private String pnr;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "commission_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING_PAYMENT"; // 'PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'REFUNDED'

    @Column(name = "boarding_point")
    private String boardingPoint;

    @Column(name = "dropping_point")
    private String droppingPoint;

    @Column(name = "contact_email", nullable = false, length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "has_free_cancellation")
    @Builder.Default
    private Boolean hasFreeCancellation = false;

    @Column(name = "free_cancellation_fee", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal freeCancellationFee = BigDecimal.ZERO;

    @Column(name = "has_trip_guarantee")
    @Builder.Default
    private Boolean hasTripGuarantee = false;

    @Column(name = "trip_guarantee_fee", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tripGuaranteeFee = BigDecimal.ZERO;

    @Column(name = "service_fee", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "refund_status", length = 30)
    @Builder.Default
    private String refundStatus = "NONE"; // 'NONE', 'REQUESTED', 'AUDIT_PENDING', 'APPROVED', 'REFUNDED'

    @Column(name = "refund_destination", length = 30)
    @Builder.Default
    private String refundDestination = "WALLET"; // 'WALLET', 'ORIGINAL_PAYMENT'

    @Column(name = "refund_stage", length = 30)
    @Builder.Default
    private String refundStage = "NONE"; // 'NONE', 'REQUESTED', 'OPERATOR_AUDIT', 'REFUND_PROCESSING', 'COMPLETED'

    @Column(name = "refund_requested_at")
    private LocalDateTime refundRequestedAt;

    @Column(name = "refund_approved_at")
    private LocalDateTime refundApprovedAt;

    @Column(name = "wallet_amount_used", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal walletAmountUsed = BigDecimal.ZERO;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BookingPassenger> passengers = new ArrayList<>();
}
