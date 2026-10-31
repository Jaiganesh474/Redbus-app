package com.redbus.ai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.redbus.ai.data.model.BookingDetails
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.localization.AppStrings
import com.redbus.ai.ui.viewmodel.RedbusUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

enum class RealtimeTripStatus(val label: String, val badgeColor: Color, val textColor: Color) {
    CONFIRMED("CONFIRMED", Color(0xFFDCFCE7), Color(0xFF15803D)),
    TRIP_STARTED("Trip Started · On the way", Color(0xFFEFF6FF), Color(0xFF1D4ED8)),
    TRIP_COMPLETED("Trip Completed", Color(0xFFDCFCE7), Color(0xFF15803D)),
    CANCELLED("CANCELLED", Color(0xFFFFE4E6), Color(0xFFBE123C)),
    REFUNDED("REFUNDED", Color(0xFFF1F5F9), Color(0xFF475569))
}

enum class BookingTabFilter(val label: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    PAST("Past Trips"),
    CANCELLED("Cancelled")
}

fun parseFlexibleTime(timeStr: String?): LocalTime? {
    if (timeStr.isNullOrBlank()) return null
    return try {
        val clean = timeStr.trim().uppercase(Locale.ENGLISH)
        if (clean.contains("AM") || clean.contains("PM")) {
            val isPm = clean.contains("PM")
            val digits = clean.replace(Regex("[^0-9:]"), "").split(":")
            var h = digits.getOrNull(0)?.toIntOrNull() ?: 12
            val m = digits.getOrNull(1)?.toIntOrNull() ?: 0
            if (isPm && h < 12) h += 12
            if (!isPm && h == 12) h = 0
            LocalTime.of(h, m)
        } else {
            val parts = clean.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            LocalTime.of(h, m)
        }
    } catch (_: Exception) {
        null
    }
}

fun calculateTripStatus(booking: BookingDetails): RealtimeTripStatus {
    if (booking.status.equals("CANCELLED", ignoreCase = true)) return RealtimeTripStatus.CANCELLED
    if (booking.status.equals("REFUNDED", ignoreCase = true)) return RealtimeTripStatus.REFUNDED

    return try {
        val now = LocalDateTime.now()
        val travelDate = LocalDate.parse(booking.travelDate.trim())

        val depTime = parseFlexibleTime(booking.departureTime) ?: LocalTime.of(12, 0)
        val depDateTime = travelDate.atTime(depTime)

        val arrTime = parseFlexibleTime(booking.arrivalTime) ?: depTime.plusHours(7)
        val arrDateTime = if (arrTime.isBefore(depTime) || arrTime == depTime) {
            travelDate.plusDays(1).atTime(arrTime)
        } else {
            travelDate.atTime(arrTime)
        }

        when {
            now.isBefore(depDateTime) -> RealtimeTripStatus.CONFIRMED
            now.isAfter(arrDateTime) -> RealtimeTripStatus.TRIP_COMPLETED
            else -> RealtimeTripStatus.TRIP_STARTED
        }
    } catch (_: Exception) {
        if (booking.status.equals("CONFIRMED", ignoreCase = true)) RealtimeTripStatus.CONFIRMED else RealtimeTripStatus.CANCELLED
    }
}

@Composable
fun MyBookingsScreen(
    uiState: RedbusUiState,
    onSearchPnr: (String) -> Unit,
    onRefreshBookings: () -> Unit,
    onCancelBooking: (String, String, String) -> Unit,
    onEmailTicket: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onSubmitReview: (Long, Int, String) -> Unit = { _, _, _ -> },
    onNavigateToAiHelp: () -> Unit = {}
) {
    var pnrQuery by remember { mutableStateOf("") }
    var activeTabFilter by remember { mutableStateOf(BookingTabFilter.ALL) }
    var bookingToCancel by remember { mutableStateOf<BookingDetails?>(null) }
    var selectedTicketToView by remember { mutableStateOf<BookingDetails?>(null) }
    var bookingToRate by remember { mutableStateOf<BookingDetails?>(null) }
    var cancelReason by remember { mutableStateOf("Change of plans") }
    var refundDestination by remember { mutableStateOf("WALLET") }

    val displayedBookings = remember(uiState.pnrSearchResult, uiState.myBookings) {
        if (uiState.pnrSearchResult != null) {
            listOf(uiState.pnrSearchResult)
        } else {
            uiState.myBookings
        }
    }

    val filteredBookings = remember(displayedBookings, activeTabFilter) {
        when (activeTabFilter) {
            BookingTabFilter.ALL -> displayedBookings
            BookingTabFilter.UPCOMING -> displayedBookings.filter {
                val s = calculateTripStatus(it)
                s == RealtimeTripStatus.CONFIRMED || s == RealtimeTripStatus.TRIP_STARTED
            }
            BookingTabFilter.PAST -> displayedBookings.filter {
                calculateTripStatus(it) == RealtimeTripStatus.TRIP_COMPLETED
            }
            BookingTabFilter.CANCELLED -> displayedBookings.filter {
                val s = calculateTripStatus(it)
                s == RealtimeTripStatus.CANCELLED || s == RealtimeTripStatus.REFUNDED
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        Surface(
            color = RedbusCrimson,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "My Bookings & PNR Lookup",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Tap any ticket to view perforated e-ticket & share",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onRefreshBookings) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Bookings",
                            tint = Color.White
                        )
                    }
                }

                // Live PNR Search Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = pnrQuery,
                        onValueChange = {
                            pnrQuery = it
                            if (it.isBlank()) onSearchPnr("")
                        },
                        placeholder = {
                            Text(
                                text = "Search any ticket by PNR number...",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            cursorColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onSearchPnr(pnrQuery) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = RedbusCrimson
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp)
                    ) {
                        Text("Find PNR", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                // Divided booking filter tabs: All | Upcoming trips | Past trips | Cancelled
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(BookingTabFilter.values()) { tab ->
                        val isSelected = activeTabFilter == tab
                        val count = when (tab) {
                            BookingTabFilter.ALL -> displayedBookings.size
                            BookingTabFilter.UPCOMING -> displayedBookings.count {
                                val s = calculateTripStatus(it)
                                s == RealtimeTripStatus.CONFIRMED || s == RealtimeTripStatus.TRIP_STARTED
                            }
                            BookingTabFilter.PAST -> displayedBookings.count {
                                calculateTripStatus(it) == RealtimeTripStatus.TRIP_COMPLETED
                            }
                            BookingTabFilter.CANCELLED -> displayedBookings.count {
                                val s = calculateTripStatus(it)
                                s == RealtimeTripStatus.CANCELLED || s == RealtimeTripStatus.REFUNDED
                            }
                        }
                        Surface(
                            onClick = { activeTabFilter = tab },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tab.label,
                                    color = if (isSelected) RedbusCrimson else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) RedbusCrimson.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "$count",
                                        color = if (isSelected) RedbusCrimson else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        uiState.bookingActionMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        if (uiState.isBookingsLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedbusCrimson)
            }
        } else if (!uiState.isAuthenticated && displayedBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = RedbusCrimson,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Sign In to View Your Bookings",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Sign in with your redBus AI account to see all your bus tickets, or search any ticket above using its PNR.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onNavigateToLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Text("Sign In to Account", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (filteredBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = when (activeTabFilter) {
                            BookingTabFilter.UPCOMING -> "No upcoming trips found"
                            BookingTabFilter.PAST -> "No past trips found"
                            BookingTabFilter.CANCELLED -> "No cancelled trips"
                            BookingTabFilter.ALL -> "No bookings found for your account yet"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Book a bus on Home or search by PNR above.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredBookings, key = { it.pnr.ifBlank { it.id.toString() } }) { booking ->
                    BookingTicketCard(
                        booking = booking,
                        onViewTicketClick = { selectedTicketToView = booking },
                        onCancelClick = { bookingToCancel = booking },
                        onEmailClick = { onEmailTicket(booking.pnr) },
                        onRateClick = { bookingToRate = booking }
                    )
                }
            }
        }
    }

        // Floating Circular AI Button (Matching Image 3)
        FloatingAiHelpButton(
            onClick = onNavigateToAiHelp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 18.dp)
        )
    }

    // View Ticket Modal matching Image 5 (media_1793296527384_86dfb532.jpg)
    selectedTicketToView?.let { ticketBooking ->
        ViewTicketDetailsModal(
            booking = ticketBooking,
            onDismiss = { selectedTicketToView = null },
            onCancelBooking = {
                selectedTicketToView = null
                bookingToCancel = ticketBooking
            }
        )
    }

    // Rate Journey Experience & Hygiene Review Modal
    bookingToRate?.let { b ->
        RateJourneyModal(
            booking = b,
            onDismiss = { bookingToRate = null },
            onSubmitReview = { busId, rating, comment ->
                onSubmitReview(busId, rating, comment)
            }
        )
    }

    if (bookingToCancel != null) {
        val b = bookingToCancel!!
        AlertDialog(
            onDismissRequest = { bookingToCancel = null },
            title = {
                Text("Cancel Ticket (PNR: ${b.pnr})", fontWeight = FontWeight.Black)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${b.sourceCity} ➔ ${b.destinationCity} (${b.operatorName})",
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Cancellation Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Refund Destination:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = refundDestination == "WALLET",
                            onClick = { refundDestination = "WALLET" },
                            label = { Text("Instant redBus Wallet") }
                        )
                        FilterChip(
                            selected = refundDestination == "ORIGINAL_PAYMENT",
                            onClick = { refundDestination = "ORIGINAL_PAYMENT" },
                            label = { Text("Original Source") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelBooking(b.pnr, cancelReason, refundDestination)
                        bookingToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Confirm Cancellation", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingToCancel = null }) {
                    Text("Keep Ticket")
                }
            }
        )
    }
}

