package com.redbus.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redbus.ai.data.model.Coupon
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.viewmodel.FareBreakdown
import com.redbus.ai.ui.viewmodel.RedbusUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    uiState: RedbusUiState,
    fareBreakdown: FareBreakdown,
    availableCoupons: List<Coupon>,
    onUpdatePassenger: (Int, String, Int, String) -> Unit,
    onUpdateContact: (String, String) -> Unit,
    onToggleFreeCancellation: (Boolean) -> Unit,
    onToggleTripGuarantee: (Boolean) -> Unit,
    onToggleUseWallet: (Boolean) -> Unit,
    onApplyCoupon: (String) -> Unit,
    onRemoveCoupon: () -> Unit,
    onConfirmAndPay: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onBack: () -> Unit
) {
    val route = uiState.selectedRoute ?: return
    var couponInput by remember { mutableStateOf("") }

    val mins = uiState.seatLockSeconds / 60
    val secs = uiState.seatLockSeconds % 60
    val formattedLockTimer = String.format(Locale.US, "%02d:%02d", mins, secs)
    val walletBalance = uiState.user?.walletBalance ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Review & Checkout",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "${route.sourceCity} ➔ ${route.destinationCity}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formattedLockTimer,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
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
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp
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
                            text = "Net Payable Amount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${fareBreakdown.netPayable.toInt()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = RedbusCrimson
                        )
                    }

                    if (!uiState.isAuthenticated) {
                        Button(
                            onClick = onNavigateToAuth,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedbusCrimson,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SIGN IN TO BOOK",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = onConfirmAndPay,
                            enabled = !uiState.isCreatingBooking,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedbusCrimson,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isCreatingBooking) "PROCESSING..." else "PAY & BOOK NOW",
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
            uiState.errorBanner?.let { err ->
                item {
                    Surface(
                        color = RedbusSoftTint,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = RedbusCrimson,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 1. Trip Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = route.operatorName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${route.busType} • Seats: ${uiState.selectedSeats.joinToString { it.seatNumber }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RedbusCrimson,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Boarding: ${uiState.selectedBoardingPoint.ifBlank { route.sourceCity }}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Dropping: ${uiState.selectedDroppingPoint.ifBlank { route.destinationCity }}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Saved Travellers from User's Account + Passenger Forms
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Passenger Details",
                            style = MaterialTheme.typography.titleLarge
                        )

                        if (uiState.savedTravellers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Saved Travellers from your redbusai.app account:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.savedTravellers) { saved ->
                                    AssistChip(
                                        onClick = {
                                            if (uiState.passengers.isNotEmpty()) {
                                                onUpdatePassenger(0, saved.name, saved.age, saved.gender)
                                            }
                                        },
                                        label = { Text("${saved.name} (${saved.age})") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        uiState.passengers.forEachIndexed { idx, passenger ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Passenger ${idx + 1} • Seat ${passenger.seatNumber}",
                                    fontWeight = FontWeight.Black,
                                    color = RedbusCrimson,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = passenger.name,
                                    onValueChange = { onUpdatePassenger(idx, it, passenger.age, passenger.gender) },
                                    label = { Text("Passenger Full Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.contactEmail,
                            onValueChange = { onUpdateContact(it, uiState.contactPhone) },
                            label = { Text("Ticket Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.contactPhone,
                            onValueChange = { onUpdateContact(uiState.contactEmail, it) },
                            label = { Text("Mobile Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. Protection Add-ons (Matching CheckoutContent on redbusai.app)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Trip Protection Add-ons",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Free Cancellation (₹49/seat)",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "100% Base Fare Refund if cancelled anytime before departure",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.hasFreeCancellation,
                                onCheckedChange = onToggleFreeCancellation
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "redBus Trip Guarantee (₹69/seat)",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "2X Refund if bus is cancelled or delayed > 2 hours",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.hasTripGuarantee,
                                onCheckedChange = onToggleTripGuarantee
                            )
                        }
                    }
                }
            }

            // 4. Live Coupons Validation (/coupons/validate)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Coupons & Offers",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = couponInput,
                                onValueChange = { couponInput = it },
                                placeholder = { Text("Enter Coupon Code") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onApplyCoupon(couponInput) },
                                enabled = !uiState.isValidatingCoupon,
                                colors = ButtonDefaults.buttonColors(containerColor = AiViolet),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (uiState.isValidatingCoupon) "..." else "Apply",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        uiState.couponMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.appliedCoupon?.valid == true) RatingEmerald else RedbusCrimson
                            )
                        }

                        if (availableCoupons.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(availableCoupons) { c ->
                                    val isApplied = uiState.appliedCoupon?.code.equals(c.code, ignoreCase = true)
                                    FilterChip(
                                        selected = isApplied,
                                        onClick = {
                                            if (isApplied) onRemoveCoupon() else {
                                                couponInput = c.code
                                                onApplyCoupon(c.code)
                                            }
                                        },
                                        label = {
                                            Text("${c.code} (${c.discountPercentage}% OFF)")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Real Wallet Balance & Fare Summary
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (uiState.isAuthenticated) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Use redBus Wallet Balance",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Available in your account: ₹${walletBalance.toInt()}",
                                        fontSize = 12.sp,
                                        color = RatingEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Checkbox(
                                    checked = uiState.useWalletBalance,
                                    onCheckedChange = onToggleUseWallet,
                                    enabled = walletBalance > 0
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        }

                        FareRow("Base Seat Fare", "₹${fareBreakdown.baseFare.toInt()}")
                        if (fareBreakdown.freeCancelFee > 0) {
                            FareRow("Free Cancellation Add-on", "+₹${fareBreakdown.freeCancelFee.toInt()}")
                        }
                        if (fareBreakdown.tripGuaranteeFee > 0) {
                            FareRow("Trip Guarantee Protection", "+₹${fareBreakdown.tripGuaranteeFee.toInt()}")
                        }
                        FareRow("GST & Platform Service Fee", "+₹${fareBreakdown.gstAndService.toInt()}")
                        if (fareBreakdown.discountAmount > 0) {
                            FareRow(
                                "Coupon Discount (${uiState.appliedCoupon?.code})",
                                "-₹${fareBreakdown.discountAmount.toInt()}",
                                isDiscount = true
                            )
                        }
                        if (fareBreakdown.walletUsed > 0) {
                            FareRow(
                                "redBus Wallet Applied",
                                "-₹${fareBreakdown.walletUsed.toInt()}",
                                isDiscount = true
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Payable", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(
                                text = "₹${fareBreakdown.netPayable.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = RedbusCrimson
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FareRow(
    label: String,
    amount: String,
    isDiscount: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDiscount) RatingEmerald else MaterialTheme.colorScheme.onSurface
        )
    }
}
