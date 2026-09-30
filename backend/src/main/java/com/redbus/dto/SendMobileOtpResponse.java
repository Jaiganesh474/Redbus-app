package com.redbus.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMobileOtpResponse {
    private boolean success;
    private String message;
    private String phone;
    private long expiresInSeconds;
    private String previewOtp;
}
