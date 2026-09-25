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
public class OperatorDto {
    private Long id;
    private Long userId;
    private String companyName;
    private String contactPerson;
    private String email;
    private String phone;
    private String kycDocUrl;
    private String bankAccountRef;
    private BigDecimal commissionRate;
    private String status;
    private LocalDateTime createdAt;
    private int totalBuses;
    private int totalSchedules;
    private long totalBookings;
    private BigDecimal totalRevenue;
}
