package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorBookingDto {
    private Long id;
    private String pnr;
    private String passengerName;
    private String passengerEmail;
    private String passengerPhone;
    private String route; // e.g. "Bangalore → Chennai"
    private LocalDate travelDate;
    private String busName;
    private List<String> seatNumbers;
    private Integer seatCount;
    private BigDecimal totalAmount;
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;
    private String status;
    private LocalDateTime bookingTime;
}
