package com.naicson.alainz_mp3player.data.repository

import com.naicson.alainz_mp3player.data.local.DeleteOutcome
import com.naicson.alainz_mp3player.data.model.Song

interface MusicRepository {
    /** Scans the device, reconciles with the local cache and returns the merged library. */
    suspend fun getSongs(): List<Song>

    /** Forces a system media-store rescan before doing the same as [getSongs] — see "refresh library". */
    suspend fun refreshLibrary(): List<Song>

    suspend fun updateMetadata(id: Long, title: String, artist: String, album: String, genre: String)
    suspend fun updateCustomCover(id: Long, uri: String?)

    /** Lazy, single-file ID3 genre fallback — see `MediaStoreAudioScanner.readGenreTag`. */
    suspend fun resolveGenre(filePath: String): String

    suspend fun requestDelete(song: Song): DeleteOutcome
    suspend fun finishDelete(song: Song): DeleteOutcome
    suspend fun removeFromCache(id: Long)
}
