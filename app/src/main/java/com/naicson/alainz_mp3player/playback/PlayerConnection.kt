package com.naicson.alainz_mp3player.playback

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.naicson.alainz_mp3player.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * App-wide bridge to the [PlaybackService]'s [MediaController]. Owns the connection, mirrors
 * player state into flows the UI can observe, and exposes transport commands — the single
 * place that talks to Media3 so `MusicViewModel` doesn't need to know about it directly.
 */
@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var controller: MediaController? = null
    private val connectionScope = CoroutineScope(Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatEnabled = MutableStateFlow(false)
    val repeatEnabled: StateFlow<Boolean> = _repeatEnabled.asStateFlow()

    /**
     * Fraction (0..1) of the device's real `STREAM_MUSIC` volume — deliberately not the
     * player's own gain (`controller.volume`), so the in-app slider always matches what the
     * hardware volume buttons and the system volume overlay show.
     */
    private val _volume = MutableStateFlow(systemVolumeFraction())
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) == AudioManager.STREAM_MUSIC) {
                _volume.value = systemVolumeFraction()
            }
        }
    }

    init {
        context.registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
    }

    private fun systemVolumeFraction(): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return 0f
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val c = controller ?: return
            _currentIndex.value = c.currentMediaItemIndex.coerceAtLeast(0)
            _durationMs.value = c.duration.coerceAtLeast(0)
            _positionMs.value = c.currentPosition.coerceAtLeast(0)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                controller?.let { _durationMs.value = it.duration.coerceAtLeast(0) }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatEnabled.value = repeatMode != Player.REPEAT_MODE_OFF
        }
    }

    private suspend fun awaitController(): MediaController {
        controller?.let { return it }
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        return suspendCancellableCoroutine { cont ->
            future.addListener(
                {
                    try {
                        val c = future.get()
                        controller = c
                        _isPlaying.value = c.isPlaying
                        _currentIndex.value = c.currentMediaItemIndex.coerceAtLeast(0)
                        _durationMs.value = c.duration.coerceAtLeast(0)
                        _shuffleEnabled.value = c.shuffleModeEnabled
                        _repeatEnabled.value = c.repeatMode != Player.REPEAT_MODE_OFF
                        c.addListener(listener)
                        startPositionTicker()
                        cont.resume(c)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                },
                MoreExecutors.directExecutor(),
            )
        }
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = connectionScope.launch {
            while (true) {
                controller?.let { if (it.isPlaying) _positionMs.value = it.currentPosition }
                delay(500)
            }
        }
    }

    suspend fun setPlaylist(songs: List<Song>) {
        val c = awaitController()
        c.setMediaItems(songs.map { it.toMediaItem() })
        c.prepare()
    }

    /** Restores where playback left off in a previous app run — seeks there without starting
     * playback, since a freshly (re)launched app shouldn't just start blaring music on its own. */
    suspend fun prepareAt(index: Int, positionMs: Long) {
        val c = awaitController()
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, positionMs)
            _currentIndex.value = index
            _positionMs.value = positionMs
        }
    }

    /** Refreshes one item's title/artist/artwork in place — e.g. after editing a song's info —
     * without interrupting playback if it's the item currently playing. */
    suspend fun updateMediaItem(index: Int, song: Song) {
        val c = awaitController()
        if (index in 0 until c.mediaItemCount) {
            c.replaceMediaItem(index, song.toMediaItem())
        }
    }

    suspend fun playAt(index: Int) {
        val c = awaitController()
        c.seekTo(index, 0L)
        c.play()
    }

    suspend fun togglePlay() {
        val c = awaitController()
        if (c.isPlaying) c.pause() else c.play()
    }

    suspend fun next() = awaitController().seekToNext()
    suspend fun previous() = awaitController().seekToPrevious()

    /** "Play in order" from a given song index — e.g. the first song of the whole library, or
     * the first song of a specific folder. `seekTo(positionMs)` (single-arg) seeks *within the
     * current item*, which was the bug here: it restarted whatever was already playing instead
     * of jumping to a different song; the two-arg `seekTo(index, positionMs)` is what actually
     * moves to a different item. */
    suspend fun seekToIndexAndPlay(index: Int) {
        val c = awaitController()
        c.seekTo(index, 0L)
        c.play()
    }

    suspend fun seekToPercent(percent: Float) {
        val c = awaitController()
        val duration = c.duration.takeIf { it > 0 } ?: return
        c.seekTo((duration * percent.coerceIn(0f, 100f) / 100f).toLong())
    }

    suspend fun seekByMs(deltaMs: Long) {
        val c = awaitController()
        val duration = c.duration.coerceAtLeast(0)
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0, duration))
    }

    suspend fun removeAt(index: Int) {
        val c = awaitController()
        if (index in 0 until c.mediaItemCount) {
            c.removeMediaItem(index)
            _currentIndex.value = c.currentMediaItemIndex.coerceAtLeast(0)
        }
    }

    suspend fun setShuffle(enabled: Boolean) {
        awaitController().shuffleModeEnabled = enabled
    }

    suspend fun setRepeat(enabled: Boolean) {
        awaitController().repeatMode = if (enabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    fun setVolume(percent: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (max * (percent / 100f).coerceIn(0f, 1f)).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        _volume.value = systemVolumeFraction()
    }
}
