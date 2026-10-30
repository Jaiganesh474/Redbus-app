package com.redbus.ai.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.redbus.ai.data.model.AiCityItem
import com.redbus.ai.data.model.Banner
import com.redbus.ai.data.model.BookingDetails
import com.redbus.ai.data.model.RouteItem
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.viewmodel.RedbusUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: RedbusUiState,
    onSwapCities: () -> Unit,
    onSelectSourceCity: (String) -> Unit,
    onSelectDestinationCity: (String) -> Unit,
    onSelectCities: (String, String) -> Unit,
    onSearchCities: (String, Boolean) -> Unit,
    onSelectDate: (String) -> Unit,
    onNlpQueryChange: (String) -> Unit,
    onExecuteNlpSearch: () -> Unit,
    onSearchBuses: () -> Unit,
    onQuickRouteClick: (RouteItem) -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToBookings: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onToggleDarkTheme: () -> Unit
) {
    var showDateModal by remember { mutableStateOf(false) }
    var activeCityPickerIsSource by remember { mutableStateOf<Boolean?>(null) }
    var bookingForWomen by remember { mutableStateOf(false) }
    var showWomenInfoDialog by remember { mutableStateOf(false) }

    var swapRotationCount by remember { mutableIntStateOf(0) }
    val rotationAngle by animateFloatAsState(
        targetValue = swapRotationCount * 180f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "swapRotation"
    )
    val swapFlipX = remember { Animatable(0f) }
    LaunchedEffect(swapRotationCount) {
        if (swapRotationCount > 0) {
            swapFlipX.snapTo(-18f)
            swapFlipX.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    val heroCardRotX = remember { Animatable(12f) }
    val heroCardScale = remember { Animatable(0.95f) }
    LaunchedEffect(Unit) {
        heroCardRotX.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        heroCardScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }

    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val tomorrowStr = remember { LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE) }

    val activePromoCode = uiState.availableCoupons.firstOrNull()?.code ?: "FIRSTBUS"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Hero Section Matching Image 2 (Warm Cream-to-Sunset Gradient + Search Card + Promo Banner)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = if (uiState.isDarkTheme) {
                                listOf(
                                    Color(0xFF1E1B2E),
                                    Color(0xFF2D1B2E),
                                    Color(0xFF4A1D2F)
                                )
                            } else {
                                listOf(
                                    Color(0xFFFFF8EE),
                                    Color(0xFFFFF1DC),
                                    Color(0xFFFFCA8A),
                                    Color(0xFFF37042)
                                )
                            }
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Row: redBus Logo Badge + Wallet Pill + My Account Pill (Image 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: redBus stylized bus badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.border(
                                1.5.dp,
                                RedbusCrimson,
                                RoundedCornerShape(12.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = "redBus",
                                    tint = RedbusCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "redBus",
                                    color = RedbusCrimson,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Right: Wallet ₹... Pill & My Account Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = onOpenWallet,
                                shape = RoundedCornerShape(50),
                                color = Color.White,
                                shadowElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Wallet",
                                        tint = Color(0xFF1E293B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Wallet",
                                        color = Color(0xFF1E293B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFF1F5F9)
                                    ) {
                                        Text(
                                            text = "₹${(uiState.user?.walletBalance ?: 0.0).toInt()}",
                                            color = Color(0xFF0F172A),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = onNavigateToAccount,
                                shape = RoundedCornerShape(50),
                                color = Color.White,
                                shadowElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonOutline,
                                        contentDescription = "My Account",
                                        tint = Color(0xFF1E293B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (uiState.isAuthenticated && uiState.user != null) {
                                            uiState.user.name.substringBefore(" ")
                                        } else {
                                            "My Account"
                                        },
                                        color = Color(0xFF1E293B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Heading: "Book bus tickets"
                    Text(
                        text = "Book bus tickets",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        color = if (uiState.isDarkTheme) Color.White else Color(0xFF111827)
                    )

                    // Main Search Card with Navy "Lowest price guaranteed*" Header & 3D Entrance Animation
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF17377B) // Navy header background
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                rotationX = heroCardRotX.value
                                scaleX = heroCardScale.value
                                scaleY = heroCardScale.value
                                cameraDistance = 16f * density
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Top Navy Strip: "Lowest price guaranteed*"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Lowest price guaranteed*",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // White Inner Card Body
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // From & To Section with Overlapping Dark Swap Circle Button & 3D Flip
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    rotationX = swapFlipX.value
                                                    cameraDistance = 14f * density
                                                }
                                        ) {
                                            // FROM Row
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        activeCityPickerIsSource = true
                                                        onSearchCities("", true)
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsBus,
                                                    contentDescription = "From",
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "From",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = uiState.sourceCity.ifBlank { "Select departure city" },
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                                thickness = 1.dp
                                            )

                                            // TO Row
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        activeCityPickerIsSource = false
                                                        onSearchCities("", false)
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsBus,
                                                    contentDescription = "To",
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "To",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = uiState.destinationCity.ifBlank { "Select destination city" },
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (uiState.destinationCity.isBlank()) {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Overlapping Dark Circular Swap Button on the Right (Matching Image 2)
                                        Surface(
                                            onClick = {
                                                swapRotationCount++
                                                onSwapCities()
                                            },
                                            shape = CircleShape,
                                            color = Color(0xFF4B5563),
                                            shadowElevation = 4.dp,
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 20.dp)
                                                .size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.SwapVert,
                                                    contentDescription = "Swap Cities",
                                                    tint = Color.White,
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .rotate(rotationAngle)
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                        thickness = 1.dp
                                    )

                                    // Date of Journey Row with Today & Tomorrow Pink Pills (Matching Image 2)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showDateModal = true }
                                            .padding(horizontal = 16.dp, vertical = 13.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = "Date of Journey",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = "Date of Journey",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = formatShortDateDisplay(uiState.travelDate),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            PinkDatePill(
                                                label = "Today",
                                                selected = uiState.travelDate == todayStr,
                                                onClick = { onSelectDate(todayStr) }
                                            )
                                            PinkDatePill(
                                                label = "Tomorrow",
                                                selected = uiState.travelDate == tomorrowStr,
                                                onClick = { onSelectDate(tomorrowStr) }
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                        thickness = 1.dp
                                    )

                                    // Booking for women Row with Custom Pill Toggle Switch (Matching Image 2)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 13.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Face3,
                                                contentDescription = "Booking for women",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = "Booking for women",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Know more",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF2563EB),
                                                    textDecoration = TextDecoration.Underline,
                                                    modifier = Modifier.clickable { showWomenInfoDialog = true }
                                                )
                                            }
                                        }

                                        RedbusStyledSwitch(
                                            checked = bookingForWomen,
                                            onCheckedChange = { bookingForWomen = it }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Full-Width Pill "Search buses" Button (Matching Image 2)
                    Button(
                        onClick = onSearchBuses,
                        enabled = uiState.sourceCity.isNotBlank() &&
                            uiState.destinationCity.isNotBlank() &&
                            !uiState.sourceCity.equals(uiState.destinationCity, ignoreCase = true),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(6.dp, RoundedCornerShape(50)),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedbusCrimson,
                            contentColor = Color.White
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Search buses",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // AI Smart NLP Search Bar Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = RedbusCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                            BasicTextField(
                                value = uiState.nlpQuery,
                                onValueChange = onNlpQueryChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(RedbusCrimson),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp),
                                decorationBox = { innerTextField ->
                                    if (uiState.nlpQuery.isEmpty()) {
                                        AutoTypingMovingPlaceholder(
                                            phrases = listOf(
                                                "Ask AI: 'AC sleeper Bangalore to Chennai tomorrow'...",
                                                "Ask AI: 'Morning Volvo bus under 1000'...",
                                                "Ask AI: 'Buses with live tracking and free cancellation'...",
                                                "Ask AI: 'Primo certified sleeper buses'..."
                                            ),
                                            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            cursorColor = RedbusCrimson
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            Button(
                                onClick = onExecuteNlpSearch,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RedbusCrimson,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "AI Go",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    // Promo Code Badge in Sunset Footer (Matching Image 2 "Use code: EARLY2027")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0284C7),
                            modifier = Modifier.border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "Use code: $activePromoCode",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Bottom Navy Partner Strip ("AUTHORIZED & VERIFIED BUS PARTNERS")
                Surface(
                    color = Color(0xFF172554),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AUTHORIZED & VERIFIED BUS PARTNERS",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }
        }

        // 2. "Upcoming trips" Section (Matching Image 2 bottom section)
        val upcomingTrips = uiState.myBookings.filter { it.status == "CONFIRMED" }
        if (upcomingTrips.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Text(
                        text = "Upcoming trips",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(upcomingTrips) { trip ->
                            UpcomingTripCard(
                                booking = trip,
                                todayStr = todayStr,
                                tomorrowStr = tomorrowStr,
                                onClick = onNavigateToBookings
                            )
                        }
                    }
                }
            }
        }

        // 3. Live Banners from Backend (/banners)
        if (uiState.banners.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 22.dp)) {
                    Text(
                        text = "AI Curated Fleet & Live Offers",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.banners) { banner ->
                            LiveBannerCard(
                                banner = banner,
                                onClick = {
                                    banner.routeInfo?.let { info ->
                                        val parts = info.split("⇄", "->", "to", "-").map { it.trim() }
                                        if (parts.size >= 2) {
                                            onSelectCities(parts[0], parts[1])
                                            onSearchBuses()
                                            return@LiveBannerCard
                                        }
                                    }
                                    onSearchBuses()
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Live AI Recommendations (/ai/recommendations) or Live Routes
        val recommendedList = uiState.aiRecommendedRoutes.ifEmpty { uiState.routes }
        if (recommendedList.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 22.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = RedbusCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recommended For Your Journey",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = uiState.aiRecommendationReason
                                ?: "Handpicked top-rated sleeper & luxury buses across popular routes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recommendedList) { route ->
                            LiveRecommendedRouteCard(
                                route = route,
                                onClick = { onQuickRouteClick(route) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Live Popular Bus Routes Across India (/routes/popular)
        if (uiState.popularRoutes.isNotEmpty()) {
            item {
                Text(
                    text = "Popular Bus Routes Across India",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            items(uiState.popularRoutes) { pair ->
                Card(
                    onClick = {
                        onSelectCities(pair.sourceCity, pair.destinationCity)
                        onSearchBuses()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = null,
                                        tint = RedbusCrimson,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${pair.sourceCity} ➔ ${pair.destinationCity}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${pair.busCount} daily buses available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "From ₹${pair.minPrice.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = RedbusCrimson,
                                fontWeight = FontWeight.Black
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Book",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedbusCrimson
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = RedbusCrimson,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dynamic Searchable City Picker Modal (Strictly excluding the opposite selected city)
    activeCityPickerIsSource?.let { isSource ->
        DynamicCitySearchDialog(
            isSource = isSource,
            currentValue = if (isSource) uiState.sourceCity else uiState.destinationCity,
            excludeCity = if (isSource) uiState.destinationCity else uiState.sourceCity,
            suggestions = if (isSource) uiState.fromCitySuggestions else uiState.toCitySuggestions,
            isSearching = uiState.isSearchingCities,
            onQueryChanged = { query -> onSearchCities(query, isSource) },
            onSelectCity = { selectedCity ->
                if (isSource) {
                    onSelectSourceCity(selectedCity)
                } else {
                    onSelectDestinationCity(selectedCity)
                }
                activeCityPickerIsSource = null
            },
            onDismiss = { activeCityPickerIsSource = null }
        )
    }

    // Select Date Modal Popup (Matching frontend/src/components/SelectDateModal.tsx)
    if (showDateModal) {
        SelectDateModal(
            selectedDateStr = uiState.travelDate,
            onDismiss = { showDateModal = false },
            onSelectDate = { newDate ->
                onSelectDate(newDate)
                showDateModal = false
            }
        )
    }

    if (showWomenInfoDialog) {
        AlertDialog(
            onDismissRequest = { showWomenInfoDialog = false },
            title = { Text("Booking for Women", fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "When enabled, redBus AI highlights women-friendly buses, verified safety ratings, and adjacent female-reserved sleeper/seater berths.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showWomenInfoDialog = false }) {
                    Text("Got it", color = RedbusCrimson, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * High-contrast, sleek pill toggle switch matching Image 2 (and fixing the washed-out toggle in Image 4).
 */
@Composable
fun RedbusStyledSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        animationSpec = tween(durationMillis = 200),
        label = "switchThumbOffset"
    )
    val trackColor = if (checked) RedbusCrimson else Color(0xFFE2E8F0)
    val borderColor = if (checked) RedbusCrimson else Color(0xFF475569)
    val thumbColor = if (checked) Color.White else Color(0xFF334155)

    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(50))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@Composable
private fun ServiceCategoryTile(
    badgeText: String,
    title: String,
    icon: ImageVector,
    iconTint: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "category3d")
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffsetY"
    )
    val rotY by infiniteTransition.animateFloat(
        initialValue = if (selected) -4f else 0f,
        targetValue = if (selected) 4f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotY"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = if (selected) 6.dp else 2.dp,
        modifier = modifier
            .graphicsLayer {
                translationY = if (selected) floatOffsetY else 0f
                rotationY = rotY
                cameraDistance = 12f * density
            }
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) RedbusCrimson.copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFBBF7D0)
            ) {
                Text(
                    text = badgeText,
                    color = Color(0xFF14532D),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )

            Text(
                text = title,
                color = Color(0xFF111827),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PinkDatePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) RedbusCrimson else Color(0xFFFFE4E6)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFF111827),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun UpcomingTripCard(
    booking: BookingDetails,
    todayStr: String,
    tomorrowStr: String,
    onClick: () -> Unit
) {
    val badgeLabel = when (booking.travelDate) {
        todayStr -> "Today"
        tomorrowStr -> "Tomorrow"
        else -> formatShortDateDisplay(booking.travelDate)
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(270.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    modifier = Modifier.border(1.dp, RedbusCrimson, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "redBus",
                        color = RedbusCrimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEDD5)
                ) {
                    Text(
                        text = badgeLabel,
                        color = Color(0xFF111827),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "${booking.sourceCity} → ${booking.destinationCity}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${booking.departureTime} · ${formatShortDateDisplay(booking.travelDate)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Dynamic Searchable City Picker Modal powered by /api/v1/ai/cities/search
 * Strictly excludes `excludeCity` so the city selected in From never reflects in To (and vice versa).
 */
@Composable
private fun DynamicCitySearchDialog(
    isSource: Boolean,
    currentValue: String,
    excludeCity: String,
    suggestions: List<AiCityItem>,
    isSearching: Boolean,
    onQueryChanged: (String) -> Unit,
    onSelectCity: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filteredCities = remember(suggestions, query, excludeCity) {
        suggestions
            .filterNot { excludeCity.isNotBlank() && it.name.equals(excludeCity.trim(), ignoreCase = true) }
            .filter {
                val q = query.trim()
                q.isEmpty() ||
                    it.name.contains(q, ignoreCase = true) ||
                    it.state.contains(q, ignoreCase = true) ||
                    it.aliases.orEmpty().any { alias -> alias.contains(q, ignoreCase = true) }
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSource) "Select Departure City (From)" else "Select Destination City (To)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onQueryChanged(it)
                    },
                    placeholder = {
                        Text(
                            text = if (isSource) "Search departure city..." else "Search destination city...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = RedbusCrimson
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                onQueryChanged("")
                            }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (excludeCity.isNotBlank()) {
                    Text(
                        text = "Note: \"$excludeCity\" is already selected as ${if (isSource) "Destination" else "Departure"} and excluded here.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSearching && filteredCities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RedbusCrimson, modifier = Modifier.size(28.dp))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(filteredCities, key = { it.name }) { city ->
                            val isSelected = currentValue.equals(city.name, ignoreCase = true)
                            Surface(
                                onClick = { onSelectCity(city.name) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) RedbusSoftTint else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (city.activeRoutesCount > 0) RatingEmeraldBg else RedbusSoftTint,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsBus,
                                                    contentDescription = null,
                                                    tint = if (city.activeRoutesCount > 0) RatingEmerald else RedbusCrimson,
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
                                                    text = city.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (city.state.isNotBlank()) {
                                                    Text(
                                                        text = city.state,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (city.tag.isNotBlank()) {
                                                Text(
                                                    text = city.tag,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (city.activeRoutesCount > 0) RatingEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
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
        }
    }
}

/**
 * Interactive Calendar Date Picker Modal matching frontend/src/components/SelectDateModal.tsx.
 */
@Composable
private fun SelectDateModal(
    selectedDateStr: String,
    onDismiss: () -> Unit,
    onSelectDate: (String) -> Unit
) {
    val today = remember { LocalDate.now() }
    val selectedDate = remember(selectedDateStr) {
        runCatching { LocalDate.parse(selectedDateStr) }.getOrDefault(today)
    }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val weekdays = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
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
                    Text(
                        text = "Select date",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                Row(modifier = Modifier.fillMaxWidth()) {
                    weekdays.forEach { wd ->
                        Text(
                            text = wd,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month"
                        )
                    }
                    val monthTitle = remember(currentYearMonth) {
                        val mName = currentYearMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.ENGLISH)
                        "$mName ${currentYearMonth.year}"
                    }
                    Text(
                        text = monthTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month"
                        )
                    }
                }

                val daysInMonth = currentYearMonth.lengthOfMonth()
                val firstDayMondayBased = currentYearMonth.atDay(1).dayOfWeek.value - 1
                val totalCells = firstDayMondayBased + daysInMonth
                val rows = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - firstDayMondayBased + 1
                                if (dayNum in 1..daysInMonth) {
                                    val cellDate = currentYearMonth.atDay(dayNum)
                                    val isPast = cellDate.isBefore(today)
                                    val isSelected = cellDate == selectedDate
                                    val isSunday = col == 6

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            onClick = {
                                                if (!isPast) {
                                                    onSelectDate(cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                }
                                            },
                                            enabled = !isPast,
                                            shape = CircleShape,
                                            color = when {
                                                isSelected -> Color(0xFF1E293B)
                                                else -> Color.Transparent
                                            },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    fontSize = 13.sp,
                                                    fontWeight = when {
                                                        isSelected -> FontWeight.ExtraBold
                                                        isSunday -> FontWeight.Bold
                                                        else -> FontWeight.SemiBold
                                                    },
                                                    color = when {
                                                        isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
                                                        isSelected -> Color.White
                                                        isSunday -> RedbusCrimson
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
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

private fun formatShortDateDisplay(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val weekday = date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
        val month = date.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
        "$weekday ${date.dayOfMonth}-$month"
    } catch (_: Exception) {
        dateStr
    }
}

@Composable
private fun LiveBannerCard(
    banner: Banner,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .width(300.dp)
            .height(160.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!banner.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = banner.imageUrl,
                    contentDescription = banner.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xEB0F172A),
                                Color(0xB37F1D1D)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = RedbusCrimson,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = banner.tag ?: "AI CURATED",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        banner.promoCode?.let { code ->
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = code,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = banner.title,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        banner.subtitle?.let { sub ->
                            Text(
                                text = sub,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveRecommendedRouteCard(
    route: RouteItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .width(265.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${route.durationHours} hrs",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = RatingEmeraldBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "★ ${route.rating}",
                        color = RatingEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "${route.sourceCity} ➔ ${route.destinationCity}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = route.operatorName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = route.busType,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Starting from",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${route.basePrice.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        color = RedbusCrimson,
                        fontWeight = FontWeight.Black
                    )
                }
                Surface(
                    color = RedbusCrimson,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Select Seats ➔",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}
