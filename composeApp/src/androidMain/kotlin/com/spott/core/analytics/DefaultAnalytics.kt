package com.spott.core.analytics

import android.util.Log

class DefaultAnalytics : Analytics {
    override fun track(event: String, properties: Map<String, Any?>) {
        val props = if (properties.isEmpty()) "" else properties.entries.joinToString(
            prefix = " ", separator = ", "
        ) { (k, v) -> "$k=$v" }
        Log.d("SpottAnalytics", "$event$props")
    }
}

