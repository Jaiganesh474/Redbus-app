package com.redbus.ai.data.model

data class OperatorRegisterRequest(
    val companyName: String,
    val contactPerson: String,
    val email: String,
    val phone: String,
    val password: String
)

data class UpdateOperatorProfileRequest(
    val companyName: String? = null,
    val contactPerson: String? = null,
    val phone: String? = null,
    val kycDocUrl: String? = null,
    val bankAccountRef: String? = null
)

data class OperatorDto(
    val id: Long? = null,
    val userId: Long? = null,
    val companyName: String? = null,
    val contactPerson: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val kycDocUrl: String? = null,
    val bankAccountRef: String? = null,
    val commissionRate: Double? = 10.0,
    val status: String? = "APPROVED",
    val primoCertified: Boolean? = false,
    val totalBuses: Int = 0,
    val totalSchedules: Int = 0,
    val totalBookings: Long = 0,
    val totalRevenue: Double = 0.0
)

data class OperatorAnalyticsDto(
    val operatorName: String? = null,
    val companyName: String? = null,
    val totalRevenue: Double = 0.0,
    val netEarnings: Double = 0.0,
    val commissionPaid: Double = 0.0,
    val ticketsSold: Long = 0,
    val activeFleetCount: Long = 0,
    val activeSchedulesCount: Long = 0,
    val totalTripsRun: Long = 0,
    val averageOccupancyPercentage: Double = 0.0,
    val routePerformance: List<RouteStat> = emptyList(),
    val busPerformance: List<BusStat> = emptyList(),
    val dailyTimeline: List<DailyTimelineStat> = emptyList(),
    val recentBookings: List<OperatorBookingDto> = emptyList()
)

data class RouteStat(
    val routeName: String = "",
    val bookingCount: Long = 0,
    val ticketsSold: Long = 0,
    val revenue: Double = 0.0,
    val occupancyPercentage: Double = 0.0
)

data class BusStat(
    val registrationNumber: String = "",
    val busType: String = "",
    val tripsCount: Long = 0,
    val revenue: Double = 0.0
)

data class DailyTimelineStat(
    val date: String = "",
    val revenue: Double = 0.0,
    val ticketsSold: Long = 0
)

data class OperatorBookingDto(
    val id: Long? = null,
    val pnr: String = "",
    val passengerName: String = "",
    val passengerEmail: String? = null,
    val passengerPhone: String? = null,
    val route: String = "",
    val travelDate: String? = null,
    val busName: String = "",
    val seatNumbers: List<String> = emptyList(),
    val seatCount: Int = 0,
    val totalAmount: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val status: String = "CONFIRMED",
    val bookingTime: String? = null
)

data class OperatorPassengerManifestDto(
    val bookingId: Long? = null,
    val pnr: String = "",
    val passengerName: String = "",
    val age: Int? = null,
    val gender: String? = null,
    val seatNumber: String = "",
    val seatType: String? = null,
    val deck: String? = null,
    val berthLabel: String? = null,
    val seatDisplay: String? = null,
    val boardingPoint: String? = null,
    val droppingPoint: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val travelDate: String? = null,
    val sourceCity: String? = null,
    val destinationCity: String? = null,
    val departureTime: String? = null,
    val arrivalTime: String? = null,
    val busOperator: String? = null,
    val busRegistration: String? = null,
    val busType: String? = null,
    val status: String? = "CONFIRMED"
)

data class OperatorRefundDto(
    val bookingId: Long? = null,
    val pnr: String = "",
    val passengerName: String = "",
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val sourceCity: String? = null,
    val destinationCity: String? = null,
    val travelDate: String? = null,
    val busName: String? = null,
    val seatNumbers: List<String> = emptyList(),
    val totalPaid: Double = 0.0,
    val refundAmount: Double = 0.0,
    val refundDestination: String? = "WALLET",
    val refundStatus: String = "REQUESTED",
    val refundStage: String? = "REQUESTED",
    val cancellationReason: String? = null,
    val requestedAt: String? = null,
    val approvedAt: String? = null
)

data class OperatorWalletLedgerDto(
    val operatorId: Long? = null,
    val companyName: String? = null,
    val currentWalletBalance: Double = 0.0,
    val totalEarningsCredited: Double = 0.0,
    val totalRefundsDebited: Double = 0.0,
    val totalPlatformCommissionPaid: Double = 0.0,
    val transactions: List<WalletTransactionItem> = emptyList()
)

data class WalletTransactionItem(
    val id: Long? = null,
    val pnr: String? = null,
    val type: String = "CREDIT_TICKET_FARE",
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val description: String? = null,
    val createdAt: String? = null
)

