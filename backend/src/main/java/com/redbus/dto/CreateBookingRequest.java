package com.redbus.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "routeId is required")
    private Long routeId;

    @NotBlank(message = "boardingPoint is required")
    private String boardingPoint;

    @NotBlank(message = "droppingPoint is required")
    private String droppingPoint;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    private String contactEmail;

    @NotBlank(message = "Contact phone is required")
    private String contactPhone;

    @NotEmpty(message = "At least one passenger must be specified")
    @Valid
    private List<PassengerDto> passengers;

    private String couponCode;

    private Boolean hasFreeCancellation;

    private Boolean hasTripGuarantee;

    private java.math.BigDecimal serviceFee;

    private Boolean useWalletBalance;
}
