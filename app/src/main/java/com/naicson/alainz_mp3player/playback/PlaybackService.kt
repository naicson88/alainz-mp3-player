package com.naicson.alainz_mp3player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.MoreExecutors
import com.naicson.alainz_mp3player.MainActivity
import com.naicson.alainz_mp3player.widget.MediaWidgets

/** Hosts the ExoPlayer instance behind a MediaSession so playback survives backgrounding/rotation and exposes system controls (notification, lock screen, headset buttons). */
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var bitmapLoader: EmbeddedArtBitmapLoader
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        // Shared with the widget's own artwork refresh below, so a cover only needs to be
        // resolved (embedded-art decode or http fetch) through one code path.
        bitmapLoader = EmbeddedArtBitmapLoader(DataSourceBitmapLoader(this))
        player.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = refreshWidget()
            override fun onIsPlayingChanged(isPlaying: Boolean) = refreshWidget()
        })

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent)
            .setBitmapLoader(bitmapLoader)
            .build()
    }

    /** Pushes the current song/transport state to the home-screen widget, if one is placed —
     * there's no automatic Android equivalent to the notification's MediaSession integration. */
    private fun refreshWidget() {
        val metadata = player.mediaMetadata
        val title = metadata.title?.toString().orEmpty()
        val artist = metadata.artist?.toString().orEmpty()
        val isPlaying = player.isPlaying
        val artworkUri = metadata.artworkUri

        if (artworkUri == null) {
            MediaWidgets.update(this, title, artist, isPlaying, null)
            return
        }
        val future = bitmapLoader.loadBitmap(artworkUri)
        future.addListener(
            {
                val bitmap = try {
                    future.get()
                } catch (e: Exception) {
                    null
                }
                MediaWidgets.update(this, title, artist, isPlaying, bitmap)
            },
            MoreExecutors.directExecutor(),
        )
    }

    /** The widget lives in this same process, so its buttons just send an explicit intent
     * straight to this already-running service instead of going through a MediaController
     * connection or a MediaButtonReceiver (which, on API 26+, only forwards play/pause anyway). */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> if (player.isPlaying) player.pause() else player.play()
            ACTION_NEXT -> player.seekToNext()
            ACTION_PREVIOUS -> player.seekToPrevious()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.naicson.alainz_mp3player.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.naicson.alainz_mp3player.action.NEXT"
        const val ACTION_PREVIOUS = "com.naicson.alainz_mp3player.action.PREVIOUS"
    }
}
