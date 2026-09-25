package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "routes", indexes = {
    @Index(name = "idx_routes_search", columnList = "source_city, destination_city, travel_date"),
    @Index(name = "idx_routes_cities", columnList = "source_city, destination_city"),
    @Index(name = "idx_routes_operator", columnList = "operator_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id")
    private Long operatorId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(name = "source_city", nullable = false, length = 100)
    private String sourceCity;

    @Column(name = "destination_city", nullable = false, length = 100)
    private String destinationCity;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "duration_hours", precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal durationHours = new BigDecimal("6.5");

    @Column(name = "distance_km", precision = 6, scale = 1)
    private BigDecimal distanceKm;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "boarding_points", columnDefinition = "TEXT")
    private String boardingPoints;

    @Column(name = "dropping_points", columnDefinition = "TEXT")
    private String droppingPoints;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
