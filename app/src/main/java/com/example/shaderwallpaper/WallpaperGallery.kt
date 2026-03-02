package com.example.shaderwallpaper

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperGallery(
    onWallpaperSelected: (String) -> Unit,
    viewModel: WallpaperViewModel = viewModel()
) {
    val availableWallpapers by viewModel.availableWallpapers.collectAsState()
    val listState = rememberLazyGridState()
    val isScrolling = listState.isScrollInProgress
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val shaderCode = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input)).use { it.readText() }
                }
            }.getOrNull()

            if (!shaderCode.isNullOrBlank()) {
                viewModel.importCustomShader(shaderCode)
                onWallpaperSelected(Constants.CUSTOM_WALLPAPER_ID)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            TopAppBar(
                title = { Text("Shader Wallpapers") },
                actions = {
                    IconButton(onClick = {
                        importLauncher.launch(arrayOf("text/*", "application/octet-stream"))
                    }) {
                        Icon(
                            imageVector = Icons.Filled.UploadFile,
                            contentDescription = "Import shader"
                        )
                    }
                }
            )

            LazyVerticalGrid(
                state = listState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(availableWallpapers) { config ->
                    WallpaperGridItem(
                        config = config,
                        isScrolling = isScrolling,
                        onClick = { onWallpaperSelected(config.shaderId) }
                    )
                }
            }
        }
    }
}

@Composable
fun WallpaperGridItem(
    config: ShaderConfig,
    isScrolling: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.6f),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GalleryShaderPreview(
                config = config,
                isScrolling = isScrolling,
                modifier = Modifier.fillMaxSize(),
                onPreviewClicked = onClick
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            ) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}