package com.naicson.alainz_mp3player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.naicson.alainz_mp3player.MainActivity
import com.naicson.alainz_mp3player.R
import com.naicson.alainz_mp3player.playback.PlaybackService
import kotlin.math.min

/**
 * Home-screen widgets mirroring the current song and transport state. Unlike the
 * notification/lock-screen player (which Media3's MediaSession gives us for free), a launcher
 * widget doesn't exist until something builds it — there's no automatic Android equivalent.
 *
 * Two sizes are offered, matching the layouts the user picked from a reference screenshot: a
 * compact 3x1 row ([MediaWidgetProvider]) and a tall 3x4 hero card ([MediaWidgetProviderLarge]).
 * [PlaybackService] pushes every state change to both via [MediaWidgets.update]; each provider
 * itself only re-renders the last known state (kept in [MediaWidgets]) when its widget host asks
 * for it — a freshly placed widget, a device reboot, etc.
 */
class MediaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = MediaWidgets.buildSmall(context, MediaWidgets.lastState)
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
    }
}

class MediaWidgetProviderLarge : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = MediaWidgets.buildLarge(context, MediaWidgets.lastState)
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
    }
}

object MediaWidgets {
    data class WidgetState(val title: String, val artist: String, val isPlaying: Boolean, val artwork: Bitmap?)

    var lastState = WidgetState(title = "", artist = "", isPlaying = false, artwork = null)
        private set

    fun update(context: Context, title: String, artist: String, isPlaying: Boolean, artwork: Bitmap?) {
        lastState = WidgetState(title, artist, isPlaying, artwork)
        pushUpdate(context, MediaWidgetProvider::class.java) { buildSmall(context, lastState) }
        pushUpdate(context, MediaWidgetProviderLarge::class.java) { buildLarge(context, lastState) }
    }

    private fun pushUpdate(context: Context, providerClass: Class<out AppWidgetProvider>, build: () -> RemoteViews) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, providerClass))
        if (ids.isEmpty()) return
        try {
            manager.updateAppWidget(ids, build())
        } catch (e: Exception) {
            // RemoteViews.setImageViewBitmap ships the bitmap through a Binder transaction —
            // oversized artwork (or just an unlucky moment) can throw TransactionTooLargeException
            // here, and since this runs inside PlaybackService, an uncaught throw would take the
            // whole app down with it on the very next track change, not just skip this refresh.
            Log.w("MediaWidgets", "Failed to update widget", e)
        }
    }

    fun buildSmall(context: Context, state: WidgetState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_media_player)
        bindCommon(context, views, state, includeArtist = false)
        return views
    }

    fun buildLarge(context: Context, state: WidgetState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_media_player_large)
        bindCommon(context, views, state, includeArtist = true)
        return views
    }

    private fun bindCommon(context: Context, views: RemoteViews, state: WidgetState, includeArtist: Boolean) {
        views.setTextViewText(R.id.widget_title, state.title.ifBlank { context.getString(R.string.widget_no_song) })
        if (includeArtist) views.setTextViewText(R.id.widget_artist, state.artist)
        views.setImageViewResource(R.id.widget_play_pause, if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        if (state.artwork != null) {
            views.setImageViewBitmap(R.id.widget_cover, shrinkForWidget(state.artwork))
        } else {
            views.setImageViewResource(R.id.widget_cover, R.drawable.ic_widget_music_note)
        }

        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent)
        views.setOnClickPendingIntent(R.id.widget_prev, actionPendingIntent(context, PlaybackService.ACTION_PREVIOUS, 1))
        views.setOnClickPendingIntent(R.id.widget_play_pause, actionPendingIntent(context, PlaybackService.ACTION_PLAY_PAUSE, 2))
        views.setOnClickPendingIntent(R.id.widget_next, actionPendingIntent(context, PlaybackService.ACTION_NEXT, 3))
    }

    /** [RemoteViews.setImageViewBitmap] ships the bitmap through a Binder transaction (~1MB
     * limit), same as the notification's, but that path already goes through Media3's own
     * scaling; ours doesn't, so it's done by hand here rather than trusting the artwork loader's
     * larger notification-sized cap (480px, which alone can approach the limit). */
    private fun shrinkForWidget(bitmap: Bitmap): Bitmap {
        val maxDimension = 180
        val scale = min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
        if (scale >= 1f) return bitmap
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    /** The widgets share this process with [PlaybackService], so their buttons can just send an
     * explicit intent straight to the already-running service instead of binding a
     * MediaController or going through a MediaButtonReceiver (which on API 26+ only forwards
     * play/pause anyway, not next/previous). */
    private fun actionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, PlaybackService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        // getForegroundService only exists from API 26 on — which is also the first API where
        // starting a plain background service is restricted in the first place, so getService is
        // the correct (and only available) choice below that.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(context, requestCode, intent, flags)
        }
    }
}
