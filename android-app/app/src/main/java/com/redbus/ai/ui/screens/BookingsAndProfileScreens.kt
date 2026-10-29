package com.redbus.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redbus.ai.data.model.BookingDetails
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.viewmodel.RedbusUiState

@Composable
fun MyBookingsScreen(
    uiState: RedbusUiState,
    onCancelBooking: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = RedbusCrimson,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                Text(
                    text = "My Bus Bookings & PNR Tickets",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Live boarding pass, QR ticket & instant wallet refund",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(uiState.myBookings, key = { it.pnr }) { booking ->
                BookingTicketCard(
                    booking = booking,
                    onCancel = { onCancelBooking(booking.pnr) }
                )
            }
        }
    }
}

@Composable
private fun BookingTicketCard(
    booking: BookingDetails,
    onCancel: () -> Unit
) {
    val isConfirmed = booking.status == "CONFIRMED"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        text = if (isConfirmed) "● CONFIRMED TICKET" else "● CANCELLED & REFUNDED",
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${booking.sourceCity} ➔ ${booking.destinationCity}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "${booking.operatorName} • ${booking.busType}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Date & Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${booking.travelDate} • ${booking.departureTime}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Seats", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = booking.passengers.joinToString { "${it.name} (${it.seatNumber})" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = RedbusCrimson
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Boarding: ${booking.boardingPoint}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Paid: ₹${booking.totalAmount.toInt()}",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    if (booking.hasFreeCancellation) {
                        Text(
                            text = "✓ Protected by 100% Free Cancellation",
                            fontSize = 11.sp,
                            color = RatingEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isConfirmed) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson)
                    ) {
                        Text("Cancel & Refund to Wallet", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileAndWalletScreen(
    uiState: RedbusUiState,
    onToggleDarkTheme: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = RedbusCrimson),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "AS",
                                color = RedbusCrimson,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Aarav Sharma",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${uiState.contactEmail} • ${uiState.contactPhone}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "RedBus AI Prime Member ✨",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Wallet Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REDBUS AI INSTANT WALLET",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${uiState.walletBalance.toInt()}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = RatingEmerald
                        )
                        Text(
                            text = "Zero-wait refunds & 1-tap checkout credit",
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

        // Theme Settings
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
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
                            Text(
                                text = "Dark Appearance Mode",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Comfortable night bus booking theme",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = uiState.isDarkTheme,
                        onCheckedChange = { onToggleDarkTheme() }
                    )
                }
            }
        }

        // Saved Travellers
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Saved Travellers",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = t.name,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${t.age} yrs • ${t.gender}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

