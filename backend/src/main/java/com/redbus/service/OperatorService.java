package com.redbus.service;

import com.redbus.dto.*;
import com.redbus.entity.*;
import com.redbus.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperatorService {

    private final OperatorRepository operatorRepository;
    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final RouteRepository routeRepository;
    private final RouteSeatRepository routeSeatRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public Operator getOrCreateOperatorForUser(User user) {
        return operatorRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Operator newOp = Operator.builder()
                            .user(user)
                            .companyName(user.getName() + " Travels")
                            .contactPerson(user.getName())
                            .email(user.getEmail())
                            .phone(user.getPhone() != null ? user.getPhone() : "+91 9876543210")
                            .commissionRate(new BigDecimal("10.00"))
                            .status("APPROVED")
                            .build();
                    return operatorRepository.save(newOp);
                });
    }

    public List<BusResponseDto> getOperatorBuses(Long operatorId) {
        return busRepository.findByOperatorId(operatorId).stream()
                .map(this::mapToBusDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BusResponseDto createBus(Long operatorId, BusCreateRequest req) {
        String regNumber = req.getRegistrationNumber();
        if (regNumber == null || regNumber.trim().isEmpty()) {
            regNumber = "IND-" + (int)(Math.random() * 9000 + 1000);
        }

        Bus bus = Bus.builder()
                .operatorId(operatorId)
                .operatorName(req.getOperatorName())
                .registrationNumber(regNumber.toUpperCase())
                .busType(req.getBusType())
                .totalSeats(req.getTotalSeats() != null ? req.getTotalSeats() : 30)
                .amenities(req.getAmenities() != null ? req.getAmenities() : "WiFi,Charging Point,Water Bottle,Blanket,Emergency Exit")
                .photoUrls(req.getPhotoUrls())
                .active(true)
                .rating(new BigDecimal("4.8"))
                .build();

        Bus savedBus = busRepository.save(bus);

        // Generate custom template seats for this bus layout
        generateBusSeats(savedBus);

        return mapToBusDto(savedBus);
    }

    private void generateBusSeats(Bus bus) {
        boolean isSleeper = bus.getBusType().toUpperCase().contains("SLEEPER");
        int total = bus.getTotalSeats();
        List<Seat> seats = new ArrayList<>();

        if (isSleeper) {
            // 2+1 Sleeper layout (Lower and Upper decks)
            int lowerCount = total / 2;
            int upperCount = total - lowerCount;

            // Lower Deck
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

            // Upper Deck
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
            // 2+2 Seater layout (Lower deck)
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
        log.info("Generated {} layout template seats for Bus ID: {}", seats.size(), bus.getId());
    }

    public List<ScheduleResponseDto> getOperatorSchedules(Long operatorId) {
        return scheduleRepository.findByOperatorId(operatorId).stream()
                .map(this::mapToScheduleDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScheduleResponseDto createSchedule(Long operatorId, ScheduleCreateRequest req) {
        Bus bus = busRepository.findById(req.getBusId())
                .orElseThrow(() -> new IllegalArgumentException("Bus not found with ID: " + req.getBusId()));

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new IllegalArgumentException("Operator not found with ID: " + operatorId));

        // Create or find matching Route template
        String boarding = req.getBoardingPoints() != null && !req.getBoardingPoints().trim().isEmpty()
                ? req.getBoardingPoints().trim()
                : req.getSourceCity() + " Central (08:00), " + req.getSourceCity() + " Bypass (08:30)";
        String dropping = req.getDroppingPoints() != null && !req.getDroppingPoints().trim().isEmpty()
                ? req.getDroppingPoints().trim()
                : req.getDestinationCity() + " City Center (14:00), " + req.getDestinationCity() + " Main Terminal (14:30)";

        Schedule schedule = Schedule.builder()
                .operator(operator)
                .bus(bus)
                .sourceCity(req.getSourceCity().trim())
                .destinationCity(req.getDestinationCity().trim())
                .boardingPoints(boarding)
                .droppingPoints(dropping)
                .departureTime(req.getDepartureTime())
                .arrivalTime(req.getArrivalTime())
                .operatingDays(req.getOperatingDays() != null && !req.getOperatingDays().trim().isEmpty() ? req.getOperatingDays().trim().toUpperCase() : "DAILY")
                .basePrice(req.getBasePrice())
                .validFrom(req.getValidFrom())
                .validTo(req.getValidTo())
                .status("ACTIVE")
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);

        // Dynamically materialize trips and seats for every valid calendar day in range!
        int createdTrips = materializeScheduleTrips(savedSchedule, bus, req, boarding, dropping);

        ScheduleResponseDto dto = mapToScheduleDto(savedSchedule);
        dto.setMaterializedTripsCount(createdTrips);
        return dto;
    }

    private int materializeScheduleTrips(Schedule schedule, Bus bus, ScheduleCreateRequest req, String boarding, String dropping) {
        LocalDate current = req.getValidFrom();
        LocalDate end = req.getValidTo();
        List<Seat> templateSeats = seatRepository.findByBusIdOrderByDeckAscRowNumAscColNumAsc(bus.getId());

        if (templateSeats.isEmpty()) {
            generateBusSeats(bus);
            templateSeats = seatRepository.findByBusIdOrderByDeckAscRowNumAscColNumAsc(bus.getId());
        }

        int count = 0;
        Route firstCreatedRoute = null;
        while (!current.isAfter(end) && count < 60) { // Safety cap up to 60 days
            if (isOperatingOnDate(schedule.getOperatingDays(), current)) {
                // Create Route instance for this date
                Route route = Route.builder()
                        .operatorId(schedule.getOperator().getId())
                        .bus(bus)
                        .sourceCity(req.getSourceCity().trim())
                        .destinationCity(req.getDestinationCity().trim())
                        .departureTime(req.getDepartureTime())
                        .arrivalTime(req.getArrivalTime())
                        .travelDate(current)
                        .basePrice(req.getBasePrice())
                        .durationHours(new BigDecimal("6.5"))
                        .boardingPoints(boarding)
                        .droppingPoints(dropping)
                        .build();

                Route savedRoute = routeRepository.save(route);
                if (firstCreatedRoute == null) {
                    firstCreatedRoute = savedRoute;
                }

                // Clone template seats to route_seats
                final Route targetRoute = savedRoute;
                List<RouteSeat> routeSeats = templateSeats.stream()
                        .map(s -> RouteSeat.builder()
                                .route(targetRoute)
                                .seat(s)
                                .status("AVAILABLE")
                                .genderRestriction("NONE")
                                .build())
                        .collect(Collectors.toList());

                routeSeatRepository.saveAll(routeSeats);
                count++;
            }
            current = current.plusDays(1);
        }

        if (firstCreatedRoute != null && schedule.getRoute() == null) {
            schedule.setRoute(firstCreatedRoute);
            scheduleRepository.save(schedule);
        }

        log.info("Materialized {} dynamic live trips and seat maps for Schedule ID: {}", count, schedule.getId());
        return count;
    }

    private boolean isOperatingOnDate(String operatingDays, LocalDate date) {
        if (operatingDays == null || operatingDays.equalsIgnoreCase("DAILY")) {
            return true;
        }
        DayOfWeek dow = date.getDayOfWeek();
        String dayName = dow.name(); // MONDAY, TUESDAY, etc.
        return operatingDays.toUpperCase().contains(dayName.substring(0, 3));
    }

    private BusResponseDto mapToBusDto(Bus bus) {
        return BusResponseDto.builder()
                .id(bus.getId())
                .operatorId(bus.getOperatorId())
                .operatorName(bus.getOperatorName())
                .registrationNumber(bus.getRegistrationNumber())
                .busType(bus.getBusType())
                .totalSeats(bus.getTotalSeats())
                .amenities(bus.getAmenities())
                .photoUrls(bus.getPhotoUrls())
                .active(bus.getActive())
                .rating(bus.getRating())
                .createdAt(bus.getCreatedAt())
                .build();
    }

    private ScheduleResponseDto mapToScheduleDto(Schedule schedule) {
        Bus bus = schedule.getBus();
        String src = schedule.getSourceCity() != null ? schedule.getSourceCity()
                : (schedule.getRoute() != null ? schedule.getRoute().getSourceCity() : "Dynamic");
        String dst = schedule.getDestinationCity() != null ? schedule.getDestinationCity()
                : (schedule.getRoute() != null ? schedule.getRoute().getDestinationCity() : "Dynamic");

        return ScheduleResponseDto.builder()
                .id(schedule.getId())
                .operatorId(schedule.getOperator().getId())
                .routeId(schedule.getRoute() != null ? schedule.getRoute().getId() : null)
                .busId(bus != null ? bus.getId() : null)
                .busName(bus != null ? bus.getOperatorName() : null)
                .busType(bus != null ? bus.getBusType() : null)
                .sourceCity(src)
                .destinationCity(dst)
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .operatingDays(schedule.getOperatingDays())
                .basePrice(schedule.getBasePrice())
                .validFrom(schedule.getValidFrom())
                .validTo(schedule.getValidTo())
                .status(schedule.getStatus())
                .createdAt(schedule.getCreatedAt())
                .build();
    }

    @Transactional
    public BusResponseDto updateBusPhotos(Long operatorId, Long busId, String photoUrls) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new com.redbus.exception.ResourceNotFoundException("Bus not found with id: " + busId));
        if (!bus.getOperatorId().equals(operatorId)) {
            throw new com.redbus.exception.BadRequestException("You are not authorized to update this bus");
        }
        bus.setPhotoUrls(photoUrls);
        Bus saved = busRepository.save(bus);
        return mapToBusDto(saved);
    }

    @Transactional
    public BusResponseDto updateBus(Long operatorId, Long busId, BusCreateRequest req) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new com.redbus.exception.ResourceNotFoundException("Bus not found with id: " + busId));
        if (operatorId != null && bus.getOperatorId() != null && !bus.getOperatorId().equals(operatorId)) {
            throw new com.redbus.exception.BadRequestException("You are not authorized to update this bus");
        }
        if (req.getOperatorName() != null && !req.getOperatorName().isBlank()) {
            bus.setOperatorName(req.getOperatorName());
        }
        if (req.getRegistrationNumber() != null && !req.getRegistrationNumber().isBlank()) {
            bus.setRegistrationNumber(req.getRegistrationNumber().toUpperCase());
        }
        if (req.getBusType() != null && !req.getBusType().isBlank()) {
            bus.setBusType(req.getBusType());
        }
        if (req.getTotalSeats() != null && req.getTotalSeats() > 0 && !req.getTotalSeats().equals(bus.getTotalSeats())) {
            bus.setTotalSeats(req.getTotalSeats());
            seatRepository.deleteByBusId(bus.getId());
            generateBusSeats(bus);
        }
        if (req.getAmenities() != null && !req.getAmenities().isBlank()) {
            bus.setAmenities(req.getAmenities());
        }
        if (req.getPhotoUrls() != null) {
            bus.setPhotoUrls(req.getPhotoUrls());
        }
        Bus saved = busRepository.save(bus);
        return mapToBusDto(saved);
    }

    @Transactional
    public void deleteBus(Long operatorId, Long busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new com.redbus.exception.ResourceNotFoundException("Bus not found with id: " + busId));
        if (operatorId != null && bus.getOperatorId() != null && !bus.getOperatorId().equals(operatorId)) {
            throw new com.redbus.exception.BadRequestException("You are not authorized to delete this bus");
        }
        seatRepository.deleteByBusId(bus.getId());
        busRepository.delete(bus);
    }

    @Transactional
    public OperatorDto updateProfile(Long operatorId, UpdateOperatorProfileRequest req) {
        Operator op = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new com.redbus.exception.ResourceNotFoundException("Operator not found with id: " + operatorId));

        if (req.getCompanyName() != null && !req.getCompanyName().isBlank()) {
            op.setCompanyName(req.getCompanyName().trim());
        }
        if (req.getContactPerson() != null && !req.getContactPerson().isBlank()) {
            op.setContactPerson(req.getContactPerson().trim());
            if (op.getUser() != null) {
                op.getUser().setName(req.getContactPerson().trim());
            }
        }
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            op.setPhone(req.getPhone().trim());
            if (op.getUser() != null) {
                op.getUser().setPhone(req.getPhone().trim());
            }
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            op.setEmail(req.getEmail().trim());
        }
        if (req.getBankAccountRef() != null) {
            op.setBankAccountRef(req.getBankAccountRef().trim());
        }
        if (req.getKycDocUrl() != null) {
            op.setKycDocUrl(req.getKycDocUrl().trim());
        }

        Operator saved = operatorRepository.save(op);
        return mapToOperatorDto(saved);
    }

    public OperatorDto mapToOperatorDto(Operator op) {
        int totalBuses = busRepository.findByOperatorId(op.getId()).size();
        int totalSchedules = scheduleRepository.findByOperatorId(op.getId()).size();

        return OperatorDto.builder()
                .id(op.getId())
                .userId(op.getUser() != null ? op.getUser().getId() : null)
                .companyName(op.getCompanyName())
                .contactPerson(op.getContactPerson())
                .email(op.getEmail())
                .phone(op.getPhone())
                .kycDocUrl(op.getKycDocUrl())
                .bankAccountRef(op.getBankAccountRef())
                .commissionRate(op.getCommissionRate())
                .status(op.getStatus())
                .createdAt(op.getCreatedAt())
                .totalBuses(totalBuses)
                .totalSchedules(totalSchedules)
                .build();
    }
}

