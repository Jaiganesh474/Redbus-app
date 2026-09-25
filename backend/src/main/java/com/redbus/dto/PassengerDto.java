package com.redbus.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class PassengerDto {
    @NotNull(message = "seatId is required")
    private Long seatId;

    @NotBlank(message = "seatNumber is required")
    private String seatNumber;

    @NotBlank(message = "Passenger name is required")
    private String name;

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 120, message = "Age must be valid")
    private Integer age;

    @NotBlank(message = "Gender is required")
    private String gender; // 'MALE', 'FEMALE', 'OTHER'
}
