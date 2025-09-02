package com.spott.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Attribution overlay for MapTiler compliance.
 * REQUIRED: Must always be visible when map is displayed.
 * 
 * Per MapTiler Terms of Service:
 * - Attribution must credit MapTiler and OpenStreetMap
 * - MapTiler logo required for free tier
 * - Must not be obscured by other UI elements
 */
@Composable
fun AttributionOverlay(
    modifier: Modifier = Modifier
) {
    var showAttributionDialog by remember { mutableStateOf(false) }
    
    // Compact attribution button
    Surface(
        modifier = modifier
            .clickable { showAttributionDialog = true },
        shape = RoundedCornerShape(4.dp),
        color = Color.White.copy(alpha = 0.9f),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // MapTiler logo text (simplified for free tier)
            Text(
                text = "MapTiler",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C5AA0)
            )
            Text(
                text = "•",
                fontSize = 10.sp,
                color = Color.Gray
            )
            Text(
                text = "© OSM",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
    
    // Full attribution dialog
    if (showAttributionDialog) {
        AttributionDialog(
            onDismiss = { showAttributionDialog = false }
        )
    }
}

@Composable
private fun AttributionDialog(
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Map Data Attribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // MapTiler attribution
                AttributionItem(
                    title = "MapTiler",
                    description = "Map tiles and styling",
                    url = "https://www.maptiler.com",
                    onUrlClick = { uriHandler.openUri(it) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // OpenStreetMap attribution
                AttributionItem(
                    title = "OpenStreetMap contributors",
                    description = "Map data © OpenStreetMap contributors",
                    url = "https://www.openstreetmap.org/copyright",
                    onUrlClick = { uriHandler.openUri(it) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // MapLibre attribution
                AttributionItem(
                    title = "MapLibre",
                    description = "Open-source mapping library",
                    url = "https://maplibre.org",
                    onUrlClick = { uriHandler.openUri(it) }
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun AttributionItem(
    title: String,
    description: String,
    url: String,
    onUrlClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUrlClick(url) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}