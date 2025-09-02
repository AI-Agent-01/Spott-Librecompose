package com.spott.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Core map surface for Spott app
 * Platform-specific implementations use MapLibre on Android, MapKit on iOS
 */
@Composable
expect fun SpotMapView(
    modifier: Modifier = Modifier,
    props: SpotMapProps,
    onEvent: (SpotMapEvent) -> Unit
)

data class SpotMapProps(
    val camera: CameraModel,
    val myLocationEnabled: Boolean = false,
    val markers: List<SpotMarker> = emptyList(),
    val polylines: List<MapPolyline> = emptyList(),
    val clustersEnabled: Boolean = true,
    val gestureSettings: GestureSettings = GestureSettings()
)

data class CameraModel(
    val target: LatLng? = null,
    val bounds: LatLngBounds? = null,
    val paddingDp: Int = 48,
    val zoom: Float? = null,
    val bearing: Double = 0.0,
    val tilt: Double = 0.0
)

data class SpotMarker(
    val id: String,
    val position: LatLng,
    val type: MarkerType,
    val label: String? = null,
    val draggable: Boolean = false
)

enum class MarkerType { 
    PRICE,        // Regular parking spot with price
    DESTINATION,  // User's destination marker
    PARKED,       // Where user parked
    TOP_SPOTT     // Premium/featured spot
}

data class MapPolyline(
    val id: String,
    val points: List<LatLng>,
    val style: PolyStyle = PolyStyle.Driving,
    val widthDp: Dp = 4.dp,
    val color: Long = 0xFF2196F3 // Material Blue
)

enum class PolyStyle { 
    Driving,  // Solid line for driving route
    Walking   // Dashed line for walking route
}

data class LatLng(
    val lat: Double, 
    val lng: Double
)

data class LatLngBounds(
    val southWest: LatLng, 
    val northEast: LatLng
)

data class GestureSettings(
    val rotateEnabled: Boolean = true,
    val tiltEnabled: Boolean = true,
    val scrollEnabled: Boolean = true,
    val zoomEnabled: Boolean = true,
    val doubleTapToZoomEnabled: Boolean = true
)

sealed interface SpotMapEvent {
    data class OnMapReady(val visibleRegion: LatLngBounds?) : SpotMapEvent
    data class OnCameraIdle(val bounds: LatLngBounds, val zoom: Float) : SpotMapEvent
    data class OnCameraMove(val center: LatLng, val zoom: Float) : SpotMapEvent
    data class OnMarkerClick(val id: String) : SpotMapEvent
    data class OnMapClick(val position: LatLng) : SpotMapEvent
    data class OnLongPress(val position: LatLng) : SpotMapEvent
    data class OnMarkerDragEnd(val id: String, val position: LatLng) : SpotMapEvent
    data class OnMyLocationClick(val position: LatLng) : SpotMapEvent
}