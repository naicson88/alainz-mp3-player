package com.naicson.alainz_mp3player.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.naicson.alainz_mp3player.ui.components.BottomNavBar
import com.naicson.alainz_mp3player.ui.components.ChangeCoverDialog
import com.naicson.alainz_mp3player.ui.components.DeleteConfirmDialog
import com.naicson.alainz_mp3player.ui.components.EditSongDialog
import com.naicson.alainz_mp3player.ui.components.MiniPlayer
import com.naicson.alainz_mp3player.ui.components.SettingsDialog
import com.naicson.alainz_mp3player.ui.components.Toast
import com.naicson.alainz_mp3player.ui.list.SongListScreen
import com.naicson.alainz_mp3player.ui.music.AppTab
import com.naicson.alainz_mp3player.ui.music.MusicViewModel
import com.naicson.alainz_mp3player.ui.player.PlayerScreen
import com.naicson.alainz_mp3player.ui.theme.LocalAccentColor
import com.naicson.alainz_mp3player.ui.theme.TextHint

/**
 * Player and List are tab-switched state, not a back-stack — mirrors the design's
 * single `sc-if activeTab` toggle rather than Navigation-Compose destinations.
 */
@Composable
fun AppRoot(viewModel: MusicViewModel = hiltViewModel(), modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSongs() }

    // Declared in the manifest but, unlike dangerous permissions, POST_NOTIFICATIONS (API 33+)
    // is never requested just by needing it — without this the media notification silently
    // never shows up, even though playback itself works fine.
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val deleteConfirmationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onDeleteConfirmationResult(result.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(Unit) {
        viewModel.deleteConfirmationRequests.collect { intentSender ->
            deleteConfirmationLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

    // ACTION_MANAGE_WRITE_SETTINGS never reports a meaningful result code, so we just recheck
    // Settings.System.canWrite() once the user comes back — see MusicViewModel.onWriteSettingsResult.
    val writeSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onWriteSettingsResult()
    }
    LaunchedEffect(Unit) {
        viewModel.writeSettingsRequests.collect { intent -> writeSettingsLauncher.launch(intent) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // enableEdgeToEdge() (MainActivity) draws the whole app behind the status/navigation
        // bars, so content needs its own inset padding — without it, top elements like the
        // folder-view back button sit close enough to the status bar that part of their touch
        // target lands in the system's gesture-detection strip and never reaches the app.
        // Only the status bar is handled here — BottomNavBar handles its own bottom inset
        // internally (see there for why a blanket safeDrawingPadding pushed its icons up).
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> LoadingLibrary()
                    state.songs.isEmpty() -> EmptyLibrary(isRefreshing = state.isRefreshing, onRefresh = viewModel::refreshLibrary)
                    else -> when (state.activeTab) {
                        AppTab.PLAYER -> PlayerScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                        AppTab.LIST -> Column(modifier = Modifier.fillMaxSize()) {
                            SongListScreen(viewModel = viewModel, modifier = Modifier.weight(1f))
                            state.currentSong?.let { song ->
                                MiniPlayer(
                                    song = song,
                                    playing = state.playing,
                                    onOpenPlayer = viewModel::goToPlayer,
                                    onTogglePlay = viewModel::togglePlay,
                                    modifier = Modifier.padding(horizontal = 14.dp).padding(top = 8.dp, bottom = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
            BottomNavBar(activeTab = state.activeTab, onGoToPlayer = viewModel::goToPlayer, onGoToList = viewModel::goToList)
        }

        state.editingSongId?.let {
            EditSongDialog(
                editForm = state.editForm,
                onTitleChange = { viewModel.setEditField(title = it) },
                onArtistChange = { viewModel.setEditField(artist = it) },
                onAlbumChange = { viewModel.setEditField(album = it) },
                onGenreChange = { viewModel.setEditField(genre = it) },
                onCancel = viewModel::cancelEdit,
                onSave = viewModel::saveEdit,
            )
        }

        state.coverPickerSongId?.let {
            ChangeCoverDialog(
                query = state.coverSearchQuery,
                onQueryChange = viewModel::setCoverSearchQuery,
                onSearch = viewModel::runCoverSearch,
                isLoading = state.coverSearchLoading,
                results = state.coverSearchResults,
                selectedUrl = state.coverSearchSelectedUrl,
                onSelectResult = viewModel::selectCoverResult,
                onCancel = viewModel::closeCoverPicker,
                onConfirm = viewModel::confirmCover,
            )
        }

        if (state.settingsOpen) {
            val accentColor by viewModel.accentColor.collectAsState()
            SettingsDialog(currentAccent = accentColor, onSelectAccent = viewModel::setAccentColor, onClose = viewModel::closeSettings)
        }

        state.pendingDeleteSongId?.let { songId ->
            val title = state.songs.firstOrNull { it.id == songId }?.title.orEmpty()
            DeleteConfirmDialog(songTitle = title, onCancel = viewModel::cancelDelete, onConfirm = viewModel::confirmDelete)
        }

        state.toast?.let { message ->
            Toast(message = message, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp))
        }
    }
}

@Composable
private fun LoadingLibrary() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LocalAccentColor.current)
    }
}

@Composable
private fun EmptyLibrary(isRefreshing: Boolean, onRefresh: () -> Unit) {
    val accent = LocalAccentColor.current
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Nenhuma música encontrada no dispositivo",
                color = TextHint,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isRefreshing) {
                CircularProgressIndicator(color = accent)
            } else {
                Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                    Text("Atualizar biblioteca")
                }
            }
        }
    }
}
