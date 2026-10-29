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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.redbus.ai.data.model.BookingDetails
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.viewmodel.RedbusUiState

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
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
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
                            text = "Synced live with your redbusai.app account",
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

                Spacer(modifier = Modifier.height(12.dp))

                // Live PNR Search Bar (/bookings/{pnr})
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSearchPnr(pnrQuery) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = RedbusCrimson
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = RedbusCrimson,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sign In to View Your Bookings",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sign in with the same account you use on redbusai.app to see all your bus tickets, or search any ticket above using its PNR.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                            shape = RoundedCornerShape(12.dp)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(displayedBookings, key = { it.pnr.ifBlank { it.id.toString() } }) { booking ->
                    BookingTicketCard(
                        booking = booking,
                        onCancelClick = { bookingToCancel = booking },
                        onEmailClick = { onEmailTicket(booking.pnr) }
                    )
                }
            }
        }
    }

    if (bookingToCancel != null) {
        val b = bookingToCancel!!
        AlertDialog(
            onDismissRequest = { bookingToCancel = null },
            title = {
                Text("Cancel Ticket (PNR: ${b.pnr})", fontWeight = FontWeight.Black)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    onCancelClick: () -> Unit,
    onEmailClick: () -> Unit
) {
    val isConfirmed = booking.status == "CONFIRMED"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Boarding: ${booking.boardingPoint} • Dropping: ${booking.droppingPoint}",
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
                        text = "Total: ₹${booking.totalAmount.toInt()}",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    booking.refundStatus?.let { rs ->
                        Text(
                            text = "Refund Status: $rs",
                            fontSize = 11.sp,
                            color = RatingEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEmailClick) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email E-Ticket",
                            tint = AiViolet
                        )
                    }
                    if (isConfirmed) {
                        OutlinedButton(
                            onClick = onCancelClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
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
    onOpenWebPortal: (String) -> Unit,
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
            onOpenWebAuth = { onOpenWebPortal("https://www.redbusai.app/login") },
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live User Account Header (Matching https://www.redbusai.app/profile)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = RedbusCrimson),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
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

                    Spacer(modifier = Modifier.height(12.dp))

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
                                    text = if (user.emailVerified == true) "✓ Verified Email" else "⚠ Unverified Email",
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
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REDBUS WALLET (LIVE SYNCED)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format("%.2f", user.walletBalance ?: 0.0)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = RatingEmerald
                        )
                        Text(
                            text = "Instant refunds & 1-tap checkout across Web & Android",
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

        // 3. Operator / Admin Role Dashboards (If user has Operator or Admin role on redbusai.app)
        if (user.role == "ROLE_OPERATOR" || user.role == "ROLE_ADMIN") {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Management Portals (${user.role})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onOpenWebPortal("https://www.redbusai.app/operator") },
                                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Open Operator Hub", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (user.role == "ROLE_ADMIN") {
                                Button(
                                    onClick = { onOpenWebPortal("https://www.redbusai.app/admin") },
                                    colors = ButtonDefaults.buttonColors(containerColor = AiViolet),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Open Admin Portal", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Live Saved Travellers (/auth/saved-travellers)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                                    Spacer(modifier = Modifier.width(10.dp))
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

        // 5. Theme & Sign Out Controls
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                                    text = "Syncs visual theme across screens",
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
private fun LiveAuthScreen(
    uiState: RedbusUiState,
    onLoginWithEmail: (String, String) -> Unit,
    onRegisterAccount: (String, String, String, String) -> Unit,
    onRequestMobileOtp: (String) -> Unit,
    onVerifyMobileOtp: (String, String) -> Unit,
    onOpenWebAuth: () -> Unit,
    onToggleDarkTheme: () -> Unit
) {
    var authTab by remember { mutableIntStateOf(0) } // 0 = Email Login, 1 = Mobile OTP, 2 = Register
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
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
                                text = "Use your exact same account from redbusai.app",
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auth Mode Tabs (Email Login | Mobile OTP | Sign Up)
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

                    Spacer(modifier = Modifier.height(14.dp))

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
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
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
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
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
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onLoginWithEmail(email, password) },
                                enabled = !uiState.isAuthLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
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
                            Spacer(modifier = Modifier.height(10.dp))
                            if (uiState.otpSent) {
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { otpCode = it },
                                    label = { Text("6-Digit OTP") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onVerifyMobileOtp(phone, otpCode) },
                                    enabled = !uiState.isAuthLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = RatingEmerald),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Text("VERIFY OTP & LOGIN", fontWeight = FontWeight.Black)
                                }
                            } else {
                                Button(
                                    onClick = { onRequestMobileOtp(phone) },
                                    enabled = !uiState.isAuthLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
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
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Mobile Number (+91...)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Create Password (min 6 chars)") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onRegisterAccount(name, email, phone, password) },
                                enabled = !uiState.isAuthLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = if (uiState.isAuthLoading) "CREATING ACCOUNT..." else "CREATE REDBUS AI ACCOUNT",
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Google / Firebase OAuth via synced redbusai.app WebView
                    OutlinedButton(
                        onClick = onOpenWebAuth,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = AiViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continue with Google / Firebase on redbusai.app",
                            fontWeight = FontWeight.Bold,
                            color = AiViolet,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
