package com.spott.app.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Driver core routes
@Serializable
data object HomeMap : NavKey

@Serializable
data class SearchDestination(val query: String? = null) : NavKey

@Serializable
data object BookingsList : NavKey

@Serializable
data class BookingDetail(val bookingId: String) : NavKey

@Serializable
data object ActiveSession : NavKey

// Host routes
@Serializable
data object HostHub : NavKey

@Serializable
data object HostWizardEntry : NavKey

@Serializable
data class HostWizardStep(val step: String) : NavKey

// Profile & Settings routes
@Serializable
data object Profile : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data object PaymentMethods : NavKey

@Serializable
data object Vehicles : NavKey

// System / Support routes
@Serializable
data object Help : NavKey

@Serializable
data object NotificationCenter : NavKey

// Dialog routes
@Serializable
data class ConfirmationDialog(
    val title: String,
    val message: String,
    val action: String
) : NavKey

@Serializable
data object ErrorDialog : NavKey