package com.redbus.ai.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
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
    data object AiCopilot : ScreenRoute("ai_copilot", "AI Copilot", Icons.Default.AutoAwesome)
    data object MyTrips : ScreenRoute("my_trips", "Bookings", Icons.Default.ConfirmationNumber)
    data object Profile : ScreenRoute("profile", "Account", Icons.Default.Person)
    data object WebPortal : ScreenRoute("web_portal", "Web UI", Icons.Default.Language)
    data object SearchResults : ScreenRoute("search_results", "Search Results")
    data object SeatSelection : ScreenRoute("seat_selection", "Select Seats")
    data object Checkout : ScreenRoute("checkout", "Checkout")
}

@Composable
fun RedbusAiApp(viewModel: RedbusViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavTabs = listOf(
        ScreenRoute.Home,
        ScreenRoute.AiCopilot,
        ScreenRoute.MyTrips,
        ScreenRoute.Profile,
        ScreenRoute.WebPortal
    )
    val showBottomBar = currentRoute in bottomNavTabs.map { it.route }

    RedBusAITheme(darkTheme = uiState.isDarkTheme) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 10.dp
                    ) {
                        bottomNavTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
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
                                        fontSize = 10.sp,
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
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ScreenRoute.Home.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(320)
                    ) + fadeIn(animationSpec = tween(320))
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(320)
                    ) + fadeOut(animationSpec = tween(320))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(320)
                    ) + fadeIn(animationSpec = tween(320))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(320)
                    ) + fadeOut(animationSpec = tween(320))
                }
            ) {
                composable(ScreenRoute.Home.route) {
                    HomeScreen(
                        uiState = uiState,
                        onSwapCities = viewModel::swapCities,
                        onSelectCities = viewModel::updateCities,
                        onSelectDate = viewModel::updateTravelDate,
                        onNlpQueryChange = viewModel::updateNlpQuery,
                        onExecuteNlpSearch = {
                            viewModel.executeAiNlpSearch {
                                navController.navigate(ScreenRoute.SearchResults.route)
                            }
                        },
                        onSearchBuses = {
                            viewModel.performSearch {
                                navController.navigate(ScreenRoute.SearchResults.route)
                            }
                        },
                        onQuickRouteClick = { route ->
                            viewModel.selectRouteForSeats(route) {
                                navController.navigate(ScreenRoute.SeatSelection.route)
                            }
                        },
                        onNavigateToAccount = {
                            navController.navigate(ScreenRoute.Profile.route)
                        },
                        onOpenWebPortal = { url ->
                            viewModel.setWebPortalUrl(url)
                            navController.navigate(ScreenRoute.WebPortal.route)
                        },
                        onToggleDarkTheme = viewModel::toggleDarkTheme
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
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(ScreenRoute.SeatSelection.route) {
                    SeatSelectionScreen(
                        uiState = uiState,
                        onSelectDeck = viewModel::selectDeck,
                        onToggleSeat = viewModel::toggleSeatSelection,
                        onUpdateBoardingDropping = viewModel::updateBoardingDropping,
                        onProceedToCheckout = {
                            viewModel.lockSelectedSeatsAndProceed {
                                navController.navigate(ScreenRoute.Checkout.route)
                            }
                        },
                        onBack = { navController.popBackStack() }
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
                            viewModel.createLiveBooking(
                                onBookingCreated = {
                                    navController.navigate(ScreenRoute.MyTrips.route) {
                                        popUpTo(ScreenRoute.Home.route)
                                    }
                                },
                                onRequireRazorpayWeb = { checkoutUrl ->
                                    viewModel.setWebPortalUrl(checkoutUrl)
                                    navController.navigate(ScreenRoute.WebPortal.route)
                                }
                            )
                        },
                        onNavigateToAuth = {
                            navController.navigate(ScreenRoute.Profile.route)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(ScreenRoute.AiCopilot.route) {
                    AiCopilotScreen(
                        uiState = uiState,
                        onSendMessage = viewModel::sendAiMessage
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
                            navController.navigate(ScreenRoute.Profile.route)
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
                        onOpenWebPortal = { url ->
                            viewModel.setWebPortalUrl(url)
                            navController.navigate(ScreenRoute.WebPortal.route)
                        },
                        onLogout = viewModel::logout,
                        onToggleDarkTheme = viewModel::toggleDarkTheme
                    )
                }

                composable(ScreenRoute.WebPortal.route) {
                    WebPortalScreen(
                        uiState = uiState,
                        initialUrl = uiState.activeWebUrl,
                        onSyncWebSessionToNative = viewModel::syncSessionFromWeb
                    )
                }
            }
        }
    }
}
