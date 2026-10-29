package com.redbus.ai.data.remote

import com.redbus.ai.BuildConfig
import com.redbus.ai.data.local.SessionManager
import com.redbus.ai.data.model.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface RedbusApiService {

    // 1. Authentication & Cross-Platform Account Endpoints (same as https://www.redbusai.app)
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/otp/send")
    suspend fun sendMobileOtp(@Body request: SendMobileOtpRequest): SendMobileOtpResponse

    @POST("auth/otp/login")
    suspend fun loginWithMobileOtp(@Body request: MobileOtpLoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun getMe(): User

    @PUT("auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): User

    // 2. Saved Travellers (synced with Web Profile)
    @GET("auth/saved-travellers")
    suspend fun getSavedTravellers(): List<SavedTraveller>

    @POST("auth/saved-travellers")
    suspend fun addSavedTraveller(@Body request: AddTravellerRequest): SavedTraveller

    @DELETE("auth/saved-travellers/{id}")
    suspend fun deleteSavedTraveller(@Path("id") id: Int)

    // 3. Banners, Popular Routes, Cities & AI Recommendations
    @GET("banners")
    suspend fun getBanners(): List<Banner>

    @GET("routes/popular")
    suspend fun getPopularRoutes(): List<CityPair>

    @GET("routes/cities")
    suspend fun getAvailableCities(): List<String>

    @GET("ai/recommendations")
    suspend fun getAiRecommendations(
        @Query("userId") userId: Int? = null,
        @Query("source") source: String? = null,
        @Query("destination") destination: String? = null
    ): AiRecommendationsResponse

    // 4. Route Search & Live Seat Layout
    @GET("routes/search")
    suspend fun searchRoutes(
        @Query("source") source: String,
        @Query("destination") destination: String,
        @Query("date") date: String,
        @Query("busType") busType: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("departureWindow") departureWindow: String? = null,
        @Query("sortBy") sortBy: String? = null
    ): List<RouteItem>

    @GET("routes/{id}")
    suspend fun getRouteById(@Path("id") routeId: Int): RouteItem

    @GET("routes/{id}/seats")
    suspend fun getRouteSeats(@Path("id") routeId: Int): SeatLayoutData

    @GET("ml/smart-seat-recommendations")
    suspend fun getSmartSeatRecommendations(
        @Query("scheduleId") scheduleId: Int,
        @Query("userId") userId: Int? = null,
        @Query("gender") gender: String? = null
    ): SmartSeatRecommendation

    // 5. Real-Time Seat Locking
    @POST("seats/lock")
    suspend fun lockSeats(@Body request: LockSeatsRequest): LockSeatsResponse

    @POST("seats/unlock")
    suspend fun unlockSeats(@Body request: LockSeatsRequest): Map<String, Boolean>

    // 6. Coupons
    @GET("coupons/available")
    suspend fun getAvailableCoupons(@Query("operatorId") operatorId: Int? = null): List<Coupon>

    @POST("coupons/validate")
    suspend fun validateCoupon(@Body request: ValidateCouponRequest): CouponValidationResponse

    // 7. Bookings & Cancellation
    @POST("bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): BookingDetails

    @GET("bookings/my-bookings")
    suspend fun getMyBookings(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PaginatedBookingsResponse

    @GET("bookings/{pnr}")
    suspend fun getBookingByPnr(@Path("pnr") pnr: String): BookingDetails

    @POST("bookings/{pnr}/cancel")
    suspend fun cancelBooking(
        @Path("pnr") pnr: String,
        @Body request: CancelBookingRequest
    ): CancelBookingResponse

    @POST("bookings/{pnr}/send-ticket")
    suspend fun sendTicketEmail(
        @Path("pnr") pnr: String,
        @Query("email") email: String? = null
    ): Map<String, Any>

    // 8. AI NLP & Chat Copilot
    @POST("ai/parse-query")
    suspend fun parseNlpQuery(@Body request: NlpParseRequest): NlpParseResult

    @POST("ai/chat")
    suspend fun chatWithAi(@Body request: AiChatRequest): AiChatResponse

    companion object {
        fun create(): RedbusApiService {
            val authInterceptor = Interceptor { chain ->
                val original = chain.request()
                val token = SessionManager.getToken()
                val requestBuilder = original.newBuilder()
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }

            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logger)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RedbusApiService::class.java)
        }
    }
}
