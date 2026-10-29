package com.redbus.ai.data.remote

import com.redbus.ai.BuildConfig
import com.redbus.ai.data.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface RedbusApiService {

    @GET("routes/search")
    suspend fun searchRoutes(
        @Query("source") source: String,
        @Query("destination") destination: String,
        @Query("date") date: String,
        @Query("busType") busType: String? = null,
        @Query("sortBy") sortBy: String? = null
    ): List<RouteItem>

    @GET("routes/popular")
    suspend fun getPopularRoutes(): List<CityPair>

    @GET("routes/cities")
    suspend fun getAvailableCities(): List<String>

    @GET("routes/{id}/seats")
    suspend fun getRouteSeats(@Path("id") routeId: Int): SeatLayoutData

    @POST("ai/parse-query")
    suspend fun parseNlpQuery(@Body request: NlpParseRequest): NlpParseResult

    @POST("ai/chat")
    suspend fun chatWithAi(@Body request: AiChatRequest): AiChatResponse

    @POST("bookings")
    suspend fun createBooking(@Body bookingPayload: Map<String, @JvmSuppressWildcards Any?>): BookingDetails

    companion object {
        fun create(): RedbusApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
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

