package com.spott.app.nav

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import com.spott.app.SpottAppViewModel
import com.spott.app.ui.screens.*
import com.spott.feature.parking.FindParkingScreen
import com.spott.feature.parking.FindParkingState
import com.spott.feature.parking.FindParkingIntent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spott.feature.parking.FindParkingViewModel

/**
 * Navigation display for Spott app with production-ready animations
 */
@Composable
fun SpottNavDisplay(
    navigator: SpottNavigator,
    viewModel: SpottAppViewModel,
    modifier: Modifier = Modifier
) {
    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
    
    NavDisplay(
        backStack = navigator.backStack,
        modifier = modifier,
        onBack = { keysToRemove -> 
            repeat(keysToRemove) { 
                navigator.navigateUp() 
            }
        },
        sceneStrategy = dialogStrategy,
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            // Driver core screens
            entry<HomeMap>(
                metadata = NavDisplay.transitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                } + NavDisplay.popTransitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                } + NavDisplay.predictivePopTransitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                }
            ) {
                val findParkingViewModel: FindParkingViewModel = viewModel()
                val state by findParkingViewModel.state.collectAsState()
                
                FindParkingScreen(
                    state = state,
                    onIntent = { intent ->
                        when (intent) {
                            is FindParkingIntent.OnSearchBarClick -> navigator.navigateToSearch()
                            else -> findParkingViewModel.handleIntent(intent)
                        }
                    }
                )
            }
            
            entry<SearchDestination>(
                metadata = NavDisplay.transitionSpec {
                    slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(VERTICAL_ANIMATION_DURATION_MS, easing = standardEasing)
                    ) togetherWith fadeOut(
                        animationSpec = tween(VERTICAL_ANIMATION_DURATION_MS / 2)
                    )
                } + NavDisplay.popTransitionSpec {
                    fadeIn(
                        animationSpec = tween(VERTICAL_ANIMATION_DURATION_MS / 2, 
                            delayMillis = VERTICAL_ANIMATION_DURATION_MS / 2)
                    ) togetherWith slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(VERTICAL_ANIMATION_DURATION_MS, easing = standardEasing)
                    )
                } + NavDisplay.predictivePopTransitionSpec {
                    fadeIn(
                        animationSpec = tween(VERTICAL_ANIMATION_DURATION_MS / 2)
                    ) togetherWith slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(VERTICAL_ANIMATION_DURATION_MS, easing = standardEasing)
                    )
                }
            ) { route ->
                SearchDestinationScreen(
                    // TODO: Pass route.query to screen when implemented
                    onNavigateUp = { navigator.navigateUp() }
                )
            }
            
            entry<BookingsList>(
                metadata = NavDisplay.transitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                } + NavDisplay.popTransitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                } + NavDisplay.predictivePopTransitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                }
            ) {
                LaunchedEffect(Unit) {
                    viewModel.clearBookingsBadge()
                }
                BookingsScreen()
            }
            
            entry<BookingDetail> { route ->
                // TODO: Implement BookingDetailScreen
                BookingsScreen() // Placeholder
            }
            
            entry<ActiveSession>(
                metadata = NavDisplay.transitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                } + NavDisplay.popTransitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                } + NavDisplay.predictivePopTransitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                }
            ) {
                ActiveSessionScreen()
            }
            
            // Host screens
            entry<HostHub>(
                metadata = NavDisplay.transitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                } + NavDisplay.popTransitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                } + NavDisplay.predictivePopTransitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                }
            ) {
                HostHubScreen()
            }
            
            entry<HostWizardEntry> {
                HostWizardEntryScreen()
            }
            
            entry<HostWizardStep> { route ->
                // TODO: Implement wizard steps
                HostWizardEntryScreen() // Placeholder
            }
            
            // Profile & Settings screens
            entry<Profile>(
                metadata = NavDisplay.transitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                } + NavDisplay.popTransitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                } + NavDisplay.predictivePopTransitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                }
            ) {
                ProfileScreen()
            }
            
            entry<Settings> {
                SettingsScreen()
            }
            
            entry<PaymentMethods> {
                // TODO: Implement PaymentMethodsScreen
                ProfileScreen() // Placeholder
            }
            
            entry<Vehicles> {
                // TODO: Implement VehiclesScreen
                ProfileScreen() // Placeholder
            }
            
            // System screens
            entry<Help>(
                metadata = NavDisplay.transitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                } + NavDisplay.popTransitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                } + NavDisplay.predictivePopTransitionSpec {
                    fadeIn(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    ) togetherWith fadeOut(
                        animationSpec = tween(FADE_ANIMATION_DURATION_MS)
                    )
                }
            ) {
                HelpScreen()
            }
            
            entry<NotificationCenter> {
                // TODO: Implement NotificationCenterScreen
                ProfileScreen() // Placeholder
            }
            
            // Dialog screens
            entry<ConfirmationDialog>(
                metadata = DialogSceneStrategy.dialog(
                    DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = false
                    )
                )
            ) { route ->
                ConfirmationDialogContent(
                    title = route.title,
                    message = route.message,
                    action = route.action,
                    onConfirm = {
                        // Handle confirmation
                        navigator.navigateUp()
                    },
                    onDismiss = {
                        navigator.navigateUp()
                    },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                )
            }
            
            entry<ErrorDialog>(
                metadata = DialogSceneStrategy.dialog(
                    DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                )
            ) {
                ErrorDialogContent(
                    onDismiss = { navigator.navigateUp() },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                )
            }
        },
        // Default transitions
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(ANIMATION_DURATION_MS, easing = standardEasing)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(ANIMATION_DURATION_MS, easing = standardEasing)
            )
        },
        popTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(ANIMATION_DURATION_MS, easing = standardEasing)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(ANIMATION_DURATION_MS, easing = standardEasing)
            )
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(ANIMATION_DURATION_MS, easing = standardEasing)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(ANIMATION_DURATION_MS, easing = standardEasing)
            )
        }
    )
}

// Animation specifications
private const val ANIMATION_DURATION_MS = 300
private const val FADE_ANIMATION_DURATION_MS = 250
private const val VERTICAL_ANIMATION_DURATION_MS = 400

private val standardEasing = FastOutSlowInEasing