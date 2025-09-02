package com.spott.core.geocoding

import com.spott.BuildConfig
import com.spott.map.LatLng
import com.spott.map.LatLngBounds
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * MapTiler implementation of the GeocodingService.
 * 
 * Implements all requirements from the engineering guide:
 * - Proper parameter configuration for parking app
 * - Request debouncing (handled by ViewModel)
 * - Error handling and resilience
 * - Correct endpoint and response parsing
 */
class MapTilerGeocodingService(
    private val config: GeocodingConfig = GeocodingConfig(
        apiKey = BuildConfig.MAPTILER_API_KEY
    )
) : GeocodingService {
    
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()
        }
    }
    
    override suspend fun autocomplete(
        query: String,
        country: String?,
        proximity: LatLng?,
        language: String
    ): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        // Validate minimum query length
        if (query.length < config.minQueryLength) {
            return@withContext emptyList()
        }
        
        try {
            val response: HttpResponse = httpClient.get("${config.baseUrl}/geocoding/${query.encodeURLPath()}.json") {
                url {
                    parameters.append("key", config.apiKey)
                    parameters.append("autocomplete", "true")
                    parameters.append("limit", config.autocompleteLimit.toString())
                    
                    // Filter to relevant types for parking app
                    parameters.append("types", config.types.joinToString(","))
                    
                    // Country restriction if provided
                    country?.let {
                        parameters.append("country", it)
                    }
                    
                    // Proximity bias for relevance
                    proximity?.let {
                        parameters.append("proximity", "${it.lng},${it.lat}")
                    }
                    
                    // Language for localized results
                    parameters.append("language", language)
                }
            }
            
            if (response.status != HttpStatusCode.OK) {
                throw GeocodingException(
                    "Geocoding request failed with status: ${response.status}"
                )
            }
            
            val featureCollection = response.body<MapTilerFeatureCollection>()
            return@withContext featureCollection.features.map { it.toPlaceSuggestion() }
            
        } catch (e: Exception) {
            when (e) {
                is GeocodingException -> throw e
                else -> throw GeocodingException(
                    "Failed to perform geocoding: ${e.message}",
                    e
                )
            }
        }
    }
    
    override suspend fun reverseGeocode(
        location: LatLng
    ): PlaceSuggestion? = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = httpClient.get("${config.baseUrl}/geocoding/${location.lng},${location.lat}.json") {
                url {
                    parameters.append("key", config.apiKey)
                    parameters.append("limit", "1")
                }
            }
            
            if (response.status != HttpStatusCode.OK) {
                return@withContext null
            }
            
            val featureCollection = response.body<MapTilerFeatureCollection>()
            return@withContext featureCollection.features.firstOrNull()?.toPlaceSuggestion()
            
        } catch (e: Exception) {
            // Reverse geocoding failures are non-critical
            return@withContext null
        }
    }
    
    fun close() {
        httpClient.close()
    }
}

// MapTiler API Response Models

@Serializable
private data class MapTilerFeatureCollection(
    @SerialName("type") val type: String = "FeatureCollection",
    @SerialName("features") val features: List<MapTilerFeature> = emptyList()
)

@Serializable
private data class MapTilerFeature(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String = "Feature",
    @SerialName("place_name") val placeName: String? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("place_type") val placeType: List<String>? = null,
    @SerialName("geometry") val geometry: MapTilerGeometry,
    @SerialName("bbox") val bbox: List<Double>? = null,
    @SerialName("properties") val properties: MapTilerProperties? = null,
    @SerialName("context") val context: List<MapTilerContextItem>? = null
) {
    fun toPlaceSuggestion(): PlaceSuggestion {
        val coordinates = LatLng(
            lat = geometry.coordinates[1],
            lng = geometry.coordinates[0]
        )
        
        val bounds = bbox?.let {
            if (it.size >= 4) {
                LatLngBounds(
                    southWest = LatLng(lat = it[1], lng = it[0]),
                    northEast = LatLng(lat = it[3], lng = it[2])
                )
            } else null
        }
        
        val placeContext = context?.let { items ->
            PlaceContext(
                country = items.find { it.id.startsWith("country") }?.text,
                region = items.find { it.id.startsWith("region") }?.text,
                postcode = items.find { it.id.startsWith("postcode") }?.text,
                locality = items.find { it.id.startsWith("place") || it.id.startsWith("locality") }?.text
            )
        }
        
        return PlaceSuggestion(
            id = id,
            placeName = placeName ?: text ?: "",
            coordinates = coordinates,
            placeType = placeType ?: emptyList(),
            bbox = bounds,
            address = properties?.address,
            context = placeContext
        )
    }
}

@Serializable
private data class MapTilerGeometry(
    @SerialName("type") val type: String,
    @SerialName("coordinates") val coordinates: List<Double>
)

@Serializable
private data class MapTilerProperties(
    @SerialName("address") val address: String? = null,
    @SerialName("category") val category: String? = null
)

@Serializable
private data class MapTilerContextItem(
    @SerialName("id") val id: String,
    @SerialName("text") val text: String
)