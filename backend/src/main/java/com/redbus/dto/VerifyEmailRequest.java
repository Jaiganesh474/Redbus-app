package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailRequest {
    private String email;
    private String token;
    private String tokenOrOtp;

    public String resolveToken() {
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        if (tokenOrOtp != null && !tokenOrOtp.isBlank()) {
            return tokenOrOtp.trim();
        }
        return null;
    }
}
