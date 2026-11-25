package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.browser.BrowserPresets
import com.example.browser.DetectedMediaItem
import com.example.browser.MediaStreamType
import com.example.media.MediaPlaybackState
import com.example.media.MediaPlayerManager
import com.example.proxy.ProxyTelemetry

@OptIn(UnstableApi::class)
@Composable
fun MediaPlayerScreen(
    playbackState: MediaPlaybackState,
    mediaPlayerManager: MediaPlayerManager,
    detectedPageMedia: List<DetectedMediaItem>,
    proxyTelemetry: ProxyTelemetry,
    onRescanPageMedia: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        if (playbackState.isFullscreen) {
            mediaPlayerManager.setFullscreen(false)
        } else {
            onBack()
        }
    }

    var customUrlInput by remember { mutableStateOf("") }

    if (playbackState.isFullscreen && playbackState.currentItem != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = mediaPlayerManager.getOrCreatePlayer()
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { pv ->
                    pv.player = mediaPlayerManager.getOrCreatePlayer()
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = { mediaPlayerManager.setFullscreen(false) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .background(Color(0x88000000), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FullscreenExit,
                    contentDescription = "Exit Fullscreen",
                    tint = Color.White
                )
            }
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            @kotlin.OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Column {
                        Text("HTML5 Media Player", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = if (proxyTelemetry.isRunning && proxyTelemetry.activeMode.name != "DIRECT") {
                                "Routed via ${proxyTelemetry.activeMode.label}"
                            } else {
                                "Media3 ExoPlayer • Direct Network"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("media_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Browser"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Active Video / Audio Viewport Card
                item {
                    val item = playbackState.currentItem
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF0A111D), Color(0xFF132238))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item != null && item.type == MediaStreamType.VIDEO) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                player = mediaPlayerManager.getOrCreatePlayer()
                                                useController = false
                                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            }
                                        },
                                        update = { pv ->
                                            pv.player = mediaPlayerManager.getOrCreatePlayer()
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = if (item?.type == MediaStreamType.AUDIO) Icons.Rounded.GraphicEq else Icons.Rounded.Movie,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = item?.title ?: "Select a detected page stream or test stream below",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFE2E8F0),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                if (playbackState.isBuffering) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                if (item?.type == MediaStreamType.VIDEO) {
                                    IconButton(
                                        onClick = { mediaPlayerManager.setFullscreen(true) },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .background(Color(0x88000000), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Fullscreen,
                                            contentDescription = "Fullscreen",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = item?.title ?: "No Media Loaded",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            playbackState.errorMessage?.let { err ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            // Scrubber
                            val duration = playbackState.durationMs.coerceAtLeast(1L)
                            val position = playbackState.positionMs.coerceIn(0L, duration)
                            Slider(
                                value = if (playbackState.durationMs > 0) position.toFloat() / duration.toFloat() else 0f,
                                onValueChange = { fraction ->
                                    mediaPlayerManager.seekTo((fraction * duration).toLong())
                                },
                                enabled = item != null && playbackState.durationMs > 0
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(playbackState.positionMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDuration(playbackState.durationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Transport Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { mediaPlayerManager.toggleMute() }) {
                                    Icon(
                                        imageVector = if (playbackState.isMuted) Icons.AutoMirrored.Rounded.VolumeOff
                                        else Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = "Toggle Mute"
                                    )
                                }
                                IconButton(onClick = { mediaPlayerManager.seekBy(-10_000L) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Replay10,
                                        contentDescription = "Rewind 10s"
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = item != null) { mediaPlayerManager.togglePlayPause() }
                                        .testTag("media_play_pause_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { mediaPlayerManager.seekBy(10_000L) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Forward10,
                                        contentDescription = "Forward 10s"
                                    )
                                }
                                IconButton(onClick = { mediaPlayerManager.toggleLoop() }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Repeat,
                                        contentDescription = "Toggle Loop",
                                        tint = if (playbackState.isLooping) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                            ) {
                                listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    FilterChip(
                                        selected = playbackState.playbackSpeed == speed,
                                        onClick = { mediaPlayerManager.setPlaybackSpeed(speed) },
                                        label = { Text("${speed}x", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Direct Stream URL Launcher
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            label = { Text("Stream URL (.mp4, .m3u8, .mp3)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_media_url_input")
                        )
                        Button(
                            onClick = {
                                if (customUrlInput.isNotBlank()) {
                                    mediaPlayerManager.playUrl(customUrlInput)
                                }
                            },
                            enabled = customUrlInput.isNotBlank(),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("play_custom_media_button")
                        ) {
                            Text("Play")
                        }
                    }
                }

                // Detected Page Media Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Detected on Active Page", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onRescanPageMedia) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rescan")
                        }
                    }
                }

                if (detectedPageMedia.isEmpty()) {
                    item {
                        Text(
                            text = "No <video> or <audio> streams detected on the active tab yet. Browse a media page or play one of the HTML5 test streams below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(detectedPageMedia, key = { it.url }) { stream ->
                        MediaStreamRow(
                            item = stream,
                            isActive = playbackState.currentItem?.url == stream.url,
                            onPlay = { mediaPlayerManager.playItem(stream) }
                        )
                    }
                }

                // Open-source HTML5 & Adaptive Stream Verification Presets
                item {
                    Text("HTML5 & Adaptive Stream Test Deck", style = MaterialTheme.typography.titleMedium)
                }

                items(BrowserPresets.openTestMediaStreams, key = { it.url }) { preset ->
                    MediaStreamRow(
                        item = preset,
                        isActive = playbackState.currentItem?.url == preset.url,
                        onPlay = { mediaPlayerManager.playItem(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaStreamRow(
    item: DetectedMediaItem,
    isActive: Boolean,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.type == MediaStreamType.AUDIO) Icons.Rounded.MusicNote else Icons.Rounded.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play stream",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
