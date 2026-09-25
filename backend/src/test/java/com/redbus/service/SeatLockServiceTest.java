package com.redbus.service;

import com.redbus.dto.SeatLockRequest;
import com.redbus.dto.SeatLockResponse;
import com.redbus.entity.Bus;
import com.redbus.entity.Route;
import com.redbus.entity.RouteSeat;
import com.redbus.entity.Seat;
import com.redbus.exception.SeatLockException;
import com.redbus.repository.RouteSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatLockServiceTest {

    @Mock
    private RouteSeatRepository routeSeatRepository;

    @InjectMocks
    private SeatLockService seatLockService;

    private RouteSeat availableRouteSeat;
    private RouteSeat bookedRouteSeat;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seatLockService, "ttlMinutes", 10);

        Bus bus = Bus.builder().id(1L).operatorName("Zingbus").build();
        Route route = Route.builder().id(10L).bus(bus).build();
        Seat seat1 = Seat.builder().id(101L).seatNumber("L1").bus(bus).build();
        Seat seat2 = Seat.builder().id(102L).seatNumber("L2").bus(bus).build();

        availableRouteSeat = RouteSeat.builder()
                .id(1L)
                .route(route)
                .seat(seat1)
                .status("AVAILABLE")
                .build();

        bookedRouteSeat = RouteSeat.builder()
                .id(2L)
                .route(route)
                .seat(seat2)
                .status("BOOKED")
                .build();
    }

    @Test
    void testLockAvailableSeatSuccess() {
        when(routeSeatRepository.findByRouteIdAndSeatId(10L, 101L))
                .thenReturn(Optional.of(availableRouteSeat));

        SeatLockRequest request = SeatLockRequest.builder()
                .routeId(10L)
                .seatIds(List.of(101L))
                .userId(1L)
                .build();

        SeatLockResponse response = seatLockService.lockSeats(request);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getLockedSeatIds().size());
        assertEquals("LOCKED", availableRouteSeat.getStatus());
        assertNotNull(availableRouteSeat.getLockExpiry());
        verify(routeSeatRepository, times(1)).save(availableRouteSeat);
    }

    @Test
    void testLockBookedSeatThrowsException() {
        when(routeSeatRepository.findByRouteIdAndSeatId(10L, 102L))
                .thenReturn(Optional.of(bookedRouteSeat));

        SeatLockRequest request = SeatLockRequest.builder()
                .routeId(10L)
                .seatIds(List.of(102L))
                .userId(1L)
                .build();

        assertThrows(SeatLockException.class, () -> seatLockService.lockSeats(request));
        verify(routeSeatRepository, never()).save(bookedRouteSeat);
    }
}
