package com.redbus.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.redbus.ai.data.model.RouteItem
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.viewmodel.RedbusUiState
import com.redbus.ai.ui.viewmodel.RouteFilter
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private enum class BusSortOption(val label: String) {
    RATINGS("Ratings"),
    DEPARTURE("Departure time"),
    PRICE("Price")
}

private enum class DepartureWindowOption(
    val id: String,
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color
) {
    MORNING("MORNING", "Morning", "6am - 12pm", Icons.Default.WbSunny, Color(0xFFF59E0B)),
    AFTERNOON("AFTERNOON", "Afternoon", "12pm - 6pm", Icons.Default.LightMode, Color(0xFFF97316)),
    EVENING("EVENING", "Evening", "6pm - 12am", Icons.Default.WbTwilight, Color(0xFFF43F5E)),
    NIGHT("NIGHT", "Night", "12am - 6am", Icons.Default.NightsStay, Color(0xFF6366F1))
}

private data class LiveFilterCounts(
    val total: Int,
    val primo: Int,
    val freeCancel: Int,
    val ac: Int,
    val sleeper: Int,
    val single: Int,
    val seater: Int,
    val nonAc: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    uiState: RedbusUiState,
    filteredRoutes: List<RouteItem>,
    onFilterSelected: (RouteFilter) -> Unit,
    onSelectRoute: (RouteItem) -> Unit,
    onUpdateTravelDate: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val rawRoutes = uiState.routes

    // Website-matching Filter States (matching frontend/src/app/search/page.tsx & Image 5)
    var aiFilterQuery by remember { mutableStateOf("") }
    var isPrimoFilter by remember { mutableStateOf(false) }
    var isFreeCancelFilter by remember { mutableStateOf(false) }
    var isAcFilter by remember { mutableStateOf(false) }
    var isSleeperFilter by remember { mutableStateOf(false) }
    var isSingleSeatFilter by remember { mutableStateOf(false) }
    var isSeaterFilter by remember { mutableStateOf(false) }
    var isNonAcFilter by remember { mutableStateOf(false) }
    var selectedDepartureWindow by remember { mutableStateOf<DepartureWindowOption?>(null) }
    var selectedMaxPrice by remember { mutableStateOf<Int?>(null) }
    var selectedOperators by remember { mutableStateOf(setOf<String>()) }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }
    var activeSort by remember { mutableStateOf(BusSortOption.RATINGS) }

    // Inline Filter buses panel expanded state + All Filters modal state
    var isFilterPanelExpanded by remember { mutableStateOf(true) }
    var showAllFiltersModal by remember { mutableStateOf(false) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    // Sync from ViewModel's activeFilter if set externally
    LaunchedEffect(uiState.activeFilter) {
        when (uiState.activeFilter) {
            RouteFilter.PRIMO -> isPrimoFilter = true
            RouteFilter.SLEEPER -> isSleeperFilter = true
            RouteFilter.SEATER -> isSeaterFilter = true
            RouteFilter.AC -> isAcFilter = true
            RouteFilter.CHEAPEST -> activeSort = BusSortOption.PRICE
            RouteFilter.ALL -> {}
        }
    }

    // Live Filter Counts derived from rawRoutes (matching website search/page.tsx lines 530-550)
    val filterCounts = remember(rawRoutes) {
        LiveFilterCounts(
            total = rawRoutes.size,
            primo = rawRoutes.count { it.rating >= 4.4 },
            freeCancel = rawRoutes.count { it.rating >= 4.0 || it.basePrice >= 600 },
            ac = rawRoutes.count {
                it.busType.contains("ac", ignoreCase = true) && !it.busType.contains("non", ignoreCase = true)
            },
            sleeper = rawRoutes.count { it.busType.contains("sleeper", ignoreCase = true) },
            single = rawRoutes.count {
                it.busType.contains("2+1", ignoreCase = true) || it.busType.contains("sleeper", ignoreCase = true)
            },
            seater = rawRoutes.count { it.busType.contains("seater", ignoreCase = true) },
            nonAc = rawRoutes.count { it.busType.contains("non", ignoreCase = true) }
        )
    }

    val availableOperators = remember(rawRoutes) {
        rawRoutes.map { it.operatorName }.distinct().sorted()
    }

    // Full AI + Multi-Filter + Sort Engine (matching website search/page.tsx lines 288-508)
    val displayedRoutes = remember(
        rawRoutes,
        aiFilterQuery,
        isPrimoFilter,
        isFreeCancelFilter,
        isAcFilter,
        isSleeperFilter,
        isSingleSeatFilter,
        isSeaterFilter,
        isNonAcFilter,
        selectedDepartureWindow,
        selectedMaxPrice,
        selectedOperators,
        selectedAmenities,
        activeSort
    ) {
        var result = rawRoutes.toList()

        // 1. AI Smart Filter natural language query
        val q = aiFilterQuery.trim().lowercase(Locale.ENGLISH)
        if (q.isNotEmpty()) {
            // Operator matching
            val matchedOp = rawRoutes.firstOrNull { q.contains(it.operatorName.lowercase(Locale.ENGLISH)) }
            if (matchedOp != null) {
                result = result.filter { it.operatorName.equals(matchedOp.operatorName, ignoreCase = true) }
            } else {
                val knownOps = listOf("intercity", "intrcity", "smartbus", "fresh", "zingbus", "zing", "ksrtc", "orange", "srs", "vrl", "jabbar", "kallada", "greenline", "parveen", "neeta", "chalo")
                val foundKw = knownOps.firstOrNull { q.contains(it) }
                if (foundKw != null) {
                    result = result.filter { it.operatorName.lowercase(Locale.ENGLISH).contains(foundKw) }
                }
            }

            // Price intent (e.g. "under 1000", "below 800")
            val maxPriceMatch = Regex("(?:under|below|less than|within|<|upto|up to)\\s*₹?\\s*(\\d+)").find(q)
            if (maxPriceMatch != null) {
                val maxP = maxPriceMatch.groupValues[1].toDoubleOrNull()
                if (maxP != null) {
                    result = result.filter { it.basePrice <= maxP }
                }
            }

            // Time of day intent
            when {
                q.contains("morning") -> result = result.filter { parseHour(it.departureTime) in 5..11 }
                q.contains("afternoon") || q.contains("noon") -> result = result.filter { parseHour(it.departureTime) in 12..16 }
                q.contains("evening") -> result = result.filter { parseHour(it.departureTime) in 17..20 }
                q.contains("night") || q.contains("overnight") -> result = result.filter {
                    val h = parseHour(it.departureTime)
                    h >= 20 || h < 5
                }
            }

            // Bus type & amenities intent
            if (q.contains("sleeper")) {
                result = result.filter { it.busType.contains("sleeper", ignoreCase = true) }
            } else if (q.contains("seater")) {
                result = result.filter { it.busType.contains("seater", ignoreCase = true) }
            }

            if (q.contains("non ac") || q.contains("non-ac") || q.contains("nonac")) {
                result = result.filter { it.busType.contains("non", ignoreCase = true) }
            } else if (q.contains("ac") || q.contains("a/c")) {
                result = result.filter {
                    it.busType.contains("ac", ignoreCase = true) && !it.busType.contains("non", ignoreCase = true)
                }
            }

            if (q.contains("volvo")) {
                result = result.filter {
                    it.busType.contains("volvo", ignoreCase = true) || it.operatorName.contains("volvo", ignoreCase = true)
                }
            }

            if (q.contains("primo") || q.contains("top rated")) {
                result = result.filter { it.rating >= 4.4 }
            }

            if (q.contains("single")) {
                result = result.filter {
                    it.busType.contains("2+1", ignoreCase = true) || it.busType.contains("sleeper", ignoreCase = true)
                }
            }

            if (q.contains("wifi") || q.contains("wi-fi")) {
                result = result.filter { r -> r.amenities.orEmpty().any { it.contains("wifi", ignoreCase = true) } }
            }
        }

        // 2. Quick Filter Toggles
        if (isPrimoFilter) {
            result = result.filter { it.rating >= 4.4 }
        }
        if (isFreeCancelFilter) {
            result = result.filter { it.rating >= 4.0 || it.basePrice >= 600 }
        }
        if (isAcFilter) {
            result = result.filter {
                it.busType.contains("ac", ignoreCase = true) && !it.busType.contains("non", ignoreCase = true)
            }
        }
        if (isSleeperFilter) {
            result = result.filter { it.busType.contains("sleeper", ignoreCase = true) }
        }
        if (isSingleSeatFilter) {
            result = result.filter {
                it.busType.contains("2+1", ignoreCase = true) || it.busType.contains("sleeper", ignoreCase = true)
            }
        }
        if (isSeaterFilter) {
            result = result.filter { it.busType.contains("seater", ignoreCase = true) }
        }
        if (isNonAcFilter) {
            result = result.filter { it.busType.contains("non", ignoreCase = true) }
        }

        // 3. Departure Window Filter
        selectedDepartureWindow?.let { win ->
            result = result.filter { r ->
                val h = parseHour(r.departureTime)
                when (win) {
                    DepartureWindowOption.MORNING -> h in 6..11
                    DepartureWindowOption.AFTERNOON -> h in 12..17
                    DepartureWindowOption.EVENING -> h in 18..23
                    DepartureWindowOption.NIGHT -> h in 0..5
                }
            }
        }

        // 4. Max Price Preset Filter
        selectedMaxPrice?.let { maxP ->
            result = result.filter { it.basePrice <= maxP }
        }

        // 5. Operators Filter
        if (selectedOperators.isNotEmpty()) {
            result = result.filter { r -> selectedOperators.any { op -> r.operatorName.equals(op, ignoreCase = true) } }
        }

        // 6. Amenities Filter
        if (selectedAmenities.isNotEmpty()) {
            result = result.filter { r ->
                selectedAmenities.all { reqAmenity ->
                    r.amenities.orEmpty().any { it.contains(reqAmenity, ignoreCase = true) }
                }
            }
        }

        // 7. Sort By (Ratings, Departure time, Price)
        when (activeSort) {
            BusSortOption.RATINGS -> result.sortedByDescending { it.rating }
            BusSortOption.DEPARTURE -> result.sortedBy { it.departureTime }
            BusSortOption.PRICE -> result.sortedBy { it.basePrice }
        }
    }

    val activeFilterCount = remember(
        aiFilterQuery,
        isPrimoFilter,
        isFreeCancelFilter,
        isAcFilter,
        isSleeperFilter,
        isSingleSeatFilter,
        isSeaterFilter,
        isNonAcFilter,
        selectedDepartureWindow,
        selectedMaxPrice,
        selectedOperators,
        selectedAmenities
    ) {
        var count = 0
        if (aiFilterQuery.isNotBlank()) count++
        if (isPrimoFilter) count++
        if (isFreeCancelFilter) count++
        if (isAcFilter) count++
        if (isSleeperFilter) count++
        if (isSingleSeatFilter) count++
        if (isSeaterFilter) count++
        if (isNonAcFilter) count++
        if (selectedDepartureWindow != null) count++
        if (selectedMaxPrice != null) count++
        count += selectedOperators.size
        count += selectedAmenities.size
        count
    }

    fun resetAllFilters() {
        aiFilterQuery = ""
        isPrimoFilter = false
        isFreeCancelFilter = false
        isAcFilter = false
        isSleeperFilter = false
        isSingleSeatFilter = false
        isSeaterFilter = false
        isNonAcFilter = false
        selectedDepartureWindow = null
        selectedMaxPrice = null
        selectedOperators = emptySet()
        selectedAmenities = emptySet()
        onFilterSelected(RouteFilter.ALL)
    }

    val todayStr = remember { LocalDate.now().toString() }
    val tomorrowStr = remember { LocalDate.now().plusDays(1).toString() }
    val isToday = uiState.travelDate == todayStr
    val isTomorrow = uiState.travelDate == tomorrowStr

    val formattedDateBadge = remember(uiState.travelDate) {
        try {
            val d = LocalDate.parse(uiState.travelDate)
            val m = d.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
            val w = d.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
            "${d.dayOfMonth} $m" to w
        } catch (_: Exception) {
            uiState.travelDate to ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Website-Matching Top Route & Date Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.sourceCity,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = " ➔ ",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = RedbusCrimson
                                )
                                Text(
                                    text = uiState.destinationCity,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${filterCounts.total} buses",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Today / Tomorrow Pills + Date Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = { onUpdateTravelDate(todayStr) },
                            shape = RoundedCornerShape(50),
                            color = if (isToday) RedbusCrimson else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Today",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            onClick = { onUpdateTravelDate(tomorrowStr) },
                            shape = RoundedCornerShape(50),
                            color = if (isTomorrow) RedbusCrimson else Color(0xFFFFE4E6)
                        ) {
                            Text(
                                text = "Tomorrow",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTomorrow) Color.White else RedbusCrimson,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            onClick = { showDatePickerModal = true },
                            shape = RoundedCornerShape(10.dp),
                            color = RedbusCrimson.copy(alpha = 0.1f),
                            modifier = Modifier.border(1.dp, RedbusCrimson.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = formattedDateBadge.first,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = RedbusCrimson
                                )
                                if (formattedDateBadge.second.isNotEmpty()) {
                                    Text(
                                        text = formattedDateBadge.second,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedbusCrimson)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 2. 5 Service Highlight Promo Cards Row (Matching Image 5 top row)
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Primo Certified
                        item {
                            PromoHighlightCard3D(
                                badgeText = "★ PRIMO",
                                badgeColor = Color(0xFFF59E0B),
                                title = "Primo Certified",
                                subtitle = "Top Rated 4.4★+ Fleet",
                                footer = if (isPrimoFilter) "Applied ✓" else "Filter Primo →",
                                backgroundBrush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFD97706), Color(0xFFB45309))
                                ),
                                contentColor = Color.White,
                                isSelected = isPrimoFilter,
                                onClick = { isPrimoFilter = !isPrimoFilter }
                            )
                        }

                        // Card 2: Free Cancellation
                        item {
                            PromoHighlightCard3D(
                                badgeText = "🛡️ 100% REFUND",
                                badgeColor = RedbusCrimson,
                                title = "Free Cancellation",
                                subtitle = "100% Refund guarantee",
                                footer = if (isFreeCancelFilter) "Applied ✓" else "Filter Free Cancel →",
                                backgroundBrush = Brush.linearGradient(
                                    colors = if (uiState.isDarkTheme) {
                                        listOf(Color(0xFF260A10), Color(0xFF18080C))
                                    } else {
                                        listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6))
                                    }
                                ),
                                contentColor = if (uiState.isDarkTheme) Color.White else Color(0xFF111827),
                                isSelected = isFreeCancelFilter,
                                onClick = { isFreeCancelFilter = !isFreeCancelFilter }
                            )
                        }

                        // Card 3: 15-Min Refund Assured
                        item {
                            PromoHighlightCard3D(
                                badgeText = "⚡ 15-MIN REFUND • ASSURED",
                                badgeColor = Color(0xFF047857),
                                title = "Instant Bank Payout",
                                subtitle = "Direct UPI & Card reversals",
                                footer = "Zero Hassle ✓",
                                backgroundBrush = Brush.linearGradient(
                                    colors = if (uiState.isDarkTheme) {
                                        listOf(Color(0xFF062419), Color(0xFF071913))
                                    } else {
                                        listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5))
                                    }
                                ),
                                contentColor = if (uiState.isDarkTheme) Color.White else Color(0xFF111827),
                                isSelected = false,
                                onClick = {}
                            )
                        }

                        // Card 4: Return Trip Savings
                        item {
                            PromoHighlightCard3D(
                                badgeText = "OFFER",
                                badgeColor = Color(0xFF2563EB),
                                title = "Return Trip Savings",
                                subtitle = "Flat 10% OFF on return bus",
                                footer = "Auto-Applied at Checkout",
                                backgroundBrush = Brush.linearGradient(
                                    colors = if (uiState.isDarkTheme) {
                                        listOf(Color(0xFF081B38), Color(0xFF081427))
                                    } else {
                                        listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
                                    }
                                ),
                                contentColor = if (uiState.isDarkTheme) Color.White else Color(0xFF111827),
                                isSelected = false,
                                onClick = {}
                            )
                        }

                        // Card 5: Last Min Steal Deals
                        item {
                            PromoHighlightCard3D(
                                badgeText = "🔥 LAST MIN",
                                badgeColor = AiViolet,
                                title = "Limited time steal deals",
                                subtitle = "Save up to ₹300 today",
                                footer = "Use code FIRSTBUS →",
                                backgroundBrush = Brush.linearGradient(
                                    colors = if (uiState.isDarkTheme) {
                                        listOf(Color(0xFF220C3B), Color(0xFF150926))
                                    } else {
                                        listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE))
                                    }
                                ),
                                contentColor = if (uiState.isDarkTheme) Color.White else Color(0xFF111827),
                                isSelected = false,
                                onClick = {}
                            )
                        }
                    }
                }

                // 3. Website-Matching "Filter buses" Panel (Matching Image 5 Left Column)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header: "Filter buses" | "All Filters" | "Reset"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { isFilterPanelExpanded = !isFilterPanelExpanded }
                                ) {
                                    Text(
                                        text = "Filter buses",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (activeFilterCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = RedbusCrimson
                                        ) {
                                            Text(
                                                text = activeFilterCount.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isFilterPanelExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Toggle Filters",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { showAllFiltersModal = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "All Filters",
                                            tint = RedbusCrimson,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "All Filters",
                                            color = RedbusCrimson,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (activeFilterCount > 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { resetAllFilters() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RestartAlt,
                                                contentDescription = "Reset",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Reset",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // ✨ AI SMART FILTER Input Box (Always visible, matching Image 5)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = AiViolet,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "AI SMART FILTER",
                                            color = AiViolet,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    if (aiFilterQuery.isNotEmpty()) {
                                        Text(
                                            text = "Clear",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { aiFilterQuery = "" }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = aiFilterQuery,
                                    onValueChange = { aiFilterQuery = it },
                                    placeholder = {
                                        Text(
                                            text = "Search 'AC sleeper under 1000', 'morning', 'Primo'...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = AiViolet,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (aiFilterQuery.isNotEmpty()) {
                                            IconButton(onClick = { aiFilterQuery = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear AI Filter",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = null,
                                                tint = AiViolet,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AiViolet,
                                        unfocusedBorderColor = AiViolet.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Expandable Full Website Filter List (Primo Bus, Free Cancellation, AC, SLEEPER, Single Seats, SEATER, NONAC + Departure Time)
                            AnimatedVisibility(
                                visible = isFilterPanelExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                                    // 7 Filter Rows with Live Counts (Matching Image 5 exact order & labels)
                                    WebsiteFilterRowItem(
                                        icon = Icons.Default.Star,
                                        iconTint = Color(0xFFF59E0B),
                                        label = "Primo Bus",
                                        count = filterCounts.primo,
                                        selected = isPrimoFilter,
                                        onClick = { isPrimoFilter = !isPrimoFilter }
                                    )
                                    WebsiteFilterRowItem(
                                        icon = Icons.Default.VerifiedUser,
                                        iconTint = RatingEmerald,
                                        label = "Free Cancellation",
                                        count = filterCounts.freeCancel,
                                        selected = isFreeCancelFilter,
                                        onClick = { isFreeCancelFilter = !isFreeCancelFilter }
                                    )
                                    WebsiteFilterRowItem(
                                        icon = Icons.Default.AcUnit,
                                        iconTint = Color(0xFF3B82F6),
                                        label = "AC",
                                        count = filterCounts.ac,
                                        selected = isAcFilter,
                                        onClick = { isAcFilter = !isAcFilter }
                                    )
                                    WebsiteFilterRowItem(
                                        icon = Icons.Default.Bed,
                                        iconTint = AiViolet,
                                        label = "SLEEPER",
                                        count = filterCounts.sleeper,
                                        selected = isSleeperFilter,
                                        onClick = { isSleeperFilter = !isSleeperFilter }
                                    )
                                    WebsiteFilterRowItem(
                                        icon = Icons.Default.EventSeat,
                                        iconTint = Color(0xFF14B8A6),
                                        label = "Single Seats",
                                        count = filterCounts.single,
                                        selected = isSingleSeatFilter,
                                        onClick = { isSingleSeatFilter = !isSingleSeatFilter }
                                    )
                                    WebsiteFilterRowItem(
                                        icon = Icons.Default.AirlineSeatReclineNormal,
                                        iconTint = Color(0xFFF97316),
                                        label = "SEATER",
                                        count = filterCounts.seater,
                                        selected = isSeaterFilter,
                                        onClick = { isSeaterFilter = !isSeaterFilter }
                                    )
                                    WebsiteFilterRowItem(
                                        icon = Icons.Default.DirectionsBus,
                                        iconTint = Color(0xFF6B7280),
                                        label = "NONAC",
                                        count = filterCounts.nonAc,
                                        selected = isNonAcFilter,
                                        onClick = { isNonAcFilter = !isNonAcFilter }
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                                    // DEPARTURE TIME 2x2 Grid (Matching Image 5)
                                    Text(
                                        text = "DEPARTURE TIME",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.6.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DepartureWindowTile(
                                            option = DepartureWindowOption.MORNING,
                                            selected = selectedDepartureWindow == DepartureWindowOption.MORNING,
                                            onClick = {
                                                selectedDepartureWindow = if (selectedDepartureWindow == DepartureWindowOption.MORNING) null else DepartureWindowOption.MORNING
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        DepartureWindowTile(
                                            option = DepartureWindowOption.AFTERNOON,
                                            selected = selectedDepartureWindow == DepartureWindowOption.AFTERNOON,
                                            onClick = {
                                                selectedDepartureWindow = if (selectedDepartureWindow == DepartureWindowOption.AFTERNOON) null else DepartureWindowOption.AFTERNOON
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DepartureWindowTile(
                                            option = DepartureWindowOption.EVENING,
                                            selected = selectedDepartureWindow == DepartureWindowOption.EVENING,
                                            onClick = {
                                                selectedDepartureWindow = if (selectedDepartureWindow == DepartureWindowOption.EVENING) null else DepartureWindowOption.EVENING
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        DepartureWindowTile(
                                            option = DepartureWindowOption.NIGHT,
                                            selected = selectedDepartureWindow == DepartureWindowOption.NIGHT,
                                            onClick = {
                                                selectedDepartureWindow = if (selectedDepartureWindow == DepartureWindowOption.NIGHT) null else DepartureWindowOption.NIGHT
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // PRICE PRESETS Row
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "PRICE PRESETS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.6.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(600, 900, 1200).forEach { priceCap ->
                                            val isSelected = selectedMaxPrice == priceCap
                                            Surface(
                                                onClick = {
                                                    selectedMaxPrice = if (isSelected) null else priceCap
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) RedbusCrimson else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "Under ₹$priceCap",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Website-Matching "X buses found" + "Sort by: Ratings | Departure time | Price" Bar (Image 5)
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${displayedRoutes.size} buses found",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Sort by:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                BusSortOption.entries.forEach { sortOpt ->
                                    val selected = activeSort == sortOpt
                                    Surface(
                                        onClick = { activeSort = sortOpt },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) RedbusCrimson.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = if (selected) {
                                            Modifier.border(1.dp, RedbusCrimson, RoundedCornerShape(8.dp))
                                        } else {
                                            Modifier
                                        }
                                    ) {
                                        Text(
                                            text = sortOpt.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                                            color = if (selected) RedbusCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Trust Strip: "✨ 3.6+ lakh bus routes on redBus" (Matching Image 5)
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.isDarkTheme) Color(0xFF1E293B) else Color(0xFFFFFBEB),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (uiState.isDarkTheme) Color(0xFF334155) else Color(0xFFFDE68A),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "3.6+ lakh bus routes on redBus",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E)
                            )
                        }
                    }
                }

                // 6. Filtered & Sorted Bus Route Cards with 3D Entrance Animation
                if (displayedRoutes.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    tint = RedbusCrimson,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "No buses match your active filters",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Try resetting your filters or searching another date.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { resetAllFilters() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Reset All Filters", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(displayedRoutes, key = { _, it -> it.id }) { index, route ->
                        BusRouteCard3D(
                            route = route,
                            index = index,
                            onSelect = { onSelectRoute(route) }
                        )
                    }
                }
            }
        }
    }

    // Full "All Filters" Modal (Operators, Amenities, Departure Time, Bus Types, Price)
    if (showAllFiltersModal) {
        AllFiltersSheetModal(
            filterCounts = filterCounts,
            availableOperators = availableOperators,
            selectedOperators = selectedOperators,
            onToggleOperator = { op ->
                selectedOperators = if (selectedOperators.contains(op)) selectedOperators - op else selectedOperators + op
            },
            selectedAmenities = selectedAmenities,
            onToggleAmenity = { am ->
                selectedAmenities = if (selectedAmenities.contains(am)) selectedAmenities - am else selectedAmenities + am
            },
            isPrimoFilter = isPrimoFilter,
            onTogglePrimo = { isPrimoFilter = !isPrimoFilter },
            isFreeCancelFilter = isFreeCancelFilter,
            onToggleFreeCancel = { isFreeCancelFilter = !isFreeCancelFilter },
            isAcFilter = isAcFilter,
            onToggleAc = { isAcFilter = !isAcFilter },
            isSleeperFilter = isSleeperFilter,
            onToggleSleeper = { isSleeperFilter = !isSleeperFilter },
            isSingleSeatFilter = isSingleSeatFilter,
            onToggleSingleSeat = { isSingleSeatFilter = !isSingleSeatFilter },
            isSeaterFilter = isSeaterFilter,
            onToggleSeater = { isSeaterFilter = !isSeaterFilter },
            isNonAcFilter = isNonAcFilter,
            onToggleNonAc = { isNonAcFilter = !isNonAcFilter },
            selectedDepartureWindow = selectedDepartureWindow,
            onSelectDepartureWindow = { win ->
                selectedDepartureWindow = if (selectedDepartureWindow == win) null else win
            },
            selectedMaxPrice = selectedMaxPrice,
            onSelectMaxPrice = { maxP ->
                selectedMaxPrice = if (selectedMaxPrice == maxP) null else maxP
            },
            onResetAll = { resetAllFilters() },
            onDismiss = { showAllFiltersModal = false }
        )
    }

    if (showDatePickerModal) {
        QuickDateSelectorDialog(
            selectedDateStr = uiState.travelDate,
            onSelectDate = { picked ->
                showDatePickerModal = false
                onUpdateTravelDate(picked)
            },
            onDismiss = { showDatePickerModal = false }
        )
    }
}

@Composable
private fun PromoHighlightCard3D(
    badgeText: String,
    badgeColor: Color,
    title: String,
    subtitle: String,
    footer: String,
    backgroundBrush: Brush,
    contentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "promoScale"
    )
    val rotY by animateFloatAsState(
        targetValue = if (isSelected) 6f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "promoRotY"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = Modifier
            .width(172.dp)
            .height(106.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationY = rotY
                cameraDistance = 14f * density
            }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) RedbusCrimson else badgeColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = title,
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = contentColor.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = footer,
                    color = if (contentColor == Color.White) Color.White else badgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun WebsiteFilterRowItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.015f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "filterRowScale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) RedbusCrimson.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) RedbusCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) RedbusCrimson else iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) RedbusCrimson else MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "($count)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) RedbusCrimson else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DepartureWindowTile(
    option: DepartureWindowOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) RedbusCrimson.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        modifier = modifier.border(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) RedbusCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = if (selected) RedbusCrimson else option.tint,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = option.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) RedbusCrimson else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = option.subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BusRouteCard3D(
    route: RouteItem,
    index: Int,
    onSelect: () -> Unit
) {
    val isPrimo = route.rating >= 4.4
    val cardRotX = remember { Animatable(12f) }
    val cardScale = remember { Animatable(0.94f) }

    LaunchedEffect(route.id) {
        cardRotX.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 320 + (index.coerceAtMost(5) * 45),
                easing = FastOutSlowInEasing
            )
        )
    }
    LaunchedEffect(route.id) {
        cardScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = cardRotX.value
                scaleX = cardScale.value
                scaleY = cardScale.value
                cameraDistance = 16f * density
            }
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Operator Icon + Operator Name + Rating Pill + Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 3D Styled Bus Emblem
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPrimo) Color(0xFFFEF3C7) else RedbusCrimson.copy(alpha = 0.12f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = if (isPrimo) Color(0xFFD97706) else RedbusCrimson,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = route.operatorName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                color = RatingEmerald,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "★ ${route.rating}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = route.busType,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${route.basePrice.toInt()}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${route.availableSeats} seats left",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (route.availableSeats <= 10) WarningAmber else RatingEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Middle Row: Departure Time/City ─── Duration (Direct) ─── Arrival Time/City (Matching Image 5)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = route.departureTime,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = route.sourceCity.uppercase(Locale.ENGLISH),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${route.durationHours} hrs",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(1.5.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                    Text(
                        text = "Direct",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RatingEmerald
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = route.arrivalTime,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = route.destinationCity.uppercase(Locale.ENGLISH),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Row: Amenity Pills + "View Seats >" Button (Matching Image 5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val amenitiesList = route.amenities.orEmpty().ifEmpty {
                        listOf("WiFi", "Charging Point", "Water Bottle")
                    }
                    amenitiesList.take(3).forEach { amenity ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = amenity,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSelect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedbusCrimson,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "View Seats",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AllFiltersSheetModal(
    filterCounts: LiveFilterCounts,
    availableOperators: List<String>,
    selectedOperators: Set<String>,
    onToggleOperator: (String) -> Unit,
    selectedAmenities: Set<String>,
    onToggleAmenity: (String) -> Unit,
    isPrimoFilter: Boolean,
    onTogglePrimo: () -> Unit,
    isFreeCancelFilter: Boolean,
    onToggleFreeCancel: () -> Unit,
    isAcFilter: Boolean,
    onToggleAc: () -> Unit,
    isSleeperFilter: Boolean,
    onToggleSleeper: () -> Unit,
    isSingleSeatFilter: Boolean,
    onToggleSingleSeat: () -> Unit,
    isSeaterFilter: Boolean,
    onToggleSeater: () -> Unit,
    isNonAcFilter: Boolean,
    onToggleNonAc: () -> Unit,
    selectedDepartureWindow: DepartureWindowOption?,
    onSelectDepartureWindow: (DepartureWindowOption) -> Unit,
    selectedMaxPrice: Int?,
    onSelectMaxPrice: (Int) -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val allAmenities = listOf("WiFi", "Charging Point", "Water Bottle", "Blanket", "Live Tracking")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Bus Filters",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onResetAll) {
                            Text("Reset All", color = RedbusCrimson, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("BUS TYPES & GUARANTEES", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    WebsiteFilterRowItem(Icons.Default.Star, Color(0xFFF59E0B), "Primo Bus", filterCounts.primo, isPrimoFilter, onTogglePrimo)
                    WebsiteFilterRowItem(Icons.Default.VerifiedUser, RatingEmerald, "Free Cancellation", filterCounts.freeCancel, isFreeCancelFilter, onToggleFreeCancel)
                    WebsiteFilterRowItem(Icons.Default.AcUnit, Color(0xFF3B82F6), "AC", filterCounts.ac, isAcFilter, onToggleAc)
                    WebsiteFilterRowItem(Icons.Default.Bed, AiViolet, "SLEEPER", filterCounts.sleeper, isSleeperFilter, onToggleSleeper)
                    WebsiteFilterRowItem(Icons.Default.EventSeat, Color(0xFF14B8A6), "Single Seats", filterCounts.single, isSingleSeatFilter, onToggleSingleSeat)
                    WebsiteFilterRowItem(Icons.Default.AirlineSeatReclineNormal, Color(0xFFF97316), "SEATER", filterCounts.seater, isSeaterFilter, onToggleSeater)
                    WebsiteFilterRowItem(Icons.Default.DirectionsBus, Color(0xFF6B7280), "NONAC", filterCounts.nonAc, isNonAcFilter, onToggleNonAc)

                    HorizontalDivider()

                    Text("DEPARTURE TIME", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        DepartureWindowTile(
                            option = DepartureWindowOption.MORNING,
                            selected = selectedDepartureWindow == DepartureWindowOption.MORNING,
                            onClick = { onSelectDepartureWindow(DepartureWindowOption.MORNING) },
                            modifier = Modifier.weight(1f)
                        )
                        DepartureWindowTile(
                            option = DepartureWindowOption.AFTERNOON,
                            selected = selectedDepartureWindow == DepartureWindowOption.AFTERNOON,
                            onClick = { onSelectDepartureWindow(DepartureWindowOption.AFTERNOON) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        DepartureWindowTile(
                            option = DepartureWindowOption.EVENING,
                            selected = selectedDepartureWindow == DepartureWindowOption.EVENING,
                            onClick = { onSelectDepartureWindow(DepartureWindowOption.EVENING) },
                            modifier = Modifier.weight(1f)
                        )
                        DepartureWindowTile(
                            option = DepartureWindowOption.NIGHT,
                            selected = selectedDepartureWindow == DepartureWindowOption.NIGHT,
                            onClick = { onSelectDepartureWindow(DepartureWindowOption.NIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider()

                    Text("MAX TICKET PRICE", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(600, 900, 1200, 1800).forEach { cap ->
                            val selected = selectedMaxPrice == cap
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectMaxPrice(cap) },
                                label = { Text("Under ₹$cap", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (availableOperators.isNotEmpty()) {
                        HorizontalDivider()
                        Text("BUS OPERATORS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        availableOperators.forEach { op ->
                            val checked = selectedOperators.contains(op)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleOperator(op) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(op, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { onToggleOperator(op) },
                                    colors = CheckboxDefaults.colors(checkedColor = RedbusCrimson)
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                    Text("AMENITIES", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allAmenities.take(3).forEach { am ->
                            FilterChip(
                                selected = selectedAmenities.contains(am),
                                onClick = { onToggleAmenity(am) },
                                label = { Text(am, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allAmenities.drop(3).forEach { am ->
                            FilterChip(
                                selected = selectedAmenities.contains(am),
                                onClick = { onToggleAmenity(am) },
                                label = { Text(am, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }

                Surface(
                    shadowElevation = 12.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(50.dp)
                    ) {
                        Text("Apply Filters", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickDateSelectorDialog(
    selectedDateStr: String,
    onSelectDate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val upcomingDates = remember {
        val today = LocalDate.now()
        (0..13).map { offset -> today.plusDays(offset.toLong()) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Journey Date", fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                upcomingDates.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { d ->
                            val iso = d.toString()
                            val isSel = iso == selectedDateStr
                            val dayName = d.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
                            val monthName = d.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
                            Surface(
                                onClick = { onSelectDate(iso) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) RedbusCrimson else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$dayName, ${d.dayOfMonth} $monthName",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = RedbusCrimson, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun parseHour(timeStr: String): Int {
    return try {
        val clean = timeStr.trim().uppercase(Locale.ENGLISH)
        val isPm = clean.contains("PM")
        val isAm = clean.contains("AM")
        val parts = clean.replace(Regex("[^0-9:]"), "").split(":")
        var h = parts.firstOrNull()?.toIntOrNull() ?: 0
        if (isPm && h < 12) h += 12
        if (isAm && h == 12) h = 0
        h
    } catch (_: Exception) {
        12
    }
}
