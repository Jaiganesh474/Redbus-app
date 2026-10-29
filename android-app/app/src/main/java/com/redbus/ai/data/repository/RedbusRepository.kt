package com.redbus.ai.data.repository

import com.redbus.ai.data.model.*
import com.redbus.ai.data.remote.RedbusApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RedbusRepository(
    private val api: RedbusApiService = RedbusApiService.create()
) {
    suspend fun login(email: String, password: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        runCatching { api.login(LoginRequest(email.trim(), password)) }
    }

    suspend fun register(name: String, email: String, phone: String, password: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            runCatching { api.register(RegisterRequest(name.trim(), email.trim(), phone.trim(), password)) }
        }

    suspend fun sendMobileOtp(phone: String): Result<SendMobileOtpResponse> = withContext(Dispatchers.IO) {
        runCatching { api.sendMobileOtp(SendMobileOtpRequest(phone.trim(), "LOGIN")) }
    }

    suspend fun loginWithMobileOtp(phone: String, otp: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        runCatching { api.loginWithMobileOtp(MobileOtpLoginRequest(phone.trim(), otp.trim())) }
    }

    suspend fun getMe(): Result<User> = withContext(Dispatchers.IO) {
        runCatching { api.getMe() }
    }

    suspend fun updateProfile(name: String, phone: String, gender: String?): Result<User> =
        withContext(Dispatchers.IO) {
            runCatching { api.updateProfile(UpdateProfileRequest(name = name, phone = phone, gender = gender)) }
        }

    suspend fun getSavedTravellers(): Result<List<SavedTraveller>> = withContext(Dispatchers.IO) {
        runCatching { api.getSavedTravellers() }
    }

    suspend fun addSavedTraveller(name: String, age: Int, gender: String): Result<SavedTraveller> =
        withContext(Dispatchers.IO) {
            runCatching { api.addSavedTraveller(AddTravellerRequest(name, age, gender)) }
        }

    suspend fun deleteSavedTraveller(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { api.deleteSavedTraveller(id) }
    }

    suspend fun getBanners(): Result<List<Banner>> = withContext(Dispatchers.IO) {
        runCatching { api.getBanners() }
    }

    suspend fun getPopularRoutes(): Result<List<CityPair>> = withContext(Dispatchers.IO) {
        runCatching { api.getPopularRoutes() }
    }

    suspend fun getAvailableCities(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching { api.getAvailableCities() }
    }

    suspend fun searchCitiesWithAi(query: String = "", exclude: String = ""): Result<List<AiCityItem>> =
        withContext(Dispatchers.IO) {
            runCatching { api.searchCitiesWithAi(query = query.trim(), exclude = exclude.trim()) }
        }

    suspend fun getAiRecommendations(userId: Int?): Result<AiRecommendationsResponse> =
        withContext(Dispatchers.IO) {
            runCatching { api.getAiRecommendations(userId = userId) }
        }

    suspend fun searchRoutes(
        source: String,
        destination: String,
        date: String,
        busType: String? = null,
        sortBy: String? = null
    ): Result<List<RouteItem>> = withContext(Dispatchers.IO) {
        runCatching {
            api.searchRoutes(
                source = source,
                destination = destination,
                date = date,
                busType = busType,
                sortBy = sortBy
            )
        }
    }

    suspend fun getRouteSeats(routeId: Int): Result<SeatLayoutData> = withContext(Dispatchers.IO) {
        runCatching { api.getRouteSeats(routeId) }
    }

    suspend fun lockSeats(routeId: Int, seatIds: List<Int>, userId: Int?): Result<LockSeatsResponse> =
        withContext(Dispatchers.IO) {
            runCatching { api.lockSeats(LockSeatsRequest(routeId, seatIds, userId)) }
        }

    suspend fun getAvailableCoupons(): Result<List<Coupon>> = withContext(Dispatchers.IO) {
        runCatching { api.getAvailableCoupons() }
    }

    suspend fun validateCoupon(code: String, bookingAmount: Double): Result<CouponValidationResponse> =
        withContext(Dispatchers.IO) {
            runCatching { api.validateCoupon(ValidateCouponRequest(code = code, bookingAmount = bookingAmount)) }
        }

    suspend fun createBooking(request: CreateBookingRequest): Result<BookingDetails> =
        withContext(Dispatchers.IO) {
            runCatching { api.createBooking(request) }
        }

    suspend fun createPaymentOrder(pnr: String): Result<PaymentOrder> =
        withContext(Dispatchers.IO) {
            runCatching { api.createPaymentOrder(CreatePaymentOrderRequest(pnr)) }
        }

    suspend fun verifyPayment(
        pnr: String,
        orderId: String,
        paymentId: String,
        signature: String
    ): Result<VerifyPaymentResponse> = withContext(Dispatchers.IO) {
        runCatching {
            api.verifyPayment(
                VerifyPaymentRequest(
                    pnr = pnr,
                    razorpayOrderId = orderId,
                    razorpayPaymentId = paymentId,
                    razorpaySignature = signature
                )
            )
        }
    }

    suspend fun getMyBookings(): Result<List<BookingDetails>> = withContext(Dispatchers.IO) {
        runCatching { api.getMyBookings().content.orEmpty() }
    }

    suspend fun getBookingByPnr(pnr: String): Result<BookingDetails> = withContext(Dispatchers.IO) {
        runCatching { api.getBookingByPnr(pnr.trim().uppercase()) }
    }

    suspend fun cancelBooking(pnr: String, reason: String, refundDestination: String): Result<CancelBookingResponse> =
        withContext(Dispatchers.IO) {
            runCatching { api.cancelBooking(pnr, CancelBookingRequest(reason, refundDestination)) }
        }

    suspend fun sendTicketEmail(pnr: String, email: String?): Result<Map<String, Any>> =
        withContext(Dispatchers.IO) {
            runCatching { api.sendTicketEmail(pnr, email) }
        }

    suspend fun parseNaturalLanguageQuery(query: String): Result<NlpParseResult> =
        withContext(Dispatchers.IO) {
            runCatching { api.parseNlpQuery(NlpParseRequest(query)) }
        }

    suspend fun askAiCopilot(
        sessionId: String?,
        message: String,
        userId: Int?,
        source: String,
        destination: String,
        date: String
    ): Result<AiChatResponse> = withContext(Dispatchers.IO) {
        runCatching {
            api.chatWithAi(
                AiChatRequest(
                    sessionId = sessionId,
                    message = message,
                    userId = userId,
                    sourceCity = source,
                    destinationCity = destination,
                    travelDate = date
                )
            )
        }
    }

    suspend fun getBusReviews(busId: Long): Result<ReviewSummary> = withContext(Dispatchers.IO) {
        runCatching { api.getBusReviews(busId) }
    }

    suspend fun createReview(request: CreateReviewRequest): Result<ReviewItem> = withContext(Dispatchers.IO) {
        runCatching { api.createReview(request) }
    }

    suspend fun getDeviceSessions(): Result<List<DeviceSessionItem>> = withContext(Dispatchers.IO) {
        runCatching { api.getDeviceSessions() }
    }

    suspend fun revokeSession(id: Long): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        runCatching { api.revokeSession(id) }
    }

    suspend fun forgotPassword(email: String): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        runCatching { api.forgotPassword(mapOf("email" to email)) }
    }

    suspend fun deactivateAccount(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        runCatching { api.deactivateAccount() }
    }

    suspend fun deleteAccount(password: String): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        runCatching { api.deleteAccount(mapOf("password" to password)) }
    }
}
