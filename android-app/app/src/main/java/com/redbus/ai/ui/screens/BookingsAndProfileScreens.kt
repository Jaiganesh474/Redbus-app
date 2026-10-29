package com.redbus.ai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.PathEffect
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
import com.redbus.ai.ui.viewmodel.RedbusUiState
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
fun MyBookingsScreen(
    uiState: RedbusUiState,
    onSearchPnr: (String) -> Unit,
    onRefreshBookings: () -> Unit,
    onCancelBooking: (String, String, String) -> Unit,
    onEmailTicket: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var pnrQuery by remember { mutableStateOf("") }
    var bookingToCancel by remember { mutableStateOf<BookingDetails?>(null) }
    var selectedTicketToView by remember { mutableStateOf<BookingDetails?>(null) }
    var cancelReason by remember { mutableStateOf("Change of plans") }
    var refundDestination by remember { mutableStateOf("WALLET") }

    val displayedBookings = remember(uiState.pnrSearchResult, uiState.myBookings) {
        if (uiState.pnrSearchResult != null) {
            listOf(uiState.pnrSearchResult)
        } else {
            uiState.myBookings
        }
    }

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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
        } else if (displayedBookings.isEmpty()) {
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
                        text = "No bookings found for your account yet",
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
                items(displayedBookings, key = { it.pnr.ifBlank { it.id.toString() } }) { booking ->
                    BookingTicketCard(
                        booking = booking,
                        onViewTicketClick = { selectedTicketToView = booking },
                        onCancelClick = { bookingToCancel = booking },
                        onEmailClick = { onEmailTicket(booking.pnr) }
                    )
                }
            }
        }
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
    onEmailClick: () -> Unit
) {
    val isConfirmed = booking.status == "CONFIRMED"

    Card(
        onClick = onViewTicketClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
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
                    color = if (isConfirmed) RatingEmeraldBg else RedbusSoftTint,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "● ${booking.status}",
                        color = if (isConfirmed) RatingEmerald else RedbusCrimson,
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

                    if (isConfirmed) {
                        OutlinedButton(
                            onClick = onCancelClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val backdropColor = Color(0xFF1C1C1E)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor.copy(alpha = 0.92f))
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Top Circular Close Button (✕) matching Image 5
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = Color(0xFF2C2C2E),
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Ticket",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Main Perforated Ticket Card (Matching Image 5)
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 1. Top Crimson Header (Source -> Duration -> Destination)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(RedbusCrimson)
                                    .padding(horizontal = 20.dp, vertical = 22.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Vertical Timeline Column (↗ ─── ↘)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.NorthEast,
                                                    contentDescription = null,
                                                    tint = RedbusCrimson,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.5.dp)
                                                .height(82.dp)
                                                .background(Color.White.copy(alpha = 0.7f))
                                        )

                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.SouthEast,
                                                    contentDescription = null,
                                                    tint = RedbusCrimson,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Right Content Column (Departure, Duration Divider, Arrival)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = booking.boardingPoint.ifBlank { "${booking.sourceCity} Bypass" },
                                                    color = Color.White.copy(alpha = 0.88f),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = booking.departureTime,
                                                    color = Color.White,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = departureDateFormatted,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    fontSize = 12.sp,
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
                                                modifier = Modifier.padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsBus,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = durationLabel,
                                                    color = Color.White.copy(alpha = 0.95f),
                                                    fontSize = 12.sp,
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
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = booking.droppingPoint.ifBlank { booking.destinationCity },
                                                    color = Color.White.copy(alpha = 0.88f),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = booking.arrivalTime,
                                                    color = Color.White,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = arrivalDateFormatted,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    fontSize = 12.sp,
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
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = booking.operatorName,
                                    color = Color(0xFF111827),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${booking.busType} · ${passengers.size} ${if (passengers.size == 1) "Seat" else "Seats"}",
                                    color = Color(0xFF6B7280),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }

                            DashedTicketDivider()

                            // 3. Passenger List Section (Name + (F)/(M), Age, Seat Pill Badge)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "($genderShort)",
                                                    color = Color(0xFF6B7280),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "(${p.age} yrs)",
                                                color = Color(0xFF6B7280),
                                                fontSize = 13.sp
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
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Semicircular Side Cutout Notches + Dashed Perforation Line (Image 5)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                DashedTicketDivider()

                                // Left semicircular cutout notch
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = (-15).dp)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(backdropColor)
                                )

                                // Right semicircular cutout notch
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 15.dp)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(backdropColor)
                                )
                            }

                            // 5. Ticket #, PNR #, and Fare Footer (Matching Image 5)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clipboard?.setPrimaryClip(ClipData.newPlainText("Ticket Number", ticketNumber))
                                            Toast.makeText(context, "Ticket #$ticketNumber copied", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Ticket Number",
                                            tint = Color(0xFF111827),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = ticketNumber,
                                            color = Color(0xFF111827),
                                            fontSize = 15.sp,
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
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = booking.pnr,
                                        color = Color(0xFF111827),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Fare Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Fare",
                                        color = Color(0xFF6B7280),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.US, "%.2f", booking.totalAmount)}",
                                        color = Color(0xFF111827),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Full-Width Red Pill Button: "Share your ticket" (Matching Image 5)
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
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Share your ticket",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
    onToggleDarkTheme: () -> Unit
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

    val user = uiState.user
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddTravellerDialog by remember { mutableStateOf(false) }

    var editName by remember(user.name) { mutableStateOf(user.name) }
    var editPhone by remember(user.phone) { mutableStateOf(user.phone.orEmpty()) }
    var editGender by remember(user.gender) { mutableStateOf(user.gender ?: "MALE") }

    var newTravellerName by remember { mutableStateOf("") }
    var newTravellerAge by remember { mutableStateOf("25") }
    var newTravellerGender by remember { mutableStateOf("MALE") }

    val initials = remember(user.name) {
        user.name.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifBlank { "U" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live User Account Header
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
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(58.dp)
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
                                            fontSize = 20.sp
                                        )
                                    }
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
                            onClick = { showEditProfileDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. Live redBus Wallet Balance Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "REDBUS WALLET BALANCE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", user.walletBalance ?: 0.0)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = RatingEmerald
                        )
                        Text(
                            text = "Instant refunds & 1-tap checkout credit",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = RatingEmerald,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        // 3. Live Saved Travellers (/auth/saved-travellers)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Saved Travellers (${uiState.savedTravellers.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        FilledTonalButton(
                            onClick = { showAddTravellerDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Traveller", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (uiState.savedTravellers.isEmpty()) {
                        Text(
                            text = "No saved travellers yet. Add family or friends for 1-tap checkout.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.savedTravellers.forEach { t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = AiViolet
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = t.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${t.age} yrs • ${t.gender}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteTraveller(t.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove Traveller",
                                        tint = RedbusCrimson
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Theme & Sign Out Controls
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = RedbusCrimson
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Dark Mode Theme", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Comfortable night bus booking appearance",
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

                    HorizontalDivider()

                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out of redBus AI", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(editName, editPhone, editGender)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddTravellerDialog) {
        AlertDialog(
            onDismissRequest = { showAddTravellerDialog = false },
            title = { Text("Add Saved Traveller", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTravellerName,
                        onValueChange = { newTravellerName = it },
                        label = { Text("Passenger Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newTravellerAge,
                        onValueChange = { newTravellerAge = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Age") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("MALE", "FEMALE").forEach { g ->
                            FilterChip(
                                selected = newTravellerGender == g,
                                onClick = { newTravellerGender = g },
                                label = { Text(g) }
                            )
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
                    Text("Save Traveller", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTravellerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
