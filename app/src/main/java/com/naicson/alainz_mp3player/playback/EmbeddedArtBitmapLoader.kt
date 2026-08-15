package com.naicson.alainz_mp3player.playback

import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.naicson.alainz_mp3player.data.local.EmbeddedArtwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The media notification and lock-screen artwork come from `MediaMetadata.artworkUri`, which
 * Media3 resolves through the session's [BitmapLoader] — lazily, only for whatever item is
 * currently relevant, unlike building bitmaps eagerly for a whole playlist. Anything using the
 * `embedded-art` scheme (see `toMediaItem`) is decoded from the song's own file tags here;
 * everything else (http(s) cover-search URLs, etc.) is handed to [delegate].
 */
class EmbeddedArtBitmapLoader(private val delegate: BitmapLoader) : BitmapLoader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = delegate.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        if (!uri.isEmbeddedArtUri()) return delegate.loadBitmap(uri)

        val future = SettableFuture.create<Bitmap>()
        val songId = uri.authority?.toLongOrNull()
        val filePath = uri.getQueryParameter("path")
        if (songId == null || filePath == null) {
            future.setException(IllegalArgumentException("Invalid embedded-art uri: $uri"))
            return future
        }
        scope.launch {
            val bitmap = EmbeddedArtwork.load(songId, filePath)
            if (bitmap != null) future.set(bitmap) else future.setException(IllegalStateException("No embedded artwork for $uri"))
        }
        return future
    }
}
