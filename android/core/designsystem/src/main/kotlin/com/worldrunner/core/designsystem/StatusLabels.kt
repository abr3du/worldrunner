package com.worldrunner.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worldrunner.core.model.SyncStatus
import com.worldrunner.core.model.Zone

/** Sync state of a Run, shown with icon and text so colour is never the only cue. */
@Composable
fun SyncStatusLabel(status: SyncStatus, modifier: Modifier = Modifier) {
    val (icon, text) = when (status) {
        SyncStatus.Pending -> Icons.Filled.Refresh to R.string.sync_pending
        SyncStatus.Confirmed -> Icons.Filled.CheckCircle to R.string.sync_confirmed
        SyncStatus.NeedsAttention -> Icons.Filled.Warning to R.string.sync_needs_attention
    }
    val color = if (status == SyncStatus.NeedsAttention) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    IconLabel(icon, stringResource(text), color, modifier)
}

/** Promotion/Relegation zone, shown with icon and text so colour is never the only cue. */
@Composable
fun ZoneLabel(zone: Zone, modifier: Modifier = Modifier) {
    when (zone) {
        Zone.Promotion -> IconLabel(Icons.Filled.KeyboardArrowUp, stringResource(R.string.zone_promotion), MaterialTheme.colorScheme.primary, modifier)
        Zone.Relegation -> IconLabel(Icons.Filled.KeyboardArrowDown, stringResource(R.string.zone_relegation), MaterialTheme.colorScheme.error, modifier)
        Zone.Safe -> Unit
    }
}

@Composable
private fun IconLabel(icon: ImageVector, text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
