package com.naicson.alainz_mp3player.data.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Reads the album art embedded in an audio file's own tags (ID3 `APIC` and friends) via
 * [MediaMetadataRetriever] — this is the fallback used as the default cover once a song has no
 * user-picked one. Decoding is real file I/O plus JPEG/PNG decompression, so results are cached
 * per song for the life of the process; a max dimension keeps a big embedded image (some are
 * several MB) from ballooning memory once decoded.
 *
 * Every visible row in a large library (this app has been tested with 1500+ songs) launches its
 * own [load] as it scrolls into view; [MediaMetadataRetriever] holds native/file-descriptor
 * resources per instance and a fast fling can otherwise pile up dozens of them at once before
 * any of them finish. The semaphore caps how many run concurrently instead.
 */
object EmbeddedArtwork {
    private const val MAX_DIMENSION_PX = 480
    private val cache = LruCache<Long, Bitmap>(40)
    private val concurrencyLimit = Semaphore(4)

    // LruCache.put() throws NullPointerException on a null value — it doesn't support caching
    // "no result" the way a regular Map would. Most real libraries have plenty of songs with no
    // embedded art at all, so this isn't an edge case: it crashed on the very first coverless
    // song whose row scrolled into view (or, via PlaybackService's own artwork lookup, the very
    // first coverless song played). This set remembers those separately instead.
    private val knownMissing = java.util.Collections.synchronizedSet(mutableSetOf<Long>())

    suspend fun load(songId: Long, filePath: String): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(songId)?.let { return@withContext it }
        if (knownMissing.contains(songId)) return@withContext null

        val bitmap = concurrencyLimit.withPermit {
            try {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(filePath)
                    retriever.embeddedPicture?.let(::decodeSampled)
                } finally {
                    retriever.release()
                }
            } catch (e: Exception) {
                null
            }
        }

        if (bitmap != null) cache.put(songId, bitmap) else knownMissing.add(songId)
        bitmap
    }

    private fun decodeSampled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_DIMENSION_PX && bounds.outHeight / (sample * 2) >= MAX_DIMENSION_PX) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}
