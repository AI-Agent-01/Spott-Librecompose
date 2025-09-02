package com.spott.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.spott.BuildConfig
import io.github.dellisd.spatialk.geojson.*
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.*
import org.maplibre.compose.expressions.*
import org.maplibre.compose.layers.*
import org.maplibre.compose.map.*
import org.maplibre.compose.sources.*
import org.maplibre.compose.settings.*
import org.maplibre.compose.map.OrnamentOptions
import kotlin.time.Duration.Companion.milliseconds
import org.maplibre.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding

/**
 * Android actual implementation of SpotMapView using MapLibre Compose
 */
@Composable
actual fun SpotMapView(
    modifier: Modifier,
    props: SpotMapProps,
    onEvent: (SpotMapEvent) -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // Convert Spott camera model to MapLibre camera state
    val cameraState = rememberCameraState {
        position = props.camera.toCameraPosition()
    }
    
    // Track camera movement
    var lastCameraPosition by remember { mutableStateOf(cameraState.position) }
    
    // Update camera when props change
    LaunchedEffect(props.camera) {
        val newPosition = props.camera.toCameraPosition()
        if (newPosition != cameraState.position) {
            if (props.camera.bounds != null) {
                // Animate to bounds
                cameraState.animateTo(
                    boundingBox = props.camera.bounds.toBoundingBox(),
                    bearing = props.camera.bearing,
                    tilt = props.camera.tilt,
                    duration = 500.milliseconds
                )
            } else if (props.camera.target != null) {
                // Animate to position
                cameraState.animateTo(
                    finalPosition = newPosition,
                    duration = 300.milliseconds
                )
            }
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Main map surface
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri(
                MapStyles.getDefaultStyleUrl(BuildConfig.MAPTILER_API_KEY)
            ),
            cameraState = cameraState,
            onMapClick = { point, features ->
                onEvent(SpotMapEvent.OnMapClick(point.toLatLng()))
                ClickResult.Pass
            },
            onMapLongClick = { point, features ->
                onEvent(SpotMapEvent.OnLongPress(point.toLatLng()))
                ClickResult.Pass
            },
            onMapLoadFinished = {
                val bounds = cameraState.projection?.visibleBounds
                onEvent(SpotMapEvent.OnMapReady(bounds?.toLatLngBounds()))
            },
            options = MapOptions(
                gestureOptions = GestureOptions(
                    rotateEnabled = props.gestureSettings.rotateEnabled,
                    pitchEnabled = props.gestureSettings.tiltEnabled,
                    scrollEnabled = props.gestureSettings.scrollEnabled,
                    zoomEnabled = props.gestureSettings.zoomEnabled,
                    doubleTapToZoomEnabled = props.gestureSettings.doubleTapToZoomEnabled
                ),
                ornamentOptions = OrnamentOptions(
                    isLogoEnabled = false,
                    isAttributionEnabled = false,
                    isCompassEnabled = false,
                    isScaleBarEnabled = false
                )
            )
        ) {
            // Render markers
            if (props.markers.isNotEmpty()) {
                MarkerLayers(
                    markers = props.markers,
                    clustersEnabled = props.clustersEnabled,
                    onMarkerClick = { markerId ->
                        onEvent(SpotMapEvent.OnMarkerClick(markerId))
                    }
                )
            }
            
            // Render polylines
            if (props.polylines.isNotEmpty()) {
                PolylineLayers(polylines = props.polylines)
            }
        }
        
        
        // Attribution overlay - REQUIRED for MapTiler compliance
        AttributionOverlay(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        )
        
        // MapLibre logo and scale bar
        DisappearingScaleBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            cameraState = cameraState
        )
        
        // Compass button
        DisappearingCompassButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            cameraState = cameraState
        )
        
        // Track camera idle events
        LaunchedEffect(cameraState.isCameraMoving) {
            if (!cameraState.isCameraMoving && cameraState.position != lastCameraPosition) {
                lastCameraPosition = cameraState.position
                val bounds = cameraState.projection?.visibleBounds
                bounds?.let {
                    onEvent(SpotMapEvent.OnCameraIdle(
                        it.toLatLngBounds(),
                        cameraState.position.zoom.toFloat()
                    ))
                }
                
                // Also send camera move event
                val center = cameraState.position.target
                onEvent(SpotMapEvent.OnCameraMove(
                    center.toLatLng(),
                    cameraState.position.zoom.toFloat()
                ))
            }
        }
    }
}

