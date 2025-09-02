package com.spott.feature.parking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spott.map.LatLng
import kotlinx.coroutines.delay

// UI State
data class SearchDestinationState(
    val searchQuery: String = "",
    val predictions: List<PlacePrediction> = emptyList(),
    val recentSearches: List<PlacePrediction> = emptyList(),
    val isLoading: Boolean = false,
    val userLocation: LatLng? = null,
    val error: String? = null
)

// Domain models
data class PlacePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val location: LatLng? = null // Will be resolved when selected
)

// Intents
sealed interface SearchDestinationIntent {
    data object OnAppear : SearchDestinationIntent
    data class OnSearchQueryChange(val query: String) : SearchDestinationIntent
    data class OnPredictionSelect(val prediction: PlacePrediction) : SearchDestinationIntent
    data object OnUseCurrentLocation : SearchDestinationIntent
    data object OnClearSearch : SearchDestinationIntent
    data object OnNavigateBack : SearchDestinationIntent
}

// Effects
sealed interface SearchDestinationEffect {
    data class NavigateToMap(val destination: LatLng) : SearchDestinationEffect
    data object NavigateBack : SearchDestinationEffect
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDestinationScreen(
    state: SearchDestinationState,
    onIntent: (SearchDestinationIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        onIntent(SearchDestinationIntent.OnAppear)
        delay(300) // Small delay for animation
        focusRequester.requestFocus()
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            SearchTopBar(
                searchQuery = state.searchQuery,
                onQueryChange = { onIntent(SearchDestinationIntent.OnSearchQueryChange(it)) },
                onClearSearch = { onIntent(SearchDestinationIntent.OnClearSearch) },
                onNavigateBack = { onIntent(SearchDestinationIntent.OnNavigateBack) },
                focusRequester = focusRequester
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Use current location option
            item {
                CurrentLocationItem(
                    onClick = { onIntent(SearchDestinationIntent.OnUseCurrentLocation) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            // Loading indicator
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
            
            // Search predictions
            if (state.predictions.isNotEmpty()) {
                item {
                    Text(
                        text = "Suggestions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                items(
                    items = state.predictions,
                    key = { it.placeId }
                ) { prediction ->
                    PredictionItem(
                        prediction = prediction,
                        onClick = { onIntent(SearchDestinationIntent.OnPredictionSelect(prediction)) }
                    )
                }
            }
            
            // Recent searches (when no query)
            if (state.searchQuery.isEmpty() && state.recentSearches.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                items(
                    items = state.recentSearches,
                    key = { it.placeId }
                ) { recent ->
                    RecentSearchItem(
                        prediction = recent,
                        onClick = { onIntent(SearchDestinationIntent.OnPredictionSelect(recent)) }
                    )
                }
            }
            
            // Error message
            state.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onNavigateBack: () -> Unit,
    focusRequester: FocusRequester
) {
    TopAppBar(
        title = {
            SearchBar(
                query = searchQuery,
                onQueryChange = onQueryChange,
                onSearch = { /* Handle search submit */ },
                active = true,
                onActiveChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text("Where are you going?")
                },
                leadingIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear"
                            )
                        }
                    }
                }
            ) {
                // Search bar content is handled outside
            }
        }
    )
}

@Composable
private fun CurrentLocationItem(
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = "Use current location",
                fontWeight = FontWeight.Medium
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun PredictionItem(
    prediction: PlacePrediction,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = prediction.primaryText,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = prediction.secondaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun RecentSearchItem(
    prediction: PlacePrediction,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = prediction.primaryText,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = prediction.secondaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}