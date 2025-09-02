package com.spott.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spott.app.nav.SpottNavigator
import com.spott.feature.parking.FindParkingIntent
import com.spott.feature.parking.FindParkingViewModel
import com.spott.feature.parking.SearchDestinationEffect
import com.spott.feature.parking.SearchDestinationScreen
import com.spott.feature.parking.SearchDestinationViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchDestinationRoute(
    navigator: SpottNavigator
) {
    val searchVm: SearchDestinationViewModel = koinViewModel()
    // Share the parking VM instance to push the destination back to the map
    val parkingVm: FindParkingViewModel = viewModel()

    val state by searchVm.state.collectAsState()
    val parkingState by parkingVm.state.collectAsState()

    // Seed proximity from current map camera target when screen opens or changes
    LaunchedEffect(parkingState.camera.target) {
        searchVm.updateProximity(parkingState.camera.target)
    }

    LaunchedEffect(Unit) {
        searchVm.effects.collect { effect ->
            when (effect) {
                is SearchDestinationEffect.NavigateToMap -> {
                    parkingVm.handleIntent(FindParkingIntent.OnDestinationSet(effect.destination))
                    navigator.navigateUp()
                }
                is SearchDestinationEffect.NavigateBack -> navigator.navigateUp()
            }
        }
    }

    SearchDestinationScreen(
        state = state,
        onIntent = searchVm::handleIntent,
    )
}