@Composable
@MaplibreComposable
private fun MarkerLayers(
    markers: List<SpotMarker>,
    clustersEnabled: Boolean,
    onMarkerClick: (String) -> Unit
) {
    // Group markers by type for different layers
    val markersByType = markers.groupBy { it.type }
    
    // Price markers (regular parking spots)
    markersByType[MarkerType.PRICE]?.let { priceMarkers ->
        val priceSource = rememberGeoJsonSource(id = "price-markers") {
            data = GeoJsonData.FeatureCollection(
                features = priceMarkers.map { marker ->
                    Feature(
                        geometry = Point(marker.position.toMapLibreLatLng()),
                        properties = mapOf(
                            "id" to marker.id,
                            "label" to (marker.label ?: ""),
                            "price" to (marker.label ?: "$0")
                        )
                    )
                }
            )
            clustersEnabled = clustersEnabled
        }
        
        // Clustered price markers
        if (clustersEnabled) {
            CircleLayer(
                id = "price-clusters",
                source = priceSource,
                filter = has("point_count"),
                circleRadius = const(20.dp),
                circleColor = const(Color(0xFF2196F3))
            )
            
            SymbolLayer(
                id = "price-cluster-labels",
                source = priceSource,
                filter = has("point_count"),
                textField = format(get("point_count").asString()),
                textSize = const(1.2f.em),
                textColor = const(Color.White)
            )
        }
        
        // Individual price markers
        CircleLayer(
            id = "price-markers-bg",
            source = priceSource,
            filter = not(has("point_count")),
            circleRadius = const(16.dp),
            circleColor = const(Color.White),
            circleStrokeColor = const(Color(0xFF2196F3)),
            circleStrokeWidth = const(2.dp),
            onClick = { features ->
                features.firstOrNull()?.let { feature ->
                    val id = feature.properties?.get("id") as? String
                    id?.let { onMarkerClick(it) }
                }
                ClickResult.Consume
            }
        )
        
        SymbolLayer(
            id = "price-labels",
            source = priceSource,
            filter = not(has("point_count")),
            textField = format(get("price").asString()),
            textSize = const(0.8f.em),
            textColor = const(Color(0xFF2196F3))
        )
    }
    
    // Destination marker
    markersByType[MarkerType.DESTINATION]?.firstOrNull()?.let { destMarker ->
        val destSource = rememberGeoJsonSource(id = "destination-marker") {
            data = GeoJsonData.Feature(
                geometry = Point(destMarker.position.toMapLibreLatLng()),
                properties = mapOf("id" to destMarker.id)
            )
        }
        
        CircleLayer(
            id = "destination-marker",
            source = destSource,
            circleRadius = const(12.dp),
            circleColor = const(Color(0xFFFF5722)), // Orange
            onClick = { _ ->
                onMarkerClick(destMarker.id)
                ClickResult.Consume
            }
        )
    }
    
    // Top Spott markers (premium spots)
    markersByType[MarkerType.TOP_SPOTT]?.let { topSpottMarkers ->
        val topSpottSource = rememberGeoJsonSource(id = "top-spott-markers") {
            data = GeoJsonData.FeatureCollection(
                features = topSpottMarkers.map { marker ->
                    Feature(
                        geometry = Point(marker.position.toMapLibreLatLng()),
                        properties = mapOf(
                            "id" to marker.id,
                            "label" to (marker.label ?: "")
                        )
                    )
                }
            )
        }
        
        CircleLayer(
            id = "top-spott-markers",
            source = topSpottSource,
            circleRadius = const(20.dp),
            circleColor = const(Color(0xFFFFD700)), // Gold
            circleStrokeColor = const(Color(0xFFFF6B00)),
            circleStrokeWidth = const(3.dp),
            onClick = { features ->
                features.firstOrNull()?.let { feature ->
                    val id = feature.properties?.get("id") as? String
                    id?.let { onMarkerClick(it) }
                }
                ClickResult.Consume
            }
        )
    }
    
    // Parked location marker
    markersByType[MarkerType.PARKED]?.firstOrNull()?.let { parkedMarker ->
        val parkedSource = rememberGeoJsonSource(id = "parked-marker") {
            data = GeoJsonData.Feature(
                geometry = Point(parkedMarker.position.toMapLibreLatLng()),
                properties = mapOf("id" to parkedMarker.id)
            )
        }
        
        CircleLayer(
            id = "parked-marker",
            source = parkedSource,
            circleRadius = const(10.dp),
            circleColor = const(Color(0xFF4CAF50)), // Green
            onClick = { _ ->
                onMarkerClick(parkedMarker.id)
                ClickResult.Consume
            }
        )
    }
}

@Composable
@MaplibreComposable
private fun PolylineLayers(polylines: List<MapPolyline>) {
    polylines.forEach { polyline ->
        val source = rememberGeoJsonSource(id = "polyline-${polyline.id}") {
            data = GeoJsonData.Feature(
                geometry = LineString(polyline.points.map { it.toMapLibreLatLng() })
            )
        }
        
        LineLayer(
            id = "polyline-layer-${polyline.id}",
            source = source,
            lineColor = const(Color(polyline.color.toInt())),
            lineWidth = const(polyline.widthDp),
            lineDashArray = if (polyline.style == PolyStyle.Walking) {
                listOf(2f, 2f) // Dashed for walking
            } else {
                null // Solid for driving
            }
        )
    }
}

// Extension functions for conversions

private fun CameraModel.toCameraPosition(): CameraPosition {
    return CameraPosition(
        target = target?.toMapLibreLatLng() ?: org.maplibre.compose.camera.LatLng(0.0, 0.0),
        zoom = zoom?.toDouble() ?: 12.0,
        bearing = bearing,
        tilt = tilt
    )
}

private fun LatLng.toMapLibreLatLng(): org.maplibre.compose.camera.LatLng {
    return org.maplibre.compose.camera.LatLng(lat, lng)
}

private fun org.maplibre.compose.camera.LatLng.toLatLng(): LatLng {
    return LatLng(latitude, longitude)
}

private fun LatLngBounds.toBoundingBox(): BoundingBox {
    return BoundingBox(
        southWest.toMapLibreLatLng(),
        northEast.toMapLibreLatLng()
    )
}

private fun BoundingBox.toLatLngBounds(): LatLngBounds {
    return LatLngBounds(
        southWest = southWest.toLatLng(),
        northEast = northEast.toLatLng()
    )
}
