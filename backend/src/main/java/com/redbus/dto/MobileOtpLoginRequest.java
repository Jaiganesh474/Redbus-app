package com.redbus.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileOtpLoginRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "OTP code is required")
    private String otp;

    private String name;
}
