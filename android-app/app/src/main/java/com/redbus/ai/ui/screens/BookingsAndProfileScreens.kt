package com.redbus.ai.ui.screens

import com.redbus.ai.BuildConfig
import com.redbus.ai.data.model.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.rotate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.redbus.ai.data.model.BookingDetails
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.localization.AppStrings
import com.redbus.ai.ui.viewmodel.RedbusUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

enum class RealtimeTripStatus(val label: String, val badgeColor: Color, val textColor: Color) {
    CONFIRMED("CONFIRMED", Color(0xFFDCFCE7), Color(0xFF15803D)),
    TRIP_STARTED("Trip Started · On the way", Color(0xFFEFF6FF), Color(0xFF1D4ED8)),
    TRIP_COMPLETED("TRIP COMPLETED", Color(0xFFF3F4F6), Color(0xFF6B7280)),
    CANCELLED("CANCELLED", Color(0xFFFEE2E2), Color(0xFFDC2626)),
    REFUNDED("CANCELLED", Color(0xFFFEE2E2), Color(0xFFDC2626))
}

enum class BookingTabFilter(val label: String) {
    UPCOMING("Upcoming"),
    PAST("Past Trips"),
    CANCELLED("Cancelled")
}

fun formatRelativeTripTime(booking: BookingDetails): String {
    if (booking.status.equals("CANCELLED", ignoreCase = true) || booking.status.equals("REFUNDED", ignoreCase = true)) {
        return ""
    }
    return try {
        val now = LocalDateTime.now()
        val travelDate = LocalDate.parse(booking.travelDate.trim())
        val depTime = parseFlexibleTime(booking.departureTime) ?: LocalTime.of(12, 0)
        val depDateTime = travelDate.atTime(depTime)
        val arrTime = parseFlexibleTime(booking.arrivalTime) ?: depTime.plusHours(7)
        val arrDateTime = if (arrTime.isBefore(depTime) || arrTime == depTime) {
            travelDate.plusDays(1).atTime(arrTime)
        } else {
            travelDate.atTime(arrTime)
        }
        if (now.isAfter(arrDateTime)) return ""
        if (!now.isBefore(depDateTime)) return "Trip Started"

        val diffMinutes = java.time.Duration.between(now, depDateTime).toMinutes()
        val diffHours = diffMinutes / 60.0
        val diffDays = diffHours / 24.0
        when {
            diffMinutes < 60 -> {
                val mins = maxOf(1L, diffMinutes)
                "Trip in $mins min${if (mins == 1L) "" else "s"}"
            }
            diffHours < 24.0 -> {
                val hrs = maxOf(1L, Math.round(diffHours))
                "Trip in $hrs hour${if (hrs == 1L) "" else "s"}"
            }
            diffDays < 7.0 -> {
                val days = maxOf(1L, Math.round(diffDays))
                "Trip in $days day${if (days == 1L) "" else "s"}"
            }
            diffDays < 30.0 -> {
                val weeks = maxOf(1L, Math.round(diffDays / 7.0))
                "Trip in $weeks week${if (weeks == 1L) "" else "s"}"
            }
            else -> {
                val months = maxOf(1L, Math.round(diffDays / 30.0))
                "Trip in $months month${if (months == 1L) "" else "s"}"
            }
        }
    } catch (_: Exception) {
        ""
    }
}

fun getBookingArnStr(booking: BookingDetails): String {
    if (!booking.refundArn.isNullOrBlank()) return booking.refundArn
    var hash = 106388800000L
    for (ch in booking.pnr) {
        hash += ch.code * 987L
    }
    return hash.toString().take(12)
}

fun formatRefundDateTimeStr(isoStr: String?, addMinutes: Long = 0, includeComma: Boolean = false): String {
    val dt = try {
        if (!isoStr.isNullOrBlank()) {
            LocalDateTime.parse(isoStr.take(19)).plusMinutes(addMinutes)
        } else {
            LocalDateTime.now().plusMinutes(addMinutes)
        }
    } catch (_: Exception) {
        LocalDateTime.now().plusMinutes(addMinutes)
    }
    val pattern = if (includeComma) "EEE, dd MMM yyyy, HH:mm" else "EEE dd MMM yyyy, HH:mm"
    return dt.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
}

fun parseFlexibleTime(timeStr: String?): LocalTime? {
    if (timeStr.isNullOrBlank()) return null
    return try {
        val clean = timeStr.trim().uppercase(Locale.ENGLISH)
        if (clean.contains("AM") || clean.contains("PM")) {
            val isPm = clean.contains("PM")
            val digits = clean.replace(Regex("[^0-9:]"), "").split(":")
            var h = digits.getOrNull(0)?.toIntOrNull() ?: 12
            val m = digits.getOrNull(1)?.toIntOrNull() ?: 0
            if (isPm && h < 12) h += 12
            if (!isPm && h == 12) h = 0
            LocalTime.of(h, m)
        } else {
            val parts = clean.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            LocalTime.of(h, m)
        }
    } catch (_: Exception) {
        null
    }
}

fun calculateTripStatus(booking: BookingDetails): RealtimeTripStatus {
    if (booking.status.equals("CANCELLED", ignoreCase = true)) return RealtimeTripStatus.CANCELLED
    if (booking.status.equals("REFUNDED", ignoreCase = true)) return RealtimeTripStatus.REFUNDED

    return try {
        val now = LocalDateTime.now()
        val travelDate = LocalDate.parse(booking.travelDate.trim())

        val depTime = parseFlexibleTime(booking.departureTime) ?: LocalTime.of(12, 0)
        val depDateTime = travelDate.atTime(depTime)

        val arrTime = parseFlexibleTime(booking.arrivalTime) ?: depTime.plusHours(7)
        val arrDateTime = if (arrTime.isBefore(depTime) || arrTime == depTime) {
            travelDate.plusDays(1).atTime(arrTime)
        } else {
            travelDate.atTime(arrTime)
        }

        when {
            now.isBefore(depDateTime) -> RealtimeTripStatus.CONFIRMED
            now.isAfter(arrDateTime) -> RealtimeTripStatus.TRIP_COMPLETED
            else -> RealtimeTripStatus.TRIP_STARTED
        }
    } catch (_: Exception) {
        if (booking.status.equals("CONFIRMED", ignoreCase = true)) RealtimeTripStatus.CONFIRMED else RealtimeTripStatus.CANCELLED
    }
}

data class CancellationSlab(
    val timeRange: String,
    val refundPct: Int,
    val feePct: Int,
    val isActive: Boolean
)

data class DynamicRefundInfo(
    val percentage: Int,
    val refundAmount: Double,
    val deductionAmount: Double,
    val isFreeCancellation: Boolean,
    val slabs: List<CancellationSlab>
)

fun calculateDynamicRefundInfo(booking: BookingDetails): DynamicRefundInfo {
    val isFree = booking.hasFreeCancellation == true
    if (isFree) {
        val freeFee = booking.freeCancellationFee ?: 0.0
        val refund = maxOf(0.0, booking.totalAmount - freeFee)
        return DynamicRefundInfo(
            percentage = 100,
            refundAmount = refund,
            deductionAmount = freeFee,
            isFreeCancellation = true,
            slabs = listOf(
                CancellationSlab("Anytime before departure", 100, 0, true)
            )
        )
    }

    var diffHours = 24.0
    try {
        if (booking.travelDate.isNotBlank()) {
            var cleanTime = booking.departureTime.trim().ifEmpty { "00:00:00" }
            if (cleanTime.matches(Regex("^\\d{1,2}:\\d{2}$"))) {
                cleanTime = cleanTime.padStart(5, '0') + ":00"
            }
            val depDateTime = java.time.LocalDateTime.parse("${booking.travelDate}T$cleanTime")
            val now = java.time.LocalDateTime.now()
            diffHours = java.time.Duration.between(now, depDateTime).toMinutes() / 60.0
        }
    } catch (_: Exception) {
        diffHours = 24.0
    }

    val activeSlabIndex = when {
        diffHours >= 24.0 -> 0
        diffHours >= 12.0 -> 1
        diffHours >= 2.0 -> 2
        else -> 3
    }

    val slabs = listOf(
        CancellationSlab("More than 24 hrs before departure", 90, 10, activeSlabIndex == 0),
        CancellationSlab("Between 12 to 24 hrs before departure", 75, 25, activeSlabIndex == 1),
        CancellationSlab("Between 2 to 12 hrs before departure", 50, 50, activeSlabIndex == 2),
        CancellationSlab("Within 2 hrs of departure", 25, 75, activeSlabIndex == 3)
    )

    val activeSlab = slabs[activeSlabIndex]
    val feePct = activeSlab.feePct
    val refundPct = activeSlab.refundPct
    val deductionAmount = booking.totalAmount * (feePct / 100.0)
    val refundAmount = maxOf(0.0, booking.totalAmount - deductionAmount)

    return DynamicRefundInfo(
        percentage = refundPct,
        refundAmount = refundAmount,
        deductionAmount = deductionAmount,
        isFreeCancellation = false,
        slabs = slabs
    )
}

