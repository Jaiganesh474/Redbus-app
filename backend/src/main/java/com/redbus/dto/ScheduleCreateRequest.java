package com.redbus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCreateRequest {
    @NotNull(message = "Bus ID is required")
    private Long busId;

    @NotBlank(message = "Source city is required")
    private String sourceCity;

    @NotBlank(message = "Destination city is required")
    private String destinationCity;

    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalTime arrivalTime;

    @NotNull(message = "Base price is required")
    private BigDecimal basePrice;

    private String operatingDays; // 'DAILY', 'MON,WED,FRI'
    private String boardingPoints;
    private String droppingPoints;

    @NotNull(message = "Valid from date is required")
    private LocalDate validFrom;

    @NotNull(message = "Valid to date is required")
    private LocalDate validTo;
}
