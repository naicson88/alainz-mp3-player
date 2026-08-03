package com.naicson.alainz_mp3player.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SongDao {
    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<SongEntity>

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id NOT IN (:keepIds)")
    suspend fun deleteMissing(keepIds: List<Long>)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE songs SET title = :title, artist = :artist, album = :album, genre = :genre WHERE id = :id")
    suspend fun updateMetadata(id: Long, title: String, artist: String, album: String, genre: String)

    @Query("UPDATE songs SET customCoverVariant = :variant WHERE id = :id")
    suspend fun updateCover(id: Long, variant: Int?)
}
