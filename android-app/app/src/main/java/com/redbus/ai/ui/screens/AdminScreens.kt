package com.redbus.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redbus.ai.data.model.*
import com.redbus.ai.ui.theme.RedbusCrimson
import com.redbus.ai.ui.viewmodel.RedbusUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AdminPortalTab(val label: String, val badge: String? = null) {
    OPERATORS("Operators & Governance"),
    PRIMO_OPERATORS("Primo Bus Operators", "0% FEE"),
    EARNINGS("Operator Earnings & Wallets", "FINANCIALS"),
    BANNERS("Promotional Banners & AI Studio", "LIVE"),
    ANALYTICS("System Analytics & Corridors"),
    BOOKINGS("Master Bookings")
}

@Composable
fun AdminHubScreen(
    uiState: RedbusUiState,
    onRefresh: () -> Unit,
    onVerifyOperator: (Long, String) -> Unit,
    onSuspendOperator: (Long, String) -> Unit,
    onUpdateCommission: (Long, Double) -> Unit,
    onTogglePrimoOperator: (Long, Boolean, String) -> Unit,
    onGenerateAiBanner: (String, (Banner) -> Unit) -> Unit,
    onPublishBanner: (CreateBannerRequest, () -> Unit) -> Unit,
    onDeleteBanner: (Int, String) -> Unit,
    onClearMessages: () -> Unit
) {
    var activeTab by remember { mutableStateOf(AdminPortalTab.OPERATORS) }
    var editingCommissionOpId by remember { mutableStateOf<Long?>(null) }
    var commissionText by remember { mutableStateOf("10") }
    var isManualRefreshing by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()

    val triggerManualRefresh: () -> Unit = {
        refreshScope.launch {
            isManualRefreshing = true
            onRefresh()
            delay(500)
            isManualRefreshing = false
        }
    }

    // AI Banner Studio states
    var aiPrompt by remember { mutableStateOf("Festive Diwali 25% discount for Bangalore to Goa AC sleeper buses") }
    var draftTitle by remember { mutableStateOf("") }
    var draftSubtitle by remember { mutableStateOf("") }
    var draftTag by remember { mutableStateOf("AI EXCLUSIVE DEAL") }
    var draftPromoCode by remember { mutableStateOf("REDBUS25") }
    var draftRouteInfo by remember { mutableStateOf("Bangalore ⇄ Goa") }
    var draftCtaText by remember { mutableStateOf("Claim 25% Off") }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    if (isManualRefreshing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = RedbusCrimson,
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    text = "Refreshing Admin Console...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        return
    }

    val operators = uiState.adminOperators
    val pendingOperators = operators.filter { it.status.equals("PENDING", ignoreCase = true) }
    val approvedOperators = operators.filter { it.status.equals("APPROVED", ignoreCase = true) }
    val primoOperators = operators.filter { it.primoCertified == true }
    val primoBuses = uiState.adminPrimoBuses
    val stats = uiState.adminStats

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Enterprise Control Center Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF090D16),
                                Color(0xFF111827),
                                Color(0xFF1E293B)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = RedbusCrimson.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, RedbusCrimson.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "MASTER ADMINISTRATION & GOVERNANCE",
                                    color = Color(0xFFFECACA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        IconButton(
                            onClick = triggerManualRefresh,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "RedBus Enterprise Control Center",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "Logged in as ${uiState.user?.email ?: "redbus@admin.in"} • Operator Governance, Primo Bus Certification & Financial Settlement",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Action / Error banners
        if (!uiState.adminActionMessage.isNullOrBlank()) {
            item {
                Surface(
                    color = Color(0xFF065F46).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = uiState.adminActionMessage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onClearMessages, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        if (!uiState.adminErrorMessage.isNullOrBlank()) {
            item {
                Surface(
                    color = RedbusCrimson.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, RedbusCrimson.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.adminErrorMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedbusCrimson,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearMessages, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = RedbusCrimson, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // 2. KPI Metrics Grid (5 Cards matching Web Admin Console)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "GROSS REVENUE",
                        value = "₹${String.format("%,.0f", stats?.totalRevenue ?: 0.0)}",
                        subtitle = "Live platform earnings",
                        accentColor = Color(0xFF10B981),
                        icon = Icons.Default.AttachMoney
                    )
                    AdminKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "PRIMO CERTIFIED FLEET",
                        value = "${primoOperators.size} Operators",
                        subtitle = "${primoBuses.size} Buses • 0% Fee",
                        accentColor = Color(0xFFF59E0B),
                        icon = Icons.Default.WorkspacePremium
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "ACTIVE BUS FLEET",
                        value = "${stats?.totalBuses ?: primoBuses.size} Coaches",
                        subtitle = "${stats?.activeSchedulesCount ?: stats?.totalRoutes ?: 0} Schedules • ${stats?.occupancyRate ?: 0.0}% Occ",
                        accentColor = Color(0xFF6366F1),
                        icon = Icons.Default.DirectionsBus
                    )
                    AdminKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "FLEET OPERATORS",
                        value = "${operators.size}",
                        subtitle = "${approvedOperators.size} Active • ${pendingOperators.size} Pending",
                        accentColor = Color(0xFF3B82F6),
                        icon = Icons.Default.Business
                    )
                }
                AdminKpiCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "BOOKINGS",
                    value = "${stats?.totalBookings ?: 0} Total Reservations",
                    subtitle = "${stats?.confirmedBookings ?: 0} Confirmed Tickets Across All Corridors",
                    accentColor = RedbusCrimson,
                    icon = Icons.Default.ConfirmationNumber
                )
            }
        }

        // 3. Scrolling Tabs Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminPortalTab.entries.forEach { tab ->
                    val selected = activeTab == tab
                    val countSuffix = when (tab) {
                        AdminPortalTab.OPERATORS -> " (${operators.size})"
                        AdminPortalTab.PRIMO_OPERATORS -> " (${primoOperators.size})"
                        AdminPortalTab.BANNERS -> " (${uiState.adminBanners.size})"
                        else -> ""
                    }
                    Surface(
                        onClick = { activeTab = tab },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) RedbusCrimson else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) RedbusCrimson else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tab.label + countSuffix,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (tab.badge != null) {
                                Surface(
                                    color = if (selected) Color.White.copy(alpha = 0.22f) else Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = tab.badge,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (selected) Color.White else Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Active Tab Content
        when (activeTab) {
            AdminPortalTab.OPERATORS -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Registered Bus Operators (${operators.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Verify transport companies, toggle Primo Bus Operator certification (0% commission-free), and govern fleet operations",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (operators.isEmpty()) {
                    item {
                        AdminEmptyCard("No active bus operators registered yet.")
                    }
                } else {
                    items(operators, key = { it.id ?: 0L }) { op ->
                        val opId = op.id ?: 0L
                        val isPrimo = op.primoCertified == true
                        val isApproved = op.status.equals("APPROVED", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Header Row: Company Name + Status Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = op.companyName ?: "Bus Operator",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isPrimo) {
                                                PrimoMiniBadge()
                                            }
                                        }
                                        Text(
                                            text = "ID: #$opId • ${op.contactPerson ?: "Manager"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${op.email ?: ""} • ${op.phone ?: ""}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        color = if (isApproved) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (isApproved) "✓ Approved" else (op.status ?: "PENDING"),
                                            color = if (isApproved) Color(0xFF065F46) else Color(0xFF991B1B),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // Fleet, Revenue & Commission Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "FLEET & SCHEDULES",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${op.totalBuses} Buses • ${op.totalSchedules} Schedules",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${op.totalBookings} Bookings",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF7C3AED)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "TOTAL REVENUE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "₹${String.format("%,.1f", op.totalRevenue)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }

                                // Primo Operator Yes/No Toggle + Commission Control
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "PRIMO OPERATOR (0% COMMISSION)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isPrimo) "0% Commission Free (Primo Certified)" else "Commission: ${(op.commissionRate ?: 10.0).toInt()}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isPrimo) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Yes / No Toggle Pill
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                            .padding(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            onClick = {
                                                if (!isPrimo) {
                                                    onTogglePrimoOperator(opId, true, op.companyName ?: "Operator")
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isPrimo) Color(0xFF192857) else Color.Transparent
                                        ) {
                                            Text(
                                                text = "Yes",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isPrimo) Color(0xFFFCD34D) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                        Surface(
                                            onClick = {
                                                if (isPrimo) {
                                                    onTogglePrimoOperator(opId, false, op.companyName ?: "Operator")
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (!isPrimo) RedbusCrimson else Color.Transparent
                                        ) {
                                            Text(
                                                text = "No",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!isPrimo) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                // Commission Edit & Governance Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!isPrimo) {
                                        if (editingCommissionOpId == opId) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = commissionText,
                                                    onValueChange = { commissionText = it },
                                                    modifier = Modifier.width(72.dp),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                )
                                                Button(
                                                    onClick = {
                                                        val rate = commissionText.toDoubleOrNull() ?: 10.0
                                                        onUpdateCommission(opId, rate)
                                                        editingCommissionOpId = null
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                                ) {
                                                    Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        } else {
                                            TextButton(
                                                onClick = {
                                                    editingCommissionOpId = opId
                                                    commissionText = (op.commissionRate ?: 10.0).toInt().toString()
                                                }
                                            ) {
                                                Text("Edit Commission (${(op.commissionRate ?: 10.0).toInt()}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "★ Primo On-Time Guarantee Active",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }

                                    if (isApproved) {
                                        OutlinedButton(
                                            onClick = { onSuspendOperator(opId, op.companyName ?: "Operator") },
                                            border = BorderStroke(1.dp, RedbusCrimson.copy(alpha = 0.5f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Suspend", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { onVerifyOperator(opId, op.companyName ?: "Operator") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Approve & Verify", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminPortalTab.PRIMO_OPERATORS -> {
                // Primo Certified Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF131D3D),
                                        Color(0xFF1E2F63),
                                        Color(0xFF131D3D)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimoOnTimeBadge()
                            Text(
                                text = "Primo Certified Bus Operators & Commission-Free Fleet",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Operators certified as Primo Bus Operators by the Administrator enjoy 0% Platform Commission (100% Net Fare Settlement) because of their top-rated buses and On-Time Guarantee.",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Approved Primo Bus Operators (${primoOperators.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (primoOperators.isEmpty()) {
                    item {
                        AdminEmptyCard("No Primo Bus Operators certified yet. Toggle 'Primo Operator' to Yes in the Operators & Governance tab.")
                    }
                } else {
                    items(primoOperators, key = { "primo_op_${it.id}" }) { op ->
                        val opId = op.id ?: 0L
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.45f)),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = op.companyName ?: "Primo Operator",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${op.contactPerson ?: ""} • ${op.email ?: ""}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    PrimoOnTimeBadge()
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${op.totalBuses} Buses • ${op.totalSchedules} Schedules • ₹${String.format("%,.0f", op.totalRevenue)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        color = Color(0xFFD1FAE5),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "0% Commission Free",
                                            color = Color(0xFF065F46),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { onTogglePrimoOperator(opId, false, op.companyName ?: "Operator") },
                                        border = BorderStroke(1.dp, RedbusCrimson.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Remove Primo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Certified Primo Buses Fleet (${primoBuses.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (primoBuses.isEmpty()) {
                    item {
                        AdminEmptyCard("No buses currently under Primo certification.")
                    }
                } else {
                    items(primoBuses, key = { "primo_bus_${it.id}" }) { bus ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = Color(0xFFF5CB39),
                                            shape = RoundedCornerShape(5.dp),
                                            border = BorderStroke(1.dp, Color(0xFF0F172A))
                                        ) {
                                            Text(
                                                text = "• ${bus.registrationNumber ?: "TN12BB4466"} •",
                                                color = Color(0xFF0F172A),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFF0F8A43),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "★ ${String.format("%.1f", bus.rating ?: 4.5)}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = bus.operatorName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${bus.busType} • ${bus.totalSeats} Seats • 0% Commission-Free",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PrimoMiniBadge()
                            }
                        }
                    }
                }
            }

            AdminPortalTab.EARNINGS -> {
                val earnings = uiState.adminOperatorEarnings
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Operator Earnings & Wallet Ledger",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminKpiCard(
                                modifier = Modifier.weight(1f),
                                title = "GROSS BOOKINGS",
                                value = "₹${String.format("%,.0f", earnings?.systemGrossRevenue ?: 0.0)}",
                                subtitle = "100% Passenger Fares",
                                accentColor = Color(0xFF2563EB),
                                icon = Icons.Default.CreditCard
                            )
                            AdminKpiCard(
                                modifier = Modifier.weight(1f),
                                title = "PLATFORM CUT",
                                value = "₹${String.format("%,.0f", earnings?.systemCommissionsCollected ?: 0.0)}",
                                subtitle = "0% for Primo • 10% Std",
                                accentColor = Color(0xFF059669),
                                icon = Icons.Default.Percent
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminKpiCard(
                                modifier = Modifier.weight(1f),
                                title = "OPERATOR PAYOUTS",
                                value = "₹${String.format("%,.0f", earnings?.systemNetOperatorPayouts ?: 0.0)}",
                                subtitle = "Net Credited to Wallets",
                                accentColor = Color(0xFF4F46E5),
                                icon = Icons.Default.AccountBalanceWallet
                            )
                            AdminKpiCard(
                                modifier = Modifier.weight(1f),
                                title = "REFUNDS AUDITED",
                                value = "₹${String.format("%,.0f", earnings?.systemTotalRefundsProcessed ?: 0.0)}",
                                subtitle = "Disbursed to Passengers",
                                accentColor = RedbusCrimson,
                                icon = Icons.Default.Replay
                            )
                        }
                    }
                }

                val opEarnings = earnings?.operatorEarnings.orEmpty()
                if (opEarnings.isEmpty()) {
                    item {
                        AdminEmptyCard("No operator financial ledger entries recorded yet.")
                    }
                } else {
                    items(opEarnings, key = { "earn_${it.operatorId}" }) { item ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
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
                                    Column {
                                        Text(
                                            text = item.companyName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${item.email} • ${item.totalConfirmedBookings} Bookings",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "Wallet: ₹${String.format("%,.0f", item.walletBalance)}",
                                            color = Color(0xFF92400E),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Gross: ₹${String.format("%,.0f", item.grossRevenue)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Fee: ₹${String.format("%,.0f", item.commissionPaid)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                    Text(
                                        text = "Net: ₹${String.format("%,.0f", item.netEarnings)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF4F46E5)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AdminPortalTab.BANNERS -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "AI Promotional Banner Studio",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = aiPrompt,
                                onValueChange = { aiPrompt = it },
                                label = { Text("Suggestive AI Campaign Prompt") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false,
                                maxLines = 2
                            )
                            Button(
                                onClick = {
                                    onGenerateAiBanner(aiPrompt) { generated ->
                                        draftTitle = generated.title
                                        draftSubtitle = generated.subtitle.orEmpty()
                                        draftTag = generated.tag ?: "AI EXCLUSIVE DEAL"
                                        draftPromoCode = generated.promoCode ?: "REDAI20"
                                        draftRouteInfo = generated.routeInfo ?: "Pan-India Express"
                                        draftCtaText = generated.ctaText ?: "Claim Offer"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Campaign with AI", fontWeight = FontWeight.Bold)
                            }

                            HorizontalDivider()

                            OutlinedTextField(
                                value = draftTitle,
                                onValueChange = { draftTitle = it },
                                label = { Text("Banner Headline *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = draftSubtitle,
                                onValueChange = { draftSubtitle = it },
                                label = { Text("Subtitle / Offer Details") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = draftPromoCode,
                                    onValueChange = { draftPromoCode = it.uppercase() },
                                    label = { Text("Promo Code") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = draftRouteInfo,
                                    onValueChange = { draftRouteInfo = it },
                                    label = { Text("Route Info") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Button(
                                onClick = {
                                    if (draftTitle.isNotBlank()) {
                                        onPublishBanner(
                                            CreateBannerRequest(
                                                title = draftTitle.trim(),
                                                subtitle = draftSubtitle.trim(),
                                                tag = draftTag.trim(),
                                                promoCode = draftPromoCode.trim(),
                                                routeInfo = draftRouteInfo.trim(),
                                                ctaText = draftCtaText.trim()
                                            )
                                        ) {
                                            draftTitle = ""
                                            draftSubtitle = ""
                                        }
                                    }
                                },
                                enabled = draftTitle.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Publish Banner Live to Home Page", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                items(uiState.adminBanners, key = { "banner_${it.id}" }) { banner ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = banner.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${banner.tag ?: "OFFER"} • Code: ${banner.promoCode ?: "N/A"} • ${banner.routeInfo ?: "All Routes"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteBanner(banner.id, banner.title) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedbusCrimson)
                            }
                        }
                    }
                }
            }

            AdminPortalTab.ANALYTICS -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Top High-Demand Bus Corridors",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val corridors = stats?.topRoutes.orEmpty()
                            if (corridors.isEmpty()) {
                                Text(
                                    text = "No corridor analytics recorded yet.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                corridors.forEach { c ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${c.sourceCity} → ${c.destinationCity}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${c.busCount} Active Buses • From ₹${c.minPrice.toInt()}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "LIVE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminPortalTab.BOOKINGS -> {
                item {
                    Text(
                        text = "Master Passenger Bookings (${uiState.adminBookings.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (uiState.adminBookings.isEmpty()) {
                    item {
                        AdminEmptyCard("No bookings found in the master ledger.")
                    }
                } else {
                    items(uiState.adminBookings, key = { "adm_bk_${it.id}_${it.pnr}" }) { b ->
                        val isConfirmed = b.status.equals("CONFIRMED", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = b.pnr,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${b.passengers?.firstOrNull()?.name ?: "Passenger"} • ${b.contactEmail}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${b.sourceCity} → ${b.destinationCity} • Seats: ${b.passengers?.joinToString { it.seatNumber } ?: "-"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "₹${b.totalAmount.toInt()}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        color = if (isConfirmed) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = b.status,
                                            color = if (isConfirmed) Color(0xFF065F46) else Color(0xFF991B1B),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AdminKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdminEmptyCard(message: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PrimoMiniBadge() {
    Surface(
        color = Color(0xFF192857),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Primo",
                color = Color(0xFFFCD34D),
                fontSize = 10.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Black
            )
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
fun PrimoOnTimeBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF192857),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Primo",
                    color = Color(0xFFFCD34D),
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Black
                )
                Icon(
                    imageVector = Icons.Default.StarOutline,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = "On Time",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 9.sp
                )
                Text(
                    text = "Guarantee",
                    color = Color(0xFFFCD34D),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 9.sp
                )
            }
        }
    }
}

