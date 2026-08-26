package com.deepkush.reprange.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.deepkush.reprange.utils.listItemShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close

data class SettingsItemSpec(
    val title: String,
    val description: String? = null,
    val icon: ImageVector? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
    val onLongClick: (() -> Unit)? = null,
    val enabled: Boolean = true,
    val isHighlighted: Boolean = false,
    val isExternalLink: Boolean = false,
)

data class SettingsGroupSpec(
    val title: String? = null,
    val items: List<SettingsItemSpec>,
)

/** ModernSwitch per §4.2: thumb shows Check (primary) when on, Close (surface) when off. */
@Composable
fun ModernSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(14.dp),
            )
        },
        colors = SwitchDefaults.colors(),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsRow(item: SettingsItemSpec, shape: Shape) {
    val disabledAlpha = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = item.onClick ?: {},
                onLongClick = item.onLongClick,
                enabled = item.onClick != null || item.onLongClick != null,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (item.icon != null) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (!item.enabled) disabledAlpha
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.enabled) MaterialTheme.colorScheme.onSurface else disabledAlpha,
                )
                if (item.description != null) {
                    Text(
                        item.description!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (item.enabled) 1f else 0.4f,
                        ),
                    )
                }
            }
            if (item.isExternalLink && item.trailingContent == null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                item.trailingContent?.invoke()
            }
        }
    }
}

@Composable
fun SettingsGroup(group: SettingsGroupSpec, modifier: Modifier = Modifier) {
    Column(modifier) {
        if (group.title != null) {
            Text(
                group.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
            )
        }
        group.items.forEachIndexed { index, item ->
            SettingsRow(item, listItemShape(index, group.items.size))
            if (index != group.items.lastIndex) Spacer(Modifier.height(2.dp))
        }
    }
}
