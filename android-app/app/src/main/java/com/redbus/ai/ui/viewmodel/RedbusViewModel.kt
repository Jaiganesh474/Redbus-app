package com.redbus.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redbus.ai.data.model.*
import com.redbus.ai.data.repository.RedbusRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

enum class RouteFilter(val label: String) {
    ALL("All Buses"),
    AI_PICK("✨ AI Picks"),
    SLEEPER("AC Sleeper"),
    CHEAPEST("Lowest Price"),
    TOP_RATED("4.6★ & Above")
}

data class RedbusUiState(
    val isDarkTheme: Boolean = false,
    val sourceCity: String = "Bangalore",
    val destinationCity: String = "Chennai",
    val travelDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val nlpQuery: String = "",
    val isLoading: Boolean = false,
    val aiStatusBanner: String? = null,
    val routes: List<RouteItem> = emptyList(),
    val activeFilter: RouteFilter = RouteFilter.ALL,
    val selectedRoute: RouteItem? = null,
    val seatLayout: SeatLayoutData? = null,
    val selectedDeck: SeatDeck = SeatDeck.LOWER,
    val selectedSeats: List<SeatItem> = emptyList(),
    val selectedBoardingPoint: String = "",
    val selectedDroppingPoint: String = "",
    val seatLockSeconds: Int = 600,
    val passengers: List<PassengerInput> = emptyList(),
    val contactEmail: String = "traveller@redbus.ai",
    val contactPhone: String = "+91 9876543210",
    val appliedCoupon: Coupon? = null,
    val couponMessage: String? = null,
    val hasFreeCancellation: Boolean = true,
    val hasTripGuarantee: Boolean = false,
    val useWalletBalance: Boolean = false,
    val walletBalance: Double = 450.0,
    val savedTravellers: List<SavedTraveller> = listOf(
        SavedTraveller(1, "Aarav Sharma", 27, "MALE"),
        SavedTraveller(2, "Priya Nair", 25, "FEMALE"),
        SavedTraveller(3, "Rohan Verma", 31, "MALE")
    ),
    val myBookings: List<BookingDetails> = emptyList(),
    val latestConfirmedBooking: BookingDetails? = null,
    val chatMessages: List<ChatMessageItem> = listOf(
        ChatMessageItem(
            id = "welcome-1",
            role = "assistant",
            content = "Hi! I'm your RedBus AI Travel Copilot ✨. Ask me to find low-vibration sleeper berths, predict bus delays, or apply the best discount coupon for your route!",
            suggestedPrompts = listOf(
                "Find night AC sleeper from Bangalore to Chennai",
                "Which seats have the least vibration?",
                "What coupons give maximum discount today?"
            ),
            timestamp = "Just now"
        )
    ),
    val isAiTyping: Boolean = false
)

