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
public class PaymentOrderResponse {
    private String orderId;
    private String currency;
    private Long amountInPaise;
    private BigDecimal amount;
    private String keyId;
    private String pnr;
}
