package com.spott.map

import android.content.Context
import com.spott.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.maplibre.android.offline.*
import org.maplibre.android.geometry.LatLngBounds as MapLibreLatLngBounds
import org.maplibre.android.maps.Style

/**
 * Manages offline map functionality for MapLibre.
 * 
 * Implements requirements from engineering guide:
 * - Ambient cache for recently viewed areas
 * - Offline packs for persistent storage
 * - Proper error handling for network failures
 */
class OfflineMapManager(
    private val context: Context
) {
    private val offlineManager = OfflineManager.getInstance(context)
    
    // Track download progress
    private val _downloadProgress = MutableStateFlow<OfflineDownloadProgress?>(null)
    val downloadProgress: Flow<OfflineDownloadProgress?> = _downloadProgress
    
    // Track offline regions
    private val _offlineRegions = MutableStateFlow<List<OfflineRegionInfo>>(emptyList())
    val offlineRegions: Flow<List<OfflineRegionInfo>> = _offlineRegions
    
    init {
        // Configure ambient cache size (100MB default)
        configureAmbientCache()
        
        // Load existing offline regions
        loadOfflineRegions()
    }
    
    /**
     * Configure ambient cache for temporary storage of recently viewed tiles.
     * This improves performance for frequently visited areas.
     */
    private fun configureAmbientCache() {
        // Set ambient cache size to 100MB
        val cacheSize = 100L * 1024L * 1024L // 100MB in bytes
        offlineManager.setMaximumAmbientCacheSize(
            cacheSize,
            object : OfflineManager.FileSourceCallback {
                override fun onSuccess() {
                    // Cache configured successfully
                }
                
                override fun onError(message: String) {
                    // Log error but don't fail - ambient cache is optional
                }
            }
        )
    }
    
    /**
     * Download an offline region for persistent storage.
     * 
     * @param regionName User-friendly name for the region
     * @param bounds Geographic bounds to download
     * @param minZoom Minimum zoom level (default 10)
     * @param maxZoom Maximum zoom level (default 16)
     * @return Region ID for tracking
     */
    suspend fun downloadOfflineRegion(
        regionName: String,
        bounds: LatLngBounds,
        minZoom: Float = 10f,
        maxZoom: Float = 16f
    ): String = withContext(Dispatchers.IO) {
        
        val styleUrl = MapStyles.getDefaultStyleUrl(BuildConfig.MAPTILER_API_KEY)
        
        // Create offline region definition
        val definition = OfflineTilePyramidRegionDefinition(
            styleUrl,
            MapLibreLatLngBounds.Builder()
                .include(org.maplibre.android.geometry.LatLng(bounds.southWest.lat, bounds.southWest.lng))
                .include(org.maplibre.android.geometry.LatLng(bounds.northEast.lat, bounds.northEast.lng))
                .build(),
            minZoom.toDouble(),
            maxZoom.toDouble(),
            context.resources.displayMetrics.density
        )
        
        // Metadata for the region
        val metadata = OfflineRegionMetadata(
            name = regionName,
            createdAt = System.currentTimeMillis()
        ).toByteArray()
        
        // Create the offline region
        val regionId = "${regionName}_${System.currentTimeMillis()}"
        
        offlineManager.createOfflineRegion(
            definition,
            metadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    // Start download
                    offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                    
                    // Monitor progress
                    offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                        override fun onStatusChanged(status: OfflineRegionStatus) {
                            val progress = OfflineDownloadProgress(
                                regionId = regionId,
                                completedTiles = status.completedResourceCount,
                                totalTiles = status.requiredResourceCount,
                                completedSize = status.completedResourceSize,
                                isComplete = status.isComplete
                            )
                            _downloadProgress.value = progress
                            
                            if (status.isComplete) {
                                loadOfflineRegions()
                            }
                        }
                        
                        override fun onError(error: OfflineRegionError) {
                            _downloadProgress.value = OfflineDownloadProgress(
                                regionId = regionId,
                                error = error.message,
                                isComplete = false
                            )
                        }
                        
                        override fun mapboxTileCountLimitExceeded(limit: Long) {
                            _downloadProgress.value = OfflineDownloadProgress(
                                regionId = regionId,
                                error = "Tile count limit exceeded: $limit",
                                isComplete = false
                            )
                        }
                    })
                }
                
                override fun onError(error: String) {
                    _downloadProgress.value = OfflineDownloadProgress(
                        regionId = regionId,
                        error = error,
                        isComplete = false
                    )
                }
            }
        )
        
        return regionId
    }
    
    /**
     * Load existing offline regions from storage.
     */
    private fun loadOfflineRegions() {
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>) {
                val regions = offlineRegions.mapNotNull { region ->
                    try {
                        val metadata = OfflineRegionMetadata.fromByteArray(region.metadata)
                        OfflineRegionInfo(
                            id = region.id,
                            name = metadata.name,
                            createdAt = metadata.createdAt
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                _offlineRegions.value = regions
            }
            
            override fun onError(error: String) {
                // Log error but don't crash
            }
        })
    }
    
    /**
     * Delete an offline region.
     */
    suspend fun deleteOfflineRegion(regionId: Long) = withContext(Dispatchers.IO) {
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>) {
                offlineRegions.find { it.id == regionId }?.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                    override fun onDelete() {
                        loadOfflineRegions()
                    }
                    
                    override fun onError(error: String) {
                        // Log error
                    }
                })
            }
            
            override fun onError(error: String) {
                // Log error
            }
        })
    }
    
    /**
     * Clear all cached data (both ambient and offline packs).
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        // Clear ambient cache
        offlineManager.clearAmbientCache(object : OfflineManager.FileSourceCallback {
            override fun onSuccess() {
                // Cache cleared
            }
            
            override fun onError(message: String) {
                // Log error
            }
        })
        
        // Delete all offline regions
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>) {
                offlineRegions.forEach { region ->
                    region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                        override fun onDelete() {
                            // Region deleted
                        }
                        
                        override fun onError(error: String) {
                            // Log error
                        }
                    })
                }
                _offlineRegions.value = emptyList()
            }
            
            override fun onError(error: String) {
                // Log error
            }
        })
    }
}

/**
 * Metadata for offline regions
 */
data class OfflineRegionMetadata(
    val name: String,
    val createdAt: Long
) {
    fun toByteArray(): ByteArray {
        return "$name|$createdAt".toByteArray()
    }
    
    companion object {
        fun fromByteArray(bytes: ByteArray): OfflineRegionMetadata {
            val str = String(bytes)
            val parts = str.split("|")
            return OfflineRegionMetadata(
                name = parts.getOrNull(0) ?: "Unknown",
                createdAt = parts.getOrNull(1)?.toLongOrNull() ?: 0L
            )
        }
    }
}

/**
 * Information about an offline region
 */
data class OfflineRegionInfo(
    val id: Long,
    val name: String,
    val createdAt: Long
)

/**
 * Progress tracking for offline downloads
 */
data class OfflineDownloadProgress(
    val regionId: String,
    val completedTiles: Long = 0,
    val totalTiles: Long = 0,
    val completedSize: Long = 0,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val percentComplete: Int
        get() = if (totalTiles > 0) {
            ((completedTiles * 100) / totalTiles).toInt()
        } else 0
}