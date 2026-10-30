package com.redbus.ai.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.redbus.ai.ui.screens.*
import com.redbus.ai.ui.theme.RedBusAITheme
import com.redbus.ai.ui.theme.RedbusCrimson
import com.redbus.ai.ui.viewmodel.RedbusViewModel

sealed class ScreenRoute(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Home : ScreenRoute("home", "Home", Icons.Default.Home)
    data object SearchResults : ScreenRoute("search_results", "Buses", Icons.Default.DirectionsBus)
    data object MyTrips : ScreenRoute("my_trips", "Bookings", Icons.Default.ConfirmationNumber)
    data object AiCopilot : ScreenRoute("ai_copilot", "AI Help", Icons.Default.AutoAwesome)
    data object Profile : ScreenRoute("profile", "Account", Icons.Default.Person)
    data object SeatSelection : ScreenRoute("seat_selection", "Select Seats")
    data object Checkout : ScreenRoute("checkout", "Checkout")
}

private fun navigateToTab(navController: NavHostController, tabRoute: String) {
    navController.navigate(tabRoute) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun RedbusAiApp(viewModel: RedbusViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showWalletModal by remember { mutableStateOf(false) }
    var showWalletLoginPrompt by remember { mutableStateOf(false) }

    val handleOpenWallet: () -> Unit = {
        if (!uiState.isAuthenticated || uiState.user == null) {
            showWalletLoginPrompt = true
        } else {
            showWalletModal = true
        }
    }

    val bottomNavTabs = listOf(
        ScreenRoute.Home,
        ScreenRoute.SearchResults,
        ScreenRoute.MyTrips,
        ScreenRoute.AiCopilot,
        ScreenRoute.Profile
    )

    // Keep the Bottom Navigation Bar visible across all tabs; highlight "Buses" during seat/checkout flow
    val activeTabRoute = when (currentRoute) {
        ScreenRoute.SeatSelection.route, ScreenRoute.Checkout.route -> ScreenRoute.SearchResults.route
        else -> currentRoute ?: ScreenRoute.Home.route
    }

    RedBusAITheme(darkTheme = uiState.isDarkTheme) {
        val statusBarBackground = when {
            uiState.isDarkTheme -> MaterialTheme.colorScheme.background
            activeTabRoute == ScreenRoute.Home.route -> Color(0xFFFFF8EE)
            else -> MaterialTheme.colorScheme.surface
        }

        Scaffold(
            topBar = {
                // Dedicated Status Bar Cutout so time, signal, Wi-Fi & battery are always visible
                // in both Light Mode (dark status bar icons) and Dark Mode (white status bar icons)
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(statusBarBackground)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 10.dp
                ) {
                    bottomNavTabs.forEach { tab ->
                        val selected = activeTabRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (tab.route == ScreenRoute.SearchResults.route &&
                                    (currentRoute == ScreenRoute.SeatSelection.route || currentRoute == ScreenRoute.Checkout.route)
                                ) {
                                    navController.popBackStack(ScreenRoute.SearchResults.route, inclusive = false)
                                } else {
                                    navigateToTab(navController, tab.route)
                                }
                            },
                            icon = {
                                tab.icon?.let {
                                    Icon(imageVector = it, contentDescription = tab.label)
                                }
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Black else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = RedbusCrimson,
                                indicatorColor = RedbusCrimson,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ScreenRoute.Home.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    ) + scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    ) + scaleOut(
                        targetScale = 0.94f,
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    ) + scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    ) + scaleOut(
                        targetScale = 0.94f,
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) {
                composable(ScreenRoute.Home.route) {
                    HomeScreen(
                        uiState = uiState,
                        onSwapCities = viewModel::swapCities,
                        onSelectSourceCity = viewModel::selectSourceCity,
                        onSelectDestinationCity = viewModel::selectDestinationCity,
                        onSelectCities = viewModel::updateCities,
                        onSearchCities = viewModel::searchCities,
                        onSelectDate = viewModel::updateTravelDate,
                        onNlpQueryChange = viewModel::updateNlpQuery,
                        onExecuteNlpSearch = {
                            viewModel.executeAiNlpSearch {
                                navigateToTab(navController, ScreenRoute.SearchResults.route)
                            }
                        },
                        onSearchBuses = {
                            viewModel.performSearch {
                                navigateToTab(navController, ScreenRoute.SearchResults.route)
                            }
                        },
                        onQuickRouteClick = { route ->
                            viewModel.selectRouteForSeats(route) {
                                navController.navigate(ScreenRoute.SeatSelection.route)
                            }
                        },
                        onNavigateToAccount = {
                            navigateToTab(navController, ScreenRoute.Profile.route)
                        },
                        onNavigateToBookings = {
                            navigateToTab(navController, ScreenRoute.MyTrips.route)
                        },
                        onOpenWallet = handleOpenWallet,
                        onToggleDarkTheme = viewModel::toggleDarkTheme,
                        onToggleBookingForWomen = viewModel::toggleBookingForWomen
                    )
                }

                composable(ScreenRoute.SearchResults.route) {
                    SearchResultsScreen(
                        uiState = uiState,
                        filteredRoutes = viewModel.getFilteredRoutes(),
                        onFilterSelected = viewModel::setFilter,
                        onSelectRoute = { route ->
                            viewModel.selectRouteForSeats(route) {
                                navController.navigate(ScreenRoute.SeatSelection.route)
                            }
                        },
                        onUpdateTravelDate = { newDate ->
                            viewModel.updateTravelDate(newDate)
                            viewModel.performSearch()
                        },
                        onBack = {
                            navigateToTab(navController, ScreenRoute.Home.route)
                        }
                    )
                }

                composable(ScreenRoute.SeatSelection.route) {
                    SeatSelectionScreen(
                        uiState = uiState,
                        onToggleSeat = viewModel::toggleSeatSelection,
                        onUpdateBoardingDropping = viewModel::updateBoardingDropping,
                        onProceedToCheckout = {
                            viewModel.lockSelectedSeatsAndProceed {
                                navController.navigate(ScreenRoute.Checkout.route)
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onSubmitReview = { busId, rating, comment ->
                            viewModel.submitReview(busId, rating, comment)
                        }
                    )
                }

                composable(ScreenRoute.Checkout.route) {
                    CheckoutScreen(
                        uiState = uiState,
                        fareBreakdown = viewModel.calculateFareBreakdown(),
                        availableCoupons = uiState.availableCoupons,
                        onUpdatePassenger = viewModel::updatePassenger,
                        onUpdateContact = viewModel::updateContactDetails,
                        onToggleFreeCancellation = viewModel::toggleFreeCancellation,
                        onToggleTripGuarantee = viewModel::toggleTripGuarantee,
                        onToggleUseWallet = viewModel::toggleUseWallet,
                        onApplyCoupon = viewModel::applyCouponCode,
                        onRemoveCoupon = viewModel::removeCoupon,
                        onConfirmAndPay = {
                            viewModel.initiateBookingAndRazorpayOrder(
                                onWalletInstantConfirm = {
                                    viewModel.resetBookingFlow()
                                    navController.popBackStack(ScreenRoute.SearchResults.route, inclusive = false)
                                    navigateToTab(navController, ScreenRoute.MyTrips.route)
                                }
                            )
                        },
                        onCompleteRazorpayPayment = { orderId, paymentId, signature ->
                            viewModel.completeRazorpayPayment(orderId, paymentId, signature) {
                                viewModel.resetBookingFlow()
                                navController.popBackStack(ScreenRoute.SearchResults.route, inclusive = false)
                                navigateToTab(navController, ScreenRoute.MyTrips.route)
                            }
                        },
                        onDismissRazorpayModal = viewModel::dismissRazorpayModal,
                        onNavigateToAuth = {
                            navigateToTab(navController, ScreenRoute.Profile.route)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(ScreenRoute.AiCopilot.route) {
                    AiCopilotScreen(
                        uiState = uiState,
                        onSendMessage = viewModel::sendAiMessage,
                        onSelectRoute = { route ->
                            viewModel.selectRouteForSeats(route) {
                                navController.navigate(ScreenRoute.SeatSelection.route)
                            }
                        }
                    )
                }

                composable(ScreenRoute.MyTrips.route) {
                    MyBookingsScreen(
                        uiState = uiState,
                        onSearchPnr = viewModel::searchBookingByPnr,
                        onRefreshBookings = viewModel::fetchMyBookings,
                        onCancelBooking = viewModel::cancelLiveBooking,
                        onEmailTicket = viewModel::emailTicket,
                        onNavigateToLogin = {
                            navigateToTab(navController, ScreenRoute.Profile.route)
                        },
                        onSubmitReview = { busId, rating, comment ->
                            viewModel.submitReview(busId, rating, comment)
                        }
                    )
                }

                composable(ScreenRoute.Profile.route) {
                    ProfileAndWalletScreen(
                        uiState = uiState,
                        onLoginWithEmail = { email, pass -> viewModel.loginWithEmail(email, pass) },
                        onRegisterAccount = { name, email, phone, pass ->
                            viewModel.registerAccount(name, email, phone, pass)
                        },
                        onRequestMobileOtp = viewModel::requestMobileOtp,
                        onVerifyMobileOtp = { phone, otp -> viewModel.verifyMobileOtpAndLogin(phone, otp) },
                        onUpdateProfile = viewModel::updateProfileDetails,
                        onAddTraveller = viewModel::addTraveller,
                        onDeleteTraveller = viewModel::deleteTraveller,
                        onRefreshAccount = viewModel::refreshAuthenticatedAccountData,
                        onLogout = viewModel::logout,
                        onToggleDarkTheme = viewModel::toggleDarkTheme,
                        onUpdateAvatar = viewModel::updateProfileAvatar,
                        onChangePassword = viewModel::changePassword,
                        onSendResetOtp = viewModel::sendPasswordResetOtp,
                        onTopUpWallet = viewModel::topUpWallet,
                        onOpenWallet = handleOpenWallet,
                        onFetchDeviceSessions = viewModel::fetchDeviceSessions,
                        onRevokeDeviceSession = viewModel::revokeDeviceSession,
                        onRevokeAllOtherSessions = viewModel::revokeAllOtherDeviceSessions,
                        onNavigateToBookings = {
                            navigateToTab(navController, ScreenRoute.MyTrips.route)
                        },
                        onSelectLanguage = viewModel::setLanguage,
                        onToggleBookingForWomen = viewModel::toggleBookingForWomen,
                        onDeactivateAccount = { viewModel.deactivateAccount { navController.navigate(ScreenRoute.Home.route) } },
                        onDeleteAccount = { pass -> viewModel.deleteAccount(pass) { navController.navigate(ScreenRoute.Home.route) } }
                    )
                }
            }
        }

        // Live Dynamic redBus Wallet Modal (opened from Home bar & Account tab)
        RedbusWalletModal(
            isOpen = showWalletModal,
            onDismiss = { showWalletModal = false },
            uiState = uiState,
            onNavigateToLogin = { navigateToTab(navController, ScreenRoute.Profile.route) }
        )

        // Login Required Prompt Dialog when accessing Wallet without being logged in
        if (showWalletLoginPrompt) {
            AlertDialog(
                onDismissRequest = { showWalletLoginPrompt = false },
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = RedbusCrimson.copy(alpha = 0.12f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = RedbusCrimson,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = "Sign In Required",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = "Please log in to your redBus account to access your redBus Wallet, view balance and cashback history.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showWalletLoginPrompt = false
                            navigateToTab(navController, ScreenRoute.Profile.route)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedbusCrimson),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Log In / Sign Up", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWalletLoginPrompt = false }) {
                        Text("Cancel", fontWeight = FontWeight.Medium)
                    }
                }
            )
        }
    }
}
