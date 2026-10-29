package com.redbus.ai.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redbus.ai.data.model.CityPair
import com.redbus.ai.data.model.RouteItem
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.viewmodel.RedbusUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: RedbusUiState,
    availableCities: List<String>,
    popularRoutes: List<CityPair>,
    onSwapCities: () -> Unit,
    onSelectCities: (String, String) -> Unit,
    onSelectDate: (String) -> Unit,
    onNlpQueryChange: (String) -> Unit,
    onExecuteNlpSearch: () -> Unit,
    onSearchBuses: () -> Unit,
    onQuickRouteClick: (RouteItem) -> Unit,
    onToggleDarkTheme: () -> Unit
) {
    var showCityPickerForSource by remember { mutableStateOf<Boolean?>(null) }
    var swapRotationCount by remember { mutableIntStateOf(0) }
    val rotationAngle by animateFloatAsState(
        targetValue = swapRotationCount * 180f,
        animationSpec = tween(durationMillis = 350),
        label = "swapRotation"
    )

    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val tomorrowStr = remember { LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val weekendStr = remember { LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // 1. Hero Header & Search Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RedbusCrimson,
                                RedbusCrimsonDark
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Column {
                    // Top App Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = "RedBus AI",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
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
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = RedbusCrimson,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "AI",
                                                color = RedbusCrimson,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Smart Bus Ticketing & AI Seat Engine",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.16f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Wallet",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "₹${uiState.walletBalance.toInt()}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // AI Natural Language Search Bar
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Natural Language Search",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = uiState.nlpQuery,
                                    onValueChange = onNlpQueryChange,
                                    placeholder = {
                                        Text(
                                            text = "e.g. \"AC Sleeper Bangalore to Chennai tomorrow\"",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                                        cursorColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onExecuteNlpSearch,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = RedbusCrimson
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = "AI Go",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Main Route Search Card
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    // Source City Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showCityPickerForSource = true }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TripOrigin,
                                            contentDescription = "From",
                                            tint = RedbusCrimson,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "FROM CITY",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = uiState.sourceCity,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )

                                    // Destination City Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showCityPickerForSource = false }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "To",
                                            tint = AiViolet,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "TO CITY",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = uiState.destinationCity,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                // Animated Swap Button
                                IconButton(
                                    onClick = {
                                        swapRotationCount++
                                        onSwapCities()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 8.dp)
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(RedbusCrimson)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = "Swap Cities",
                                        tint = Color.White,
                                        modifier = Modifier.rotate(rotationAngle)
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )

                            // Date Selection + Quick Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Date",
                                        tint = RedbusCrimson,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "TRAVEL DATE",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = uiState.travelDate,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    DateChip(
                                        label = "Today",
                                        selected = uiState.travelDate == todayStr,
                                        onClick = { onSelectDate(todayStr) }
                                    )
                                    DateChip(
                                        label = "Tomorrow",
                                        selected = uiState.travelDate == tomorrowStr,
                                        onClick = { onSelectDate(tomorrowStr) }
                                    )
                                    DateChip(
                                        label = "+2d",
                                        selected = uiState.travelDate == weekendStr,
                                        onClick = { onSelectDate(weekendStr) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Primary Search CTA
                            Button(
                                onClick = onSearchBuses,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RedbusCrimson,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SEARCH BUSES",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. AI Smart Picks Carousel
        item {
            Column(modifier = Modifier.padding(top = 20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AiViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Smart Picks For You",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "98% On-Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = RatingEmerald
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.routes) { route ->
                        AiSmartPickCard(
                            route = route,
                            onClick = { onQuickRouteClick(route) }
                        )
                    }
                }
            }
        }

        // 3. Exclusive AI Coupons Strip
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                Text(
                    text = "Trending RedBus AI Offers",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = AiViolet,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "CODE: REDBUSAI20",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Flat 20% OFF up to ₹250 on AI Smart Sleeper Buses",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Valid on all Android app bookings • Auto-applies at checkout",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = AiViolet,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // 4. Popular Bus Corridors
        item {
            PaddingValues(horizontal = 16.dp)
            Text(
                text = "Popular Bus Corridors",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        items(popularRoutes) { pair ->
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
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
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
                                text = "${pair.busCount}+ Daily AC & Sleeper Coaches",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "from ₹${pair.minPrice.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = RedbusCrimson,
                            fontWeight = FontWeight.Black
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // City Picker Dialog
    if (showCityPickerForSource != null) {
        val isPickingSource = showCityPickerForSource == true
        AlertDialog(
            onDismissRequest = { showCityPickerForSource = null },
            title = {
                Text(
                    text = if (isPickingSource) "Select Source City" else "Select Destination City",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableCities.forEach { city ->
                        Surface(
                            onClick = {
                                if (isPickingSource) {
                                    onSelectCities(city, uiState.destinationCity)
                                } else {
                                    onSelectCities(uiState.sourceCity, city)
                                }
                                showCityPickerForSource = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = city, fontWeight = FontWeight.Bold)
                                Icon(
                                    imageVector = Icons.Default.ArrowForwardIos,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityPickerForSource = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DateChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) RedbusCrimson else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun AiSmartPickCard(
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
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = AiSoftPurple,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = route.aiTag ?: "AI Smart Pick",
                        color = AiViolet,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    color = RatingEmeraldBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "★ ${route.rating}",
                        color = RatingEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                text = "${route.departureTime} - ${route.arrivalTime} • ${route.busType}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${route.basePrice.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    color = RedbusCrimson,
                    fontWeight = FontWeight.Black
                )
                Surface(
                    color = RedbusCrimson,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Book Seats ➔",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

