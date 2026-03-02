package com.example.shaderwallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shaderwallpaper.ui.theme.ShaderWallpaperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShaderWallpaperTheme {
                // Navigation State
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Gallery) }
                val viewModel: WallpaperViewModel = viewModel()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        is Screen.Gallery -> {
                            WallpaperGallery(
                                onWallpaperSelected = { id ->
                                    viewModel.selectWallpaper(id)
                                    currentScreen = Screen.Detail
                                },
                                viewModel = viewModel
                            )
                        }
                        is Screen.Detail -> {
                            // Handle system back button
                            BackHandler {
                                currentScreen = Screen.Gallery
                            }

                            WallpaperDetailScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.Gallery }
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen {
    object Gallery : Screen()
    object Detail : Screen()
}

@Preview(showBackground = true,
        showSystemUi = true)
@Composable
fun WallpaperScreenPreview() {
    ShaderWallpaperTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background // This fixes the background for Dark Mode
        ) {
            WallpaperDetailScreen()
        }
    }
}
