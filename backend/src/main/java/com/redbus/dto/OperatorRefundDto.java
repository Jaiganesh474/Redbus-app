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
public class OperatorRefundDto {
    private Long bookingId;
    private String pnr;
    private String passengerName;
    private String contactEmail;
    private String contactPhone;
    private String sourceCity;
    private String destinationCity;
    private LocalDate travelDate;
    private String busName;
    private List<String> seatNumbers;
    private BigDecimal totalPaid;
    private BigDecimal refundAmount;
    private String refundDestination; // 'WALLET' or 'ORIGINAL_PAYMENT'
    private String refundStatus; // 'REQUESTED', 'AUDIT_PENDING', 'APPROVED', 'REFUNDED'
    private String refundStage; // 'REQUESTED', 'OPERATOR_AUDIT', 'REFUND_PROCESSING', 'COMPLETED'
    private String cancellationReason;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
}
