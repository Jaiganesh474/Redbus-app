package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPaymentResponse {
    private boolean success;
    private String pnr;
    private String bookingStatus;
    private String message;
}
