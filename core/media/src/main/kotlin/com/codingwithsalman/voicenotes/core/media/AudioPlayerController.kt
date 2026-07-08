package com.codingwithsalman.voicenotes.core.media

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

/**
 * Single-track player for note playback. Owned by the note ViewModel;
 * call [release] from onCleared.
 */
class AudioPlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var player: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    /** Poll-based position stream; light enough at 10 Hz and only hot while collected. */
    val positionMs: Flow<Long> = flow {
        while (true) {
            emit(player?.currentPosition ?: 0L)
            delay(100)
        }
    }

    fun load(path: String) {
        release()
        player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(File(path).toUri()))
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    _isPlaying.value = isPlayingNow
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _durationMs.value = duration.coerceAtLeast(0L)
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        seekTo(0)
                        pause()
                    }
                }
            })
            prepare()
        }
    }

    fun playPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun seekToFraction(fraction: Float) {
        val p = player ?: return
        val duration = _durationMs.value
        if (duration > 0) p.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    fun seekToMs(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun setSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed.coerceIn(0.5f, 3f))
    }

    fun release() {
        player?.release()
        player = null
        _isPlaying.value = false
        _durationMs.value = 0L
    }
}
