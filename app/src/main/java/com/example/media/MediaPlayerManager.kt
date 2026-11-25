package com.example.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.browser.DetectedMediaItem
import com.example.browser.MediaStreamType
import com.example.proxy.ProxyEngineManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MediaPlaybackState(
    val currentItem: DetectedMediaItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isMuted: Boolean = false,
    val isLooping: Boolean = false,
    val isFullscreen: Boolean = false,
    val errorMessage: String? = null
)

class MediaPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(MediaPlaybackState())
    val state: StateFlow<MediaPlaybackState> = _state.asStateFlow()

    private var _exoPlayer: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    fun getOrCreatePlayer(): ExoPlayer {
        val existing = _exoPlayer
        if (existing != null) return existing

        val dataSourceFactory = OkHttpDataSource.Factory(
            ProxyEngineManager.buildProxyOkHttpClient()
        ).setUserAgent("RelayWeb-HTML5-MediaPlayer/1.0")

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = error.localizedMessage ?: "Media playback failed"
                    )
                }
            }
        })

        _exoPlayer = player
        startProgressPolling()
        return player
    }

    @OptIn(UnstableApi::class)
    fun rebuildForCurrentProxy() {
        val current = _state.value.currentItem
        val pos = _exoPlayer?.currentPosition ?: 0L
        val wasPlaying = _exoPlayer?.isPlaying == true
        _exoPlayer?.release()
        _exoPlayer = null
        if (current != null) {
            playItem(current, seekToMs = pos, autoPlay = wasPlaying)
        }
    }

    fun playItem(item: DetectedMediaItem, seekToMs: Long = 0L, autoPlay: Boolean = true) {
        val player = getOrCreatePlayer()
        _state.update {
            it.copy(
                currentItem = item,
                errorMessage = null,
                isBuffering = true,
                positionMs = seekToMs
            )
        }
        player.setMediaItem(MediaItem.fromUri(item.url))
        player.prepare()
        if (seekToMs > 0L) {
            player.seekTo(seekToMs)
        }
        player.playWhenReady = autoPlay
    }

    fun playUrl(url: String, title: String = "Custom HTML5 Stream") {
        val clean = url.trim()
        if (clean.isEmpty()) return
        val lower = clean.lowercase().substringBefore('?')
        val type = if (
            lower.endsWith(".mp3") || lower.endsWith(".aac") ||
            lower.endsWith(".ogg") || lower.endsWith(".flac") || lower.endsWith(".wav")
        ) {
            MediaStreamType.AUDIO
        } else {
            MediaStreamType.VIDEO
        }
        playItem(
            DetectedMediaItem(
                url = clean,
                title = title,
                type = type
            )
        )
    }

    fun togglePlayPause() {
        val player = _exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = _exoPlayer ?: return
        val clamped = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(clamped)
        _state.update { it.copy(positionMs = clamped) }
    }

    fun seekBy(deltaMs: Long) {
        val player = _exoPlayer ?: return
        seekTo(player.currentPosition + deltaMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        val player = _exoPlayer ?: return
        player.playbackParameters = PlaybackParameters(speed)
        _state.update { it.copy(playbackSpeed = speed) }
    }

    fun toggleMute() {
        val player = _exoPlayer ?: return
        val nextMuted = !_state.value.isMuted
        player.volume = if (nextMuted) 0f else 1f
        _state.update { it.copy(isMuted = nextMuted) }
    }

    fun toggleLoop() {
        val player = _exoPlayer ?: return
        val nextLoop = !_state.value.isLooping
        player.repeatMode = if (nextLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _state.update { it.copy(isLooping = nextLoop) }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _state.update { it.copy(isFullscreen = fullscreen) }
    }

    fun stopPlayback() {
        _exoPlayer?.stop()
        _state.update {
            MediaPlaybackState()
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = _exoPlayer
                if (player != null && _state.value.currentItem != null) {
                    _state.update {
                        it.copy(
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        _exoPlayer?.release()
        _exoPlayer = null
    }
}
