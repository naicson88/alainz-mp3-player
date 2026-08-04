package com.naicson.alainz_mp3player.data.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationSec: Int,
    val filePath: String,
    /** Content URI of a user-picked cover (via the system photo picker), persisted in Room;
     * null until the user picks one, in which case it takes priority over embedded artwork. */
    val customCoverUri: String? = null,
)

/** The directory the file lives in — used to group songs into "folders" in the List tab. */
val Song.folderPath: String
    get() = filePath.substringBeforeLast('/', filePath)

fun folderDisplayName(path: String): String =
    path.trimEnd('/').substringAfterLast('/').ifBlank { path }
