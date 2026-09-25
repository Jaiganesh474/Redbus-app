package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLayoutDto {
    private Long routeId;
    private Long busId;
    private String operatorName;
    private String busType;
    private BigDecimal basePrice;
    private List<SeatDto> seats;
}
