package com.redbus.ai.data.model

import com.google.gson.JsonElement

data class User(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val role: String = "ROLE_USER",
    val roles: List<String>? = null,
    val emailVerified: Boolean? = false,
    val avatarUrl: String? = null,
    val gender: String? = null,
    val operatorStatus: String? = null,
    val walletBalance: Double? = 0.0,
    val isActive: Boolean? = true,
    val status: String? = null
)

data class AuthResponse(
    val token: String,
    val user: User,
    val message: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String
)

data class SendMobileOtpRequest(
    val phone: String,
    val purpose: String = "LOGIN"
)

data class SendMobileOtpResponse(
    val success: Boolean = false,
    val message: String? = null,
    val phone: String? = null,
    val expiresInSeconds: Int? = 60,
    val previewOtp: String? = null
)

data class MobileOtpLoginRequest(
    val phone: String,
    val otp: String,
    val name: String? = null
)

data class UpdateProfileRequest(
    val name: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val avatarUrl: String? = null,
    val currentPassword: String? = null,
    val newPassword: String? = null
)

data class SavedTraveller(
    val id: Int = 0,
    val name: String = "",
    val age: Int = 18,
    val gender: String = "MALE"
)

data class AddTravellerRequest(
    val name: String,
    val age: Int,
    val gender: String
)

data class Banner(
    val id: Int = 0,
    val tag: String? = null,
    val title: String = "",
    val subtitle: String? = null,
    val ctaText: String? = null,
    val ctaLink: String? = null,
    val bgGradient: String? = null,
    val badgeColor: String? = null,
    val imageUrl: String? = null,
    val promoCode: String? = null,
    val discountPercentage: Int? = null,
    val routeInfo: String? = null,
    val isAiGenerated: Boolean? = false
)

data class RouteItem(
    val id: Int = 0,
    val busId: Int = 0,
    val operatorName: String = "",
    val busType: String = "",
    val rating: Double = 4.5,
    val amenities: List<String>? = emptyList(),
    val sourceCity: String = "",
    val destinationCity: String = "",
    val departureTime: String = "",
    val arrivalTime: String = "",
    val travelDate: String = "",
    val durationHours: Double = 0.0,
    val basePrice: Double = 0.0,
    val availableSeats: Int = 0,
    val boardingPoints: List<String>? = emptyList(),
    val droppingPoints: List<String>? = emptyList(),
    val busPhotoUrl: String? = null
)

data class AiRecommendationsResponse(
    val recommendedRoutes: List<RouteItem>? = emptyList(),
    val reason: String? = null
)

data class SeatItem(
    val id: Int = 0,
    val seatId: Int = 0,
    val seatNumber: String = "",
    val seatType: String = "SLEEPER", // "SEATER" | "SLEEPER"
    val deck: String = "LOWER", // "LOWER" | "UPPER"
    val rowNum: Int = 1,
    val colNum: Int = 1,
    val status: String = "AVAILABLE", // "AVAILABLE" | "LOCKED" | "BOOKED"
    val genderRestriction: String? = "NONE", // "NONE" | "FEMALE" | "MALE"
    val bookedGender: String? = null,
    val price: Double = 0.0,
    val lockedByUserId: Int? = null
)

data class SeatLayoutData(
    val routeId: Int = 0,
    val busId: Int = 0,
    val operatorName: String = "",
    val busType: String = "",
    val basePrice: Double = 0.0,
    val seats: List<SeatItem> = emptyList()
)

data class LockSeatsRequest(
    val routeId: Int,
    val seatIds: List<Int>,
    val userId: Int? = null
)

data class LockSeatsResponse(
    val success: Boolean = false,
    val expiresAt: String? = null,
    val lockedSeatIds: List<Int>? = emptyList(),
    val lockExpiry: String? = null,
    val remainingSeconds: Int? = 600
)

data class CityPair(
    val sourceCity: String = "",
    val destinationCity: String = "",
    val busCount: Int = 0,
    val minPrice: Double = 0.0
)

data class AiCityItem(
    val name: String = "",
    val state: String = "",
    val tag: String = "",
    val aliases: List<String>? = emptyList(),
    val activeRoutesCount: Int = 0
)

data class PassengerInput(
    val seatId: Int,
    val seatNumber: String,
    val name: String,
    val age: Int,
    val gender: String
)

data class CreateBookingRequest(
    val routeId: Int,
    val boardingPoint: String,
    val droppingPoint: String,
    val contactEmail: String,
    val contactPhone: String,
    val passengers: List<PassengerInput>,
    val couponCode: String? = null,
    val hasFreeCancellation: Boolean = false,
    val hasTripGuarantee: Boolean = false,
    val serviceFee: Double = 25.0,
    val useWalletBalance: Boolean = false
)

