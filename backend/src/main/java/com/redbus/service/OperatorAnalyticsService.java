package com.redbus.service;

import com.redbus.dto.OperatorAnalyticsDto;
import com.redbus.dto.OperatorBookingDto;
import com.redbus.entity.Booking;
import com.redbus.entity.BookingPassenger;
import com.redbus.entity.Bus;
import com.redbus.entity.Operator;
import com.redbus.entity.Route;
import com.redbus.repository.BookingRepository;
import com.redbus.repository.BusRepository;
import com.redbus.repository.RouteRepository;
import com.redbus.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperatorAnalyticsService {

    private final BookingRepository bookingRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final ScheduleRepository scheduleRepository;

    public OperatorAnalyticsDto getAnalyticsOverview(Operator operator) {
        Long opId = operator.getId();

        List<Booking> allBookings = bookingRepository.findByOperatorIdOrderByCreatedAtDesc(opId);
        List<Booking> confirmedBookings = allBookings.stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());

        List<Bus> buses = busRepository.findByOperatorId(opId);
        List<Route> routes = routeRepository.findAll().stream()
                .filter(r -> opId.equals(r.getOperatorId()))
                .collect(Collectors.toList());

        // 1. Core Financial Aggregations
        BigDecimal totalRevenue = confirmedBookings.stream()
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal commissionPaid = confirmedBookings.stream()
                .map(b -> b.getCommissionAmount() != null ? b.getCommissionAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Fallback: If commission was 0, compute 10%
        if (commissionPaid.compareTo(BigDecimal.ZERO) == 0 && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            commissionPaid = totalRevenue.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal netEarnings = totalRevenue.subtract(commissionPaid);

        // 2. Ticket & Capacity Aggregations
        long ticketsSold = confirmedBookings.stream()
                .mapToLong(b -> b.getPassengers() != null ? b.getPassengers().size() : 1)
                .sum();

        long totalSeatsOffered = routes.stream()
                .mapToLong(r -> r.getBus() != null ? r.getBus().getTotalSeats() : 30)
                .sum();

        BigDecimal occupancyRate = BigDecimal.ZERO;
        if (totalSeatsOffered > 0) {
            occupancyRate = BigDecimal.valueOf((double) ticketsSold / totalSeatsOffered * 100)
                    .setScale(1, RoundingMode.HALF_UP);
        }

        // 3. Route Performance Breakdown
        Map<String, List<Booking>> bookingsByRoute = confirmedBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getRoute().getSourceCity() + " → " + b.getRoute().getDestinationCity()));

        List<OperatorAnalyticsDto.RouteStat> routeStats = new ArrayList<>();
        bookingsByRoute.forEach((routeName, bList) -> {
            long rTickets = bList.stream().mapToLong(b -> b.getPassengers() != null ? b.getPassengers().size() : 1).sum();
            BigDecimal rRev = bList.stream().map(Booking::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            routeStats.add(OperatorAnalyticsDto.RouteStat.builder()
                    .routeName(routeName)
                    .bookingCount((long) bList.size())
                    .ticketsSold(rTickets)
                    .revenue(rRev)
                    .occupancyPercentage(BigDecimal.valueOf(Math.min(100.0, (rTickets * 100.0) / Math.max(1, bList.size() * 30))).setScale(1, RoundingMode.HALF_UP))
                    .build());
        });

        // 4. Bus Performance Breakdown
        Map<String, List<Route>> routesByBus = routes.stream()
                .collect(Collectors.groupingBy(r -> r.getBus() != null && r.getBus().getRegistrationNumber() != null ? r.getBus().getRegistrationNumber() : "BUS-" + r.getBus().getId()));

        List<OperatorAnalyticsDto.BusStat> busStats = new ArrayList<>();
        routesByBus.forEach((regNo, rList) -> {
            BigDecimal bRev = confirmedBookings.stream()
                    .filter(b -> b.getRoute() != null && b.getRoute().getBus() != null && regNo.equals(b.getRoute().getBus().getRegistrationNumber()))
                    .map(Booking::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String bType = !rList.isEmpty() && rList.get(0).getBus() != null ? rList.get(0).getBus().getBusType() : "AC Sleeper";

            busStats.add(OperatorAnalyticsDto.BusStat.builder()
                    .registrationNumber(regNo)
                    .busType(bType)
                    .tripsCount((long) rList.size())
                    .revenue(bRev)
                    .build());
        });

        // 5. Daily Timeline for Trend Charts (Last 7 days or matching bookings)
        Map<String, List<Booking>> bookingsByDate = confirmedBookings.stream()
                .filter(b -> b.getCreatedAt() != null)
                .collect(Collectors.groupingBy(b -> b.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)));

        List<OperatorAnalyticsDto.DailyTimelineStat> dailyTimeline = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            String dStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
            List<Booking> dayBookings = bookingsByDate.getOrDefault(dStr, Collections.emptyList());
            BigDecimal dRev = dayBookings.stream().map(Booking::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            long dTickets = dayBookings.stream().mapToLong(b -> b.getPassengers() != null ? b.getPassengers().size() : 1).sum();

            dailyTimeline.add(OperatorAnalyticsDto.DailyTimelineStat.builder()
                    .date(d.format(DateTimeFormatter.ofPattern("MMM dd")))
                    .revenue(dRev)
                    .ticketsSold(dTickets)
                    .build());
        }

        // 6. Recent Bookings List
        List<OperatorBookingDto> recentBookings = allBookings.stream()
                .limit(20)
                .map(this::mapToOperatorBookingDto)
                .collect(Collectors.toList());

        return OperatorAnalyticsDto.builder()
                .operatorName(operator.getContactPerson())
                .companyName(operator.getCompanyName())
                .totalRevenue(totalRevenue)
                .netEarnings(netEarnings)
                .commissionPaid(commissionPaid)
                .ticketsSold(ticketsSold)
                .activeFleetCount((long) buses.size())
                .activeSchedulesCount((long) scheduleRepository.findByOperatorId(opId).size())
                .totalTripsRun((long) routes.size())
                .averageOccupancyPercentage(occupancyRate)
                .routePerformance(routeStats)
                .busPerformance(busStats)
                .dailyTimeline(dailyTimeline)
                .recentBookings(recentBookings)
                .build();
    }

    public List<OperatorBookingDto> getOperatorBookings(Operator operator) {
        return bookingRepository.findByOperatorIdOrderByCreatedAtDesc(operator.getId()).stream()
                .map(this::mapToOperatorBookingDto)
                .collect(Collectors.toList());
    }

    private OperatorBookingDto mapToOperatorBookingDto(Booking b) {
        List<String> seatNums = b.getPassengers() != null
                ? b.getPassengers().stream().map(BookingPassenger::getSeatNumber).collect(Collectors.toList())
                : Collections.emptyList();

        String pName = b.getPassengers() != null && !b.getPassengers().isEmpty()
                ? b.getPassengers().get(0).getName()
                : (b.getUser() != null ? b.getUser().getName() : "Passenger");

        BigDecimal comm = b.getCommissionAmount() != null ? b.getCommissionAmount() : b.getTotalAmount().multiply(new BigDecimal("0.10"));
        BigDecimal net = b.getTotalAmount().subtract(comm);

        return OperatorBookingDto.builder()
                .id(b.getId())
                .pnr(b.getPnr())
                .passengerName(pName)
                .passengerEmail(b.getContactEmail())
                .passengerPhone(b.getContactPhone())
                .route(b.getRoute() != null ? b.getRoute().getSourceCity() + " → " + b.getRoute().getDestinationCity() : "N/A")
                .travelDate(b.getRoute() != null ? b.getRoute().getTravelDate() : null)
                .busName(b.getRoute() != null && b.getRoute().getBus() != null ? b.getRoute().getBus().getOperatorName() : "Bus")
                .seatNumbers(seatNums)
                .seatCount(seatNums.size())
                .totalAmount(b.getTotalAmount())
                .commissionAmount(comm)
                .netAmount(net)
                .status(b.getStatus())
                .bookingTime(b.getCreatedAt())
                .build();
    }

    public List<com.redbus.dto.OperatorPassengerManifestDto> getPassengerManifest(
            Operator operator,
            LocalDate travelDate,
            Long busId,
            Long scheduleId
    ) {
        Long opId = operator.getId();
        List<Booking> allBookings = bookingRepository.findByOperatorIdOrderByCreatedAtDesc(opId);
        final com.redbus.entity.Schedule targetSchedule = (scheduleId != null)
                ? scheduleRepository.findById(scheduleId).orElse(null)
                : null;

        return allBookings.stream()
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()) && !"EXPIRED".equalsIgnoreCase(b.getStatus()))
                .filter(b -> b.getRoute() != null)
                .filter(b -> travelDate == null || travelDate.equals(b.getRoute().getTravelDate()))
                .filter(b -> busId == null || (b.getRoute().getBus() != null && busId.equals(b.getRoute().getBus().getId())))
                .filter(b -> {
                    if (scheduleId == null) return true;
                    Route r = b.getRoute();
                    if (r == null) return false;
                    if (scheduleId.equals(r.getId())) return true;
                    if (targetSchedule != null) {
                        boolean matchSrc = targetSchedule.getSourceCity() == null || targetSchedule.getSourceCity().equalsIgnoreCase(r.getSourceCity());
                        boolean matchDst = targetSchedule.getDestinationCity() == null || targetSchedule.getDestinationCity().equalsIgnoreCase(r.getDestinationCity());
                        boolean matchTime = targetSchedule.getDepartureTime() == null || targetSchedule.getDepartureTime().equals(r.getDepartureTime());
                        boolean matchBus = targetSchedule.getBus() == null || (r.getBus() != null && targetSchedule.getBus().getId().equals(r.getBus().getId()));
                        return matchSrc && matchDst && matchTime && matchBus;
                    }
                    return false;
                })
                .flatMap(b -> {
                    Route r = b.getRoute();
                    Bus bus = r != null ? r.getBus() : null;
                    List<BookingPassenger> passList = b.getPassengers() != null ? b.getPassengers() : Collections.emptyList();

                    return passList.stream().map(p -> {
                        String sNum = p.getSeatNumber() != null ? p.getSeatNumber().trim() : "";
                        String deck = "LOWER";
                        String seatType = "SEATER";
                        if (p.getSeat() != null) {
                            if (p.getSeat().getDeck() != null) deck = p.getSeat().getDeck();
                            if (p.getSeat().getSeatType() != null) seatType = p.getSeat().getSeatType();
                        } else if (bus != null && bus.getBusType() != null && bus.getBusType().toLowerCase().contains("sleeper")) {
                            seatType = "SLEEPER";
                            if (sNum.toUpperCase().startsWith("U")) deck = "UPPER";
                        }

                        // Determine Berth Label
                        String berthLabel;
                        String seatDisplay;
                        if ("SLEEPER".equalsIgnoreCase(seatType) || (bus != null && bus.getBusType() != null && bus.getBusType().toLowerCase().contains("sleeper"))) {
                            if ("UPPER".equalsIgnoreCase(deck) || sNum.toUpperCase().startsWith("U")) {
                                berthLabel = "Upper Berth";
                                seatDisplay = sNum + " (Upper Berth)";
                            } else {
                                berthLabel = "Lower Berth";
                                seatDisplay = sNum + " (Lower Berth)";
                            }
                        } else {
                            berthLabel = "Seater";
                            seatDisplay = sNum + (sNum.toLowerCase().contains("seater") ? "" : " (Seater)");
                        }

                        String depTime = r != null && r.getDepartureTime() != null ? r.getDepartureTime().format(DateTimeFormatter.ofPattern("hh:mm a")) : "N/A";
                        String arrTime = r != null && r.getArrivalTime() != null ? r.getArrivalTime().format(DateTimeFormatter.ofPattern("hh:mm a")) : "N/A";

                        return com.redbus.dto.OperatorPassengerManifestDto.builder()
                                .bookingId(b.getId())
                                .pnr(b.getPnr())
                                .passengerName(p.getName())
                                .age(p.getAge())
                                .gender(p.getGender())
                                .seatNumber(sNum)
                                .seatType(seatType)
                                .deck(deck)
                                .berthLabel(berthLabel)
                                .seatDisplay(seatDisplay)
                                .boardingPoint(b.getBoardingPoint() != null ? b.getBoardingPoint() : (r != null ? r.getSourceCity() : "N/A"))
                                .droppingPoint(b.getDroppingPoint() != null ? b.getDroppingPoint() : (r != null ? r.getDestinationCity() : "N/A"))
                                .contactPhone(b.getContactPhone())
                                .contactEmail(b.getContactEmail())
                                .travelDate(r != null ? r.getTravelDate() : null)
                                .sourceCity(r != null ? r.getSourceCity() : "N/A")
                                .destinationCity(r != null ? r.getDestinationCity() : "N/A")
                                .departureTime(depTime)
                                .arrivalTime(arrTime)
                                .busOperator(bus != null ? bus.getOperatorName() : (operator.getCompanyName() != null ? operator.getCompanyName() : "Express Coach"))
                                .busRegistration(bus != null ? bus.getRegistrationNumber() : "Fleet Bus")
                                .busType(bus != null ? bus.getBusType() : "AC Coach")
                                .status(b.getStatus())
                                .build();
                    });
                })
                .sorted(Comparator.comparing(com.redbus.dto.OperatorPassengerManifestDto::getSeatNumber, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }
}

