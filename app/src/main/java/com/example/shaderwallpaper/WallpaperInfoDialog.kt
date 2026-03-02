package com.example.shaderwallpaper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WallpaperInfoDialog(
    config: ShaderConfig,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = config.name,
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (config.description.isNotEmpty()) {
                    Text(
                        text = config.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (config.credit.isNotEmpty()) {
                    val creditText = "Credit: ${config.credit}"
                    val annotatedString = buildAnnotatedString {
                        val urlPattern = java.util.regex.Pattern.compile("(https?://\\S+|www\\.\\S+)")
                        val matcher = urlPattern.matcher(creditText)
                        var lastIndex = 0
                        while (matcher.find()) {
                            val start = matcher.start()
                            val end = matcher.end()
                            // Append text before match
                            if (start > lastIndex) {
                                append(creditText.substring(lastIndex, start))
                            }

                            // Annotate match as URL
                            val url = matcher.group()
                            val fullUrl = if (url.startsWith("www.")) "https://$url" else url

                            pushStringAnnotation(tag = "URL", annotation = fullUrl)
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(url)
                            }
                            pop()
                            lastIndex = end
                        }
                        // Append remaining text
                        if (lastIndex < creditText.length) {
                            append(creditText.substring(lastIndex))
                        }
                    }

                    @Suppress("DEPRECATION") // ClickableText is deprecated but LinkAnnotation requires newer Compose
                    ClickableText(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    try {
                                        uriHandler.openUri(annotation.item)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                        }
                    )
                }
                if (config.tags.isNotEmpty()) {
                    Text(
                        text = "Tags:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        config.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFF9800), // Orange
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

