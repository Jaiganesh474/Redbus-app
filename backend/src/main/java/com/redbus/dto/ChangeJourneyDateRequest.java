package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeJourneyDateRequest {
    private LocalDate newTravelDate;
    private Long newRouteId;
    private List<String> newSeatNumbers;
}

