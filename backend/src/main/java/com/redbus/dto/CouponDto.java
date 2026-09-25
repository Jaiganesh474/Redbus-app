package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDto {
    private Long id;
    private String code;
    private BigDecimal discountPercentage;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minBookingAmount;
    private Long operatorId;
    private String operatorName;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer usageLimit;
    private Integer timesUsed;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
