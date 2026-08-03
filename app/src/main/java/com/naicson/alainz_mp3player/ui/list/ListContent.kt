package com.naicson.alainz_mp3player.ui.list

import com.naicson.alainz_mp3player.data.model.Song
import com.naicson.alainz_mp3player.data.model.folderPath
import com.naicson.alainz_mp3player.ui.music.ListView
import com.naicson.alainz_mp3player.ui.music.MusicUiState

data class FolderSummary(val path: String, val count: Int, val stripeIndex: Int)

/** What the list body should render, mirroring the design's buildListItems/buildFolderRows/buildSearchItems split. */
sealed interface ListContent {
    data class AllSongs(val groups: List<Pair<String, List<IndexedValue<Song>>>>) : ListContent
    data class Folders(val folders: List<FolderSummary>) : ListContent
    data class SongsOnly(val songs: List<IndexedValue<Song>>) : ListContent
}

fun buildListContent(state: MusicUiState): ListContent {
    if (state.searchOpen) {
        return ListContent.SongsOnly(state.searchMatches())
    }
    return when (state.listView) {
        ListView.FOLDERS -> {
            val openFolder = state.openFolder
            if (openFolder != null) {
                ListContent.SongsOnly(state.songs.withIndex().filter { it.value.folderPath == openFolder })
            } else {
                val folders = state.songs.withIndex()
                    .groupBy { it.value.folderPath }
                    .entries
                    .mapIndexed { i, entry -> FolderSummary(entry.key, entry.value.size, i) }
                ListContent.Folders(folders)
            }
        }
        ListView.SONGS -> {
            val groups = LinkedHashMap<String, MutableList<IndexedValue<Song>>>()
            state.songs.withIndex().forEach { indexed ->
                val letter = indexed.value.title.take(1).uppercase()
                groups.getOrPut(letter) { mutableListOf() }.add(indexed)
            }
            ListContent.AllSongs(groups.map { it.key to it.value })
        }
    }
}
