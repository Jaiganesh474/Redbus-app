package com.redbus.dto;

import jakarta.validation.constraints.NotEmpty;
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
public class SeatLockRequest {
    @NotNull(message = "routeId is required")
    private Long routeId;

    @NotEmpty(message = "At least one seatId must be provided")
    private List<Long> seatIds;

    private Long userId;
}
