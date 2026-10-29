package com.redbus.ai.ui.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.redbus.ai.data.model.Coupon
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.viewmodel.FareBreakdown
import com.redbus.ai.ui.viewmodel.PendingRazorpayPayment
import com.redbus.ai.ui.viewmodel.RedbusUiState
import org.json.JSONObject
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
    onCompleteRazorpayPayment: (String, String, String) -> Unit,
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
                                text = if (uiState.isCreatingBooking) "CREATING ORDER..." else "PAY WITH RAZORPAY",
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

            // 3. Trip Protection Add-ons (Matching checkout/page.tsx ₹21/passenger)
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
                                    text = "Free Cancellation (₹21/seat)",
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
                                    text = "redBus Trip Guarantee (₹21/seat)",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Up to 130% Refund if bus is cancelled or delayed",
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
                        FareRow("AI Operator Service Fee", "+₹${fareBreakdown.gstAndService.toInt()}")
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

    // OFFICIAL RAZORPAY CHECKOUT TEST PAGE IN MOBILE APP (Identical to checkout/page.tsx lines 428-498)
    uiState.pendingRazorpayPayment?.let { pendingOrder ->
        RazorpayOfficialTestPageModal(
            payment = pendingOrder,
            isVerifying = uiState.isVerifyingPayment,
            onDismiss = onDismissRazorpayModal,
            onPaymentSuccess = { orderId, paymentId, sig ->
                onCompleteRazorpayPayment(orderId, paymentId, sig)
            }
        )
    }
}