@Composable
fun CancelTicketWebsiteModal(
    booking: BookingDetails,
    onDismiss: () -> Unit,
    onConfirmCancel: (pnr: String, reason: String, refundDestination: String) -> Unit
) {
    var cancelReason by remember { mutableStateOf("Change of plans") }
    var refundDestination by remember { mutableStateOf("WALLET") }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }
    var showTermsWarning by remember { mutableStateOf(false) }
    var agreeToTerms by remember { mutableStateOf(false) }

    val refundInfo = remember(booking) { calculateDynamicRefundInfo(booking) }
    val reasonsList = listOf(
        "Change of plans",
        "Booked by mistake",
        "Found alternative transport",
        "Medical emergency",
        "Trip postponed"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Header (RotateCcw, Title, Route & Date badge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RedbusCrimson.copy(alpha = 0.1f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = RedbusCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cancel Ticket (${booking.pnr})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${booking.sourceCity} ➔ ${booking.destinationCity} (${booking.operatorName})",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3F4F6)
                        ) {
                            Text(
                                text = booking.travelDate.ifBlank { "2026-11-01" },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF3F4F6))

                    // 2. Official Cancellation Policy Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OFFICIAL CANCELLATION POLICY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF374151),
                                letterSpacing = 0.5.sp
                            )
                            if (refundInfo.isFreeCancellation) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFECFDF5),
                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                ) {
                                    Text(
                                        text = "🛡️ Free Cancellation Applied",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Policy Table
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column {
                                // Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Time of Cancellation",
                                        modifier = Modifier.weight(2.4f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Refund %",
                                        modifier = Modifier.weight(0.8f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Fee %",
                                        modifier = Modifier.weight(0.8f),
                                        textAlign = TextAlign.End,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Slab rows
                                refundInfo.slabs.forEach { slab ->
                                    HorizontalDivider(color = Color(0xFFF3F4F6))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (slab.isActive) Color(0xFFECFDF5) else Color.White)
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(2.4f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (slab.isActive) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(Color(0xFF10B981), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = slab.timeRange,
                                                fontSize = 11.sp,
                                                fontWeight = if (slab.isActive) FontWeight.Bold else FontWeight.Medium,
                                                color = if (slab.isActive) Color(0xFF065F46) else Color(0xFF4B5563)
                                            )
                                        }
                                        Text(
                                            text = "${slab.refundPct}%",
                                            modifier = Modifier.weight(0.8f),
                                            textAlign = TextAlign.Center,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857)
                                        )
                                        Text(
                                            text = "${slab.feePct}%",
                                            modifier = Modifier.weight(0.8f),
                                            textAlign = TextAlign.End,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Dynamic Financial Breakdown Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Paid Fare:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151)
                                )
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", booking.totalAmount)}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Cancellation Charges (${100 - refundInfo.percentage}%):",
                                    fontSize = 12.sp,
                                    color = Color(0xFFDC2626)
                                )
                                Text(
                                    text = "-₹${String.format(Locale.US, "%.2f", refundInfo.deductionAmount)}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Estimated Refund Amount:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                }
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", refundInfo.refundAmount)}",
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }

                    // 4. Where would you like to receive your refund?
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Where would you like to receive your refund? *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Option 1: redBus Wallet
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                if (refundDestination == "WALLET") 2.dp else 1.dp,
                                if (refundDestination == "WALLET") Color(0xFF10B981) else Color(0xFFE5E7EB)
                            ),
                            color = if (refundDestination == "WALLET") Color(0xFFECFDF5) else Color.White,
                            onClick = { refundDestination = "WALLET" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "redBus Wallet",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF065F46)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFD1FAE5),
                                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                    ) {
                                        Text(
                                            text = "RECOMMENDED (INSTANT)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF065F46),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Instant balance upon operator audit approval. 100% usable on any bus ticket.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Option 2: Original Method
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                if (refundDestination == "ORIGINAL_PAYMENT") 2.dp else 1.dp,
                                if (refundDestination == "ORIGINAL_PAYMENT") Color(0xFF3B82F6) else Color(0xFFE5E7EB)
                            ),
                            color = if (refundDestination == "ORIGINAL_PAYMENT") Color(0xFFEFF6FF) else Color.White,
                            onClick = { refundDestination = "ORIGINAL_PAYMENT" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Original Method",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF1E40AF)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFDBEAFE),
                                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                    ) {
                                        Text(
                                            text = "3-5 DAYS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF1E40AF),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Settlement returned to original bank/card/UPI account within 3-5 business days.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // 5. Reason for cancellation selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Reason for cancellation *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { reasonDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cancelReason,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF6B7280)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = reasonDropdownExpanded,
                                onDismissRequest = { reasonDropdownExpanded = false }
                            ) {
                                reasonsList.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r, fontSize = 12.sp, fontWeight = if (cancelReason == r) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            cancelReason = r
                                            reasonDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 6. Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF3F4F6),
                                contentColor = Color(0xFF374151)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Keep Booking", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showTermsWarning = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedbusCrimson,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Review Terms & Cancel (₹${String.format(Locale.US, "%.2f", refundInfo.refundAmount)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // High-Attention Terms Review Dialog Modal
    if (showTermsWarning) {
        Dialog(
            onDismissRequest = { showTermsWarning = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.dp, Color(0xFFFCA5A5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = RedbusCrimson.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = RedbusCrimson,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Confirm Cancellation",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "PNR: ${booking.pnr} · ${booking.sourceCity} ➔ ${booking.destinationCity}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Fare Paid:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("₹${String.format(Locale.US, "%.2f", booking.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cancellation Fee:", fontSize = 12.sp, color = Color(0xFFDC2626))
                                    Text("-₹${String.format(Locale.US, "%.2f", refundInfo.deductionAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFFDC2626))
                                }
                                HorizontalDivider(color = Color(0xFFFECACA))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Refund Due:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                    Text("₹${String.format(Locale.US, "%.2f", refundInfo.refundAmount)}", fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Color(0xFF047857))
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("1. Non-Reversible Action: All booked seats will immediately be released to other passengers.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
                            Text("2. Settlement: Refund will be processed to ${if (refundDestination == "WALLET") "redBus Wallet (Instant)" else "Original Payment Account (3-5 business days)"}.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
                            Text("3. Cancellation Receipt: Dispatched to ${booking.contactEmail.ifBlank { "registered email" }}.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { agreeToTerms = !agreeToTerms }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = agreeToTerms,
                                onCheckedChange = { agreeToTerms = it },
                                colors = CheckboxDefaults.colors(checkedColor = RedbusCrimson)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "I have reviewed and agree to the Cancellation Policy & Refund Terms",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showTermsWarning = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF3F4F6),
                                    contentColor = Color(0xFF374151)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text("Go Back", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    showTermsWarning = false
                                    onConfirmCancel(booking.pnr, cancelReason, refundDestination)
                                },
                                enabled = agreeToTerms,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RedbusCrimson,
                                    contentColor = Color.White,
                                    disabledContainerColor = RedbusCrimson.copy(alpha = 0.4f),
                                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(46.dp)
                            ) {
                                Text("Yes, Confirm Cancellation", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class AppNotificationData(
    val id: String,
    val type: String, // "BOOKING", "OFFER", "ALERT"
    val title: String,
    val message: String,
    val timestamp: String,
    val actionLabel: String? = null,
    val onClickAction: () -> Unit = {}
)

@Composable
fun NotificationsDropdownModal(
    uiState: RedbusUiState,
    onDismiss: () -> Unit,
    onNavigateToBookings: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onOpenTicket: (BookingDetails) -> Unit = {},
    onMarkAsRead: (String) -> Unit = {},
    onMarkAllAsRead: (Collection<String>) -> Unit = {},
    onDismissNotification: (String) -> Unit = {},
    onClearAllNotifications: (Collection<String>) -> Unit = {}
) {
    var activeTab by remember { mutableStateOf("ALL") }

    val todayStr = remember {
        java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
    }

    val allNotifications = remember(uiState.myBookings, uiState.availableCoupons, uiState.user?.walletBalance, todayStr) {
        val list = mutableListOf<AppNotificationData>()

        // 1. From real bookings
        uiState.myBookings.forEach { b ->
            when (b.status.uppercase(Locale.US)) {
                "CONFIRMED" -> {
                    val isToday = b.travelDate == todayStr
                    val seatsStr = b.passengers?.mapNotNull { it.seatNumber }?.filter { it.isNotBlank() }?.joinToString(", ") ?: "Confirmed"
                    if (isToday) {
                        list.add(
                            AppNotificationData(
                                id = "booking-live-${b.pnr}",
                                type = "BOOKING",
                                title = "Live Boarding Today! 📍",
                                message = "Your bus ${b.operatorName} (${b.sourceCity} ➔ ${b.destinationCity}) departs today at ${b.departureTime}. Boarding at ${b.boardingPoint.ifBlank { "Main Terminal" }}. PNR: ${b.pnr}",
                                timestamp = "Today at ${b.departureTime}",
                                actionLabel = "View Ticket >",
                                onClickAction = {
                                    onDismiss()
                                    onOpenTicket(b)
                                }
                            )
                        )
                    } else {
                        list.add(
                            AppNotificationData(
                                id = "booking-conf-${b.pnr}",
                                type = "BOOKING",
                                title = "Trip Confirmed! 🚌",
                                message = "Booking confirmed for ${b.sourceCity} ➔ ${b.destinationCity} on ${b.travelDate} (${b.departureTime}) with ${b.operatorName}. Seat(s): $seatsStr. PNR: ${b.pnr}",
                                timestamp = b.travelDate,
                                actionLabel = "View Ticket >",
                                onClickAction = {
                                    onDismiss()
                                    onOpenTicket(b)
                                }
                            )
                        )
                    }
                }
                "CANCELLED", "REFUNDED" -> {
                    val refund = if ((b.refundAmount ?: 0.0) > 0.0) b.refundAmount!! else b.totalAmount
                    list.add(
                        AppNotificationData(
                            id = "booking-cancel-${b.pnr}",
                            type = "ALERT",
                            title = "Refund Credited to Wallet 💰",
                            message = "Booking ${b.pnr} (${b.sourceCity} ➔ ${b.destinationCity}) was cancelled. Refund of ₹${String.format(Locale.US, "%.2f", refund)} credited to your redBus Wallet.",
                            timestamp = b.travelDate.ifBlank { "2026-11-01" },
                            actionLabel = "Check Wallet >",
                            onClickAction = {
                                onDismiss()
                                onNavigateToWallet()
                            }
                        )
                    )
                }
            }
        }

        // 2. From Available Coupons
        uiState.availableCoupons.forEach { c ->
            list.add(
                AppNotificationData(
                    id = "coupon-${c.code}",
                    type = "OFFER",
                    title = "Festive Promo ${c.code} Active 🎉",
                    message = "Save up to ₹${c.maxDiscountAmount?.toInt() ?: 200} on all routes with promo code ${c.code}.",
                    timestamp = "Valid till ${c.expiryDate ?: "2026-12-31"}",
                    actionLabel = "Apply Promo >",
                    onClickAction = {
                        onDismiss()
                        onNavigateToBookings()
                    }
                )
            )
        }

        // 3. Fallbacks if list has few items
        if (list.none { it.type == "OFFER" }) {
            list.add(
                AppNotificationData(
                    id = "offer-luxury20",
                    type = "OFFER",
                    title = "Festive Promo LUXURY20 Active 🎉",
                    message = "Save up to ₹200 on all Primo AC Sleeper routes with promo code LUXURY20.",
                    timestamp = "Valid till 2026-12-31",
                    actionLabel = "Apply Promo >",
                    onClickAction = {
                        onDismiss()
                        onNavigateToBookings()
                    }
                )
            )
        }
        list.add(
            AppNotificationData(
                id = "safety-standard-alert",
                type = "ALERT",
                title = "Primo Safety Standard Verified 🛡️",
                message = "All partner fleets sanitized and equipped with verified emergency contact tools.",
                timestamp = "Verified Today",
                actionLabel = null,
                onClickAction = {}
            )
        )

        list
    }

    val visibleNotifs = allNotifications.filter { it.id !in uiState.dismissedNotificationIds }
    val filteredNotifs = visibleNotifs.filter {
        when (activeTab) {
            "BOOKING" -> it.type == "BOOKING"
            "OFFER" -> it.type == "OFFER"
            "ALERT" -> it.type == "ALERT"
            else -> true
        }
    }
    val unreadCount = visibleNotifs.count { it.id !in uiState.readNotificationIds }
    val userName = uiState.user?.name?.split(" ")?.firstOrNull() ?: "Jaiganesh"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RedbusCrimson.copy(alpha = 0.12f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = RedbusCrimson,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Notifications",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (unreadCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = RedbusCrimson
                                        ) {
                                            Text(
                                                text = "$unreadCount new",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Live trip updates for $userName",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (unreadCount > 0) {
                                Text(
                                    text = "✓ Mark all read",
                                    fontSize = 11.sp,
                                    color = RedbusCrimson,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable {
                                            onMarkAllAsRead(visibleNotifs.map { it.id })
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF3F4F6))

                    // Category Tabs (All, Bookings, Offers, Alerts)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All",
                            "BOOKING" to "Bookings",
                            "OFFER" to "Offers",
                            "ALERT" to "Alerts"
                        ).forEach { (tabKey, label) ->
                            val isSelected = activeTab == tabKey
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFFEF2F2) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, Color(0xFFFECACA)) else null,
                                onClick = { activeTab = tabKey }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) RedbusCrimson else Color(0xFF6B7280),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF3F4F6))

                    // List of Notifications
                    if (filteredNotifs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFFD1D5DB),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No notifications right now",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "We will alert you about your real-time trips and offers here.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredNotifs, key = { it.id }) { notif ->
                                val isRead = notif.id in uiState.readNotificationIds
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (!isRead) Color(0xFFFFF5F5) else Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        val iconBg = when (notif.type) {
                                            "BOOKING" -> if (notif.title.contains("Live", ignoreCase = true)) Color(0xFFFEE2E2) else Color(0xFFDBEAFE)
                                            "OFFER" -> Color(0xFFFEF3C7)
                                            else -> Color(0xFFD1FAE5)
                                        }
                                        val iconTint = when (notif.type) {
                                            "BOOKING" -> if (notif.title.contains("Live", ignoreCase = true)) RedbusCrimson else Color(0xFF2563EB)
                                            "OFFER" -> RatingAmber
                                            else -> Color(0xFF059669)
                                        }
                                        val iconVec = when (notif.type) {
                                            "BOOKING" -> if (notif.title.contains("Live", ignoreCase = true)) Icons.Default.LocationOn else Icons.Default.ConfirmationNumber
                                            "OFFER" -> Icons.Default.LocalOffer
                                            else -> Icons.Default.AccountBalanceWallet
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = iconBg,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = iconVec,
                                                    contentDescription = null,
                                                    tint = iconTint,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = notif.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (!isRead) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .background(RedbusCrimson, CircleShape)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = notif.message,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 15.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = notif.timestamp,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF9CA3AF),
                                                    fontWeight = FontWeight.Medium
                                                )

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (notif.actionLabel != null) {
                                                        Text(
                                                            text = notif.actionLabel,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = RedbusCrimson,
                                                            modifier = Modifier.clickable {
                                                                onMarkAsRead(notif.id)
                                                                notif.onClickAction()
                                                            }
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Dismiss",
                                                        tint = Color(0xFF9CA3AF),
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clickable {
                                                                onDismissNotification(notif.id)
                                                            }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Footer
                    if (visibleNotifs.isNotEmpty()) {
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    onClearAllNotifications(visibleNotifs.map { it.id })
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Clear all",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Text(
                                text = "View My Bookings →",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedbusCrimson,
                                modifier = Modifier.clickable {
                                    onDismiss()
                                    onNavigateToBookings()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyBookingsScreen(
    uiState: RedbusUiState,
    onSearchPnr: (String) -> Unit,
    onRefreshBookings: () -> Unit,
    onCancelBooking: (String, String, String) -> Unit,
    onChangeTripPoints: (String, String?, String?, () -> Unit, (String) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onChangeJourneyDate: (String, String?, Long?, List<String>?, () -> Unit, (String) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onEmailTicket: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onSubmitReview: (Long, Int, String, String?) -> Unit = { _, _, _, _ -> },
    onNavigateToAiHelp: () -> Unit = {},
    onMarkNotificationAsRead: (String) -> Unit = {},
    onMarkAllNotificationsAsRead: (Collection<String>) -> Unit = {},
    onDismissNotification: (String) -> Unit = {},
    onClearAllNotifications: (Collection<String>) -> Unit = {},
    onOpenBusSearchForDate: (String, String, String) -> Unit = { _, _, _ -> }
) {
    var pnrQuery by remember { mutableStateOf("") }
    var activeTabFilter by remember { mutableStateOf(BookingTabFilter.UPCOMING) }
    var bookingToCancel by remember { mutableStateOf<BookingDetails?>(null) }
    var selectedTicketToView by remember { mutableStateOf<BookingDetails?>(null) }
    var perforatedTicketToView by remember { mutableStateOf<BookingDetails?>(null) }

    LaunchedEffect(uiState.myBookings, uiState.pnrSearchResult) {
        selectedTicketToView?.let { current ->
            val fresh = uiState.myBookings.find { it.pnr == current.pnr }
                ?: uiState.pnrSearchResult?.takeIf { it.pnr == current.pnr }
            if (fresh != null) selectedTicketToView = fresh
        }
        perforatedTicketToView?.let { current ->
            val fresh = uiState.myBookings.find { it.pnr == current.pnr }
                ?: uiState.pnrSearchResult?.takeIf { it.pnr == current.pnr }
            if (fresh != null) perforatedTicketToView = fresh
        }
    }
    var bookingToRate by remember { mutableStateOf<BookingDetails?>(null) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("Change of plans") }
    var refundDestination by remember { mutableStateOf("WALLET") }

    val displayedBookings = remember(uiState.pnrSearchResult, uiState.myBookings) {
        if (uiState.pnrSearchResult != null) {
            listOf(uiState.pnrSearchResult)
        } else {
            uiState.myBookings
        }
    }

    val filteredBookings = remember(displayedBookings, activeTabFilter) {
        when (activeTabFilter) {
            BookingTabFilter.UPCOMING -> displayedBookings.filter {
                val s = calculateTripStatus(it)
                s == RealtimeTripStatus.CONFIRMED || s == RealtimeTripStatus.TRIP_STARTED
            }
            BookingTabFilter.PAST -> displayedBookings.filter {
                calculateTripStatus(it) == RealtimeTripStatus.TRIP_COMPLETED
            }
            BookingTabFilter.CANCELLED -> displayedBookings.filter {
                val s = calculateTripStatus(it)
                s == RealtimeTripStatus.CANCELLED || s == RealtimeTripStatus.REFUNDED
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        Surface(
            color = RedbusCrimson,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            text = "Tap any ticket to view perforated e-ticket & share",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val todayIso = remember { java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) }
                        val hasUnreadNotifs = remember(uiState.myBookings, uiState.availableCoupons, uiState.readNotificationIds, uiState.dismissedNotificationIds, todayIso) {
                            val list = mutableListOf<String>()
                            uiState.myBookings.forEach { b ->
                                when (b.status.uppercase(java.util.Locale.US)) {
                                    "CONFIRMED" -> {
                                        if (b.travelDate == todayIso) list.add("booking-live-${b.pnr}") else list.add("booking-conf-${b.pnr}")
                                    }
                                    "CANCELLED", "REFUNDED" -> list.add("booking-cancel-${b.pnr}")
                                }
                            }
                            uiState.availableCoupons.forEach { c -> list.add("coupon-${c.code}") }
                            if (list.none { it.startsWith("coupon-") }) list.add("offer-luxury20")
                            list.add("safety-standard-alert")
                            list.filter { it !in uiState.dismissedNotificationIds }.any { it !in uiState.readNotificationIds }
                        }
                        IconButton(onClick = { showNotificationsModal = true }) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.White
                                )
                                if (hasUnreadNotifs) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFFFDE047), CircleShape)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onRefreshBookings) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Bookings",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Live PNR Search Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                    Button(
                        onClick = { onSearchPnr(pnrQuery) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = RedbusCrimson
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp)
                    ) {
                        Text("Find PNR", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                // Divided booking filter tabs: Upcoming | Past Trips | Cancelled
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(BookingTabFilter.values()) { tab ->
                        val isSelected = activeTabFilter == tab
                        val count = when (tab) {
                            BookingTabFilter.UPCOMING -> displayedBookings.count {
                                val s = calculateTripStatus(it)
                                s == RealtimeTripStatus.CONFIRMED || s == RealtimeTripStatus.TRIP_STARTED
                            }
                            BookingTabFilter.PAST -> displayedBookings.count {
                                calculateTripStatus(it) == RealtimeTripStatus.TRIP_COMPLETED
                            }
                            BookingTabFilter.CANCELLED -> displayedBookings.count {
                                val s = calculateTripStatus(it)
                                s == RealtimeTripStatus.CANCELLED || s == RealtimeTripStatus.REFUNDED
                            }
                        }
                        Surface(
                            onClick = { activeTabFilter = tab },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tab.label,
                                    color = if (isSelected) RedbusCrimson else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) RedbusCrimson.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "$count",
                                        color = if (isSelected) RedbusCrimson else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = RedbusCrimson,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Sign In to View Your Bookings",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Sign in with your redBus AI account to see all your bus tickets, or search any ticket above using its PNR.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onNavigateToLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Text("Sign In to Account", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (filteredBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = when (activeTabFilter) {
                            BookingTabFilter.UPCOMING -> "No upcoming trips found"
                            BookingTabFilter.PAST -> "No past trips found"
                            BookingTabFilter.CANCELLED -> "No cancelled trips"
                        },
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredBookings, key = { it.pnr.ifBlank { it.id.toString() } }) { booking ->
                    BookingTicketCard(
                        booking = booking,
                        isReviewed = booking.userRating != null || uiState.reviewedPnrs.containsKey(booking.pnr),
                        onViewTicketClick = { selectedTicketToView = booking },
                        onCancelClick = { bookingToCancel = booking },
                        onEmailClick = { onEmailTicket(booking.pnr) },
                        onRateClick = { bookingToRate = booking }
                    )
                }
            }
        }
    }

        // Floating Circular AI Button (Matching Image 3)
        FloatingAiHelpButton(
            onClick = onNavigateToAiHelp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 18.dp)
        )
    }

    // Enhanced Ticket Journey Modal (Matching Image 4, Image 2, Image 3)
    selectedTicketToView?.let { ticketBooking ->
        EnhancedTicketJourneyModal(
            booking = ticketBooking,
            reviewedPnrs = uiState.reviewedPnrs,
            onDismiss = { selectedTicketToView = null },
            onOpenPerforatedTicket = {
                perforatedTicketToView = ticketBooking
            },
            onChangeTripPoints = { pnr, bp, dp, onSuccess, onError ->
                onChangeTripPoints(pnr, bp, dp, {
                    onSuccess()
                    onRefreshBookings()
                }, onError)
            },
            onChangeJourneyDate = { pnr, newDate, newRouteId, newSeatNumbers, onSuccess, onError ->
                onChangeJourneyDate(pnr, newDate, newRouteId, newSeatNumbers, {
                    onSuccess()
                    onRefreshBookings()
                }, onError)
            },
            onCancelBooking = {
                bookingToCancel = ticketBooking
            },
            onSubmitReview = { busId, rating, comment, pnr ->
                onSubmitReview(busId, rating, comment, pnr)
            },
            onHelpClick = onNavigateToAiHelp,
            onOpenBusSearchForDate = { src, dst, dateStr ->
                selectedTicketToView = null
                onOpenBusSearchForDate(src, dst, dateStr)
            }
        )
    }

    // Perforated View Ticket Modal (Matching Image 5)
    perforatedTicketToView?.let { ticketBooking ->
        ViewTicketDetailsModal(
            booking = ticketBooking,
            onDismiss = { perforatedTicketToView = null },
            onCancelBooking = {
                perforatedTicketToView = null
                bookingToCancel = ticketBooking
            }
        )
    }

    // Rate Journey Experience & Hygiene Review Modal
    bookingToRate?.let { b ->
        RateJourneyModal(
            booking = b,
            onDismiss = { bookingToRate = null },
            onSubmitReview = { busId, rating, comment, pnr ->
                onSubmitReview(busId, rating, comment, pnr)
            }
        )
    }

    if (showNotificationsModal) {
        NotificationsDropdownModal(
            uiState = uiState,
            onDismiss = { showNotificationsModal = false },
            onNavigateToBookings = { showNotificationsModal = false },
            onNavigateToWallet = { showNotificationsModal = false },
            onOpenTicket = { ticket ->
                selectedTicketToView = ticket
            },
            onMarkAsRead = onMarkNotificationAsRead,
            onMarkAllAsRead = onMarkAllNotificationsAsRead,
            onDismissNotification = onDismissNotification,
            onClearAllNotifications = onClearAllNotifications
        )
    }

    if (bookingToCancel != null) {
        val b = bookingToCancel!!
        CancelTicketWebsiteModal(
            booking = b,
            onDismiss = { bookingToCancel = null },
            onConfirmCancel = { pnr, reason, refundDest ->
                onCancelBooking(pnr, reason, refundDest)
                bookingToCancel = null
            }
        )
    }
}

@Composable
private fun BookingTicketCard(
    booking: BookingDetails,
    isReviewed: Boolean = false,
    onViewTicketClick: () -> Unit,
    onCancelClick: () -> Unit,
    onEmailClick: () -> Unit,
    onRateClick: () -> Unit = {}
) {
    val tripStatus = calculateTripStatus(booking)
    val cardScale = remember { Animatable(0.95f) }
    val cardRotX = remember { Animatable(8f) }
    LaunchedEffect(booking.pnr) {
        cardScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }
    LaunchedEffect(booking.pnr) {
        cardRotX.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
    }

    Card(
        onClick = onViewTicketClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale.value
                scaleY = cardScale.value
                rotationX = cardRotX.value
                cameraDistance = 14f * density
            }
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = tripStatus.badgeColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "● ${tripStatus.label}",
                        color = tripStatus.textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                val relTime = remember(booking.travelDate, booking.departureTime, booking.status) {
                    formatRelativeTripTime(booking)
                }
                if (relTime.isNotBlank() && (tripStatus == RealtimeTripStatus.CONFIRMED || tripStatus == RealtimeTripStatus.TRIP_STARTED)) {
                    Surface(
                        color = Color(0xFFEEF2FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = relTime,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color(0xFF4F46E5)
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = "${booking.sourceCity} ➔ ${booking.destinationCity}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${booking.operatorName} • ${booking.busType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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

            Text(
                text = "Boarding: ${booking.boardingPoint} • Dropping: ${booking.droppingPoint}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", booking.totalAmount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onViewTicketClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedbusCrimson,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = "View Ticket",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("View Ticket", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onEmailClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email E-Ticket",
                            tint = AiViolet,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    when (tripStatus) {
                        RealtimeTripStatus.CONFIRMED -> {
                            OutlinedButton(
                                onClick = onCancelClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        RealtimeTripStatus.TRIP_COMPLETED -> {
                            val isRated = isReviewed || booking.userRating != null
                            Button(
                                onClick = onRateClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRated) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                                    contentColor = if (isRated) Color(0xFFB45309) else Color(0xFF15803D)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isRated) Color(0xFFB45309) else Color(0xFF15803D),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isRated) "Rated ★" else "Rate Journey",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        RealtimeTripStatus.TRIP_STARTED -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFF93C5FD))
                            ) {
                                Text(
                                    text = "On the way",
                                    color = Color(0xFF1D4ED8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                        else -> { /* Cancelled or Refunded */ }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Booked Seat Layout Modal (Matching Image 3 & Image 4)
 */
@Composable
private fun BookedSeatLayoutModal(
    booking: BookingDetails,
    onDismiss: () -> Unit
) {
    val bookedSeatNumbers = remember(booking.passengers) {
        booking.passengers.orEmpty().mapNotNull { it.seatNumber.trim().ifEmpty { null } }.toSet()
    }

    val isSleeper = remember(booking.busType, bookedSeatNumbers) {
        booking.busType.contains("Sleeper", ignoreCase = true) ||
        bookedSeatNumbers.any { it.startsWith("U", ignoreCase = true) || it.startsWith("L", ignoreCase = true) }
    }

    // Lower Deck 1+2 Sleeper Berths (Image 4: L1-L15)
    val lowerDeckRows = listOf(
        Triple("L1", "L2", "L3"),
        Triple("L4", "L5", "L6"),
        Triple("L7", "L8", "L9"),
        Triple("L10", "L11", "L12"),
        Triple("L13", "L14", "L15")
    )

    // Upper Deck 1+2 Sleeper Berths (Image 4: U1-U15)
    val upperDeckRows = listOf(
        Triple("U1", "U2", "U3"),
        Triple("U4", "U5", "U6"),
        Triple("U7", "U8", "U9"),
        Triple("U10", "U11", "U12"),
        Triple("U13", "U14", "U15")
    )

    // 2+2 Seater Fallback
    val seaterRows = listOf(
        listOf("S1", "S2", "AISLE", "S3", "S4"),
        listOf("S5", "S6", "AISLE", "S7", "S8"),
        listOf("S9", "S10", "AISLE", "S11", "S12"),
        listOf("S13", "S14", "AISLE", "S15", "S16"),
        listOf("S17", "S18", "AISLE", "S19", "S20"),
        listOf("S21", "S22", "AISLE", "S23", "S24")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Booked Seat Map",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${booking.operatorName} • ${booking.busType}",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f), modifier = Modifier.padding(vertical = 10.dp))

                // Dual-Deck Body (Matching Image 3)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isSleeper) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Left Card: Lower Deck
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Lower Deck Header with Steering Wheel Icon (Image 3)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Lower deck",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFE2E8F0),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("✇", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    // Lower Deck Berths
                                    lowerDeckRows.forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Single Berth Left (L1, L4, ...)
                                            val isBookedLeft = bookedSeatNumbers.any { it.equals(row.first, ignoreCase = true) }
                                            BerthItem(seatNum = row.first, isBooked = isBookedLeft)

                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Double Berths Right (L2-L3, L5-L6, ...)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                val isBookedMid = bookedSeatNumbers.any { it.equals(row.second, ignoreCase = true) }
                                                val isBookedRight = bookedSeatNumbers.any { it.equals(row.third, ignoreCase = true) }
                                                BerthItem(seatNum = row.second, isBooked = isBookedMid)
                                                BerthItem(seatNum = row.third, isBooked = isBookedRight)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "▼ Emergency Exit",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Right Card: Upper Deck
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Upper Deck Header with UP tag (Image 3)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Upper deck",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE2E8F0)
                                        ) {
                                            Text(
                                                text = "UP",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    // Upper Deck Berths
                                    upperDeckRows.forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Single Berth Left (U1, U4, ...)
                                            val isBookedLeft = bookedSeatNumbers.any { it.equals(row.first, ignoreCase = true) }
                                            BerthItem(seatNum = row.first, isBooked = isBookedLeft)

                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Double Berths Right (U2-U3, U5-U6, ...)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                val isBookedMid = bookedSeatNumbers.any { it.equals(row.second, ignoreCase = true) }
                                                val isBookedRight = bookedSeatNumbers.any { it.equals(row.third, ignoreCase = true) }
                                                BerthItem(seatNum = row.second, isBooked = isBookedMid)
                                                BerthItem(seatNum = row.third, isBooked = isBookedRight)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "▼ Emergency Exit",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    } else {
                        // Seater 2+2 Layout
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            seaterRows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    row.forEach { seatNum ->
                                        if (seatNum == "AISLE") {
                                            Spacer(modifier = Modifier.width(20.dp))
                                        } else {
                                            val isBooked = bookedSeatNumbers.any { it.equals(seatNum, ignoreCase = true) }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isBooked) Color(0xFFEC4899) else Color(0xFFE2E8F0),
                                                border = BorderStroke(1.dp, if (isBooked) Color(0xFFBE185D) else Color(0xFFCBD5E1)),
                                                modifier = Modifier
                                                    .width(46.dp)
                                                    .height(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = seatNum,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isBooked) Color.White else Color(0xFF64748B)
                                                        )
                                                        if (isBooked) {
                                                            Text(
                                                                text = "YOURS",
                                                                fontSize = 7.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = Color(0xFFFEF08A)
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
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f), modifier = Modifier.padding(vertical = 10.dp))

                // Clean Legend (Image 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEC4899),
                            modifier = Modifier.size(14.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Your Booked Seat (${bookedSeatNumbers.joinToString(", ").ifEmpty { "Yours" }})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE2E8F0),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.size(14.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Other Berths", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun BerthItem(seatNum: String, isBooked: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isBooked) Color(0xFFEC4899) else Color(0xFFE2E8F0),
        border = BorderStroke(
            if (isBooked) 1.5.dp else 1.dp,
            if (isBooked) Color(0xFFBE185D) else Color(0xFFCBD5E1)
        ),
        shadowElevation = if (isBooked) 4.dp else 0.dp,
        modifier = Modifier
            .width(42.dp)
            .height(64.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Pillow contour top
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = if (isBooked) Color.White.copy(alpha = 0.45f) else Color(0xFFCBD5E1).copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
            ) {}

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = seatNum,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isBooked) Color.White else Color(0xFF94A3B8)
                )
                if (isBooked) {
                    Text(
                        text = "YOURS",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFEF08A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/**
 * Bottom Sheet / Modal to change Boarding & Dropping Points (Matching Image 1)
 */
@Composable
private fun ChangeTripPointsBottomSheet(
    booking: BookingDetails,
    onDismiss: () -> Unit,
    onConfirm: (newBoarding: String, newDropping: String) -> Unit
) {
    var selectedBoarding by remember { mutableStateOf(booking.boardingPoint) }
    var selectedDropping by remember { mutableStateOf(booking.droppingPoint) }
    var editingMode by remember { mutableStateOf<String?>("MENU") } // "MENU", "BOARDING", "DROPPING"

    val boardingOptions = remember(booking.sourceCity, booking.availableBoardingPoints) {
        val parsed = booking.availableBoardingPoints?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        if (!parsed.isNullOrEmpty()) parsed else listOf(
            "${booking.sourceCity} Central Omni Bus Stand (21:30)",
            "${booking.sourceCity} Marathahalli Multiplex (21:30)",
            "${booking.sourceCity} Madiwala Petrol Bunk (22:00)",
            "${booking.sourceCity} Electronic City Toll Plaza (22:30)",
            "${booking.sourceCity} Bypass Junction (23:00)"
        )
    }

    val droppingOptions = remember(booking.destinationCity, booking.availableDroppingPoints) {
        val parsed = booking.availableDroppingPoints?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        if (!parsed.isNullOrEmpty()) parsed else listOf(
            "${booking.destinationCity} Poonamallee (05:25)",
            "${booking.destinationCity} Koyambedu Omni Bus Stand (05:30)",
            "${booking.destinationCity} Guindy Kathipara (05:45)",
            "${booking.destinationCity} Perungalathur (06:00)",
            "${booking.destinationCity} Central Station / Egmore (06:30)"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header (Image 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editingMode == "BOARDING") "Select Boarding Point"
                                   else if (editingMode == "DROPPING") "Select Dropping Point"
                                   else "Change trip details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = {
                            if (editingMode != "MENU") editingMode = "MENU" else onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))

                    when (editingMode) {
                        "BOARDING" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                boardingOptions.forEach { opt ->
                                    val isSel = opt.equals(selectedBoarding, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSel) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isSel) RedbusCrimson else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedBoarding = opt
                                                editingMode = "MENU"
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(opt, fontSize = 12.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) RedbusCrimson else Color(0xFF1E293B))
                                            if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = RedbusCrimson, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(36.dp))
                            }
                        }

                        "DROPPING" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                droppingOptions.forEach { opt ->
                                    val isSel = opt.equals(selectedDropping, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSel) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isSel) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedDropping = opt
                                                editingMode = "MENU"
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(opt, fontSize = 12.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color(0xFF2563EB) else Color(0xFF1E293B))
                                            if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(36.dp))
                            }
                        }

                        else -> {
                            // Main Options matching Image 1
                            // Option 1: Change boarding point
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editingMode = "BOARDING" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Change boarding point", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text(selectedBoarding.ifBlank { "Select boarding point" }, fontSize = 11.5.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                                }
                            }

                            // Option 2: Change dropping point
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editingMode = "DROPPING" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Change dropping point", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text(selectedDropping.ifBlank { "Select dropping point" }, fontSize = 11.5.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                                }
                            }

                            // Confirm Button
                            Button(
                                onClick = {
                                    onConfirm(selectedBoarding, selectedDropping)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Update Trip Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Free Bus Change Monthly Calendar Modal & Date Change Confirmation
 */
@Composable
private fun FreeBusChangeModal(
    booking: BookingDetails,
    onDismiss: () -> Unit,
    onConfirmDateChange: (newDate: String, newRouteId: Long?, newSeatNumbers: List<String>?) -> Unit,
    onOpenBusSearchForDate: (sourceCity: String, destinationCity: String, travelDate: String) -> Unit = { _, _, _ -> }
) {
    val currentTripDate = remember(booking.travelDate) {
        try { LocalDate.parse(booking.travelDate.trim()) } catch (_: Exception) { LocalDate.now() }
    }
    val todayDate = remember { LocalDate.now() }
    val passengerCount = remember(booking.passengers) { (booking.passengers?.size ?: 1).coerceAtLeast(1) }
    val currentSeatsList = remember(booking.passengers) {
        booking.passengers?.mapNotNull { it.seatNumber.trim().ifEmpty { null } }?.ifEmpty { listOf("L1") } ?: listOf("L1")
    }

    var availableDatesMap by remember { mutableStateOf<Map<String, com.redbus.ai.data.model.AvailableDateItem>>(emptyMap()) }
    var isLoadingDates by remember { mutableStateOf(true) }

    LaunchedEffect(booking.pnr) {
        isLoadingDates = true
        try {
            val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.redbus.ai.data.remote.RedbusApiService.create().getAvailableDatesForBooking(booking.pnr)
            }
            availableDatesMap = res.dates.orEmpty().associateBy { it.date }
        } catch (_: Exception) {
        } finally {
            isLoadingDates = false
        }
    }

    var displayedMonth by remember {
        mutableStateOf(java.time.YearMonth.from(currentTripDate))
    }
    var candidateDate by remember { mutableStateOf<LocalDate?>(null) }
    var candidateOption by remember { mutableStateOf<com.redbus.ai.data.model.AvailableDateItem?>(null) }
    var showConfirmPrompt by remember { mutableStateOf(false) }
    var showSeatSelectionPrompt by remember { mutableStateOf(false) }
    var showBusNotAvailablePrompt by remember { mutableStateOf(false) }
    var selectedReplacementSeats by remember { mutableStateOf<List<String>>(emptyList()) }

    val daysInMonth = displayedMonth.lengthOfMonth()
    val firstDayOfWeek = displayedMonth.atDay(1).dayOfWeek.value % 7

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.width(345.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Free bus change", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("${booking.operatorName} • Seat: ${currentSeatsList.joinToString(", ")}", fontSize = 11.sp, color = Color.Gray)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Month Nav
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${displayedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }}, ${displayedMonth.year}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray)
                        }
                        IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Weekday Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { d ->
                        Text(d, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (isLoadingDates) {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RedbusCrimson, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    }
                } else {
                    // Calendar Grid
                    val totalCells = firstDayOfWeek + daysInMonth
                    val totalRows = (totalCells + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (row in 0 until totalRows) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                for (col in 0..6) {
                                    val dayNum = row * 7 + col - firstDayOfWeek + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val thisDate = displayedMonth.atDay(dayNum)
                                        val dateStr = thisDate.toString()
                                        val dateOption = availableDatesMap[dateStr]
                                        val isPast = thisDate.isBefore(todayDate)
                                        val isCurrentTrip = thisDate == currentTripDate
                                        val isBusAvailable = if (availableDatesMap.isEmpty()) !isPast else (dateOption?.busAvailable == true)
                                        val hasSeatConflict = isBusAvailable && dateOption != null && !dateOption.sameSeatsAvailable && !isCurrentTrip
                                        val isSelected = thisDate == candidateDate

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when {
                                                        isSelected -> Color(0xFF2563EB)
                                                        isCurrentTrip -> Color(0xFFFEE2E2)
                                                        !isPast && !isBusAvailable -> Color(0xFFF1F5F9).copy(alpha = 0.5f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable(enabled = !isPast && !isCurrentTrip) {
                                                    candidateDate = thisDate
                                                    candidateOption = dateOption
                                                    if (!isBusAvailable) {
                                                        showBusNotAvailablePrompt = true
                                                    } else if (hasSeatConflict) {
                                                        selectedReplacementSeats = emptyList()
                                                        showSeatSelectionPrompt = true
                                                    } else {
                                                        showConfirmPrompt = true
                                                    }
                                                }
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected || isCurrentTrip || (isBusAvailable && !isPast)) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isPast -> Color(0xFFCBD5E1)
                                                        isCurrentTrip -> RedbusCrimson
                                                        !isBusAvailable -> Color(0xFF94A3B8).copy(alpha = 0.55f)
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                if (!isPast && !isCurrentTrip) {
                                                    Spacer(modifier = Modifier.height(1.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                when {
                                                                    isSelected -> Color.White
                                                                    !isBusAvailable -> Color.Transparent
                                                                    hasSeatConflict -> Color(0xFFF59E0B)
                                                                    else -> Color(0xFF10B981)
                                                                }
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(38.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Same seat free", fontSize = 9.5.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick new seat", fontSize = 9.5.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Faded = No bus", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Current trip date: ${booking.travelDate} (Highlighted in red)",
                    fontSize = 10.5.sp,
                    color = Color.Gray
                )
            }
        }
    }

    // 1. Bus Not Available on Selected Date Prompt
    if (showBusNotAvailablePrompt && candidateDate != null) {
        AlertDialog(
            onDismissRequest = { showBusNotAvailablePrompt = false },
            icon = { Icon(Icons.Default.EventBusy, contentDescription = null, tint = RedbusCrimson) },
            title = {
                Text(
                    text = "Bus Not Available on $candidateDate",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.5.sp
                )
            },
            text = {
                Text(
                    text = "${booking.operatorName} is not scheduled or has no seats available on $candidateDate.\n\nPlease select another highlighted date on the calendar, or browse other buses available on $candidateDate.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetDateStr = candidateDate.toString()
                        showBusNotAvailablePrompt = false
                        onDismiss()
                        onOpenBusSearchForDate(booking.sourceCity, booking.destinationCity, targetDateStr)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Search Buses on $candidateDate", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBusNotAvailablePrompt = false }) {
                    Text("Pick Another Date", color = Color.Gray, fontSize = 12.sp)
                }
            }
        )
    }

    // 2. Seat Conflict -> Select Different Available Seat(s) Prompt
    if (showSeatSelectionPrompt && candidateDate != null) {
        val availSeats = candidateOption?.availableSeatNumbers.orEmpty()
        val conflictList = candidateOption?.conflictingSeats?.takeIf { it.isNotEmpty() } ?: currentSeatsList
        AlertDialog(
            onDismissRequest = { showSeatSelectionPrompt = false },
            icon = { Icon(Icons.Default.EventSeat, contentDescription = null, tint = Color(0xFFD97706)) },
            title = {
                Text(
                    text = "Seat ${conflictList.joinToString(", ")} Already Booked",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.5.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your booked seat (${conflictList.joinToString(", ")}) is already taken on $candidateDate. Please select $passengerCount available seat(s) below (${availSeats.size} available) to update your journey date:",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 210.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val chunked = availSeats.chunked(4)
                            chunked.forEach { rowSeats ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowSeats.forEach { sNum ->
                                        val isPicked = selectedReplacementSeats.contains(sNum)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isPicked) Color(0xFF10B981) else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, if (isPicked) Color(0xFF059669) else Color(0xFF94A3B8)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    selectedReplacementSeats = if (isPicked) {
                                                        selectedReplacementSeats - sNum
                                                    } else if (selectedReplacementSeats.size < passengerCount) {
                                                        selectedReplacementSeats + sNum
                                                    } else {
                                                        selectedReplacementSeats.drop(1) + sNum
                                                    }
                                                }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = sNum,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPicked) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                    repeat(4 - rowSeats.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Selected New Seat(s): ${if (selectedReplacementSeats.isEmpty()) "Tap seat above" else selectedReplacementSeats.joinToString(", ")} (${selectedReplacementSeats.size}/$passengerCount)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedReplacementSeats.size == passengerCount) Color(0xFF059669) else Color(0xFFD97706)
                    )

                    OutlinedButton(
                        onClick = {
                            val targetDateStr = candidateDate.toString()
                            showSeatSelectionPrompt = false
                            onDismiss()
                            onOpenBusSearchForDate(booking.sourceCity, booking.destinationCity, targetDateStr)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Open Full Bus Seat Search Results", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedReplacementSeats.size == passengerCount,
                    onClick = {
                        showSeatSelectionPrompt = false
                        onConfirmDateChange(
                            candidateDate.toString(),
                            candidateOption?.routeId,
                            selectedReplacementSeats
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm Seat & Update Date", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSeatSelectionPrompt = false }) {
                    Text("Cancel", color = Color.Gray, fontSize = 12.sp)
                }
            }
        )
    }

    // 3. Standard Confirmation Modal (When same seat is available on new date)
    if (showConfirmPrompt && candidateDate != null) {
        AlertDialog(
            onDismissRequest = { showConfirmPrompt = false },
            icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = RedbusCrimson) },
            title = {
                Text(
                    text = "Confirm Date Change",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Confirm rescheduling your journey date to ${candidateDate}?\n\n• Route: ${booking.sourceCity} → ${booking.destinationCity}\n• Seat(s): ${currentSeatsList.joinToString(", ")} (Available)\n• An updated E-Ticket & PDF Invoice will be emailed to ${booking.contactEmail}.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmPrompt = false
                        onConfirmDateChange(candidateDate.toString(), candidateOption?.routeId, null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm Date Change", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmPrompt = false }) {
                    Text("Cancel", color = Color.Gray, fontSize = 12.sp)
                }
            }
        )
    }
}

/**
 * Enhanced View Ticket & Journey Details Modal (Matching Image 4, Image 2, Image 3)
 */
@Composable
fun EnhancedTicketJourneyModal(
    booking: BookingDetails,
    reviewedPnrs: Map<String, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onOpenPerforatedTicket: () -> Unit,
    onChangeTripPoints: (pnr: String, bp: String?, dp: String?, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onChangeJourneyDate: (pnr: String, newDate: String?, newRouteId: Long?, newSeatNumbers: List<String>?, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onCancelBooking: () -> Unit,
    onSubmitReview: (Long, Int, String, String?) -> Unit = { _, _, _, _ -> },
    onHelpClick: () -> Unit,
    onOpenBusSearchForDate: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableIntStateOf(0) }
    var showSeatModal by remember { mutableStateOf(false) }
    var showChangeTripPointsSheet by remember { mutableStateOf(false) }
    var showFreeBusChangeModal by remember { mutableStateOf(false) }
    var showRateModal by remember { mutableStateOf(false) }
    var showRefundDetailsModal by remember { mutableStateOf(false) }

    val tripStatus = remember(booking) { calculateTripStatus(booking) }
    val isCompleted = tripStatus == RealtimeTripStatus.TRIP_COMPLETED
    val isCancelled = booking.status.equals("CANCELLED", ignoreCase = true) || booking.status.equals("REFUNDED", ignoreCase = true)
    val refundAmt = booking.refundAmount ?: booking.totalAmount
    val arnStr = remember(booking.pnr, booking.refundArn) { getBookingArnStr(booking) }
    val cancelBannerTime = remember(booking.refundRequestedAt, booking.createdAt) {
        val dt = try {
            val raw = booking.refundRequestedAt?.takeIf { it.isNotBlank() } ?: booking.createdAt
            LocalDateTime.parse(raw.take(19))
        } catch (_: Exception) {
            LocalDateTime.now()
        }
        dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH))
    }
    val completedTimeFormatted = remember(booking.refundApprovedAt, booking.refundRequestedAt, booking.createdAt) {
        formatRefundDateTimeStr(
            booking.refundApprovedAt?.takeIf { it.isNotBlank() } ?: booking.refundRequestedAt?.takeIf { it.isNotBlank() } ?: booking.createdAt,
            addMinutes = 2,
            includeComma = false
        )
    }

    val passengerName = remember(booking.passengers) {
        booking.passengers?.firstOrNull()?.name?.ifBlank { "Passenger" } ?: "Passenger"
    }
    val seatNumber = remember(booking.passengers) {
        booking.passengers?.mapNotNull { it.seatNumber.trim().ifEmpty { null } }?.joinToString(", ") ?: "1F"
    }
    val passengerCount = remember(booking.passengers) {
        booking.passengers?.size?.coerceAtLeast(1) ?: 1
    }

    val comfortScore = remember(booking.busRating) {
        String.format(Locale.US, "%.1f", (booking.busRating ?: 4.4) * 2.0)
    }

    // 2-Hour Deadline Calculation
    val isWithin2Hours = remember(booking.travelDate, booking.departureTime) {
        try {
            val travelDate = LocalDate.parse(booking.travelDate.trim())
            val depTime = parseFlexibleTime(booking.departureTime) ?: LocalTime.of(12, 0)
            val depDateTime = travelDate.atTime(depTime)
            LocalDateTime.now().isAfter(depDateTime.minusHours(2))
        } catch (_: Exception) {
            false
        }
    }

    val ticketNumber = remember(booking.id, booking.pnr) {
        if (booking.id > 0) "TVBT${47600000 + booking.id}" else "TVBT${booking.pnr.takeLast(8).uppercase()}"
    }

    val durationLabel = remember(booking.departureTime, booking.arrivalTime) {
        calculateDurationLabel(booking.departureTime, booking.arrivalTime)
    }
    val depDateFormatted = remember(booking.travelDate) {
        formatTicketDate(booking.travelDate, plusDays = 0)
    }
    val arrDateFormatted = remember(booking.travelDate) {
        formatTicketDate(booking.travelDate, plusDays = 1)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top App Bar matching Image 4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back", modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Ticket details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Ticket #$ticketNumber", fontSize = 11.5.sp, color = Color.Gray)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onHelpClick, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = {
                                val shareText = "🚌 redBus Ticket #$ticketNumber | PNR: ${booking.pnr}\nRoute: ${booking.sourceCity} ➔ ${booking.destinationCity}\nDeparture: ${booking.departureTime}, $depDateFormatted\nPassenger: $passengerName (Seat: $seatNumber)"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Ticket Details"))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray, modifier = Modifier.size(19.dp))
                        }
                    }
                }

                // Red Top Banner when Cancelled (Matching Image 3)
                if (isCancelled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE53935))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Ticket cancelled on $cancelBannerTime",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Sub-Tabs Bar: Ticket details, Boarding point, Dropping point, Rest stop details
                val subTabs = listOf("Ticket details", "Boarding point", "Dropping point", "Rest stop details")
                ScrollableTabRow(
                    selectedTabIndex = activeSubTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = RedbusCrimson,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)) }
                ) {
                    subTabs.forEachIndexed { idx, tabTitle ->
                        Tab(
                            selected = activeSubTab == idx,
                            onClick = { activeSubTab = idx },
                            text = {
                                Text(
                                    text = tabTitle,
                                    fontSize = 12.sp,
                                    fontWeight = if (activeSubTab == idx) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                // Sub-Tab Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (activeSubTab) {
                        1 -> {
                            // Boarding Point Tab
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedbusCrimson, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("BOARDING LOCATION", fontWeight = FontWeight.Black, fontSize = 13.sp, color = RedbusCrimson)
                                        }
                                        Text(booking.boardingPoint.ifBlank { "${booking.sourceCity} Main Bus Station" }, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text("Reporting Time: 15 minutes before departure (${booking.departureTime})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Helpline contact: ${booking.contactPhone}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Dropping Point Tab
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("DROPPING LOCATION", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF0284C7))
                                        }
                                        Text(booking.droppingPoint.ifBlank { "${booking.destinationCity} Main Terminal" }, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text("Estimated Arrival: ${booking.arrivalTime}, $arrDateFormatted", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Rest Stop Details Tab
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("MIDWAY REST STOP & HYGIENE", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFFD97706))
                                        }
                                        Text("Shri Balaji Highway Express Plaza", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text("Halt Duration: 20-30 minutes • Clean sanitized washrooms, hot vegetarian meals, coffee & refreshments.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEF3C7)
                                        ) {
                                            Text("⭐ Hygiene Rating 4.6/5 Verified by redBus", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // Main "Ticket details" Tab Content
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (isCancelled) {
                                    // ===============================================================
                                    // IMAGE 3 UI: REFUND SUMMARY CARD + CANCELLED ROUTE CARD
                                    // ===============================================================
                                    Card(
                                        shape = RoundedCornerShape(22.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Wallet with cash notes graphic
                                            Box(
                                                modifier = Modifier.size(56.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF16A34A),
                                                    modifier = Modifier
                                                        .size(width = 38.dp, height = 24.dp)
                                                        .offset(y = (-8).dp)
                                                        .rotate(-12f)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text("₹", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF6D4C41),
                                                    border = BorderStroke(1.dp, Color(0xFF5D4037)),
                                                    modifier = Modifier
                                                        .size(width = 44.dp, height = 30.dp)
                                                        .offset(y = 4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(end = 6.dp),
                                                        contentAlignment = Alignment.CenterEnd
                                                    ) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = Color(0xFFFBBF24),
                                                            modifier = Modifier.size(8.dp)
                                                        ) {}
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "₹${String.format(Locale.US, "%.0f", refundAmt)} refunded in 2 min",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF111827)
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF16A34A),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Lightning fast refund",
                                                    color = Color(0xFF16A34A),
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Canvas(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .height(1.dp)
                                            ) {
                                                drawLine(
                                                    color = Color(0xFFD1D5DB),
                                                    start = Offset(0f, 0f),
                                                    end = Offset(size.width, 0f),
                                                    strokeWidth = 3f,
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                                )
                                            }

                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row {
                                                    Text("To: ", fontSize = 12.5.sp, color = Color(0xFF4B5563))
                                                    Text(
                                                        text = if (booking.refundDestination.equals("ORIGINAL_PAYMENT", ignoreCase = true))
                                                            "Original payment method"
                                                        else
                                                            "Original payment method (redBus Wallet)",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF111827)
                                                    )
                                                }
                                                Row {
                                                    Text("Completed on: ", fontSize = 12.5.sp, color = Color(0xFF4B5563))
                                                    Text(
                                                        text = completedTimeFormatted,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF111827)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row {
                                                    Text("Your ARN is ", fontSize = 12.5.sp, color = Color(0xFF4B5563))
                                                    Text(
                                                        text = arnStr,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF111827)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Button(
                                                onClick = { showRefundDetailsModal = true },
                                                shape = RoundedCornerShape(50),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFFDE2E4),
                                                    contentColor = Color(0xFF111827)
                                                ),
                                                contentPadding = PaddingValues(vertical = 13.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Refund Details",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.5.sp
                                                )
                                            }
                                        }
                                    }

                                    // Cancelled Route + Passenger Card (Bottom of Image 3)
                                    Card(
                                        shape = RoundedCornerShape(22.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            // Grey Upper Section
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFE5E7EB))
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Left Vertical 2-Dot Timeline
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(0xFF6B7280),
                                                        modifier = Modifier.size(8.dp)
                                                    ) {}
                                                    Box(
                                                        modifier = Modifier
                                                            .width(1.5.dp)
                                                            .height(74.dp)
                                                            .background(Color(0xFF9CA3AF))
                                                    )
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(0xFF6B7280),
                                                        modifier = Modifier.size(8.dp)
                                                    ) {}
                                                }

                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    // Source Row
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(booking.sourceCity, fontWeight = FontWeight.Black, fontSize = 14.5.sp, color = Color(0xFF111827))
                                                            Text(
                                                                booking.boardingPoint.ifBlank { "${booking.sourceCity} Pickup Point" },
                                                                fontSize = 11.5.sp,
                                                                color = Color(0xFF4B5563),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text(booking.departureTime, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF111827))
                                                            Text(depDateFormatted, fontSize = 11.sp, color = Color(0xFF4B5563))
                                                        }
                                                    }

                                                    // Middle Divider with Booking Cancelled
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFD1D5DB))
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.DirectionsBus,
                                                                contentDescription = null,
                                                                tint = Color(0xFF374151),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                text = "Booking Cancelled",
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF374151)
                                                            )
                                                        }
                                                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFD1D5DB))
                                                    }

                                                    // Destination Row
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Bottom
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(booking.destinationCity, fontWeight = FontWeight.Black, fontSize = 14.5.sp, color = Color(0xFF111827))
                                                            Text(
                                                                booking.droppingPoint.ifBlank { "${booking.destinationCity} Dropoff Point" },
                                                                fontSize = 11.5.sp,
                                                                color = Color(0xFF4B5563),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text(booking.arrivalTime, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF111827))
                                                            Text(arrDateFormatted, fontSize = 11.sp, color = Color(0xFF4B5563))
                                                        }
                                                    }
                                                }
                                            }

                                            // White Lower Section
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White)
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(booking.operatorName, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF111827))
                                                Text("${booking.busType} · $passengerCount Seat", fontSize = 12.sp, color = Color(0xFF6B7280))
                                                Text(passengerName, fontSize = 12.sp, color = Color(0xFF4B5563))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFE0E7FF)
                                                ) {
                                                    Text(
                                                        text = seatNumber,
                                                        color = Color(0xFF1D4ED8),
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }

                                                HorizontalDivider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(top = 4.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { onOpenPerforatedTicket() }
                                                        .padding(vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Ticket details",
                                                        color = Color(0xFF1D4ED8),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                if (isCompleted) {
                                    // Rate this trip section matching Image 3
                                    val existingRating = booking.userRating ?: reviewedPnrs[booking.pnr]
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = if (existingRating != null) "Your Rating" else "Rate this trip",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                (1..5).forEach { starIdx ->
                                                    val isFilled = existingRating != null && starIdx <= existingRating
                                                    Icon(
                                                        imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarOutline,
                                                        contentDescription = "Rate $starIdx Stars",
                                                        tint = RatingAmber,
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clickable {
                                                                showRateModal = true
                                                            }
                                                    )
                                                }
                                            }
                                            if (existingRating != null) {
                                                Text(
                                                    text = "You rated this trip $existingRating / 5 ⭐",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF047857)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Excellent Comfort Score Banner (Matching Image 4)
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "$passengerName, great bus choice",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Excellent comfort score $comfortScore/10",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                // Journey Card (Matching Image 4 / Image 3)
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        // Card Top Header Banner (Red for Active, Dark Slate for Completed)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isCompleted) Color(0xFF1E293B)
                                                    else RedbusCrimson
                                                )
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // 1. Source Row (Departure)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(booking.sourceCity, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        booking.boardingPoint.ifBlank { "${booking.sourceCity} Pickup Point" },
                                                        color = Color.White.copy(alpha = 0.85f),
                                                        fontSize = 11.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(booking.departureTime, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                                    Text(depDateFormatted, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                                                }
                                            }

                                            // 2. Middle Bus Duration Connector
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color.Transparent,
                                                    border = BorderStroke(1.5.dp, Color.White),
                                                    modifier = Modifier.size(8.dp)
                                                ) {}
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(1.dp)
                                                        .background(Color.White.copy(alpha = 0.35f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = Color.Black.copy(alpha = 0.25f)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                            Text(
                                                                text = if (isCompleted) "Trip Completed" else durationLabel,
                                                                color = Color.White,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(6.dp)) {}
                                            }

                                            // 3. Destination Row (Arrival)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.SouthEast, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(booking.destinationCity, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        booking.droppingPoint.ifBlank { "${booking.destinationCity} Dropoff Point" },
                                                        color = Color.White.copy(alpha = 0.85f),
                                                        fontSize = 11.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(booking.arrivalTime, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                                    Text(arrDateFormatted, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        // Card Bottom White Section
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(booking.operatorName, fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Text("${booking.busType} · $passengerCount Seat", fontSize = 11.5.sp, color = Color.Gray)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(passengerName, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFFCE7F3)
                                                    ) {
                                                        Text(
                                                            text = seatNumber,
                                                            color = Color(0xFFDB2777),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                // View Seat Button with graphic
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    modifier = Modifier.clickable { showSeatModal = true }
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text("::::", fontSize = 12.sp, color = Color(0xFF94A3B8), letterSpacing = 2.sp)
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text("View seat", color = Color(0xFF2563EB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))

                                            // Underlined "Ticket details" link matching Image 4
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onOpenPerforatedTicket() }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Ticket details",
                                                    color = Color(0xFF2563EB),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                                )
                                            }
                                        }
                                    }
                                }

                                // Tracking details info text (Image 4)
                                Text(
                                    text = "Bus number and tracking details will be shared on the day of journey on ${booking.contactPhone}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Action Card 1: Change trip details (Image 4, Image 1 & Image 5)
                                val canChangeTrip = !isWithin2Hours && !isCancelled && !isCompleted
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    color = if (!canChangeTrip) Color(0xFFF8FAFC) else Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = canChangeTrip) {
                                            showChangeTripPointsSheet = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (!canChangeTrip) Color.Gray else Color(0xFF1E293B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Change trip details",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                                color = if (!canChangeTrip) Color.Gray else Color(0xFF111827)
                                            )
                                            Text(
                                                text = when {
                                                    isCompleted -> "Trip completed"
                                                    isWithin2Hours -> "Locked within 2 hours of departure"
                                                    else -> "Boarding and dropping point"
                                                },
                                                fontSize = 11.sp,
                                                color = when {
                                                    isCompleted -> Color.Gray
                                                    isWithin2Hours -> RedbusCrimson
                                                    else -> Color(0xFF6B7280)
                                                }
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                                    }
                                }

                                // Action Card 2: Free bus change (Image 4, Image 2 & Image 5)
                                val canBusChange = !isCancelled && !isCompleted
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    color = if (!canBusChange) Color(0xFFF8FAFC) else Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = canBusChange) {
                                            showFreeBusChangeModal = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = if (!canBusChange) Color.Gray else Color(0xFF1E293B),
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Free bus change",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                                color = if (!canBusChange) Color.Gray else Color(0xFF111827)
                                            )
                                            Text(
                                                text = if (isCompleted) "Trip completed" else "Postpone journey date to another date",
                                                fontSize = 11.sp,
                                                color = if (!canBusChange) Color.Gray else Color(0xFF6B7280)
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                                    }
                                }

                                // Action Card 3: Cancel Booking (Image 4 & Image 5)
                                if (!isCancelled) {
                                    val canCancelBooking = !isCompleted
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        color = if (!canCancelBooking) Color(0xFFF8FAFC) else Color.White,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = canCancelBooking) {
                                                onCancelBooking()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Cancel,
                                                contentDescription = null,
                                                tint = if (!canCancelBooking) Color.Gray else Color(0xFF1E293B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Cancel Booking",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                                    color = if (!canCancelBooking) Color.Gray else Color(0xFF111827)
                                                )
                                                Text(
                                                    text = if (isCompleted) "Trip completed" else "Eligible for partial refund",
                                                    fontSize = 11.sp,
                                                    color = if (!canCancelBooking) Color.Gray else Color(0xFF6B7280)
                                                )
                                            }
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
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
    }

    // Modal: Refund Details Tracker (Matching Image 4)
    if (showRefundDetailsModal) {
        RefundDetailsTrackerModal(
            booking = booking,
            ticketNumber = ticketNumber,
            onDismiss = { showRefundDetailsModal = false },
            onHelpClick = onHelpClick
        )
    }

    // Modal: Booked Seat Layout Map
    if (showSeatModal) {
        BookedSeatLayoutModal(
            booking = booking,
            onDismiss = { showSeatModal = false }
        )
    }

    // Modal: Change Trip Points (Image 1)
    if (showChangeTripPointsSheet) {
        ChangeTripPointsBottomSheet(
            booking = booking,
            onDismiss = { showChangeTripPointsSheet = false },
            onConfirm = { newBp, newDp ->
                onChangeTripPoints(booking.pnr, newBp, newDp, {
                    showChangeTripPointsSheet = false
                    Toast.makeText(context, "Trip points updated successfully!", Toast.LENGTH_SHORT).show()
                }, { errMsg ->
                    Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                })
            }
        )
    }

    // Modal: Free Bus Change (Image 2)
    if (showFreeBusChangeModal) {
        FreeBusChangeModal(
            booking = booking,
            onDismiss = { showFreeBusChangeModal = false },
            onConfirmDateChange = { newDate, newRouteId, newSeatNumbers ->
                onChangeJourneyDate(booking.pnr, newDate, newRouteId, newSeatNumbers, {
                    showFreeBusChangeModal = false
                    Toast.makeText(context, "Journey rescheduled to $newDate! Updated ticket emailed.", Toast.LENGTH_LONG).show()
                }, { errMsg ->
                    val cleanErr = errMsg.substringAfterLast(":")
                    Toast.makeText(context, cleanErr, Toast.LENGTH_LONG).show()
                })
            },
            onOpenBusSearchForDate = onOpenBusSearchForDate
        )
    }

    // Modal: Rate Journey Experience
    if (showRateModal) {
        RateJourneyModal(
            booking = booking,
            onDismiss = { showRateModal = false },
            onSubmitReview = { busId, rating, comment, pnr ->
                onSubmitReview(busId, rating, comment, pnr)
                showRateModal = false
                Toast.makeText(context, "Review submitted! Thank you.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Full-screen Refund Details Tracker Modal matching Image 4 (`media_1794035986171_1aabacfe.jpg`)
 */
@Composable
fun RefundDetailsTrackerModal(
    booking: BookingDetails,
    ticketNumber: String,
    onDismiss: () -> Unit,
    onHelpClick: () -> Unit = {}
) {
    val refundAmt = booking.refundAmount ?: booking.totalAmount
    val totalPaid = booking.totalAmount.takeIf { it > 0.0 } ?: refundAmt
    val totalDeductions = maxOf(0.0, totalPaid - refundAmt)
    val arnStr = remember(booking.pnr, booking.refundArn) { getBookingArnStr(booking) }

    val reqTimeStr = remember(booking.refundRequestedAt, booking.createdAt) {
        formatRefundDateTimeStr(
            booking.refundRequestedAt?.takeIf { it.isNotBlank() } ?: booking.createdAt,
            addMinutes = 0,
            includeComma = true
        )
    }
    val initTimeStr = remember(booking.refundRequestedAt, booking.createdAt) {
        formatRefundDateTimeStr(
            booking.refundRequestedAt?.takeIf { it.isNotBlank() } ?: booking.createdAt,
            addMinutes = 1,
            includeComma = true
        )
    }
    val compTimeStr = remember(booking.refundApprovedAt, booking.refundRequestedAt, booking.createdAt) {
        formatRefundDateTimeStr(
            booking.refundApprovedAt?.takeIf { it.isNotBlank() } ?: booking.refundRequestedAt?.takeIf { it.isNotBlank() } ?: booking.createdAt,
            addMinutes = 2,
            includeComma = true
        )
    }

    var expandTotalPaid by remember { mutableStateOf(false) }
    var expandTotalDeductions by remember { mutableStateOf(true) }
    var showPolicyTable by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color.White,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF111827), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Refund details", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF111827))
                            Text("Ticket #$ticketNumber", fontSize = 11.5.sp, color = Color(0xFF6B7280))
                        }
                    }

                    IconButton(onClick = onHelpClick, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.HeadsetMic, contentDescription = "Support", tint = Color(0xFF374151), modifier = Modifier.size(20.dp))
                    }
                }

                // Solid Green Credited Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF007A27))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Refund of ₹${String.format(Locale.US, "%.0f", refundAmt)} has been credited",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Refund status section
                    Column {
                        Text(
                            text = "Refund status",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        val steps = listOf(
                            Triple("Completed", "Refund Request Received", reqTimeStr),
                            Triple("Completed", "Refund Initiated", initTimeStr),
                            Triple("Completed", "Refund Completed", compTimeStr)
                        )

                        steps.forEachIndexed { index, (statusLabel, title, timestamp) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF007A27),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    if (index < steps.lastIndex) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(48.dp)
                                                .background(Color(0xFF007A27))
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.padding(bottom = if (index < steps.lastIndex) 12.dp else 0.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF007A27)
                                    )
                                    Text(
                                        text = title,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF111827)
                                    )
                                    Text(
                                        text = timestamp,
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                    }

                    // Light-blue ARN Info Box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEBF3FF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "The refund amount should reflect in your original payment method by now. If there is an issue, please contact your bank's customer care. Your ARN is: $arnStr",
                                fontSize = 12.5.sp,
                                color = Color(0xFF1F2937),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Centered View cancellation policy link
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPolicyTable = !showPolicyTable }
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "View cancellation policy",
                            color = Color(0xFF1D4ED8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                    }

                    if (showPolicyTable) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF9FAFB),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Operator Cancellation Policy:", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF111827))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("More than 24 hrs before departure", fontSize = 11.5.sp, color = Color(0xFF4B5563))
                                    Text("10% deduction", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("12 to 24 hrs before departure", fontSize = 11.5.sp, color = Color(0xFF4B5563))
                                    Text("25% deduction", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Free Cancellation / Operator Cancelled", fontSize = 11.5.sp, color = Color(0xFF4B5563))
                                    Text("₹0 deduction", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007A27))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Bottom Refund Breakdown Card (Dark Header + Expandable Rows)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Dark Header (#333333)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF333333))
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Refund amount", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text("₹${String.format(Locale.US, "%.0f", refundAmt)}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }

                            // Row 1: Total paid
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandTotalPaid = !expandTotalPaid }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total paid", fontWeight = FontWeight.Black, fontSize = 13.5.sp, color = Color(0xFF111827))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("₹${String.format(Locale.US, "%.0f", totalPaid)}", fontWeight = FontWeight.Black, fontSize = 13.5.sp, color = Color(0xFF111827))
                                        Icon(
                                            imageVector = if (expandTotalPaid) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color(0xFF374151),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (expandTotalPaid) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val baseFare = maxOf(0.0, totalPaid - (booking.serviceFee ?: 0.0) - (booking.freeCancellationFee ?: 0.0) - (booking.tripGuaranteeFee ?: 0.0))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Base Ticket Fare & GST", fontSize = 12.sp, color = Color(0xFF4B5563))
                                        Text("₹${String.format(Locale.US, "%.1f", baseFare)}", fontSize = 12.sp, color = Color(0xFF4B5563))
                                    }
                                    if ((booking.serviceFee ?: 0.0) > 0.0) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Operator Service Fee", fontSize = 12.sp, color = Color(0xFF4B5563))
                                            Text("₹${String.format(Locale.US, "%.1f", booking.serviceFee ?: 0.0)}", fontSize = 12.sp, color = Color(0xFF4B5563))
                                        }
                                    }
                                    if ((booking.freeCancellationFee ?: 0.0) > 0.0) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Free Cancellation Add-on", fontSize = 12.sp, color = Color(0xFF4B5563))
                                            Text("₹${String.format(Locale.US, "%.1f", booking.freeCancellationFee ?: 0.0)}", fontSize = 12.sp, color = Color(0xFF4B5563))
                                        }
                                    }
                                    if ((booking.tripGuaranteeFee ?: 0.0) > 0.0) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Trip Guarantee Protection", fontSize = 12.sp, color = Color(0xFF4B5563))
                                            Text("₹${String.format(Locale.US, "%.1f", booking.tripGuaranteeFee ?: 0.0)}", fontSize = 12.sp, color = Color(0xFF4B5563))
                                        }
                                    }
                                    if ((booking.tripGuaranteeCashback ?: 0.0) > 0.0) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Trip Guarantee Cashback Bonus", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007A27))
                                            Text("+₹${String.format(Locale.US, "%.1f", booking.tripGuaranteeCashback ?: 0.0)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007A27))
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF3F4F6))

                            // Row 2: Total deductions
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandTotalDeductions = !expandTotalDeductions }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total deductions", fontWeight = FontWeight.Black, fontSize = 13.5.sp, color = Color(0xFF111827))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("₹${String.format(Locale.US, "%.1f", totalDeductions)}", fontWeight = FontWeight.Black, fontSize = 13.5.sp, color = Color(0xFF111827))
                                        Icon(
                                            imageVector = if (expandTotalDeductions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color(0xFF374151),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (expandTotalDeductions) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Cancellation charges (incl GST)", fontSize = 12.sp, color = Color(0xFF4B5563))
                                        Text("₹${String.format(Locale.US, "%.1f", totalDeductions)}", fontSize = 12.sp, color = Color(0xFF4B5563))
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
 * Perforated View Ticket Popup Modal matching Image 5 (`media_1793296527384_86dfb532.jpg`).
 */
@Composable
private fun ViewTicketDetailsModal(
    booking: BookingDetails,
    onDismiss: () -> Unit,
    onCancelBooking: () -> Unit
) {
    val context = LocalContext.current
    val ticketNumber = remember(booking.id, booking.pnr) {
        if (booking.id > 0) "TVBS${12801000 + booking.id}" else "TVBS${booking.pnr.takeLast(8).uppercase()}"
    }

    val departureDateFormatted = remember(booking.travelDate) {
        formatTicketDate(booking.travelDate, plusDays = 0)
    }
    val arrivalDateFormatted = remember(booking.travelDate) {
        formatTicketDate(booking.travelDate, plusDays = 1)
    }
    val durationLabel = remember(booking.departureTime, booking.arrivalTime) {
        calculateDurationLabel(booking.departureTime, booking.arrivalTime)
    }
    val passengers = remember(booking.passengers) {
        booking.passengers.orEmpty().ifEmpty {
            listOf(
                com.redbus.ai.data.model.PassengerInput(
                    seatId = 1,
                    seatNumber = "L1",
                    name = "Passenger",
                    age = 25,
                    gender = "MALE"
                )
            )
        }
    }

    val ticketScale = remember { Animatable(0.94f) }
    LaunchedEffect(Unit) {
        ticketScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        val backdropColor = Color(0xFF141416)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropColor.copy(alpha = 0.94f))
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Circular Close Button (✕) matching Image 5
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = Color(0xFF2C2C2E),
                    modifier = Modifier
                        .size(38.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Ticket",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Scrollable Middle Ticket Card Container (Properly bounded so bottom Share button is ALWAYS completely visible)
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Main Perforated Ticket Card (Matching Image 5) with smooth entry animation
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = ticketScale.value
                                scaleY = ticketScale.value
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 1. Top Crimson Header (Source -> Duration -> Destination)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(RedbusCrimson)
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Vertical Timeline Column (↗ ─── ↘)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(end = 10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.NorthEast,
                                                    contentDescription = null,
                                                    tint = RedbusCrimson,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.5.dp)
                                                .height(58.dp)
                                                .background(Color.White.copy(alpha = 0.7f))
                                        )

                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.SouthEast,
                                                    contentDescription = null,
                                                    tint = RedbusCrimson,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Right Content Column (Departure, Duration Divider, Arrival)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Departure Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = booking.sourceCity,
                                                    color = Color.White,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = booking.boardingPoint.ifBlank { "${booking.sourceCity} Bypass" },
                                                    color = Color.White.copy(alpha = 0.88f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = booking.departureTime,
                                                    color = Color.White,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = departureDateFormatted,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        // Center Duration Line: ──── 🚌 7h 10 min ────
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(1.dp)
                                                    .background(Color.White.copy(alpha = 0.35f))
                                            )
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsBus,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = durationLabel,
                                                    color = Color.White.copy(alpha = 0.95f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(1.dp)
                                                    .background(Color.White.copy(alpha = 0.35f))
                                            )
                                        }

                                        // Arrival Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = booking.destinationCity,
                                                    color = Color.White,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = booking.droppingPoint.ifBlank { booking.destinationCity },
                                                    color = Color.White.copy(alpha = 0.88f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = booking.arrivalTime,
                                                    color = Color.White,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = arrivalDateFormatted,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. White Operator & Bus Type Section
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = booking.operatorName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${booking.busType} · ${passengers.size} ${if (passengers.size == 1) "Seat" else "Seats"}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }

                            DashedTicketDivider()

                            // 3. Passenger List Section (Name + (F)/(M), Age, Seat Pill Badge)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                passengers.forEach { p ->
                                    val genderShort = if (p.gender.startsWith("F", ignoreCase = true)) "F" else "M"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = p.name,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "($genderShort)",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = "(${p.age} yrs)",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                        }

                                        // Blue-tinted Seat Number Pill Badge (Matching Image 5 "1F")
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEEF2FF)
                                        ) {
                                            Text(
                                                text = p.seatNumber,
                                                color = Color(0xFF1E3A8A),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Semicircular Side Cutout Notches + Dashed Perforation Line (Image 5)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                DashedTicketDivider()

                                // Left semicircular cutout notch
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = (-12).dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(backdropColor)
                                )

                                // Right semicircular cutout notch
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 12.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(backdropColor)
                                )
                            }

                            // 5. Ticket #, PNR #, and Fare Footer (Matching Image 5)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Ticket # Row with Copy Icon
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ticket #",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.clickable {
                                            val clipboard =
                                                context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clipboard?.setPrimaryClip(
                                                ClipData.newPlainText(
                                                    "Ticket Number",
                                                    ticketNumber
                                                )
                                            )
                                            Toast.makeText(context, "Ticket #$ticketNumber copied", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Ticket Number",
                                            tint = Color(0xFF111827),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = ticketNumber,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // PNR # Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PNR #",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = booking.pnr,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Fare Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Fare",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.US, "%.2f", booking.totalAmount)}",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Full-Width Red Pill Button: "Share your ticket" (Always Pinned & Fully Visible)
                Button(
                    onClick = {
                        val shareText = buildString {
                            appendLine("🚌 redBus AI Confirmed E-Ticket")
                            appendLine("Ticket #: $ticketNumber")
                            appendLine("PNR #: ${booking.pnr}")
                            appendLine("Route: ${booking.sourceCity} ➔ ${booking.destinationCity}")
                            appendLine("Operator: ${booking.operatorName} (${booking.busType})")
                            appendLine("Departure: ${booking.departureTime}, $departureDateFormatted (${booking.boardingPoint})")
                            appendLine("Arrival: ${booking.arrivalTime}, $arrivalDateFormatted (${booking.droppingPoint})")
                            appendLine("Passengers: ${passengers.joinToString { "${it.name} - Seat ${it.seatNumber}" }}")
                            appendLine("Total Fare: ₹${String.format(Locale.US, "%.2f", booking.totalAmount)}")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "redBus E-Ticket PNR ${booking.pnr}")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share your ticket"))
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedbusCrimson,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = AppStrings.get("share_ticket"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RateJourneyModal(
    booking: BookingDetails,
    onDismiss: () -> Unit,
    onSubmitReview: (Long, Int, String, String?) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var cleanlinessRating by remember { mutableIntStateOf(5) }
    var punctualityRating by remember { mutableIntStateOf(5) }
    var staffRating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }

    val hygieneTags = listOf(
        "Sanitized Bus",
        "Clean Blankets",
        "Deep Cleaned Seats",
        "AC Filter Clean",
        "Punctual Departure",
        "Polite Crew",
        "Safe Driving",
        "Clean Rest Stop"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .systemBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with Close Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rate Your Journey Experience",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${booking.sourceCity} ➔ ${booking.destinationCity} • ${booking.operatorName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    // 1. Overall Star Rating
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when (rating) {
                                5 -> "⭐ Excellent Journey"
                                4 -> "⭐ Very Good"
                                3 -> "⭐ Good"
                                2 -> "⭐ Fair"
                                else -> "⭐ Needs Improvement"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = RatingAmber
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (star in 1..5) {
                                Icon(
                                    imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star stars",
                                    tint = if (star <= rating) RatingAmber else Color.Gray.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { rating = star }
                                )
                            }
                        }
                    }

                    // 2. Hygiene & Service Breakdown
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Hygiene & Service Quality", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            // Bus Cleanliness & Hygiene
                            RatingAspectRow(
                                title = "Bus Hygiene & Cleanliness",
                                rating = cleanlinessRating,
                                onRatingChange = { cleanlinessRating = it }
                            )

                            // Punctuality
                            RatingAspectRow(
                                title = "Punctuality & Timeliness",
                                rating = punctualityRating,
                                onRatingChange = { punctualityRating = it }
                            )

                            // Staff & Driver Behavior
                            RatingAspectRow(
                                title = "Staff & Driver Behavior",
                                rating = staffRating,
                                onRatingChange = { staffRating = it }
                            )
                        }
                    }

                    // 3. Experience & Hygiene Tags
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("What went well?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            hygieneTags.forEach { tag ->
                                val isSelected = selectedTags.contains(tag)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                                    },
                                    label = { Text(tag, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RedbusCrimson.copy(alpha = 0.15f),
                                        selectedLabelColor = RedbusCrimson
                                    )
                                )
                            }
                        }
                    }

                    // 4. Feedback comment
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Write your review (hygiene, comfort, route)") },
                        placeholder = { Text("Share feedback about hygiene, blankets, driving quality...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 5. Submit Button
                    Button(
                        onClick = {
                            val targetBusId = booking.busId ?: if (booking.routeId > 0) booking.routeId.toLong() else 1L
                            val fullComment = buildString {
                                if (comment.isNotBlank()) append(comment.trim())
                                if (selectedTags.isNotEmpty()) {
                                    if (isNotEmpty()) append(" | ")
                                    append("Tags: ")
                                    append(selectedTags.joinToString(", "))
                                }
                                append(" (Hygiene: $cleanlinessRating/5, Punctuality: $punctualityRating/5, Staff: $staffRating/5)")
                            }
                            onSubmitReview(targetBusId, rating, fullComment, booking.pnr)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Submit Review", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingAspectRow(
    title: String,
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 1..5) {
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (i <= rating) RatingAmber else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onRatingChange(i) }
                )
            }
        }
    }
}

