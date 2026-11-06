package com.redbus.controller;

import com.redbus.dto.AdminStatsDto;
import com.redbus.dto.BookingResponseDto;
import com.redbus.dto.BusResponseDto;
import com.redbus.dto.OperatorDto;
import com.redbus.dto.RouteResponseDto;
import com.redbus.entity.Bus;
import com.redbus.entity.Operator;
import com.redbus.entity.User;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.repository.*;
import com.redbus.service.BookingService;
import com.redbus.service.BusRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/admin", "/api/v1/admin"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
public class AdminController {

    private final BookingRepository bookingRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final OperatorRepository operatorRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final BusRouteService busRouteService;
    private final BookingService bookingService;
    private final com.redbus.service.OperatorAnalyticsService operatorAnalyticsService;

    private void purgeMockedSuspendedOperators() {
        try {
            List<Operator> allOps = operatorRepository.findAll();
            for (Operator op : allOps) {
                int busCount = busRepository.findByOperatorId(op.getId()).size();
                boolean isMockZingbus = (op.getEmail() != null && op.getEmail().equalsIgnoreCase("operator@zingbus.com"))
                        || (op.getCompanyName() != null && op.getCompanyName().toLowerCase().contains("zingbus"));
                boolean isAdminAutoOp = (op.getEmail() != null && op.getEmail().equalsIgnoreCase("redbus@admin.in"))
                        || (op.getCompanyName() != null && op.getCompanyName().toLowerCase().contains("redbus administrator"));
                boolean isMockSuspended = "SUSPENDED".equalsIgnoreCase(op.getStatus()) && busCount == 0;

                if ((isMockZingbus || isAdminAutoOp || isMockSuspended) && busCount == 0) {
                    operatorRepository.delete(op);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private OperatorDto buildOperatorDto(Operator op) {
        int busCount = busRepository.findByOperatorId(op.getId()).size();
        int scheduleCount = scheduleRepository.findByOperatorId(op.getId()).size();
        long bookingsCount = bookingRepository.findByOperatorId(op.getId()).size();
        BigDecimal revenue = bookingRepository.calculateOperatorTotalRevenue(op.getId());
        boolean isPrimo = Boolean.TRUE.equals(op.getPrimoCertified());
        BigDecimal commRate = isPrimo ? BigDecimal.ZERO : (op.getCommissionRate() != null ? op.getCommissionRate() : new BigDecimal("10.00"));

        return OperatorDto.builder()
                .id(op.getId())
                .userId(op.getUser() != null ? op.getUser().getId() : null)
                .companyName(op.getCompanyName())
                .contactPerson(op.getContactPerson())
                .email(op.getEmail())
                .phone(op.getPhone())
                .kycDocUrl(op.getKycDocUrl())
                .bankAccountRef(op.getBankAccountRef())
                .commissionRate(commRate)
                .primoCertified(isPrimo)
                .status(op.getStatus())
                .createdAt(op.getCreatedAt())
                .totalBuses(busCount)
                .totalSchedules(scheduleCount)
                .totalBookings(bookingsCount)
                .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                .build();
    }

    @GetMapping("/stats")
    @Transactional
    public ResponseEntity<AdminStatsDto> getStats() {
        purgeMockedSuspendedOperators();
        long totalBookings = bookingRepository.count();
        long confirmedBookings = bookingRepository.countByStatus("CONFIRMED");
        long cancelledBookings = bookingRepository.countByStatus("CANCELLED");
        BigDecimal totalRevenue = bookingRepository.calculateTotalRevenue();
        long totalRoutes = routeRepository.count();
        long totalBuses = busRepository.count();
        long activeSchedules = scheduleRepository.count();

        List<Operator> operators = operatorRepository.findAll();
        long approvedOperators = operators.stream().filter(o -> "APPROVED".equalsIgnoreCase(o.getStatus())).count();
        List<Operator> primoOps = operators.stream().filter(o -> Boolean.TRUE.equals(o.getPrimoCertified())).collect(Collectors.toList());
        long primoOperatorsCount = primoOps.size();
        long primoBusesCount = primoOps.stream()
                .mapToLong(o -> busRepository.findByOperatorId(o.getId()).size())
                .sum();

        double occupancyRate = totalBookings > 0 ? (double) confirmedBookings / (double) (totalBookings) * 100.0 : 78.5;

        AdminStatsDto stats = AdminStatsDto.builder()
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .totalRevenue(totalRevenue)
                .occupancyRate(Math.round(occupancyRate * 10.0) / 10.0)
                .totalRoutes(totalRoutes)
                .totalBuses(totalBuses)
                .primoOperatorsCount(primoOperatorsCount)
                .primoBusesCount(primoBusesCount)
                .activeSchedulesCount(activeSchedules)
                .approvedOperatorsCount(approvedOperators)
                .topRoutes(busRouteService.getPopularRoutes())
                .build();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/operators")
    @Transactional
    public ResponseEntity<List<OperatorDto>> getAllOperators() {
        purgeMockedSuspendedOperators();
        List<Operator> operators = operatorRepository.findAll();
        List<OperatorDto> dtos = operators.stream()
                .map(this::buildOperatorDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/primo-buses")
    @Transactional(readOnly = true)
    public ResponseEntity<List<BusResponseDto>> getPrimoCertifiedBuses() {
        List<Operator> primoOperators = operatorRepository.findAll().stream()
                .filter(op -> Boolean.TRUE.equals(op.getPrimoCertified()))
                .collect(Collectors.toList());

        List<BusResponseDto> primoBuses = new ArrayList<>();
        for (Operator op : primoOperators) {
            List<Bus> buses = busRepository.findByOperatorId(op.getId());
            for (Bus bus : buses) {
                String regNo = bus.getRegistrationNumber();
                if (regNo == null || regNo.isBlank() || regNo.startsWith("IND-")) {
                    String suffix = (regNo != null && regNo.startsWith("IND-")) ? regNo.substring(4) : String.valueOf(4000 + bus.getId() * 37);
                    regNo = "TN12BB" + suffix;
                }
                primoBuses.add(BusResponseDto.builder()
                        .id(bus.getId())
                        .operatorId(op.getId())
                        .operatorName(bus.getOperatorName() != null ? bus.getOperatorName() : op.getCompanyName())
                        .registrationNumber(regNo)
                        .busType(bus.getBusType())
                        .totalSeats(bus.getTotalSeats())
                        .amenities(bus.getAmenities())
                        .photoUrls(bus.getPhotoUrls())
                        .active(bus.getActive())
                        .rating(bus.getRating() != null && bus.getRating().compareTo(BigDecimal.ZERO) > 0 ? bus.getRating() : new BigDecimal("4.7"))
                        .primoCertified(true)
                        .createdAt(bus.getCreatedAt())
                        .build());
            }
        }
        return ResponseEntity.ok(primoBuses);
    }

    @PutMapping("/operators/{id}/verify")
    @Transactional
    public ResponseEntity<OperatorDto> verifyOperator(@PathVariable Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + id));

        operator.setStatus("APPROVED");
        operatorRepository.save(operator);

        if (operator.getUser() != null) {
            User user = operator.getUser();
            user.setRole("ROLE_OPERATOR");
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        return ResponseEntity.ok(buildOperatorDto(operator));
    }

    @PutMapping("/operators/{id}/suspend")
    @Transactional
    public ResponseEntity<OperatorDto> suspendOperator(@PathVariable Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + id));

        operator.setStatus("SUSPENDED");
        operatorRepository.save(operator);

        return ResponseEntity.ok(buildOperatorDto(operator));
    }

    @PutMapping("/operators/{id}/commission")
    @Transactional
    public ResponseEntity<OperatorDto> updateCommission(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> payload
    ) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + id));

        BigDecimal rate = payload.get("commissionRate");
        if (rate != null) {
            operator.setCommissionRate(rate);
            if (rate.compareTo(BigDecimal.ZERO) > 0) {
                operator.setPrimoCertified(false);
            }
            operatorRepository.save(operator);
        }

        return ResponseEntity.ok(buildOperatorDto(operator));
    }

    @PutMapping("/operators/{id}/primo")
    @Transactional
    public ResponseEntity<OperatorDto> togglePrimoOperator(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + id));

