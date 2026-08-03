package com.naicson.alainz_mp3player.ui.music

import com.naicson.alainz_mp3player.data.model.Song
import com.naicson.alainz_mp3player.data.model.folderPath

enum class AppTab { PLAYER, LIST }
enum class ListView { SONGS, FOLDERS }

data class EditForm(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val genre: String = "",
)

data class MusicUiState(
    val activeTab: AppTab = AppTab.PLAYER,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val songs: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
    val playing: Boolean = false,
    val progressPercent: Float = 0f,
    val volumePercent: Float = 100f,
    val openMenuSongId: Long? = null,
    val playerMenuOpen: Boolean = false,
    val editingSongId: Long? = null,
    val editForm: EditForm = EditForm(),
    val coverPickerSongId: Long? = null,
    val coverSearchSelectedIndex: Int? = null,
    /** songId -> chosen result index, standing in for a picked cover image. */
    val customCovers: Map<Long, Int> = emptyMap(),
    val toast: String? = null,
    val listView: ListView = ListView.SONGS,
    /** Folder identity is the real device directory path, not the album. */
    val openFolder: String? = null,
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val pendingDeleteSongId: Long? = null,
) {
    val currentSong: Song? get() = songs.getOrNull(currentIndex)

    fun folderSongs(folderPath: String): List<Song> = songs.filter { it.folderPath == folderPath }

    fun searchMatches(): List<IndexedValue<Song>> {
        val q = searchQuery.trim().lowercase()
        if (q.length < 3) return emptyList()
        return songs.withIndex().filter { (_, s) -> s.title.lowercase().contains(q) || s.artist.lowercase().contains(q) }
    }
}
