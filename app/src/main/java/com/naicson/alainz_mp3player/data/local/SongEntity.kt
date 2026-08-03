package com.naicson.alainz_mp3player.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.naicson.alainz_mp3player.data.model.Song

/**
 * Cached mirror of a device song, keyed by the MediaStore `_ID`. Title/artist/album/genre and
 * the custom cover survive across app launches; duration/filePath are refreshed from the
 * device on every scan since the user never edits those.
 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationSec: Int,
    val filePath: String,
    val customCoverVariant: Int? = null,
)

fun SongEntity.toSong() = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    genre = genre,
    durationSec = durationSec,
    filePath = filePath,
    customCoverVariant = customCoverVariant,
)
