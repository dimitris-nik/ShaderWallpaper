package com.example.shaderwallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun ColorPickerDialog(
    colorParams: Map<String, Color>,
    onColorChanged: (String, Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (colorParams.isEmpty()) {
        LaunchedEffect(Unit) { onDismissRequest() }
        return
    }

    // Keep internal state if needed, or rely on external state update + recomposition
    // Since onColorChanged updates the ViewModel which updates the State, it should be fine.

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Customize Colors") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                colorParams.forEach { (key, color) ->
                    val label = key.replace("_", " ").replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                    }

                    ColorItemEditor(
                        label = label,
                        color = color,
                        onColorChanged = { newColor -> onColorChanged(key, newColor) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Done")
            }
        }
    )
}

@Composable
fun ColorItemEditor(
    label: String,
    color: Color,
    onColorChanged: (Color) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Red
        ColorSlider(
            label = "R",
            value = color.red,
            color = Color.Red,
            onValueChange = { onColorChanged(color.copy(red = it)) }
        )

        // Green
        ColorSlider(
            label = "G",
            value = color.green,
            color = Color.Green,
            onValueChange = { onColorChanged(color.copy(green = it)) }
        )

        // Blue
        ColorSlider(
            label = "B",
            value = color.blue,
            color = Color.Blue,
            onValueChange = { onColorChanged(color.copy(blue = it)) }
        )
    }
}

@Composable
fun ColorSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(32.dp) // Compact
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.24f)
            )
        )
    }
}

