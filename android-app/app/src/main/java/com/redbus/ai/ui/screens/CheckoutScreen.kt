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

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape

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
import androidx.compose.ui.graphics.RectangleShape
import kotlinx.coroutines.delay

import com.redbus.ai.data.model.*

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
    onAddTraveller: (String, Int, String) -> Unit = { _, _, _ -> },

    onUpdateContact: (String, String, String) -> Unit,

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

    var showBookingDetailsModal by remember { mutableStateOf(false) }
    var showEditContactDialog by remember { mutableStateOf(false) }
    var showAddPassengerDialog by remember { mutableStateOf(false) }
    var newPassengerName by remember { mutableStateOf("") }
    var newPassengerAge by remember { mutableStateOf("") }
    var newPassengerGender by remember { mutableStateOf("MALE") }
    var editContactPhone by remember { mutableStateOf(uiState.contactPhone) }
    var editContactEmail by remember { mutableStateOf(uiState.contactEmail) }
    var editStateOfResidence by remember { mutableStateOf(uiState.stateOfResidence.ifBlank { "Karnataka" }) }



    // Auto-return to buses tab when countdown timer expires

    LaunchedEffect(uiState.seatLockSeconds) {

        if (uiState.seatLockSeconds <= 0) {

            onBack()

        }

    }



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

                windowInsets = WindowInsets(0, 0, 0, 0),

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

                            onClick = { onConfirmAndPay() },

                            enabled = !uiState.isCreatingBooking,

                            shape = RoundedCornerShape(14.dp),

                            colors = ButtonDefaults.buttonColors(

                                containerColor = RedbusCrimson,

                                contentColor = Color.White

                            ),

                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)

                        ) {

                            Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(

                                text = "PROCEED TO PAY (₹${fareBreakdown.netPayable.toInt()})",

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



            // 1. Trip Summary Card with 'View details' (Matching Image 4)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = route.operatorName.ifBlank { "IntrCity SmartBus" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = route.busType,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "View details >",
                                color = Color(0xFF0284C7),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clickable { showBookingDetailsModal = true }
                                    .padding(4.dp)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = route.departureTime.take(5),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = route.sourceCity,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.selectedBoardingPoint.ifBlank { route.sourceCity },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = calculateTripDuration(route.departureTime, route.arrivalTime, route.durationHours),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = RedbusCrimson, modifier = Modifier.size(16.dp))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = route.arrivalTime.take(5),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = route.destinationCity,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.selectedDroppingPoint.ifBlank { route.destinationCity },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${uiState.selectedSeats.size} Seat(s) | ${uiState.selectedSeats.joinToString { it.seatNumber }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedbusCrimson
                                )
                                Text(
                                    text = route.travelDate,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Contact Details Card (Exact match to Video 1 vid1_f2.jpg)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Contact Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ticket details will be sent to",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = "Edit",
                                color = RedbusCrimson,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clickable {
                                        editContactPhone = uiState.contactPhone
                                        editContactEmail = uiState.contactEmail
                                        editStateOfResidence = uiState.stateOfResidence.ifBlank { "Karnataka" }
                                        showEditContactDialog = true
                                    }
                                    .padding(4.dp)
                            )
                        }

                        val rawPhone = uiState.contactPhone.ifBlank { "8939129572" }.removePrefix("+91").trim()
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Text(
                                text = "+91 $rawPhone",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Text(
                                text = uiState.contactEmail.ifBlank { "jaiganeshrio474@gmail.com" },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.clickable {
                                editContactPhone = uiState.contactPhone
                                editContactEmail = uiState.contactEmail
                                editStateOfResidence = uiState.stateOfResidence.ifBlank { "Karnataka" }
                                showEditContactDialog = true
                            }
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Text(
                                text = uiState.stateOfResidence.ifBlank { "Karnataka" },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // WhatsApp updates pill (Video 1)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "WhatsApp communication enabled",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Passenger Details Card (Exact match to Video 1 vid1_f2.jpg)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val totalSeats = if (uiState.selectedSeats.isNotEmpty()) uiState.selectedSeats.size else 1
                        val filledCount = uiState.passengers.count { it.name.isNotBlank() }

                        Column {
                            Text(
                                text = "Passenger details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$filledCount/$totalSeats Selected",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Button: + Add new passenger (Full-width pill above list, matching Video 1)
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFFFEBEE),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    newPassengerName = ""
                                    newPassengerAge = ""
                                    newPassengerGender = "MALE"
                                    showAddPassengerDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add new passenger",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        // Saved travellers list with Checkboxes and Circular Avatars (Video 1)
                        val displayTravellers = if (uiState.savedTravellers.isNotEmpty()) {
                            uiState.savedTravellers
                        } else {
                            listOf(
                                com.redbus.ai.data.model.SavedTraveller(id = 1, name = "Jaiganesh", age = 23, gender = "MALE"),
                                com.redbus.ai.data.model.SavedTraveller(id = 2, name = "Krishnaveni", age = 65, gender = "FEMALE")
                            )
                        }

                        displayTravellers.forEachIndexed { tIdx, traveller ->
                            val isAssigned = uiState.passengers.any { it.name.equals(traveller.name, ignoreCase = true) }
                            val isFemale = traveller.gender.startsWith("F", ignoreCase = true)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isAssigned) {
                                            val pIdx = uiState.passengers.indexOfFirst { it.name.equals(traveller.name, ignoreCase = true) }
                                            if (pIdx >= 0) {
                                                onUpdatePassenger(pIdx, "", 0, "MALE")
                                            }
                                        } else {
                                            val pIdx = uiState.passengers.indexOfFirst { it.name.isBlank() }
                                            if (pIdx >= 0) {
                                                onUpdatePassenger(pIdx, traveller.name, traveller.age, traveller.gender)
                                            } else if (uiState.passengers.isNotEmpty()) {
                                                onUpdatePassenger(0, traveller.name, traveller.age, traveller.gender)
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Circular Avatar: Light Purple for Male, Light Pink for Female
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isFemale) Color(0xFFFCE7F3) else Color(0xFFEDE9FE),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (isFemale) Color(0xFFBE185D) else Color(0xFF4338CA),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = traveller.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val genderLabel = if (isFemale) "Female" else "Male"
                                        Text(
                                            text = "$genderLabel, ${traveller.age} Years",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Checkbox(
                                    checked = isAssigned,
                                    onCheckedChange = { checked ->
                                        if (!checked) {
                                            val pIdx = uiState.passengers.indexOfFirst { it.name.equals(traveller.name, ignoreCase = true) }
                                            if (pIdx >= 0) {
                                                onUpdatePassenger(pIdx, "", 0, "MALE")
                                            }
                                        } else {
                                            val pIdx = uiState.passengers.indexOfFirst { it.name.isBlank() }
                                            if (pIdx >= 0) {
                                                onUpdatePassenger(pIdx, traveller.name, traveller.age, traveller.gender)
                                            } else if (uiState.passengers.isNotEmpty()) {
                                                onUpdatePassenger(0, traveller.name, traveller.age, traveller.gender)
                                            }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = RedbusCrimson,
                                        uncheckedColor = Color(0xFFCBD5E1)
                                    )
                                )
                            }

                            if (tIdx < displayTravellers.size - 1) {
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            }
                        }
                    }
                }
            }

            // 3. Trip Protection Add-ons (Matching 1st and 2nd images)

            item {

                Card(

                    shape = RoundedCornerShape(18.dp),

                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),

                    modifier = Modifier.fillMaxWidth()

                ) {

                    Column(

                        modifier = Modifier.padding(18.dp),

                        verticalArrangement = Arrangement.spacedBy(14.dp)

                    ) {

                        Text(

                            text = "Trip Protection Add-ons",

                            style = MaterialTheme.typography.titleLarge

                        )



                        // --- Free Cancellation Guarantee ---

                        Column(

                            modifier = Modifier

                                .fillMaxWidth()

                                .background(Color(0xFFF0FDF4), RoundedCornerShape(14.dp))

                                .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(14.dp))

                                .padding(14.dp),

                            verticalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Row(

                                    verticalAlignment = Alignment.CenterVertically,

                                    horizontalArrangement = Arrangement.spacedBy(8.dp),

                                    modifier = Modifier.weight(1f)

                                ) {

                                    Surface(

                                        shape = RoundedCornerShape(8.dp),

                                        color = Color(0xFFDCFCE7),

                                        modifier = Modifier.size(32.dp)

                                    ) {

                                        Box(contentAlignment = Alignment.Center) {

                                            Icon(

                                                imageVector = Icons.Default.Shield,

                                                contentDescription = null,

                                                tint = Color(0xFF15803D),

                                                modifier = Modifier.size(18.dp)

                                            )

                                        }

                                    }

                                    Column {

                                        Row(

                                            verticalAlignment = Alignment.CenterVertically,

                                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                                        ) {

                                            Text(

                                                text = "Free Cancellation",

                                                fontWeight = FontWeight.Bold,

                                                fontSize = 14.sp,

                                                color = Color(0xFF0F172A)

                                            )

                                            Surface(

                                                color = Color(0xFF0D9488),

                                                shape = RoundedCornerShape(6.dp)

                                            ) {

                                                Text(

                                                    text = "POPULAR",

                                                    color = Color.White,

                                                    fontSize = 9.sp,

                                                    fontWeight = FontWeight.Black,

                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)

                                                )

                                            }

                                        }

                                        Text(

                                            text = "₹21/seat",

                                            fontSize = 11.sp,

                                            fontWeight = FontWeight.Bold,

                                            color = Color(0xFF15803D)

                                        )

                                    }

                                }



                                RedbusStyledSwitch(

                                    checked = uiState.hasFreeCancellation,

                                    onCheckedChange = onToggleFreeCancellation

                                )

                            }



                            Text(

                                text = "Get 100% full refund of ticket price if you cancel before departure time!",

                                fontSize = 12.sp,

                                fontWeight = FontWeight.Medium,

                                color = Color(0xFF166534),

                                lineHeight = 16.sp

                            )

                            Text(

                                text = "100% base fare refunded (₹${fareBreakdown.baseFare.toInt()}) if cancelled anytime before trip start time.",

                                fontSize = 11.sp,

                                color = Color(0xFF475569)

                            )

                        }



                        // --- redBus Trip Guarantee ---

                        Column(

                            modifier = Modifier

                                .fillMaxWidth()

                                .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))

                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))

                                .padding(14.dp),

                            verticalArrangement = Arrangement.spacedBy(10.dp)

                        ) {

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Row(

                                    verticalAlignment = Alignment.CenterVertically,

                                    horizontalArrangement = Arrangement.spacedBy(8.dp),

                                    modifier = Modifier.weight(1f)

                                ) {

                                    Surface(

                                        shape = RoundedCornerShape(8.dp),

                                        color = RedbusCrimson.copy(alpha = 0.1f),

                                        modifier = Modifier.size(32.dp)

                                    ) {

                                        Box(contentAlignment = Alignment.Center) {

                                            Icon(

                                                imageVector = Icons.Default.Security,

                                                contentDescription = null,

                                                tint = RedbusCrimson,

                                                modifier = Modifier.size(18.dp)

                                            )

                                        }

                                    }

                                    Column {

                                        Text(

                                            text = "Trip Guarantee",

                                            fontWeight = FontWeight.Bold,

                                            fontSize = 14.sp,

                                            color = Color(0xFF0F172A)

                                        )

                                        Text(

                                            text = "₹21 per passenger",

                                            fontSize = 11.sp,

                                            fontWeight = FontWeight.SemiBold,

                                            color = Color(0xFF64748B)

                                        )

                                    }

                                }



                                RedbusStyledSwitch(

                                    checked = uiState.hasTripGuarantee,

                                    onCheckedChange = onToggleTripGuarantee

                                )

                            }



                            Text(

                                text = "Get a full refund + ₹230 extra if your bus gets cancelled by the operator.",

                                fontSize = 12.sp,

                                color = Color(0xFF334155),

                                lineHeight = 16.sp

                            )



                            // Visual refund preview box (Matching 1st image)

                            Surface(

                                shape = RoundedCornerShape(12.dp),

                                color = Color(0xFFF0F4FF),

                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCE6FF)),

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                Column(

                                    modifier = Modifier.padding(12.dp),

                                    horizontalAlignment = Alignment.CenterHorizontally,

                                    verticalArrangement = Arrangement.spacedBy(6.dp)

                                ) {

                                    Text(

                                        text = "Get ₹${fareBreakdown.baseFare.toInt() + 230} refund if bus gets cancelled",

                                        fontSize = 11.sp,

                                        fontWeight = FontWeight.Bold,

                                        color = Color(0xFF1E3A8A)

                                    )



                                    Row(

                                        verticalAlignment = Alignment.CenterVertically,

                                        horizontalArrangement = Arrangement.spacedBy(16.dp)

                                    ) {

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                            Text(

                                                text = "🚌 ₹${fareBreakdown.baseFare.toInt()}",

                                                fontSize = 14.sp,

                                                fontWeight = FontWeight.Black,

                                                color = Color(0xFF1E40AF)

                                            )

                                            Text(

                                                text = "Full refund",

                                                fontSize = 10.sp,

                                                color = Color(0xFF64748B)

                                            )

                                        }



                                        Text(

                                            text = "+",

                                            fontSize = 16.sp,

                                            fontWeight = FontWeight.Bold,

                                            color = Color(0xFF3B82F6)

                                        )



                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                            Text(

                                                text = "💳 ₹230",

                                                fontSize = 14.sp,

                                                fontWeight = FontWeight.Black,

                                                color = Color(0xFF1E40AF)

                                            )

                                            Text(

                                                text = "Cashback",

                                                fontSize = 10.sp,

                                                color = Color(0xFF64748B)

                                            )

                                        }

                                    }

                                }

                            }



                            // Social proof pill (Matching 1st image)

                            Surface(

                                shape = RoundedCornerShape(8.dp),

                                color = Color(0xFFECFDF5),

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                Text(

                                    text = if (uiState.tripGuaranteeBuyers > 0) {
                                        "Bought by ${uiState.tripGuaranteeBuyers} passengers this month"
                                    } else {
                                        "Selected by live passengers this month"
                                    },

                                    fontSize = 11.sp,

                                    fontWeight = FontWeight.Medium,

                                    color = Color(0xFF047857),

                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)

                                )

                            }

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



    

    // SLIDE-UP MODAL: BUS BOOKING DETAILS (Matching Images 3, 4, 5)

    if (showBookingDetailsModal) {

        var selectedDetailsTab by remember { mutableStateOf("trip") }



        Dialog(

            onDismissRequest = { showBookingDetailsModal = false },

            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)

        ) {

            Surface(

                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),

                shape = RectangleShape,

                color = MaterialTheme.colorScheme.background

            ) {

                Column(modifier = Modifier.fillMaxSize()) {

                    // Top Bar: Pay ₹XXX with Back Arrow & Lock Countdown Timer (Images 3, 4, 5)

                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .background(Color(0xFFF8FAFC))

                            .padding(horizontal = 14.dp, vertical = 10.dp),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            IconButton(

                                onClick = { showBookingDetailsModal = false },

                                modifier = Modifier.size(32.dp)

                            ) {

                                Icon(

                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,

                                    contentDescription = "Back",

                                    tint = Color(0xFF0F172A),

                                    modifier = Modifier.size(20.dp)

                                )

                            }

                            Text(

                                text = "Pay ₹${fareBreakdown.netPayable.toInt()}",

                                fontWeight = FontWeight.Black,

                                fontSize = 16.sp,

                                color = Color(0xFF0F172A)

                            )

                        }



                        Surface(

                            color = Color(0xFFFFEDD5),

                            shape = RoundedCornerShape(8.dp)

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(4.dp)

                            ) {

                                Icon(

                                    imageVector = Icons.Default.Timer,

                                    contentDescription = null,

                                    tint = Color(0xFFC2410C),

                                    modifier = Modifier.size(13.dp)

                                )

                                Text(

                                    text = formattedLockTimer,

                                    fontWeight = FontWeight.Black,

                                    fontSize = 11.sp,

                                    color = Color(0xFFC2410C)

                                )

                            }

                        }

                    }



                    // Title Header: Bus booking details and Close X button

                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(horizontal = 18.dp, vertical = 12.dp),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Text(

                            text = "Bus booking details",

                            fontWeight = FontWeight.Black,

                            fontSize = 18.sp,

                            color = Color(0xFF0F172A)

                        )

                        IconButton(

                            onClick = { showBookingDetailsModal = false },

                            modifier = Modifier.size(32.dp)

                        ) {

                            Icon(

                                imageVector = Icons.Default.Close,

                                contentDescription = "Close",

                                tint = Color(0xFF64748B)

                            )

                        }

                    }



                    // Horizontal Tabs Bar (Trip info, Cancellation policy, Passenger info, Fare breakup)

                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(horizontal = 14.dp),

                        horizontalArrangement = Arrangement.SpaceBetween

                    ) {

                        listOf(

                            "trip" to "Trip info",

                            "cancellation" to "Cancellation policy",

                            "passenger" to "Passenger info",

                            "fare" to "Fare breakup"

                        ).forEach { (tabId, label) ->

                            val isSelected = selectedDetailsTab == tabId

                            Column(

                                modifier = Modifier

                                    .weight(1f)

                                    .clickable { selectedDetailsTab = tabId }

                                    .padding(vertical = 8.dp),

                                horizontalAlignment = Alignment.CenterHorizontally

                            ) {

                                Text(

                                    text = label,

                                    fontSize = 11.sp,

                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,

                                    color = if (isSelected) RedbusCrimson else Color(0xFF64748B),

                                    maxLines = 1

                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                if (isSelected) {

                                    Box(

                                        modifier = Modifier

                                            .height(3.dp)

                                            .fillMaxWidth(0.8f)

                                            .background(RedbusCrimson, RoundedCornerShape(2.dp))

                                    )

                                } else {

                                    Spacer(modifier = Modifier.height(3.dp))

                                }

                            }

                        }

                    }



                    HorizontalDivider(color = Color(0xFFE2E8F0))



                    // Tab Body

                    LazyColumn(

                        modifier = Modifier

                            .weight(1f)

                            .fillMaxWidth(),

                        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 80.dp),

                        verticalArrangement = Arrangement.spacedBy(16.dp)

                    ) {

                        // TAB 1: TRIP INFO (Image 3 top)

                        if (selectedDetailsTab == "trip" || selectedDetailsTab == "cancellation") {

                            item {

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                                    Text(

                                        text = route.operatorName,

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 16.sp,

                                        color = Color(0xFF0F172A)

                                    )

                                    Text(

                                        text = route.busType,

                                        fontSize = 12.sp,

                                        color = Color(0xFF64748B)

                                    )



                                    Spacer(modifier = Modifier.height(8.dp))



                                    // Timeline Departure -> Arrival

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        verticalAlignment = Alignment.Top

                                    ) {

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                            Box(

                                                modifier = Modifier

                                                    .size(10.dp)

                                                    .background(Color(0xFF64748B), RoundedCornerShape(5.dp))

                                            )

                                            Box(

                                                modifier = Modifier

                                                    .width(2.dp)

                                                    .height(48.dp)

                                                    .background(Color(0xFFE2E8F0))

                                            )

                                            Box(

                                                modifier = Modifier

                                                    .size(10.dp)

                                                    .background(Color(0xFF64748B), RoundedCornerShape(5.dp))

                                            )

                                        }



                                        Spacer(modifier = Modifier.width(12.dp))



                                        Column(

                                            modifier = Modifier.weight(1f),

                                            verticalArrangement = Arrangement.SpaceBetween

                                        ) {

                                            Row(

                                                modifier = Modifier.fillMaxWidth(),

                                                horizontalArrangement = Arrangement.SpaceBetween

                                            ) {

                                                Column {

                                                    Text(

                                                        text = route.departureTime,

                                                        fontWeight = FontWeight.Bold,

                                                        fontSize = 14.sp,

                                                        color = Color(0xFF0F172A)

                                                    )

                                                    Text(

                                                        text = route.travelDate ?: "Today",

                                                        fontSize = 11.sp,

                                                        color = Color(0xFF64748B)

                                                    )

                                                }

                                                Text(

                                                    text = uiState.selectedBoardingPoint.ifBlank { "${route.sourceCity} Terminal" },

                                                    fontWeight = FontWeight.SemiBold,

                                                    fontSize = 13.sp,

                                                    color = Color(0xFF0F172A)

                                                )

                                            }



                                            Text(

                                                text = if (route.durationHours > 0) "${route.durationHours}h" else "6h 5m",

                                                fontSize = 11.sp,

                                                color = Color(0xFF94A3B8),

                                                modifier = Modifier.padding(vertical = 4.dp)

                                            )



                                            Row(

                                                modifier = Modifier.fillMaxWidth(),

                                                horizontalArrangement = Arrangement.SpaceBetween

                                            ) {

                                                Column {

                                                    Text(

                                                        text = route.arrivalTime,

                                                        fontWeight = FontWeight.Bold,

                                                        fontSize = 14.sp,

                                                        color = Color(0xFF0F172A)

                                                    )

                                                    Text(

                                                        text = "Next Day",

                                                        fontSize = 11.sp,

                                                        color = Color(0xFF64748B)

                                                    )

                                                }

                                                Text(

                                                    text = uiState.selectedDroppingPoint.ifBlank { "${route.destinationCity} Terminal" },

                                                    fontWeight = FontWeight.SemiBold,

                                                    fontSize = 13.sp,

                                                    color = Color(0xFF0F172A)

                                                )

                                            }

                                        }

                                    }

                                }

                            }

                        }



                        // TAB 2: CANCELLATION POLICY (Image 3 bottom)

                        if (selectedDetailsTab == "cancellation") {

                            item {

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                                    Text(

                                        text = "Cancellation policy",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 15.sp,

                                        color = Color(0xFF0F172A)

                                    )



                                    // Cancellation Table (Image 3)

                                    Surface(

                                        shape = RoundedCornerShape(12.dp),

                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),

                                        color = Color.White

                                    ) {

                                        Column {

                                            Row(

                                                modifier = Modifier

                                                    .fillMaxWidth()

                                                    .background(Color(0xFFF8FAFC))

                                                    .padding(12.dp),

                                                horizontalArrangement = Arrangement.SpaceBetween

                                            ) {

                                                Text("Time window", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))

                                                Text("Refund", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))

                                            }

                                            HorizontalDivider(color = Color(0xFFE2E8F0))



                                            listOf(

                                                "Up to 96h before departure" to "85%",

                                                "48h before departure" to "70%",

                                                "24h before departure" to "25%",

                                                "Less than 24h before departure" to "5%"

                                            ).forEachIndexed { i, (time, ref) ->

                                                Row(

                                                    modifier = Modifier

                                                        .fillMaxWidth()

                                                        .padding(horizontal = 12.dp, vertical = 10.dp),

                                                    horizontalArrangement = Arrangement.SpaceBetween,

                                                    verticalAlignment = Alignment.CenterVertically

                                                ) {

                                                    Text(time, fontSize = 12.sp, color = Color(0xFF334155))

                                                    Text(ref, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                                }

                                                if (i < 3) HorizontalDivider(color = Color(0xFFF1F5F9))

                                            }

                                        }

                                    }



                                    // Footnotes (Image 3)

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                                        Text("* Cancellation charges are computed on a per seat basis. Above cancellation fare is calculated based on seat fare of ₹${fareBreakdown.baseFare.toInt()}", fontSize = 10.sp, color = Color(0xFF64748B))

                                        Text("* Cancellation charges are calculated based on service start date + time at :${route.departureTime}", fontSize = 10.sp, color = Color(0xFF64748B))

                                        Text("* Ticket cannot be cancelled after scheduled bus departure time from the first boarding point", fontSize = 10.sp, color = Color(0xFF64748B))

                                        Text("* Note: Cancellation charges mentioned above are excluding GST", fontSize = 10.sp, color = Color(0xFF64748B))

                                    }

                                }

                            }

                        }



                        // TAB 3: PASSENGER INFO (Image 4)

                        if (selectedDetailsTab == "passenger" || selectedDetailsTab == "trip") {

                            item {

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                                    Text(

                                        text = "Passengers",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 15.sp,

                                        color = Color(0xFF0F172A)

                                    )

                                    Text(

                                        text = "${uiState.passengers.size} seat",

                                        fontSize = 11.sp,

                                        color = Color(0xFF64748B)

                                    )



                                    uiState.passengers.forEach { p ->

                                        Row(

                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement = Arrangement.SpaceBetween,

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Column {

                                                Text(

                                                    text = p.name.ifBlank { "Passenger" },

                                                    fontWeight = FontWeight.Bold,

                                                    fontSize = 14.sp,

                                                    color = Color(0xFF0F172A)

                                                )

                                                Text(

                                                    text = "${p.gender}, ${p.age} years",

                                                    fontSize = 12.sp,

                                                    color = Color(0xFF64748B)

                                                )

                                            }



                                            Surface(

                                                shape = RoundedCornerShape(8.dp),

                                                color = Color(0xFFFCE7F3)

                                            ) {

                                                Text(

                                                    text = p.seatNumber,

                                                    fontSize = 12.sp,

                                                    fontWeight = FontWeight.Black,

                                                    color = Color(0xFFBE185D),

                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)

                                                )

                                            }

                                        }

                                    }



                                    Spacer(modifier = Modifier.height(6.dp))



                                    // Realistic Landscape Seat Map Visual matching Image 4
                                    LandscapeSeatMapVisual(selectedSeats = uiState.selectedSeats)

                                    Spacer(modifier = Modifier.height(6.dp))



                                    // Passenger Contact Details

                                    Text(

                                        text = "Passenger contact details",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 14.sp,

                                        color = Color(0xFF0F172A)

                                    )

                                    Text(

                                        text = "We'll send ticket updates here",

                                        fontSize = 11.sp,

                                        color = Color(0xFF64748B)

                                    )

                                    Text(

                                        text = uiState.contactEmail.ifBlank { "support@redbus.ai" },

                                        fontSize = 13.sp,

                                        fontWeight = FontWeight.SemiBold,

                                        color = Color(0xFF334155)

                                    )

                                    Text(

                                        text = uiState.contactPhone.ifBlank { "+91 9189391295" },

                                        fontSize = 13.sp,

                                        fontWeight = FontWeight.SemiBold,

                                        color = Color(0xFF334155)

                                    )

                                }

                            }

                        }



                        // TAB 4: FARE BREAKUP & PROCEED TO PAY (Image 5)

                        if (selectedDetailsTab == "fare" || selectedDetailsTab == "passenger" || selectedDetailsTab == "trip") {

                            item {

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    Text(

                                        text = "Price breakup",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 15.sp,

                                        color = Color(0xFF0F172A)

                                    )

                                    Text(

                                        text = "${uiState.passengers.size} seat",

                                        fontSize = 11.sp,

                                        color = Color(0xFF64748B)

                                    )



                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.SpaceBetween

                                    ) {

                                        Text("Base Fare", fontSize = 13.sp, color = Color(0xFF475569))

                                        Text("₹${fareBreakdown.baseFare.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                    }



                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.SpaceBetween

                                    ) {

                                        Text("Operator Service Fee", fontSize = 13.sp, color = Color(0xFF475569))

                                        Text("+₹${fareBreakdown.gstAndService.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                    }



                                    if (uiState.hasFreeCancellation) {

                                        Row(

                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement = Arrangement.SpaceBetween

                                        ) {

                                            Text("Free Cancellation Guarantee", fontSize = 13.sp, color = Color(0xFF15803D))

                                            Text("+₹${fareBreakdown.freeCancelFee.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))

                                        }

                                    }



                                    if (uiState.hasTripGuarantee) {

                                        Row(

                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement = Arrangement.SpaceBetween

                                        ) {

                                            Text("redBus Trip Guarantee", fontSize = 13.sp, color = RedbusCrimson)

                                            Text("+₹${fareBreakdown.tripGuaranteeFee.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RedbusCrimson)

                                        }

                                    }



                                    if (fareBreakdown.discountAmount > 0) {

                                        Row(

                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement = Arrangement.SpaceBetween

                                        ) {

                                            Text("Coupon Discount", fontSize = 13.sp, color = Color(0xFF15803D))

                                            Text("-₹${fareBreakdown.discountAmount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))

                                        }

                                    }



                                    if (fareBreakdown.walletUsed > 0) {

                                        Row(

                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement = Arrangement.SpaceBetween

                                        ) {

                                            Text("Wallet Balance Applied", fontSize = 13.sp, color = Color(0xFF0D9488))

                                            Text("-₹${fareBreakdown.walletUsed.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))

                                        }

                                    }



                                    HorizontalDivider(color = Color(0xFFCBD5E1))



                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.SpaceBetween,

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Text("Total", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF0F172A))

                                        Text("₹${fareBreakdown.netPayable.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = RedbusCrimson)

                                    }



                                    Spacer(modifier = Modifier.height(14.dp))



                                    // PROCEED TO PAY BUTTON (Image 5)

                                    Button(

                                        onClick = {

                                            showBookingDetailsModal = false

                                            onConfirmAndPay()

                                        },

                                        modifier = Modifier

                                            .fillMaxWidth()

                                            .height(52.dp),

                                        shape = RoundedCornerShape(14.dp),

                                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)

                                    ) {

                                        Icon(

                                            imageVector = Icons.Default.Lock,

                                            contentDescription = null,

                                            modifier = Modifier.size(18.dp)

                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(

                                            text = "PROCEED TO PAY (₹${fareBreakdown.netPayable.toInt()})",

                                            fontWeight = FontWeight.Black,

                                            fontSize = 14.sp

                                        )

                                    }



                                    Spacer(modifier = Modifier.height(28.dp))

                                }

                            }

                        }



                        item {

                            Spacer(modifier = Modifier.height(48.dp))

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

// Edit Contact Dialog (Image 4)
    if (showEditContactDialog) {
        AlertDialog(
            onDismissRequest = { showEditContactDialog = false },
            title = { Text("Edit Contact Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editContactPhone,
                        onValueChange = { editContactPhone = it },
                        label = { Text("Mobile Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editContactEmail,
                        onValueChange = { editContactEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editStateOfResidence,
                        onValueChange = { editStateOfResidence = it },
                        label = { Text("State of Residence") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateContact(editContactEmail, editContactPhone, editStateOfResidence)
                        showEditContactDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditContactDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add New Passenger Dialog (Image 4 & 5)
    if (showAddPassengerDialog) {
        AlertDialog(
            onDismissRequest = { showAddPassengerDialog = false },
            title = { Text("Add New Passenger", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPassengerName,
                        onValueChange = { newPassengerName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassengerAge,
                        onValueChange = { newPassengerAge = it },
                        label = { Text("Age") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gender:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        FilterChip(
                            selected = newPassengerGender == "MALE",
                            onClick = { newPassengerGender = "MALE" },
                            label = { Text("Male") }
                        )
                        FilterChip(
                            selected = newPassengerGender == "FEMALE",
                            onClick = { newPassengerGender = "FEMALE" },
                            label = { Text("Female") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassengerName.isNotBlank()) {
                            val age = newPassengerAge.toIntOrNull() ?: 25
                            val trimmedName = newPassengerName.trim()
                            onAddTraveller(trimmedName, age, newPassengerGender)
                            // Assign to first empty passenger seat
                            val emptyIdx = uiState.passengers.indexOfFirst { it.name.isBlank() }
                            if (emptyIdx >= 0) {
                                onUpdatePassenger(emptyIdx, trimmedName, age, newPassengerGender)
                            } else if (uiState.passengers.isNotEmpty()) {
                                onUpdatePassenger(0, trimmedName, age, newPassengerGender)
                            }
                            showAddPassengerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Add Passenger")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPassengerDialog = false }) {
                    Text("Cancel")
                }
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

    var activePopupUrl by remember { mutableStateOf<String?>(null) }

    var popupWebViewRef by remember { mutableStateOf<WebView?>(null) }

    var containerRef by remember { mutableStateOf<FrameLayout?>(null) }

    var hasCompletedCallback by remember(payment.orderId) { mutableStateOf(false) }



    val effectiveKeyId = payment.keyId.ifBlank { "rzp_test_TdqUpSHbhNlafI" }



    val checkoutHtml = remember(payment, effectiveKeyId) {

        val safeName = JSONObject.quote(payment.passengerName)

        val safeEmail = JSONObject.quote(payment.contactEmail)

        val safePhone = JSONObject.quote(payment.contactPhone)

        val safePnr = JSONObject.quote(payment.pnr)

        val safeOrderId = JSONObject.quote(payment.orderId)

        val safeKey = JSONObject.quote(effectiveKeyId)



        """

        <!DOCTYPE html>

        <html>

        <head>

            <meta charset="utf-8" />

            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover" />

            <title>Razorpay Secure Checkout - redBus India</title>

            <script src="https://checkout.razorpay.com/v1/checkout.js"></script>

            <style>

                * { box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }

                html, body {

                    margin: 0;

                    padding: 0;

                    width: 100%;

                    height: 100%;

                    background: transparent;

                    overflow: hidden;

                    -webkit-text-size-adjust: 100%;

                }

                iframe {

                    max-width: 100% !important;

                    max-height: 100% !important;

                }

            </style>

        </head>

        <body>

            <script>

                var paymentHandled = false;



                // Relay any postMessage from Razorpay Test Bank popup to all child iframes and check for direct success/failure

                window.addEventListener('message', function(event) {

                    try {

                        for (var i = 0; i < window.frames.length; i++) {

                            try {

                                window.frames[i].postMessage(event.data, '*');

                            } catch (fe) {}

                        }

                        var payload = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;

                        if (payload) {

                            var resp = payload.response || payload.data || payload;

                            if (resp && resp.razorpay_payment_id && resp.razorpay_signature && !paymentHandled) {

                                paymentHandled = true;

                                if (window.RazorpayBridge) {

                                    window.RazorpayBridge.onPaymentSuccess(

                                        resp.razorpay_order_id || $safeOrderId,

                                        resp.razorpay_payment_id,

                                        resp.razorpay_signature

                                    );

                                }

                            } else if (resp && resp.error && resp.error.description) {

                                if (window.RazorpayBridge) {

                                    window.RazorpayBridge.onGatewayError(resp.error.description);

                                }

                            }

                        }

                    } catch (err) {}

                }, false);



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

                            if (!paymentHandled && window.RazorpayBridge) {

                                paymentHandled = true;

                                window.RazorpayBridge.onPaymentSuccess(

                                    response.razorpay_order_id || $safeOrderId,

                                    response.razorpay_payment_id || "",

                                    response.razorpay_signature || ""

                                );

                            }

                        },

                        modal: {

                            ondismiss: function () {

                                if (!paymentHandled && window.RazorpayBridge) {

                                    window.RazorpayBridge.onPaymentDismissed();

                                }

                            }

                        }

                    };

                    try {

                        var rzp = new window.Razorpay(options);

                        rzp.on('payment.failed', function (resp) {

                            var desc = (resp && resp.error && resp.error.description)

                                ? resp.error.description

                                : "Payment was declined in Razorpay Test Mode. You can retry or select Success.";

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

                    setTimeout(launchRazorpay, 120);

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

            decorFitsSystemWindows = true

        )

    ) {

        Column(

            modifier = Modifier

                .fillMaxSize()

                .background(Color.Black.copy(alpha = 0.55f))

                .statusBarsPadding()

                .navigationBarsPadding()

        ) {

            // When the Razorpay Test Bank Popup (`api.razorpay.com/v1/gateway/mocksharp/payment`) is open,

            // show the exact Test Bank URL bar + Test Mode badge at the top (matching Image 4)

            if (activePopupUrl != null) {

                Surface(

                    color = Color(0xFF1E293B),

                    modifier = Modifier.fillMaxWidth()

                ) {

                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(horizontal = 12.dp, vertical = 8.dp),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.SpaceBetween

                    ) {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            modifier = Modifier.weight(1f)

                        ) {

                            Icon(

                                imageVector = Icons.Default.Lock,

                                contentDescription = null,

                                tint = Color(0xFF34D399),

                                modifier = Modifier.size(14.dp)

                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(

                                text = activePopupUrl ?: "api.razorpay.com/v1/gateway/mocksharp/payment",

                                color = Color.White,

                                fontSize = 11.sp,

                                maxLines = 1

                            )

                        }



                        Spacer(modifier = Modifier.width(8.dp))



                        Surface(

                            color = RedbusCrimson,

                            shape = RoundedCornerShape(6.dp)

                        ) {

                            Text(

                                text = "TEST MODE",

                                color = Color.White,

                                fontSize = 10.sp,

                                fontWeight = FontWeight.Black,

                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)

                            )

                        }



                        IconButton(

                            onClick = {

                                popupWebViewRef?.let { pop ->

                                    containerRef?.removeView(pop)

                                    pop.destroy()

                                }

                                popupWebViewRef = null

                                activePopupUrl = null

                            },

                            modifier = Modifier.size(28.dp)

                        ) {

                            Icon(

                                imageVector = Icons.Default.Close,

                                contentDescription = "Close Test Bank Window",

                                tint = Color.White,

                                modifier = Modifier.size(16.dp)

                            )

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

                            setBackgroundColor(android.graphics.Color.TRANSPARENT)

                        }

                        containerRef = container



                        lateinit var primaryWebView: WebView



                        val bridgeObject = object {

                            @JavascriptInterface

                            fun onGatewayReady() {

                                mainHandler.post { isLoadingGateway = false }

                            }



                            @JavascriptInterface

                            fun onPaymentSuccess(orderId: String, paymentId: String, signature: String) {

                                mainHandler.post {

                                    if (!hasCompletedCallback && paymentId.isNotBlank() && signature.isNotBlank()) {

                                        hasCompletedCallback = true

                                        popupWebViewRef?.let { pop ->

                                            container.removeView(pop)

                                        }

                                        popupWebViewRef = null

                                        activePopupUrl = null

                                        onPaymentSuccess(

                                            orderId.ifBlank { payment.orderId },

                                            paymentId,

                                            signature

                                        )

                                    }

                                }

                            }



                            @JavascriptInterface

                            fun onPaymentDismissed() {

                                mainHandler.post {

                                    if (!isVerifying && !hasCompletedCallback) onDismiss()

                                }

                            }



                            @JavascriptInterface

                            fun onGatewayError(message: String) {

                                mainHandler.post {

                                    isLoadingGateway = false

                                    popupWebViewRef?.let { pop ->

                                        container.removeView(pop)

                                    }

                                    popupWebViewRef = null

                                    activePopupUrl = null

                                    gatewayError = message

                                }

                            }



                            @JavascriptInterface

                            fun onForwardPopupMessage(rawJson: String) {

                                mainHandler.post {

                                    val quoted = JSONObject.quote(rawJson)

                                    primaryWebView.evaluateJavascript(

                                        """

                                        (function() {

                                            try {

                                                var msg = $quoted;

                                                window.postMessage(msg, '*');

                                                for (var i = 0; i < window.frames.length; i++) {

                                                    try { window.frames[i].postMessage(msg, '*'); } catch(e) {}

                                                }

                                            } catch(e) {}

                                        })();

                                        """.trimIndent(),

                                        null

                                    )

                                }

                            }

                        }



                        primaryWebView = WebView(context).apply {

                            layoutParams = FrameLayout.LayoutParams(

                                ViewGroup.LayoutParams.MATCH_PARENT,

                                ViewGroup.LayoutParams.MATCH_PARENT

                            )

                            setBackgroundColor(android.graphics.Color.TRANSPARENT)

                            settings.apply {

                                javaScriptEnabled = true

                                domStorageEnabled = true

                                databaseEnabled = true

                                javaScriptCanOpenWindowsAutomatically = true

                                setSupportMultipleWindows(true)

                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                                loadWithOverviewMode = false

                                useWideViewPort = false

                                textZoom = 100

                                // Strip Android WebView '; wv' marker so Razorpay checkout.js runs in standard web mode

                                // and opens https://api.razorpay.com/v1/gateway/mocksharp/payment (Success / Failure test page)

                                userAgentString = userAgentString

                                    .replace("; wv", "")

                                    .replace(Regex("Version/\\d+\\.\\d+\\s*"), "")

                            }

                            setInitialScale(100)



                            CookieManager.getInstance().setAcceptCookie(true)

                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)



                            addJavascriptInterface(bridgeObject, "RazorpayBridge")



                            webViewClient = object : WebViewClient() {

                                override fun shouldOverrideUrlLoading(

                                    view: WebView?,

                                    request: WebResourceRequest?

                                ): Boolean {

                                    val url = request?.url?.toString().orEmpty()

                                    if (url.startsWith("http://") || url.startsWith("https://")) {

                                        return false

                                    }

                                    return true

                                }

                            }



                            // Support Razorpay Test Bank Popup Windows (api.razorpay.com/v1/gateway/mocksharp/payment)

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

                                        setBackgroundColor(android.graphics.Color.WHITE)

                                        settings.apply {

                                            javaScriptEnabled = true

                                            domStorageEnabled = true

                                            databaseEnabled = true

                                            javaScriptCanOpenWindowsAutomatically = true

                                            setSupportMultipleWindows(true)

                                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                                            loadWithOverviewMode = false

                                            useWideViewPort = false

                                            textZoom = 100

                                            builtInZoomControls = false

                                            displayZoomControls = false

                                            userAgentString = userAgentString

                                                .replace("; wv", "")

                                                .replace(Regex("Version/\\d+\\.\\d+\\s*"), "")

                                        }

                                        setInitialScale(100)

                                        CookieManager.getInstance().setAcceptCookie(true)

                                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                        addJavascriptInterface(bridgeObject, "RazorpayBridge")



                                        webViewClient = object : WebViewClient() {

                                            override fun shouldOverrideUrlLoading(

                                                v: WebView?,

                                                req: WebResourceRequest?

                                            ): Boolean {

                                                val targetUrl = req?.url?.toString().orEmpty()

                                                if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {

                                                    mainHandler.post { activePopupUrl = targetUrl }

                                                    return false

                                                }

                                                return true

                                            }



                                            override fun onPageStarted(

                                                v: WebView?,

                                                url: String?,

                                                favicon: android.graphics.Bitmap?

                                            ) {

                                                super.onPageStarted(v, url, favicon)

                                                if (!url.isNullOrBlank() && url != "about:blank") {

                                                    mainHandler.post { activePopupUrl = url }

                                                }

                                            }



                                            override fun onPageFinished(v: WebView?, url: String?) {

                                                super.onPageFinished(v, url)

                                                if (!url.isNullOrBlank() && url != "about:blank") {

                                                    mainHandler.post { activePopupUrl = url }

                                                }

                                                // Hook window.opener.postMessage and inspect Razorpay callback response

                                                // so clicking 'Success' or 'Failure' on Razorpay Software Private Ltd Bank always completes

                                                v?.evaluateJavascript(

                                                    """

                                                    (function() {

                                                        try {

                                                            if (window.opener && !window.__rzpOpenerHooked) {

                                                                window.__rzpOpenerHooked = true;

                                                                var origPost = window.opener.postMessage.bind(window.opener);

                                                                window.opener.postMessage = function(msg, targetOrigin) {

                                                                    try {

                                                                        var str = typeof msg === 'string' ? msg : JSON.stringify(msg);

                                                                        if (window.RazorpayBridge) {

                                                                            window.RazorpayBridge.onForwardPopupMessage(str);

                                                                        }

                                                                    } catch(e) {}

                                                                    return origPost(msg, targetOrigin);

                                                                };

                                                            }

                                                            var htmlText = document.documentElement ? document.documentElement.innerHTML : '';

                                                            if (htmlText.indexOf('razorpay_payment_id') !== -1 && htmlText.indexOf('razorpay_signature') !== -1) {

                                                                var payMatch = htmlText.match(/"razorpay_payment_id"\s*:\s*"([^"]+)"/);

                                                                var ordMatch = htmlText.match(/"razorpay_order_id"\s*:\s*"([^"]+)"/);

                                                                var sigMatch = htmlText.match(/"razorpay_signature"\s*:\s*"([^"]+)"/);

                                                                if (payMatch && sigMatch && window.RazorpayBridge) {

                                                                    var payloadObj = {

                                                                        razorpay_payment_id: payMatch[1],

                                                                        razorpay_order_id: ordMatch ? ordMatch[1] : '',

                                                                        razorpay_signature: sigMatch[1]

                                                                    };

                                                                    window.RazorpayBridge.onForwardPopupMessage(JSON.stringify(payloadObj));

                                                                    setTimeout(function() {

                                                                        if (window.RazorpayBridge) {

                                                                            window.RazorpayBridge.onPaymentSuccess(

                                                                                payloadObj.razorpay_order_id,

                                                                                payloadObj.razorpay_payment_id,

                                                                                payloadObj.razorpay_signature

                                                                            );

                                                                        }

                                                                    }, 350);

                                                                }

                                                            } else if (htmlText.indexOf('"error"') !== -1 && htmlText.indexOf('"description"') !== -1 && htmlText.indexOf('Welcome to Razorpay') === -1) {

                                                                var errMatch = htmlText.match(/"description"\s*:\s*"([^"]+)"/);

                                                                if (errMatch && window.RazorpayBridge) {

                                                                    setTimeout(function() {

                                                                        window.RazorpayBridge.onGatewayError(errMatch[1]);

                                                                    }, 350);

                                                                }

                                                            }

                                                        } catch(e) {}

                                                    })();

                                                    """.trimIndent(),

                                                    null

                                                )

                                            }

                                        }



                                        webChromeClient = object : WebChromeClient() {

                                            override fun onCloseWindow(window: WebView?) {

                                                mainHandler.post {

                                                    container.removeView(this@apply)

                                                    if (popupWebViewRef == this@apply) {

                                                        popupWebViewRef = null

                                                        activePopupUrl = null

                                                    }

                                                }

                                            }

                                        }

                                    }

                                    popupWebViewRef = popupWebView

                                    activePopupUrl = "https://api.razorpay.com/v1/gateway/mocksharp/payment?key_id=$effectiveKeyId"

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



                if (isLoadingGateway && gatewayError == null) {

                    Box(

                        modifier = Modifier.fillMaxSize(),

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

                                    text = "Opening Razorpay Test Page...",

                                    fontWeight = FontWeight.Black,

                                    fontSize = 14.sp

                                )

                            }

                        }

                    }

                }



                gatewayError?.let { err ->

                    Box(

                        modifier = Modifier

                            .fillMaxSize()

                            .background(Color.Black.copy(alpha = 0.6f))

                            .padding(24.dp),

                        contentAlignment = Alignment.Center

                    ) {

                        Card(

                            shape = RoundedCornerShape(22.dp),

                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

                        ) {

                            Column(

                                modifier = Modifier.padding(24.dp),

                                horizontalAlignment = Alignment.CenterHorizontally,

                                verticalArrangement = Arrangement.spacedBy(12.dp)

                            ) {

                                Surface(

                                    shape = RoundedCornerShape(50),

                                    color = RedbusSoftTint,

                                    modifier = Modifier.size(52.dp)

                                ) {

                                    Box(contentAlignment = Alignment.Center) {

                                        Icon(

                                            imageVector = Icons.Default.ErrorOutline,

                                            contentDescription = null,

                                            tint = RedbusCrimson,

                                            modifier = Modifier.size(28.dp)

                                        )

                                    }

                                }

                                Text(

                                    text = "Test Payment Failed",

                                    fontWeight = FontWeight.Black,

                                    fontSize = 17.sp,

                                    color = MaterialTheme.colorScheme.onSurface

                                )

                                Text(

                                    text = err,

                                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                                    fontSize = 13.sp,

                                    fontWeight = FontWeight.Medium

                                )

                                Row(

                                    horizontalArrangement = Arrangement.spacedBy(12.dp),

                                    modifier = Modifier.fillMaxWidth()

                                ) {

                                    OutlinedButton(

                                        onClick = onDismiss,

                                        shape = RoundedCornerShape(12.dp),

                                        modifier = Modifier.weight(1f)

                                    ) {

                                        Text("Cancel", fontWeight = FontWeight.Bold)

                                    }

                                    Button(

                                        onClick = { gatewayError = null },

                                        shape = RoundedCornerShape(12.dp),

                                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),

                                        modifier = Modifier.weight(1f)

                                    ) {

                                        Text("Try Again", fontWeight = FontWeight.Bold)

                                    }

                                }

                            }

                        }

                    }

                }



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



private fun calculateTripDuration(depTime: String?, arrTime: String?, fallback: Double? = 6.5): String {
    if (!depTime.isNullOrBlank() && !arrTime.isNullOrBlank()) {
        try {
            val depParts = depTime.take(5).split(":").map { it.toInt() }
            val arrParts = arrTime.take(5).split(":").map { it.toInt() }
            val depMinutes = depParts[0] * 60 + depParts[1]
            val arrMinutes = arrParts[0] * 60 + arrParts[1]
            var diff = arrMinutes - depMinutes
            if (diff <= 0) diff += 24 * 60
            val hours = diff / 60
            val mins = diff % 60
            return if (mins > 0) "${hours}h ${mins}m" else "${hours} hrs"
        } catch (e: Exception) {
            // fallback
        }
    }
    return if (fallback != null && fallback > 0) "${fallback} hrs" else "6.5 hrs"
}

/**
 * Realistic Landscape 2-Deck Sleeper Seat Map matching Image 4 / Web Version
 */
@Composable
private fun LandscapeSeatMapVisual(selectedSeats: List<SeatItem>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header & Legend matching Image 4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Selected seat layout",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFE91E63), RoundedCornerShape(2.dp))
                    )
                    Text("Your Seat", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp))
                    )
                    Text("Other", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }

        // Side-by-side Decks Container (Matching Web Image 4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Lower Deck
            DeckCard(
                title = "Lower deck",
                headerBadge = "✇",
                rows = listOf(
                    Triple("L1", "L2", "L3"),
                    Triple("L4", "L5", "L6"),
                    Triple("L7", "L8", "L9"),
                    Triple("L10", "L11", "L12"),
                    Triple("L13", "L14", "L15")
                ),
                selectedSeats = selectedSeats,
                modifier = Modifier.weight(1f)
            )

            // Upper Deck
            DeckCard(
                title = "Upper deck",
                headerBadge = "UP",
                rows = listOf(
                    Triple("U1", "U2", "U3"),
                    Triple("U4", "U5", "U6"),
                    Triple("U7", "U8", "U9"),
                    Triple("U10", "U11", "U12"),
                    Triple("U13", "U14", "U15")
                ),
                selectedSeats = selectedSeats,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DeckCard(
    title: String,
    headerBadge: String,
    rows: List<Triple<String, String, String>>,
    selectedSeats: List<SeatItem>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = headerBadge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Seat Rows (2+1 Layout with Aisle matching Image 4)
            rows.forEach { r ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Single Berth
                    val isLeftSel = selectedSeats.any { it.seatNumber.equals(r.first, ignoreCase = true) }
                    SleeperBerthItem(seatNumber = r.first, isSelected = isLeftSel)

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right Double Berths
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val isR1Sel = selectedSeats.any { it.seatNumber.equals(r.second, ignoreCase = true) }
                        SleeperBerthItem(seatNumber = r.second, isSelected = isR1Sel)

                        val isR2Sel = selectedSeats.any { it.seatNumber.equals(r.third, ignoreCase = true) }
                        SleeperBerthItem(seatNumber = r.third, isSelected = isR2Sel)
                    }
                }
            }

            Text(
                text = "▼ EMERGENCY EXIT",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SleeperBerthItem(
    seatNumber: String,
    isSelected: Boolean
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isSelected) Color(0xFFE91E63) else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFFBE185D) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .width(26.dp)
            .height(34.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pillow Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.5f) else Color(0xFFCBD5E1),
                        RoundedCornerShape(1.dp)
                    )
            )
            // Seat Number
            Text(
                text = seatNumber,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}
