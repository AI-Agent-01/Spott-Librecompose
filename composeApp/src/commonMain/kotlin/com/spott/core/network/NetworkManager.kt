package com.spott.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages network connectivity state and error handling.
 * Implements resilience patterns from the engineering guide.
 */
interface NetworkManager {
    val isOnline: StateFlow<Boolean>
    val networkErrors: Flow<NetworkError>
    
    fun reportError(error: NetworkError)
    fun clearErrors()
}

/**
 * Network error types for proper handling and user feedback
 */
sealed class NetworkError(
    val message: String,
    val isRetryable: Boolean = true
) {
    class ConnectionError(
        message: String = "No internet connection"
    ) : NetworkError(message)
    
    class ApiError(
        message: String,
        val statusCode: Int? = null
    ) : NetworkError(message, isRetryable = statusCode != 401 && statusCode != 403)
    
    class TimeoutError(
        message: String = "Request timed out"
    ) : NetworkError(message)
    
    class QuotaExceededError(
        message: String = "API quota exceeded. Please try again later."
    ) : NetworkError(message, isRetryable = false)
    
    class Unknown(
        message: String = "An unexpected error occurred"
    ) : NetworkError(message)
}

/**
 * Default implementation of NetworkManager
 */
class DefaultNetworkManager : NetworkManager {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    private val _networkErrors = MutableStateFlow<NetworkError?>(null)
    override val networkErrors: Flow<NetworkError?> = _networkErrors
    
    fun updateConnectivity(isConnected: Boolean) {
        _isOnline.value = isConnected
        if (!isConnected) {
            reportError(NetworkError.ConnectionError())
        }
    }
    
    override fun reportError(error: NetworkError) {
        _networkErrors.value = error
    }
    
    override fun clearErrors() {
        _networkErrors.value = null
    }
}

/**
 * Retry configuration for network requests
 */
data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000L,
    val maxDelayMs: Long = 10000L,
    val backoffMultiplier: Double = 2.0
) {
    fun getDelay(attempt: Int): Long {
        val delay = (initialDelayMs * Math.pow(backoffMultiplier, attempt.toDouble())).toLong()
        return minOf(delay, maxDelayMs)
    }
}