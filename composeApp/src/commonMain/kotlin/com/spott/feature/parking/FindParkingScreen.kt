package com.spott.feature.parking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spott.map.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// UI State
data class FindParkingState(
    val isLoading: Boolean = false,
    val camera: CameraModel = CameraModel(
        target = LatLng(37.7749, -122.4194), // Default to SF
        zoom = 13f
    ),
    val userLocation: LatLng? = null,
    val destinationLocation: LatLng? = null,
    val parkingSpots: List<ParkingSpot> = emptyList(),
    val selectedSpotId: String? = null,
    val searchAreaButtonVisible: Boolean = false,
    val duration: ParkingDuration = ParkingDuration.ONE_HOUR,
    val isMyLocationEnabled: Boolean = false,
    val error: String? = null
)

// Intents
sealed interface FindParkingIntent {
    data object OnAppear : FindParkingIntent
    data object OnSearchBarClick : FindParkingIntent
    data object OnMyLocationClick : FindParkingIntent
    data object OnSearchThisAreaClick : FindParkingIntent
    data class OnMarkerClick(val markerId: String) : FindParkingIntent
    data class OnMapCameraMove(val center: LatLng, val zoom: Float) : FindParkingIntent
    data class OnMapCameraIdle(val bounds: LatLngBounds, val zoom: Float) : FindParkingIntent
    data class OnDurationChange(val duration: ParkingDuration) : FindParkingIntent
    data class OnDestinationSet(val location: LatLng) : FindParkingIntent
    data object OnDismissError : FindParkingIntent
}

// Effects
sealed interface FindParkingEffect {
    data class NavigateToSearch(val currentLocation: LatLng?) : FindParkingEffect
    data class ShowListingDetails(val spotId: String) : FindParkingEffect
    data class ShowToast(val message: String) : FindParkingEffect
    data object RequestLocationPermission : FindParkingEffect
}

// Domain models
data class ParkingSpot(
    val id: String,
    val location: LatLng,
    val pricePerHour: Float,
    val isTopSpott: Boolean = false,
    val rating: Float? = null,
    val distance: Float? = null // in meters
)

enum class ParkingDuration(val label: String, val hours: Float) {
    FIFTEEN_MIN("15m", 0.25f),
    THIRTY_MIN("30m", 0.5f),
    ONE_HOUR("1h", 1f),
    TWO_HOURS("2h", 2f),
    FOUR_HOURS("4h", 4f),
    ALL_DAY("All Day", 24f)
}

@Composable
fun FindParkingScreen(
    state: FindParkingState,
    onIntent: (FindParkingIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showSearchAreaButton by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Map
        SpotMapView(
            modifier = Modifier.fillMaxSize(),
            props = SpotMapProps(
                camera = state.camera,
                myLocationEnabled = state.isMyLocationEnabled,
                markers = buildMarkers(
                    spots = state.parkingSpots,
                    destination = state.destinationLocation,
                    selectedId = state.selectedSpotId
                ),
                clustersEnabled = state.camera.zoom ?: 13f < 15f
            ),
            onEvent = { event ->
                when (event) {
                    is SpotMapEvent.OnCameraMove -> {
                        onIntent(FindParkingIntent.OnMapCameraMove(event.center, event.zoom))
                    }
                    is SpotMapEvent.OnCameraIdle -> {
                        onIntent(FindParkingIntent.OnMapCameraIdle(event.bounds, event.zoom))
                        // Show search area button after delay
                        scope.launch {
                            delay(1000)
                            showSearchAreaButton = true
                        }
                    }
                    is SpotMapEvent.OnMarkerClick -> {
                        onIntent(FindParkingIntent.OnMarkerClick(event.id))
                    }
                    is SpotMapEvent.OnMyLocationClick -> {
                        onIntent(FindParkingIntent.OnMyLocationClick)
                    }
                    else -> {}
                }
            }
        )
        
        // Floating Search Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            onClick = { onIntent(FindParkingIntent.OnSearchBarClick) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (state.destinationLocation != null) {
                        "Destination set"
                    } else {
                        "Where are you going?"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (state.destinationLocation != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        
        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search this area button
            AnimatedVisibility(
                visible = showSearchAreaButton && state.searchAreaButtonVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FilledTonalButton(
                    onClick = {
                        onIntent(FindParkingIntent.OnSearchThisAreaClick)
                        showSearchAreaButton = false
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search this area")
                }
            }
            
            // Duration chip
            AssistChip(
                onClick = { /* TODO: Show duration picker */ },
                label = {
                    Text("Now • ${state.duration.label}")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        
        // FAB: My Location
        FloatingActionButton(
            onClick = { onIntent(FindParkingIntent.OnMyLocationClick) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "My Location",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Loading overlay
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Error snackbar
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                action = {
                    TextButton(onClick = { onIntent(FindParkingIntent.OnDismissError) }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

private fun buildMarkers(
    spots: List<ParkingSpot>,
    destination: LatLng?,
    selectedId: String?
): List<SpotMarker> {
    val markers = mutableListOf<SpotMarker>()
    
    // Add parking spot markers
    spots.forEach { spot ->
        markers.add(
            SpotMarker(
                id = spot.id,
                position = spot.location,
                type = if (spot.isTopSpott) MarkerType.TOP_SPOTT else MarkerType.PRICE,
                label = "$${spot.pricePerHour}/h",
                draggable = false
            )
        )
    }
    
    // Add destination marker if set
    destination?.let {
        markers.add(
            SpotMarker(
                id = "destination",
                position = it,
                type = MarkerType.DESTINATION,
                label = null,
                draggable = true
            )
        )
    }
    
    return markers
}

// Preview
@Composable
fun FindParkingScreenPreview() {
    val sampleSpots = listOf(
        ParkingSpot(
            id = "1",
            location = LatLng(37.7749, -122.4194),
            pricePerHour = 7f,
            rating = 4.5f
        ),
        ParkingSpot(
            id = "2",
            location = LatLng(37.7759, -122.4184),
            pricePerHour = 5f,
            isTopSpott = true,
            rating = 4.8f
        ),
        ParkingSpot(
            id = "3",
            location = LatLng(37.7739, -122.4204),
            pricePerHour = 8f,
            rating = 4.2f
        )
    )
    
    MaterialTheme {
        FindParkingScreen(
            state = FindParkingState(
                parkingSpots = sampleSpots,
                destinationLocation = LatLng(37.7755, -122.4186)
            ),
            onIntent = {}
        )
    }
}