package com.spott.app.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey

/**
 * Manages multiple navigation back stacks for top-level destinations.
 * This allows preserving navigation history when switching between drawer sections.
 * 
 * Based on Nav3's TopLevelBackStack pattern from CommonUiActivity example.
 */
class SpottTopLevelBackStack(startKey: NavKey) {
    
    // Maintain a stack for each top-level destination
    private var topLevelStacks: LinkedHashMap<NavKey, SnapshotStateList<NavKey>> = linkedMapOf(
        startKey to mutableStateListOf(startKey)
    )
    
    // Track the current top-level destination
    var currentTopLevel by mutableStateOf(startKey)
        private set
    
    // Expose flattened back stack for NavDisplay
    val backStack = mutableStateListOf(startKey)
    
    // Define which destinations are top-level (from drawer)
    private val topLevelDestinations = setOf(
        HomeMap,
        BookingsList,
        ActiveSession,
        HostHub,
        HostWizardEntry,
        Profile,
        Settings,
        Help
    )
    
    /**
     * Updates the flattened back stack from all top-level stacks
     */
    private fun updateBackStack() {
        backStack.apply {
            clear()
            addAll(topLevelStacks.flatMap { it.value })
        }
    }
    
    /**
     * Determines if a destination is a top-level destination
     */
    fun isTopLevelDestination(key: NavKey): Boolean {
        return key in topLevelDestinations
    }
    
    /**
     * Navigate to a top-level destination, preserving its stack
     */
    fun navigateToTopLevel(key: NavKey) {
        // If this top-level doesn't exist yet, create its stack
        if (topLevelStacks[key] == null) {
            topLevelStacks[key] = mutableStateListOf(key)
        } else {
            // Move existing stack to the end (most recent)
            topLevelStacks.apply {
                remove(key)?.let {
                    put(key, it)
                }
            }
        }
        currentTopLevel = key
        updateBackStack()
    }
    
    /**
     * Add a destination within the current section
     */
    fun navigateWithinSection(key: NavKey) {
        topLevelStacks[currentTopLevel]?.add(key)
        updateBackStack()
    }
    
    /**
     * Navigate to any destination, determining if it's top-level
     */
    fun navigateTo(key: NavKey) {
        if (isTopLevelDestination(key)) {
            navigateToTopLevel(key)
        } else {
            navigateWithinSection(key)
        }
    }
    
    /**
     * Remove the last destination from the back stack
     * @return true if navigation was handled, false if should exit app
     */
    fun navigateUp(): Boolean {
        val currentStack = topLevelStacks[currentTopLevel]
        
        if (currentStack == null || currentStack.isEmpty()) {
            return false
        }
        
        // If we're at the root of a section
        if (currentStack.size == 1) {
            // If this is the only remaining top-level, can't go back
            if (topLevelStacks.size == 1) {
                return false
            }
            
            // Remove this top-level and switch to previous
            topLevelStacks.remove(currentTopLevel)
            currentTopLevel = topLevelStacks.keys.last()
            updateBackStack()
            return true
        }
        
        // Otherwise just pop from current stack
        currentStack.removeLastOrNull()
        updateBackStack()
        return true
    }
    
    /**
     * Replace the current destination with a new one
     */
    fun replaceWith(key: NavKey) {
        val currentStack = topLevelStacks[currentTopLevel]
        currentStack?.let {
            if (it.isNotEmpty()) {
                it.removeAt(it.lastIndex)
            }
            it.add(key)
            updateBackStack()
        }
    }
    
    /**
     * Navigate to the root of the current section
     */
    fun navigateToRoot() {
        val currentStack = topLevelStacks[currentTopLevel]
        currentStack?.let {
            val root = it.firstOrNull()
            if (root != null) {
                it.clear()
                it.add(root)
                updateBackStack()
            }
        }
    }
    
    /**
     * Get the current destination
     */
    fun getCurrentDestination(): NavKey? {
        return backStack.lastOrNull()
    }
    
    /**
     * Check if we're at a specific destination
     */
    fun isCurrentDestination(destination: NavKey): Boolean {
        return getCurrentDestination() == destination
    }
    
    /**
     * Get the size of the back stack
     */
    val size: Int
        get() = backStack.size
    
    /**
     * Remove last or null (for compatibility)
     */
    fun removeLastOrNull(): NavKey? {
        return if (navigateUp()) {
            getCurrentDestination()
        } else {
            null
        }
    }
}