package com.redbus.controller;

import com.redbus.dto.CityPairDto;
import com.redbus.dto.RouteResponseDto;
import com.redbus.dto.SeatLayoutDto;
import com.redbus.service.BusRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/routes", "/api/v1/routes"})
@RequiredArgsConstructor
public class RouteController {

    private final BusRouteService busRouteService;

    @GetMapping("/search")
    public ResponseEntity<List<RouteResponseDto>> searchRoutes(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String busType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String departureWindow,
            @RequestParam(required = false, defaultValue = "departure_asc") String sortBy
    ) {
        List<RouteResponseDto> routes = busRouteService.searchRoutes(
                source, destination, date, busType, minPrice, maxPrice, departureWindow, sortBy
        );
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<RouteResponseDto> getRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(busRouteService.getRouteById(routeId));
    }

    @GetMapping("/{routeId}/seats")
    public ResponseEntity<SeatLayoutDto> getRouteSeats(@PathVariable Long routeId) {
        return ResponseEntity.ok(busRouteService.getRouteSeats(routeId));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<CityPairDto>> getPopularRoutes() {
        return ResponseEntity.ok(busRouteService.getPopularRoutes());
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCities() {
        return ResponseEntity.ok(busRouteService.getAvailableCities());
    }
}
