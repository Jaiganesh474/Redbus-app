package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupons_code", columnList = "code"),
    @Index(name = "idx_coupons_operator", columnList = "operator_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = new BigDecimal("2.00");

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_booking_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minBookingAmount = BigDecimal.ZERO;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name", length = 150)
    private String operatorName;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(name = "usage_limit", nullable = false)
    @Builder.Default
    private Integer usageLimit = 1000;

    @Column(name = "times_used", nullable = false)
    @Builder.Default
    private Integer timesUsed = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
