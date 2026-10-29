package com.redbus.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redbus.ai.data.local.SessionManager
import com.redbus.ai.data.model.*
import com.redbus.ai.data.repository.RedbusRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class RouteFilter(val label: String) {
    ALL("All Buses"),
    PRIMO("★ Primo (4.4+)"),
    SLEEPER("Sleeper"),
    SEATER("Seater"),
    AC("AC"),
    CHEAPEST("Price: Low to High")
}

data class RedbusUiState(
    val isDarkTheme: Boolean = SessionManager.isDarkMode(),
    // Live Authenticated Account State (synced with https://www.redbusai.app)
    val user: User? = SessionManager.getUser(),
    val token: String? = SessionManager.getToken(),
    val isAuthenticated: Boolean = SessionManager.getToken() != null,
    val isAuthLoading: Boolean = false,
    val authError: String? = null,
    val authSuccessMessage: String? = null,
    val otpSent: Boolean = false,
    // Live Home & Discovery Data from Backend
    val availableCities: List<String> = emptyList(),
    val popularRoutes: List<CityPair> = emptyList(),
    val banners: List<Banner> = emptyList(),
    val aiRecommendedRoutes: List<RouteItem> = emptyList(),
    val aiRecommendationReason: String? = null,
    val availableCoupons: List<Coupon> = emptyList(),
    // Search Parameters & Results
    val sourceCity: String = "Bangalore",
    val destinationCity: String = "Chennai",
    val travelDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val nlpQuery: String = "",
    val isLoading: Boolean = false,
    val errorBanner: String? = null,
    val aiStatusBanner: String? = null,
    val routes: List<RouteItem> = emptyList(),
    val activeFilter: RouteFilter = RouteFilter.ALL,
    // Seat Layout & Real-Time Seat Locking
    val selectedRoute: RouteItem? = null,
    val seatLayout: SeatLayoutData? = null,
    val selectedDeck: String = "LOWER",
    val selectedSeats: List<SeatItem> = emptyList(),
    val lockedSeatIds: List<Int> = emptyList(),
    val selectedBoardingPoint: String = "",
    val selectedDroppingPoint: String = "",
    val seatLockSeconds: Int = 600,
    // Checkout State
    val passengers: List<PassengerInput> = emptyList(),
    val contactEmail: String = "",
    val contactPhone: String = "",
    val appliedCoupon: CouponValidationResponse? = null,
    val couponMessage: String? = null,
    val isValidatingCoupon: Boolean = false,
    val hasFreeCancellation: Boolean = false,
    val hasTripGuarantee: Boolean = false,
    val useWalletBalance: Boolean = false,
    val isCreatingBooking: Boolean = false,
    // Live Account Saved Travellers & Bookings
    val savedTravellers: List<SavedTraveller> = emptyList(),
    val myBookings: List<BookingDetails> = emptyList(),
    val pnrSearchResult: BookingDetails? = null,
    val isBookingsLoading: Boolean = false,
    val bookingActionMessage: String? = null,
    // AI Copilot Chat
    val aiSessionId: String? = null,
    val chatMessages: List<ChatMessageItem> = listOf(
        ChatMessageItem(
            id = "welcome-live",
            role = "assistant",
            content = "Welcome to redBus AI! Ask me about live bus schedules, seat availability, active coupons, or your bookings on redbusai.app.",
            suggestedPrompts = listOf(
                "Show AC Sleeper buses from Bangalore to Chennai today",
                "What coupons are active right now?",
                "How does redBus Free Cancellation work?"
            ),
            timestamp = "Live"
        )
    ),
    val isAiTyping: Boolean = false,
    // Synchronized Web Portal URL when opening https://www.redbusai.app
    val activeWebUrl: String = "https://www.redbusai.app/"
)

