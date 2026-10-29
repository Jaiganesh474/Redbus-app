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
import androidx.compose.material.icons.filled.*
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
import com.redbus.ai.data.model.*
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.viewmodel.RedbusUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSelectionScreen(
    uiState: RedbusUiState,
    onSelectDeck: (SeatDeck) -> Unit,
    onToggleSeat: (SeatItem) -> Unit,
    onApplyAiSmartSeat: () -> Unit,
    onUpdateBoardingDropping: (String, String) -> Unit,
    onProceedToCheckout: () -> Unit,
    onBack: () -> Unit
) {
    val route = uiState.selectedRoute ?: return
    val layout = uiState.seatLayout ?: return
    var showPointsDialog by remember { mutableStateOf(false) }

    val deckSeats = remember(layout.seats, uiState.selectedDeck) {
        layout.seats.filter { it.deck == uiState.selectedDeck }
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
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedbusCrimson,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Continue ➔",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
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
            // 1. AI Smart Seat Recommendation Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = AiViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Smart Seat Engine",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Berths L4, L5 & U4 have 42% lower road vibration & fastest exit access.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = onApplyAiSmartSeat,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AiViolet,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "✨ AI Pick",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Deck Switcher (Lower Deck vs Upper Deck)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    SeatDeck.entries.forEach { deck ->
                        val active = uiState.selectedDeck == deck
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
                                text = if (deck == SeatDeck.LOWER) "LOWER DECK" else "UPPER DECK (SLEEPER)",
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 3. Color Legend
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendDot(color = RatingEmerald, label = "Available")
                    LegendDot(color = AiViolet, label = "AI Pick")
                    LegendDot(color = FemaleSeatPink, label = "Female")
                    LegendDot(color = RedbusCrimson, label = "Selected")
                    LegendDot(color = Color.Gray, label = "Booked")
                }
            }

            // 4. Realistic 2+1 Coach Cabin Matrix
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Driver Cabin Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FRONT CABIN / ENTRY",
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
                                        contentDescription = "Steering Wheel",
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

                        // Seat Rows (2+1 Coach Layout)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowsMap.forEach { (_, rowSeats) ->
                                val col1 = rowSeats.firstOrNull { it.colNum == 1 }
                                val col2 = rowSeats.firstOrNull { it.colNum == 2 }
                                val col3 = rowSeats.firstOrNull { it.colNum == 3 }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Twin Seats (Col 1 & Col 2)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        col1?.let { seat ->
                                            SeatCell(
                                                seat = seat,
                                                isSelected = uiState.selectedSeats.any { it.id == seat.id },
                                                onClick = { onToggleSeat(seat) }
                                            )
                                        }
                                        col2?.let { seat ->
                                            SeatCell(
                                                seat = seat,
                                                isSelected = uiState.selectedSeats.any { it.id == seat.id },
                                                onClick = { onToggleSeat(seat) }
                                            )
                                        }
                                    }

                                    // Aisle Label
                                    Text(
                                        text = "AISLE",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Right Single Sleeper Berth (Col 3)
                                    col3?.let { seat ->
                                        SeatCell(
                                            seat = seat,
                                            isSelected = uiState.selectedSeats.any { it.id == seat.id },
                                            onClick = { onToggleSeat(seat) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Boarding & Dropping Point Card
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
                                text = "Boarding: ${uiState.selectedBoardingPoint}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dropping: ${uiState.selectedDroppingPoint}",
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

    if (showPointsDialog) {
        AlertDialog(
            onDismissRequest = { showPointsDialog = false },
            title = { Text("Select Boarding & Dropping Points", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Boarding Points", fontWeight = FontWeight.Black, color = RedbusCrimson)
                    route.boardingPoints.forEach { bp ->
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
                    route.droppingPoints.forEach { dp ->
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
    val isAvailable = seat.status == SeatStatus.AVAILABLE
    val borderColor = when {
        isSelected -> RedbusCrimson
        !isAvailable -> Color.Gray.copy(alpha = 0.4f)
        seat.isAiRecommended -> AiViolet
        seat.genderRestriction == GenderRestriction.FEMALE -> FemaleSeatPink
        else -> RatingEmerald
    }
    val containerColor = when {
        isSelected -> RedbusCrimson
        !isAvailable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        seat.isAiRecommended -> AiSoftPurple
        seat.genderRestriction == GenderRestriction.FEMALE -> FemaleSeatPinkBg
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        isSelected -> Color.White
        !isAvailable -> Color.Gray
        seat.isAiRecommended -> AiViolet
        seat.genderRestriction == GenderRestriction.FEMALE -> FemaleSeatPink
        else -> MaterialTheme.colorScheme.onSurface
    }

    val heightDp = if (seat.seatType == SeatType.SLEEPER) 74.dp else 58.dp

    Box(
        modifier = Modifier
            .width(66.dp)
            .height(heightDp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.8.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = isAvailable, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (seat.isAiRecommended && !isSelected) {
                Text(
                    text = "✨ AI",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = AiViolet
                )
            }
            Text(
                text = seat.seatNumber,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (!isAvailable) "SOLD" else "₹${seat.price.toInt()}",
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