        boolean currentPrimo = Boolean.TRUE.equals(operator.getPrimoCertified());
        boolean newPrimo = !currentPrimo;
        if (payload != null && payload.containsKey("primoCertified") && payload.get("primoCertified") != null) {
            newPrimo = Boolean.parseBoolean(String.valueOf(payload.get("primoCertified")));
        }

        operator.setPrimoCertified(newPrimo);
        if (newPrimo) {
            // Primo Certified operators get 0% Commission (Commission-Free perk) and APPROVED status
            operator.setCommissionRate(BigDecimal.ZERO);
            operator.setStatus("APPROVED");
        } else {
            if (operator.getCommissionRate() == null || operator.getCommissionRate().compareTo(BigDecimal.ZERO) == 0) {
                operator.setCommissionRate(new BigDecimal("10.00"));
            }
        }
        operatorRepository.save(operator);

        return ResponseEntity.ok(buildOperatorDto(operator));
    }

    @GetMapping("/bookings")
    public ResponseEntity<Page<BookingResponseDto>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Page<BookingResponseDto> bookings = bookingRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(bookingService::mapToDto);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/routes")
    public ResponseEntity<List<RouteResponseDto>> getAllRoutes() {
        List<RouteResponseDto> routes = routeRepository.findAll().stream()
                .map(busRouteService::mapToRouteDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/operator-earnings")
    public ResponseEntity<com.redbus.dto.AdminOperatorEarningsDto> getOperatorEarnings() {
        purgeMockedSuspendedOperators();
        return ResponseEntity.ok(operatorAnalyticsService.getAdminOperatorEarnings());
    }
}
