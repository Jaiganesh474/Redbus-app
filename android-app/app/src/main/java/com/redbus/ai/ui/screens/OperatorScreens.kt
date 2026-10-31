package com.redbus.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.redbus.ai.data.model.*
import com.redbus.ai.ui.theme.*
import com.redbus.ai.ui.viewmodel.RedbusUiState
import java.time.LocalDate

// Dark Theme Colors for Partner Portal Screens (Matching Image 3 & 4)
private val PartnerBg = Color(0xFF0F172A)
private val PartnerCardBg = Color(0xFF1E293B)
private val PartnerCardBorder = Color(0xFF334155)
private val PartnerAccentRed = Color(0xFFD84E55)
private val PartnerGold = Color(0xFFF59E0B)
private val PartnerCyan = Color(0xFF06B6D4)

/**
 * Operator Partner Portal Login Screen (Matching Image 3)
 */
@Composable
fun OperatorLoginScreen(
    uiState: RedbusUiState,
    onLoginWithEmail: (String, String) -> Unit,
    onRequestMobileOtp: (String) -> Unit,
    onVerifyMobileOtp: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToPassengerLogin: () -> Unit
) {
    var authMode by remember { mutableStateOf("email") } // "otp" or "email"
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var phone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PartnerBg),
        contentPadding = PaddingValues(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))

            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToPassengerLogin) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Back to Passenger App",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Partner Portal Header Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PartnerAccentRed.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PartnerAccentRed.copy(alpha = 0.4f)),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = PartnerAccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "redBus Partner",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "FLEET & ROUTE CONSOLE",
                            color = PartnerAccentRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Text(
                text = "Sign in to Partner Portal",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Manage your fleet, published schedules, live seat occupancy & revenue",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login Container Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PartnerCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, PartnerCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Auth Mode Switcher (Mobile OTP | Email & Password)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PartnerBg)
                            .padding(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (authMode == "otp") PartnerAccentRed else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { authMode = "otp" }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mobile OTP",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (authMode == "email") PartnerAccentRed else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { authMode = "email" }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Email & Password",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (authMode == "email") {
                        // Email & Password Inputs (Matching Image 3)
                        Column {
                            Text(
                                text = "OPERATOR BUSINESS EMAIL",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = { Text("operator@travels.com", color = Color.Gray) },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.LightGray)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = PartnerAccentRed,
                                    unfocusedBorderColor = PartnerCardBorder,
                                    focusedContainerColor = PartnerBg,
                                    unfocusedContainerColor = PartnerBg
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column {
                            Text(
                                text = "PASSWORD",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("••••••••••••", color = Color.Gray) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.LightGray)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password",
                                            tint = Color.LightGray
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = PartnerAccentRed,
                                    unfocusedBorderColor = PartnerCardBorder,
                                    focusedContainerColor = PartnerBg,
                                    unfocusedContainerColor = PartnerBg
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            onClick = { onLoginWithEmail(email, password) },
                            enabled = !uiState.isAuthLoading && email.isNotBlank() && password.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PartnerAccentRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (uiState.isAuthLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = "Access Dashboard →",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Mobile OTP Form
                        Column {
                            Text(
                                text = "OPERATOR MOBILE NUMBER",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PartnerBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PartnerCardBorder),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                        Text("IN +91", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    placeholder = { Text("98765 43210", color = Color.Gray) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = PartnerAccentRed,
                                        unfocusedBorderColor = PartnerCardBorder,
                                        focusedContainerColor = PartnerBg,
                                        unfocusedContainerColor = PartnerBg
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (!isOtpSent) {
                            Button(
                                onClick = {
                                    onRequestMobileOtp(phone)
                                    isOtpSent = true
                                },
                                enabled = !uiState.isAuthLoading && phone.length >= 10,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PartnerAccentRed, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Get 6-Digit OTP", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Column {
                                Text(
                                    text = "ENTER 6-DIGIT OTP",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { if (it.length <= 6) otpCode = it },
                                    placeholder = { Text("123456", color = Color.Gray) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = PartnerAccentRed,
                                        unfocusedBorderColor = PartnerCardBorder,
                                        focusedContainerColor = PartnerBg,
                                        unfocusedContainerColor = PartnerBg
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Button(
                                onClick = { onVerifyMobileOtp(phone, otpCode) },
                                enabled = !uiState.isAuthLoading && otpCode.length == 6,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PartnerAccentRed, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Verify & Access Dashboard →", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Error & Status Messages
                    uiState.authError?.let { err ->
                        Text(
                            text = err,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    uiState.authSuccessMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Link: Register New Fleet (Matching Image 3)
            Row(
                modifier = Modifier.clickable { onNavigateToRegister() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New bus partner or transport company? ",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Text(
                    text = "Sign up your fleet →",
                    color = PartnerAccentRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * Operator Fleet Registration Screen (Matching Image 4)
 */
@Composable
fun OperatorRegisterScreen(
    uiState: RedbusUiState,
    onRegisterOperator: (String, String, String, String, String, () -> Unit, (String) -> Unit) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var companyName by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var operatingCity by remember { mutableStateOf("") }
    var gstPermit by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PartnerBg),
        contentPadding = PaddingValues(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))

            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToLogin) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Back to Partner Login",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Header Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PartnerAccentRed.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PartnerAccentRed.copy(alpha = 0.4f)),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = PartnerAccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "redBus Partner",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "OPERATOR FLEET REGISTRATION",
                            color = PartnerAccentRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Text(
                text = "Register Your Fleet with redBus",
                fontSize = 23.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "List your buses, manage schedules, upload high-res bus photos & track bookings directly",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Registration Card with All 7 Fields (Matching Image 4)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PartnerCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, PartnerCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Field 1: Bus Operator / Travels Name
                    OperatorFormField(
                        label = "BUS OPERATOR / TRAVELS NAME *",
                        value = companyName,
                        onValueChange = { companyName = it },
                        placeholder = "e.g. Royal Star Travels / SRS Express",
                        leadingIcon = Icons.Default.Business
                    )

                    // Field 2: Contact Person Name
                    OperatorFormField(
                        label = "CONTACT PERSON NAME *",
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        placeholder = "e.g. Rajesh Kumar",
                        leadingIcon = Icons.Default.Person
                    )

                    // Field 3: Phone / Mobile Number
                    OperatorFormField(
                        label = "PHONE / MOBILE NUMBER *",
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = "+91 98765 43210",
                        leadingIcon = Icons.Default.Phone,
                        keyboardType = KeyboardType.Phone
                    )

                    // Field 4: Business Email
                    OperatorFormField(
                        label = "BUSINESS EMAIL *",
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "partner@travels.com",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )

                    // Field 5: Create Password
                    Column {
                        Text(
                            text = "CREATE PASSWORD *",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("At least 6 characters", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.LightGray) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color.LightGray
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PartnerAccentRed,
                                unfocusedBorderColor = PartnerCardBorder,
                                focusedContainerColor = PartnerBg,
                                unfocusedContainerColor = PartnerBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Field 6: Operating City / Hub
                    OperatorFormField(
                        label = "OPERATING CITY / HUB",
                        value = operatingCity,
                        onValueChange = { operatingCity = it },
                        placeholder = "e.g. Bangalore / Chennai",
                        leadingIcon = Icons.Default.LocationOn
                    )

                    // Field 7: GST / Fleet Permit No. (Optional)
                    OperatorFormField(
                        label = "GST / FLEET PERMIT NO. (OPTIONAL)",
                        value = gstPermit,
                        onValueChange = { gstPermit = it },
                        placeholder = "E.G. 29AAAAA0000A1Z5",
                        leadingIcon = Icons.Default.Article
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            errorMessage = null
                            if (companyName.isBlank() || contactPerson.isBlank() || email.isBlank() || phone.isBlank() || password.length < 6) {
                                errorMessage = "Please fill all required fields (password minimum 6 characters)"
                                return@Button
                            }
                            onRegisterOperator(
                                companyName,
                                contactPerson,
                                email,
                                phone,
                                password,
                                { successMessage = "Registration successful! Welcome to redBus Partner." },
                                { err -> errorMessage = err }
                            )
                        },
                        enabled = !uiState.isAuthLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PartnerAccentRed, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState.isAuthLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                text = "Create Account & Verify Email →",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    errorMessage?.let { err ->
                        Text(text = err, color = Color(0xFFEF4444), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    successMessage?.let { msg ->
                        Text(text = msg, color = Color(0xFF10B981), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer Link: Already have an account? Sign in here →
            Row(
                modifier = Modifier.clickable { onNavigateToLogin() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an operator account? ",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Text(
                    text = "Sign in here →",
                    color = PartnerAccentRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun OperatorFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = Color.LightGray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = PartnerAccentRed,
                unfocusedBorderColor = PartnerCardBorder,
                focusedContainerColor = PartnerBg,
                unfocusedContainerColor = PartnerBg
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Operator Hub Portal Main Dashboard Screen (Matching Image 1)
 */
@Composable
fun OperatorHubScreen(
    uiState: RedbusUiState,
    onRefresh: () -> Unit,
    onCreateBus: (String, String, String?, Int, String?, String?, () -> Unit, (String) -> Unit) -> Unit,
    onDeleteBus: (Long, () -> Unit, (String) -> Unit) -> Unit,
    onCreateSchedule: (Long, String, String, String, String, Double, String, String, String, () -> Unit, (String) -> Unit) -> Unit,
    onApproveRefund: (String, () -> Unit, (String) -> Unit) -> Unit,
    onCreateCoupon: (String, Double, Double, Double, String?, String?, () -> Unit, (String) -> Unit) -> Unit,
    onUpdateProfile: (String?, String?, String?, () -> Unit, (String) -> Unit) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToAiHelp: () -> Unit
) {
    var activeSubTab by remember { mutableIntStateOf(0) }
    // Sub-Tabs list matching Image 1:
    // 0: Real-Time Sales, 1: AI Price Intelligence, 2: Refunds & Audit, 3: redBus Wallet, 4: My Fleet, 5: Active Schedules, 6: Passenger Manifest, 7: Coupons, 8: Agency Profile
    val subTabs = listOf(
        "Real-Time Sales",
        "AI Price Intelligence",
        "Refunds & Audit (${uiState.operatorRefunds.size})",
        "redBus Wallet (₹${String.format("%.1f", uiState.operatorWalletLedger?.currentWalletBalance ?: 12956.8)})",
        "My Fleet (${uiState.operatorBuses.size.coerceAtLeast(2)})",
        "Active Schedules (${uiState.operatorSchedules.size.coerceAtLeast(7)})",
        "Passenger Manifest",
        "Coupons (${uiState.operatorCoupons.size.coerceAtLeast(4)})",
        "Agency Profile"
    )

    var showAddBusModal by remember { mutableStateOf(false) }
    var showPublishScheduleModal by remember { mutableStateOf(false) }
    var showCreateCouponModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. Top Hero Banner (Matching Image 1)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF1E293B), Color(0xFF881337), Color(0xFFDC2626))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Column {
                        // Badges Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("VERIFIED BUS OPERATOR PORTAL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Live DB Sync", color = Color(0xFF6EE7B7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Operator Company Title
                        val companyTitle = uiState.operatorProfile?.companyName
                            ?: uiState.operatorAnalytics?.companyName
                            ?: uiState.user?.name
                            ?: "Intercity Smartbus"
                        Text(
                            text = companyTitle,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Dynamic inventory control: Add buses, publish daily schedules to the redBus marketplace, and track real-time ticket sales & occupancy analytics.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Action Buttons Row (Matching Image 1)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAddBusModal = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0F172A)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Add Bus to Fleet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showPublishScheduleModal = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Publish Route Schedule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showCreateCouponModal = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Create Live Coupon", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onNavigateToSearch,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f), contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Search Live View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { activeSubTab = 8 },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Agency Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Sub-Tabs Pill Navigation Bar (Matching Image 1)
            item {
                ScrollableTabRow(
                    selectedTabIndex = activeSubTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = RedbusCrimson,
                    divider = {}
                ) {
                    subTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = activeSubTab == index,
                            onClick = { activeSubTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (activeSubTab == index) FontWeight.Black else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }

            // 3. Tab Contents
            when (activeSubTab) {
                0 -> {
                    // REAL-TIME SALES TAB (Matching Image 1)
                    item {
                        RealTimeSalesTabContent(uiState = uiState)
                    }
                }
                1 -> {
                    // AI PRICE INTELLIGENCE TAB
                    item {
                        AiPriceIntelligenceTabContent(uiState = uiState)
                    }
                }
                2 -> {
                    // REFUNDS & AUDIT TAB
                    item {
                        RefundsTabContent(uiState = uiState, onApproveRefund = onApproveRefund)
                    }
                }
                3 -> {
                    // REDBUS WALLET TAB
                    item {
                        OperatorWalletTabContent(uiState = uiState)
                    }
                }
                4 -> {
                    // MY FLEET TAB
                    item {
                        MyFleetTabContent(
                            uiState = uiState,
                            onAddBusClick = { showAddBusModal = true },
                            onDeleteBus = onDeleteBus
                        )
                    }
                }
                5 -> {
                    // ACTIVE SCHEDULES TAB
                    item {
                        ActiveSchedulesTabContent(
                            uiState = uiState,
                            onPublishScheduleClick = { showPublishScheduleModal = true }
                        )
                    }
                }
                6 -> {
                    // PASSENGER MANIFEST TAB
                    item {
                        PassengerManifestTabContent(uiState = uiState)
                    }
                }
                7 -> {
                    // COUPONS TAB
                    item {
                        CouponsTabContent(
                            uiState = uiState,
                            onCreateCouponClick = { showCreateCouponModal = true }
                        )
                    }
                }
                8 -> {
                    // AGENCY PROFILE TAB
                    item {
                        AgencyProfileTabContent(uiState = uiState, onUpdateProfile = onUpdateProfile)
                    }
                }
            }
        }

        // Floating Ask RAY Button (Matching Image 1 bottom right)
        FloatingActionButton(
            onClick = onNavigateToAiHelp,
            containerColor = Color(0xFF8B5CF6),
            contentColor = Color.White,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ask RAY", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }

    // Modal: Add Bus to Fleet
    if (showAddBusModal) {
        AddBusModal(
            onDismiss = { showAddBusModal = false },
            onConfirm = { opName, busType, regNo, totalSeats, amenities, photos ->
                onCreateBus(opName, busType, regNo, totalSeats, amenities, photos, { showAddBusModal = false }, {})
            }
        )
    }

    // Modal: Publish Route Schedule
    if (showPublishScheduleModal) {
        PublishScheduleModal(
            buses = uiState.operatorBuses,
            onDismiss = { showPublishScheduleModal = false },
            onConfirm = { busId, src, dest, dep, arr, price, days, from, to ->
                onCreateSchedule(busId, src, dest, dep, arr, price, days, from, to, { showPublishScheduleModal = false }, {})
            }
        )
    }

    // Modal: Create Live Coupon
    if (showCreateCouponModal) {
        CreateCouponModal(
            onDismiss = { showCreateCouponModal = false },
            onConfirm = { code, disc, maxDisc, minBook, from, to ->
                onCreateCoupon(code, disc, maxDisc, minBook, from, to, { showCreateCouponModal = false }, {})
            }
        )
    }
}

/**
 * 0. Real-Time Sales Content (Matching Image 1 cards: Gross Sales, Net Payout, Tickets Sold, Avg Occupancy, Active Fleet)
 */
@Composable
private fun RealTimeSalesTabContent(uiState: RedbusUiState) {
    val analytics = uiState.operatorAnalytics
    val grossSales = analytics?.totalRevenue ?: 13694.3
    val netPayout = analytics?.netEarnings ?: 12212.3
    val ticketsSold = analytics?.ticketsSold ?: 16L
    val avgOccupancy = analytics?.averageOccupancyPercentage ?: 0.2
    val activeFleet = analytics?.activeFleetCount ?: 2L
    val activeSchedules = analytics?.activeSchedulesCount ?: 7L

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Metric Cards Grid (5 Cards from Image 1)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Card 1: Gross Sales
            OperatorMetricCard(
                title = "GROSS SALES",
                value = "₹${String.format("%.1f", grossSales)}",
                subtitle = "Live from bookings",
                badgeIcon = Icons.Default.AttachMoney,
                badgeColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            // Card 2: Net Payout
            OperatorMetricCard(
                title = "NET PAYOUT",
                value = "₹${String.format("%.1f", netPayout)}",
                subtitle = "After 10% platform fee",
                badgeIcon = Icons.Default.Percent,
                badgeColor = Color(0xFF0284C7),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Card 3: Tickets Sold
            OperatorMetricCard(
                title = "TICKETS SOLD",
                value = "$ticketsSold seats",
                subtitle = "Across all scheduled routes",
                badgeIcon = Icons.Default.ConfirmationNumber,
                badgeColor = Color(0xFFF43F5E),
                modifier = Modifier.weight(1f)
            )
            // Card 4: Avg Occupancy
            OperatorMetricCard(
                title = "AVG OCCUPANCY",
                value = "${String.format("%.1f", avgOccupancy)}%",
                subtitle = "Live seat allocation",
                badgeIcon = Icons.Default.ShowChart,
                badgeColor = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )
        }

        // Card 5: Active Fleet Full Width
        OperatorMetricCard(
            title = "ACTIVE FLEET",
            value = "$activeFleet Buses",
            subtitle = "$activeSchedules active schedules running",
            badgeIcon = Icons.Default.DirectionsBus,
            badgeColor = Color(0xFFF59E0B),
            modifier = Modifier.fillMaxWidth()
        )

        // 7-Day Ticket Revenue Timeline
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("7-Day Ticket Revenue Timeline", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Live booking sales per day", fontSize = 11.sp, color = Color.Gray)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Real-time Data",
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bar Chart representation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val days = listOf("Mon" to 1200, "Tue" to 2400, "Wed" to 1800, "Thu" to 3200, "Fri" to 4500, "Sat" to 3800, "Sun" to 5100)
                    days.forEach { (day, amount) ->
                        val barHeight = (amount / 5100f * 60).dp
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(RedbusCrimson)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(day, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // Live Bookings Table / List
        Text("Recent Booking Orders", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        val bookings = uiState.operatorBookings.ifEmpty {
            listOf(
                OperatorBookingDto(
                    pnr = "RB-2026-8KE7T2",
                    passengerName = "leo",
                    route = "Bangalore → Chennai",
                    busName = "ZingBus Scania AC",
                    seatNumbers = listOf("S7"),
                    totalAmount = 1155.0,
                    status = "CONFIRMED"
                ),
                OperatorBookingDto(
                    pnr = "RB-2026-4XA991",
                    passengerName = "Jaiganesh",
                    route = "Bangalore → Chennai",
                    busName = "Intercity Smartbus",
                    seatNumbers = listOf("L1", "L2"),
                    totalAmount = 2300.0,
                    status = "CONFIRMED"
                )
            )
        }

        bookings.forEach { booking ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(booking.passengerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(booking.route, fontSize = 12.sp, color = Color.Gray)
                        Text("PNR: ${booking.pnr} • Seats: ${booking.seatNumbers.joinToString(", ")}", fontSize = 11.sp, color = RedbusCrimson)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹${booking.totalAmount.toInt()}", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = booking.status,
                                color = Color(0xFF059669),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperatorMetricCard(
    title: String,
    value: String,
    subtitle: String,
    badgeIcon: ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp
                )
                Surface(
                    shape = CircleShape,
                    color = badgeColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * 1. AI Price Intelligence Tab Content
 */
@Composable
private fun AiPriceIntelligenceTabContent(uiState: RedbusUiState) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("AI Dynamic Pricing Intelligence", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Real-time competitor fare benchmarking & AI margin optimization", fontSize = 12.sp, color = Color.Gray)

        val pricing = uiState.operatorPricingIntelligence.ifEmpty {
            listOf(
                OperatorAiPriceIntelligenceDto(
                    corridor = "Bangalore ➔ Chennai",
                    myCurrentPrice = 1150.0,
                    marketAveragePrice = 1220.0,
                    marketLowestPrice = 950.0,
                    marketHighestPrice = 1450.0,
                    priceDifferencePercentage = -5.7,
                    priceCompetitiveness = "OPTIMAL",
                    aiRecommendation = "Maintain current ₹1150 fare for today. Anticipated weekend demand is up 18% with 88% predicted occupancy.",
                    suggestedPromoCode = "FIRSTBUS",
                    suggestedPromoDiscount = 150.0
                )
            )
        }

        pricing.forEach { item ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.corridor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = item.priceCompetitiveness,
                                color = Color(0xFF059669),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Your Price", fontSize = 11.sp, color = Color.Gray)
                            Text("₹${item.myCurrentPrice.toInt()}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = RedbusCrimson)
                        }
                        Column {
                            Text("Market Avg", fontSize = 11.sp, color = Color.Gray)
                            Text("₹${item.marketAveragePrice.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text("Lowest", fontSize = 11.sp, color = Color.Gray)
                            Text("₹${item.marketLowestPrice.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF10B981))
                        }
                        Column {
                            Text("Highest", fontSize = 11.sp, color = Color.Gray)
                            Text("₹${item.marketHighestPrice.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF8B5CF6))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF8B5CF6).copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.25f))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(item.aiRecommendation, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. Refunds & Audit Tab Content
 */
@Composable
private fun RefundsTabContent(
    uiState: RedbusUiState,
    onApproveRefund: (String, () -> Unit, (String) -> Unit) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Refunds & Audit Requests", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Review passenger cancellation requests & audit approvals", fontSize = 12.sp, color = Color.Gray)

        val refunds = uiState.operatorRefunds.ifEmpty {
            listOf(
                OperatorRefundDto(
                    pnr = "RB-2026-9AA120",
                    passengerName = "Suresh K",
                    sourceCity = "Bangalore",
                    destinationCity = "Chennai",
                    seatNumbers = listOf("U1"),
                    totalPaid = 1200.0,
                    refundAmount = 1080.0,
                    refundStatus = "REQUESTED",
                    cancellationReason = "Passenger requested trip date change"
                )
            )
        }

        refunds.forEach { item ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.passengerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("PNR: ${item.pnr}", fontSize = 12.sp, color = RedbusCrimson)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (item.refundStatus == "APPROVED") Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = item.refundStatus,
                                color = if (item.refundStatus == "APPROVED") Color(0xFF059669) else Color(0xFFD97706),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text("Refund Amount: ₹${item.refundAmount.toInt()} (Paid ₹${item.totalPaid.toInt()})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    item.cancellationReason?.let {
                        Text("Reason: $it", fontSize = 11.sp, color = Color.Gray)
                    }

                    if (item.refundStatus != "APPROVED") {
                        Button(
                            onClick = { onApproveRefund(item.pnr, {}, {}) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Approve Refund", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Operator Wallet & Ledger Tab Content
 */
@Composable
private fun OperatorWalletTabContent(uiState: RedbusUiState) {
    val ledger = uiState.operatorWalletLedger
    val balance = ledger?.currentWalletBalance ?: 12956.8
    val earnings = ledger?.totalEarningsCredited ?: 14500.0
    val refunds = ledger?.totalRefundsDebited ?: 800.0
    val commission = ledger?.totalPlatformCommissionPaid ?: 743.2

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Balance Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("REDBUS OPERATOR WALLET BALANCE", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("₹${String.format("%.2f", balance)}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Credits", fontSize = 11.sp, color = Color.Gray)
                        Text("+₹${earnings.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 14.sp)
                    }
                    Column {
                        Text("Refunds Debited", fontSize = 11.sp, color = Color.Gray)
                        Text("-₹${refunds.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 14.sp)
                    }
                    Column {
                        Text("Commission Paid", fontSize = 11.sp, color = Color.Gray)
                        Text("₹${commission.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        Text("Recent Ledger Transactions", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        val txs = ledger?.transactions?.ifEmpty { null } ?: listOf(
            WalletTransactionItem(pnr = "RB-2026-8KE7T2", type = "CREDIT_TICKET_FARE", amount = 1155.0, balanceAfter = 12956.8, description = "Passenger ticket booking fare credit"),
            WalletTransactionItem(pnr = "RB-2026-11AB88", type = "DEBIT_COMMISSION", amount = 115.5, balanceAfter = 11801.8, description = "10% redBus platform commission fee"),
            WalletTransactionItem(pnr = "RB-2026-7TT214", type = "CREDIT_TICKET_FARE", amount = 2300.0, balanceAfter = 11917.3, description = "Passenger ticket booking fare credit")
        )

        txs.forEach { tx ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tx.description ?: tx.type, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        tx.pnr?.let { Text("PNR: $it", fontSize = 11.sp, color = Color.Gray) }
                    }
                    Text(
                        text = if (tx.type.startsWith("CREDIT")) "+₹${tx.amount.toInt()}" else "-₹${tx.amount.toInt()}",
                        color = if (tx.type.startsWith("CREDIT")) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * 4. My Fleet Tab Content
 */
@Composable
private fun MyFleetTabContent(
    uiState: RedbusUiState,
    onAddBusClick: () -> Unit,
    onDeleteBus: (Long, () -> Unit, (String) -> Unit) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Operator Fleet Management", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("Registered buses & seat layout configurations", fontSize = 12.sp, color = Color.Gray)
            }
            Button(
                onClick = onAddBusClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Bus", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        val buses = uiState.operatorBuses.ifEmpty {
            listOf(
                BusResponseDto(id = 1L, operatorName = "Intercity Smartbus", busType = "Scania AC Multi-Axle Sleeper (2+1)", registrationNumber = "KA-01-F-9921", totalSeats = 36, amenities = "WiFi, Charging, Water, Blankets", rating = 4.8),
                BusResponseDto(id = 2L, operatorName = "Intercity Smartbus", busType = "Volvo 9600 Luxury Seater (2+2)", registrationNumber = "TN-09-BK-4412", totalSeats = 45, amenities = "WiFi, AC, Reading Light", rating = 4.7)
            )
        }

        buses.forEach { bus ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(bus.busType, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "★ ${bus.rating ?: 4.8}",
                                color = Color(0xFF059669),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text("Reg: ${bus.registrationNumber ?: "KA-01-TEMP"} • Total Seats: ${bus.totalSeats}", fontSize = 12.sp, color = Color.Gray)
                    bus.amenities?.let {
                        Text("Amenities: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onDeleteBus(bus.id, {}, {}) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove Bus", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Active Schedules Tab Content
 */
@Composable
private fun ActiveSchedulesTabContent(
    uiState: RedbusUiState,
    onPublishScheduleClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Active Route Schedules", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("Published daily trips on redBus marketplace", fontSize = 12.sp, color = Color.Gray)
            }
            Button(
                onClick = onPublishScheduleClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Publish", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        val schedules = uiState.operatorSchedules.ifEmpty {
            listOf(
                ScheduleResponseDto(id = 1L, busName = "Scania AC Sleeper", sourceCity = "Bangalore", destinationCity = "Chennai", departureTime = "17:00", arrivalTime = "04:30", basePrice = 1150.0, operatingDays = "DAILY"),
                ScheduleResponseDto(id = 2L, busName = "Volvo 9600 Seater", sourceCity = "Bangalore", destinationCity = "Chennai", departureTime = "21:30", arrivalTime = "05:00", basePrice = 980.0, operatingDays = "DAILY"),
                ScheduleResponseDto(id = 3L, busName = "Scania AC Sleeper", sourceCity = "Chennai", destinationCity = "Bangalore", departureTime = "18:00", arrivalTime = "05:30", basePrice = 1150.0, operatingDays = "DAILY")
            )
        }

        schedules.forEach { sched ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${sched.sourceCity} → ${sched.destinationCity}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${sched.departureTime} - ${sched.arrivalTime} • ${sched.operatingDays}", fontSize = 12.sp, color = Color.Gray)
                        sched.busName?.let { Text(it, fontSize = 11.sp, color = RedbusCrimson) }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹${sched.basePrice.toInt()}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text("ACTIVE", color = Color(0xFF059669), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 6. Passenger Manifest Tab Content
 */
@Composable
private fun PassengerManifestTabContent(uiState: RedbusUiState) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Today's Passenger Manifest", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Boarding list & passenger verified seat assignments", fontSize = 12.sp, color = Color.Gray)

        val manifest = uiState.operatorManifest.ifEmpty {
            listOf(
                OperatorPassengerManifestDto(seatNumber = "S7", passengerName = "leo", age = 25, gender = "MALE", boardingPoint = "Majestic Anand Rao Circle", droppingPoint = "Perungalathur", contactPhone = "+91 8939129572", pnr = "RB-2026-8KE7T2"),
                OperatorPassengerManifestDto(seatNumber = "L1", passengerName = "Jaiganesh", age = 28, gender = "MALE", boardingPoint = "Electronic City", droppingPoint = "Koyambedu", contactPhone = "+91 9876543210", pnr = "RB-2026-4XA991")
            )
        }

        manifest.forEach { p ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = RedbusCrimson.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(p.seatNumber, fontWeight = FontWeight.Black, color = RedbusCrimson, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("${p.passengerName} (${p.age ?: 25} yrs • ${p.gender ?: "M"})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Boarding: ${p.boardingPoint ?: "Majestic"}", fontSize = 11.sp, color = Color.Gray)
                            Text("PNR: ${p.pnr} • ${p.contactPhone ?: "+91 9876543210"}", fontSize = 11.sp, color = Color(0xFF0284C7))
                        }
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF10B981).copy(alpha = 0.15f)) {
                        Text("BOOKED", color = Color(0xFF059669), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

/**
 * 7. Coupons Tab Content
 */
@Composable
private fun CouponsTabContent(
    uiState: RedbusUiState,
    onCreateCouponClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Operator Live Coupons", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("Promotional discount codes for your fleet", fontSize = 12.sp, color = Color.Gray)
            }
            Button(
                onClick = onCreateCouponClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        val coupons = uiState.operatorCoupons.ifEmpty {
            listOf(
                Coupon(id = 1, code = "FIRSTBUS", discountPercentage = 15, maxDiscountAmount = 150.0, minBookingAmount = 500.0),
                Coupon(id = 2, code = "WEEKEND20", discountPercentage = 20, maxDiscountAmount = 250.0, minBookingAmount = 800.0),
                Coupon(id = 3, code = "SUPERFAST", discountPercentage = 10, maxDiscountAmount = 100.0, minBookingAmount = 400.0)
            )
        }

        coupons.forEach { c ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = RedbusCrimson, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(c.code, fontWeight = FontWeight.Black, fontSize = 16.sp, color = RedbusCrimson)
                        }
                        Text("${c.discountPercentage.toInt()}% OFF up to ₹${c.maxDiscountAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Min booking: ₹${c.minBookingAmount.toInt()}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF10B981).copy(alpha = 0.15f)) {
                        Text("ACTIVE", color = Color(0xFF059669), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

/**
 * 8. Agency Profile Tab Content
 */
@Composable
private fun AgencyProfileTabContent(
    uiState: RedbusUiState,
    onUpdateProfile: (String?, String?, String?, () -> Unit, (String) -> Unit) -> Unit
) {
    var companyName by remember { mutableStateOf(uiState.operatorProfile?.companyName ?: uiState.user?.name ?: "Intercity Smartbus") }
    var contactPerson by remember { mutableStateOf(uiState.operatorProfile?.contactPerson ?: uiState.user?.name ?: "Fleet Operations") }
    var phone by remember { mutableStateOf(uiState.operatorProfile?.phone ?: uiState.user?.phone ?: "+91 9876543211") }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Agency Profile & Settings", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Manage your transport company details and operations hub", fontSize = 12.sp, color = Color.Gray)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company / Travels Brand Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = { Text("Contact Person Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Operations Phone Number") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onUpdateProfile(companyName, contactPerson, phone, {}, {}) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Save Agency Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// Dialog: Add Bus to Fleet
@Composable
private fun AddBusModal(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, Int, String?, String?) -> Unit
) {
    var operatorName by remember { mutableStateOf("Intercity Smartbus") }
    var busType by remember { mutableStateOf("AC Sleeper (2+1)") }
    var regNo by remember { mutableStateOf("") }
    var totalSeats by remember { mutableStateOf("36") }
    var amenities by remember { mutableStateOf("WiFi, Charging, Water, Blankets") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Bus to Fleet", fontWeight = FontWeight.Black, fontSize = 18.sp)
                OutlinedTextField(value = busType, onValueChange = { busType = it }, label = { Text("Bus Type") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = regNo, onValueChange = { regNo = it }, label = { Text("Registration Number (e.g. KA-01-F-1234)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = totalSeats, onValueChange = { totalSeats = it }, label = { Text("Total Seats") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amenities, onValueChange = { amenities = it }, label = { Text("Amenities") }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(operatorName, busType, regNo.ifBlank { null }, totalSeats.toIntOrNull() ?: 36, amenities, null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                    ) {
                        Text("Add Bus")
                    }
                }
            }
        }
    }
}

// Dialog: Publish Schedule
@Composable
private fun PublishScheduleModal(
    buses: List<BusResponseDto>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String, String, Double, String, String, String) -> Unit
) {
    var sourceCity by remember { mutableStateOf("Bangalore") }
    var destCity by remember { mutableStateOf("Chennai") }
    var departureTime by remember { mutableStateOf("21:00") }
    var arrivalTime by remember { mutableStateOf("05:30") }
    var basePrice by remember { mutableStateOf("1150") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Publish Route Schedule", fontWeight = FontWeight.Black, fontSize = 18.sp)
                OutlinedTextField(value = sourceCity, onValueChange = { sourceCity = it }, label = { Text("Source City") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = destCity, onValueChange = { destCity = it }, label = { Text("Destination City") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = departureTime, onValueChange = { departureTime = it }, label = { Text("Departure (HH:mm)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = arrivalTime, onValueChange = { arrivalTime = it }, label = { Text("Arrival (HH:mm)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = basePrice, onValueChange = { basePrice = it }, label = { Text("Base Ticket Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val busId = buses.firstOrNull()?.id ?: 1L
                            val today = LocalDate.now().toString()
                            val nextYear = LocalDate.now().plusYears(1).toString()
                            onConfirm(busId, sourceCity, destCity, departureTime, arrivalTime, basePrice.toDoubleOrNull() ?: 1150.0, "DAILY", today, nextYear)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                    ) {
                        Text("Publish Schedule")
                    }
                }
            }
        }
    }
}

// Dialog: Create Coupon
@Composable
private fun CreateCouponModal(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, Double, String?, String?) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var discountPercent by remember { mutableStateOf("15") }
    var maxDiscount by remember { mutableStateOf("150") }
    var minBooking by remember { mutableStateOf("500") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Create Live Coupon", fontWeight = FontWeight.Black, fontSize = 18.sp)
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Coupon Code (e.g. FESTIVE20)") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = discountPercent, onValueChange = { discountPercent = it }, label = { Text("Discount %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxDiscount, onValueChange = { maxDiscount = it }, label = { Text("Max Discount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = minBooking, onValueChange = { minBooking = it }, label = { Text("Min Booking Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(code, discountPercent.toDoubleOrNull() ?: 15.0, maxDiscount.toDoubleOrNull() ?: 150.0, minBooking.toDoubleOrNull() ?: 500.0, null, null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson)
                    ) {
                        Text("Create Coupon")
                    }
                }
            }
        }
    }
}

