package com.redbus.ai.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class OperatorRegisterRequest(
    val companyName: String,
    val contactPerson: String,
    val email: String,
    val phone: String,
    val password: String
)

@Serializable
data class UpdateOperatorProfileRequest(
    val companyName: String? = null,
    val contactPerson: String? = null,
    val phone: String? = null,
    val kycDocUrl: String? = null,
    val bankAccountRef: String? = null
)

@Serializable
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
    val totalBuses: Int = 0,
    val totalSchedules: Int = 0,
    val totalBookings: Long = 0,
    val totalRevenue: Double = 0.0
)

@Serializable
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

@Serializable
data class RouteStat(
    val routeName: String = "",
    val bookingCount: Long = 0,
    val ticketsSold: Long = 0,
    val revenue: Double = 0.0,
    val occupancyPercentage: Double = 0.0
)

@Serializable
data class BusStat(
    val registrationNumber: String = "",
    val busType: String = "",
    val tripsCount: Long = 0,
    val revenue: Double = 0.0
)

@Serializable
data class DailyTimelineStat(
    val date: String = "",
    val revenue: Double = 0.0
)

@Serializable
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

@Serializable
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

@Serializable
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

@Serializable
data class OperatorWalletLedgerDto(
    val operatorId: Long? = null,
    val companyName: String? = null,
    val currentWalletBalance: Double = 0.0,
    val totalEarningsCredited: Double = 0.0,
    val totalRefundsDebited: Double = 0.0,
    val totalPlatformCommissionPaid: Double = 0.0,
    val transactions: List<WalletTransactionItem> = emptyList()
)

@Serializable
data class WalletTransactionItem(
    val id: Long? = null,
    val pnr: String? = null,
    val type: String = "CREDIT_TICKET_FARE",
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val description: String? = null,
    val createdAt: String? = null
)

@Serializable
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

@Serializable
data class CompetitorBenchmark(
    val operatorName: String = "",
    val busType: String = "",
    val price: Double = 0.0,
    val rating: Double = 4.5,
    val differenceFromMe: Double = 0.0
)

@Serializable
data class BusCreateRequest(
    val operatorName: String,
    val busType: String,
    val registrationNumber: String? = null,
    val totalSeats: Int,
    val amenities: String? = null,
    val photoUrls: String? = null
)

@Serializable
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
    val createdAt: String? = null
)

@Serializable
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

@Serializable
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
    val validFrom: String? = null,
    val validTo: String? = null,
    val status: String = "ACTIVE",
    val materializedTripsCount: Int? = 0,
    val createdAt: String? = null
)

@Serializable
data class CreateCouponRequest(
    val code: String,
    val discountPercentage: Double,
    val maxDiscountAmount: Double,
    val minBookingAmount: Double = 0.0,
    val validFrom: String? = null,
    val validTo: String? = null,
    val usageLimit: Int = 1000
)
