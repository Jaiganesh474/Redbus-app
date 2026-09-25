package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedules", indexes = {
    @Index(name = "idx_schedules_operator", columnList = "operator_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = true)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(name = "source_city", length = 100)
    private String sourceCity;

    @Column(name = "destination_city", length = 100)
    private String destinationCity;

    @Column(name = "boarding_points", columnDefinition = "TEXT")
    private String boardingPoints;

    @Column(name = "dropping_points", columnDefinition = "TEXT")
    private String droppingPoints;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "operating_days", nullable = false, length = 100)
    @Builder.Default
    private String operatingDays = "DAILY"; // 'DAILY', 'MON,WED,FRI', etc.

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // 'ACTIVE', 'PAUSED'

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
