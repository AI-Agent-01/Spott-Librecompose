package com.spott.app.ui.drawer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.spott.app.SpottUiState
import com.spott.app.nav.*

data class Nav3DrawerItem(
    val destination: NavKey,
    val label: String,
    val icon: ImageVector,
    val section: DrawerSection = DrawerSection.Main,
    val showBadge: Boolean = false,
    val visible: Boolean = true,
)

fun nav3DrawerItems(state: SpottUiState): List<Nav3DrawerItem> = listOf(
    Nav3DrawerItem(HomeMap, "Find Parking", Icons.Filled.LocationOn, DrawerSection.Main),
    Nav3DrawerItem(
        BookingsList, "Recent Bookings", Icons.Filled.History,
        DrawerSection.Main, showBadge = state.unreadBookingsBadge
    ),
    Nav3DrawerItem(
        ActiveSession, "Active Session", Icons.Filled.DirectionsCar,
        DrawerSection.Session, visible = state.hasActiveBooking
    ),
    Nav3DrawerItem(
        HostHub, "Host Hub", Icons.Filled.Dashboard,
        DrawerSection.Host, visible = state.isHostUser
    ),
    Nav3DrawerItem(
        HostWizardEntry, "Become a Host", Icons.Filled.Star,
        DrawerSection.Host, visible = !state.isHostUser
    ),
    Nav3DrawerItem(Profile, "Profile & Account", Icons.Filled.Person, DrawerSection.Account),
    Nav3DrawerItem(Settings, "Settings", Icons.Filled.Settings, DrawerSection.Account),
).filter { it.visible }

@Composable
fun Nav3DrawerContent(
    navigator: SpottNavigator,
    appState: SpottUiState,
    currentDestination: Any?,
    onCloseDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.semantics { paneTitle = "Navigation menu" }
    ) {
        Spacer(Modifier.height(12.dp))
        
        // App header
        Text(
            text = "Spott",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
        )
        
        HorizontalDivider(Modifier.padding(horizontal = 28.dp))
        Spacer(Modifier.height(8.dp))

        val items = nav3DrawerItems(appState)
        val sections = items.groupBy { it.section }

        // Main section
        sections[DrawerSection.Main]?.forEach { item ->
            NavigationDrawerItem(
                icon = { 
                    Icon(
                        imageVector = item.icon, 
                        contentDescription = null
                    ) 
                },
                label = { Text(item.label) },
                badge = if (item.showBadge) {{ Badge() }} else null,
                selected = currentDestination == item.destination,
                onClick = {
                    navigator.navigateToTopLevel(item.destination)
                    onCloseDrawer()
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        // Session section
        sections[DrawerSection.Session]?.let { sessionItems ->
            if (sessionItems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(Modifier.padding(horizontal = 28.dp))
                Spacer(Modifier.height(8.dp))
                
                sessionItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { 
                            Icon(
                                imageVector = item.icon, 
                                contentDescription = null
                            ) 
                        },
                        label = { Text(item.label) },
                        selected = currentDestination == item.destination,
                        onClick = {
                            navigator.navigateTo(item.destination)
                            onCloseDrawer()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }

        // Host section
        sections[DrawerSection.Host]?.let { hostItems ->
            if (hostItems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(Modifier.padding(horizontal = 28.dp))
                Spacer(Modifier.height(8.dp))
                
                hostItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { 
                            Icon(
                                imageVector = item.icon, 
                                contentDescription = null
                            ) 
                        },
                        label = { Text(item.label) },
                        selected = currentDestination == item.destination,
                        onClick = {
                            navigator.navigateTo(item.destination)
                            onCloseDrawer()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }

        // Account section
        sections[DrawerSection.Account]?.let { accountItems ->
            if (accountItems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(Modifier.padding(horizontal = 28.dp))
                Spacer(Modifier.height(8.dp))
                
                accountItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { 
                            Icon(
                                imageVector = item.icon, 
                                contentDescription = null
                            ) 
                        },
                        label = { Text(item.label) },
                        selected = currentDestination == item.destination,
                        onClick = {
                            navigator.navigateTo(item.destination)
                            onCloseDrawer()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    }
}