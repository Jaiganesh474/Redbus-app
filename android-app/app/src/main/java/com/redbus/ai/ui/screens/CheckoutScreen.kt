package com.redbus.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.redbus.ai.data.model.Coupon
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.viewmodel.FareBreakdown
import com.redbus.ai.ui.viewmodel.PendingRazorpayPayment
import com.redbus.ai.ui.viewmodel.RedbusUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

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
    onCompleteRazorpayPayment: (String, String) -> Unit,
    onDismissRazorpayModal: () -> Unit,
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
                        .padding(horizontal = 18.dp, vertical = 16.dp),
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
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
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
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isCreatingBooking) "CREATING ORDER..." else "PROCEED TO RAZORPAY",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            modifier = Modifier.padding(14.dp)
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
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
                        Spacer(modifier = Modifier.height(4.dp))
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

            // 2. Saved Travellers + Passenger Details
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Passenger Details",
                            style = MaterialTheme.typography.titleLarge
                        )

                        if (uiState.savedTravellers.isNotEmpty()) {
                            Text(
                                text = "Tap a saved traveller to autofill:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

                        uiState.passengers.forEachIndexed { idx, passenger ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Passenger ${idx + 1} • Seat ${passenger.seatNumber}",
                                    fontWeight = FontWeight.Black,
                                    color = RedbusCrimson,
                                    fontSize = 13.sp
                                )
                                OutlinedTextField(
                                    value = passenger.name,
                                    onValueChange = { onUpdatePassenger(idx, it, passenger.age, passenger.gender) },
                                    label = { Text("Passenger Full Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.contactEmail,
                            onValueChange = { onUpdateContact(it, uiState.contactPhone) },
                            label = { Text("Ticket Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
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

            // 3. Trip Protection Add-ons
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Trip Protection Add-ons",
                            style = MaterialTheme.typography.titleLarge
                        )

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
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.hasFreeCancellation,
                                onCheckedChange = onToggleFreeCancellation
                            )
                        }

                        HorizontalDivider()

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
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.hasTripGuarantee,
                                onCheckedChange = onToggleTripGuarantee
                            )
                        }
                    }
                }
            }

            // 4. Coupons & Offers
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Coupons & Offers",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = couponInput,
                                onValueChange = { couponInput = it },
                                placeholder = { Text("Enter Coupon Code") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onApplyCoupon(couponInput) },
                                enabled = !uiState.isValidatingCoupon,
                                colors = ButtonDefaults.buttonColors(containerColor = AiViolet),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp)
                            ) {
                                Text(
                                    text = if (uiState.isValidatingCoupon) "..." else "Apply",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        uiState.couponMessage?.let { msg ->
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.appliedCoupon?.valid == true) RatingEmerald else RedbusCrimson
                            )
                        }

                        if (availableCoupons.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

            // 5. Wallet & Fare Breakdown
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                        text = "Available Balance: ₹${walletBalance.toInt()}",
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
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

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

    // IN-APP RAZORPAY PAYMENT GATEWAY MODAL (Identical to RazorpayModal.tsx on website)
    uiState.pendingRazorpayPayment?.let { pendingOrder ->
        RazorpayMobilePaymentModal(
            payment = pendingOrder,
            isVerifying = uiState.isVerifyingPayment,
            onDismiss = onDismissRazorpayModal,
            onPaymentSuccess = { paymentId, sig ->
                onCompleteRazorpayPayment(paymentId, sig)
            }
        )
    }
}

