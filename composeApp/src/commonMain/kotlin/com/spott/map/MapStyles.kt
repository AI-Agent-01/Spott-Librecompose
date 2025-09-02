package com.spott.map

/**
 * Centralized configuration for map styles and providers
 */
object MapStyles {
    // MapTiler base URL
    private const val MAPTILER_BASE_URL = "https://api.maptiler.com"
    
    // Available MapTiler styles
    object Styles {
        const val STREETS_V2 = "streets-v2"
        const val OUTDOOR = "outdoor"
        const val SATELLITE = "satellite"
        const val HYBRID = "hybrid"
        const val BASIC = "basic"
        const val BRIGHT = "bright"
        const val DARK = "dark"
        const val LIGHT = "light"
    }
    
    /**
     * Constructs a MapTiler style URL with the API key
     * @param style The style name from [Styles]
     * @param apiKey The MapTiler API key
     * @return Complete style URL for MapLibre
     */
    fun getStyleUrl(style: String = Styles.STREETS_V2, apiKey: String): String {
        return "$MAPTILER_BASE_URL/maps/$style/style.json?key=$apiKey"
    }
    
    /**
     * Default style for the Spott app
     */
    fun getDefaultStyleUrl(apiKey: String): String {
        return getStyleUrl(Styles.STREETS_V2, apiKey)
    }
    
    /**
     * Dark mode style for night driving
     */
    fun getDarkStyleUrl(apiKey: String): String {
        return getStyleUrl(Styles.DARK, apiKey)
    }
}