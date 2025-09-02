package com.spott.feature.parking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spott.core.analytics.Analytics
import com.spott.core.analytics.NoopAnalytics
import com.spott.map.LatLng
import com.spott.map.LatLngBounds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FindParkingViewModel(
    private val analytics: Analytics = NoopAnalytics
) : ViewModel() {
    
    private val _state = MutableStateFlow(FindParkingState())
    val state: StateFlow<FindParkingState> = _state.asStateFlow()
    
    private val _effects = MutableSharedFlow<FindParkingEffect>()
    val effects: SharedFlow<FindParkingEffect> = _effects.asSharedFlow()
    
    // Track last search bounds to detect significant camera moves
    private var lastSearchBounds: LatLngBounds? = null
    private var lastSearchTime = 0L
    
    fun handleIntent(intent: FindParkingIntent) {
        when (intent) {
            is FindParkingIntent.OnAppear -> handleOnAppear()
            is FindParkingIntent.OnSearchBarClick -> handleSearchBarClick()
            is FindParkingIntent.OnMyLocationClick -> handleMyLocationClick()
            is FindParkingIntent.OnSearchThisAreaClick -> handleSearchThisArea()
            is FindParkingIntent.OnMarkerClick -> handleMarkerClick(intent.markerId)
            is FindParkingIntent.OnMapCameraMove -> handleMapCameraMove(intent.center, intent.zoom)
            is FindParkingIntent.OnMapCameraIdle -> handleMapCameraIdle(intent.bounds, intent.zoom)
            is FindParkingIntent.OnDurationChange -> handleDurationChange(intent.duration)
            is FindParkingIntent.OnDestinationSet -> handleDestinationSet(intent.location)
            is FindParkingIntent.OnDismissError -> handleDismissError()
        }
    }
    
    private fun handleOnAppear() {
        viewModelScope.launch {
            // Request location permission if not granted
            _effects.emit(FindParkingEffect.RequestLocationPermission)
            
            // Load initial parking spots for default area
            loadParkingSpots(_state.value.camera.target)
        }
    }
    
    private fun handleSearchBarClick() {
        viewModelScope.launch {
            _effects.emit(
                FindParkingEffect.NavigateToSearch(_state.value.userLocation)
            )
        }
    }
    
    private fun handleMyLocationClick() {
        viewModelScope.launch {
            // Simulate getting user location
            // In real app, this would use platform location services
            val userLocation = LatLng(37.7749, -122.4194) // Mock location
            
            _state.update { current ->
                current.copy(
                    userLocation = userLocation,
                    camera = current.camera.copy(
                        target = userLocation,
                        zoom = 15f
                    ),
                    isMyLocationEnabled = true
                )
            }
            
            // Load spots around user location
            loadParkingSpots(userLocation)
        }
    }
    
    private fun handleSearchThisArea() {
        viewModelScope.launch {
            val currentBounds = lastSearchBounds
            if (currentBounds != null) {
                _state.update { it.copy(isLoading = true) }
                
                // Simulate API call to fetch spots in visible area
                delay(500)
                
                val spots = generateMockSpots(currentBounds)
                
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        parkingSpots = spots,
                        searchAreaButtonVisible = false
                    )
                }
                
                lastSearchTime = System.currentTimeMillis()
            }
        }
    }
    
    private fun handleMarkerClick(markerId: String) {
        viewModelScope.launch {
            if (markerId == "destination") {
                // Handle destination marker click
                return@launch
            }
            
            _state.update { it.copy(selectedSpotId = markerId) }
            _effects.emit(FindParkingEffect.ShowListingDetails(markerId))
        }
    }
    
    private fun handleMapCameraMove(center: LatLng, zoom: Float) {
        // Hide search area button while moving
        _state.update { it.copy(searchAreaButtonVisible = false) }
    }
    
    private fun handleMapCameraIdle(bounds: LatLngBounds, zoom: Float) {
        viewModelScope.launch {
            val shouldShowSearchButton = shouldShowSearchAreaButton(bounds)
            
            if (shouldShowSearchButton) {
                delay(1000) // Wait before showing button
                _state.update { it.copy(searchAreaButtonVisible = true) }
            }
            
            lastSearchBounds = bounds
        }
    }
    
    private fun handleDurationChange(duration: ParkingDuration) {
        _state.update { it.copy(duration = duration) }
    }
    
    private fun handleDestinationSet(location: LatLng) {
        viewModelScope.launch {
            _state.update { current ->
                current.copy(
                    destinationLocation = location,
                    camera = current.camera.copy(
                        target = location,
                        zoom = 15f
                    )
                )
            }
            
            // Load parking spots around destination
            loadParkingSpots(location)

            // Notify user
            _effects.emit(FindParkingEffect.ShowToast("Destination set"))

            // Track analytics
            analytics.track(
                event = "destination_set",
                properties = mapOf(
                    "lat" to location.lat,
                    "lng" to location.lng
                )
            )
        }
    }
    
    private fun handleDismissError() {
        _state.update { it.copy(error = null) }
    }
    
    private suspend fun loadParkingSpots(center: LatLng?) {
        if (center == null) return
        
        _state.update { it.copy(isLoading = true) }
        
        try {
            // Simulate API call
            delay(800)
            
            val spots = generateMockSpotsAroundPoint(center)
            
            _state.update { current ->
                current.copy(
                    isLoading = false,
                    parkingSpots = spots
                )
            }
        } catch (e: Exception) {
            _state.update { current ->
                current.copy(
                    isLoading = false,
                    error = "Failed to load parking spots"
                )
            }
        }
    }
    
    private fun shouldShowSearchAreaButton(bounds: LatLngBounds): Boolean {
        val lastBounds = lastSearchBounds ?: return false
        val timeSinceLastSearch = System.currentTimeMillis() - lastSearchTime
        
        // Show button if camera moved significantly and enough time passed
        if (timeSinceLastSearch < 5000) return false
        
        // Check if bounds changed significantly (simplified check)
        val latDiff = kotlin.math.abs(bounds.northEast.lat - lastBounds.northEast.lat)
        val lngDiff = kotlin.math.abs(bounds.northEast.lng - lastBounds.northEast.lng)
        
        return latDiff > 0.005 || lngDiff > 0.005
    }
    
    private fun generateMockSpotsAroundPoint(center: LatLng): List<ParkingSpot> {
        return listOf(
            ParkingSpot(
                id = "spot1",
                location = LatLng(center.lat + 0.001, center.lng + 0.001),
                pricePerHour = 7f,
                rating = 4.5f,
                distance = 150f
            ),
            ParkingSpot(
                id = "spot2",
                location = LatLng(center.lat - 0.001, center.lng + 0.002),
                pricePerHour = 5f,
                isTopSpott = true,
                rating = 4.8f,
                distance = 200f
            ),
            ParkingSpot(
                id = "spot3",
                location = LatLng(center.lat + 0.002, center.lng - 0.001),
                pricePerHour = 8f,
                rating = 4.2f,
                distance = 300f
            ),
            ParkingSpot(
                id = "spot4",
                location = LatLng(center.lat - 0.002, center.lng - 0.002),
                pricePerHour = 6f,
                rating = 4.0f,
                distance = 400f
            ),
            ParkingSpot(
                id = "spot5",
                location = LatLng(center.lat + 0.003, center.lng),
                pricePerHour = 9f,
                isTopSpott = true,
                rating = 4.9f,
                distance = 250f
            )
        )
    }
    
    private fun generateMockSpots(bounds: LatLngBounds): List<ParkingSpot> {
        val centerLat = (bounds.northEast.lat + bounds.southWest.lat) / 2
        val centerLng = (bounds.northEast.lng + bounds.southWest.lng) / 2
        return generateMockSpotsAroundPoint(LatLng(centerLat, centerLng))
    }
}
