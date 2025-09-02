package com.spott.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey

/**
 * Centralized navigator for managing app navigation.
 * This class encapsulates the navigation backstack and provides
 * type-safe navigation methods.
 * 
 * Uses SpottTopLevelBackStack to preserve navigation history
 * when switching between drawer sections.
 */
@Stable
class SpottNavigator {
    
    lateinit var topLevelBackStack: SpottTopLevelBackStack
        private set
    
    // Expose backStack for NavDisplay compatibility
    val backStack get() = topLevelBackStack.backStack

    fun initialize(topLevelBackStack: SpottTopLevelBackStack) {
        this.topLevelBackStack = topLevelBackStack
    }

    // Navigation methods
    fun navigateTo(destination: NavKey) {
        topLevelBackStack.navigateTo(destination)
    }
    
    fun navigateToTopLevel(destination: NavKey) {
        topLevelBackStack.navigateToTopLevel(destination)
    }

    fun navigateUp(): Boolean {
        return topLevelBackStack.navigateUp()
    }

    fun popBackStack(count: Int = 1) {
        repeat(count) {
            topLevelBackStack.navigateUp()
        }
    }

    fun navigateToRoot() {
        topLevelBackStack.navigateToRoot()
    }

    fun replaceWith(destination: NavKey) {
        topLevelBackStack.replaceWith(destination)
    }

    // Specific navigation methods for common flows
    fun navigateToSearch(query: String? = null) {
        navigateTo(SearchDestination(query))
    }

    fun navigateToBookingDetail(bookingId: String) {
        navigateTo(BookingDetail(bookingId))
    }

    fun navigateToActiveSession() {
        navigateTo(ActiveSession)
    }

    fun navigateToProfile() {
        navigateTo(Profile)
    }

    fun navigateToHostHub() {
        navigateTo(HostHub)
    }

    fun navigateToBecomeHost() {
        navigateTo(HostWizardEntry)
    }

    fun navigateToSettings() {
        navigateTo(Settings)
    }

    fun showConfirmationDialog(
        title: String,
        message: String,
        action: String
    ) {
        navigateTo(ConfirmationDialog(title, message, action))
    }

    fun showErrorDialog() {
        navigateTo(ErrorDialog)
    }

    // Check current destination
    fun isCurrentDestination(destination: NavKey): Boolean {
        return topLevelBackStack.isCurrentDestination(destination)
    }

    fun getCurrentDestination(): NavKey? {
        return topLevelBackStack.getCurrentDestination()
    }
}

/**
 * Remember and provide a SpottNavigator instance
 */
@Composable
fun rememberSpottNavigator(
    startDestination: NavKey = HomeMap
): SpottNavigator {
    val topLevelBackStack = remember { SpottTopLevelBackStack(startDestination) }
    return remember {
        SpottNavigator().apply {
            initialize(topLevelBackStack)
        }
    }
}