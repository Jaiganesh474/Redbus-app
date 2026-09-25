package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingResponse {
    private String pnr;
    private String status;
    private BigDecimal refundAmount;
    private BigDecimal walletBalance;
    private String message;
}