@Composable
private fun BookingTicketCard(
    booking: BookingDetails,
    onViewTicketClick: () -> Unit,
    onCancelClick: () -> Unit,
    onEmailClick: () -> Unit,
    onRateClick: () -> Unit = {}
) {
    val tripStatus = calculateTripStatus(booking)
    val cardScale = remember { Animatable(0.95f) }
    val cardRotX = remember { Animatable(8f) }
    LaunchedEffect(booking.pnr) {
        cardScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }
    LaunchedEffect(booking.pnr) {
        cardRotX.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
    }

    Card(
        onClick = onViewTicketClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale.value
                scaleY = cardScale.value
                rotationX = cardRotX.value
                cameraDistance = 14f * density
            }
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = tripStatus.badgeColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "● ${tripStatus.label}",
                        color = tripStatus.textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "PNR: ${booking.pnr}",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = AiViolet
                )
            }

            Column {
                Text(
                    text = "${booking.sourceCity} ➔ ${booking.destinationCity}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${booking.operatorName} • ${booking.busType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Journey Date & Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${booking.travelDate} • ${booking.departureTime}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Passengers / Seats", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = booking.passengers.orEmpty().joinToString { "${it.name} (${it.seatNumber})" }
                            .ifBlank { "Confirmed Seat" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = RedbusCrimson
                    )
                }
            }

            Text(
                text = "Boarding: ${booking.boardingPoint} • Dropping: ${booking.droppingPoint}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", booking.totalAmount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    booking.refundStatus?.let { rs ->
                        Text(
                            text = "Refund: $rs",
                            fontSize = 11.sp,
                            color = RatingEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onViewTicketClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedbusCrimson,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = "View Ticket",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("View Ticket", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onEmailClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email E-Ticket",
                            tint = AiViolet,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    when (tripStatus) {
                        RealtimeTripStatus.CONFIRMED -> {
                            OutlinedButton(
                                onClick = onCancelClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        RealtimeTripStatus.TRIP_COMPLETED -> {
                            Button(
                                onClick = onRateClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDCFCE7),
                                    contentColor = Color(0xFF15803D)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rate Journey", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        RealtimeTripStatus.TRIP_STARTED -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFF93C5FD))
                            ) {
                                Text(
                                    text = "On the way",
                                    color = Color(0xFF1D4ED8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                        else -> { /* Cancelled or Refunded */ }
                    }
                }
            }
        }
    }
}

/**
 * Perforated View Ticket Popup Modal matching Image 5 (`media_1793296527384_86dfb532.jpg`).
 */
@Composable
private fun ViewTicketDetailsModal(
    booking: BookingDetails,
    onDismiss: () -> Unit,
    onCancelBooking: () -> Unit
) {
    val context = LocalContext.current
    val ticketNumber = remember(booking.id, booking.pnr) {
        if (booking.id > 0) "TVBS${12801000 + booking.id}" else "TVBS${booking.pnr.takeLast(8).uppercase()}"
    }

    val departureDateFormatted = remember(booking.travelDate) {
        formatTicketDate(booking.travelDate, plusDays = 0)
    }
    val arrivalDateFormatted = remember(booking.travelDate) {
        formatTicketDate(booking.travelDate, plusDays = 1)
    }
    val durationLabel = remember(booking.departureTime, booking.arrivalTime) {
        calculateDurationLabel(booking.departureTime, booking.arrivalTime)
    }
    val passengers = remember(booking.passengers) {
        booking.passengers.orEmpty().ifEmpty {
            listOf(
                com.redbus.ai.data.model.PassengerInput(
                    seatId = 1,
                    seatNumber = "L1",
                    name = "Passenger",
                    age = 25,
                    gender = "MALE"
                )
            )
        }
    }

    val ticketScale = remember { Animatable(0.94f) }
    LaunchedEffect(Unit) {
        ticketScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        val backdropColor = Color(0xFF141416)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor.copy(alpha = 0.94f))
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Circular Close Button (✕) matching Image 5
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = Color(0xFF2C2C2E),
                    modifier = Modifier
                        .size(38.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Ticket",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Scrollable Middle Ticket Card Container (Properly bounded so bottom Share button is ALWAYS completely visible)
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Main Perforated Ticket Card (Matching Image 5) with smooth entry animation
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = ticketScale.value
                                scaleY = ticketScale.value
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 1. Top Crimson Header (Source -> Duration -> Destination)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(RedbusCrimson)
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Vertical Timeline Column (↗ ─── ↘)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(end = 10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.NorthEast,
                                                    contentDescription = null,
                                                    tint = RedbusCrimson,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.5.dp)
                                                .height(58.dp)
                                                .background(Color.White.copy(alpha = 0.7f))
                                        )

                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.SouthEast,
                                                    contentDescription = null,
                                                    tint = RedbusCrimson,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Right Content Column (Departure, Duration Divider, Arrival)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Departure Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = booking.sourceCity,
                                                    color = Color.White,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = booking.boardingPoint.ifBlank { "${booking.sourceCity} Bypass" },
                                                    color = Color.White.copy(alpha = 0.88f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = booking.departureTime,
                                                    color = Color.White,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = departureDateFormatted,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        // Center Duration Line: ──── 🚌 7h 10 min ────
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(1.dp)
                                                    .background(Color.White.copy(alpha = 0.35f))
                                            )
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsBus,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = durationLabel,
                                                    color = Color.White.copy(alpha = 0.95f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(1.dp)
                                                    .background(Color.White.copy(alpha = 0.35f))
                                            )
                                        }

                                        // Arrival Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = booking.destinationCity,
                                                    color = Color.White,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = booking.droppingPoint.ifBlank { booking.destinationCity },
                                                    color = Color.White.copy(alpha = 0.88f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = booking.arrivalTime,
                                                    color = Color.White,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = arrivalDateFormatted,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. White Operator & Bus Type Section
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = booking.operatorName,
                                    color = Color(0xFF111827),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${booking.busType} · ${passengers.size} ${if (passengers.size == 1) "Seat" else "Seats"}",
                                    color = Color(0xFF6B7280),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }

                            DashedTicketDivider()

                            // 3. Passenger List Section (Name + (F)/(M), Age, Seat Pill Badge)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                passengers.forEach { p ->
                                    val genderShort = if (p.gender.startsWith("F", ignoreCase = true)) "F" else "M"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = p.name,
                                                    color = Color(0xFF111827),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "($genderShort)",
                                                    color = Color(0xFF6B7280),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = "(${p.age} yrs)",
                                                color = Color(0xFF6B7280),
                                                fontSize = 12.sp
                                            )
                                        }

                                        // Blue-tinted Seat Number Pill Badge (Matching Image 5 "1F")
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEEF2FF)
                                        ) {
                                            Text(
                                                text = p.seatNumber,
                                                color = Color(0xFF1E3A8A),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Semicircular Side Cutout Notches + Dashed Perforation Line (Image 5)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                DashedTicketDivider()

                                // Left semicircular cutout notch
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = (-12).dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(backdropColor)
                                )

                                // Right semicircular cutout notch
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 12.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(backdropColor)
                                )
                            }

                            // 5. Ticket #, PNR #, and Fare Footer (Matching Image 5)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Ticket # Row with Copy Icon
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ticket #",
                                        color = Color(0xFF6B7280),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.clickable {
                                            val clipboard =
                                                context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clipboard?.setPrimaryClip(
                                                ClipData.newPlainText(
                                                    "Ticket Number",
                                                    ticketNumber
                                                )
                                            )
                                            Toast.makeText(context, "Ticket #$ticketNumber copied", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Ticket Number",
                                            tint = Color(0xFF111827),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = ticketNumber,
                                            color = Color(0xFF111827),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // PNR # Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PNR #",
                                        color = Color(0xFF6B7280),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = booking.pnr,
                                        color = Color(0xFF111827),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Fare Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Fare",
                                        color = Color(0xFF6B7280),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.US, "%.2f", booking.totalAmount)}",
                                        color = Color(0xFF111827),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Full-Width Red Pill Button: "Share your ticket" (Always Pinned & Fully Visible)
                Button(
                    onClick = {
                        val shareText = buildString {
                            appendLine("🚌 redBus AI Confirmed E-Ticket")
                            appendLine("Ticket #: $ticketNumber")
                            appendLine("PNR #: ${booking.pnr}")
                            appendLine("Route: ${booking.sourceCity} ➔ ${booking.destinationCity}")
                            appendLine("Operator: ${booking.operatorName} (${booking.busType})")
                            appendLine("Departure: ${booking.departureTime}, $departureDateFormatted (${booking.boardingPoint})")
                            appendLine("Arrival: ${booking.arrivalTime}, $arrivalDateFormatted (${booking.droppingPoint})")
                            appendLine("Passengers: ${passengers.joinToString { "${it.name} - Seat ${it.seatNumber}" }}")
                            appendLine("Total Fare: ₹${String.format(Locale.US, "%.2f", booking.totalAmount)}")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "redBus E-Ticket PNR ${booking.pnr}")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share your ticket"))
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedbusCrimson,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = AppStrings.get("share_ticket", uiState.selectedLanguage),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RateJourneyModal(
    booking: BookingDetails,
    onDismiss: () -> Unit,
    onSubmitReview: (Long, Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var cleanlinessRating by remember { mutableIntStateOf(5) }
    var punctualityRating by remember { mutableIntStateOf(5) }
    var staffRating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }

    val hygieneTags = listOf(
        "Sanitized Bus",
        "Clean Blankets",
        "Deep Cleaned Seats",
        "AC Filter Clean",
        "Punctual Departure",
        "Polite Crew",
        "Safe Driving",
        "Clean Rest Stop"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .systemBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with Close Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rate Your Journey Experience",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${booking.sourceCity} ➔ ${booking.destinationCity} • ${booking.operatorName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    // 1. Overall Star Rating
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when (rating) {
                                5 -> "⭐ Excellent Journey"
                                4 -> "⭐ Very Good"
                                3 -> "⭐ Good"
                                2 -> "⭐ Fair"
                                else -> "⭐ Needs Improvement"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = RatingAmber
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (star in 1..5) {
                                Icon(
                                    imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star stars",
                                    tint = if (star <= rating) RatingAmber else Color.Gray.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { rating = star }
                                )
                            }
                        }
                    }

                    // 2. Hygiene & Service Breakdown
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Hygiene & Service Quality", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            // Bus Cleanliness & Hygiene
                            RatingAspectRow(
                                title = "Bus Hygiene & Cleanliness",
                                rating = cleanlinessRating,
                                onRatingChange = { cleanlinessRating = it }
                            )

                            // Punctuality
                            RatingAspectRow(
                                title = "Punctuality & Timeliness",
                                rating = punctualityRating,
                                onRatingChange = { punctualityRating = it }
                            )

                            // Staff & Driver Behavior
                            RatingAspectRow(
                                title = "Staff & Driver Behavior",
                                rating = staffRating,
                                onRatingChange = { staffRating = it }
                            )
                        }
                    }

                    // 3. Experience & Hygiene Tags
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("What went well?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            hygieneTags.forEach { tag ->
                                val isSelected = selectedTags.contains(tag)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                                    },
                                    label = { Text(tag, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RedbusCrimson.copy(alpha = 0.15f),
                                        selectedLabelColor = RedbusCrimson
                                    )
                                )
                            }
                        }
                    }

                    // 4. Feedback comment
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Write your review (hygiene, comfort, route)") },
                        placeholder = { Text("Share feedback about hygiene, blankets, driving quality...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 5. Submit Button
                    Button(
                        onClick = {
                            val targetBusId = if (booking.routeId > 0) booking.routeId.toLong() else booking.id.toLong()
                            val fullComment = buildString {
                                if (comment.isNotBlank()) append(comment.trim())
                                if (selectedTags.isNotEmpty()) {
                                    if (isNotEmpty()) append(" | ")
                                    append("Tags: ")
                                    append(selectedTags.joinToString(", "))
                                }
                                append(" (Hygiene: $cleanlinessRating/5, Punctuality: $punctualityRating/5, Staff: $staffRating/5)")
                            }
                            onSubmitReview(targetBusId, rating, fullComment)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Submit Review", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingAspectRow(
    title: String,
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 1..5) {
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (i <= rating) RatingAmber else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onRatingChange(i) }
                )
            }
        }
    }
}

