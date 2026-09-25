package com.redbus.dto;

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
public class NlpParseResponse {
    private String sourceCity;
    private String destinationCity;
    private LocalDate travelDate;
    private String busType;
    private BigDecimal maxPrice;
    private String timePreference; // 'MORNING', 'AFTERNOON', 'EVENING', 'NIGHT'
    private String rawQuery;
}