/**
 * Loads the official Razorpay Checkout Test Page (`https://checkout.razorpay.com/v1/checkout.js`)
 * inside the mobile app with full support for Razorpay Test Mode Bank popup windows (`onCreateWindow`),
 * matching `frontend/src/app/checkout/page.tsx` lines 316-498 and verifying real HMAC-SHA256 signatures.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RazorpayOfficialTestPageModal(
    payment: PendingRazorpayPayment,
    isVerifying: Boolean,
    onDismiss: () -> Unit,
    onPaymentSuccess: (String, String, String) -> Unit
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var isLoadingGateway by remember { mutableStateOf(true) }
    var gatewayError by remember { mutableStateOf<String?>(null) }

    val checkoutHtml = remember(payment) {
        val safeName = JSONObject.quote(payment.passengerName)
        val safeEmail = JSONObject.quote(payment.contactEmail)
        val safePhone = JSONObject.quote(payment.contactPhone)
        val safePnr = JSONObject.quote(payment.pnr)
        val safeOrderId = JSONObject.quote(payment.orderId)
        val safeKey = JSONObject.quote(payment.keyId)

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <title>Razorpay Secure Checkout - redBus India</title>
            <script src="https://checkout.razorpay.com/v1/checkout.js"></script>
            <style>
                * { box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                body {
                    margin: 0;
                    padding: 0;
                    background: #0F172A;
                    color: #FFFFFF;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    min-height: 100vh;
                    text-align: center;
                }
                .loader-box {
                    padding: 24px;
                    max-width: 320px;
                }
                .spinner {
                    width: 40px;
                    height: 40px;
                    border: 4px solid rgba(255,255,255,0.2);
                    border-top-color: #D84E55;
                    border-radius: 50%;
                    animation: spin 0.8s linear infinite;
                    margin: 0 auto 16px auto;
                }
                @keyframes spin { to { transform: rotate(360deg); } }
                .title { font-size: 16px; font-weight: 800; margin-bottom: 6px; }
                .sub { font-size: 12px; color: #94A3B8; }
            </style>
        </head>
        <body>
            <div class="loader-box" id="statusBox">
                <div class="spinner"></div>
                <div class="title">Opening Razorpay Test Gateway...</div>
                <div class="sub">PNR: ${payment.pnr} • ₹${payment.amount.toInt()}</div>
            </div>
            <script>
                function launchRazorpay() {
                    if (typeof window.Razorpay === 'undefined') {
                        if (window.RazorpayBridge) {
                            window.RazorpayBridge.onGatewayError("Failed to load Razorpay SDK. Please check your internet connection.");
                        }
                        return;
                    }
                    var options = {
                        key: $safeKey,
                        amount: ${payment.amountInPaise},
                        currency: "INR",
                        name: "redBus India",
                        description: "Bus Booking PNR: " + $safePnr,
                        order_id: $safeOrderId,
                        prefill: {
                            name: $safeName,
                            email: $safeEmail,
                            contact: $safePhone
                        },
                        notes: {
                            pnr: $safePnr
                        },
                        theme: {
                            color: "#d84e55"
                        },
                        handler: function (response) {
                            if (window.RazorpayBridge) {
                                window.RazorpayBridge.onPaymentSuccess(
                                    response.razorpay_order_id || $safeOrderId,
                                    response.razorpay_payment_id || "",
                                    response.razorpay_signature || ""
                                );
                            }
                        },
                        modal: {
                            ondismiss: function () {
                                if (window.RazorpayBridge) {
                                    window.RazorpayBridge.onPaymentDismissed();
                                }
                            }
                        }
                    };
                    try {
                        var rzp = new window.Razorpay(options);
                        rzp.on('payment.failed', function (resp) {
                            var desc = (resp && resp.error && resp.error.description) ? resp.error.description : "Payment failed in Razorpay.";
                            if (window.RazorpayBridge) {
                                window.RazorpayBridge.onGatewayError(desc);
                            }
                        });
                        rzp.open();
                        if (window.RazorpayBridge) {
                            window.RazorpayBridge.onGatewayReady();
                        }
                    } catch (e) {
                        if (window.RazorpayBridge) {
                            window.RazorpayBridge.onGatewayError(e.message || "Error launching Razorpay.");
                        }
                    }
                }
                window.onload = function() {
                    setTimeout(launchRazorpay, 150);
                };
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Dialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar with Razorpay Test Mode Indicator & Close Button
                Surface(
                    color = Color(0xFF0C2340),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = Color(0xFFF59E0B),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "TEST MODE",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Razorpay Checkout • redBus India",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "PNR: ${payment.pnr} • Payable: ₹${payment.amount.toInt()}",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { if (!isVerifying) onDismiss() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Razorpay",
                                tint = Color.White
                            )
                        }
                    }
                }

                gatewayError?.let { err ->
                    Surface(
                        color = RedbusSoftTint,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = err,
                                color = RedbusCrimson,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismiss) {
                                Text("Close", color = RedbusCrimson, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            val container = FrameLayout(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }

                            val primaryWebView = WebView(context).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    javaScriptCanOpenWindowsAutomatically = true
                                    setSupportMultipleWindows(true)
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                }

                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun onGatewayReady() {
                                            mainHandler.post { isLoadingGateway = false }
                                        }

                                        @JavascriptInterface
                                        fun onPaymentSuccess(orderId: String, paymentId: String, signature: String) {
                                            mainHandler.post {
                                                onPaymentSuccess(orderId, paymentId, signature)
                                            }
                                        }

                                        @JavascriptInterface
                                        fun onPaymentDismissed() {
                                            mainHandler.post {
                                                if (!isVerifying) onDismiss()
                                            }
                                        }

                                        @JavascriptInterface
                                        fun onGatewayError(message: String) {
                                            mainHandler.post {
                                                isLoadingGateway = false
                                                gatewayError = message
                                            }
                                        }
                                    },
                                    "RazorpayBridge"
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        return false
                                    }
                                }

                                // Support Razorpay Test Bank Popup Windows (Success / Failure test page)
                                webChromeClient = object : WebChromeClient() {
                                    override fun onCreateWindow(
                                        view: WebView?,
                                        isDialog: Boolean,
                                        isUserGesture: Boolean,
                                        resultMsg: Message?
                                    ): Boolean {
                                        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                        val popupWebView = WebView(context).apply {
                                            layoutParams = FrameLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            settings.apply {
                                                javaScriptEnabled = true
                                                domStorageEnabled = true
                                                javaScriptCanOpenWindowsAutomatically = true
                                                setSupportMultipleWindows(true)
                                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            }
                                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                            webViewClient = WebViewClient()
                                            webChromeClient = object : WebChromeClient() {
                                                override fun onCloseWindow(window: WebView?) {
                                                    container.removeView(this@apply)
                                                    destroy()
                                                }
                                            }
                                        }
                                        container.addView(popupWebView)
                                        transport.webView = popupWebView
                                        resultMsg.sendToTarget()
                                        return true
                                    }
                                }

                                loadDataWithBaseURL(
                                    "https://www.redbusai.app",
                                    checkoutHtml,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }

                            container.addView(primaryWebView)
                            container
                        }
                    )

                    if (isVerifying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = RedbusCrimson)
                                    Text(
                                        text = "Verifying Payment & Issuing E-Ticket...",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
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

@Composable
private fun FareRow(
    label: String,
    value: String,
    isDiscount: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isDiscount) RatingEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isDiscount) FontWeight.Bold else FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = if (isDiscount) RatingEmerald else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}
