package com.redbus.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.50", message = "Discount percentage must be at least 0.5%")
    @DecimalMax(value = "90.00", message = "Discount percentage cannot exceed 90%")
    private BigDecimal discountPercentage;

    private BigDecimal maxDiscountAmount;

    private BigDecimal minBookingAmount;

    private LocalDate validFrom;

    private LocalDate validTo;

    private Integer usageLimit;
}
