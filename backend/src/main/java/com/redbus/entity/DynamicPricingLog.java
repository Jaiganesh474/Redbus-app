package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ml_dynamic_pricing_logs", indexes = {
    @Index(name = "idx_pricing_schedule", columnList = "schedule_id"),
    @Index(name = "idx_pricing_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicPricingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "source_city", length = 100)
    private String sourceCity;

    @Column(name = "destination_city", length = 100)
    private String destinationCity;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "predicted_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal predictedPrice;

    @Column(name = "surge_multiplier", nullable = false)
    @Builder.Default
    private Double surgeMultiplier = 1.0;

    @Column(name = "demand_factor", nullable = false)
    @Builder.Default
    private Double demandFactor = 1.0;

    @Column(name = "occupancy_rate")
    @Builder.Default
    private Double occupancyRate = 0.0;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
