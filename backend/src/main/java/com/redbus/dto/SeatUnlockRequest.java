package com.redbus.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatUnlockRequest {
    @NotNull(message = "routeId is required")
    private Long routeId;
    private List<Long> seatIds;
    private Long userId;
}