data class CreatePaymentOrderRequest(
    val pnr: String
)

data class PaymentOrder(
    val orderId: String = "",
    val currency: String = "INR",
    val amountInPaise: Int = 0,
    val amount: Double = 0.0,
    val keyId: String = "",
    val pnr: String = ""
)

data class VerifyPaymentRequest(
    val pnr: String,
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val razorpaySignature: String
)

data class VerifyPaymentResponse(
    val success: Boolean = false,
    val pnr: String = "",
    val message: String? = null
)

data class BookingDetails(
    val id: Int = 0,
    val pnr: String = "",
    val routeId: Int = 0,
    val sourceCity: String = "",
    val destinationCity: String = "",
    val travelDate: String = "",
    val departureTime: String = "",
    val arrivalTime: String = "",
    val operatorName: String = "",
    val busType: String = "",
    val totalAmount: Double = 0.0,
    val status: String = "CONFIRMED",
    val refundStatus: String? = null,
    val refundDestination: String? = null,
    val refundStage: String? = null,
    val boardingPoint: String = "",
    val droppingPoint: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val couponCode: String? = null,
    val discountAmount: Double? = 0.0,
    val hasFreeCancellation: Boolean? = false,
    val freeCancellationFee: Double? = 0.0,
    val hasTripGuarantee: Boolean? = false,
    val tripGuaranteeFee: Double? = 0.0,
    val serviceFee: Double? = 0.0,
    val walletAmountUsed: Double? = 0.0,
    val refundAmount: Double? = null,
    val createdAt: String = "",
    val passengers: List<PassengerInput>? = emptyList()
)

data class PaginatedBookingsResponse(
    val content: List<BookingDetails>? = emptyList(),
    val totalElements: Int? = 0
)

data class CancelBookingRequest(
    val reason: String = "Change of plans",
    val refundDestination: String = "WALLET"
)

data class CancelBookingResponse(
    val pnr: String = "",
    val status: String = "",
    val refundAmount: Double = 0.0,
    val walletBalance: Double? = null,
    val message: String = ""
)

data class Coupon(
    val id: Int = 0,
    val operatorId: Int? = null,
    val operatorName: String? = null,
    val code: String = "",
    val title: String? = null,
    val description: String? = null,
    val discountPercentage: Int = 0,
    val maxDiscountAmount: Double = 0.0,
    val minBookingAmount: Double = 0.0,
    val expiryDate: String? = null
)

data class ValidateCouponRequest(
    val code: String,
    val bookingAmount: Double,
    val operatorId: Int? = null
)

data class CouponValidationResponse(
    val valid: Boolean = false,
    val code: String? = null,
    val discountPercentage: Int? = null,
    val discountAmount: Double? = null,
    val finalAmount: Double? = null,
    val message: String? = null
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
    val userId: Int? = null,
    val sourceCity: String? = null,
    val destinationCity: String? = null,
    val travelDate: String? = null
)

data class AiChatResponse(
    val sessionId: String? = null,
    val reply: String = "",
    val toolExecuted: String? = null,
    val toolData: JsonElement? = null,
    val suggestedPrompts: List<String>? = null
)

data class ChatMessageItem(
    val id: String,
    val role: String,
    val content: String,
    val suggestedPrompts: List<String> = emptyList(),
    val timestamp: String,
    val routes: List<RouteItem>? = null
)

data class SmartSeatRecommendation(
    val recommendedSeats: List<String>? = emptyList(),
    val reason: String? = null
)

data class ReviewItem(
    val id: Long = 0,
    val busId: Long = 0,
    val userId: Long? = null,
    val userName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val tags: List<String>? = emptyList(),
    val createdAt: String? = null
)

data class ReviewSummary(
    val averageRating: Double = 4.5,
    val totalRatings: Int = 427,
    val starPercentages: Map<String, Int>? = emptyMap(),
    val lovedTags: Map<String, Int>? = emptyMap(),
    val reviews: List<ReviewItem> = emptyList()
)

data class CreateReviewRequest(
    val busId: Long,
    val rating: Int,
    val comment: String,
    val userName: String? = null,
    val tags: List<String>? = emptyList()
)

data class DeviceSessionItem(
    val id: Long = 0,
    val deviceName: String = "",
    val deviceType: String? = "MOBILE",
    val ipAddress: String? = null,
    val location: String? = null,
    val lastActive: String? = null,
    val isCurrent: Boolean = false,
    val isActive: Boolean = true
)
