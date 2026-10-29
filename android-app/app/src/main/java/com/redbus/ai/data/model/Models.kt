package com.redbus.ai.data.model

data class RouteItem(
    val id: Int,
    val busId: Int,
    val operatorName: String,
    val busType: String,
    val rating: Double,
    val amenities: List<String>,
    val sourceCity: String,
    val destinationCity: String,
    val departureTime: String,
    val arrivalTime: String,
    val travelDate: String,
    val durationHours: Double,
    val basePrice: Double,
    val availableSeats: Int,
    val boardingPoints: List<String>,
    val droppingPoints: List<String>,
    val busPhotoUrl: String? = null,
    val onTimeProbability: Int = 96,
    val aiTag: String? = null
)

enum class SeatDeck { LOWER, UPPER }
enum class SeatType { SEATER, SLEEPER }
enum class SeatStatus { AVAILABLE, LOCKED, BOOKED }
enum class GenderRestriction { NONE, FEMALE, MALE }

data class SeatItem(
    val id: Int,
    val seatId: Int,
    val seatNumber: String,
    val seatType: SeatType,
    val deck: SeatDeck,
    val rowNum: Int,
    val colNum: Int,
    val status: SeatStatus,
    val genderRestriction: GenderRestriction = GenderRestriction.NONE,
    val price: Double,
    val isAiRecommended: Boolean = false,
    val aiReason: String? = null
)

data class SeatLayoutData(
    val routeId: Int,
    val busId: Int,
    val operatorName: String,
    val busType: String,
    val basePrice: Double,
    val seats: List<SeatItem>
)

data class CityPair(
    val sourceCity: String,
    val destinationCity: String,
    val busCount: Int,
    val minPrice: Double
)

data class PassengerInput(
    val seatId: Int,
    val seatNumber: String,
    val name: String,
    val age: Int,
    val gender: String // "MALE" | "FEMALE" | "OTHER"
)

data class Coupon(
    val code: String,
    val title: String,
    val description: String,
    val discountPercentage: Int,
    val maxDiscountAmount: Double,
    val minBookingAmount: Double
)

data class BookingDetails(
    val id: Int,
    val pnr: String,
    val routeId: Int,
    val sourceCity: String,
    val destinationCity: String,
    val travelDate: String,
    val departureTime: String,
    val arrivalTime: String,
    val operatorName: String,
    val busType: String,
    val totalAmount: Double,
    val status: String, // "CONFIRMED" | "CANCELLED"
    val boardingPoint: String,
    val droppingPoint: String,
    val contactEmail: String,
    val contactPhone: String,
    val couponCode: String? = null,
    val discountAmount: Double = 0.0,
    val hasFreeCancellation: Boolean = false,
    val hasTripGuarantee: Boolean = false,
    val walletAmountUsed: Double = 0.0,
    val createdAt: String,
    val passengers: List<PassengerInput>
)

data class NlpParseRequest(val query: String)

data class NlpParseResult(
    val sourceCity: String? = null,
    val destinationCity: String? = null,
    val travelDate: String? = null,
    val busType: String? = null,
    val maxPrice: Double? = null,
    val timePreference: String? = null,
    val rawQuery: String? = null
)

data class AiChatRequest(
    val sessionId: String? = null,
    val message: String,
    val sourceCity: String? = null,
    val destinationCity: String? = null,
    val travelDate: String? = null
)

data class AiChatResponse(
    val sessionId: String,
    val reply: String,
    val toolExecuted: String? = null,
    val suggestedPrompts: List<String>? = null
)

data class ChatMessageItem(
    val id: String,
    val role: String, // "user" | "assistant"
    val content: String,
    val suggestedPrompts: List<String> = emptyList(),
    val timestamp: String
)

data class SavedTraveller(
    val id: Int,
    val name: String,
    val age: Int,
    val gender: String
)

