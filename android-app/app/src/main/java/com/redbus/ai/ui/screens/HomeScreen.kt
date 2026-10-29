package com.redbus.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.redbus.ai.data.model.AiCityItem
import com.redbus.ai.data.model.Banner
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
    onToggleDarkTheme: () -> Unit
) {
    var showDateModal by remember { mutableStateOf(false) }
    var swapRotationCount by remember { mutableIntStateOf(0) }
    val rotationAngle by animateFloatAsState(
        targetValue = swapRotationCount * 180f,
        animationSpec = tween(durationMillis = 350),
        label = "swapRotation"
    )

    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val tomorrowStr = remember { LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Hero Header & Search Section (Matching HeroSearch.tsx)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(RedbusCrimson, RedbusCrimsonDark)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Top App Bar Row with Live Account / Wallet Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = "redBus AI",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "redBus",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "AI EDITION",
                                            color = RedbusCrimson,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Book Bus Tickets with Trust & Confidence",
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = onNavigateToAccount,
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isAuthenticated) Icons.Default.AccountBalanceWallet else Icons.Default.Person,
                                        contentDescription = "Account",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (uiState.isAuthenticated && uiState.user != null) {
                                            "${uiState.user.name.substringBefore(" ")} • ₹${(uiState.user.walletBalance ?: 0.0).toInt()}"
                                        } else {
                                            "Sign In"
                                        },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            IconButton(
                                onClick = onToggleDarkTheme,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                            ) {
                                Icon(
                                    imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // NLP Natural Language Search Pill (Matching HeroSearch.tsx)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp,
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
                                        Text(
                                            text = "Ask AI: 'AC sleeper Bangalore to Chennai tomorrow'...",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "AI Search",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Standard Search Card (Matching HeroSearch.tsx + AiCityDropdown.tsx + SelectDateModal.tsx)
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // FROM CITY (Dynamic Searchable AiCityDropdown excluding To City)
                            AiCityDropdownField(
                                label = "From",
                                value = uiState.sourceCity,
                                excludeCity = uiState.destinationCity,
                                placeholder = "Select departure city",
                                suggestions = uiState.fromCitySuggestions,
                                isSearching = uiState.isSearchingCities,
                                onQueryChanged = { query ->
                                    onSearchCities(query, true)
                                },
                                onCitySelected = { selectedCity ->
                                    onSelectSourceCity(selectedCity)
                                }
                            )

                            // Swap Cities Button Pill (Matching HeroSearch.tsx mobile swap button)
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                )
                                Surface(
                                    onClick = {
                                        swapRotationCount++
                                        onSwapCities()
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        RoundedCornerShape(50)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Swap Cities",
                                            tint = RedbusCrimson,
                                            modifier = Modifier
                                                .size(15.dp)
                                                .rotate(rotationAngle)
                                        )
                                        Text(
                                            text = "Swap Cities",
                                            color = RedbusCrimson,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // TO CITY (Dynamic Searchable AiCityDropdown strictly excluding From City)
                            AiCityDropdownField(
                                label = "To",
                                value = uiState.destinationCity,
                                excludeCity = uiState.sourceCity,
                                placeholder = "Select destination city",
                                suggestions = uiState.toCitySuggestions,
                                isSearching = uiState.isSearchingCities,
                                onQueryChanged = { query ->
                                    onSearchCities(query, false)
                                },
                                onCitySelected = { selectedCity ->
                                    onSelectDestinationCity(selectedCity)
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )

                            // Travel Date Box (Matching HeroSearch.tsx lines 203-257)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Date",
                                            tint = RedbusCrimson,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Date",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DateQuickPill(
                                            label = "Today",
                                            selected = uiState.travelDate == todayStr,
                                            onClick = { onSelectDate(todayStr) }
                                        )
                                        DateQuickPill(
                                            label = "Tomorrow",
                                            selected = uiState.travelDate == tomorrowStr,
                                            onClick = { onSelectDate(tomorrowStr) }
                                        )
                                    }
                                }

                                // Clickable Date Selector Box opening SelectDateModal
                                Surface(
                                    onClick = { showDateModal = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                            RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 13.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = RedbusCrimson,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = formatShortDateDisplay(uiState.travelDate),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "Change",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RedbusCrimson
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // SEARCH BUSES Button
                            Button(
                                onClick = onSearchBuses,
                                enabled = uiState.sourceCity.isNotBlank() &&
                                    uiState.destinationCity.isNotBlank() &&
                                    !uiState.sourceCity.equals(uiState.destinationCity, ignoreCase = true),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(14.dp),
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
                                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = "SEARCH BUSES",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Popular Route Quick Badges (Matching HeroSearch.tsx lines 273-295)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Text(
                                text = "Popular:",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        val quickPairs = listOf(
                            "Bangalore" to "Chennai",
                            "Mumbai" to "Pune",
                            "Delhi" to "Jaipur",
                            "Hyderabad" to "Bangalore"
                        )
                        items(quickPairs) { (fromCity, toCity) ->
                            Surface(
                                onClick = {
                                    onSelectCities(fromCity, toCity)
                                },
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier.border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(50)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = fromCity, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Text(text = "➔", color = Color(0xFFFCD34D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = toCity, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Live Banners from Backend (/banners)
        if (uiState.banners.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 20.dp)) {
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

        // 3. Live AI Recommendations (/ai/recommendations) or Live Routes
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

        // 4. Live Available Coupons (/coupons/available)
        if (uiState.availableCoupons.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 22.dp)) {
                    Text(
                        text = "Active Promo Codes",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.availableCoupons) { coupon ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.width(265.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = AiViolet,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = coupon.code,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = coupon.title ?: "${coupon.discountPercentage}% OFF up to ₹${coupon.maxDiscountAmount.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = coupon.description ?: "Min booking ₹${coupon.minBookingAmount.toInt()}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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
}

/**
 * Dynamic Searchable City Dropdown Input matching frontend/src/components/AiCityDropdown.tsx.
 * Fetches live city suggestions from /api/v1/ai/cities/search and strictly excludes `excludeCity`.
 */
@Composable
private fun AiCityDropdownField(
    label: String,
    value: String,
    excludeCity: String,
    placeholder: String,
    suggestions: List<AiCityItem>,
    isSearching: Boolean,
    onQueryChanged: (String) -> Unit,
    onCitySelected: (String) -> Unit
) {
    var isOpen by remember { mutableStateOf(false) }
    var inputValue by remember(value) { mutableStateOf(value) }

    // Strictly exclude `excludeCity` so the city selected in From never appears in To (and vice versa)
    val filteredSuggestions = remember(suggestions, inputValue, excludeCity) {
        suggestions
            .filterNot { excludeCity.isNotBlank() && it.name.equals(excludeCity.trim(), ignoreCase = true) }
            .filter {
                val q = inputValue.trim()
                q.isEmpty() ||
                    q.equals(value.trim(), ignoreCase = true) ||
                    it.name.contains(q, ignoreCase = true) ||
                    it.state.contains(q, ignoreCase = true) ||
                    it.aliases.orEmpty().any { alias -> alias.contains(q, ignoreCase = true) }
            }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = label,
                tint = RedbusCrimson,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Direct Searchable Input Box
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isOpen) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isOpen) 1.5.dp else 1.dp,
                    color = if (isOpen) RedbusCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable {
                    if (!isOpen) {
                        isOpen = true
                        onQueryChanged("")
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = RedbusCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                    BasicTextField(
                        value = inputValue,
                        onValueChange = { newText ->
                            inputValue = newText
                            if (!isOpen) isOpen = true
                            onQueryChanged(newText)
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(RedbusCrimson),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (inputValue.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (inputValue.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                inputValue = ""
                                isOpen = true
                                onQueryChanged("")
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear city",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            isOpen = !isOpen
                            if (isOpen) onQueryChanged("")
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle city dropdown",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Expandable Live City Suggestions List (Matching AiCityDropdown.tsx lines 154-236)
        AnimatedVisibility(visible = isOpen) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                if (isSearching && filteredSuggestions.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = RedbusCrimson,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Searching routes & cities...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (filteredSuggestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        filteredSuggestions.take(7).forEach { city ->
                            val isSelected = value.equals(city.name, ignoreCase = true)
                            Surface(
                                onClick = {
                                    inputValue = city.name
                                    onCitySelected(city.name)
                                    isOpen = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    RedbusSoftTint
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (city.activeRoutesCount > 0) RatingEmeraldBg else RedbusSoftTint,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsBus,
                                                    contentDescription = null,
                                                    tint = if (city.activeRoutesCount > 0) RatingEmerald else RedbusCrimson,
                                                    modifier = Modifier.size(16.dp)
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
                                                    fontSize = 14.sp,
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (city.tag.isNotBlank()) {
                                                    Surface(
                                                        color = if (city.activeRoutesCount > 0) {
                                                            RatingEmeraldBg
                                                        } else {
                                                            MaterialTheme.colorScheme.surfaceVariant
                                                        },
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = city.tag,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (city.activeRoutesCount > 0) {
                                                                RatingEmerald
                                                            } else {
                                                                MaterialTheme.colorScheme.onSurfaceVariant
                                                            },
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                val aliases = city.aliases.orEmpty().take(2)
                                                if (aliases.isNotEmpty()) {
                                                    Text(
                                                        text = "• ${aliases.joinToString(", ")}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = RedbusCrimson,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "No cities found matching \"$inputValue\"",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (inputValue.trim().isNotEmpty() && !inputValue.trim().equals(excludeCity.trim(), ignoreCase = true)) {
                            TextButton(
                                onClick = {
                                    onCitySelected(inputValue.trim())
                                    isOpen = false
                                }
                            ) {
                                Text("Use \"${inputValue.trim()}\"", color = RedbusCrimson, fontWeight = FontWeight.Bold)
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
                // Header: "Select date" + Circular Close Button
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

                // Weekdays Strip (Mon .. Sun)
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

                // Month & Year Navigation: ← Month Year →
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

                // Days Grid (Monday = 0 .. Sunday = 6)
                val daysInMonth = currentYearMonth.lengthOfMonth()
                val firstDayMondayBased = currentYearMonth.atDay(1).dayOfWeek.value - 1 // 0..6
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

/**
 * Formats YYYY-MM-DD into "Thu 29-Oct" matching formatShortDateDisplay() in frontend/src/lib/dateUtils.ts.
 */
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
private fun DateQuickPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) RedbusSoftTint else MaterialTheme.colorScheme.surfaceVariant,
        modifier = if (selected) {
            Modifier.border(1.dp, RedbusCrimson.copy(alpha = 0.45f), RoundedCornerShape(50))
        } else {
            Modifier
        }
    ) {
        Text(
            text = label,
            color = if (selected) RedbusCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
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
