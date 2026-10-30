package com.redbus.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redbus.ai.data.model.ReviewItem
import com.redbus.ai.data.model.RouteItem
import com.redbus.ai.data.model.SeatItem
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.viewmodel.RedbusUiState
import kotlin.math.roundToInt

private enum class SeatModalStep {
    SELECT_SEATS,
    BOARD_DROP
}

@Composable
fun SeatSelectionScreen(
    uiState: RedbusUiState,
    onToggleSeat: (SeatItem) -> Unit,
    onUpdateBoardingDropping: (String, String) -> Unit,
    onProceedToCheckout: () -> Unit,
    onBack: () -> Unit,
    onSubmitReview: (Long, Int, String) -> Unit = { _, _, _ -> }
) {
    val route = uiState.selectedRoute ?: return
    val layout = uiState.seatLayout ?: return

    var currentStep by remember { mutableStateOf(SeatModalStep.SELECT_SEATS) }
    var activeDetailTab by remember { mutableStateOf("cancellation") }

    // Check if the bus actually has an upper deck (true for Sleeper coaches, false for Seater 2+2 buses)
    val hasUpperDeck = remember(layout.seats) {
        layout.seats.any { it.deck.equals("UPPER", ignoreCase = true) }
    }

    // Partition seats into Lower Deck and Upper Deck without creating duplicate phantom seats
    val lowerSeats = remember(layout.seats, hasUpperDeck) {
        if (hasUpperDeck) {
            layout.seats.filter { it.deck.equals("LOWER", ignoreCase = true) || it.deck.isBlank() }
        } else {
            layout.seats
        }
    }
    val upperSeats = remember(layout.seats, hasUpperDeck) {
        if (hasUpperDeck) {
            layout.seats.filter { it.deck.equals("UPPER", ignoreCase = true) }
        } else {
            emptyList()
        }
    }

    val boardingStops = remember(route) {
        route.boardingPoints?.takeIf { it.isNotEmpty() } ?: listOf(
            "${route.sourceCity} Majestic Bus Stand (21:15)",
            "${route.sourceCity} Madiwala Police Station (21:45)",
            "${route.sourceCity} Silk Board Flyover (22:05)",
            "${route.sourceCity} Electronic City Toll (22:30)"
        )
    }

    val droppingStops = remember(route) {
        route.droppingPoints?.takeIf { it.isNotEmpty() } ?: listOf(
            "${route.destinationCity} Koyambedu CMBT (04:45)",
            "${route.destinationCity} Guindy Kathipara (05:05)",
            "${route.destinationCity} Perungudi OMR (05:20)",
            "${route.destinationCity} Tambaram GST Road (05:35)"
        )
    }

    val totalSelectedPrice = uiState.selectedSeats.sumOf {
        if (it.price > 0) it.price else route.basePrice
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Header matching redbusai.app screenshot
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentStep == SeatModalStep.BOARD_DROP) {
                                        currentStep = SeatModalStep.SELECT_SEATS
                                    } else {
                                        onBack()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (currentStep == SeatModalStep.BOARD_DROP)
                                        Icons.AutoMirrored.Filled.ArrowBack
                                    else
                                        Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = route.sourceCity,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = " ➔ ",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = RedbusCrimson
                                    )
                                    Text(
                                        text = route.destinationCity,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${route.operatorName} • ${route.busType} • ${route.travelDate} (${route.departureTime} - ${route.arrivalTime})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Green Rating Badge + Ratings Count
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                color = Color(0xFF15803D),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = String.format("%.1f", route.rating),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "427 ratings",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Step Breadcrumbs Bar: Select seats | Board/Drop point | Passenger Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepTabItem(
                            title = "Select seats",
                            isActive = currentStep == SeatModalStep.SELECT_SEATS,
                            isEnabled = true,
                            onClick = { currentStep = SeatModalStep.SELECT_SEATS }
                        )
                        StepTabItem(
                            title = "Board/Drop point",
                            isActive = currentStep == SeatModalStep.BOARD_DROP,
                            isEnabled = uiState.selectedSeats.isNotEmpty(),
                            onClick = {
                                if (uiState.selectedSeats.isNotEmpty()) {
                                    currentStep = SeatModalStep.BOARD_DROP
                                }
                            }
                        )
                        StepTabItem(
                            title = "Passenger Info",
                            isActive = false,
                            isEnabled = uiState.selectedSeats.isNotEmpty() && uiState.selectedBoardingPoint.isNotBlank(),
                            onClick = {
                                if (uiState.selectedSeats.isNotEmpty()) {
                                    onProceedToCheckout()
                                }
                            }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        },
        bottomBar = {
            // Sticky Bottom Bar matching screenshot
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Selected Seats: ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (uiState.selectedSeats.isEmpty()) "None"
                                else "${uiState.selectedSeats.joinToString(", ") { it.seatNumber }} (₹${totalSelectedPrice.roundToInt()})",
                                fontSize = 12.sp,
                                fontWeight = if (uiState.selectedSeats.isEmpty()) FontWeight.Normal else FontWeight.Black,
                                color = if (uiState.selectedSeats.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.selectedBoardingPoint.isBlank() || uiState.selectedDroppingPoint.isBlank())
                                "Board & Drop points not selected"
                            else
                                "${uiState.selectedBoardingPoint.substringBefore(" (")} ➔ ${uiState.selectedDroppingPoint.substringBefore(" (")}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val hasSeats = uiState.selectedSeats.isNotEmpty()
                    Button(
                        onClick = {
                            if (currentStep == SeatModalStep.SELECT_SEATS) {
                                if (uiState.selectedBoardingPoint.isBlank()) {
                                    onUpdateBoardingDropping(
                                        boardingStops.first(),
                                        droppingStops.first()
                                    )
                                }
                                currentStep = SeatModalStep.BOARD_DROP
                            } else {
                                onProceedToCheckout()
                            }
                        },
                        enabled = hasSeats && !uiState.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedbusCrimson,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
                    ) {
                        Text(
                            text = if (currentStep == SeatModalStep.SELECT_SEATS)
                                "Select boarding & dropping points"
                            else if (uiState.isLoading)
                                "Locking Seats..."
                            else
                                "Proceed to Passenger Info",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (currentStep == SeatModalStep.SELECT_SEATS) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Dual Deck Side-by-Side Sleeper Coach Chassis (Exact Match to Screenshot)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (hasUpperDeck) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // LOWER DECK CHASSIS
                                DeckChassisColumn(
                                    title = "Lower Deck",
                                    isLowerDeck = true,
                                    deckSeats = lowerSeats,
                                    basePrice = route.basePrice,
                                    selectedSeats = uiState.selectedSeats,
                                    onToggleSeat = onToggleSeat,
                                    modifier = Modifier.weight(1f)
                                )

                                // UPPER DECK CHASSIS
                                DeckChassisColumn(
                                    title = "Upper Deck",
                                    isLowerDeck = false,
                                    deckSeats = upperSeats,
                                    basePrice = route.basePrice,
                                    selectedSeats = uiState.selectedSeats,
                                    onToggleSeat = onToggleSeat,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            // Single Deck Chassis for Seater 2+2 / Single-Deck Buses (No duplicates)
                            SingleDeckChassisColumn(
                                busType = route.busType,
                                deckSeats = lowerSeats,
                                basePrice = route.basePrice,
                                selectedSeats = uiState.selectedSeats,
                                onToggleSeat = onToggleSeat,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Seat Legend Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SeatLegendItem(
                                borderColor = Color(0xFF10B981),
                                fillColor = Color.White,
                                label = "Available"
                            )
                            SeatLegendItem(
                                borderColor = Color(0xFF15803D),
                                fillColor = Color(0xFF15803D),
                                label = "Selected"
                            )
                            SeatLegendItem(
                                borderColor = Color(0xFFCBD5E1),
                                fillColor = Color(0xFFE2E8F0),
                                label = "Sold"
                            )
                            SeatLegendItem(
                                borderColor = Color(0xFFF472B6),
                                fillColor = Color(0xFFFDF2F8),
                                label = "Female"
                            )
                        }
                    }
                }

                // 2. Detail Tabs & Cancellation Policy Table (Exact Match to Right Panel in Screenshot)
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Horizontal Scrollable Tabs Bar with Left/Right Chevron Buttons
                            val scrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(scrollState)
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val tabs = listOf(
                                        "highlights" to "Highlights",
                                        "photos" to "📸 Bus Photos",
                                        "ai_demand" to "⚡ AI Punctuality & Demand",
                                        "cancellation" to "Cancellation policy",
                                        "boarding" to "Boarding & Dropping",
                                        "reviews" to "⭐ Customer Reviews (${if (uiState.currentBusReviews.isNotEmpty()) uiState.currentBusReviews.size else 4})"
                                    )
                                    tabs.forEach { (id, label) ->
                                        val selected = activeDetailTab == id
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (selected) RedbusCrimson.copy(alpha = 0.12f)
                                                    else Color.Transparent
                                                )
                                                .clickable { activeDetailTab = id }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (selected) RedbusCrimson
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            // Tab Content Area
                            Box(modifier = Modifier.padding(16.dp)) {
                                when (activeDetailTab) {
                                    "cancellation" -> CancellationPolicyPanel(route = route)
                                    "highlights" -> HighlightsPanel(route = route)
                                    "ai_demand" -> AiPunctualityPanel(route = route)
                                    "photos" -> BusPhotosPanel(route = route)
                                    "boarding" -> BoardingPreviewPanel(
                                        boardingStops = boardingStops,
                                        droppingStops = droppingStops
                                    )
                                    "reviews" -> CustomerReviewsPanel(
                                        busId = if (route.busId > 0) route.busId.toLong() else route.id.toLong(),
                                        reviews = uiState.currentBusReviews,
                                        isLoading = uiState.isLoadingReviews,
                                        onSubmitReview = onSubmitReview
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // STEP 2: BOARDING & DROPPING POINT SELECTION
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Select Boarding Point (${route.sourceCity})",
                                style = MaterialTheme.typography.titleMedium,
                                color = RedbusCrimson,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            boardingStops.forEach { bp ->
                                val isChosen = uiState.selectedBoardingPoint == bp
                                Surface(
                                    onClick = {
                                        onUpdateBoardingDropping(
                                            bp,
                                            uiState.selectedDroppingPoint.ifBlank { droppingStops.first() }
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isChosen) {
                                        if (uiState.isDarkTheme) Color(0xFF4C1D24) else Color(0xFFFFF1F2)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                        .border(
                                            width = if (isChosen) 1.5.dp else 1.dp,
                                            color = if (isChosen) RedbusCrimson else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            RadioButton(
                                                selected = isChosen,
                                                onClick = null,
                                                colors = RadioButtonDefaults.colors(selectedColor = RedbusCrimson)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = bp,
                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Select Dropping Point (${route.destinationCity})",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            droppingStops.forEach { dp ->
                                val isChosen = uiState.selectedDroppingPoint == dp
                                Surface(
                                    onClick = {
                                        onUpdateBoardingDropping(
                                            uiState.selectedBoardingPoint.ifBlank { boardingStops.first() },
                                            dp
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isChosen) {
                                        if (uiState.isDarkTheme) Color(0xFF123B2B) else Color(0xFFECFDF5)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                        .border(
                                            width = if (isChosen) 1.5.dp else 1.dp,
                                            color = if (isChosen) Color(0xFF15803D) else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isChosen,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF15803D))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = dp,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepTabItem(
    title: String,
    isActive: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                isActive -> RedbusCrimson
                isEnabled -> Color(0xFF475569)
                else -> Color(0xFFCBD5E1)
            }
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(2.5.dp)
                .background(if (isActive) RedbusCrimson else Color.Transparent)
        )
    }
}

private fun isSeatSelected(seat: SeatItem, selectedSeats: List<SeatItem>): Boolean {
    val targetId = if (seat.seatId != 0) seat.seatId else seat.id
    return selectedSeats.any {
        val curId = if (it.seatId != 0) it.seatId else it.id
        if (curId != 0 && targetId != 0) {
            curId == targetId
        } else {
            it.seatNumber == seat.seatNumber && it.deck == seat.deck
        }
    }
}

@Composable
private fun SingleDeckChassisColumn(
    busType: String,
    deckSeats: List<SeatItem>,
    basePrice: Double,
    selectedSeats: List<SeatItem>,
    onToggleSeat: (SeatItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val is2x2 = busType.contains("2+2", ignoreCase = true) ||
            busType.contains("2x2", ignoreCase = true) ||
            busType.contains("Seater", ignoreCase = true) ||
            deckSeats.size > 20

    val rows = remember(deckSeats, is2x2) {
        val grouped = deckSeats.groupBy { s ->
            if (s.rowNum > 0) s.rowNum else 1
        }
        if (grouped.size > 1) {
            grouped.toSortedMap().values.toList()
        } else {
            deckSeats.chunked(if (is2x2) 4 else 3)
        }
    }

    Column(modifier = modifier) {
        // Deck Header: BUS SEAT LAYOUT + Steering Wheel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BUS SEAT LAYOUT (${deckSeats.size} SEATS)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(26.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Support,
                        contentDescription = "Steering Wheel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Rounded White Bus Chassis
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 32.dp,
                topEnd = 32.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.8.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(
                        topStart = 32.dp,
                        topEnd = 32.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rows.forEach { rowList ->
                    if (is2x2) {
                        // 2 on Left, Aisle, 2 on Right (2+2 Seater matching ZingBus)
                        val s1 = rowList.getOrNull(0)
                        val s2 = rowList.getOrNull(1)
                        val s3 = rowList.getOrNull(2)
                        val s4 = rowList.getOrNull(3)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Pair (2 Seats)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (s1 != null) {
                                    SleeperBerthItem(
                                        seat = s1,
                                        fallbackPrice = basePrice,
                                        isSelected = isSeatSelected(s1, selectedSeats),
                                        onClick = { onToggleSeat(s1) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(42.dp))
                                }

                                if (s2 != null) {
                                    SleeperBerthItem(
                                        seat = s2,
                                        fallbackPrice = basePrice,
                                        isSelected = isSeatSelected(s2, selectedSeats),
                                        onClick = { onToggleSeat(s2) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(42.dp))
                                }
                            }

                            // Center Aisle
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "· ·",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Right Pair (2 Seats)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (s3 != null) {
                                    SleeperBerthItem(
                                        seat = s3,
                                        fallbackPrice = basePrice,
                                        isSelected = isSeatSelected(s3, selectedSeats),
                                        onClick = { onToggleSeat(s3) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(42.dp))
                                }

                                if (s4 != null) {
                                    SleeperBerthItem(
                                        seat = s4,
                                        fallbackPrice = basePrice,
                                        isSelected = isSeatSelected(s4, selectedSeats),
                                        onClick = { onToggleSeat(s4) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(42.dp))
                                }
                            }
                        }
                    } else {
                        // 1 on Left, Aisle, 2 on Right (2+1 Single Deck Sleeper)
                        val s1 = rowList.getOrNull(0)
                        val s2 = rowList.getOrNull(1)
                        val s3 = rowList.getOrNull(2)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (s1 != null) {
                                SleeperBerthItem(
                                    seat = s1,
                                    fallbackPrice = basePrice,
                                    isSelected = isSeatSelected(s1, selectedSeats),
                                    onClick = { onToggleSeat(s1) }
                                )
                            } else {
                                Spacer(modifier = Modifier.width(42.dp))
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (s2 != null) {
                                    SleeperBerthItem(
                                        seat = s2,
                                        fallbackPrice = basePrice,
                                        isSelected = isSeatSelected(s2, selectedSeats),
                                        onClick = { onToggleSeat(s2) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(42.dp))
                                }

                                if (s3 != null) {
                                    SleeperBerthItem(
                                        seat = s3,
                                        fallbackPrice = basePrice,
                                        isSelected = isSeatSelected(s3, selectedSeats),
                                        onClick = { onToggleSeat(s3) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(42.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "▼ EMERGENCY EXIT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DeckChassisColumn(
    title: String,
    isLowerDeck: Boolean,
    deckSeats: List<SeatItem>,
    basePrice: Double,
    selectedSeats: List<SeatItem>,
    onToggleSeat: (SeatItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group into rows of 3 seats (1 left + 2 right) just like redbusai.app screenshot
    val rows = remember(deckSeats) {
        val grouped = deckSeats.groupBy { s ->
            if (s.rowNum > 0) s.rowNum else 1
        }
        if (grouped.size > 1) {
            grouped.toSortedMap().values.toList()
        } else {
            deckSeats.chunked(3)
        }
    }

    Column(modifier = modifier) {
        // Deck Header Label + Steering Wheel / Upper Arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLowerDeck) {
                        Icon(
                            imageVector = Icons.Default.Support,
                            contentDescription = "Steering Wheel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = "⬆",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Rounded White Bus Deck Chassis
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.8.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rows.forEach { rowList ->
                    val leftSeat = rowList.find { it.colNum == 1 } ?: rowList.getOrNull(0)
                    val rightSeat1 = rowList.find { it.colNum == 2 } ?: rowList.getOrNull(1)
                    val rightSeat2 = rowList.find { it.colNum == 3 } ?: rowList.getOrNull(2)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Single Sleeper Berth (Col 1: L1, L4, L7, L10, L13)
                        if (leftSeat != null) {
                            SleeperBerthItem(
                                seat = leftSeat,
                                fallbackPrice = basePrice,
                                isSelected = isSeatSelected(leftSeat, selectedSeats),
                                onClick = { onToggleSeat(leftSeat) }
                            )
                        } else {
                            Spacer(modifier = Modifier.width(42.dp))
                        }

                        // Aisle Space
                        Spacer(modifier = Modifier.width(8.dp))

                        // Right Double Sleeper Berths (Col 2 & Col 3: L2 & L3, L5 & L6...)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (rightSeat1 != null) {
                                SleeperBerthItem(
                                    seat = rightSeat1,
                                    fallbackPrice = basePrice,
                                    isSelected = isSeatSelected(rightSeat1, selectedSeats),
                                    onClick = { onToggleSeat(rightSeat1) }
                                )
                            } else {
                                Spacer(modifier = Modifier.width(42.dp))
                            }

                            if (rightSeat2 != null) {
                                SleeperBerthItem(
                                    seat = rightSeat2,
                                    fallbackPrice = basePrice,
                                    isSelected = isSeatSelected(rightSeat2, selectedSeats),
                                    onClick = { onToggleSeat(rightSeat2) }
                                )
                            } else {
                                Spacer(modifier = Modifier.width(42.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "▼ EMERGENCY EXIT",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SleeperBerthItem(
    seat: SeatItem,
    fallbackPrice: Double,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isSold = seat.status.equals("BOOKED", ignoreCase = true) ||
            seat.status.equals("LOCKED", ignoreCase = true)
    val isFemale = seat.genderRestriction.equals("FEMALE", ignoreCase = true) ||
            seat.bookedGender.equals("FEMALE", ignoreCase = true)

    val borderColor = when {
        isSold && isFemale -> Color(0xFFF472B6)
        isSold -> MaterialTheme.colorScheme.outline
        isSelected -> Color(0xFF15803D)
        else -> Color(0xFF10B981)
    }

    val containerColor = when {
        isSold && isFemale -> Color(0xFFFDF2F8)
        isSold -> MaterialTheme.colorScheme.surface
        isSelected -> Color(0xFF15803D)
        else -> MaterialTheme.colorScheme.surface
    }

    val pillowColor = when {
        isSold && isFemale -> Color(0xFFFBCFE8)
        isSold -> MaterialTheme.colorScheme.surfaceVariant
        isSelected -> Color(0xFF6EE7B7)
        else -> Color(0xFFD1FAE5)
    }

    val priceText = if (isSold) "Sold" else "₹${(if (seat.price > 0) seat.price else fallbackPrice).roundToInt()}"
    val priceColor = when {
        isSold -> MaterialTheme.colorScheme.onSurfaceVariant
        isSelected -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    val seatNumColor = when {
        isSelected -> Color(0xFFD1FAE5)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .width(42.dp)
            .height(74.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(1.8.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = !isSold, onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 2.dp)
    ) {
        // Top Pillow Bar inside Sleeper Berth
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(22.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(pillowColor)
        )

        // Center Icon when Selected or Sold
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.Center)
            )
        } else if (isSold) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = if (isFemale) Color(0xFFF472B6) else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.Center)
            )
        }

        // Bottom Price + Seat Number (Exact match to screenshot: ₹850 above L1)
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = priceText,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = priceColor,
                lineHeight = 11.sp
            )
            Text(
                text = seat.seatNumber,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = seatNumColor,
                lineHeight = 9.sp
            )
        }
    }
}

@Composable
private fun SeatLegendItem(
    borderColor: Color,
    fillColor: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(fillColor)
                .border(1.5.dp, borderColor, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CancellationPolicyPanel(route: RouteItem) {
    val fare = route.basePrice.roundToInt().coerceAtLeast(650)
    val tiers = listOf(
        Triple("More than 24 hrs before departure", "85% Refund (₹${(fare * 0.85).roundToInt()})", "✓ 100% Refund\n(₹$fare)"),
        Triple("12 hrs to 24 hrs before departure", "70% Refund (₹${(fare * 0.70).roundToInt()})", "✓ 100% Refund\n(₹$fare)"),
        Triple("6 hrs to 12 hrs before departure", "50% Refund (₹${(fare * 0.50).roundToInt()})", "✓ 100% Refund\n(₹$fare)"),
        Triple("2 hrs to 6 hrs before departure", "25% Refund (₹${(fare * 0.25).roundToInt()})", "✓ 100% Refund\n(₹$fare)"),
        Triple("Less than 2 hrs before departure", "0% refund", "0% refund")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = "Cancellation policy",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Dynamic refund schedule for journey on ${route.travelDate} (${route.departureTime}).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Table Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
        ) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 10.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time before travel",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "Without free\ncancellation",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "With free\ncancellation",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF15803D),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }

            tiers.forEach { (timeRange, withoutText, withText) ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeRange,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = withoutText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (MaterialTheme.colorScheme.background == DarkBackground) {
                                    Color(0xFF123B2B)
                                } else {
                                    Color(0xFFECFDF5)
                                }
                            )
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = withText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF34D399),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Bottom Red Free Cancellation Callout Banner (Matching Screenshot)
        Surface(
            color = if (MaterialTheme.colorScheme.background == DarkBackground) {
                Color(0xFF4C1D24)
            } else {
                Color(0xFFFFF1F2)
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (MaterialTheme.colorScheme.background == DarkBackground) {
                        Color(0xFF7F1D1D)
                    } else {
                        Color(0xFFFECDD3)
                    },
                    RoundedCornerShape(14.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = RedbusCrimson,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "₹",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Add Free Cancellation while booking to get a 100% refund on cancellation.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (MaterialTheme.colorScheme.background == DarkBackground) {
                        Color(0xFFFFCDD2)
                    } else {
                        Color(0xFF881337)
                    }
                )
            }
        }
    }
}

@Composable
private fun HighlightsPanel(route: RouteItem) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Bus Highlights & Amenities",
            fontWeight = FontWeight.Black,
            fontSize = 15.sp
        )
        Text(
            text = "${route.operatorName} operates ${route.busType} with live GPS tracking, sanitized sleeper berths, and 24/7 passenger assistance.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (route.amenities?.takeIf { it.isNotEmpty() } ?: listOf("Live Tracking", "USB Charging", "Blanket", "Reading Light")).forEach { amenity ->
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = amenity,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiPunctualityPanel(route: RouteItem) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "⚡ AI Punctuality & Route Demand",
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = AiViolet
        )
        Text(
            text = "• 96% historical on-time arrival for ${route.sourceCity} ➔ ${route.destinationCity}\n• Mid-coach berths (L4, L5, U4, U5) offer 40% lower road vibration.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BusPhotosPanel(route: RouteItem) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Verified Coach Fleet (${route.operatorName})",
            fontWeight = FontWeight.Black,
            fontSize = 15.sp
        )
        Text(
            text = "${route.busType} • Memory foam sleeper berths, individual charging ports & privacy curtains.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BoardingPreviewPanel(
    boardingStops: List<String>,
    droppingStops: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Boarding Points", fontWeight = FontWeight.Black, fontSize = 14.sp, color = RedbusCrimson)
        boardingStops.forEach { stop ->
            Text("• $stop", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("Dropping Points", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF15803D))
        droppingStops.forEach { stop ->
            Text("• $stop", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Customer Reviews and Ratings Panel matching Image 4 (`media_1793309130865_7ec679a9.png`)
 * Real dynamic reviews fetched from /api/reviews with "+ Write Review" modal.
 */
@Composable
private fun CustomerReviewsPanel(
    busId: Long,
    reviews: List<ReviewItem>,
    isLoading: Boolean,
    onSubmitReview: (Long, Int, String) -> Unit
) {
    var showWriteReviewDialog by remember { mutableStateOf(false) }

    val displayedReviews = remember(reviews) {
        reviews.ifEmpty {
            listOf(
                ReviewItem(
                    id = 1,
                    busId = busId,
                    userName = "Arun Kumar R.",
                    rating = 5,
                    comment = "Excellent Volvo journey! Bus was on time at Madiwala. Clean blankets, working charging port, and very smooth driving overnight."
                ),
                ReviewItem(
                    id = 2,
                    busId = busId,
                    userName = "Priya Venkatesh",
                    rating = 5,
                    comment = "Safe for solo female travelers. The sleeper berth was spacious and curtains provided full privacy. Driver was courteous."
                ),
                ReviewItem(
                    id = 3,
                    busId = busId,
                    userName = "Karthik Subramanian",
                    rating = 4,
                    comment = "Good rest stop at Shri Balaji Bhavan. Food and washrooms were clean. Arrived in Chennai 15 minutes ahead of schedule."
                ),
                ReviewItem(
                    id = 4,
                    busId = busId,
                    userName = "Divya N.",
                    rating = 4,
                    comment = "Pleasant travel experience. Water bottle provided. Smooth online ticket confirmation via redBus."
                )
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header matching screenshot: "Customer Reviews" and "+ Write Review"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Customer Reviews",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(
                onClick = { showWriteReviewDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = RedbusCrimson)
            ) {
                Text(
                    text = "+ Write Review",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
        }

        if (isLoading && reviews.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedbusCrimson, modifier = Modifier.size(28.dp))
            }
        } else {
            displayedReviews.forEach { r ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = r.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Rating badge pill matching screenshot (e.g. 5 ★ in green surface)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${r.rating}",
                                        color = Color(0xFF15803D),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = r.comment,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    if (showWriteReviewDialog) {
        var selectedRating by remember { mutableStateOf(5) }
        var reviewText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showWriteReviewDialog = false },
            title = {
                Text(
                    text = "Write Bus Review",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Rate your journey experience:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            IconButton(
                                onClick = { selectedRating = star },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star stars",
                                    tint = if (star <= selectedRating) Color(0xFFEAB308) else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        label = { Text("Your Review / Journey Feedback") },
                        placeholder = { Text("e.g. Bus was on time, driver was courteous, clean seats...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reviewText.isNotBlank()) {
                            onSubmitReview(busId, selectedRating, reviewText)
                            showWriteReviewDialog = false
                        }
                    },
                    enabled = reviewText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Submit Review", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWriteReviewDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
