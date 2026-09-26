package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorPassengerManifestDto {
    private Long bookingId;
    private String pnr;
    private String passengerName;
    private Integer age;
    private String gender;
    private String seatNumber;
    private String seatType;     // "SLEEPER", "SEATER"
    private String deck;         // "LOWER", "UPPER"
    private String berthLabel;   // "Lower Berth", "Upper Berth", "Seater"
    private String seatDisplay;  // "L1 (Lower Berth)", "U4 (Upper Berth)", "S12 (Seater)"
    private String boardingPoint;
    private String droppingPoint;
    private String contactPhone;
    private String contactEmail;
    private LocalDate travelDate;
    private String sourceCity;
    private String destinationCity;
    private String departureTime;
    private String arrivalTime;
    private String busOperator;
    private String busRegistration;
    private String busType;
    private String status;
}
