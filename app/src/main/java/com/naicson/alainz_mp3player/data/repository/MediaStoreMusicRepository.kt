package com.naicson.alainz_mp3player.data.repository

import com.naicson.alainz_mp3player.data.local.DeleteOutcome
import com.naicson.alainz_mp3player.data.local.MediaDeleter
import com.naicson.alainz_mp3player.data.local.MediaStoreAudioScanner
import com.naicson.alainz_mp3player.data.local.SongDao
import com.naicson.alainz_mp3player.data.local.SongEntity
import com.naicson.alainz_mp3player.data.local.contentUri
import com.naicson.alainz_mp3player.data.local.toSong
import com.naicson.alainz_mp3player.data.model.Song
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Merges a fresh [MediaStoreAudioScanner] pass with the Room cache: user edits (title, artist,
 * album, genre) and the custom cover survive a rescan for songs still on the device; songs
 * removed from the device are pruned from the cache; songs seen for the first time are
 * inserted with their file-tag values.
 */
@Singleton
class MediaStoreMusicRepository @Inject constructor(
    private val scanner: MediaStoreAudioScanner,
    private val dao: SongDao,
    private val deleter: MediaDeleter,
) : MusicRepository {

    override suspend fun refreshLibrary(): List<Song> {
        scanner.rescanDevice()
        return getSongs()
    }

    override suspend fun getSongs(): List<Song> {
        val deviceSongs = scanner.scan()
        val cached = dao.getAll().associateBy { it.id }

        val merged = deviceSongs.map { device ->
            val existing = cached[device.id]
            if (existing != null) {
                existing.copy(durationSec = device.durationSec, filePath = device.filePath)
            } else {
                SongEntity(
                    id = device.id,
                    title = device.title,
                    artist = device.artist,
                    album = device.album,
                    genre = device.genre,
                    durationSec = device.durationSec,
                    filePath = device.filePath,
                )
            }
        }

        dao.upsertAll(merged)
        dao.deleteMissing(merged.map { it.id })
        return merged.map { it.toSong() }
    }

    override suspend fun updateMetadata(id: Long, title: String, artist: String, album: String, genre: String) {
        dao.updateMetadata(id, title, artist, album, genre)
    }

    override suspend fun updateCustomCover(id: Long, uri: String?) {
        dao.updateCover(id, uri)
    }

    override suspend fun resolveGenre(filePath: String): String = scanner.readGenreTag(filePath)

    override suspend fun requestDelete(song: Song): DeleteOutcome = deleter.requestDelete(song.contentUri())

    override suspend fun finishDelete(song: Song): DeleteOutcome = deleter.finishAfterConfirmation(song.contentUri())

    override suspend fun removeFromCache(id: Long) {
        dao.deleteById(id)
    }
}
