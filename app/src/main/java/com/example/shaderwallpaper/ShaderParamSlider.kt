package com.example.shaderwallpaper

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShaderParamSlider(
    paramKey: String,
    param: ShaderFloatParam,
    onValueChange: (Float) -> Unit
) {
    Column {
        // Formatting key name
        val label = paramKey.replace("_", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }

        // Interaction source to track if slider is being touched
        val interactionSource = remember { MutableInteractionSource() }
        var isInteracting by remember { mutableStateOf(false) }

        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press, is DragInteraction.Start -> isInteracting = true
                    is PressInteraction.Release, is PressInteraction.Cancel, is DragInteraction.Stop, is DragInteraction.Cancel -> isInteracting = false
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Show value only when interacting
            if (isInteracting) {
                Text(
                    text = String.format(Locale.getDefault(), "%.2f", param.value),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Slider(
            value = param.value,
            onValueChange = onValueChange,
            valueRange = param.min..param.max,
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    thumbSize = DpSize(0.dp, 0.dp)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(20.dp),
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 10.dp,
                    drawStopIndicator = null
                )
            }
        )
    }
}