@Composable
private fun RazorpayMobilePaymentModal(
    payment: PendingRazorpayPayment,
    isVerifying: Boolean,
    onDismiss: () -> Unit,
    onPaymentSuccess: (String, String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("upi") } // "upi" | "card" | "netbanking" | "wallet"
    var upiId by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("4532 •••• •••• 8892") }
    var cardExpiry by remember { mutableStateOf("08/28") }
    var cardCvv by remember { mutableStateOf("842") }
    var cardHolder by remember { mutableStateOf(payment.passengerName) }
    var selectedBank by remember { mutableStateOf("HDFC") }
    var selectedWallet by remember { mutableStateOf("AmazonPay") }

    var isProcessingGateway by remember { mutableStateOf(false) }
    var processingStepText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!isProcessingGateway && !isVerifying) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Top Razorpay Brand & Order Summary Banner (Deep Navy #0C2340 to #08182B)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0C2340), Color(0xFF08182B))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "redBus",
                                            color = RedbusCrimson,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "redBus India",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFF3B82F6).copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(5.dp)
                                        ) {
                                            Text(
                                                text = "VERIFIED",
                                                color = Color(0xFF93C5FD),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Official Razorpay Ticketing Gateway",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { if (!isProcessingGateway && !isVerifying) onDismiss() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }

                        // PNR & Amount Payable Card
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Booking PNR", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                    Text(
                                        text = payment.pnr,
                                        color = Color(0xFFFCD34D),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Passenger", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                    Text(
                                        text = payment.passengerName,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Amount Payable",
                                        color = Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.US, "%.2f", payment.amount)}",
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp
                                    )
                                }
                            }
                        }

                        // Payment Method Selector Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val methods = listOf(
                                "upi" to "UPI / QR",
                                "card" to "Card",
                                "netbanking" to "NetBanking",
                                "wallet" to "Wallets"
                            )
                            methods.forEach { (id, label) ->
                                val active = selectedMethod == id
                                Surface(
                                    onClick = { selectedMethod = id },
                                    color = if (active) Color(0xFF2563EB) else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Active Payment Method Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedMethod) {
                        "upi" -> {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Scan QR Code using any UPI App",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Surface(
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.size(132.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCode2,
                                                contentDescription = "UPI QR",
                                                tint = Color.White,
                                                modifier = Modifier.size(78.dp)
                                            )
                                            Text(
                                                text = "GPAY • PHONEPE",
                                                color = Color(0xFFFCD34D),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("GPay", "PhonePe", "Paytm", "BHIM").forEach { app ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                            ) {
                                                Text(
                                                    text = app,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Or enter UPI ID / VPA",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(onClick = { upiId = "traveler@okhdfcbank" }) {
                                        Text("Fill Demo VPA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                OutlinedTextField(
                                    value = upiId,
                                    onValueChange = { upiId = it },
                                    placeholder = { Text("e.g. yourname@okaxis, mobile@paytm") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        "card" -> {
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { cardNumber = it },
                                label = { Text("Card Number") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = cardExpiry,
                                    onValueChange = { cardExpiry = it },
                                    label = { Text("Expiry (MM/YY)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = cardCvv,
                                    onValueChange = { cardCvv = it },
                                    label = { Text("CVV") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = cardHolder,
                                onValueChange = { cardHolder = it },
                                label = { Text("Cardholder Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        "netbanking" -> {
                            Text("Select Popular Bank", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            val banks = listOf("HDFC", "SBI", "ICICI", "Axis", "Kotak", "PNB")
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                banks.chunked(3).forEach { rowBanks ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowBanks.forEach { bank ->
                                            val active = selectedBank == bank
                                            Surface(
                                                onClick = { selectedBank = bank },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (active) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(
                                                        width = if (active) 2.dp else 1.dp,
                                                        color = if (active) Color(0xFF2563EB) else MaterialTheme.colorScheme.outline,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                            ) {
                                                Text(
                                                    text = "🏦 $bank",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = if (active) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(vertical = 14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "wallet" -> {
                            Text("Select Digital Wallet", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            val wallets = listOf(
                                "AmazonPay" to "Amazon Pay • Fast 1-click checkout",
                                "Mobikwik" to "MobiKwik • SuperCash accepted",
                                "Airtel" to "Airtel Money • Instant cashback",
                                "Simpl" to "Simpl PayLater • Pay next month"
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                wallets.forEach { (id, desc) ->
                                    val active = selectedWallet == id
                                    Surface(
                                        onClick = { selectedWallet = id },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (active) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = if (active) 2.dp else 1.dp,
                                                color = if (active) Color(0xFF2563EB) else MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                    ) {
                                        Text(
                                            text = desc,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (active) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Razorpay Pay CTA Button & Animated Processing State
                    if (isProcessingGateway || isVerifying) {
                        Surface(
                            color = Color(0xFF2563EB),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Processing Razorpay Payment...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = processingStepText.ifBlank { "Verifying signature with redBus bank gateway..." },
                                    color = Color(0xFFBFDBFE),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessingGateway = true
                                    processingStepText = "Contacting Razorpay Gateway..."
                                    delay(550)
                                    processingStepText = "Authorizing transaction with bank..."
                                    delay(550)
                                    processingStepText = "Securing 256-bit SSL signature..."
                                    delay(500)
                                    val payId = "pay_" + UUID.randomUUID().toString().replace("-", "").take(10)
                                    val sig = "mock_sig_sandbox"
                                    onPaymentSuccess(payId, sig)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pay ₹${String.format(Locale.US, "%.2f", payment.amount)} with Razorpay",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Razorpay Trust Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = RatingEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Razorpay Secured",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "256-bit SSL Encrypted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
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
