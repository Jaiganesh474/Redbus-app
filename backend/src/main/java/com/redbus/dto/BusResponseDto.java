package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusResponseDto {
    private Long id;
    private Long operatorId;
    private String operatorName;
    private String registrationNumber;
    private String busType;
    private Integer totalSeats;
    private String amenities;
    private String photoUrls;
    private Boolean active;
    private BigDecimal rating;
    private LocalDateTime createdAt;
}
