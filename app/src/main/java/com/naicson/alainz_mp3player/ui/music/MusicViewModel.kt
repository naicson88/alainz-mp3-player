package com.naicson.alainz_mp3player.ui.music

import android.content.Intent
import android.content.IntentSender
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naicson.alainz_mp3player.data.local.DeleteOutcome
import com.naicson.alainz_mp3player.data.local.RingtoneAssigner
import com.naicson.alainz_mp3player.data.local.UserPreferencesRepository
import com.naicson.alainz_mp3player.data.local.contentUri
import com.naicson.alainz_mp3player.data.model.Song
import com.naicson.alainz_mp3player.data.model.folderPath
import com.naicson.alainz_mp3player.data.remote.AlbumArtSearchService
import com.naicson.alainz_mp3player.data.repository.MusicRepository
import com.naicson.alainz_mp3player.playback.PlayerConnection
import com.naicson.alainz_mp3player.ui.theme.AccentBlue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
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
@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val player: PlayerConnection,
    private val ringtoneAssigner: RingtoneAssigner,
    private val albumArtSearch: AlbumArtSearchService,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    val accentColor: StateFlow<Color> = preferences.accentColor.stateIn(viewModelScope, SharingStarted.Eagerly, AccentBlue)

    /** One-shot system delete-confirmation prompts the Activity must launch — see `AppRoot`. */
    private val _deleteConfirmationRequests = MutableSharedFlow<IntentSender>(extraBufferCapacity = 1)
    val deleteConfirmationRequests: SharedFlow<IntentSender> = _deleteConfirmationRequests.asSharedFlow()
    private var pendingSystemDeleteSongId: Long? = null

    /** One-shot "grant WRITE_SETTINGS" system-screen prompts the Activity must launch — see `AppRoot`. */
    private val _writeSettingsRequests = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val writeSettingsRequests: SharedFlow<Intent> = _writeSettingsRequests.asSharedFlow()
    private var pendingRingtoneSongId: Long? = null

    private var toastJob: Job? = null
    private var coverSearchJob: Job? = null

    init {
        viewModelScope.launch {
            player.isPlaying.collect { playing ->
                _uiState.update { it.copy(playing = playing) }
                if (!playing) persistPlaybackState()
            }
        }
        viewModelScope.launch {
            player.currentIndex.collect { index ->
                _uiState.update { it.copy(currentIndex = index) }
                persistPlaybackState()
            }
        }
        viewModelScope.launch { player.shuffleEnabled.collect { s -> _uiState.update { it.copy(shuffle = s) } } }
        viewModelScope.launch { player.repeatEnabled.collect { r -> _uiState.update { it.copy(repeat = r) } } }
        viewModelScope.launch { player.volume.collect { v -> _uiState.update { it.copy(volumePercent = v * 100f) } } }
        viewModelScope.launch {
            player.positionMs.collect { pos ->
                val duration = player.durationMs.value
                _uiState.update { it.copy(progressPercent = if (duration > 0) pos.toFloat() / duration * 100f else 0f) }
            }
        }
        // Position updates twice a second while playing — sampled down to every 5s so resuming
        // later doesn't need pinpoint accuracy but also doesn't hammer DataStore on every tick.
        viewModelScope.launch { player.positionMs.sample(5000).collect { persistPlaybackState() } }
    }

    private fun persistPlaybackState() {
        val song = _uiState.value.currentSong ?: return
        viewModelScope.launch { preferences.saveLastPlayback(song.id, player.positionMs.value) }
    }

    /** Triggered once the audio permission is granted — see `RequireAudioPermission`. */
    fun loadSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val songs = repository.getSongs()
            player.setPlaylist(songs)

            // Resume where playback left off last run — paused, not auto-playing, since a
            // freshly (re)launched app shouldn't just start blaring music on its own.
            var restoredIndex = 0
            val lastPlayback = preferences.lastPlayback.first()
            if (lastPlayback != null) {
                val (songId, positionMs) = lastPlayback
                val index = songs.indexOfFirst { it.id == songId }
                if (index >= 0) {
                    restoredIndex = index
                    player.prepareAt(index, positionMs)
                }
            }

            _uiState.update { it.copy(songs = songs, isLoading = false, currentIndex = restoredIndex) }
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
            _uiState.update { it.copy(songs = songs, isRefreshing = false) }
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

    /** Tapping the bottom-nav "List" icon should always land on the list root — closing
     * whatever folder is open, not just switching tabs (which is a no-op if already on List). */
    fun goToList() = _uiState.update { it.copy(activeTab = AppTab.LIST, openFolder = null) }

    fun openSettings() = _uiState.update { it.copy(settingsOpen = true) }
    fun closeSettings() = _uiState.update { it.copy(settingsOpen = false) }
    fun setAccentColor(color: Color) = viewModelScope.launch { preferences.setAccentColor(color) }

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
            player.seekToIndexAndPlay(indices.first())
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
        player.seekToIndexAndPlay(0)
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

    fun openEdit(songId: Long) {
        val song = _uiState.value.songs.firstOrNull { it.id == songId } ?: return
        _uiState.update { s ->
            s.copy(
                editingSongId = songId,
                editForm = EditForm(title = song.title, artist = song.artist, album = song.album, genre = song.genre),
                openMenuSongId = null,
                playerMenuOpen = false,
            )
        }
        // The ID3 fallback is real per-file I/O — too slow to run for the whole library up
        // front (see MediaStoreAudioScanner.readGenreTag), so it only runs here, for the one
        // song actually being edited, and only when nothing already filled the field in.
        if (song.genre.isBlank()) {
            viewModelScope.launch {
                val genre = repository.resolveGenre(song.filePath)
                if (genre.isNotBlank()) {
                    _uiState.update { s -> if (s.editingSongId == songId) s.copy(editForm = s.editForm.copy(genre = genre)) else s }
                }
            }
        }
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
        var updatedIndex = -1
        var updatedSong: Song? = null
        _uiState.update { s ->
            val songs = s.songs.mapIndexed { index, song ->
                if (song.id != id) return@mapIndexed song
                song.copy(title = form.title, artist = form.artist, album = form.album, genre = form.genre).also {
                    updatedIndex = index
                    updatedSong = it
                }
            }
            s.copy(songs = songs, editingSongId = null)
        }
        viewModelScope.launch {
            repository.updateMetadata(id, form.title, form.artist, form.album, form.genre)
            // Otherwise the media notification/lock screen would keep showing the old
            // title/artist until the app is fully reloaded, since it reads from the player's
            // own MediaItem metadata, not from this screen's state.
            updatedSong?.let { song -> player.updateMediaItem(updatedIndex, song) }
        }
    }

    /** Menu action — opens the cover picker pre-searched on the song's own artist/title. */
    fun openChangeCover(songId: Long) {
        val song = _uiState.value.songs.firstOrNull { it.id == songId } ?: return
        _uiState.update {
            it.copy(
                coverPickerSongId = songId,
                coverSearchQuery = "${song.artist} ${song.title}".trim(),
                coverSearchResults = emptyList(),
                coverSearchSelectedUrl = null,
                openMenuSongId = null,
                playerMenuOpen = false,
            )
        }
        runCoverSearch()
    }

    fun setCoverSearchQuery(query: String) = _uiState.update { it.copy(coverSearchQuery = query) }

    fun runCoverSearch() {
        val query = _uiState.value.coverSearchQuery.trim()
        if (query.isEmpty()) return
        coverSearchJob?.cancel()
        coverSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(coverSearchLoading = true, coverSearchResults = emptyList(), coverSearchSelectedUrl = null) }
            val results = albumArtSearch.search(query)
            _uiState.update { it.copy(coverSearchLoading = false, coverSearchResults = results) }
        }
    }

    fun selectCoverResult(url: String) = _uiState.update { it.copy(coverSearchSelectedUrl = url) }

    fun closeCoverPicker() {
        coverSearchJob?.cancel()
        _uiState.update {
            it.copy(coverPickerSongId = null, coverSearchQuery = "", coverSearchResults = emptyList(), coverSearchSelectedUrl = null)
        }
    }

    fun confirmCover() {
        val current = _uiState.value
        val songId = current.coverPickerSongId ?: return
        val url = current.coverSearchSelectedUrl ?: return
        _uiState.update { s ->
            s.copy(
                songs = s.songs.map { if (it.id == songId) it.copy(customCoverUri = url) else it },
                coverPickerSongId = null,
                coverSearchQuery = "",
                coverSearchResults = emptyList(),
                coverSearchSelectedUrl = null,
            )
        }
        viewModelScope.launch { repository.updateCustomCover(songId, url) }
    }

    /** Menu action — writing the ringtone needs WRITE_SETTINGS, a special-access permission
     * only grantable from a system settings screen; see `RingtoneAssigner`. */
    fun setRingtone(songId: Long) {
        _uiState.update { it.copy(openMenuSongId = null, playerMenuOpen = false) }
        if (!ringtoneAssigner.canWrite()) {
            pendingRingtoneSongId = songId
            showToast("Permita a alteração de configurações para definir o toque")
            _writeSettingsRequests.tryEmit(ringtoneAssigner.manageWriteSettingsIntent())
            return
        }
        applyRingtone(songId)
    }

    /** Called once the user returns from the WRITE_SETTINGS screen launched from `AppRoot`. */
    fun onWriteSettingsResult() {
        val songId = pendingRingtoneSongId ?: return
        pendingRingtoneSongId = null
        if (ringtoneAssigner.canWrite()) applyRingtone(songId) else showToast("Permissão não concedida")
    }

    private fun applyRingtone(songId: Long) {
        val song = _uiState.value.songs.firstOrNull { it.id == songId } ?: return
        showToast(if (ringtoneAssigner.setAsRingtone(song.contentUri())) "Definido como toque: ${song.title}" else "Não foi possível definir o toque")
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
