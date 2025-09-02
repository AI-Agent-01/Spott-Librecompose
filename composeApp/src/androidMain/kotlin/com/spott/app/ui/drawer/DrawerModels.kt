package com.spott.app.ui.drawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.spott.app.SpottUiState
import com.spott.app.nav.*

data class DrawerItem(
    val destination: NavKey,
    val label: String,
    val icon: ImageVector,
    val section: DrawerSection = DrawerSection.Main,
    val showBadge: Boolean = false,
    val visible: Boolean = true,
)

enum class DrawerSection { Main, Session, Host, Account, Support }

fun drawerItems(state: SpottUiState): List<DrawerItem> = listOf(
    DrawerItem(HomeMap, "Find Parking", Icons.Filled.LocationOn, DrawerSection.Main),
    DrawerItem(
        BookingsList, "Recent Bookings", Icons.Filled.History,
        DrawerSection.Main, showBadge = state.unreadBookingsBadge
    ),
    DrawerItem(
        ActiveSession, "Active Session", Icons.Filled.DirectionsCar,
        DrawerSection.Session, visible = state.hasActiveBooking
    ),
    DrawerItem(
        HostHub, "Host Hub", Icons.Filled.Dashboard,
        DrawerSection.Host, visible = state.isHostUser
    ),
    DrawerItem(
        HostWizardEntry, "Become a Host", Icons.Filled.Star,
        DrawerSection.Host, visible = !state.isHostUser
    ),
    DrawerItem(Profile, "Profile & Account", Icons.Filled.Person, DrawerSection.Account),
    DrawerItem(Settings, "Settings", Icons.Filled.Settings, DrawerSection.Account),
    // Optional: help/legal in Support section. Keep off by default.
    // DrawerItem(Route.Help.route, "Help & Legal", Icons.Filled.HelpOutline, DrawerSection.Support)
).filter { it.visible }