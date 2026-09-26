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
public class CancelBookingRequest {
    private String reason;
    private String refundDestination; // 'WALLET' or 'ORIGINAL_PAYMENT'
}
