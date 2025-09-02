package com.spott.app

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class SpottUiState(
    val isHostUser: Boolean = false,
    val hasActiveBooking: Boolean = false,
    val unreadBookingsBadge: Boolean = false, // badge for Bookings when active
)

class SpottAppViewModel : ViewModel() {
    private val _state = MutableStateFlow(SpottUiState())
    val state: StateFlow<SpottUiState> = _state

    fun setHost(isHost: Boolean) = _state.update { it.copy(isHostUser = isHost) }
    fun setActiveBooking(active: Boolean) = _state.update {
        it.copy(hasActiveBooking = active, unreadBookingsBadge = active)
    }
    fun clearBookingsBadge() = _state.update { it.copy(unreadBookingsBadge = false) }
}