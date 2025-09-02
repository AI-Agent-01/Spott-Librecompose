package com.spott.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spott.app.nav.*
import com.spott.app.ui.drawer.Nav3DrawerContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SpottAppNav3(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier
) {
    val navigator = rememberSpottNavigator(startDestination = HomeMap)
    val vm: SpottAppViewModel = viewModel()
    val appState by vm.state.collectAsState()
    
    val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val currentDestination by remember {
        derivedStateOf { navigator.getCurrentDestination() }
    }
    
    if (isExpandedScreen) {
        PermanentNavigationDrawerContent(
            navigator = navigator,
            vm = vm,
            appState = appState,
            currentDestination = currentDestination,
            modifier = modifier
        )
    } else {
        ModalNavigationDrawerContent(
            navigator = navigator,
            vm = vm,
            appState = appState,
            currentDestination = currentDestination,
            modifier = modifier
        )
    }
}

@Composable
private fun PermanentNavigationDrawerContent(
    navigator: SpottNavigator,
    vm: SpottAppViewModel,
    appState: SpottUiState,
    currentDestination: Any?,
    modifier: Modifier = Modifier
) {
    PermanentNavigationDrawer(
        drawerContent = {
            Nav3DrawerContent(
                navigator = navigator,
                appState = appState,
                currentDestination = currentDestination
            )
        },
        modifier = modifier
    ) {
        SpottScaffold(
            navigator = navigator,
            vm = vm,
            currentDestination = currentDestination,
            showMenuIcon = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalNavigationDrawerContent(
    navigator: SpottNavigator,
    vm: SpottAppViewModel,
    appState: SpottUiState,
    currentDestination: Any?,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Nav3DrawerContent(
                navigator = navigator,
                appState = appState,
                currentDestination = currentDestination,
                onCloseDrawer = {
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        },
        gesturesEnabled = true,
        modifier = modifier
    ) {
        SpottScaffold(
            navigator = navigator,
            vm = vm,
            currentDestination = currentDestination,
            showMenuIcon = true,
            onMenuClick = {
                scope.launch {
                    drawerState.open()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpottScaffold(
    navigator: SpottNavigator,
    vm: SpottAppViewModel,
    currentDestination: Any?,
    showMenuIcon: Boolean,
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Only show top bar on drawer destinations (not on Search)
    val showTopBar = currentDestination !is SearchDestination
    
    Scaffold(
        modifier = modifier,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { 
                        Text(
                            text = when (currentDestination) {
                                HomeMap -> "Find Parking"
                                BookingsList -> "Recent Bookings"
                                ActiveSession -> "Active Session"
                                HostHub -> "Host Hub"
                                HostWizardEntry -> "Become a Host"
                                Profile -> "Profile & Account"
                                Settings -> "Settings"
                                Help -> "Help & Legal"
                                else -> "Spott"
                            }
                        )
                    },
                    navigationIcon = {
                        if (showMenuIcon) {
                            IconButton(
                                onClick = onMenuClick,
                                modifier = Modifier.semantics { 
                                    contentDescription = "Open navigation menu" 
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize()) {
            SpottNavDisplay(
                navigator = navigator,
                viewModel = vm,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}