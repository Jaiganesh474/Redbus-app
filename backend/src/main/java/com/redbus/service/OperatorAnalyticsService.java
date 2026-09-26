package com.redbus.service;

import com.redbus.dto.*;
import com.redbus.entity.*;
import com.redbus.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final OperatorRepository operatorRepository;
    private final OperatorWalletTransactionRepository operatorWalletTransactionRepository;
    private final UserRepository userRepository;

    private List<Booking> getBookingsForOperator(Operator operator) {
        Long opId = operator != null ? operator.getId() : null;
        List<Bus> operatorBuses = opId != null ? busRepository.findByOperatorId(opId) : Collections.emptyList();
        Set<Long> operatorBusIds = operatorBuses.stream().map(Bus::getId).collect(Collectors.toSet());

        return bookingRepository.findAll().stream()
                .filter(b -> {
                    if (opId == null) return true;
                    if (opId.equals(b.getOperatorId())) return true;
                    if (b.getRoute() != null) {
                        if (opId.equals(b.getRoute().getOperatorId())) return true;
                        if (b.getRoute().getBus() != null) {
                            if (operatorBusIds.contains(b.getRoute().getBus().getId())) return true;
                            if (opId.equals(b.getRoute().getBus().getOperatorId())) return true;
                            if (operator != null && operator.getCompanyName() != null) {
                                String busOpName = b.getRoute().getBus().getOperatorName();
                                if (busOpName != null && (busOpName.equalsIgnoreCase(operator.getCompanyName()) || busOpName.equalsIgnoreCase(operator.getContactPerson()))) {
                                    return true;
                                }
                            }
                        }
                    }
                    // If booking operatorId is null, match to active operator
                    if (b.getOperatorId() == null) return true;
                    return false;
                })
                .sorted(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public OperatorAnalyticsDto getAnalyticsOverview(Operator operator) {
        Long opId = operator.getId();

        List<Booking> allBookings = getBookingsForOperator(operator);
        List<Booking> confirmedBookings = allBookings.stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());

        List<Bus> buses = busRepository.findByOperatorId(opId);
        if (buses.isEmpty()) {
            buses = busRepository.findAll();
        }

        List<Route> routes = routeRepository.findAll().stream()
                .filter(r -> opId.equals(r.getOperatorId()) || (r.getBus() != null && opId.equals(r.getBus().getOperatorId())) || r.getOperatorId() == null)
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
                .filter(b -> b.getRoute() != null)
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
                .collect(Collectors.groupingBy(r -> r.getBus() != null && r.getBus().getRegistrationNumber() != null ? r.getBus().getRegistrationNumber() : (r.getBus() != null ? "BUS-" + r.getBus().getId() : "FLEET")));

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

        // 5. Daily Timeline for Trend Charts
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
        return getBookingsForOperator(operator).stream()
                .map(this::mapToOperatorBookingDto)
                .collect(Collectors.toList());
    }

    public List<OperatorRefundDto> getOperatorRefunds(Operator operator) {
        List<Booking> allBookings = getBookingsForOperator(operator);
        return allBookings.stream()
                .filter(b -> "CANCELLED".equalsIgnoreCase(b.getStatus()) || "REFUNDED".equalsIgnoreCase(b.getStatus()) || b.getRefundStatus() != null)
                .map(b -> {
                    String pName = b.getPassengers() != null && !b.getPassengers().isEmpty()
                            ? b.getPassengers().get(0).getName()
                            : (b.getUser() != null ? b.getUser().getName() : "Passenger");
                    List<String> seatNums = b.getPassengers() != null
                            ? b.getPassengers().stream().map(BookingPassenger::getSeatNumber).collect(Collectors.toList())
                            : Collections.emptyList();
                    Route r = b.getRoute();
                    String busName = (r != null && r.getBus() != null) ? r.getBus().getOperatorName() : "Express Coach";

                    BigDecimal totalPaid = b.getTotalAmount().add(b.getWalletAmountUsed() != null ? b.getWalletAmountUsed() : BigDecimal.ZERO);
                    BigDecimal refundAmt = b.getRefundAmount() != null ? b.getRefundAmount() : totalPaid.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP);

                    return OperatorRefundDto.builder()
                            .bookingId(b.getId())
                            .pnr(b.getPnr())
                            .passengerName(pName)
                            .contactEmail(b.getContactEmail())
                            .contactPhone(b.getContactPhone())
                            .sourceCity(r != null ? r.getSourceCity() : "N/A")
                            .destinationCity(r != null ? r.getDestinationCity() : "N/A")
                            .travelDate(r != null ? r.getTravelDate() : null)
                            .busName(busName)
                            .seatNumbers(seatNums)
                            .totalPaid(totalPaid)
                            .refundAmount(refundAmt)
                            .refundDestination(b.getRefundDestination() != null ? b.getRefundDestination() : "WALLET")
                            .refundStatus(b.getRefundStatus() != null ? b.getRefundStatus() : ("REFUNDED".equalsIgnoreCase(b.getStatus()) ? "REFUNDED" : "REQUESTED"))
                            .refundStage(b.getRefundStage() != null ? b.getRefundStage() : ("REFUNDED".equalsIgnoreCase(b.getStatus()) ? "COMPLETED" : "OPERATOR_AUDIT"))
                            .cancellationReason(b.getCancellationReason() != null ? b.getCancellationReason() : "Customer cancelled booking")
                            .requestedAt(b.getRefundRequestedAt() != null ? b.getRefundRequestedAt() : b.getCreatedAt())
                            .approvedAt(b.getRefundApprovedAt())
                            .build();
                })
                .sorted(Comparator.comparing(OperatorRefundDto::getRequestedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public List<OperatorAiPriceIntelligenceDto> getAiPriceIntelligence(Operator operator) {
        Long opId = operator != null ? operator.getId() : null;
        List<Route> allRoutes = routeRepository.findAll();
        List<Route> operatorRoutes = allRoutes.stream()
                .filter(r -> opId != null && (opId.equals(r.getOperatorId()) || (r.getBus() != null && opId.equals(r.getBus().getOperatorId()))))
                .collect(Collectors.toList());

        // If operator has no registered routes yet, provide intelligence on top platform corridors
        if (operatorRoutes.isEmpty()) {
            operatorRoutes = allRoutes.stream().limit(3).collect(Collectors.toList());
        }

        Map<String, List<Route>> routesByCorridor = allRoutes.stream()
                .collect(Collectors.groupingBy(r -> r.getSourceCity() + " ➔ " + r.getDestinationCity()));

        List<OperatorAiPriceIntelligenceDto> intelligenceList = new ArrayList<>();

        for (Route myRoute : operatorRoutes) {
            String corridorKey = myRoute.getSourceCity() + " ➔ " + myRoute.getDestinationCity();
            List<Route> corridorRoutes = routesByCorridor.getOrDefault(corridorKey, Collections.singletonList(myRoute));

            BigDecimal myPrice = myRoute.getBasePrice() != null ? myRoute.getBasePrice() : new BigDecimal("850.00");
            String myBusType = myRoute.getBus() != null ? myRoute.getBus().getBusType() : "AC Sleeper (2+1)";

            List<BigDecimal> prices = corridorRoutes.stream()
                    .map(r -> r.getBasePrice() != null ? r.getBasePrice() : new BigDecimal("800.00"))
                    .collect(Collectors.toList());

            BigDecimal lowestPrice = prices.stream().min(BigDecimal::compareTo).orElse(myPrice);
            BigDecimal highestPrice = prices.stream().max(BigDecimal::compareTo).orElse(myPrice);

            BigDecimal sumPrices = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgPrice = prices.isEmpty() ? myPrice : sumPrices.divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);

            BigDecimal diffPct = BigDecimal.ZERO;
            if (avgPrice.compareTo(BigDecimal.ZERO) > 0) {
                diffPct = myPrice.subtract(avgPrice).divide(avgPrice, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP);
            }

            String competitiveness;
            String recommendation;
            String promoCode = "RBPROMO" + (Math.abs(diffPct.intValue()) + 5);
            BigDecimal promoDiscount = new BigDecimal("10.00");
            int predictedOccupancy = 78;

            if (diffPct.compareTo(new BigDecimal("-8.0")) <= 0) {
                competitiveness = "HIGHLY_COMPETITIVE";
                recommendation = "Your fare is " + Math.abs(diffPct.doubleValue()) + "% lower than the corridor average. High demand velocity detected! Consider a marginal ₹50 dynamic increase for peak slots or advertise this value fare with promo code '" + promoCode + "' to capture 94%+ load factor.";
                promoCode = "VALUE" + Math.abs(diffPct.intValue());
                promoDiscount = new BigDecimal("5.00");
                predictedOccupancy = 93;
            } else if (diffPct.compareTo(new BigDecimal("8.0")) >= 0) {
                competitiveness = "PREMIUM_OVERPRICED";
                recommendation = "Your fare is " + diffPct.doubleValue() + "% above competitor average. To avoid seat spoilage on weekdays, activate an AI promotional campaign offering ₹" + myPrice.multiply(new BigDecimal("0.12")).setScale(0, RoundingMode.HALF_UP) + " off (Promo: " + promoCode + ").";
                promoDiscount = new BigDecimal("12.00");
                predictedOccupancy = 65;
            } else {
                competitiveness = "OPTIMAL_MARKET_FIT";
                recommendation = "Your pricing is optimally aligned with market equilibrium (within ±5% of corridor benchmark). Maintain current base fare and deploy flash weekend vouchers to maximize yield.";
                promoCode = "SPEEDY" + (int)(Math.random() * 50 + 10);
                promoDiscount = new BigDecimal("8.00");
                predictedOccupancy = 84;
            }

            // Build competitor benchmarks
            List<OperatorAiPriceIntelligenceDto.CompetitorBenchmark> benchmarks = new ArrayList<>();
            for (Route compRoute : corridorRoutes) {
                String compOp = compRoute.getBus() != null && compRoute.getBus().getOperatorName() != null ? compRoute.getBus().getOperatorName() : "Express Partner";
                String compType = compRoute.getBus() != null ? compRoute.getBus().getBusType() : "AC Multi-Axle";
                BigDecimal compPrice = compRoute.getBasePrice() != null ? compRoute.getBasePrice() : new BigDecimal("800.00");
                BigDecimal diffFromMe = compPrice.subtract(myPrice);

                benchmarks.add(OperatorAiPriceIntelligenceDto.CompetitorBenchmark.builder()
                        .operatorName(compOp)
                        .busType(compType)
                        .price(compPrice)
                        .rating(BigDecimal.valueOf(4.2 + (compRoute.getId() % 7) * 0.1).setScale(1, RoundingMode.HALF_UP))
                        .differenceFromMe(diffFromMe)
                        .build());
            }

            intelligenceList.add(OperatorAiPriceIntelligenceDto.builder()
                    .corridor(corridorKey)
                    .myRouteId(myRoute.getId())
                    .myBusType(myBusType)
                    .myCurrentPrice(myPrice)
                    .marketAveragePrice(avgPrice)
                    .marketLowestPrice(lowestPrice)
                    .marketHighestPrice(highestPrice)
                    .priceDifferencePercentage(diffPct)
                    .priceCompetitiveness(competitiveness)
                    .aiRecommendation(recommendation)
                    .suggestedPromoCode(promoCode)
                    .suggestedPromoDiscount(promoDiscount)
                    .predictedDemandOccupancy(predictedOccupancy)
                    .competitorBenchmarks(benchmarks)
                    .build());
        }

        return intelligenceList;
    }

    public OperatorWalletLedgerDto getWalletLedger(Operator operator) {
        Long opId = operator.getId();
        List<OperatorWalletTransaction> txs = operatorWalletTransactionRepository.findByOperatorIdOrderByCreatedAtDesc(opId);

        BigDecimal curBalance = BigDecimal.ZERO;
        if (operator.getUser() != null) {
            User opUser = userRepository.findById(operator.getUser().getId()).orElse(operator.getUser());
            curBalance = opUser.getWalletBalance() != null ? opUser.getWalletBalance() : BigDecimal.ZERO;
        }

        BigDecimal totalEarnings = txs.stream()
                .filter(t -> "CREDIT_TICKET_FARE".equalsIgnoreCase(t.getType()) || "CREDIT".equalsIgnoreCase(t.getType()))
                .map(OperatorWalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRefunds = txs.stream()
                .filter(t -> "DEBIT_REFUND_AUDIT".equalsIgnoreCase(t.getType()) || "DEBIT".equalsIgnoreCase(t.getType()))
                .map(OperatorWalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Booking> bookings = getBookingsForOperator(operator).stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());

        BigDecimal totalCommission = bookings.stream()
                .map(b -> b.getCommissionAmount() != null ? b.getCommissionAmount() : b.getTotalAmount().multiply(new BigDecimal("0.10")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OperatorWalletLedgerDto.WalletTransactionItem> items = txs.stream()
                .map(t -> OperatorWalletLedgerDto.WalletTransactionItem.builder()
                        .id(t.getId())
                        .pnr(t.getPnr())
                        .type(t.getType())
                        .amount(t.getAmount())
                        .balanceAfter(t.getBalanceAfter())
                        .description(t.getDescription())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return OperatorWalletLedgerDto.builder()
                .operatorId(opId)
                .companyName(operator.getCompanyName())
                .currentWalletBalance(curBalance)
                .totalEarningsCredited(totalEarnings)
                .totalRefundsDebited(totalRefunds)
                .totalPlatformCommissionPaid(totalCommission)
                .transactions(items)
                .build();
    }

    public AdminOperatorEarningsDto getAdminOperatorEarnings() {
        List<Operator> operators = operatorRepository.findAll();
        List<Booking> allBookings = bookingRepository.findAll();
        List<Bus> allBuses = busRepository.findAll();
        List<Route> allRoutes = routeRepository.findAll();

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        List<AdminOperatorEarningsDto.OperatorEarningItem> items = new ArrayList<>();

        for (Operator op : operators) {
            Long opId = op.getId();
            List<Bus> opBuses = allBuses.stream().filter(b -> opId.equals(b.getOperatorId())).collect(Collectors.toList());
            Set<Long> busIds = opBuses.stream().map(Bus::getId).collect(Collectors.toSet());

            List<Route> opRoutes = allRoutes.stream()
                    .filter(r -> opId.equals(r.getOperatorId()) || (r.getBus() != null && busIds.contains(r.getBus().getId())))
                    .collect(Collectors.toList());

            List<Booking> opBookings = allBookings.stream()
                    .filter(b -> {
                        if (opId.equals(b.getOperatorId())) return true;
                        if (b.getRoute() != null && (opId.equals(b.getRoute().getOperatorId()) || (b.getRoute().getBus() != null && busIds.contains(b.getRoute().getBus().getId())))) return true;
                        return false;
                    })
                    .collect(Collectors.toList());

            long confirmedCount = opBookings.stream().filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus())).count();
            long cancelledCount = opBookings.stream().filter(b -> "CANCELLED".equalsIgnoreCase(b.getStatus()) || "REFUNDED".equalsIgnoreCase(b.getStatus())).count();

            BigDecimal opGross = opBookings.stream()
                    .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                    .map(Booking::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal opComm = opBookings.stream()
                    .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                    .map(b -> b.getCommissionAmount() != null ? b.getCommissionAmount() : b.getTotalAmount().multiply(new BigDecimal("0.10")))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal opRefunds = opBookings.stream()
                    .filter(b -> "REFUNDED".equalsIgnoreCase(b.getStatus()) && b.getRefundAmount() != null)
                    .map(Booking::getRefundAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal opNet = opGross.subtract(opComm).subtract(opRefunds).max(BigDecimal.ZERO);
            BigDecimal walletBal = (op.getUser() != null && op.getUser().getWalletBalance() != null) ? op.getUser().getWalletBalance() : BigDecimal.ZERO;

            totalGross = totalGross.add(opGross);
            totalCommission = totalCommission.add(opComm);
            totalRefunds = totalRefunds.add(opRefunds);
            totalNet = totalNet.add(opNet);

            items.add(AdminOperatorEarningsDto.OperatorEarningItem.builder()
                    .operatorId(opId)
                    .companyName(op.getCompanyName() != null ? op.getCompanyName() : op.getContactPerson())
                    .contactPerson(op.getContactPerson())
                    .email(op.getEmail())
                    .phone(op.getPhone())
                    .status(op.getStatus())
                    .totalBuses(opBuses.size())
                    .totalRoutes(opRoutes.size())
                    .totalConfirmedBookings(confirmedCount)
                    .totalCancelledBookings(cancelledCount)
                    .grossRevenue(opGross)
                    .commissionPaid(opComm)
                    .netEarnings(opNet)
                    .walletBalance(walletBal)
                    .totalRefundsApproved(opRefunds)
                    .build());
        }

        return AdminOperatorEarningsDto.builder()
                .systemGrossRevenue(totalGross)
                .systemCommissionsCollected(totalCommission)
                .systemTotalRefundsProcessed(totalRefunds)
                .systemNetOperatorPayouts(totalNet)
                .operatorEarnings(items)
                .build();
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

    public List<OperatorPassengerManifestDto> getPassengerManifest(
            Operator operator,
            LocalDate travelDate,
            Long busId,
            Long scheduleId
    ) {
        List<Booking> allBookings = getBookingsForOperator(operator);
        final Schedule targetSchedule = (scheduleId != null)
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

                        return OperatorPassengerManifestDto.builder()
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
                                .busOperator(bus != null ? bus.getOperatorName() : (operator != null && operator.getCompanyName() != null ? operator.getCompanyName() : "Express Coach"))
                                .busRegistration(bus != null ? bus.getRegistrationNumber() : "Fleet Bus")
                                .busType(bus != null ? bus.getBusType() : "AC Coach")
                                .status(b.getStatus())
                                .build();
                    });
                })
                .sorted(Comparator.comparing(OperatorPassengerManifestDto::getSeatNumber, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }
}

