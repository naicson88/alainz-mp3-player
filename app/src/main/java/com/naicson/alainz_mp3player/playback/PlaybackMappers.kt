package com.naicson.alainz_mp3player.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.naicson.alainz_mp3player.data.local.contentUri
import com.naicson.alainz_mp3player.data.model.Song

fun Song.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(contentUri())
        .setMediaMetadata(metadata)
        .build()
}
