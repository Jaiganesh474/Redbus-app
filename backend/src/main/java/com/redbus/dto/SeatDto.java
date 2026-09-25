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
public class SeatDto {
    private Long id;
    private Long seatId;
    private String seatNumber;
    private String seatType; // 'SEATER', 'SLEEPER'
    private String deck; // 'LOWER', 'UPPER'
    private int rowNum;
    private int colNum;
    private String status; // 'AVAILABLE', 'LOCKED', 'BOOKED'
    private String genderRestriction; // 'NONE', 'FEMALE', 'MALE'
    private String bookedGender; // 'FEMALE', 'MALE'
    private BigDecimal price;
    private Long lockedByUserId;
}
