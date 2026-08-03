package com.naicson.alainz_mp3player.data.local

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.naicson.alainz_mp3player.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Raw read of the device's audio library via [MediaStore] — no caching, no persisted edits.
 * `DATA` (the absolute file path) is deprecated for direct file I/O on API 29+, but reading it
 * as a plain column purely to group songs into folders is still supported and the simplest
 * option, since we're not opening the file this way.
 */
@Singleton
class MediaStoreAudioScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * A freshly downloaded file (e.g. via the browser) isn't in [MediaStore] until something
     * scans it — that scan can lag well behind the download, especially on emulators. Asking
     * [MediaScannerConnection] to (re)scan the whole shared storage tree forwards to the
     * system's own scanDirectory pass, so [scan] sees new files immediately afterwards.
     */
    suspend fun rescanDevice() = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<Unit> { cont ->
            MediaScannerConnection.scanFile(
                context,
                arrayOf(Environment.getExternalStorageDirectory().path),
                null,
            ) { _, _ -> if (cont.isActive) cont.resume(Unit) }
        }
    }

    suspend fun scan(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val hasGenreColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATA)
            if (hasGenreColumn) add(MediaStore.Audio.Media.GENRE)
        }.toTypedArray()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val genreCol = if (hasGenreColumn) cursor.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1

            while (cursor.moveToNext()) {
                songs += Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol) ?: "Sem título",
                    artist = cursor.getString(artistCol)?.takeUnless { it == "<unknown>" } ?: "Artista desconhecido",
                    album = cursor.getString(albumCol).orEmpty(),
                    genre = (if (genreCol >= 0) cursor.getString(genreCol) else null).orEmpty(),
                    durationSec = (cursor.getLong(durationCol) / 1000).toInt(),
                    filePath = cursor.getString(dataCol).orEmpty(),
                )
            }
        }
        songs
    }
}
