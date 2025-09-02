package com.spott.feature.parking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spott.core.geocoding.GeocodingException
import com.spott.core.geocoding.GeocodingService
import com.spott.core.geocoding.PlaceSuggestion
import com.spott.core.analytics.Analytics
import com.spott.map.LatLng
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * ViewModel for SearchDestinationScreen with proper debouncing and validation.
 * 
 * Implements all requirements from the engineering guide:
 * - 300ms debounce delay
 * - Minimum 3-character input validation
 * - Request cancellation for in-flight requests
 * - Proper error handling and UI states
 */
class SearchDestinationViewModel(
    private val geocodingService: GeocodingService,
    private val analytics: Analytics
) : ViewModel() {
    
    private val _state = MutableStateFlow(SearchDestinationState())
    val state: StateFlow<SearchDestinationState> = _state.asStateFlow()
    
    private val _effects = MutableSharedFlow<SearchDestinationEffect>()
    val effects: SharedFlow<SearchDestinationEffect> = _effects.asSharedFlow()
    
    // Debounce configuration
    private val searchQueryFlow = MutableStateFlow("")
    private var searchJob: Job? = null
    
    // Constants from engineering guide
    companion object {
        private const val DEBOUNCE_DELAY_MS = 300L
        private const val MIN_QUERY_LENGTH = 3
        private const val DEFAULT_COUNTRY = "AU" // Australia for initial deployment
    }
    
    init {
        // Set up debounced search with proper cancellation
        searchQueryFlow
            .debounce(DEBOUNCE_DELAY_MS)
            .filter { it.length >= MIN_QUERY_LENGTH }
            .distinctUntilChanged()
            .onEach { query ->
                performSearch(query)
            }
            .launchIn(viewModelScope)
    }
    
    fun handleIntent(intent: SearchDestinationIntent) {
        when (intent) {
            is SearchDestinationIntent.OnAppear -> handleOnAppear()
            is SearchDestinationIntent.OnSearchQueryChange -> handleSearchQueryChange(intent.query)
            is SearchDestinationIntent.OnPredictionSelect -> handlePredictionSelect(intent.prediction)
            is SearchDestinationIntent.OnUseCurrentLocation -> handleUseCurrentLocation()
            is SearchDestinationIntent.OnClearSearch -> handleClearSearch()
            is SearchDestinationIntent.OnNavigateBack -> handleNavigateBack()
        }
    }
    
    private fun handleOnAppear() {
        // Load recent searches from local storage (if implemented)
        loadRecentSearches()
    }
    
    private fun handleSearchQueryChange(query: String) {
        // Update UI immediately
        _state.update { it.copy(searchQuery = query) }
        
        // Cancel any in-flight search request
        searchJob?.cancel()
        
        // Handle query length validation
        when {
            query.isEmpty() -> {
                // Clear predictions and show recent searches
                _state.update { current ->
                    current.copy(
                        predictions = emptyList(),
                        isLoading = false,
                        error = null
                    )
                }
            }
            query.length < MIN_QUERY_LENGTH -> {
                // Show minimum length message
                _state.update { current ->
                    current.copy(
                        predictions = emptyList(),
                        isLoading = false,
                        error = null,
                        showMinLengthHint = true
                    )
                }
            }
            else -> {
                // Valid query length - trigger debounced search
                _state.update { it.copy(showMinLengthHint = false) }
                searchQueryFlow.value = query
            }
        }
    }
    
    private fun performSearch(query: String) {
        // Cancel previous search if still running
        searchJob?.cancel()
        
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Get user location or map center for proximity bias
                val proximity = _state.value.userLocation ?: LatLng(-33.8688, 151.2093) // Sydney default
                
                val suggestions = geocodingService.autocomplete(
                    query = query,
                    country = DEFAULT_COUNTRY,
                    proximity = proximity,
                    language = "en"
                )
                
                // Check if this job was cancelled while waiting
                if (isActive) {
                    _state.update { current ->
                        current.copy(
                            predictions = suggestions.map { it.toUiModel() },
                            isLoading = false,
                            error = if (suggestions.isEmpty()) "No results found" else null
                        )
                    }
                    
                    // Track analytics
                    trackSearchPerformed(query, suggestions.size)
                }
                
            } catch (e: CancellationException) {
                // Job was cancelled, this is expected behavior
                throw e
            } catch (e: GeocodingException) {
                // Geocoding-specific error
                if (isActive) {
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            error = "Search failed: ${e.message}",
                            predictions = emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                // Network or other error
                if (isActive) {
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            error = "Network error. Please check your connection.",
                            predictions = emptyList()
                        )
                    }
                }
            }
        }
    }
    
    private fun handlePredictionSelect(prediction: PlacePrediction) {
        viewModelScope.launch {
            // Save to recent searches
            saveToRecentSearches(prediction)
            
            // Navigate to map with selected location
            _effects.emit(
                SearchDestinationEffect.NavigateToMap(
                    destination = LatLng(
                        prediction.location?.lat ?: 0.0,
                        prediction.location?.lng ?: 0.0
                    )
                )
            )
            
            // Track analytics
            trackSuggestionSelected(prediction)
        }
    }
    
    private fun handleUseCurrentLocation() {
        viewModelScope.launch {
            val currentLocation = _state.value.userLocation
            if (currentLocation != null) {
                _effects.emit(SearchDestinationEffect.NavigateToMap(currentLocation))
            } else {
                // Request location permission if not available
                _state.update { it.copy(error = "Location not available") }
            }
        }
    }

    // External proximity update (e.g., from current map camera center)
    fun updateProximity(proximity: LatLng?) {
        _state.update { it.copy(userLocation = proximity) }
    }
    
    private fun handleClearSearch() {
        searchJob?.cancel()
        _state.update { current ->
            current.copy(
                searchQuery = "",
                predictions = emptyList(),
                isLoading = false,
                error = null,
                showMinLengthHint = false
            )
        }
    }
    
    private fun handleNavigateBack() {
        viewModelScope.launch {
            searchJob?.cancel()
            _effects.emit(SearchDestinationEffect.NavigateBack)
        }
    }
    
    private fun loadRecentSearches() {
        // TODO: Load from local storage
        val mockRecentSearches = listOf(
            PlacePrediction(
                placeId = "recent1",
                primaryText = "Melbourne Airport",
                secondaryText = "Melbourne VIC, Australia",
                location = LatLng(-37.6733, 144.8433)
            ),
            PlacePrediction(
                placeId = "recent2",
                primaryText = "Flinders Street Station",
                secondaryText = "Melbourne VIC, Australia",
                location = LatLng(-37.8183, 144.9671)
            )
        )
        _state.update { it.copy(recentSearches = mockRecentSearches) }
    }
    
    private fun saveToRecentSearches(prediction: PlacePrediction) {
        // TODO: Persist to local storage
        _state.update { current ->
            val updated = current.recentSearches.toMutableList()
            updated.removeAll { it.placeId == prediction.placeId }
            updated.add(0, prediction)
            current.copy(recentSearches = updated.take(10)) // Keep last 10
        }
    }
    
    // Analytics tracking
    private fun trackSearchPerformed(query: String, resultCount: Int) {
        analytics.track(
            event = "search_started",
            properties = mapOf(
                "query_length" to query.length,
                "result_count" to resultCount
            )
        )
    }
    
    private fun trackSuggestionSelected(prediction: PlacePrediction) {
        analytics.track(
            event = "suggestion_selected",
            properties = mapOf(
                "place_id" to prediction.placeId,
                "primary_text" to prediction.primaryText
            )
        )
    }
    
    override fun onCleared() {
        searchJob?.cancel()
        super.onCleared()
    }
}

// Extension to convert domain model to UI model
private fun PlaceSuggestion.toUiModel(): PlacePrediction {
    return PlacePrediction(
        placeId = id,
        primaryText = placeName.substringBefore(",").trim(),
        secondaryText = placeName.substringAfter(",", "").trim().ifEmpty {
            context?.let { "${it.locality ?: ""} ${it.region ?: ""} ${it.country ?: ""}".trim() } ?: ""
        },
        location = coordinates
    )
}

// Updated state to include new fields
data class SearchDestinationState(
    val searchQuery: String = "",
    val predictions: List<PlacePrediction> = emptyList(),
    val recentSearches: List<PlacePrediction> = emptyList(),
    val isLoading: Boolean = false,
    val userLocation: LatLng? = null,
    val error: String? = null,
    val showMinLengthHint: Boolean = false
)
