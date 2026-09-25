package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private Long id;
    private String pnr;
    private Long routeId;
    private String sourceCity;
    private String destinationCity;
    private LocalDate travelDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String operatorName;
    private String busType;
    private BigDecimal totalAmount;
    private String status;
    private String boardingPoint;
    private String droppingPoint;
    private String contactEmail;
    private String contactPhone;
    private String cancellationReason;
    private BigDecimal refundAmount;
    private BigDecimal walletAmountUsed;
    private String couponCode;
    private BigDecimal discountAmount;
    private Boolean hasFreeCancellation;
    private BigDecimal freeCancellationFee;
    private Boolean hasTripGuarantee;
    private BigDecimal tripGuaranteeFee;
    private BigDecimal serviceFee;
    private LocalDateTime createdAt;
    private List<PassengerDto> passengers;
}
