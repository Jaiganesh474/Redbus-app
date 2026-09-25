package com.redbus.service;

import com.redbus.dto.SeatLockRequest;
import com.redbus.dto.SeatLockResponse;
import com.redbus.dto.SeatUnlockRequest;
import com.redbus.entity.RouteSeat;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.exception.SeatLockException;
import com.redbus.repository.RouteSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final RouteSeatRepository routeSeatRepository;

    @Value("${app.seat-lock.ttl-minutes:10}")
    private int ttlMinutes;

    @Transactional
    public SeatLockResponse lockSeats(SeatLockRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusMinutes(ttlMinutes);
        List<Long> lockedSeatIds = new ArrayList<>();

        for (Long seatId : request.getSeatIds()) {
            RouteSeat routeSeat = routeSeatRepository.findByRouteIdAndSeatId(request.getRouteId(), seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat " + seatId + " not found on route " + request.getRouteId()));

            // Check if seat is already booked
            if ("BOOKED".equalsIgnoreCase(routeSeat.getStatus())) {
                throw new SeatLockException("Seat " + routeSeat.getSeat().getSeatNumber() + " is already booked.");
            }

            // Check if seat is locked by someone else and lock hasn't expired yet
            if ("LOCKED".equalsIgnoreCase(routeSeat.getStatus()) && routeSeat.getLockExpiry() != null && routeSeat.getLockExpiry().isAfter(now)) {
                if (request.getUserId() == null || !request.getUserId().equals(routeSeat.getLockedByUserId())) {
                    throw new SeatLockException("Seat " + routeSeat.getSeat().getSeatNumber() + " is currently reserved by another passenger. Please select another seat.");
                }
            }

            // Acquire / extend lock
            routeSeat.setStatus("LOCKED");
            routeSeat.setLockExpiry(expiry);
            routeSeat.setLockedByUserId(request.getUserId());
            routeSeatRepository.save(routeSeat);
            lockedSeatIds.add(seatId);
        }

        return SeatLockResponse.builder()
                .success(true)
                .routeId(request.getRouteId())
                .lockedSeatIds(lockedSeatIds)
                .lockExpiry(expiry)
                .remainingSeconds(Duration.between(now, expiry).getSeconds())
                .message("Seats successfully reserved for " + ttlMinutes + " minutes.")
                .build();
    }

    @Transactional
    public boolean unlockSeats(SeatUnlockRequest request) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            return false;
        }

        for (Long seatId : request.getSeatIds()) {
            routeSeatRepository.findByRouteIdAndSeatId(request.getRouteId(), seatId).ifPresent(routeSeat -> {
                if ("LOCKED".equalsIgnoreCase(routeSeat.getStatus())) {
                    routeSeat.setStatus("AVAILABLE");
                    routeSeat.setLockExpiry(null);
                    routeSeat.setLockedByUserId(null);
                    routeSeatRepository.save(routeSeat);
                }
            });
        }
        return true;
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void releaseExpiredLocksJob() {
        LocalDateTime now = LocalDateTime.now();
        int released = routeSeatRepository.releaseExpiredLocks(now);
        if (released > 0) {
            log.info("Released {} expired seat locks at {}", released, now);
        }
    }
}