@Composable
private fun DashedTicketDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
    ) {
        drawLine(
            color = Color(0xFFD1D5DB),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        )
    }
}

private fun formatTicketDate(dateStr: String, plusDays: Long = 0): String {
    return try {
        val date = LocalDate.parse(dateStr).plusDays(plusDays)
        val weekday = date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
        val month = date.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
        "$weekday, ${date.dayOfMonth} $month"
    } catch (_: Exception) {
        dateStr
    }
}

private fun calculateDurationLabel(dep: String, arr: String): String {
    return try {
        fun toMins(t: String): Int {
            val clean = t.trim().uppercase()
            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")
            val parts = clean.replace(Regex("[^0-9:]"), "").split(":")
            var h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            if (isPm && h < 12) h += 12
            if (isAm && h == 12) h = 0
            return h * 60 + m
        }

        var diff = toMins(arr) - toMins(dep)
        if (diff <= 0) diff += 24 * 60
        val hrs = diff / 60
        val mins = diff % 60
        if (mins > 0) "${hrs}h ${mins} min" else "${hrs}h 00 min"
    } catch (_: Exception) {
        "7h 10 min"
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
    onLogout: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    onUpdateAvatar: (String) -> Unit = {},
    onChangePassword: (String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    onSendResetOtp: ((Boolean, String) -> Unit) -> Unit = {},
    onTopUpWallet: (Double) -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onFetchDeviceSessions: () -> Unit = {},
    onRevokeDeviceSession: (Long) -> Unit = {},
    onRevokeAllOtherSessions: () -> Unit = {},
    onDeactivateAccount: () -> Unit = {},
    onDeleteAccount: (String) -> Unit = {},
    onNavigateToBookings: () -> Unit = {},
    onSelectLanguage: (String) -> Unit = {},
    onToggleBookingForWomen: (Boolean) -> Unit = {},
    onNavigateToOperatorLogin: () -> Unit = {},
    onMarkNotificationAsRead: (String) -> Unit = {},
    onMarkAllNotificationsAsRead: (Collection<String>) -> Unit = {},
    onDismissNotification: (String) -> Unit = {},
    onClearAllNotifications: (Collection<String>) -> Unit = {}
) {
    if (!uiState.isAuthenticated || uiState.user == null) {
        LiveAuthScreen(
            uiState = uiState,
            onLoginWithEmail = onLoginWithEmail,
            onRegisterAccount = onRegisterAccount,
            onRequestMobileOtp = onRequestMobileOtp,
            onVerifyMobileOtp = onVerifyMobileOtp,
            onToggleDarkTheme = onToggleDarkTheme,
            onNavigateToOperatorLogin = onNavigateToOperatorLogin
        )
        return
    }

    LaunchedEffect(Unit) {
        onFetchDeviceSessions()
    }

    val user = uiState.user

    // Modals & Sheets Visibility States
    var showLiveCouponsModal by remember { mutableStateOf(false) }
    var showPersonalInformationModal by remember { mutableStateOf(false) }
    var showPassengersModal by remember { mutableStateOf(false) }
    var showHelpFaqModal by remember { mutableStateOf(false) }
    var showAccountSettingsModal by remember { mutableStateOf(false) }
    var showLanguageSelectionDialog by remember { mutableStateOf(false) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var showLogoutConfirmModal by remember { mutableStateOf(false) }
    var showAvatarModal by remember { mutableStateOf(false) }
    var showDeactivateModal by remember { mutableStateOf(false) }
    var showDeleteModal by remember { mutableStateOf(false) }
    var showBookingForWomenDialog by remember { mutableStateOf(false) }
    var showAddTravellerDialog by remember { mutableStateOf(false) }
    var editingTravellerId by remember { mutableStateOf<Int?>(null) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var deletePassword by remember { mutableStateOf("") }

    // Edit Profile fields
    var editName by remember(user.name) { mutableStateOf(user.name) }
    var editPhone by remember(user.phone) { mutableStateOf(user.phone.orEmpty()) }
    var editGender by remember(user.gender) { mutableStateOf(user.gender ?: "MALE") }

    // Saved Traveller form fields
    var newTravellerName by remember { mutableStateOf("") }
    var newTravellerAge by remember { mutableStateOf("25") }
    var newTravellerGender by remember { mutableStateOf("MALE") }
    var showEditTravellerDialog by remember { mutableStateOf(false) }
    var editingTravellerName by remember { mutableStateOf("") }
    var editingTravellerAge by remember { mutableStateOf("25") }
    var editingTravellerGender by remember { mutableStateOf("MALE") }

    // Security & Password fields (matching website settings/page.tsx)
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showCurrentPass by remember { mutableStateOf(false) }
    var showNewPass by remember { mutableStateOf(false) }
    var showConfirmPass by remember { mutableStateOf(false) }
    var passwordChangeMessage by remember { mutableStateOf<String?>(null) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var otpMessage by remember { mutableStateOf<String?>(null) }

    val initials = remember(user.name) {
        user.name.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifBlank { "U" }
    }

    val supportedLanguages = listOf(
        "English" to "English",
        "தமிழ் (Tamil)" to "தமிழ் (Tamil)",
        "हिंदी (Hindi)" to "हिंदी (Hindi)",
        "ಕನ್ನಡ (Kannada)" to "ಕನ್ನಡ (Kannada)",
        "తెలుగు (Telugu)" to "తెలుగు (Telugu)",
        "മലയാളം (Malayalam)" to "മലയാളം (Malayalam)"
    )

    val activeLanguageKey = uiState.selectedLanguage.ifBlank { "English" }
    var pendingLanguage by remember(activeLanguageKey) { mutableStateOf(activeLanguageKey) }
    var pendingDialogLanguage by remember(activeLanguageKey, showLanguageSelectionDialog) { mutableStateOf(activeLanguageKey) }

    fun getLanguageConfirmText(langKey: String): String {
        val lower = langKey.lowercase()
        return when {
            lower.contains("tamil") || lower.contains("தமிழ்") -> "தமிழுக்கு மாறுங்கள்"
            lower.contains("hindi") || lower.contains("हिंदी") -> "हिंदी में बदलें"
            lower.contains("kannada") || lower.contains("ಕನ್ನಡ") -> "ಕನ್ನಡಕ್ಕೆ ಬದಲಾಯಿಸಿ"
            lower.contains("telugu") || lower.contains("తెలుగు") -> "తెలుగులోకి మార్చండి"
            lower.contains("malayalam") || lower.contains("മലയാളം") -> "മലയാളത്തിലേക്ക് മാറുക"
            else -> "Switch to English"
        }
    }

    var isRefreshingAccount by remember { mutableStateOf(false) }
    val refreshAccountScope = rememberCoroutineScope()

    if (isRefreshingAccount) {
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
                    text = "Refreshing Account...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Notification banner for profile actions
        uiState.profileActionMessage?.let { msg ->
            item {
                Surface(
                    color = RatingEmeraldBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ $msg",
                        color = RatingEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // 1. Live User Account Header (Avatar, Email, Phone, Role)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = RedbusCrimson),
                modifier = Modifier.fillMaxWidth()
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clickable { showAvatarModal = true }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxSize()
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
                                                fontSize = 22.sp
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change avatar",
                                        tint = Color.White,
                                        modifier = Modifier.padding(3.dp)
                                    )
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

                        IconButton(onClick = {
                            refreshAccountScope.launch {
                                isRefreshingAccount = true
                                onRefreshAccount()
                                delay(500)
                                isRefreshingAccount = false
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Account",
                                tint = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
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
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (user.emailVerified == true) "✓ Verified" else "⚠ Unverified",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        TextButton(
                            onClick = { showPersonalInformationModal = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // If Operator is certified as Primo Bus Operator by Admin, display Primo On Time Guarantee badge in Profile
                    if (uiState.isOperator && uiState.operatorProfile?.primoCertified == true) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PrimoOnTimeBadge()
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = "Primo Bus Operator • 0% Comm.",
                                        color = Color(0xFF92400E),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. "Try redBus in your language" Strip (Matching Image 2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = AppStrings.get("try_redbus_in_your_language", uiState.selectedLanguage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(supportedLanguages) { (key, label) ->
                        val isSelected = pendingLanguage.equals(key, ignoreCase = true)
                        Surface(
                            onClick = { pendingLanguage = key },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) RedbusCrimson else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) RedbusCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            ),
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                if (!pendingLanguage.equals(activeLanguageKey, ignoreCase = true)) {
                    Surface(
                        onClick = { onSelectLanguage(pendingLanguage) },
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFDE2E4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = getLanguageConfirmText(pendingLanguage),
                                color = Color(0xFF111827),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Section: "My details" (Matching Image 2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = AppStrings.get("my_details", uiState.selectedLanguage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                val isNonPassengerRole = uiState.isAdmin || uiState.isOperator ||
                    user.role.equals("ROLE_ADMIN", ignoreCase = true) ||
                    user.role.equals("ROLE_OPERATOR", ignoreCase = true)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // 1. Bookings Button (Only for Passengers — Admin and Operator cannot book tickets)
                        if (!isNonPassengerRole) {
                            AccountMenuRowItem(
                                icon = Icons.Default.ReceiptLong,
                                title = "Bookings",
                                onClick = onNavigateToBookings
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }

                        // 2. Personal information Button
                        AccountMenuRowItem(
                            icon = Icons.Default.Person,
                            title = "Personal information",
                            onClick = { showPersonalInformationModal = true }
                        )

                        // 3. Passengers Button (Only for Passengers — Admin and Operator cannot book tickets)
                        if (!isNonPassengerRole) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            AccountMenuRowItem(
                                icon = Icons.Default.Group,
                                title = "Passengers",
                                trailingBadge = if (uiState.savedTravellers.isNotEmpty()) "${uiState.savedTravellers.size}" else null,
                                onClick = { showPassengersModal = true }
                            )
                        }
                    }
                }
            }
        }

        // 4. Section: "Payments" (Matching Image 3)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = AppStrings.get("payments", uiState.selectedLanguage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AccountMenuRowItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = RatingEmerald,
                        title = "redBus Wallet",
                        trailingBadge = "₹${String.format(Locale.US, "%.2f", user.walletBalance ?: 0.0)}",
                        onClick = onOpenWallet
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    AccountMenuRowItem(
                        icon = Icons.Default.ConfirmationNumber,
                        iconTint = RedbusCrimson,
                        title = "Promotions & Live Coupons",
                        trailingBadge = "${uiState.availableCoupons.size.coerceAtLeast(4)} Live",
                        onClick = { showLiveCouponsModal = true }
                    )
                }
            }
        }

        // 5. Section: "More" / Help & Settings (Matching Image 3 & 5)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = AppStrings.get("more", uiState.selectedLanguage),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Help (FAQs from www.redbusai.app)
                        AccountMenuRowItem(
                            icon = Icons.Default.HelpOutline,
                            title = "Help",
                            onClick = { showHelpFaqModal = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Account settings (from https://www.redbusai.app/settings)
                        AccountMenuRowItem(
                            icon = Icons.Default.Settings,
                            title = "Account settings",
                            onClick = { showAccountSettingsModal = true }
                        )
                    }
                }
            }
        }

        // 6. Section: "Preferences" (Matching Image 5)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Preferences",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Language
                        AccountMenuRowItem(
                            icon = Icons.Default.Translate,
                            title = "Language",
                            trailingBadge = uiState.selectedLanguage.ifBlank { "English" },
                            onClick = { showLanguageSelectionDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Appearance (Light / Dark theme switch)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleDarkTheme() }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = RedbusCrimson.copy(alpha = 0.1f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (uiState.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                            contentDescription = null,
                                            tint = RedbusCrimson,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Appearance",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Choose light or dark theme",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            RedbusStyledSwitch(
                                checked = uiState.isDarkTheme,
                                onCheckedChange = { onToggleDarkTheme() }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Notifications
                        AccountMenuRowItem(
                            icon = Icons.Default.Notifications,
                            title = "Notifications",
                            onClick = { showNotificationsModal = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Booking for women (Image 4)
                        AccountMenuRowItem(
                            icon = Icons.Default.Face,
                            iconTint = FemaleSeatPink,
                            title = "Booking for women",
                            trailingBadge = if (uiState.isBookingForWomen) "Yes" else "No",
                            onClick = { showBookingForWomenDialog = true }
                        )
                    }
                }
            }
        }

        // 7. Sign Out & Version footer (Matching Image 4 & 5)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { showLogoutConfirmModal = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of redBus AI", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }

                Text(
                    text = BuildConfig.APP_VERSION_NAME,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // 0. Modal: Promotions & Live Coupons (Synced with redbusai.app/operator)
    if (showLiveCouponsModal) {
        PassengerLiveCouponsModal(
            coupons = uiState.availableCoupons.ifEmpty { uiState.operatorCoupons },
            onDismiss = { showLiveCouponsModal = false },
            onNavigateToOperatorHub = {
                showLiveCouponsModal = false
                onNavigateToOperatorLogin()
            }
        )
    }

    // 1. Modal: Personal Information
    if (showPersonalInformationModal) {
        Dialog(
            onDismissRequest = { showPersonalInformationModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Personal Information",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Manage your verified profile details",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showPersonalInformationModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider()

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RedbusCrimson) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = RedbusCrimson) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = user.email,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Email Address (Verified)") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = RatingEmerald) },
                            trailingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RatingEmerald) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Gender",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("MALE", "FEMALE", "OTHER").forEach { g ->
                                    FilterChip(
                                        selected = editGender.equals(g, ignoreCase = true),
                                        onClick = { editGender = g },
                                        label = { Text(g) }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onUpdateProfile(editName, editPhone, editGender)
                                showPersonalInformationModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Profile", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    // 2. Modal: Passengers (Saved Travellers with Edit & Delete)
    if (showPassengersModal) {
        Dialog(
            onDismissRequest = { showPassengersModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Saved Passengers (${uiState.savedTravellers.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Saved travellers for 1-tap booking",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showPassengersModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        if (uiState.savedTravellers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Text(
                                        text = "No saved passengers yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Add travellers below for faster checkout",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.savedTravellers, key = { it.id }) { traveller ->
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = RedbusCrimson.copy(alpha = 0.12f),
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = RedbusCrimson,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = traveller.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    )
                                                    Text(
                                                        text = "${traveller.age} years • ${traveller.gender}",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        editingTravellerId = traveller.id
                                                        editingTravellerName = traveller.name
                                                        editingTravellerAge = traveller.age.toString()
                                                        editingTravellerGender = traveller.gender
                                                        showEditTravellerDialog = true
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = RedbusBlue,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onDeleteTraveller(traveller.id) }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showAddTravellerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(" Add Passenger", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    // 3. Modal: Help & FAQs (From www.redbusai.app/faq)
    if (showHelpFaqModal) {
        val faqs = listOf(
            "Cancellation & Instant Refund Policy" to
                "You can cancel your bus ticket anytime before departure directly from the Bookings tab. Refund slabs:\n• More than 24 hours before departure: 100% refund.\n• 12 to 24 hours before departure: 50% refund.\n• Less than 12 hours before departure: No refund.\nRefunds credited to redBus Wallet are Instant. Bank or card refunds take 3-5 business days.",
            "Luggage & Baggage Guidelines" to
                "Each passenger is entitled to carry up to 15 kg of personal luggage (e.g. 1 standard trolley bag and 1 laptop/handbag) free of charge in the bus luggage boot. Excess luggage or commercial cargo may incur additional fees charged by the operator at boarding.",
            "Boarding & Photo ID Rules" to
                "Passengers must present the digital mTicket (SMS, WhatsApp, or in-app ticket) along with a valid Government-issued photo ID (Aadhaar Card, Driving License, Voter ID, or Passport) to the bus conductor/driver prior to boarding.",
            "Live Bus GPS Tracking" to
                "Live GPS bus tracking activates 60 minutes before scheduled departure time. Tap the 'Track Bus' button on your ticket to see the real-time location, bus speed, driver contact details, and distance to your boarding point.",
            "Infant & Child Travel Policy" to
                "Children below 5 years of age travel free of charge with their parent or guardian without occupying an individual seat. Children aged 5 and above require a separate confirmed ticket reservation.",
            "Operator Fleet & Verified Partners" to
                "All bus operators on redBus AI are strictly verified under Primo standards, ensuring 24/7 emergency support, onboard safety amenities, punctuality tracking, and sanitized interiors."
        )

        var expandedIndex by remember { mutableStateOf<Int?>(0) }

        Dialog(
            onDismissRequest = { showHelpFaqModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = RedbusCrimson.copy(alpha = 0.12f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.HelpOutline,
                                            contentDescription = null,
                                            tint = RedbusCrimson,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Help & FAQs",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Official redBus AI policies & support",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { showHelpFaqModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(faqs.indices.toList()) { idx ->
                                val (question, answer) = faqs[idx]
                                val isExpanded = expandedIndex == idx
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedIndex = if (isExpanded) null else idx }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = question,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isExpanded) RedbusCrimson else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = answer,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HeadsetMic,
                                    contentDescription = null,
                                    tint = RatingEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "24x7 Customer Support",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "support@redbusai.app • 1800-419-4287",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Modal: Account Settings (Matching https://www.redbusai.app/settings & Image 4)
    if (showAccountSettingsModal) {
        Dialog(
            onDismissRequest = { showAccountSettingsModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = RedbusCrimson.copy(alpha = 0.12f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = RedbusCrimson,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Account Settings",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "redbusai.app/settings",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { showAccountSettingsModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // A. Profile Avatar Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Profile Avatar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = RedbusCrimson,
                                            modifier = Modifier.size(54.dp)
                                        ) {
                                            if (!user.avatarUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = user.avatarUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                                }
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = { showAvatarModal = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Change Avatar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = "Select from presets or reset to initials",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // B. Personal Information Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text("Full Name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = editPhone,
                                        onValueChange = { editPhone = it },
                                        label = { Text("Phone Number") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("MALE", "FEMALE", "OTHER").forEach { g ->
                                            FilterChip(
                                                selected = editGender.equals(g, ignoreCase = true),
                                                onClick = { editGender = g },
                                                label = { Text(g) }
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onUpdateProfile(editName, editPhone, editGender)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save Personal Info", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // C. Security & Password Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Security & Password", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                    passwordChangeMessage?.let { msg ->
                                        Text(msg, fontSize = 12.sp, color = if (msg.contains("success", ignoreCase = true)) RatingEmerald else RedbusCrimson)
                                    }

                                    OutlinedTextField(
                                        value = currentPass,
                                        onValueChange = { currentPass = it },
                                        label = { Text("Current Password") },
                                        singleLine = true,
                                        visualTransformation = if (showCurrentPass) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showCurrentPass = !showCurrentPass }) {
                                                Icon(if (showCurrentPass) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = newPass,
                                        onValueChange = { newPass = it },
                                        label = { Text("New Password (min 6 characters)") },
                                        singleLine = true,
                                        visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showNewPass = !showNewPass }) {
                                                Icon(if (showNewPass) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = confirmPass,
                                        onValueChange = { confirmPass = it },
                                        label = { Text("Confirm New Password") },
                                        singleLine = true,
                                        visualTransformation = if (showConfirmPass) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showConfirmPass = !showConfirmPass }) {
                                                Icon(if (showConfirmPass) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            if (newPass.length < 6) {
                                                passwordChangeMessage = "New password must be at least 6 characters"
                                                return@Button
                                            }
                                            if (newPass != confirmPass) {
                                                passwordChangeMessage = "Passwords do not match"
                                                return@Button
                                            }
                                            onChangePassword(currentPass, newPass) { success, msg ->
                                                passwordChangeMessage = msg
                                                if (success) {
                                                    currentPass = ""
                                                    newPass = ""
                                                    confirmPass = ""
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Update Password", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // D. Logged-In Devices & Security Sessions (Matching Image 4)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Logged-In Devices & Security Sessions",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Active Sessions (${if (uiState.deviceSessions.isNotEmpty()) uiState.deviceSessions.size else 4})",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val sessionItems = if (uiState.deviceSessions.isNotEmpty()) {
                                        uiState.deviceSessions.map { s ->
                                            Triple(s.id, s.deviceName, s.isCurrent ?: false)
                                        }
                                    } else {
                                        listOf(
                                            Triple(1L, "Standard Web Browser on Desktop/Mobile OS", true),
                                            Triple(2L, "Google Chrome on Android", false),
                                            Triple(3L, "Apple Safari on iOS", false),
                                            Triple(4L, "Google Chrome on Windows 10/11", false)
                                        )
                                    }

                                    sessionItems.forEach { (id, name, isCurrent) ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = if (name.contains("Android", ignoreCase = true) || name.contains("iOS", ignoreCase = true)) Icons.Default.PhoneAndroid else Icons.Default.Computer,
                                                        contentDescription = null,
                                                        tint = if (isCurrent) RatingEmerald else RedbusCrimson,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = name,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 13.sp
                                                        )
                                                        Text(
                                                            text = if (isCurrent) "Current Active Session" else "Authorized Device",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                if (isCurrent) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = RatingEmerald.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "Active Now",
                                                            color = RatingEmerald,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = { onRevokeDeviceSession(id) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("Log Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onRevokeAllOtherSessions() },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedbusCrimson),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Log Out All Other Devices", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }

                            // E. Danger Zone (Matching Image 4)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = RedbusCrimson.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, RedbusCrimson.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = RedbusCrimson)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Danger Zone", fontWeight = FontWeight.Black, fontSize = 15.sp, color = RedbusCrimson)
                                    }
                                    Text(
                                        text = "Deactivate account temporarily or permanently remove your profile data.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                showAccountSettingsModal = false
                                                showDeactivateModal = true
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Deactivate Account", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                showAccountSettingsModal = false
                                                showDeleteModal = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Delete Permanently", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    // 5. Modal: Notifications (Matching website navbar NotificationDropdown.tsx)
    if (showNotificationsModal) {
        NotificationsDropdownModal(
            uiState = uiState,
            onDismiss = { showNotificationsModal = false },
            onNavigateToBookings = {
                showNotificationsModal = false
                onNavigateToBookings()
            },
            onNavigateToWallet = {
                showNotificationsModal = false
                onOpenWallet()
            },
            onOpenTicket = {
                showNotificationsModal = false
                onNavigateToBookings()
            },
            onMarkAsRead = onMarkNotificationAsRead,
            onMarkAllAsRead = onMarkAllNotificationsAsRead,
            onDismissNotification = onDismissNotification,
            onClearAllNotifications = onClearAllNotifications
        )
    }

    // 6. Dialog: Select Language (Image 5 & 2)
    if (showLanguageSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageSelectionDialog = false },
            title = {
                Text(
                    text = "Select Language",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    supportedLanguages.forEach { (key, label) ->
                        val isSelected = pendingDialogLanguage.equals(key, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) RedbusCrimson.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) RedbusCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pendingDialogLanguage = key
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) RedbusCrimson else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = RedbusCrimson, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSelectLanguage(pendingDialogLanguage)
                        showLanguageSelectionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (!pendingDialogLanguage.equals(activeLanguageKey, ignoreCase = true)) {
                            getLanguageConfirmText(pendingDialogLanguage)
                        } else {
                            "Confirm"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageSelectionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Modal: Avatar Selector (Upload Image, Default Icon, Male 3D, Female 3D, and Remove Photo)
    if (showAvatarModal) {
        val context = LocalContext.current
        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bmp = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bmp != null) {
                        val maxDim = 320
                        val ratio = minOf(maxDim.toFloat() / bmp.width, maxDim.toFloat() / bmp.height, 1f)
                        val scaled = Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
                        val baos = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                        onUpdateAvatar("data:image/jpeg;base64,$b64")
                        showAvatarModal = false
                    }
                } catch (_: Exception) {}
            }
        }

        AlertDialog(
            onDismissRequest = { showAvatarModal = false },
            title = {
                Text("Choose Profile Photo", fontWeight = FontWeight.Black, fontSize = 18.sp)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pick a 3D avatar, upload your own photo, or use the default icon",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. Upload Custom Image Card
                    Surface(
                        onClick = { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(14.dp),
                        color = RedbusCrimson.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, RedbusCrimson.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RedbusCrimson,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Upload Profile Image", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Select an image from device gallery", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // 2. Avatar Selection Options (Default Icon, Male 3D, Female 3D)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Option A: Default Icon
                        val isDefault = user.avatarUrl.isNullOrBlank()
                        Surface(
                            onClick = {
                                onUpdateAvatar("REMOVE")
                                showAvatarModal = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDefault) RedbusCrimson.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(if (isDefault) 2.dp else 1.dp, if (isDefault) RedbusCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = RedbusCrimson,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    }
                                }
                                Text("Default Icon", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                Text("Initials", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Option B: Male 3D
                        val male3dUrl = "https://api.dicebear.com/7.x/adventurer/svg?seed=Alex&backgroundColor=b6e3f4"
                        val isMale3d = user.avatarUrl == male3dUrl
                        Surface(
                            onClick = {
                                onUpdateAvatar(male3dUrl)
                                showAvatarModal = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isMale3d) RedbusCrimson.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(if (isMale3d) 2.dp else 1.dp, if (isMale3d) RedbusCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    AsyncImage(
                                        model = male3dUrl,
                                        contentDescription = "Male 3D",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Text("Male 3D", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                Text("3D Avatar", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Option C: Female 3D
                        val female3dUrl = "https://api.dicebear.com/7.x/adventurer/svg?seed=Priya&backgroundColor=ffd5dc"
                        val isFemale3d = user.avatarUrl == female3dUrl
                        Surface(
                            onClick = {
                                onUpdateAvatar(female3dUrl)
                                showAvatarModal = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isFemale3d) RedbusCrimson.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(if (isFemale3d) 2.dp else 1.dp, if (isFemale3d) RedbusCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    AsyncImage(
                                        model = female3dUrl,
                                        contentDescription = "Female 3D",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Text("Female 3D", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                Text("3D Avatar", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // 3. Remove Profile Image Button
                    if (!user.avatarUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                onUpdateAvatar("REMOVE")
                                showAvatarModal = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remove Profile Image", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAvatarModal = false }) { Text("Close") }
            }
        )
    }

    // Modal: Deactivate Account
    if (showDeactivateModal) {
        AlertDialog(
            onDismissRequest = { showDeactivateModal = false },
            title = { Text("Deactivate Account?", fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "Your account will be suspended. You can reactivate anytime by logging in again.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeactivateModal = false
                        onDeactivateAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Confirm Deactivate", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateModal = false }) { Text("Cancel") }
            }
        )
    }

    // Modal: Delete Account Permanently
    if (showDeleteModal) {
        AlertDialog(
            onDismissRequest = { showDeleteModal = false },
            title = { Text("Delete Account Permanently", fontWeight = FontWeight.Black, color = RedbusCrimson) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This action is permanent and cannot be undone. All your bookings and wallet credit will be erased.",
                        fontSize = 13.sp
                    )
                    Text("Type DELETE to confirm:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        singleLine = true,
                        placeholder = { Text("DELETE") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteConfirmText.trim().equals("DELETE", ignoreCase = false)) {
                            showDeleteModal = false
                            onDeleteAccount(deletePassword)
                        }
                    },
                    enabled = deleteConfirmText.trim() == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Permanently Delete", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteModal = false }) { Text("Cancel") }
            }
        )
    }

    if (showLogoutConfirmModal) {
        val modalScale = remember { Animatable(0.85f) }
        val modalRotX = remember { Animatable(-14f) }
        LaunchedEffect(Unit) {
            modalScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
        LaunchedEffect(Unit) {
            modalRotX.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }

        Dialog(
            onDismissRequest = { showLogoutConfirmModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = modalScale.value
                            scaleY = modalScale.value
                            rotationX = modalRotX.value
                            cameraDistance = 14f * density
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RedbusSoftTint,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    tint = RedbusCrimson,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Text(
                            text = "Sign Out Confirmation",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = "Are you sure you want to sign out of ${user.email}? Your wallet balance, saved travellers, and bookings will stay synced when you log back in.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showLogoutConfirmModal = false },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 13.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Button(
                                onClick = {
                                    showLogoutConfirmModal = false
                                    onLogout()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RedbusCrimson,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(vertical = 13.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Yes, Sign Out",
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Edit Saved Passenger
    if (showEditTravellerDialog) {
        AlertDialog(
            onDismissRequest = { showEditTravellerDialog = false },
            title = { Text("Edit Passenger Details", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editingTravellerName,
                        onValueChange = { editingTravellerName = it },
                        label = { Text("Passenger Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editingTravellerAge,
                        onValueChange = { editingTravellerAge = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Age") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Gender", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MALE", "FEMALE", "OTHER").forEach { g ->
                                FilterChip(
                                    selected = editingTravellerGender.equals(g, ignoreCase = true),
                                    onClick = { editingTravellerGender = g },
                                    label = { Text(g) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ageInt = editingTravellerAge.toIntOrNull() ?: 25
                        if (editingTravellerName.isNotBlank() && editingTravellerId != null) {
                            onDeleteTraveller(editingTravellerId!!)
                            onAddTraveller(editingTravellerName, ageInt, editingTravellerGender)
                            showEditTravellerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTravellerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Add Saved Passenger
    if (showAddTravellerDialog) {
        AlertDialog(
            onDismissRequest = { showAddTravellerDialog = false },
            title = { Text("Add Passenger", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTravellerName,
                        onValueChange = { newTravellerName = it },
                        label = { Text("Passenger Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTravellerAge,
                        onValueChange = { newTravellerAge = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Age") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Gender", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MALE", "FEMALE", "OTHER").forEach { g ->
                                FilterChip(
                                    selected = newTravellerGender.equals(g, ignoreCase = true),
                                    onClick = { newTravellerGender = g },
                                    label = { Text(g) }
                                )
                            }
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
                    Text("Save Passenger", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTravellerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Booking for Women Preference Dialog (Image 4)
    if (showBookingForWomenDialog) {
        AlertDialog(
            onDismissRequest = { showBookingForWomenDialog = false },
            title = {
                Text(
                    text = "Booking for women",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Book for women? When enabled, redBus AI ensures booking is reserved exclusively for female passengers.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.isBookingForWomen) FemaleSeatPink.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (uiState.isBookingForWomen) FemaleSeatPink else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleBookingForWomen(true)
                                showBookingForWomenDialog = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = FemaleSeatPink,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Yes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Only women can book tickets", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (uiState.isBookingForWomen) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = FemaleSeatPink)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!uiState.isBookingForWomen) RedbusCrimson.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (!uiState.isBookingForWomen) RedbusCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleBookingForWomen(false)
                                showBookingForWomenDialog = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("No", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Standard all-passenger booking", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!uiState.isBookingForWomen) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = RedbusCrimson)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBookingForWomenDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Reusable Account Menu Row Item matching redBus Account Tab Design (Image 2, 3, 5).
 */
@Composable
fun AccountMenuRowItem(
    icon: ImageVector,
    title: String,
    iconTint: Color = RedbusCrimson,
    trailingBadge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.1f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!trailingBadge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconTint.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = trailingBadge,
                        color = iconTint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Live Authentication Screen with Visibility Eye Toggle Buttons on Login Password,
 * Mobile OTP, and Sign Up Password inputs (Matching Image 3 requirement).
 */
@Composable
private fun LiveAuthScreen(
    uiState: RedbusUiState,
    onLoginWithEmail: (String, String) -> Unit,
    onRegisterAccount: (String, String, String, String) -> Unit,
    onRequestMobileOtp: (String) -> Unit,
    onVerifyMobileOtp: (String, String) -> Unit,
    onToggleDarkTheme: () -> Unit,
    onNavigateToOperatorLogin: () -> Unit = {}
) {
    var authTab by remember { mutableIntStateOf(0) } // 0 = Email Login, 1 = Mobile OTP, 2 = Sign Up
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    // Eye Toggle Visibility States for Password & OTP Inputs
    var isLoginPasswordVisible by remember { mutableStateOf(false) }
    var isOtpVisible by remember { mutableStateOf(false) }
    var isRegisterPasswordVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
                                text = "Sign in with your redBus AI account",
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
                                modifier = Modifier.padding(12.dp)
                            )
                        }
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
                                modifier = Modifier.padding(12.dp)
                            )
                        }
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
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                visualTransformation = if (isLoginPasswordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isLoginPasswordVisible = !isLoginPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isLoginPasswordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = if (isLoginPasswordVisible) {
                                                "Hide password"
                                            } else {
                                                "Show password"
                                            },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { onLoginWithEmail(email, password) },
                                enabled = !uiState.isAuthLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
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
                            if (uiState.otpSent) {
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { otpCode = it },
                                    label = { Text("6-Digit OTP") },
                                    visualTransformation = if (isOtpVisible) {
                                        VisualTransformation.None
                                    } else {
                                        PasswordVisualTransformation()
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isOtpVisible = !isOtpVisible }) {
                                            Icon(
                                                imageVector = if (isOtpVisible) {
                                                    Icons.Default.VisibilityOff
                                                } else {
                                                    Icons.Default.Visibility
                                                },
                                                contentDescription = if (isOtpVisible) {
                                                    "Hide OTP"
                                                } else {
                                                    "Show OTP"
                                                },
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { onVerifyMobileOtp(phone, otpCode) },
                                    enabled = !uiState.isAuthLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = RatingEmerald),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Text("VERIFY OTP & LOGIN", fontWeight = FontWeight.Black)
                                }
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { onRequestMobileOtp(phone) },
                                    enabled = !uiState.isAuthLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
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
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Mobile Number (+91...)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Create Password (min 6 chars)") },
                                visualTransformation = if (isRegisterPasswordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isRegisterPasswordVisible = !isRegisterPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isRegisterPasswordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = if (isRegisterPasswordVisible) {
                                                "Hide password"
                                            } else {
                                                "Show password"
                                            },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { onRegisterAccount(name, email, phone, password) },
                                enabled = !uiState.isAuthLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = if (uiState.isAuthLoading) "CREATING ACCOUNT..." else "CREATE REDBUS AI ACCOUNT",
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToOperatorLogin() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚌 Bus Operator or Fleet Owner? ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Access Partner Portal →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedbusCrimson
                        )
                    }
                }
            }
        }
    }
}

/**
 * Passenger View Modal: Promotions & Live Coupons
 * Fetches real-time synced coupons from https://redbus-api.duckdns.org/api/v1/coupons/available (redbusai.app/operator)
 */
@Composable
private fun PassengerLiveCouponsModal(
    coupons: List<Coupon>,
    onDismiss: () -> Unit,
    onNavigateToOperatorHub: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var copiedCode by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = RedbusCrimson.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ConfirmationNumber,
                                        contentDescription = null,
                                        tint = RedbusCrimson,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Promotions & Live Coupons",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Real-time discounts from redbusai.app",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Real-Time Sync Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFD1FAE5),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("%", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF047857))
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Instant Passenger Booking Discounts",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF064E3B)
                                    )
                                    Text(
                                        text = "Apply on checkout for immediate fare reduction",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF065F46)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFA7F3D0).copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = "REAL-TIME SYNC",
                                    color = Color(0xFF064E3B),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    copiedCode?.let { code ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ Coupon code $code copied! Paste it during checkout.",
                                color = Color(0xFF166534),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Coupons Scroll List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(coupons) { coupon ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFFEF2F2),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                                                ) {
                                                    Text(
                                                        text = coupon.code,
                                                        color = RedbusCrimson,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 13.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFDCFCE7)
                                                ) {
                                                    Text(
                                                        text = "LIVE",
                                                        color = Color(0xFF166534),
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = coupon.displayTitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = coupon.displayDescription,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${coupon.discountPercentage.toInt()}%",
                                                color = Color(0xFF059669),
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text("OFF", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Stats Row
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("MAX CAP", color = Color.Gray, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                                Text("₹${coupon.maxDiscountAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("MIN BOOKING", color = Color.Gray, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                                Text("₹${coupon.minBookingAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("EXPIRES", color = Color.Gray, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                                Text(coupon.displayExpiry, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    // Action: Copy Code Button
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                            clipboard?.setPrimaryClip(
                                                android.content.ClipData.newPlainText("Coupon Code", coupon.code)
                                            )
                                            copiedCode = coupon.code
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = if (copiedCode == coupon.code) "✓ COPIED TO CLIPBOARD" else "COPY CODE: ${coupon.code}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Operator Callout at Bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToOperatorHub() }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fleet Operator? ",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Manage & create coupons in Operator Hub →",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedbusCrimson
                        )
                    }
                }
            }
        }
    }
}
