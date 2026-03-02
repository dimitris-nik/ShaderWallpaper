package com.example.shaderwallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shaderwallpaper.ui.theme.ShaderWallpaperTheme

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WallpaperDetailScreen(
    viewModel: WallpaperViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val config by viewModel.shaderConfig.collectAsState()
    var showFullscreen by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        WallpaperInfoDialog(
            config = config,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showColorDialog) {
        ColorPickerDialog(
            colorParams = config.colorParams,
            onColorChanged = { key, newColor ->
                viewModel.updateColorParam(key, newColor)
            },
            onDismissRequest = { showColorDialog = false }
        )
    }

    if (showFullscreen) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false) // Fullscreen dialog
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ShaderLivePreview(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showFullscreen = false }, // Click to close
                    config = config
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(config.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (config.colorParams.isNotEmpty()) {
                        IconButton(onClick = { showColorDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = "Customize Colors"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Respect Scaffold padding
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // --- Live Preview ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { showFullscreen = true }
            ) {
                ShaderLivePreview(
                    modifier = Modifier.matchParentSize(),
                    config = config,
                    isPaused = showFullscreen
                )
            }

            config.floatParams.forEach { (key, param) ->
                ShaderParamSlider(
                    paramKey = key,
                    param = param,
                    onValueChange = { newValue ->
                        viewModel.updateFloatParam(key, newValue)
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = {
                        viewModel.saveSettingsToDevice(context)
                        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                            putExtra(
                                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                ComponentName(context, LiveWallpaperService::class.java)
                            )
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Wallpaper, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Wallpaper")
                }

                FilledTonalIconButton(onClick = { showInfoDialog = true }) {
                    Icon(Icons.Filled.Info, contentDescription = "Info")
                }
            }
        }
    }
}

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    showSystemUi = true)
@Composable
fun WallpaperDetailScreenPreview() {
    ShaderWallpaperTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            WallpaperDetailScreen()
        }
    }
}
