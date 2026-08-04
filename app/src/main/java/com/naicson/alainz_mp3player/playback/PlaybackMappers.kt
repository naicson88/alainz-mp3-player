package com.naicson.alainz_mp3player.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.naicson.alainz_mp3player.data.local.contentUri
import com.naicson.alainz_mp3player.data.model.Song

/** Scheme carrying a song's id/file path through a [Uri] so [EmbeddedArtBitmapLoader] can
 * resolve it lazily — there's no real fetchable resource for artwork embedded in an audio
 * file's own tags, unlike a plain http(s) cover URL. */
private const val EMBEDDED_ART_SCHEME = "embedded-art"

fun embeddedArtUri(songId: Long, filePath: String): Uri =
    Uri.Builder().scheme(EMBEDDED_ART_SCHEME).authority(songId.toString()).appendQueryParameter("path", filePath).build()

fun Uri.isEmbeddedArtUri(): Boolean = scheme == EMBEDDED_ART_SCHEME

fun Song.toMediaItem(): MediaItem {
    // A user-picked cover (from the search) is a plain http(s) image URL Media3's default
    // loader can fetch directly; otherwise fall back to the file's own embedded artwork.
    val artworkUri = customCoverUri?.let(Uri::parse) ?: embeddedArtUri(id, filePath)
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(artworkUri)
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(contentUri())
        .setMediaMetadata(metadata)
        .build()
}
