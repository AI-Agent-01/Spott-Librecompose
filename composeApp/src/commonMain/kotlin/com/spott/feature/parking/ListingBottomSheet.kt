package com.spott.feature.parking

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ListingDetails(
    val id: String,
    val title: String,
    val address: String,
    val pricePerHour: Float,
    val rating: Float,
    val reviewCount: Int,
    val distance: Float, // in meters
    val walkTime: Int, // in minutes
    val entryHint: String? = null,
    val isTopSpott: Boolean = false,
    val availability: String = "Available",
    val images: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingBottomSheet(
    listing: ListingDetails,
    selectedDuration: ParkingDuration,
    onDurationChange: (ParkingDuration) -> Unit,
    onBookNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { true }
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Header
            ListingHeader(listing)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Duration Selector
            DurationSelector(
                selectedDuration = selectedDuration,
                onDurationChange = onDurationChange,
                pricePerHour = listing.pricePerHour
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Entry hint if available
            listing.entryHint?.let { hint ->
                EntryHintCard(hint)
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Total price and Book button
            BookingFooter(
                totalPrice = calculateTotalPrice(listing.pricePerHour, selectedDuration),
                duration = selectedDuration,
                onBookNow = onBookNow
            )
        }
    }
}

@Composable
private fun ListingHeader(listing: ListingDetails) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${listing.pricePerHour}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/hr",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    
                    if (listing.isTopSpott) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Badge(
                            containerColor = Color(0xFFFFD700),
                            contentColor = Color.Black
                        ) {
                            Text("TOP SPOTT", fontSize = 10.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${listing.rating}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        text = "(${listing.reviewCount} reviews)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${listing.walkTime} min",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Text(
                    text = "${formatDistance(listing.distance)} away",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = listing.address,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        AssistChip(
            onClick = { },
            label = {
                Text(
                    text = listing.availability,
                    style = MaterialTheme.typography.labelSmall
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF4CAF50)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun DurationSelector(
    selectedDuration: ParkingDuration,
    onDurationChange: (ParkingDuration) -> Unit,
    pricePerHour: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "How long?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Duration ruler/slider visual
        DurationRuler(
            selectedDuration = selectedDuration,
            onDurationChange = onDurationChange
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Quick duration chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickDurationChip(
                label = "+15m",
                onClick = { onDurationChange(ParkingDuration.FIFTEEN_MIN) },
                selected = selectedDuration == ParkingDuration.FIFTEEN_MIN
            )
            QuickDurationChip(
                label = "+30m",
                onClick = { onDurationChange(ParkingDuration.THIRTY_MIN) },
                selected = selectedDuration == ParkingDuration.THIRTY_MIN
            )
            QuickDurationChip(
                label = "+1h",
                onClick = { onDurationChange(ParkingDuration.ONE_HOUR) },
                selected = selectedDuration == ParkingDuration.ONE_HOUR
            )
            QuickDurationChip(
                label = "+2h",
                onClick = { onDurationChange(ParkingDuration.TWO_HOURS) },
                selected = selectedDuration == ParkingDuration.TWO_HOURS
            )
        }
    }
}

@Composable
private fun DurationRuler(
    selectedDuration: ParkingDuration,
    onDurationChange: (ParkingDuration) -> Unit
) {
    val durations = ParkingDuration.values()
    val selectedIndex = durations.indexOf(selectedDuration)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            durations.forEach { duration ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (duration == selectedDuration) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            }
                        )
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = duration.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (duration == selectedDuration) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (duration == selectedDuration) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickDurationChip(
    label: String,
    onClick: () -> Unit,
    selected: Boolean
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    )
}

@Composable
private fun EntryHintCard(hint: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Entry instructions",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BookingFooter(
    totalPrice: Float,
    duration: ParkingDuration,
    onBookNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$${String.format("%.2f", totalPrice)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " for ${duration.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onBookNow,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Book Now",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        TextButton(
            onClick = { /* One-tap payment setup */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Set up one-tap payment",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun calculateTotalPrice(pricePerHour: Float, duration: ParkingDuration): Float {
    return pricePerHour * duration.hours
}

private fun formatDistance(meters: Float): String {
    return when {
        meters < 1000 -> "${meters.toInt()}m"
        else -> "${String.format("%.1f", meters / 1000)}km"
    }
}