class RedbusViewModel(
    private val repository: RedbusRepository = RedbusRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RedbusUiState())
    val uiState: StateFlow<RedbusUiState> = _uiState.asStateFlow()

    val availableCities = repository.availableCities
    val popularRoutes = repository.popularRoutes
    val availableCoupons = repository.availableCoupons

    private var timerJob: Job? = null

    init {
        performSearch()
        seedSampleBooking()
    }

    fun toggleDarkTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun updateCities(source: String, destination: String) {
        _uiState.update {
            it.copy(sourceCity = source, destinationCity = destination)
        }
    }

    fun swapCities() {
        _uiState.update {
            it.copy(
                sourceCity = it.destinationCity,
                destinationCity = it.sourceCity
            )
        }
    }

    fun updateTravelDate(date: String) {
        _uiState.update { it.copy(travelDate = date) }
    }

    fun updateNlpQuery(query: String) {
        _uiState.update { it.copy(nlpQuery = query) }
    }

    fun setFilter(filter: RouteFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun getFilteredRoutes(): List<RouteItem> {
        val state = _uiState.value
        return when (state.activeFilter) {
            RouteFilter.ALL -> state.routes
            RouteFilter.AI_PICK -> state.routes.filter { it.aiTag != null || it.onTimeProbability >= 96 }
            RouteFilter.SLEEPER -> state.routes.filter { it.busType.contains("Sleeper", ignoreCase = true) }
            RouteFilter.CHEAPEST -> state.routes.sortedBy { it.basePrice }
            RouteFilter.TOP_RATED -> state.routes.filter { it.rating >= 4.6 }.sortedByDescending { it.rating }
        }
    }

    fun performSearch(onComplete: (() -> Unit)? = null) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, aiStatusBanner = null) }
            val results = repository.searchRoutes(
                source = state.sourceCity,
                destination = state.destinationCity,
                date = state.travelDate
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    routes = results,
                    activeFilter = RouteFilter.ALL
                )
            }
            onComplete?.invoke()
        }
    }

    fun executeAiNlpSearch(onSuccessNavigate: () -> Unit) {
        val query = _uiState.value.nlpQuery.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val parsed = repository.parseNaturalLanguageQuery(query)
            val src = parsed.sourceCity ?: _uiState.value.sourceCity
            val dst = parsed.destinationCity ?: _uiState.value.destinationCity
            val dt = parsed.travelDate ?: _uiState.value.travelDate

            val results = repository.searchRoutes(src, dst, dt)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    sourceCity = src,
                    destinationCity = dst,
                    travelDate = dt,
                    routes = results,
                    aiStatusBanner = "AI NLP Parsed: $src ➔ $dst on $dt ${parsed.busType?.let { t -> "• $t" } ?: ""}"
                )
            }
            onSuccessNavigate()
        }
    }

    fun selectRouteForSeats(route: RouteItem, onReady: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedRoute = route) }
            val layout = repository.getSeatsForRoute(route)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    seatLayout = layout,
                    selectedDeck = SeatDeck.LOWER,
                    selectedSeats = emptyList(),
                    selectedBoardingPoint = route.boardingPoints.firstOrNull().orEmpty(),
                    selectedDroppingPoint = route.droppingPoints.firstOrNull().orEmpty(),
                    appliedCoupon = null,
                    couponMessage = null
                )
            }
            startSeatLockTimer()
            onReady()
        }
    }

    fun selectDeck(deck: SeatDeck) {
        _uiState.update { it.copy(selectedDeck = deck) }
    }

    fun toggleSeatSelection(seat: SeatItem) {
        if (seat.status != SeatStatus.AVAILABLE) return
        _uiState.update { state ->
            val current = state.selectedSeats.toMutableList()
            val existingIndex = current.indexOfFirst { it.id == seat.id }
            if (existingIndex >= 0) {
                current.removeAt(existingIndex)
            } else if (current.size < 6) {
                current.add(seat)
            }
            val syncedPassengers = current.mapIndexed { idx, s ->
                state.passengers.getOrNull(idx)?.copy(
                    seatId = s.seatId,
                    seatNumber = s.seatNumber
                ) ?: PassengerInput(
                    seatId = s.seatId,
                    seatNumber = s.seatNumber,
                    name = state.savedTravellers.getOrNull(idx)?.name ?: "",
                    age = state.savedTravellers.getOrNull(idx)?.age ?: 26,
                    gender = if (s.genderRestriction == GenderRestriction.FEMALE) "FEMALE"
                    else state.savedTravellers.getOrNull(idx)?.gender ?: "MALE"
                )
            }
            state.copy(selectedSeats = current, passengers = syncedPassengers)
        }
    }

    fun applyAiSmartSeatPick() {
        val layout = _uiState.value.seatLayout ?: return
        val bestSeat = layout.seats.firstOrNull { it.isAiRecommended && it.status == SeatStatus.AVAILABLE }
            ?: layout.seats.firstOrNull { it.status == SeatStatus.AVAILABLE }
            ?: return

        _uiState.update { state ->
            if (state.selectedSeats.any { it.id == bestSeat.id }) state
            else {
                val updated = (state.selectedSeats + bestSeat).take(6)
                val syncedPassengers = updated.mapIndexed { idx, s ->
                    PassengerInput(
                        seatId = s.seatId,
                        seatNumber = s.seatNumber,
                        name = state.savedTravellers.getOrNull(idx)?.name ?: "Aarav Sharma",
                        age = state.savedTravellers.getOrNull(idx)?.age ?: 27,
                        gender = if (s.genderRestriction == GenderRestriction.FEMALE) "FEMALE" else "MALE"
                    )
                }
                state.copy(
                    selectedDeck = bestSeat.deck,
                    selectedSeats = updated,
                    passengers = syncedPassengers
                )
            }
        }
    }

    fun updateBoardingDropping(boarding: String, dropping: String) {
        _uiState.update {
            it.copy(selectedBoardingPoint = boarding, selectedDroppingPoint = dropping)
        }
    }

    fun updatePassenger(index: Int, name: String, age: Int, gender: String) {
        _uiState.update { state ->
            val list = state.passengers.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(name = name, age = age, gender = gender)
            }
            state.copy(passengers = list)
        }
    }

    fun updateContactDetails(email: String, phone: String) {
        _uiState.update { it.copy(contactEmail = email, contactPhone = phone) }
    }

    fun toggleFreeCancellation(enabled: Boolean) {
        _uiState.update { it.copy(hasFreeCancellation = enabled) }
    }

    fun toggleTripGuarantee(enabled: Boolean) {
        _uiState.update { it.copy(hasTripGuarantee = enabled) }
    }

    fun toggleUseWallet(enabled: Boolean) {
        _uiState.update { it.copy(useWalletBalance = enabled) }
    }

    fun applyCouponCode(code: String) {
        val clean = code.trim().uppercase()
        val match = availableCoupons.firstOrNull { it.code.equals(clean, ignoreCase = true) }
        val baseSeatTotal = _uiState.value.selectedSeats.sumOf { it.price }
        if (match == null) {
            _uiState.update {
                it.copy(appliedCoupon = null, couponMessage = "Invalid coupon code. Try REDBUSAI20 or FIRST50")
            }
        } else if (baseSeatTotal < match.minBookingAmount) {
            _uiState.update {
                it.copy(
                    appliedCoupon = null,
                    couponMessage = "Minimum fare of ₹${match.minBookingAmount.toInt()} required for ${match.code}"
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    appliedCoupon = match,
                    couponMessage = "🎉 ${match.code} applied! Saving ${match.discountPercentage}% on your trip"
                )
            }
        }
    }

    fun removeCoupon() {
        _uiState.update { it.copy(appliedCoupon = null, couponMessage = null) }
    }

    fun calculateFareBreakdown(): FareBreakdown {
        val state = _uiState.value
        val baseFare = state.selectedSeats.sumOf { it.price }
        val freeCancelFee = if (state.hasFreeCancellation) state.selectedSeats.size * 49.0 else 0.0
        val tripGuaranteeFee = if (state.hasTripGuarantee) state.selectedSeats.size * 69.0 else 0.0
        val gstAndService = (baseFare * 0.05).coerceAtLeast(25.0)
        val discount = state.appliedCoupon?.let { coupon ->
            ((baseFare * coupon.discountPercentage) / 100.0).coerceAtMost(coupon.maxDiscountAmount)
        } ?: 0.0
        val gross = (baseFare + freeCancelFee + tripGuaranteeFee + gstAndService - discount).coerceAtLeast(0.0)
        val walletUsed = if (state.useWalletBalance) state.walletBalance.coerceAtMost(gross) else 0.0
        val netPayable = (gross - walletUsed).coerceAtLeast(0.0)

        return FareBreakdown(
            baseFare = baseFare,
            freeCancelFee = freeCancelFee,
            tripGuaranteeFee = tripGuaranteeFee,
            gstAndService = gstAndService,
            discountAmount = discount,
            walletUsed = walletUsed,
            netPayable = netPayable
        )
    }

    fun confirmBookingAndPay(onSuccess: () -> Unit) {
        val state = _uiState.value
        val route = state.selectedRoute ?: return
        val fare = calculateFareBreakdown()
        val pnr = "RBAI${Random.nextInt(100000, 999999)}"

        val newBooking = BookingDetails(
            id = Random.nextInt(1000, 9999),
            pnr = pnr,
            routeId = route.id,
            sourceCity = route.sourceCity,
            destinationCity = route.destinationCity,
            travelDate = route.travelDate,
            departureTime = route.departureTime,
            arrivalTime = route.arrivalTime,
            operatorName = route.operatorName,
            busType = route.busType,
            totalAmount = fare.netPayable,
            status = "CONFIRMED",
            boardingPoint = state.selectedBoardingPoint,
            droppingPoint = state.selectedDroppingPoint,
            contactEmail = state.contactEmail,
            contactPhone = state.contactPhone,
            couponCode = state.appliedCoupon?.code,
            discountAmount = fare.discountAmount,
            hasFreeCancellation = state.hasFreeCancellation,
            hasTripGuarantee = state.hasTripGuarantee,
            walletAmountUsed = fare.walletUsed,
            createdAt = LocalDate.now().toString(),
            passengers = state.passengers.mapIndexed { idx, p ->
                if (p.name.isBlank()) p.copy(name = "Traveller ${idx + 1}") else p
            }
        )

        _uiState.update {
            it.copy(
                walletBalance = (it.walletBalance - fare.walletUsed).coerceAtLeast(0.0),
                myBookings = listOf(newBooking) + it.myBookings,
                latestConfirmedBooking = newBooking
            )
        }
        onSuccess()
    }

    fun cancelBookingToWallet(pnr: String) {
        _uiState.update { state ->
            var refunded = 0.0
            val updatedList = state.myBookings.map { b ->
                if (b.pnr == pnr && b.status == "CONFIRMED") {
                    refunded = if (b.hasFreeCancellation) b.totalAmount else b.totalAmount * 0.85
                    b.copy(status = "CANCELLED")
                } else b
            }
            state.copy(
                myBookings = updatedList,
                walletBalance = state.walletBalance + refunded
            )
        }
    }

    fun sendAiMessage(userText: String) {
        val clean = userText.trim()
        if (clean.isEmpty()) return

        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val userMsg = ChatMessageItem(
            id = "u-${System.currentTimeMillis()}",
            role = "user",
            content = clean,
            timestamp = now
        )

        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMsg,
                isAiTyping = true
            )
        }

        viewModelScope.launch {
            delay(550)
            val state = _uiState.value
            val response = repository.askAiCopilot(
                message = clean,
                source = state.sourceCity,
                destination = state.destinationCity,
                date = state.travelDate
            )
            val aiMsg = ChatMessageItem(
                id = "ai-${System.currentTimeMillis()}",
                role = "assistant",
                content = response.reply,
                suggestedPrompts = response.suggestedPrompts.orEmpty(),
                timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            )
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + aiMsg,
                    isAiTyping = false
                )
            }
        }
    }

    private fun startSeatLockTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(seatLockSeconds = 600) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.seatLockSeconds > 0) {
                delay(1000L)
                _uiState.update { it.copy(seatLockSeconds = (it.seatLockSeconds - 1).coerceAtLeast(0)) }
            }
        }
    }

    private fun seedSampleBooking() {
        val sample = BookingDetails(
            id = 901,
            pnr = "RBAI849201",
            routeId = 101,
            sourceCity = "Bangalore",
            destinationCity = "Chennai",
            travelDate = LocalDate.now().plusDays(1).toString(),
            departureTime = "21:30",
            arrivalTime = "04:15",
            operatorName = "IntrCity SmartBus AI",
            busType = "Bharat Benz AC Sleeper (2+1)",
            totalAmount = 749.0,
            status = "CONFIRMED",
            boardingPoint = "Bangalore Silk Board (21:30)",
            droppingPoint = "Chennai Koyambedu (04:00)",
            contactEmail = "traveller@redbus.ai",
            contactPhone = "+91 9876543210",
            couponCode = "REDBUSAI20",
            discountAmount = 149.0,
            hasFreeCancellation = true,
            hasTripGuarantee = true,
            walletAmountUsed = 0.0,
            createdAt = LocalDate.now().toString(),
            passengers = listOf(
                PassengerInput(seatId = 4, seatNumber = "L4", name = "Aarav Sharma", age = 27, gender = "MALE")
            )
        )
        _uiState.update { it.copy(myBookings = listOf(sample)) }
    }
}

data class FareBreakdown(
    val baseFare: Double,
    val freeCancelFee: Double,
    val tripGuaranteeFee: Double,
    val gstAndService: Double,
    val discountAmount: Double,
    val walletUsed: Double,
    val netPayable: Double
)

