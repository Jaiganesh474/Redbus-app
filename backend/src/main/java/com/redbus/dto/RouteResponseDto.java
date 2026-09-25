package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponseDto {
    private Long id;
    private Long busId;
    private String operatorName;
    private String busType;
    private BigDecimal rating;
    private List<String> amenities;
    private String sourceCity;
    private String destinationCity;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private LocalDate travelDate;
    private BigDecimal durationHours;
    private BigDecimal basePrice;
    private int availableSeats;
    private List<String> boardingPoints;
    private List<String> droppingPoints;
    private String busPhotoUrl;
}
