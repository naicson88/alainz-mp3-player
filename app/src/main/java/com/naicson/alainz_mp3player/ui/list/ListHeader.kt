package com.naicson.alainz_mp3player.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.data.model.folderDisplayName
import com.naicson.alainz_mp3player.ui.components.ShuffleOrderButtons
import com.naicson.alainz_mp3player.ui.music.ListView
import com.naicson.alainz_mp3player.ui.music.MusicUiState
import com.naicson.alainz_mp3player.ui.music.MusicViewModel
import com.naicson.alainz_mp3player.ui.theme.AppTextStyles
import com.naicson.alainz_mp3player.ui.theme.Border
import com.naicson.alainz_mp3player.ui.theme.ElevatedSurface
import com.naicson.alainz_mp3player.ui.theme.LocalAccentColor
import com.naicson.alainz_mp3player.ui.theme.TextFaint
import com.naicson.alainz_mp3player.ui.theme.TextMuted
import com.naicson.alainz_mp3player.ui.theme.TextPrimary
import kotlin.math.roundToInt

@Composable
fun ListHeader(state: MusicUiState, viewModel: MusicViewModel, modifier: Modifier = Modifier) {
    val openFolder = state.openFolder
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        if (state.listView == ListView.FOLDERS && openFolder != null) {
            FolderDetailHeader(state, openFolder, viewModel)
        } else {
            if (state.searchOpen) {
                SearchRow(query = state.searchQuery, onQueryChange = viewModel::setSearchQuery, onClose = viewModel::closeSearch)
            } else {
                NormalHeader(
                    isRefreshing = state.isRefreshing,
                    onOpenSearch = viewModel::openSearch,
                    onRefresh = viewModel::refreshLibrary,
                    onOpenSettings = viewModel::openSettings,
                )
                TabsRow(listView = state.listView, onSongsTab = viewModel::selectSongsTab, onFoldersTab = viewModel::selectFoldersTab)
                if (state.listView == ListView.SONGS) {
                    CountAndPlayRow(
                        label = if (state.songs.isEmpty()) "" else "${state.currentIndex + 1}/${state.songs.size}",
                        onShuffle = viewModel::playAllShuffle,
                        onOrder = viewModel::playAllOrder,
                    )
                }
            }
            val matches = state.searchMatches()
            if (state.searchOpen && state.searchQuery.trim().length >= 3 && matches.isNotEmpty()) {
                CountAndPlayRow(
                    label = if (matches.size == 1) "1 música encontrada" else "${matches.size} músicas encontradas",
                    onShuffle = { viewModel.playSearch(true) },
                    onOrder = { viewModel.playSearch(false) },
                )
            }
        }
    }
}

@Composable
private fun NormalHeader(isRefreshing: Boolean, onOpenSearch: () -> Unit, onRefresh: () -> Unit, onOpenSettings: () -> Unit) {
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("My Songs", style = AppTextStyles.screenTitle, color = TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .clickable(enabled = !isRefreshing, onClick = onRefresh),
                contentAlignment = Alignment.Center,
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = "Atualizar biblioteca", tint = TextPrimary, modifier = Modifier.size(17.dp))
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = TextPrimary, modifier = Modifier.size(17.dp))
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Configurações", tint = TextPrimary, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun SearchRow(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .background(ElevatedSurface, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            if (query.isEmpty()) {
                Text("Buscar por música ou artista", style = AppTextStyles.modalInputText, color = TextFaint)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle.Default.copy(color = TextPrimary, fontSize = AppTextStyles.modalInputText.fontSize),
                singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(TextPrimary),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Fechar busca", tint = TextPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TabsRow(listView: ListView, onSongsTab: () -> Unit, onFoldersTab: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TabPill(label = "Músicas", selected = listView == ListView.SONGS, onClick = onSongsTab, modifier = Modifier.weight(1f))
        TabPill(label = "Pastas", selected = listView == ListView.FOLDERS, onClick = onFoldersTab, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccentColor.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (selected) accent else Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = AppTextStyles.pillButtonLabel, color = TextPrimary)
    }
}

@Composable
private fun CountAndPlayRow(label: String, onShuffle: () -> Unit, onOrder: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = AppTextStyles.listSummary, color = TextMuted)
        ShuffleOrderButtons(onShuffle = onShuffle, onOrder = onOrder)
    }
}

@Composable
private fun FolderDetailHeader(state: MusicUiState, folder: String, viewModel: MusicViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = viewModel::closeFolderView) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
        }
        Text(folderDisplayName(folder), style = AppTextStyles.folderTitle, color = TextPrimary, maxLines = 1)
    }
    val folderSongs = state.folderSongs(folder)
    val totalMin = (folderSongs.sumOf { it.durationSec } / 60f).roundToInt()
    val countLabel = if (folderSongs.size == 1) "1 música" else "${folderSongs.size} músicas"
    CountAndPlayRow(
        label = "$countLabel • $totalMin min",
        onShuffle = { viewModel.playFolder(folder, true) },
        onOrder = { viewModel.playFolder(folder, false) },
    )
}
