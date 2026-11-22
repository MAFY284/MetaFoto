package com.metafoto.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metafoto.app.R
import com.metafoto.app.ui.theme.ThemeMode

@Composable
fun ThemeModeDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeOption(
                    mode = ThemeMode.SYSTEM,
                    currentMode = currentMode,
                    icon = Icons.Outlined.Brightness4,
                    label = stringResource(R.string.theme_system),
                    onModeSelected = onModeSelected,
                )
                ThemeOption(
                    mode = ThemeMode.LIGHT,
                    currentMode = currentMode,
                    icon = Icons.Outlined.LightMode,
                    label = stringResource(R.string.theme_light),
                    onModeSelected = onModeSelected,
                )
                ThemeOption(
                    mode = ThemeMode.DARK,
                    currentMode = currentMode,
                    icon = Icons.Outlined.DarkMode,
                    label = stringResource(R.string.theme_dark),
                    onModeSelected = onModeSelected,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    currentMode: ThemeMode,
    icon: ImageVector,
    label: String,
    onModeSelected: (ThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onModeSelected(mode) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        )
        RadioButton(
            selected = currentMode == mode,
            onClick = { onModeSelected(mode) },
        )
    }
}