class RedbusViewModel(
    private val repository: RedbusRepository = RedbusRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RedbusUiState())
    val uiState: StateFlow<RedbusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        refreshAllLiveBackendData()
    }

    fun refreshAllLiveBackendData() {
        val currentUser = SessionManager.getUser()
        val currentToken = SessionManager.getToken()
        _uiState.update {
            it.copy(
                user = currentUser,
                token = currentToken,
                isAuthenticated = !currentToken.isNullOrBlank(),
                contactEmail = currentUser?.email ?: it.contactEmail,
                contactPhone = currentUser?.phone ?: it.contactPhone
            )
        }

        viewModelScope.launch {
            repository.getAvailableCities().onSuccess { cities ->
                if (cities.isNotEmpty()) {
                    _uiState.update { state ->
                        val src = if (state.sourceCity in cities) state.sourceCity else cities.first()
                        val dst = if (state.destinationCity in cities && state.destinationCity != src) {
                            state.destinationCity
                        } else {
                            cities.firstOrNull { it != src } ?: state.destinationCity
                        }
                        state.copy(availableCities = cities, sourceCity = src, destinationCity = dst)
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.getPopularRoutes().onSuccess { popular ->
                _uiState.update { it.copy(popularRoutes = popular) }
            }
        }

        viewModelScope.launch {
            repository.getBanners().onSuccess { banners ->
                _uiState.update { it.copy(banners = banners) }
            }
        }

        viewModelScope.launch {
            repository.getAiRecommendations(currentUser?.id).onSuccess { rec ->
                _uiState.update {
                    it.copy(
                        aiRecommendedRoutes = rec.recommendedRoutes.orEmpty(),
                        aiRecommendationReason = rec.reason
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getAvailableCoupons().onSuccess { coupons ->
                _uiState.update { it.copy(availableCoupons = coupons) }
            }
        }

        performSearch()

        if (!currentToken.isNullOrBlank()) {
            refreshAuthenticatedAccountData()
        }
    }

    fun refreshAuthenticatedAccountData() {
        if (SessionManager.getToken().isNullOrBlank()) return

        viewModelScope.launch {
            repository.getMe().onSuccess { freshUser ->
                SessionManager.updateUser(freshUser)
                _uiState.update {
                    it.copy(
                        user = freshUser,
                        isAuthenticated = true,
                        contactEmail = freshUser.email,
                        contactPhone = freshUser.phone.orEmpty()
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getSavedTravellers().onSuccess { travellers ->
                _uiState.update { it.copy(savedTravellers = travellers) }
            }
        }

        fetchMyBookings()
    }

    // --- Authentication (Exact same endpoints as https://www.redbusai.app) ---

    fun clearAuthMessages() {
        _uiState.update { it.copy(authError = null, authSuccessMessage = null) }
    }

    fun loginWithEmail(email: String, password: String, onSuccess: () -> Unit = {}) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter your email and password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authSuccessMessage = null) }
            repository.login(email, password)
                .onSuccess { res ->
                    SessionManager.saveSession(res.token, res.user)
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            user = res.user,
                            token = res.token,
                            isAuthenticated = true,
                            contactEmail = res.user.email,
                            contactPhone = res.user.phone.orEmpty(),
                            authSuccessMessage = "Logged in as ${res.user.name}"
                        )
                    }
                    refreshAuthenticatedAccountData()
                    onSuccess()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = extractErrorMessage(err, "Invalid email or password.")
                        )
                    }
                }
        }
    }

    fun registerAccount(name: String, email: String, phone: String, password: String, onSuccess: () -> Unit = {}) {
        if (name.isBlank() || email.isBlank() || password.length < 6) {
            _uiState.update { it.copy(authError = "Please fill all fields (password min 6 chars).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authSuccessMessage = null) }
            repository.register(name, email, phone, password)
                .onSuccess { res ->
                    SessionManager.saveSession(res.token, res.user)
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            user = res.user,
                            token = res.token,
                            isAuthenticated = true,
                            contactEmail = res.user.email,
                            contactPhone = res.user.phone.orEmpty(),
                            authSuccessMessage = res.message ?: "Account created & signed in!"
                        )
                    }
                    refreshAuthenticatedAccountData()
                    onSuccess()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = extractErrorMessage(err, "Registration failed. Email or phone may already be registered.")
                        )
                    }
                }
        }
    }

    fun requestMobileOtp(phone: String) {
        if (phone.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter your registered mobile number.") }
            return
        }
        val formattedPhone = if (phone.trim().startsWith("+")) phone.trim() else "+91${phone.trim().removePrefix("0")}"
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authSuccessMessage = null) }
            repository.sendMobileOtp(formattedPhone)
                .onSuccess { res ->
                    val hint = res.previewOtp?.let { " (OTP: $it)" } ?: ""
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            otpSent = true,
                            authSuccessMessage = (res.message ?: "OTP sent to $formattedPhone") + hint
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = extractErrorMessage(err, "Mobile number not registered on redbusai.app.")
                        )
                    }
                }
        }
    }

    fun verifyMobileOtpAndLogin(phone: String, otp: String, onSuccess: () -> Unit = {}) {
        val formattedPhone = if (phone.trim().startsWith("+")) phone.trim() else "+91${phone.trim().removePrefix("0")}"
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            repository.loginWithMobileOtp(formattedPhone, otp)
                .onSuccess { res ->
                    SessionManager.saveSession(res.token, res.user)
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            otpSent = false,
                            user = res.user,
                            token = res.token,
                            isAuthenticated = true,
                            contactEmail = res.user.email,
                            contactPhone = res.user.phone.orEmpty(),
                            authSuccessMessage = "Logged in as ${res.user.name}"
                        )
                    }
                    refreshAuthenticatedAccountData()
                    onSuccess()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = extractErrorMessage(err, "Invalid or expired OTP.")
                        )
                    }
                }
        }
    }

    /**
     * Bidirectional Web <-> Native Android Session Sync:
     * When the user logs in or updates their wallet/profile inside https://www.redbusai.app WebView,
     * this immediately updates the native Android app's state and SharedPreferences.
     */
    fun syncSessionFromWeb(token: String?, userJson: String?) {
        if (token.isNullOrBlank()) {
            if (_uiState.value.isAuthenticated) {
                logout()
            }
            return
        }
        val currentToken = _uiState.value.token
        if (token != currentToken || !userJson.isNullOrBlank()) {
            if (!userJson.isNullOrBlank()) {
                SessionManager.saveRawWebSession(token, userJson)
            }
            val parsedUser = SessionManager.getUser()
            _uiState.update {
                it.copy(
                    token = token,
                    user = parsedUser ?: it.user,
                    isAuthenticated = true,
                    contactEmail = parsedUser?.email ?: it.contactEmail,
                    contactPhone = parsedUser?.phone ?: it.contactPhone
                )
            }
            refreshAuthenticatedAccountData()
        }
    }

    fun logout() {
        SessionManager.clearSession()
        _uiState.update {
            it.copy(
                user = null,
                token = null,
                isAuthenticated = false,
                savedTravellers = emptyList(),
                myBookings = emptyList(),
                pnrSearchResult = null,
                authSuccessMessage = null,
                authError = null
            )
        }
    }

    fun updateProfileDetails(name: String, phone: String, gender: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authSuccessMessage = null) }
            repository.updateProfile(name, phone, gender)
                .onSuccess { updatedUser ->
                    SessionManager.updateUser(updatedUser)
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            user = updatedUser,
                            authSuccessMessage = "Profile synced with redbusai.app!"
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = extractErrorMessage(err, "Failed to update profile.")
                        )
                    }
                }
        }
    }

    fun addTraveller(name: String, age: Int, gender: String) {
        viewModelScope.launch {
            repository.addSavedTraveller(name, age, gender).onSuccess {
                repository.getSavedTravellers().onSuccess { list ->
                    _uiState.update { it.copy(savedTravellers = list, authSuccessMessage = "Traveller saved!") }
                }
            }
        }
    }

    fun deleteTraveller(id: Int) {
        viewModelScope.launch {
            repository.deleteSavedTraveller(id).onSuccess {
                repository.getSavedTravellers().onSuccess { list ->
                    _uiState.update { it.copy(savedTravellers = list) }
                }
            }
        }
    }

    // --- Theme & Navigation Helpers ---

    fun toggleDarkTheme() {
        val next = !_uiState.value.isDarkTheme
        SessionManager.setDarkMode(next)
        _uiState.update { it.copy(isDarkTheme = next) }
    }

    fun setWebPortalUrl(url: String) {
        _uiState.update { it.copy(activeWebUrl = url) }
    }

    // --- Search & Route Filtering ---

    fun updateCities(source: String, destination: String) {
        _uiState.update { it.copy(sourceCity = source, destinationCity = destination) }
    }

    fun swapCities() {
        _uiState.update {
            it.copy(sourceCity = it.destinationCity, destinationCity = it.sourceCity)
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
            RouteFilter.PRIMO -> state.routes.filter { it.rating >= 4.4 }.sortedByDescending { it.rating }
            RouteFilter.SLEEPER -> state.routes.filter { it.busType.contains("Sleeper", ignoreCase = true) }
            RouteFilter.SEATER -> state.routes.filter { it.busType.contains("Seater", ignoreCase = true) }
            RouteFilter.AC -> state.routes.filter {
                it.busType.contains("AC", ignoreCase = true) && !it.busType.contains("Non-AC", ignoreCase = true)
            }
            RouteFilter.CHEAPEST -> state.routes.sortedBy { it.basePrice }
        }
    }

    fun performSearch(onComplete: (() -> Unit)? = null) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorBanner = null, aiStatusBanner = null) }
            repository.searchRoutes(
                source = state.sourceCity,
                destination = state.destinationCity,
                date = state.travelDate
            ).onSuccess { results ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        routes = results,
                        activeFilter = RouteFilter.ALL
                    )
                }
                onComplete?.invoke()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorBanner = extractErrorMessage(err, "Unable to reach redbusai.app server.")
                    )
                }
            }
        }
    }

    fun executeAiNlpSearch(onSuccessNavigate: () -> Unit) {
        val query = _uiState.value.nlpQuery.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorBanner = null) }
            repository.parseNaturalLanguageQuery(query)
                .onSuccess { parsed ->
                    val src = parsed.sourceCity ?: _uiState.value.sourceCity
                    val dst = parsed.destinationCity ?: _uiState.value.destinationCity
                    val dt = parsed.travelDate ?: _uiState.value.travelDate

                    repository.searchRoutes(src, dst, dt, busType = parsed.busType)
                        .onSuccess { results ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    sourceCity = src,
                                    destinationCity = dst,
                                    travelDate = dt,
                                    routes = results,
                                    aiStatusBanner = "AI Parsed: $src ➔ $dst on $dt ${parsed.busType?.let { t -> "• $t" } ?: ""}"
                                )
                            }
                            onSuccessNavigate()
                        }
                        .onFailure { err ->
                            _uiState.update {
                                it.copy(isLoading = false, errorBanner = extractErrorMessage(err, "Failed to search routes."))
                            }
                        }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isLoading = false, errorBanner = extractErrorMessage(err, "AI NLP service unavailable."))
                    }
                }
        }
    }

    // --- Live Seat Layout & Real-Time Seat Locking ---

    fun selectRouteForSeats(route: RouteItem, onReady: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedRoute = route, errorBanner = null) }
            repository.getRouteSeats(route.id)
                .onSuccess { layout ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            seatLayout = layout,
                            selectedDeck = "LOWER",
                            selectedSeats = emptyList(),
                            lockedSeatIds = emptyList(),
                            selectedBoardingPoint = route.boardingPoints?.firstOrNull().orEmpty(),
                            selectedDroppingPoint = route.droppingPoints?.firstOrNull().orEmpty(),
                            appliedCoupon = null,
                            couponMessage = null
                        )
                    }
                    onReady()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorBanner = extractErrorMessage(err, "Could not load seat layout from server.")
                        )
                    }
                }
        }
    }

    fun selectDeck(deck: String) {
        _uiState.update { it.copy(selectedDeck = deck) }
    }

    fun toggleSeatSelection(seat: SeatItem) {
        if (!seat.status.equals("AVAILABLE", ignoreCase = true)) return
        _uiState.update { state ->
            val current = state.selectedSeats.toMutableList()
            val idx = current.indexOfFirst { it.seatId == seat.seatId && it.seatNumber == seat.seatNumber }
            if (idx >= 0) {
                current.removeAt(idx)
            } else if (current.size < 6) {
                current.add(seat)
            }
            val syncedPassengers = current.mapIndexed { pIdx, s ->
                state.passengers.getOrNull(pIdx)?.copy(
                    seatId = if (s.seatId != 0) s.seatId else s.id,
                    seatNumber = s.seatNumber
                ) ?: PassengerInput(
                    seatId = if (s.seatId != 0) s.seatId else s.id,
                    seatNumber = s.seatNumber,
                    name = if (pIdx == 0) state.user?.name.orEmpty() else state.savedTravellers.getOrNull(pIdx - 1)?.name.orEmpty(),
                    age = state.savedTravellers.getOrNull(pIdx)?.age ?: 25,
                    gender = if (s.genderRestriction.equals("FEMALE", ignoreCase = true)) "FEMALE"
                    else state.user?.gender ?: "MALE"
                )
            }
            state.copy(selectedSeats = current, passengers = syncedPassengers)
        }
    }

    fun lockSelectedSeatsAndProceed(onProceed: () -> Unit) {
        val state = _uiState.value
        val route = state.selectedRoute ?: return
        val seatIds = state.selectedSeats.map { if (it.seatId != 0) it.seatId else it.id }
        if (seatIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.lockSeats(route.id, seatIds, state.user?.id)
                .onSuccess { lockRes ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lockedSeatIds = lockRes.lockedSeatIds ?: seatIds,
                            seatLockSeconds = lockRes.remainingSeconds ?: 600
                        )
                    }
                    startSeatLockTimer(lockRes.remainingSeconds ?: 600)
                    onProceed()
                }
                .onFailure {
                    // Proceed even if seat lock endpoint returns already-locked by current user
                    _uiState.update { it.copy(isLoading = false) }
                    startSeatLockTimer(600)
                    onProceed()
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
        if (clean.isEmpty()) return
        val baseFare = _uiState.value.selectedSeats.sumOf { it.price }

        viewModelScope.launch {
            _uiState.update { it.copy(isValidatingCoupon = true, couponMessage = null) }
            repository.validateCoupon(clean, baseFare)
                .onSuccess { res ->
                    if (res.valid) {
                        _uiState.update {
                            it.copy(
                                isValidatingCoupon = false,
                                appliedCoupon = res,
                                couponMessage = res.message ?: "Coupon ${res.code} applied! Saved ₹${res.discountAmount?.toInt() ?: 0}"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isValidatingCoupon = false,
                                appliedCoupon = null,
                                couponMessage = res.message ?: "Coupon is not valid for this booking."
                            )
                        }
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isValidatingCoupon = false,
                            appliedCoupon = null,
                            couponMessage = extractErrorMessage(err, "Invalid or expired coupon code.")
                        )
                    }
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
        val discount = state.appliedCoupon?.discountAmount ?: 0.0
        val gross = (baseFare + freeCancelFee + tripGuaranteeFee + gstAndService - discount).coerceAtLeast(0.0)
        val userWallet = state.user?.walletBalance ?: 0.0
        val walletUsed = if (state.useWalletBalance) userWallet.coerceAtMost(gross) else 0.0
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

    fun createLiveBooking(onBookingCreated: (BookingDetails) -> Unit, onRequireRazorpayWeb: (String) -> Unit) {
        val state = _uiState.value
        val route = state.selectedRoute ?: return
        if (!state.isAuthenticated) {
            _uiState.update { it.copy(errorBanner = "Please sign in to your redBus AI account to complete booking.") }
            return
        }

        val request = CreateBookingRequest(
            routeId = route.id,
            boardingPoint = state.selectedBoardingPoint.ifBlank { route.sourceCity },
            droppingPoint = state.selectedDroppingPoint.ifBlank { route.destinationCity },
            contactEmail = state.contactEmail.ifBlank { state.user?.email.orEmpty() },
            contactPhone = state.contactPhone.ifBlank { state.user?.phone.orEmpty() },
            passengers = state.passengers.mapIndexed { idx, p ->
                p.copy(name = p.name.ifBlank { state.user?.name ?: "Passenger ${idx + 1}" })
            },
            couponCode = state.appliedCoupon?.code,
            hasFreeCancellation = state.hasFreeCancellation,
            hasTripGuarantee = state.hasTripGuarantee,
            useWallet = state.useWalletBalance
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingBooking = true, errorBanner = null) }
            repository.createBooking(request)
                .onSuccess { booking ->
                    _uiState.update { it.copy(isCreatingBooking = false) }
                    refreshAuthenticatedAccountData()
                    if (booking.status == "PENDING_PAYMENT" && booking.totalAmount > 0) {
                        // Open synced https://www.redbusai.app/checkout or /my-bookings to complete Razorpay order
                        onRequireRazorpayWeb("https://www.redbusai.app/checkout?routeId=${route.id}")
                    } else {
                        onBookingCreated(booking)
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isCreatingBooking = false,
                            errorBanner = extractErrorMessage(err, "Could not create booking. Please check passenger details.")
                        )
                    }
                }
        }
    }

    // --- Live Bookings & PNR Lookup ---

    fun fetchMyBookings() {
        if (SessionManager.getToken().isNullOrBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBookingsLoading = true) }
            repository.getMyBookings()
                .onSuccess { list ->
                    _uiState.update { it.copy(isBookingsLoading = false, myBookings = list) }
                }
                .onFailure {
                    _uiState.update { it.copy(isBookingsLoading = false) }
                }
        }
    }

    fun searchBookingByPnr(pnr: String) {
        if (pnr.isBlank()) {
            _uiState.update { it.copy(pnrSearchResult = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBookingsLoading = true, bookingActionMessage = null) }
            repository.getBookingByPnr(pnr)
                .onSuccess { booking ->
                    _uiState.update { it.copy(isBookingsLoading = false, pnrSearchResult = booking) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isBookingsLoading = false,
                            pnrSearchResult = null,
                            bookingActionMessage = extractErrorMessage(err, "No booking found with PNR: ${pnr.uppercase()}")
                        )
                    }
                }
        }
    }

    fun cancelLiveBooking(pnr: String, reason: String = "Change of plans", refundDest: String = "WALLET") {
        viewModelScope.launch {
            _uiState.update { it.copy(isBookingsLoading = true, bookingActionMessage = null) }
            repository.cancelBooking(pnr, reason, refundDest)
                .onSuccess { res ->
                    _uiState.update {
                        it.copy(
                            isBookingsLoading = false,
                            bookingActionMessage = res.message.ifBlank { "Booking $pnr cancelled. Refund: ₹${res.refundAmount.toInt()}" }
                        )
                    }
                    refreshAuthenticatedAccountData()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isBookingsLoading = false,
                            bookingActionMessage = extractErrorMessage(err, "Failed to cancel booking.")
                        )
                    }
                }
        }
    }

    fun emailTicket(pnr: String) {
        viewModelScope.launch {
            repository.sendTicketEmail(pnr, _uiState.value.user?.email)
                .onSuccess {
                    _uiState.update { it.copy(bookingActionMessage = "E-Ticket & invoice for $pnr emailed successfully!") }
                }
                .onFailure {
                    _uiState.update { it.copy(bookingActionMessage = "Could not send e-ticket email right now.") }
                }
        }
    }

    // --- Live AI Copilot ---

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
            val state = _uiState.value
            repository.askAiCopilot(
                sessionId = state.aiSessionId,
                message = clean,
                userId = state.user?.id,
                source = state.sourceCity,
                destination = state.destinationCity,
                date = state.travelDate
            ).onSuccess { response ->
                val aiMsg = ChatMessageItem(
                    id = "ai-${System.currentTimeMillis()}",
                    role = "assistant",
                    content = response.reply,
                    suggestedPrompts = response.suggestedPrompts.orEmpty(),
                    timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                )
                _uiState.update {
                    it.copy(
                        aiSessionId = response.sessionId ?: it.aiSessionId,
                        chatMessages = it.chatMessages + aiMsg,
                        isAiTyping = false
                    )
                }
            }.onFailure { err ->
                val errMsg = ChatMessageItem(
                    id = "ai-err-${System.currentTimeMillis()}",
                    role = "assistant",
                    content = extractErrorMessage(err, "Unable to reach redbusai.app AI service right now. Please check your connection."),
                    timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                )
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + errMsg,
                        isAiTyping = false
                    )
                }
            }
        }
    }

    private fun startSeatLockTimer(seconds: Int) {
        timerJob?.cancel()
        _uiState.update { it.copy(seatLockSeconds = seconds) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.seatLockSeconds > 0) {
                delay(1000L)
                _uiState.update { it.copy(seatLockSeconds = (it.seatLockSeconds - 1).coerceAtLeast(0)) }
            }
        }
    }

    private fun extractErrorMessage(err: Throwable, fallback: String): String {
        if (err is HttpException) {
            val body = try {
                err.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
            if (!body.isNullOrBlank()) {
                val msgMatch = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body)
                if (msgMatch != null) return msgMatch.groupValues[1]
                val errMatch = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(body)
                if (errMatch != null) return errMatch.groupValues[1]
            }
        }
        return err.localizedMessage?.takeIf { it.isNotBlank() } ?: fallback
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
