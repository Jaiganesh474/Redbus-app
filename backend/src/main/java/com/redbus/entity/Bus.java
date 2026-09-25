package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "buses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(name = "operator_name", nullable = false, length = 150)
    private String operatorName;

    @Column(name = "bus_type", nullable = false, length = 100)
    private String busType;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrls;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal rating = new BigDecimal("4.5");

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
