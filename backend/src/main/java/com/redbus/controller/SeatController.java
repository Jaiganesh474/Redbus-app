package com.redbus.controller;

import com.redbus.dto.SeatLockRequest;
import com.redbus.dto.SeatLockResponse;
import com.redbus.dto.SeatUnlockRequest;
import com.redbus.service.SeatLockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/seats", "/api/v1/seats"})
@RequiredArgsConstructor
public class SeatController {

    private final SeatLockService seatLockService;

    @PostMapping("/lock")
    public ResponseEntity<SeatLockResponse> lockSeats(@Valid @RequestBody SeatLockRequest request) {
        SeatLockResponse response = seatLockService.lockSeats(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/unlock")
    public ResponseEntity<Map<String, Object>> unlockSeats(@RequestBody SeatUnlockRequest request) {
        boolean success = seatLockService.unlockSeats(request);
        return ResponseEntity.ok(Map.of("success", success, "message", "Seats unlocked"));
    }
}
