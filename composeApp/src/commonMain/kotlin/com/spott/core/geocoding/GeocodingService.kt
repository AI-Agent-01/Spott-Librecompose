package com.spott.core.geocoding

import com.spott.map.LatLng
import com.spott.map.LatLngBounds

/**
 * Common interface for geocoding services across platforms.
 * Following KMP architecture for platform-specific implementations.
 */
interface GeocodingService {
    /**
     * Performs autocomplete search for places.
     * 
     * @param query The search query (minimum 3 characters)
     * @param country ISO country code to restrict results (e.g., "AU" for Australia)
     * @param proximity Optional location to bias results towards
     * @param language Language code for results (e.g., "en")
     * @return List of place suggestions
     * @throws GeocodingException if the request fails
     */
    suspend fun autocomplete(
        query: String,
        country: String? = null,
        proximity: LatLng? = null,
        language: String = "en"
    ): List<PlaceSuggestion>
    
    /**
     * Performs reverse geocoding to get place information from coordinates.
     * 
     * @param location The coordinates to reverse geocode
     * @return Place information or null if not found
     */
    suspend fun reverseGeocode(
        location: LatLng
    ): PlaceSuggestion?
}

/**
 * Represents a place suggestion from the geocoding service.
 * Maps to MapTiler's GeoJSON Feature response.
 */
data class PlaceSuggestion(
    val id: String,
    val placeName: String,
    val coordinates: LatLng,
    val placeType: List<String> = emptyList(),
    val bbox: LatLngBounds? = null,
    val address: String? = null,
    val context: PlaceContext? = null
)

/**
 * Additional context about a place (country, region, etc.)
 */
data class PlaceContext(
    val country: String? = null,
    val region: String? = null,
    val postcode: String? = null,
    val locality: String? = null
)

/**
 * Exception thrown when geocoding operations fail
 */
class GeocodingException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Configuration for geocoding requests
 */
data class GeocodingConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.maptiler.com",
    val autocompleteLimit: Int = 8,
    val minQueryLength: Int = 3,
    val debounceDelayMs: Long = 300L,
    val types: List<String> = listOf("poi", "address", "street", "place")
)