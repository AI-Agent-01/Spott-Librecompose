package com.spott.core.analytics

/** Simple analytics facade used across KMP. */
interface Analytics {
    fun track(event: String, properties: Map<String, Any?> = emptyMap())
}

/** No-op implementation for non-Android targets unless provided. */
object NoopAnalytics : Analytics {
    override fun track(event: String, properties: Map<String, Any?>) { /* no-op */ }
}

