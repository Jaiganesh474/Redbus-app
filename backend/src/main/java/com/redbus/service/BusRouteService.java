package com.redbus.service;

import com.redbus.dto.*;
import com.redbus.entity.*;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusRouteService {

    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final RouteSeatRepository routeSeatRepository;
    private final ScheduleRepository scheduleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncAllActiveSchedulesOnStartup() {
        try {
            List<Schedule> activeSchedules = scheduleRepository.findByStatus("ACTIVE");
            LocalDate today = LocalDate.now();
            int count = 0;
            for (Schedule schedule : activeSchedules) {
                LocalDate start = schedule.getValidFrom() != null && schedule.getValidFrom().isAfter(today)
                        ? schedule.getValidFrom() : today;
                LocalDate end = schedule.getValidTo() != null && schedule.getValidTo().isBefore(today.plusDays(30))
                        ? schedule.getValidTo() : today.plusDays(30);

                LocalDate current = start;
                while (!current.isAfter(end)) {
                    if (ensureScheduleMaterialized(schedule, current)) {
                        count++;
                    }
                    current = current.plusDays(1);
                }
            }
            log.info("Startup sync: Materialized {} active schedule route dates.", count);
        } catch (Exception e) {
            log.warn("Error during startup schedule synchronization: {}", e.getMessage());
        }
    }

    @Transactional
    public boolean ensureScheduleMaterialized(Schedule schedule, LocalDate date) {
        if (schedule == null || schedule.getBus() == null) return false;
        if (!"ACTIVE".equalsIgnoreCase(schedule.getStatus())) return false;

        if (schedule.getValidFrom() != null && date.isBefore(schedule.getValidFrom())) return false;
        if (schedule.getValidTo() != null && date.isAfter(schedule.getValidTo())) return false;
        if (!isOperatingOnDate(schedule.getOperatingDays(), date)) return false;

        Bus bus = schedule.getBus();
        List<Route> existing = routeRepository.findByBusIdAndTravelDateAndDepartureTime(
                bus.getId(), date, schedule.getDepartureTime()
        );

        if (existing.isEmpty()) {
            String boarding = schedule.getBoardingPoints() != null && !schedule.getBoardingPoints().isBlank()
                    ? schedule.getBoardingPoints()
                    : schedule.getSourceCity() + " Central (08:00), " + schedule.getSourceCity() + " Bypass (08:30)";
            String dropping = schedule.getDroppingPoints() != null && !schedule.getDroppingPoints().isBlank()
                    ? schedule.getDroppingPoints()
                    : schedule.getDestinationCity() + " City Center (14:00), " + schedule.getDestinationCity() + " Main Terminal (14:30)";

            Route route = Route.builder()
                    .operatorId(schedule.getOperator() != null ? schedule.getOperator().getId() : bus.getOperatorId())
                    .bus(bus)
                    .sourceCity(schedule.getSourceCity())
                    .destinationCity(schedule.getDestinationCity())
                    .departureTime(schedule.getDepartureTime())
                    .arrivalTime(schedule.getArrivalTime())
                    .travelDate(date)
                    .basePrice(schedule.getBasePrice())
                    .durationHours(new BigDecimal("6.5"))
                    .boardingPoints(boarding)
                    .droppingPoints(dropping)
                    .build();

            Route savedRoute = routeRepository.save(route);

            List<Seat> templateSeats = seatRepository.findByBusIdOrderByDeckAscRowNumAscColNumAsc(bus.getId());
            if (templateSeats.isEmpty()) {
                generateBusSeats(bus);
                templateSeats = seatRepository.findByBusIdOrderByDeckAscRowNumAscColNumAsc(bus.getId());
            }

            List<RouteSeat> routeSeats = templateSeats.stream()
                    .map(s -> RouteSeat.builder()
                            .route(savedRoute)
                            .seat(s)
                            .status("AVAILABLE")
                            .genderRestriction("NONE")
                            .build())
                    .collect(Collectors.toList());

            routeSeatRepository.saveAll(routeSeats);

            if (schedule.getRoute() == null) {
                schedule.setRoute(savedRoute);
                scheduleRepository.save(schedule);
            }
            return true;
        }
        return false;
    }

    private boolean isOperatingOnDate(String operatingDays, LocalDate date) {
        if (operatingDays == null || operatingDays.equalsIgnoreCase("DAILY")) {
            return true;
        }
        DayOfWeek dow = date.getDayOfWeek();
        String dayName = dow.name(); // MONDAY, TUESDAY, etc.
        return operatingDays.toUpperCase().contains(dayName.substring(0, 3));
    }

    private void generateBusSeats(Bus bus) {
        boolean isSleeper = bus.getBusType().toUpperCase().contains("SLEEPER");
        int total = bus.getTotalSeats() != null ? bus.getTotalSeats() : 30;
        List<Seat> seats = new ArrayList<>();

        if (isSleeper) {
            int lowerCount = total / 2;
            int upperCount = total - lowerCount;

            for (int i = 1; i <= lowerCount; i++) {
                int row = (i - 1) / 3 + 1;
                int col = (i - 1) % 3 + 1;
                seats.add(Seat.builder()
                        .bus(bus)
                        .seatNumber("L" + i)
                        .seatType("SLEEPER")
                        .deck("LOWER")
                        .rowNum(row)
                        .colNum(col)
                        .build());
            }

            for (int i = 1; i <= upperCount; i++) {
                int row = (i - 1) / 3 + 1;
                int col = (i - 1) % 3 + 1;
                seats.add(Seat.builder()
                        .bus(bus)
                        .seatNumber("U" + i)
                        .seatType("SLEEPER")
                        .deck("UPPER")
                        .rowNum(row)
                        .colNum(col)
                        .build());
            }
        } else {
            for (int i = 1; i <= total; i++) {
                int row = (i - 1) / 4 + 1;
                int col = (i - 1) % 4 + 1;
                seats.add(Seat.builder()
                        .bus(bus)
                        .seatNumber("S" + i)
                        .seatType("SEATER")
                        .deck("LOWER")
                        .rowNum(row)
                        .colNum(col)
                        .build());
            }
        }

        seatRepository.saveAll(seats);
    }

    @Transactional
    public List<RouteResponseDto> searchRoutes(
            String source,
            String destination,
            LocalDate travelDate,
            String busType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String departureWindow,
            String sortBy
    ) {
        // 1. Synchronize any matching active operator schedules on-demand
        if (source != null && destination != null) {
            List<Schedule> matchingSchedules = scheduleRepository.findActiveBySourceAndDestination(source.trim(), destination.trim());
            if (travelDate != null) {
                for (Schedule s : matchingSchedules) {
                    ensureScheduleMaterialized(s, travelDate);
                }
            } else {
                LocalDate cur = LocalDate.now();
                for (int i = 0; i < 7; i++) {
                    for (Schedule s : matchingSchedules) {
                        ensureScheduleMaterialized(s, cur.plusDays(i));
                    }
                }
            }
        }

        List<Route> routes;
        if (travelDate != null) {
            routes = routeRepository.findBySourceAndDestinationAndDate(source, destination, travelDate);
            if (routes.isEmpty()) {
                // Fallback to all active dates for this route pair
                routes = routeRepository.findBySourceAndDestination(source, destination);
            }
        } else {
            routes = routeRepository.findBySourceAndDestination(source, destination);
        }

        // Fallback to reverse corridor if still empty (e.g. searching Chennai <-> Bangalore)
        if (routes.isEmpty()) {
            routes = routeRepository.findBySourceAndDestination(destination, source);
        }

        // Apply in-memory stream filtering
        return routes.stream()
                .filter(r -> {
                    if (busType != null && !busType.isBlank()) {
                        String busTypeLower = r.getBus().getBusType().toLowerCase();
                        String filterLower = busType.toLowerCase();
                        if (filterLower.contains("sleeper") && !busTypeLower.contains("sleeper")) return false;
                        if (filterLower.contains("seater") && !busTypeLower.contains("seater")) return false;
                        if (filterLower.contains("volvo") && !busTypeLower.contains("volvo")) return false;
                    }
                    return true;
                })
                .filter(r -> {
                    if (minPrice != null && r.getBasePrice().compareTo(minPrice) < 0) return false;
                    if (maxPrice != null && r.getBasePrice().compareTo(maxPrice) > 0) return false;
                    return true;
                })
                .filter(r -> {
                    if (departureWindow != null && !departureWindow.isBlank() && !departureWindow.equalsIgnoreCase("ALL")) {
                        LocalTime dep = r.getDepartureTime();
                        return switch (departureWindow.toUpperCase()) {
                            case "MORNING" -> !dep.isBefore(LocalTime.of(6, 0)) && dep.isBefore(LocalTime.of(12, 0));
                            case "AFTERNOON" -> !dep.isBefore(LocalTime.of(12, 0)) && dep.isBefore(LocalTime.of(18, 0));
                            case "EVENING" -> !dep.isBefore(LocalTime.of(18, 0));
                            case "NIGHT" -> dep.isBefore(LocalTime.of(6, 0));
                            default -> true;
                        };
                    }
                    return true;
                })
                .sorted((r1, r2) -> {
                    if (sortBy == null) return r1.getDepartureTime().compareTo(r2.getDepartureTime());
                    return switch (sortBy.toLowerCase()) {
                        case "price_asc" -> r1.getBasePrice().compareTo(r2.getBasePrice());
                        case "price_desc" -> r2.getBasePrice().compareTo(r1.getBasePrice());
                        case "rating_desc" -> {
                            BigDecimal rating1 = r1.getBus().getRating() != null ? r1.getBus().getRating() : BigDecimal.ZERO;
                            BigDecimal rating2 = r2.getBus().getRating() != null ? r2.getBus().getRating() : BigDecimal.ZERO;
                            yield rating2.compareTo(rating1);
                        }
                        case "departure_desc" -> r2.getDepartureTime().compareTo(r1.getDepartureTime());
                        default -> r1.getDepartureTime().compareTo(r2.getDepartureTime());
                    };
                })
                .map(this::mapToRouteDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RouteResponseDto getRouteById(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));
        return mapToRouteDto(route);
    }

    @Transactional
    public SeatLayoutDto getRouteSeats(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        List<RouteSeat> routeSeats = routeSeatRepository.findByRouteId(routeId);

        // If route seats have not been generated yet for this route, initialize from the bus seat catalog
        if (routeSeats.isEmpty()) {
            List<Seat> busSeats = seatRepository.findByBusIdOrderByDeckAscRowNumAscColNumAsc(route.getBus().getId());
            if (busSeats.isEmpty()) {
                generateBusSeats(route.getBus());
                busSeats = seatRepository.findByBusIdOrderByDeckAscRowNumAscColNumAsc(route.getBus().getId());
            }
            for (Seat s : busSeats) {
                RouteSeat rs = RouteSeat.builder()
                        .route(route)
                        .seat(s)
                        .status("AVAILABLE")
                        .genderRestriction("NONE")
                        .build();
                routeSeats.add(rs);
            }
            routeSeats = routeSeatRepository.saveAll(routeSeats);
        }

        List<SeatDto> seatDtos = routeSeats.stream().map(rs -> {
            Seat s = rs.getSeat();
            BigDecimal price = rs.getPriceOverride() != null ? rs.getPriceOverride() : route.getBasePrice();
            return SeatDto.builder()
                    .id(rs.getId())
                    .seatId(s.getId())
                    .seatNumber(s.getSeatNumber())
                    .seatType(s.getSeatType())
                    .deck(s.getDeck())
                    .rowNum(s.getRowNum())
                    .colNum(s.getColNum())
                    .status(rs.getStatus())
                    .genderRestriction(rs.getGenderRestriction())
                    .price(price)
                    .lockedByUserId(rs.getLockedByUserId())
                    .bookedGender(rs.getBookedGender())
                    .build();
        }).collect(Collectors.toList());

        return SeatLayoutDto.builder()
                .routeId(route.getId())
                .busId(route.getBus().getId())
                .operatorName(route.getBus().getOperatorName())
                .busType(route.getBus().getBusType())
                .basePrice(route.getBasePrice())
                .seats(seatDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CityPairDto> getPopularRoutes() {
        List<Object[]> pairs = routeRepository.findDistinctCityPairs();
        List<Object[]> schedPairs = scheduleRepository.findDistinctCityPairs();

        Map<String, CityPairDto> resultMap = new HashMap<>();

        for (Object[] pair : pairs) {
            String src = (String) pair[0];
            String dst = (String) pair[1];
            if (src == null || dst == null) continue;
            String key = src.toLowerCase() + "_" + dst.toLowerCase();
            List<Route> routes = routeRepository.findBySourceAndDestination(src, dst);
            if (!routes.isEmpty()) {
                BigDecimal minPrice = routes.stream()
                        .map(Route::getBasePrice)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.valueOf(500));
                resultMap.put(key, CityPairDto.builder()
                        .sourceCity(src)
                        .destinationCity(dst)
                        .busCount(routes.size())
                        .minPrice(minPrice)
                        .build());
            }
        }

        for (Object[] pair : schedPairs) {
            String src = (String) pair[0];
            String dst = (String) pair[1];
            if (src == null || dst == null) continue;
            String key = src.toLowerCase() + "_" + dst.toLowerCase();
            if (!resultMap.containsKey(key)) {
                List<Schedule> scheds = scheduleRepository.findActiveBySourceAndDestination(src, dst);
                if (!scheds.isEmpty()) {
                    BigDecimal minPrice = scheds.stream()
                            .map(Schedule::getBasePrice)
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.valueOf(500));
                    resultMap.put(key, CityPairDto.builder()
                            .sourceCity(src)
                            .destinationCity(dst)
                            .busCount(scheds.size())
                            .minPrice(minPrice)
                            .build());
                }
            }
        }

        return new ArrayList<>(resultMap.values());
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableCities() {
        Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        cities.addAll(routeRepository.findDistinctSourceCities());
        cities.addAll(routeRepository.findDistinctDestinationCities());
        cities.addAll(scheduleRepository.findDistinctSourceCities());
        cities.addAll(scheduleRepository.findDistinctDestinationCities());
        return new ArrayList<>(cities);
    }

    public RouteResponseDto mapToRouteDto(Route route) {
        int availableSeats = routeSeatRepository.countAvailableSeats(route.getId());

        List<String> amenitiesList = Collections.emptyList();
        if (route.getBus().getAmenities() != null && !route.getBus().getAmenities().isBlank()) {
            amenitiesList = Arrays.stream(route.getBus().getAmenities().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        List<String> boardingPointsList = Collections.emptyList();
        if (route.getBoardingPoints() != null && !route.getBoardingPoints().isBlank()) {
            boardingPointsList = Arrays.stream(route.getBoardingPoints().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        List<String> droppingPointsList = Collections.emptyList();
        if (route.getDroppingPoints() != null && !route.getDroppingPoints().isBlank()) {
            droppingPointsList = Arrays.stream(route.getDroppingPoints().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        return RouteResponseDto.builder()
                .id(route.getId())
                .busId(route.getBus().getId())
                .operatorName(route.getBus().getOperatorName())
                .busType(route.getBus().getBusType())
                .rating(route.getBus().getRating())
                .amenities(amenitiesList)
                .sourceCity(route.getSourceCity())
                .destinationCity(route.getDestinationCity())
                .departureTime(route.getDepartureTime())
                .arrivalTime(route.getArrivalTime())
                .travelDate(route.getTravelDate())
                .durationHours(route.getDurationHours())
                .basePrice(route.getBasePrice())
                .availableSeats(availableSeats)
                .boardingPoints(boardingPointsList)
                .droppingPoints(droppingPointsList)
                .busPhotoUrl(route.getBus() != null ? route.getBus().getPhotoUrls() : null)
                .build();
    }
}

