package com.naicson.alainz_mp3player.ui.music

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naicson.alainz_mp3player.data.local.DeleteOutcome
import com.naicson.alainz_mp3player.data.model.folderPath
import com.naicson.alainz_mp3player.data.repository.MusicRepository
import com.naicson.alainz_mp3player.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single shared view model behind both the Player and List tabs — ported from the
 * claude.ai/design prototype, which likewise kept one state object for the whole app
 * (current song, transport, menus, edit/cover modals, search, toast). Transport state
 * (playing, position, shuffle, repeat, volume, current index) is mirrored from the real
 * [PlayerConnection] rather than simulated.
 */
@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val player: PlayerConnection,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    /** One-shot system delete-confirmation prompts the Activity must launch — see `AppRoot`. */
    private val _deleteConfirmationRequests = MutableSharedFlow<IntentSender>(extraBufferCapacity = 1)
    val deleteConfirmationRequests: SharedFlow<IntentSender> = _deleteConfirmationRequests.asSharedFlow()
    private var pendingSystemDeleteSongId: Long? = null

    private var toastJob: Job? = null

    init {
        viewModelScope.launch { player.isPlaying.collect { playing -> _uiState.update { it.copy(playing = playing) } } }
        viewModelScope.launch { player.currentIndex.collect { index -> _uiState.update { it.copy(currentIndex = index) } } }
        viewModelScope.launch { player.shuffleEnabled.collect { s -> _uiState.update { it.copy(shuffle = s) } } }
        viewModelScope.launch { player.repeatEnabled.collect { r -> _uiState.update { it.copy(repeat = r) } } }
        viewModelScope.launch { player.volume.collect { v -> _uiState.update { it.copy(volumePercent = v * 100f) } } }
        viewModelScope.launch {
            player.positionMs.collect { pos ->
                val duration = player.durationMs.value
                _uiState.update { it.copy(progressPercent = if (duration > 0) pos.toFloat() / duration * 100f else 0f) }
            }
        }
    }

    /** Triggered once the audio permission is granted — see `RequireAudioPermission`. */
    fun loadSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val songs = repository.getSongs()
            val customCovers = songs.mapNotNull { song -> song.customCoverVariant?.let { song.id to it } }.toMap()
            _uiState.update { it.copy(songs = songs, customCovers = customCovers, isLoading = false, currentIndex = 0) }
            player.setPlaylist(songs)
        }
    }

    /** Manual "refresh library" trigger — forces a media-store rescan so freshly downloaded
     * files show up without the user having to reboot or wait on the system scanner. */
    fun refreshLibrary() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val before = _uiState.value.songs.size
            val songs = repository.refreshLibrary()
            val customCovers = songs.mapNotNull { song -> song.customCoverVariant?.let { song.id to it } }.toMap()
            _uiState.update { it.copy(songs = songs, customCovers = customCovers, isRefreshing = false) }
            player.setPlaylist(songs)
            val added = songs.size - before
            showToast(
                when {
                    added > 0 -> if (added == 1) "1 nova música encontrada" else "$added novas músicas encontradas"
                    else -> "Biblioteca já está atualizada"
                },
            )
        }
    }

    fun goToPlayer() = _uiState.update { it.copy(activeTab = AppTab.PLAYER) }
    fun goToList() = _uiState.update { it.copy(activeTab = AppTab.LIST) }

    fun toggleShuffle() = viewModelScope.launch { player.setShuffle(!_uiState.value.shuffle) }
    fun toggleRepeat() = viewModelScope.launch { player.setRepeat(!_uiState.value.repeat) }
    fun togglePlay() = viewModelScope.launch { player.togglePlay() }

    fun prevSong() = viewModelScope.launch { player.previous() }
    fun nextSong() = viewModelScope.launch { player.next() }

    fun selectSong(index: Int) = viewModelScope.launch { player.playAt(index) }

    fun selectSongAndOpenPlayer(index: Int) {
        _uiState.update { it.copy(activeTab = AppTab.PLAYER) }
        viewModelScope.launch { player.playAt(index) }
    }

    fun seekTo(percent: Float) = viewModelScope.launch { player.seekToPercent(percent) }
    fun setVolume(percent: Float) = viewModelScope.launch { player.setVolume(percent) }
    fun skipBack10() = viewModelScope.launch { player.seekByMs(-10_000) }
    fun skipForward10() = viewModelScope.launch { player.seekByMs(10_000) }

    fun toggleSongMenu(songId: Long) = _uiState.update { it.copy(openMenuSongId = if (it.openMenuSongId == songId) null else songId) }
    fun togglePlayerMenu() = _uiState.update { it.copy(playerMenuOpen = !it.playerMenuOpen) }
    fun closeMenus() = _uiState.update { it.copy(openMenuSongId = null, playerMenuOpen = false) }

    fun selectSongsTab() = _uiState.update { it.copy(listView = ListView.SONGS, openFolder = null) }
    fun selectFoldersTab() = _uiState.update { it.copy(listView = ListView.FOLDERS, openFolder = null) }
    fun openFolder(name: String) = _uiState.update { it.copy(openFolder = name) }
    fun closeFolderView() = _uiState.update { it.copy(openFolder = null) }

    fun openSearch() = _uiState.update { it.copy(searchOpen = true) }
    fun closeSearch() = _uiState.update { it.copy(searchOpen = false, searchQuery = "") }
    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun playFolder(folderPath: String, shuffleMode: Boolean) = viewModelScope.launch {
        val songs = _uiState.value.songs
        val indices = songs.indices.filter { songs[it].folderPath == folderPath }
        if (indices.isEmpty()) return@launch
        if (!shuffleMode) {
            player.setShuffle(false)
            player.seekToStartAndPlay()
            return@launch
        }
        player.setShuffle(true)
        player.playAt(indices.random())
    }

    fun playAllShuffle() = viewModelScope.launch {
        val songs = _uiState.value.songs
        if (songs.isEmpty()) return@launch
        player.setShuffle(true)
        player.playAt(songs.indices.random())
    }

    fun playAllOrder() = viewModelScope.launch {
        player.setShuffle(false)
        player.seekToStartAndPlay()
    }

    fun playSearch(shuffleMode: Boolean) = viewModelScope.launch {
        val state = _uiState.value
        val indices = state.searchMatches().map { it.index }
        if (indices.isEmpty()) return@launch
        if (shuffleMode) {
            player.setShuffle(true)
            player.playAt(indices.random())
        } else {
            player.setShuffle(false)
            player.playAt(if (state.currentIndex in indices) state.currentIndex else indices.first())
        }
    }

    fun openEdit(songId: Long) = _uiState.update { s ->
        val song = s.songs.firstOrNull { it.id == songId } ?: return@update s
        s.copy(
            editingSongId = songId,
            editForm = EditForm(title = song.title, artist = song.artist, album = song.album, genre = song.genre),
            openMenuSongId = null,
            playerMenuOpen = false,
        )
    }

    fun cancelEdit() = _uiState.update { it.copy(editingSongId = null) }

    fun setEditField(title: String? = null, artist: String? = null, album: String? = null, genre: String? = null) {
        _uiState.update {
            it.copy(
                editForm = it.editForm.copy(
                    title = title ?: it.editForm.title,
                    artist = artist ?: it.editForm.artist,
                    album = album ?: it.editForm.album,
                    genre = genre ?: it.editForm.genre,
                ),
            )
        }
    }

    fun saveEdit() {
        val current = _uiState.value
        val id = current.editingSongId ?: return
        val form = current.editForm
        _uiState.update { s ->
            s.copy(
                songs = s.songs.map { song ->
                    if (song.id == id) song.copy(title = form.title, artist = form.artist, album = form.album, genre = form.genre)
                    else song
                },
                editingSongId = null,
            )
        }
        viewModelScope.launch { repository.updateMetadata(id, form.title, form.artist, form.album, form.genre) }
    }

    fun openChangeCover(songId: Long) = _uiState.update {
        it.copy(coverPickerSongId = songId, coverSearchSelectedIndex = null, openMenuSongId = null, playerMenuOpen = false)
    }

    fun closeCoverPicker() = _uiState.update { it.copy(coverPickerSongId = null, coverSearchSelectedIndex = null) }
    fun selectCoverResult(index: Int) = _uiState.update { it.copy(coverSearchSelectedIndex = index) }

    fun confirmCover() {
        val current = _uiState.value
        val songId = current.coverPickerSongId ?: return
        val resultIndex = current.coverSearchSelectedIndex ?: return
        _uiState.update { s ->
            s.copy(customCovers = s.customCovers + (songId to resultIndex), coverPickerSongId = null, coverSearchSelectedIndex = null)
        }
        viewModelScope.launch { repository.updateCustomCover(songId, resultIndex) }
    }

    fun setRingtone(songId: Long) {
        val title = _uiState.value.songs.firstOrNull { it.id == songId }?.title ?: return
        _uiState.update { it.copy(openMenuSongId = null, playerMenuOpen = false) }
        showToast("Definido como toque: $title")
    }

    fun requestDelete(songId: Long) = _uiState.update { it.copy(pendingDeleteSongId = songId, openMenuSongId = null, playerMenuOpen = false) }
    fun cancelDelete() = _uiState.update { it.copy(pendingDeleteSongId = null) }

    /** User confirmed the in-app "Excluir" dialog — now ask the OS to actually delete the file. */
    fun confirmDelete() {
        val song = _uiState.value.let { s -> s.songs.firstOrNull { it.id == s.pendingDeleteSongId } } ?: return
        _uiState.update { it.copy(pendingDeleteSongId = null) }
        viewModelScope.launch {
            when (val outcome = repository.requestDelete(song)) {
                DeleteOutcome.Deleted -> finalizeDelete(song.id)
                is DeleteOutcome.RequiresConfirmation -> {
                    pendingSystemDeleteSongId = song.id
                    _deleteConfirmationRequests.emit(outcome.intentSender)
                }
                is DeleteOutcome.Failed -> showToast(outcome.message)
            }
        }
    }

    /** Result of the system delete-confirmation dialog launched from `AppRoot`. */
    fun onDeleteConfirmationResult(confirmed: Boolean) {
        val songId = pendingSystemDeleteSongId ?: return
        pendingSystemDeleteSongId = null
        if (!confirmed) return
        val song = _uiState.value.songs.firstOrNull { it.id == songId } ?: return
        viewModelScope.launch {
            when (val outcome = repository.finishDelete(song)) {
                DeleteOutcome.Deleted -> finalizeDelete(songId)
                is DeleteOutcome.Failed -> showToast(outcome.message)
                is DeleteOutcome.RequiresConfirmation -> Unit
            }
        }
    }

    private fun finalizeDelete(songId: Long) {
        val removedIndex = _uiState.value.songs.indexOfFirst { it.id == songId }
        _uiState.update { s ->
            val songs = s.songs.filterNot { it.id == songId }
            var index = s.currentIndex
            if (removedIndex in 0..s.currentIndex && index > 0) index -= 1
            if (index >= songs.size) index = (songs.size - 1).coerceAtLeast(0)
            s.copy(songs = songs, currentIndex = index)
        }
        viewModelScope.launch {
            repository.removeFromCache(songId)
            if (removedIndex >= 0) player.removeAt(removedIndex)
        }
    }

    private fun showToast(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toast = message) }
        toastJob = viewModelScope.launch {
            delay(2200)
            _uiState.update { it.copy(toast = null) }
        }
    }
}
