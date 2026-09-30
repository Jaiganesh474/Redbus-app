package com.redbus.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMobileOtpRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;

    @Builder.Default
    private String purpose = "LOGIN"; // LOGIN, RESET_PASSWORD, REGISTER
}
