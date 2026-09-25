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
public class CityPairDto {
    private String sourceCity;
    private String destinationCity;
    private long busCount;
    private BigDecimal minPrice;
}
