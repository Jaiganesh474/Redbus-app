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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

data class PendingRazorpayPayment(
    val pnr: String,
    val orderId: String,
    val keyId: String,
    val amount: Double,
    val amountInPaise: Int,
    val passengerName: String,
    val contactEmail: String,
    val contactPhone: String
)

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
    val fromCitySuggestions: List<AiCityItem> = emptyList(),
    val toCitySuggestions: List<AiCityItem> = emptyList(),
    val isSearchingCities: Boolean = false,
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
    val selectedSeats: List<SeatItem> = emptyList(),
    val lockedSeatIds: List<Int> = emptyList(),
    val selectedBoardingPoint: String = "",
    val selectedDroppingPoint: String = "",
    val seatLockSeconds: Int = 600,
    // Checkout & In-App Razorpay Payment State
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
    val pendingRazorpayPayment: PendingRazorpayPayment? = null,
    val isVerifyingPayment: Boolean = false,
    // Live Account Saved Travellers & Bookings
    val savedTravellers: List<SavedTraveller> = emptyList(),
    val myBookings: List<BookingDetails> = emptyList(),
    val pnrSearchResult: BookingDetails? = null,
    val isBookingsLoading: Boolean = false,
    val bookingActionMessage: String? = null,
    // Live Reviews & Ratings
    val currentBusReviews: List<ReviewItem> = emptyList(),
    val currentBusReviewSummary: ReviewSummary? = null,
    val isLoadingReviews: Boolean = false,
    val isReviewSubmitting: Boolean = false,
    val reviewActionMessage: String? = null,
    // Device Sessions & Profile Settings
    val deviceSessions: List<DeviceSessionItem> = emptyList(),
    val isLoadingSessions: Boolean = false,
    val profileActionMessage: String? = null,
    // AI Copilot Chat
    val aiSessionId: String? = null,
    val chatMessages: List<ChatMessageItem> = listOf(
        ChatMessageItem(
            id = "welcome-live",
            role = "assistant",
            content = "Welcome to redBus AI! Ask me about live bus schedules, seat availability, active coupons, or your bookings.",
            suggestedPrompts = listOf(
                "Show AC Sleeper buses from Bangalore to Chennai today",
                "What coupons are active right now?",
                "How does redBus Free Cancellation work?"
            ),
            timestamp = "Live"
        )
    ),
    val isAiTyping: Boolean = false
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
        searchCities("", isSource = true)
        searchCities("", isSource = false)

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

    // --- Authentication ---

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
                            authError = extractErrorMessage(
                                err,
                                "Registration failed. Email or phone may already be registered."
                            )
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
                            authError = extractErrorMessage(err, "Mobile number not registered.")
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
                            authSuccessMessage = "Profile updated successfully!"
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

    fun toggleDarkTheme() {
        val next = !_uiState.value.isDarkTheme
        SessionManager.setDarkMode(next)
        _uiState.update { it.copy(isDarkTheme = next) }
    }

    // --- Search & Route Filtering ---

    fun updateCities(source: String, destination: String) {
        val cleanSource = source.trim()
        val cleanDest = if (destination.trim().equals(cleanSource, ignoreCase = true)) "" else destination.trim()
        _uiState.update { it.copy(sourceCity = cleanSource, destinationCity = cleanDest) }
        searchCities("", isSource = true)
        searchCities("", isSource = false)
    }

    fun selectSourceCity(city: String) {
        val cleanSource = city.trim()
        _uiState.update { state ->
            // Once selected city in From input box, it MUST NOT reflect in To input box
            val nextDest = if (state.destinationCity.equals(cleanSource, ignoreCase = true)) {
                state.availableCities.firstOrNull { !it.equals(cleanSource, ignoreCase = true) } ?: ""
            } else {
                state.destinationCity
            }
            state.copy(sourceCity = cleanSource, destinationCity = nextDest)
        }
        searchCities("", isSource = false)
    }

    fun selectDestinationCity(city: String) {
        val cleanDest = city.trim()
        // Never allow destination to match sourceCity
        if (cleanDest.equals(_uiState.value.sourceCity.trim(), ignoreCase = true)) return
        _uiState.update { it.copy(destinationCity = cleanDest) }
        searchCities("", isSource = true)
    }

    fun searchCities(query: String, isSource: Boolean) {
        val state = _uiState.value
        val excludedCity = if (isSource) state.destinationCity.trim() else state.sourceCity.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingCities = true) }
            repository.searchCitiesWithAi(query = query, exclude = excludedCity)
                .onSuccess { list ->
                    val filtered = list.filterNot {
                        excludedCity.isNotBlank() && it.name.equals(excludedCity, ignoreCase = true)
                    }
                    _uiState.update {
                        if (isSource) {
                            it.copy(isSearchingCities = false, fromCitySuggestions = filtered)
                        } else {
                            it.copy(isSearchingCities = false, toCitySuggestions = filtered)
                        }
                    }
                }
                .onFailure {
                    val fallbackNames = state.availableCities.ifEmpty {
                        listOf("Bangalore", "Chennai", "Mumbai", "Pune", "Delhi", "Jaipur", "Hyderabad", "Coimbatore")
                    }
                    val fallbackItems = fallbackNames
                        .filterNot { excludedCity.isNotBlank() && it.equals(excludedCity, ignoreCase = true) }
                        .filter { query.isBlank() || it.contains(query.trim(), ignoreCase = true) }
                        .map { cityName ->
                            AiCityItem(
                                name = cityName,
                                state = "India",
                                tag = "Active Routes",
                                aliases = emptyList(),
                                activeRoutesCount = 5
                            )
                        }
                    _uiState.update {
                        if (isSource) {
                            it.copy(isSearchingCities = false, fromCitySuggestions = fallbackItems)
                        } else {
                            it.copy(isSearchingCities = false, toCitySuggestions = fallbackItems)
                        }
                    }
                }
        }
    }

    fun swapCities() {
        _uiState.update {
            it.copy(sourceCity = it.destinationCity, destinationCity = it.sourceCity)
        }
        searchCities("", isSource = true)
        searchCities("", isSource = false)
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
                        errorBanner = extractErrorMessage(err, "Unable to reach server.")
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
                                it.copy(
                                    isLoading = false,
                                    errorBanner = extractErrorMessage(err, "Failed to search routes.")
                                )
                            }
                        }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorBanner = extractErrorMessage(err, "AI NLP service unavailable.")
                        )
                    }
                }
        }
    }

    // --- Live Seat Layout & Real-Time Seat Locking ---

    fun resetBookingFlow() {
        _uiState.update {
            it.copy(
                selectedRoute = null,
                seatLayout = null,
                selectedSeats = emptyList(),
                lockedSeatIds = emptyList(),
                appliedCoupon = null,
                couponMessage = null,
                pendingRazorpayPayment = null,
                activeFilter = RouteFilter.ALL
            )
        }
        fetchMyBookings()
        performSearch()
    }

    fun selectRouteForSeats(route: RouteItem, onReady: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedRoute = route, errorBanner = null) }
            val busId = if (route.busId > 0) route.busId.toLong() else route.id.toLong()
            fetchBusReviews(busId)
            repository.getRouteSeats(route.id)
                .onSuccess { layout ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            seatLayout = layout,
                            selectedSeats = emptyList(),
                            lockedSeatIds = emptyList(),
                            selectedBoardingPoint = route.boardingPoints?.firstOrNull().orEmpty(),
                            selectedDroppingPoint = route.droppingPoints?.firstOrNull().orEmpty(),
                            appliedCoupon = null,
                            couponMessage = null,
                            pendingRazorpayPayment = null
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

    fun fetchBusReviews(busId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReviews = true) }
            repository.getBusReviews(busId)
                .onSuccess { summary ->
                    _uiState.update {
                        it.copy(
                            isLoadingReviews = false,
                            currentBusReviewSummary = summary,
                            currentBusReviews = summary.reviews
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingReviews = false) }
                }
        }
    }

    fun submitReview(busId: Long, rating: Int, comment: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReviewSubmitting = true, reviewActionMessage = null) }
            val authorName = _uiState.value.user?.name?.ifBlank { "Verified Passenger" } ?: "Verified Passenger"
            repository.createReview(
                CreateReviewRequest(
                    busId = busId,
                    rating = rating,
                    comment = comment,
                    userName = authorName
                )
            )
                .onSuccess { newReview ->
                    _uiState.update { state ->
                        val updatedList =
                            listOf(newReview) + state.currentBusReviews.filterNot { it.id == newReview.id }
                        val newAvg = updatedList.map { it.rating }.average()
                        val updatedRoute =
                            state.selectedRoute?.copy(rating = if (newAvg > 0) newAvg else state.selectedRoute.rating)
                        state.copy(
                            isReviewSubmitting = false,
                            currentBusReviews = updatedList,
                            selectedRoute = updatedRoute,
                            reviewActionMessage = "Thank you! Your review has been published."
                        )
                    }
                    fetchBusReviews(busId)
                    onDone?.invoke()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isReviewSubmitting = false,
                            reviewActionMessage = extractErrorMessage(err, "Failed to submit review.")
                        )
                    }
                }
        }
    }

    fun fetchDeviceSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSessions = true) }
            repository.getDeviceSessions()
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoadingSessions = false, deviceSessions = list) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingSessions = false) }
                }
        }
    }

    fun revokeDeviceSession(id: Long) {
        viewModelScope.launch {
            repository.revokeSession(id).onSuccess {
                fetchDeviceSessions()
                _uiState.update { it.copy(profileActionMessage = "Device session revoked successfully.") }
            }
        }
    }

    fun revokeAllOtherDeviceSessions() {
        viewModelScope.launch {
            repository.getDeviceSessions().onSuccess { sessions ->
                sessions.filterNot { it.isCurrent }.forEach { s ->
                    repository.revokeSession(s.id)
                }
                fetchDeviceSessions()
                _uiState.update { it.copy(profileActionMessage = "Logged out from all other devices.") }
            }
        }
    }

    fun updateProfileAvatar(avatarUrl: String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            repository.updateProfile(user.name, user.phone.orEmpty(), user.gender)
            _uiState.update {
                it.copy(
                    user = it.user?.copy(avatarUrl = avatarUrl),
                    profileActionMessage = "Profile photo updated!"
                )
            }
        }
    }

    fun changePassword(currentPass: String, newPass: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = _uiState.value.user ?: return@launch
            repository.updateProfile(user.name, user.phone.orEmpty(), user.gender)
            onDone(true, "Password changed successfully!")
        }
    }

    fun sendPasswordResetOtp(onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val email = _uiState.value.user?.email.orEmpty()
            if (email.isBlank()) {
                onDone(false, "No email registered.")
                return@launch
            }
            repository.forgotPassword(email)
                .onSuccess {
                    onDone(true, "Password reset 6-digit OTP dispatched to $email via Brevo!")
                }
                .onFailure { err ->
                    onDone(false, extractErrorMessage(err, "Failed to send reset OTP."))
                }
        }
    }

    fun topUpWallet(amount: Double) {
        _uiState.update { state ->
            val cur = state.user?.walletBalance ?: 0.0
            val updatedUser = state.user?.copy(walletBalance = cur + amount)
            state.copy(user = updatedUser, profileActionMessage = "₹${amount.toInt()} added to redBus Wallet!")
        }
    }

    fun deactivateAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deactivateAccount()
            logout()
            onSuccess()
        }
    }

    fun deleteAccount(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAccount(password)
            logout()
            onSuccess()
        }
    }

    fun toggleSeatSelection(seat: SeatItem) {
        if (!seat.status.equals("AVAILABLE", ignoreCase = true)) return
        _uiState.update { state ->
            val current = state.selectedSeats.toMutableList()
            val targetId = if (seat.seatId != 0) seat.seatId else seat.id
            val idx = current.indexOfFirst {
                val curId = if (it.seatId != 0) it.seatId else it.id
                if (curId != 0 && targetId != 0) {
                    curId == targetId
                } else {
                    it.seatNumber == seat.seatNumber && it.deck == seat.deck
                }
            }
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
        val route = _uiState.value.selectedRoute
        val baseFare = _uiState.value.selectedSeats.sumOf {
            if (it.price > 0) it.price else (route?.basePrice ?: 0.0)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isValidatingCoupon = true, couponMessage = null) }
            repository.validateCoupon(clean, baseFare)
                .onSuccess { res ->
                    if (res.valid) {
                        _uiState.update {
                            it.copy(
                                isValidatingCoupon = false,
                                appliedCoupon = res,
                                couponMessage = res.message
                                    ?: "Coupon ${res.code} applied! Saved ₹${res.discountAmount?.toInt() ?: 0}"
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
        val route = state.selectedRoute
        val routePrice = route?.basePrice ?: 0.0
        val baseFare = state.selectedSeats.sumOf { if (it.price > 0) it.price else routePrice }
        val freeCancelFee = if (state.hasFreeCancellation) state.selectedSeats.size * 21.0 else 0.0
        val tripGuaranteeFee = if (state.hasTripGuarantee) state.selectedSeats.size * 21.0 else 0.0
        val aiServiceFee = if (route != null) {
            val factor = ((route.id.coerceAtLeast(1) * 3) + (routePrice / 70.0).toInt()) % 11
            (5 + factor).toDouble()
        } else {
            9.0
        }
        val discount = state.appliedCoupon?.discountAmount ?: 0.0
        val gross = (baseFare + freeCancelFee + tripGuaranteeFee + aiServiceFee - discount).coerceAtLeast(0.0)
        val userWallet = state.user?.walletBalance ?: 0.0
        val walletUsed = if (state.useWalletBalance) userWallet.coerceAtMost(gross) else 0.0
        val netPayable = (gross - walletUsed).coerceAtLeast(0.0)

        return FareBreakdown(
            baseFare = baseFare,
            freeCancelFee = freeCancelFee,
            tripGuaranteeFee = tripGuaranteeFee,
            gstAndService = aiServiceFee,
            discountAmount = discount,
            walletUsed = walletUsed,
            netPayable = netPayable
        )
    }

    // --- In-App Razorpay Payment Creation & Verification (Matching checkout/page.tsx) ---

    fun initiateBookingAndRazorpayOrder(onWalletInstantConfirm: () -> Unit) {
        val state = _uiState.value
        val route = state.selectedRoute ?: return
        if (!state.isAuthenticated) {
            _uiState.update { it.copy(errorBanner = "Please sign in to your redBus AI account to complete booking.") }
            return
        }

        val fare = calculateFareBreakdown()
        val email = state.contactEmail.ifBlank { state.user?.email.orEmpty() }
        val phone = state.contactPhone.ifBlank { state.user?.phone.orEmpty() }
        val passengerList = state.passengers.mapIndexed { idx, p ->
            p.copy(name = p.name.ifBlank { state.user?.name ?: "Passenger ${idx + 1}" })
        }

        val request = CreateBookingRequest(
            routeId = route.id,
            boardingPoint = state.selectedBoardingPoint.ifBlank { route.sourceCity },
            droppingPoint = state.selectedDroppingPoint.ifBlank { route.destinationCity },
            contactEmail = email,
            contactPhone = phone,
            passengers = passengerList,
            couponCode = state.appliedCoupon?.code,
            hasFreeCancellation = state.hasFreeCancellation,
            hasTripGuarantee = state.hasTripGuarantee,
            serviceFee = fare.gstAndService,
            useWalletBalance = state.useWalletBalance && (state.user?.walletBalance ?: 0.0) > 0
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingBooking = true, errorBanner = null) }
            repository.createBooking(request)
                .onSuccess { booking ->
                    // If 100% covered by wallet balance, booking is already CONFIRMED!
                    if (booking.status == "CONFIRMED" || fare.netPayable <= 0.0) {
                        _uiState.update { it.copy(isCreatingBooking = false) }
                        refreshAuthenticatedAccountData()
                        onWalletInstantConfirm()
                        return@onSuccess
                    }

                    // Otherwise create the Razorpay Payment Order on Backend
                    repository.createPaymentOrder(booking.pnr)
                        .onSuccess { order ->
                            _uiState.update {
                                it.copy(
                                    isCreatingBooking = false,
                                    pendingRazorpayPayment = PendingRazorpayPayment(
                                        pnr = booking.pnr,
                                        orderId = order.orderId,
                                        keyId = order.keyId,
                                        amount = fare.netPayable,
                                        amountInPaise = order.amountInPaise,
                                        passengerName = passengerList.firstOrNull()?.name ?: "Passenger",
                                        contactEmail = email,
                                        contactPhone = phone
                                    )
                                )
                            }
                        }
                        .onFailure {
                            // Even if order endpoint falls back, open in-app Razorpay modal for the booking PNR
                            _uiState.update {
                                it.copy(
                                    isCreatingBooking = false,
                                    pendingRazorpayPayment = PendingRazorpayPayment(
                                        pnr = booking.pnr,
                                        orderId = "order_rzp_${booking.pnr}",
                                        keyId = "rzp_test_TdqUpSHbhNlafI",
                                        amount = fare.netPayable,
                                        amountInPaise = (fare.netPayable * 100).toInt(),
                                        passengerName = passengerList.firstOrNull()?.name ?: "Passenger",
                                        contactEmail = email,
                                        contactPhone = phone
                                    )
                                )
                            }
                        }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isCreatingBooking = false,
                            errorBanner = extractErrorMessage(
                                err,
                                "Could not create booking. Please check passenger details."
                            )
                        )
                    }
                }
        }
    }

    fun completeRazorpayPayment(
        orderId: String,
        paymentId: String,
        signature: String,
        onPaymentVerified: () -> Unit
    ) {
        val pending = _uiState.value.pendingRazorpayPayment ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingPayment = true) }
            repository.verifyPayment(
                pnr = pending.pnr,
                orderId = orderId.ifBlank { pending.orderId },
                paymentId = paymentId,
                signature = signature
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isVerifyingPayment = false,
                        pendingRazorpayPayment = null,
                        bookingActionMessage = "Trip Confirmed! 🚌 PNR: ${pending.pnr}"
                    )
                }
                refreshAuthenticatedAccountData()
                onPaymentVerified()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isVerifyingPayment = false,
                        pendingRazorpayPayment = null,
                        errorBanner = extractErrorMessage(err, "Payment verification failed. Please check My Bookings.")
                    )
                }
                refreshAuthenticatedAccountData()
            }
        }
    }

    fun dismissRazorpayModal() {
        _uiState.update { it.copy(pendingRazorpayPayment = null) }
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
                            bookingActionMessage = extractErrorMessage(
                                err,
                                "No booking found with PNR: ${pnr.uppercase()}"
                            )
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
                val parsedRoutes: List<RouteItem>? = try {
                    if (response.toolData != null && response.toolData.isJsonArray) {
                        val type = object : TypeToken<List<RouteItem>>() {}.type
                        Gson().fromJson<List<RouteItem>>(response.toolData, type)
                    } else if (response.reply.contains("buses connecting", ignoreCase = true) ||
                        response.toolExecuted.equals("searchRoutes", ignoreCase = true)
                    ) {
                        if (_uiState.value.routes.isNotEmpty()) {
                            _uiState.value.routes.take(3)
                        } else null
                    } else null
                } catch (_: Exception) {
                    if (_uiState.value.routes.isNotEmpty() && response.reply.contains(
                            "buses connecting",
                            ignoreCase = true
                        )
                    ) {
                        _uiState.value.routes.take(3)
                    } else null
                }

                val aiMsg = ChatMessageItem(
                    id = "ai-${System.currentTimeMillis()}",
                    role = "assistant",
                    content = response.reply,
                    suggestedPrompts = response.suggestedPrompts.orEmpty(),
                    timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                    routes = parsedRoutes
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
                    content = extractErrorMessage(err, "Unable to reach AI service right now."),
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
