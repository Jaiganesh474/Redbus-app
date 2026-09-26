package com.redbus.controller;

import com.redbus.dto.AdminStatsDto;
import com.redbus.dto.BookingResponseDto;
import com.redbus.dto.OperatorDto;
import com.redbus.dto.RouteResponseDto;
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

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        long totalBookings = bookingRepository.count();
        long confirmedBookings = bookingRepository.countByStatus("CONFIRMED");
        long cancelledBookings = bookingRepository.countByStatus("CANCELLED");
        BigDecimal totalRevenue = bookingRepository.calculateTotalRevenue();
        long totalRoutes = routeRepository.count();
        long totalBuses = busRepository.count();

        double occupancyRate = totalBookings > 0 ? (double) confirmedBookings / (double) (totalBookings) * 100.0 : 78.5;

        AdminStatsDto stats = AdminStatsDto.builder()
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .totalRevenue(totalRevenue)
                .occupancyRate(Math.round(occupancyRate * 10.0) / 10.0)
                .totalRoutes(totalRoutes)
                .totalBuses(totalBuses)
                .topRoutes(busRouteService.getPopularRoutes())
                .build();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/operators")
    public ResponseEntity<List<OperatorDto>> getAllOperators() {
        List<Operator> operators = operatorRepository.findAll();
        List<OperatorDto> dtos = operators.stream().map(op -> {
            int busCount = busRepository.findByOperatorId(op.getId()).size();
            int scheduleCount = scheduleRepository.findByOperatorId(op.getId()).size();
            long bookingsCount = bookingRepository.findByOperatorId(op.getId()).size();
            BigDecimal revenue = bookingRepository.calculateOperatorTotalRevenue(op.getId());

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
                    .totalBuses(busCount)
                    .totalSchedules(scheduleCount)
                    .totalBookings(bookingsCount)
                    .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/operators/{id}/verify")
    @Transactional
    public ResponseEntity<OperatorDto> verifyOperator(@PathVariable Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + id));

        operator.setStatus("APPROVED");
        operatorRepository.save(operator);

        // Ensure associated user has ROLE_OPERATOR and is email-verified
        if (operator.getUser() != null) {
            User user = operator.getUser();
            user.setRole("ROLE_OPERATOR");
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        return ResponseEntity.ok(OperatorDto.builder()
                .id(operator.getId())
                .userId(operator.getUser() != null ? operator.getUser().getId() : null)
                .companyName(operator.getCompanyName())
                .contactPerson(operator.getContactPerson())
                .email(operator.getEmail())
                .phone(operator.getPhone())
                .commissionRate(operator.getCommissionRate())
                .status(operator.getStatus())
                .createdAt(operator.getCreatedAt())
                .build());
    }

    @PutMapping("/operators/{id}/suspend")
    @Transactional
    public ResponseEntity<OperatorDto> suspendOperator(@PathVariable Long id) {
        Operator operator = operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + id));

        operator.setStatus("SUSPENDED");
        operatorRepository.save(operator);

        return ResponseEntity.ok(OperatorDto.builder()
                .id(operator.getId())
                .userId(operator.getUser() != null ? operator.getUser().getId() : null)
                .companyName(operator.getCompanyName())
                .contactPerson(operator.getContactPerson())
                .email(operator.getEmail())
                .phone(operator.getPhone())
                .commissionRate(operator.getCommissionRate())
                .status(operator.getStatus())
                .createdAt(operator.getCreatedAt())
                .build());
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
            operatorRepository.save(operator);
        }

        return ResponseEntity.ok(OperatorDto.builder()
                .id(operator.getId())
                .userId(operator.getUser() != null ? operator.getUser().getId() : null)
                .companyName(operator.getCompanyName())
                .contactPerson(operator.getContactPerson())
                .email(operator.getEmail())
                .phone(operator.getPhone())
                .commissionRate(operator.getCommissionRate())
                .status(operator.getStatus())
                .createdAt(operator.getCreatedAt())
                .build());
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
        return ResponseEntity.ok(operatorAnalyticsService.getAdminOperatorEarnings());
    }
}
