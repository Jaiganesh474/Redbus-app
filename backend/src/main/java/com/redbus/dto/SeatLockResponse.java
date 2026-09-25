package com.redbus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLockResponse {
    private boolean success;
    private Long routeId;
    private List<Long> lockedSeatIds;
    private LocalDateTime lockExpiry;
    private long remainingSeconds;
    private String message;
}
