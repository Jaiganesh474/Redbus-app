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
public class CouponValidationResponse {
    private boolean valid;
    private String code;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal finalPayableAmount;
    private String message;
}
