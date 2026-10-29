package com.redbus.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redbus.ai.data.model.SeatItem
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.viewmodel.RedbusUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSelectionScreen(
    uiState: RedbusUiState,
    onSelectDeck: (String) -> Unit,
    onToggleSeat: (SeatItem) -> Unit,
    onUpdateBoardingDropping: (String, String) -> Unit,
    onProceedToCheckout: () -> Unit,
    onBack: () -> Unit
) {
    val route = uiState.selectedRoute ?: return
    val layout = uiState.seatLayout ?: return
    var showPointsDialog by remember { mutableStateOf(false) }

    val hasUpperDeck = remember(layout.seats) {
        layout.seats.any { it.deck.equals("UPPER", ignoreCase = true) }
    }
    val deckSeats = remember(layout.seats, uiState.selectedDeck) {
        val filtered = layout.seats.filter { it.deck.equals(uiState.selectedDeck, ignoreCase = true) }
        filtered.ifEmpty { layout.seats }
    }
    val rowsMap = remember(deckSeats) {
        deckSeats.groupBy { it.rowNum }.toSortedMap()
    }
    val totalSelectedPrice = uiState.selectedSeats.sumOf { it.price }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = route.operatorName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "${route.sourceCity} ➔ ${route.destinationCity} • ${route.departureTime}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RedbusCrimson)
            )
        },
        bottomBar = {
            if (uiState.selectedSeats.isNotEmpty()) {
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
                        Column {
                            Text(
                                text = "Seats: ${uiState.selectedSeats.joinToString(", ") { it.seatNumber }}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total Fare: ₹${totalSelectedPrice.toInt()}",
                                style = MaterialTheme.typography.titleLarge,
                                color = RedbusCrimson,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Button(
                            onClick = onProceedToCheckout,
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedbusCrimson,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = if (uiState.isLoading) "Locking..." else "Lock Seats & Continue ➔",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (hasUpperDeck) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        listOf("LOWER", "UPPER").forEach { deck ->
                            val active = uiState.selectedDeck.equals(deck, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) RedbusCrimson else Color.Transparent)
                                    .clickable { onSelectDeck(deck) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (deck == "LOWER") "LOWER DECK" else "UPPER DECK",
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendDot(color = RatingEmerald, label = "Available")
                    LegendDot(color = FemaleSeatPink, label = "Female")
                    LegendDot(color = RedbusCrimson, label = "Selected")
                    LegendDot(color = WarningAmber, label = "Locked")
                    LegendDot(color = Color.Gray, label = "Booked")
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE SEAT MAP (${uiState.selectedDeck} DECK)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Support,
                                        contentDescription = "Driver",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowsMap.forEach { (_, rowSeats) ->
                                val sortedCols = rowSeats.sortedBy { it.colNum }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    sortedCols.forEach { seat ->
                                        SeatCell(
                                            seat = seat,
                                            isSelected = uiState.selectedSeats.any {
                                                it.seatId == seat.seatId && it.seatNumber == seat.seatNumber
                                            },
                                            onClick = { onToggleSeat(seat) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!route.boardingPoints.isNullOrEmpty() || !route.droppingPoints.isNullOrEmpty()) {
                item {
                    Card(
                        onClick = { showPointsDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "BOARDING & DROPPING POINTS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RedbusCrimson
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Boarding: ${uiState.selectedBoardingPoint.ifBlank { route.sourceCity }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Dropping: ${uiState.selectedDroppingPoint.ifBlank { route.destinationCity }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { showPointsDialog = true }) {
                                Text("Change", color = RedbusCrimson, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPointsDialog) {
        AlertDialog(
            onDismissRequest = { showPointsDialog = false },
            title = { Text("Select Boarding & Dropping Points", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Boarding Points", fontWeight = FontWeight.Black, color = RedbusCrimson)
                    route.boardingPoints.orEmpty().forEach { bp ->
                        Surface(
                            onClick = { onUpdateBoardingDropping(bp, uiState.selectedDroppingPoint) },
                            color = if (uiState.selectedBoardingPoint == bp) RedbusSoftTint else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = bp,
                                modifier = Modifier.padding(10.dp),
                                fontWeight = if (uiState.selectedBoardingPoint == bp) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Dropping Points", fontWeight = FontWeight.Black, color = AiViolet)
                    route.droppingPoints.orEmpty().forEach { dp ->
                        Surface(
                            onClick = { onUpdateBoardingDropping(uiState.selectedBoardingPoint, dp) },
                            color = if (uiState.selectedDroppingPoint == dp) AiSoftPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = dp,
                                modifier = Modifier.padding(10.dp),
                                fontWeight = if (uiState.selectedDroppingPoint == dp) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPointsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun SeatCell(
    seat: SeatItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isAvailable = seat.status.equals("AVAILABLE", ignoreCase = true)
    val isLocked = seat.status.equals("LOCKED", ignoreCase = true)
    val isFemale = seat.genderRestriction.equals("FEMALE", ignoreCase = true)
    val isSleeper = seat.seatType.equals("SLEEPER", ignoreCase = true)

    val borderColor = when {
        isSelected -> RedbusCrimson
        isLocked -> WarningAmber
        !isAvailable -> Color.Gray.copy(alpha = 0.4f)
        isFemale -> FemaleSeatPink
        else -> RatingEmerald
    }
    val containerColor = when {
        isSelected -> RedbusCrimson
        isLocked -> WarningAmberBg
        !isAvailable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        isFemale -> FemaleSeatPinkBg
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        isSelected -> Color.White
        isLocked -> WarningAmber
        !isAvailable -> Color.Gray
        isFemale -> FemaleSeatPink
        else -> MaterialTheme.colorScheme.onSurface
    }

    val heightDp = if (isSleeper) 72.dp else 56.dp

    Box(
        modifier = Modifier
            .width(64.dp)
            .height(heightDp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.8.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = isAvailable, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = seat.seatNumber,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = when {
                    isLocked -> "LOCKED"
                    !isAvailable -> "SOLD"
                    else -> "₹${seat.price.toInt()}"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
