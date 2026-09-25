package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponseDto {
    private Long id;
    private Long operatorId;
    private Long routeId;
    private Long busId;
    private String busName;
    private String busType;
    private String sourceCity;
    private String destinationCity;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String operatingDays;
    private BigDecimal basePrice;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String status;
    private Integer materializedTripsCount;
    private LocalDateTime createdAt;
}