data class OperatorAiPriceIntelligenceDto(
    val corridor: String = "",
    val myRouteId: Long? = null,
    val myBusType: String? = null,
    val myCurrentPrice: Double = 0.0,
    val marketAveragePrice: Double = 0.0,
    val marketLowestPrice: Double = 0.0,
    val marketHighestPrice: Double = 0.0,
    val priceDifferencePercentage: Double = 0.0,
    val priceCompetitiveness: String = "OPTIMAL",
    val aiRecommendation: String = "",
    val suggestedPromoCode: String? = null,
    val suggestedPromoDiscount: Double? = null,
    val predictedDemandOccupancy: Int? = 75,
    val competitorBenchmarks: List<CompetitorBenchmark> = emptyList()
)

data class CompetitorBenchmark(
    val operatorName: String = "",
    val busType: String = "",
    val price: Double = 0.0,
    val rating: Double = 4.5,
    val differenceFromMe: Double = 0.0
)

data class BusCreateRequest(
    val operatorName: String,
    val busType: String,
    val registrationNumber: String? = null,
    val totalSeats: Int,
    val amenities: String? = null,
    val photoUrls: String? = null
)

data class BusResponseDto(
    val id: Long = 0,
    val operatorId: Long? = null,
    val operatorName: String = "",
    val registrationNumber: String? = null,
    val busType: String = "",
    val totalSeats: Int = 40,
    val amenities: String? = null,
    val photoUrls: String? = null,
    val active: Boolean? = true,
    val rating: Double? = 4.5,
    val primoCertified: Boolean? = false,
    val createdAt: String? = null
)

data class ScheduleCreateRequest(
    val busId: Long,
    val sourceCity: String,
    val destinationCity: String,
    val departureTime: String,
    val arrivalTime: String,
    val basePrice: Double,
    val operatingDays: String = "DAILY",
    val boardingPoints: String? = null,
    val droppingPoints: String? = null,
    val validFrom: String,
    val validTo: String
)

data class ScheduleResponseDto(
    val id: Long = 0,
    val operatorId: Long? = null,
    val routeId: Long? = null,
    val busId: Long = 0,
    val busName: String? = null,
    val busType: String? = null,
    val sourceCity: String = "",
    val destinationCity: String = "",
    val departureTime: String = "",
    val arrivalTime: String = "",
    val operatingDays: String = "DAILY",
    val basePrice: Double = 0.0,
    val boardingPoints: String? = null,
    val droppingPoints: String? = null,
    val validFrom: String? = null,
    val validTo: String? = null,
    val status: String = "ACTIVE",
    val materializedTripsCount: Int? = 0,
    val createdAt: String? = null
)

data class CreateCouponRequest(
    val code: String,
    val discountPercentage: Double,
    val maxDiscountAmount: Double,
    val minBookingAmount: Double = 0.0,
    val validFrom: String? = null,
    val validTo: String? = null,
    val usageLimit: Int = 1000
)

data class AdminStatsDto(
    val totalBookings: Long = 0,
    val confirmedBookings: Long = 0,
    val cancelledBookings: Long = 0,
    val totalRevenue: Double = 0.0,
    val occupancyRate: Double = 0.0,
    val totalRoutes: Long = 0,
    val totalBuses: Long = 0,
    val primoOperatorsCount: Long? = 0,
    val primoBusesCount: Long? = 0,
    val activeSchedulesCount: Long? = 0,
    val approvedOperatorsCount: Long? = 0,
    val topRoutes: List<CityPair>? = emptyList()
)

data class AdminOperatorEarningItem(
    val operatorId: Long = 0,
    val companyName: String = "",
    val email: String = "",
    val status: String = "APPROVED",
    val totalConfirmedBookings: Long = 0,
    val grossRevenue: Double = 0.0,
    val commissionPaid: Double = 0.0,
    val netEarnings: Double = 0.0,
    val totalRefundsApproved: Double = 0.0,
    val walletBalance: Double = 0.0
)

data class AdminOperatorEarningsDto(
    val systemGrossRevenue: Double = 0.0,
    val systemCommissionsCollected: Double = 0.0,
    val systemNetOperatorPayouts: Double = 0.0,
    val systemTotalRefundsProcessed: Double = 0.0,
    val operatorEarnings: List<AdminOperatorEarningItem> = emptyList()
)

data class CreateBannerRequest(
    val title: String,
    val subtitle: String? = null,
    val tag: String? = "EXCLUSIVE OFFER",
    val promoCode: String? = null,
    val discountPercentage: Int? = 20,
    val bgGradient: String? = "from-slate-950 via-red-950/80 to-slate-900",
    val badgeColor: String? = "bg-red-500/20 text-red-300 border-red-500/30",
    val routeInfo: String? = "All India Routes",
    val imageUrl: String? = "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=1200&q=80",
    val ctaText: String? = "Book Now",
    val ctaLink: String? = "/search",
    val active: Boolean = true,
    val sortOrder: Int = 1,
    val isAiGenerated: Boolean = true
)

data class GenerateAiBannerRequest(
    val prompt: String,
    val targetRoute: String? = null,
    val targetDiscount: Int? = 25
)