@Composable
private fun DashedTicketDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
    ) {
        drawLine(
            color = Color(0xFFD1D5DB),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        )
    }
}

private fun formatTicketDate(dateStr: String, plusDays: Long = 0): String {
    return try {
        val date = LocalDate.parse(dateStr).plusDays(plusDays)
        val weekday = date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
        val month = date.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
        "$weekday, ${date.dayOfMonth} $month"
    } catch (_: Exception) {
        dateStr
    }
}

private fun calculateDurationLabel(dep: String, arr: String): String {
    return try {
        fun toMins(t: String): Int {
            val clean = t.trim().uppercase()
            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")
            val parts = clean.replace(Regex("[^0-9:]"), "").split(":")
            var h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            if (isPm && h < 12) h += 12
            if (isAm && h == 12) h = 0
            return h * 60 + m
        }

        var diff = toMins(arr) - toMins(dep)
        if (diff <= 0) diff += 24 * 60
        val hrs = diff / 60
        val mins = diff % 60
        if (mins > 0) "${hrs}h ${mins} min" else "${hrs}h 00 min"
    } catch (_: Exception) {
        "7h 10 min"
    }
}

@Composable
fun ProfileAndWalletScreen(
    uiState: RedbusUiState,
    onLoginWithEmail: (String, String) -> Unit,
    onRegisterAccount: (String, String, String, String) -> Unit,
    onRequestMobileOtp: (String) -> Unit,
    onVerifyMobileOtp: (String, String) -> Unit,
    onUpdateProfile: (String, String, String?) -> Unit,
    onAddTraveller: (String, Int, String) -> Unit,
    onDeleteTraveller: (Int) -> Unit,
    onRefreshAccount: () -> Unit,
    onLogout: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    onUpdateAvatar: (String) -> Unit = {},
    onChangePassword: (String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    onSendResetOtp: ((Boolean, String) -> Unit) -> Unit = {},
    onTopUpWallet: (Double) -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onFetchDeviceSessions: () -> Unit = {},
    onRevokeDeviceSession: (Long) -> Unit = {},
    onRevokeAllOtherSessions: () -> Unit = {},
    onDeactivateAccount: () -> Unit = {},
    onDeleteAccount: (String) -> Unit = {},
    onNavigateToBookings: () -> Unit = {},
    onSelectLanguage: (String) -> Unit = {},
    onToggleBookingForWomen: (Boolean) -> Unit = {}
) {
    if (!uiState.isAuthenticated || uiState.user == null) {
        LiveAuthScreen(
            uiState = uiState,
            onLoginWithEmail = onLoginWithEmail,
            onRegisterAccount = onRegisterAccount,
            onRequestMobileOtp = onRequestMobileOtp,
            onVerifyMobileOtp = onVerifyMobileOtp,
            onToggleDarkTheme = onToggleDarkTheme
        )
        return
    }

    LaunchedEffect(Unit) {
        onFetchDeviceSessions()
    }

    val user = uiState.user

    // Modals & Sheets Visibility States
    var showPersonalInformationModal by remember { mutableStateOf(false) }
    var showPassengersModal by remember { mutableStateOf(false) }
    var showHelpFaqModal by remember { mutableStateOf(false) }
    var showAccountSettingsModal by remember { mutableStateOf(false) }
    var showLanguageSelectionDialog by remember { mutableStateOf(false) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var showLogoutConfirmModal by remember { mutableStateOf(false) }
    var showAvatarModal by remember { mutableStateOf(false) }
    var showDeactivateModal by remember { mutableStateOf(false) }
    var showDeleteModal by remember { mutableStateOf(false) }
    var showBookingForWomenDialog by remember { mutableStateOf(false) }
    var showAddTravellerDialog by remember { mutableStateOf(false) }
    var editingTravellerId by remember { mutableStateOf<Int?>(null) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var deletePassword by remember { mutableStateOf("") }

    // Edit Profile fields
    var editName by remember(user.name) { mutableStateOf(user.name) }
    var editPhone by remember(user.phone) { mutableStateOf(user.phone.orEmpty()) }
    var editGender by remember(user.gender) { mutableStateOf(user.gender ?: "MALE") }

    // Saved Traveller form fields
    var newTravellerName by remember { mutableStateOf("") }
    var newTravellerAge by remember { mutableStateOf("25") }
    var newTravellerGender by remember { mutableStateOf("MALE") }
    var showEditTravellerDialog by remember { mutableStateOf(false) }
    var editingTravellerName by remember { mutableStateOf("") }
    var editingTravellerAge by remember { mutableStateOf("25") }
    var editingTravellerGender by remember { mutableStateOf("MALE") }

    // Security & Password fields (matching website settings/page.tsx)
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showCurrentPass by remember { mutableStateOf(false) }
    var showNewPass by remember { mutableStateOf(false) }
    var showConfirmPass by remember { mutableStateOf(false) }
    var passwordChangeMessage by remember { mutableStateOf<String?>(null) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var otpMessage by remember { mutableStateOf<String?>(null) }

    val initials = remember(user.name) {
        user.name.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifBlank { "U" }
    }

    val supportedLanguages = listOf(
        "English" to "English",
        "தமிழ் (Tamil)" to "தமிழ் (Tamil)",
        "हिंदी (Hindi)" to "हिंदी (Hindi)",
        "ಕನ್ನಡ (Kannada)" to "ಕನ್ನಡ (Kannada)",
        "తెలుగు (Telugu)" to "తెలుగు (Telugu)",
        "മലയാളം (Malayalam)" to "മലയാളം (Malayalam)"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Notification banner for profile actions
        uiState.profileActionMessage?.let { msg ->
            item {
                Surface(
                    color = RatingEmeraldBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ $msg",
                        color = RatingEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // 1. Live User Account Header (Avatar, Email, Phone, Role)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = RedbusCrimson),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clickable { showAvatarModal = true }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (!user.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = user.avatarUrl,
                                            contentDescription = user.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = initials,
                                                color = RedbusCrimson,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 22.sp
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change avatar",
                                        tint = Color.White,
                                        modifier = Modifier.padding(3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = user.name,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = user.email,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp
                                )
                                if (!user.phone.isNullOrBlank()) {
                                    Text(
                                        text = user.phone,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        IconButton(onClick = onRefreshAccount) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Account",
                                tint = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = when (user.role) {
                                        "ROLE_ADMIN" -> "🛡️ Administrator"
                                        "ROLE_OPERATOR" -> "🚍 Bus Operator"
                                        else -> "👤 Passenger"
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (user.emailVerified == true) "✓ Verified" else "⚠ Unverified",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        TextButton(
                            onClick = { showPersonalInformationModal = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. "Try redBus in your language" Strip (Matching Image 2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = AppStrings.get("try_redbus_in_your_language", uiState.selectedLanguage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(supportedLanguages) { (key, label) ->
                        val isSelected = uiState.selectedLanguage.equals(key, ignoreCase = true) ||
                                (key == "English" && uiState.selectedLanguage.isBlank())
                        Surface(
                            onClick = { onSelectLanguage(key) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) RedbusCrimson else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) RedbusCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            ),
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Section: "My details" (Matching Image 2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = AppStrings.get("my_details", uiState.selectedLanguage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // 1. Bookings Button
                        AccountMenuRowItem(
                            icon = Icons.Default.ReceiptLong,
                            title = "Bookings",
                            onClick = onNavigateToBookings
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // 2. Personal information Button
                        AccountMenuRowItem(
                            icon = Icons.Default.Person,
                            title = "Personal information",
                            onClick = { showPersonalInformationModal = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // 3. Passengers Button
                        AccountMenuRowItem(
                            icon = Icons.Default.Group,
                            title = "Passengers",
                            trailingBadge = if (uiState.savedTravellers.isNotEmpty()) "${uiState.savedTravellers.size}" else null,
                            onClick = { showPassengersModal = true }
                        )
                    }
                }
            }
        }

        // 4. Section: "Payments" (Matching Image 3)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = AppStrings.get("payments", uiState.selectedLanguage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AccountMenuRowItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = RatingEmerald,
                        title = "redBus Wallet",
                        trailingBadge = "₹${String.format(Locale.US, "%.2f", user.walletBalance ?: 0.0)}",
                        onClick = onOpenWallet
                    )
                }
            }
        }

        // 5. Section: "More" / Help & Settings (Matching Image 3 & 5)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = AppStrings.get("more", uiState.selectedLanguage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Help (FAQs from www.redbusai.app)
                        AccountMenuRowItem(
                            icon = Icons.Default.HelpOutline,
                            title = "Help",
                            onClick = { showHelpFaqModal = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Account settings (from https://www.redbusai.app/settings)
                        AccountMenuRowItem(
                            icon = Icons.Default.Settings,
                            title = "Account settings",
                            onClick = { showAccountSettingsModal = true }
                        )
                    }
                }
            }
        }

        // 6. Section: "Preferences" (Matching Image 5)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Preferences",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Language
                        AccountMenuRowItem(
                            icon = Icons.Default.Translate,
                            title = "Language",
                            trailingBadge = uiState.selectedLanguage.ifBlank { "English" },
                            onClick = { showLanguageSelectionDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Appearance (Light / Dark theme switch)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleDarkTheme() }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = RedbusCrimson.copy(alpha = 0.1f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (uiState.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                            contentDescription = null,
                                            tint = RedbusCrimson,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Appearance",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Choose light or dark theme",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            RedbusStyledSwitch(
                                checked = uiState.isDarkTheme,
                                onCheckedChange = { onToggleDarkTheme() }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Notifications
                        AccountMenuRowItem(
                            icon = Icons.Default.Notifications,
                            title = "Notifications",
                            onClick = { showNotificationsModal = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Booking for women (Image 4)
                        AccountMenuRowItem(
                            icon = Icons.Default.Face,
                            iconTint = FemaleSeatPink,
                            title = "Booking for women",
                            trailingBadge = if (uiState.isBookingForWomen) "Yes" else "No",
                            onClick = { showBookingForWomenDialog = true }
                        )
                    }
                }
            }
        }

        // 7. Sign Out & Version footer (Matching Image 4 & 5)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { showLogoutConfirmModal = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of redBus AI", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }

                Text(
                    text = "V82.7.2",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // 1. Modal: Personal Information
    if (showPersonalInformationModal) {
        Dialog(
            onDismissRequest = { showPersonalInformationModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Personal Information",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Manage your verified profile details",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showPersonalInformationModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider()

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RedbusCrimson) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = RedbusCrimson) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = user.email,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Email Address (Verified)") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = RatingEmerald) },
                            trailingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RatingEmerald) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Gender",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("MALE", "FEMALE", "OTHER").forEach { g ->
                                    FilterChip(
                                        selected = editGender.equals(g, ignoreCase = true),
                                        onClick = { editGender = g },
                                        label = { Text(g) }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onUpdateProfile(editName, editPhone, editGender)
                                showPersonalInformationModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Profile", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    // 2. Modal: Passengers (Saved Travellers with Edit & Delete)
    if (showPassengersModal) {
        Dialog(
            onDismissRequest = { showPassengersModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Passengers (${uiState.savedTravellers.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Saved travellers for 1-tap booking",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showPassengersModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        if (uiState.savedTravellers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Text(
                                        text = "No saved passengers yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Add travellers below for faster checkout",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.savedTravellers, key = { it.id }) { traveller ->
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = RedbusCrimson.copy(alpha = 0.12f),
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = RedbusCrimson,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = traveller.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    )
                                                    Text(
                                                        text = "${traveller.age} yrs • ${traveller.gender}",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        editingTravellerId = traveller.id
                                                        editingTravellerName = traveller.name
                                                        editingTravellerAge = traveller.age.toString()
                                                        editingTravellerGender = traveller.gender
                                                        showEditTravellerDialog = true
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = RedbusBlue,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onDeleteTraveller(traveller.id) }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = RedbusCrimson,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showAddTravellerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+ Add Passenger", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    // 3. Modal: Help & FAQs (From www.redbusai.app/faq)
    if (showHelpFaqModal) {
        val faqs = listOf(
            "Cancellation & Instant Refund Policy" to
                "You can cancel your bus ticket anytime before departure directly from the Bookings tab. Refund slabs:\n• More than 24 hours before departure: 100% refund.\n• 12 to 24 hours before departure: 50% refund.\n• Less than 12 hours before departure: No refund.\nRefunds credited to redBus Wallet are Instant. Bank or card refunds take 3-5 business days.",
            "Luggage & Baggage Guidelines" to
                "Each passenger is entitled to carry up to 15 kg of personal luggage (e.g. 1 standard trolley bag and 1 laptop/handbag) free of charge in the bus luggage boot. Excess luggage or commercial cargo may incur additional fees charged by the operator at boarding.",
            "Boarding & Photo ID Rules" to
                "Passengers must present the digital mTicket (SMS, WhatsApp, or in-app ticket) along with a valid Government-issued photo ID (Aadhaar Card, Driving License, Voter ID, or Passport) to the bus conductor/driver prior to boarding.",
            "Live Bus GPS Tracking" to
                "Live GPS bus tracking activates 60 minutes before scheduled departure time. Tap the 'Track Bus' button on your ticket to see the real-time location, bus speed, driver contact details, and distance to your boarding point.",
            "Infant & Child Travel Policy" to
                "Children below 5 years of age travel free of charge with their parent or guardian without occupying an individual seat. Children aged 5 and above require a separate confirmed ticket reservation.",
            "Operator Fleet & Verified Partners" to
                "All bus operators on redBus AI are strictly verified under Primo standards, ensuring 24/7 emergency support, onboard safety amenities, punctuality tracking, and sanitized interiors."
        )

        var expandedIndex by remember { mutableStateOf<Int?>(0) }

        Dialog(
            onDismissRequest = { showHelpFaqModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = RedbusCrimson.copy(alpha = 0.12f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.HelpOutline,
                                            contentDescription = null,
                                            tint = RedbusCrimson,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Help & FAQs",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Official redBus AI policies & support",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { showHelpFaqModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(faqs.indices.toList()) { idx ->
                                val (question, answer) = faqs[idx]
                                val isExpanded = expandedIndex == idx
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedIndex = if (isExpanded) null else idx }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = question,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isExpanded) RedbusCrimson else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = answer,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HeadsetMic,
                                    contentDescription = null,
                                    tint = RatingEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "24x7 Customer Support",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "support@redbusai.app • 1800-419-4287",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Modal: Account Settings (Matching https://www.redbusai.app/settings & Image 4)
    if (showAccountSettingsModal) {
        Dialog(
            onDismissRequest = { showAccountSettingsModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = RedbusCrimson.copy(alpha = 0.12f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = RedbusCrimson,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Account Settings",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "redbusai.app/settings",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { showAccountSettingsModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // A. Profile Avatar Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Profile Avatar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = RedbusCrimson,
                                            modifier = Modifier.size(54.dp)
                                        ) {
                                            if (!user.avatarUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = user.avatarUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                                }
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = { showAvatarModal = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Change Avatar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = "Select from presets or reset to initials",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // B. Personal Information Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text("Full Name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = editPhone,
                                        onValueChange = { editPhone = it },
                                        label = { Text("Phone Number") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("MALE", "FEMALE", "OTHER").forEach { g ->
                                            FilterChip(
                                                selected = editGender.equals(g, ignoreCase = true),
                                                onClick = { editGender = g },
                                                label = { Text(g) }
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onUpdateProfile(editName, editPhone, editGender)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save Personal Info", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // C. Security & Password Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Security & Password", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                    passwordChangeMessage?.let { msg ->
                                        Text(msg, fontSize = 12.sp, color = if (msg.contains("success", ignoreCase = true)) RatingEmerald else RedbusCrimson)
                                    }

                                    OutlinedTextField(
                                        value = currentPass,
                                        onValueChange = { currentPass = it },
                                        label = { Text("Current Password") },
                                        singleLine = true,
                                        visualTransformation = if (showCurrentPass) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showCurrentPass = !showCurrentPass }) {
                                                Icon(if (showCurrentPass) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = newPass,
                                        onValueChange = { newPass = it },
                                        label = { Text("New Password (min 6 characters)") },
                                        singleLine = true,
                                        visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showNewPass = !showNewPass }) {
                                                Icon(if (showNewPass) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = confirmPass,
                                        onValueChange = { confirmPass = it },
                                        label = { Text("Confirm New Password") },
                                        singleLine = true,
                                        visualTransformation = if (showConfirmPass) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showConfirmPass = !showConfirmPass }) {
                                                Icon(if (showConfirmPass) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            if (newPass.length < 6) {
                                                passwordChangeMessage = "New password must be at least 6 characters"
                                                return@Button
                                            }
                                            if (newPass != confirmPass) {
                                                passwordChangeMessage = "Passwords do not match"
                                                return@Button
                                            }
                                            onChangePassword(currentPass, newPass) { success, msg ->
                                                passwordChangeMessage = msg
                                                if (success) {
                                                    currentPass = ""
                                                    newPass = ""
                                                    confirmPass = ""
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Update Password", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // D. Logged-In Devices & Security Sessions (Matching Image 4)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Logged-In Devices & Security Sessions",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Active Sessions (${if (uiState.deviceSessions.isNotEmpty()) uiState.deviceSessions.size else 4})",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val sessionItems = if (uiState.deviceSessions.isNotEmpty()) {
                                        uiState.deviceSessions.map { s ->
                                            Triple(s.id, s.deviceName, s.isCurrent ?: false)
                                        }
                                    } else {
                                        listOf(
                                            Triple(1L, "Standard Web Browser on Desktop/Mobile OS", true),
                                            Triple(2L, "Google Chrome on Android", false),
                                            Triple(3L, "Apple Safari on iOS", false),
                                            Triple(4L, "Google Chrome on Windows 10/11", false)
                                        )
                                    }

                                    sessionItems.forEach { (id, name, isCurrent) ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = if (name.contains("Android", ignoreCase = true) || name.contains("iOS", ignoreCase = true)) Icons.Default.PhoneAndroid else Icons.Default.Computer,
                                                        contentDescription = null,
                                                        tint = if (isCurrent) RatingEmerald else RedbusCrimson,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = name,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 13.sp
                                                        )
                                                        Text(
                                                            text = if (isCurrent) "Current Active Session" else "Authorized Device",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                if (isCurrent) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = RatingEmerald.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "Active Now",
                                                            color = RatingEmerald,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = { onRevokeDeviceSession(id) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("Log Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onRevokeAllOtherSessions() },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Log Out All Other Devices", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }

                            // E. Danger Zone (Matching Image 4)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = RedbusCrimson.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, RedbusCrimson.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = RedbusCrimson)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Danger Zone", fontWeight = FontWeight.Black, fontSize = 15.sp, color = RedbusCrimson)
                                    }
                                    Text(
                                        text = "Deactivate account temporarily or permanently remove your profile data.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                showAccountSettingsModal = false
                                                showDeactivateModal = true
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Deactivate Account", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                showAccountSettingsModal = false
                                                showDeleteModal = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Delete Permanently", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    // 5. Modal: Notifications (Matching website navbar NotificationDropdown.tsx)
    if (showNotificationsModal) {
        var notifTab by remember { mutableStateOf("ALL") }

        Dialog(
            onDismissRequest = { showNotificationsModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = RedbusCrimson.copy(alpha = 0.12f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = RedbusCrimson,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Notifications",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Live updates & alerts",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { showNotificationsModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category tabs: ALL, BOOKING, OFFER, ALERT
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("ALL", "BOOKING", "OFFER", "ALERT").forEach { tabKey ->
                                val isTabSelected = notifTab == tabKey
                                FilterChip(
                                    selected = isTabSelected,
                                    onClick = { notifTab = tabKey },
                                    label = {
                                        Text(
                                            when (tabKey) {
                                                "BOOKING" -> "Bookings"
                                                "OFFER" -> "Offers"
                                                "ALERT" -> "Alerts"
                                                else -> "All"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        val mockNotifications = listOf(
                            Triple("BOOKING", "Live Boarding Today! 📍", "Your bus Intrcity SmartBus departs today. Track bus live GPS on your ticket."),
                            Triple("OFFER", "Festive Promo LUXURY20 Active 🎉", "Save up to ₹200 on all Primo AC Sleeper routes with promo code LUXURY20."),
                            Triple("ALERT", "redBus Wallet Credit Synced 💳", "Instant refund & checkout credit of ₹${String.format(Locale.US, "%.2f", user.walletBalance ?: 0.0)} is ready to use."),
                            Triple("OFFER", "Welcome Coupon FIRSTBUS", "Get flat 15% discount on your next bus journey booking."),
                            Triple("ALERT", "Primo Safety Standard Verified 🛡️", "All partner fleets sanitized and equipped with verified emergency contact tools.")
                        )

                        val filteredNotifs = mockNotifications.filter {
                            if (notifTab == "ALL") true else it.first == notifTab
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredNotifs) { (cat, title, body) ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = when (cat) {
                                                "BOOKING" -> RedbusBlue.copy(alpha = 0.15f)
                                                "OFFER" -> RatingAmber.copy(alpha = 0.15f)
                                                else -> RatingEmerald.copy(alpha = 0.15f)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = when (cat) {
                                                        "BOOKING" -> Icons.Default.ConfirmationNumber
                                                        "OFFER" -> Icons.Default.LocalOffer
                                                        else -> Icons.Default.NotificationsActive
                                                    },
                                                    contentDescription = null,
                                                    tint = when (cat) {
                                                        "BOOKING" -> RedbusBlue
                                                        "OFFER" -> RatingAmber
                                                        else -> RatingEmerald
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    // 6. Dialog: Select Language (Image 5 & 2)
    if (showLanguageSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageSelectionDialog = false },
            title = {
                Text(
                    text = "Select Language",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    supportedLanguages.forEach { (key, label) ->
                        val isSelected = uiState.selectedLanguage.equals(key, ignoreCase = true) ||
                                (key == "English" && uiState.selectedLanguage.isBlank())
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) RedbusCrimson.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) RedbusCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectLanguage(key)
                                    showLanguageSelectionDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) RedbusCrimson else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = RedbusCrimson, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageSelectionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Modal: Avatar Selector
    if (showAvatarModal) {
        val sampleAvatars = listOf(
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&h=200&fit=crop"
        )
        AlertDialog(
            onDismissRequest = { showAvatarModal = false },
            title = { Text("Choose Avatar", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Pick a passenger profile avatar:", fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        sampleAvatars.forEach { url ->
                            Surface(
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clickable {
                                        onUpdateAvatar(url)
                                        showAvatarModal = false
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            onUpdateAvatar("")
                            showAvatarModal = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset to Default Initials")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAvatarModal = false }) { Text("Close") }
            }
        )
    }

    // Modal: Deactivate Account
    if (showDeactivateModal) {
        AlertDialog(
            onDismissRequest = { showDeactivateModal = false },
            title = { Text("Deactivate Account?", fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "Your account will be suspended. You can reactivate anytime by logging in again.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeactivateModal = false
                        onDeactivateAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Confirm Deactivate", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateModal = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: Delete Account Permanently
    if (showDeleteModal) {
        AlertDialog(
            onDismissRequest = { showDeleteModal = false },
            title = { Text("Delete Account Permanently", fontWeight = FontWeight.Black, color = RedbusCrimson) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This action is permanent and cannot be undone. All your bookings and wallet credit will be erased.",
                        fontSize = 13.sp
                    )
                    Text("Type DELETE to confirm:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        singleLine = true,
                        placeholder = { Text("DELETE") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteConfirmText.trim().equals("DELETE", ignoreCase = false)) {
                            showDeleteModal = false
                            onDeleteAccount(deletePassword)
                        }
                    },
                    enabled = deleteConfirmText.trim() == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Permanently Delete", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteModal = false }) { Text("Cancel") }
            }
        )
    }

    if (showLogoutConfirmModal) {
        val modalScale = remember { Animatable(0.85f) }
        val modalRotX = remember { Animatable(-14f) }
        LaunchedEffect(Unit) {
            modalScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
        LaunchedEffect(Unit) {
            modalRotX.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }

        Dialog(
            onDismissRequest = { showLogoutConfirmModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = modalScale.value
                            scaleY = modalScale.value
                            rotationX = modalRotX.value
                            cameraDistance = 14f * density
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RedbusSoftTint,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    tint = RedbusCrimson,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Text(
                            text = "Sign Out Confirmation",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = "Are you sure you want to sign out of ${user.email}? Your wallet balance, saved travellers, and bookings will stay synced when you log back in.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showLogoutConfirmModal = false },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 13.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Button(
                                onClick = {
                                    showLogoutConfirmModal = false
                                    onLogout()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RedbusCrimson,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(vertical = 13.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Yes, Sign Out",
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Edit Saved Passenger
    if (showEditTravellerDialog) {
        AlertDialog(
            onDismissRequest = { showEditTravellerDialog = false },
            title = { Text("Edit Passenger Details", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editingTravellerName,
                        onValueChange = { editingTravellerName = it },
                        label = { Text("Passenger Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editingTravellerAge,
                        onValueChange = { editingTravellerAge = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Age") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Gender", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MALE", "FEMALE", "OTHER").forEach { g ->
                                FilterChip(
                                    selected = editingTravellerGender.equals(g, ignoreCase = true),
                                    onClick = { editingTravellerGender = g },
                                    label = { Text(g) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ageInt = editingTravellerAge.toIntOrNull() ?: 25
                        if (editingTravellerName.isNotBlank() && editingTravellerId != null) {
                            onDeleteTraveller(editingTravellerId!!)
                            onAddTraveller(editingTravellerName, ageInt, editingTravellerGender)
                            showEditTravellerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTravellerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Add Saved Passenger
    if (showAddTravellerDialog) {
        AlertDialog(
            onDismissRequest = { showAddTravellerDialog = false },
            title = { Text("Add Passenger", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTravellerName,
                        onValueChange = { newTravellerName = it },
                        label = { Text("Passenger Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTravellerAge,
                        onValueChange = { newTravellerAge = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Age") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Gender", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MALE", "FEMALE", "OTHER").forEach { g ->
                                FilterChip(
                                    selected = newTravellerGender.equals(g, ignoreCase = true),
                                    onClick = { newTravellerGender = g },
                                    label = { Text(g) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ageInt = newTravellerAge.toIntOrNull() ?: 25
                        if (newTravellerName.isNotBlank()) {
                            onAddTraveller(newTravellerName, ageInt, newTravellerGender)
                            newTravellerName = ""
                            showAddTravellerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Save Passenger", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTravellerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Booking for Women Preference Dialog (Image 4)
    if (showBookingForWomenDialog) {
        AlertDialog(
            onDismissRequest = { showBookingForWomenDialog = false },
            title = {
                Text(
                    text = "Booking for women",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Book for women? When enabled, redBus AI ensures booking is reserved exclusively for female passengers.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.isBookingForWomen) FemaleSeatPink.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (uiState.isBookingForWomen) FemaleSeatPink else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleBookingForWomen(true)
                                showBookingForWomenDialog = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = FemaleSeatPink,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Yes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Only women can book tickets", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (uiState.isBookingForWomen) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = FemaleSeatPink)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!uiState.isBookingForWomen) RedbusCrimson.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (!uiState.isBookingForWomen) RedbusCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleBookingForWomen(false)
                                showBookingForWomenDialog = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("No", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Standard all-passenger booking", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!uiState.isBookingForWomen) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = RedbusCrimson)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBookingForWomenDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Reusable Account Menu Row Item matching redBus Account Tab Design (Image 2, 3, 5).
 */
@Composable
fun AccountMenuRowItem(
    icon: ImageVector,
    title: String,
    iconTint: Color = RedbusCrimson,
    trailingBadge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.1f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!trailingBadge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconTint.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = trailingBadge,
                        color = iconTint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Live Authentication Screen with Visibility Eye Toggle Buttons on Login Password,
 * Mobile OTP, and Sign Up Password inputs (Matching Image 3 requirement).
 */
@Composable
private fun LiveAuthScreen(
    uiState: RedbusUiState,
    onLoginWithEmail: (String, String) -> Unit,
    onRegisterAccount: (String, String, String, String) -> Unit,
    onRequestMobileOtp: (String) -> Unit,
    onVerifyMobileOtp: (String, String) -> Unit,
    onToggleDarkTheme: () -> Unit
) {
    var authTab by remember { mutableIntStateOf(0) } // 0 = Email Login, 1 = Mobile OTP, 2 = Sign Up
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    // Eye Toggle Visibility States for Password & OTP Inputs
    var isLoginPasswordVisible by remember { mutableStateOf(false) }
    var isOtpVisible by remember { mutableStateOf(false) }
    var isRegisterPasswordVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "redBus AI Account",
                                style = MaterialTheme.typography.headlineMedium,
                                color = RedbusCrimson
                            )
                            Text(
                                text = "Sign in with your redBus AI account",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onToggleDarkTheme) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                    }

                    TabRow(
                        selectedTabIndex = authTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = RedbusCrimson,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = authTab == 0,
                            onClick = { authTab = 0 },
                            text = { Text("Email Login", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = authTab == 1,
                            onClick = { authTab = 1 },
                            text = { Text("Mobile OTP", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = authTab == 2,
                            onClick = { authTab = 2 },
                            text = { Text("Sign Up", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }

                    uiState.authError?.let { err ->
                        Surface(
                            color = RedbusSoftTint,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                color = RedbusCrimson,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    uiState.authSuccessMessage?.let { msg ->
                        Surface(
                            color = RatingEmeraldBg,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = RatingEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    when (authTab) {
                        0 -> {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                visualTransformation = if (isLoginPasswordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isLoginPasswordVisible = !isLoginPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isLoginPasswordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = if (isLoginPasswordVisible) {
                                                "Hide password"
                                            } else {
                                                "Show password"
                                            },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { onLoginWithEmail(email, password) },
                                enabled = !uiState.isAuthLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = if (uiState.isAuthLoading) "SIGNING IN..." else "SIGN IN TO REDBUS AI",
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        1 -> {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Registered Mobile Number (+91...)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (uiState.otpSent) {
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { otpCode = it },
                                    label = { Text("6-Digit OTP") },
                                    visualTransformation = if (isOtpVisible) {
                                        VisualTransformation.None
                                    } else {
                                        PasswordVisualTransformation()
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isOtpVisible = !isOtpVisible }) {
                                            Icon(
                                                imageVector = if (isOtpVisible) {
                                                    Icons.Default.VisibilityOff
                                                } else {
                                                    Icons.Default.Visibility
                                                },
                                                contentDescription = if (isOtpVisible) {
                                                    "Hide OTP"
                                                } else {
                                                    "Show OTP"
                                                },
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { onVerifyMobileOtp(phone, otpCode) },
                                    enabled = !uiState.isAuthLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = RatingEmerald),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Text("VERIFY OTP & LOGIN", fontWeight = FontWeight.Black)
                                }
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { onRequestMobileOtp(phone) },
                                    enabled = !uiState.isAuthLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Text(
                                        text = if (uiState.isAuthLoading) "SENDING OTP..." else "SEND OTP CODE",
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        2 -> {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Mobile Number (+91...)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Create Password (min 6 chars)") },
                                visualTransformation = if (isRegisterPasswordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isRegisterPasswordVisible = !isRegisterPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isRegisterPasswordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = if (isRegisterPasswordVisible) {
                                                "Hide password"
                                            } else {
                                                "Show password"
                                            },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { onRegisterAccount(name, email, phone, password) },
                                enabled = !uiState.isAuthLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = if (uiState.isAuthLoading) "CREATING ACCOUNT..." else "CREATE REDBUS AI ACCOUNT",
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
