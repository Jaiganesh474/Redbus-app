package com.redbus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusCreateRequest {
    @NotBlank(message = "Operator or bus brand name is required")
    private String operatorName;

    @NotBlank(message = "Bus type is required (e.g. AC Sleeper 2+1, AC Seater 2+2)")
    private String busType;

    private String registrationNumber;

    @NotNull(message = "Total seats count is required")
    private Integer totalSeats;

    private String amenities;
    private String photoUrls;
}
