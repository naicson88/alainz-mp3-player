package com.naicson.alainz_mp3player.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.ui.music.MusicUiState
import com.naicson.alainz_mp3player.ui.music.MusicViewModel
import com.naicson.alainz_mp3player.ui.theme.AppTextStyles
import com.naicson.alainz_mp3player.ui.theme.ScreenBackground
import com.naicson.alainz_mp3player.ui.theme.TextHint
import kotlinx.coroutines.launch

@Composable
fun SongListScreen(viewModel: MusicViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val content = remember(state.songs, state.listView, state.openFolder, state.searchOpen, state.searchQuery) {
        buildListContent(state)
    }
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize().background(ScreenBackground)) {
        ListHeader(state = state, viewModel = viewModel)

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            val trimmedQuery = state.searchQuery.trim()
            when {
                state.searchOpen && trimmedQuery.length < 3 -> EmptyHint("Digite ao menos 3 caracteres para buscar")
                state.searchOpen && content is ListContent.SongsOnly && content.songs.isEmpty() ->
                    EmptyHint("Nenhum resultado para \"$trimmedQuery\"")
                else -> ListBody(content = content, state = state, viewModel = viewModel, listState = listState)
            }

            if (content is ListContent.AllSongs && content.groups.isNotEmpty()) {
                AlphabetIndex(content = content, listState = listState, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
            }
        }
    }
}

@Composable
private fun AlphabetIndex(content: ListContent.AllSongs, listState: LazyListState, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    val availableLetters = remember(content) { content.groups.map { it.first }.toSet() }
    val headerIndices = remember(content) { headerFlatIndices(content.groups) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var isDraggingScrollbar by remember { mutableStateOf(false) }

    AlphabetScrollbar(
        visible = listState.isScrollInProgress || isDraggingScrollbar,
        availableLetters = availableLetters,
        activeLetter = activeLetter,
        onDragStateChanged = { isDraggingScrollbar = it },
        onLetterSelected = { letter ->
            val target = nearestAvailableLetter(letter, availableLetters) ?: return@AlphabetScrollbar
            if (target != activeLetter) {
                activeLetter = target
                headerIndices[target]?.let { index -> coroutineScope.launch { listState.scrollToItem(index) } }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun EmptyHint(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp), contentAlignment = Alignment.Center) {
        Text(text, style = AppTextStyles.emptyStateHint, color = TextHint, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListBody(content: ListContent, state: MusicUiState, viewModel: MusicViewModel, listState: LazyListState) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        when (content) {
            is ListContent.AllSongs -> content.groups.forEach { (letter, songs) ->
                stickyHeader { HeaderRow(letter) }
                items(songs, key = { it.value.id }) { indexed ->
                    SongRow(
                        song = indexed.value,
                        isCurrent = indexed.index == state.currentIndex,
                        stripeEven = indexed.index % 2 == 0,
                        menuExpanded = state.openMenuSongId == indexed.value.id,
                        onSelect = { viewModel.selectSong(indexed.index) },
                        onOpenPlayer = { viewModel.selectSongAndOpenPlayer(indexed.index) },
                        onToggleMenu = { viewModel.toggleSongMenu(indexed.value.id) },
                        onDismissMenu = viewModel::closeMenus,
                        onEdit = { viewModel.openEdit(indexed.value.id) },
                        onChangeCover = { viewModel.openChangeCover(indexed.value.id) },
                        onSetRingtone = { viewModel.setRingtone(indexed.value.id) },
                        onDelete = { viewModel.requestDelete(indexed.value.id) },
                    )
                }
            }

            is ListContent.Folders -> items(content.folders, key = { it.path }) { folder ->
                FolderRow(summary = folder, stripeEven = folder.stripeIndex % 2 == 0, onOpen = { viewModel.openFolder(folder.path) })
            }

            is ListContent.SongsOnly -> items(content.songs, key = { it.value.id }) { indexed ->
                SongRow(
                    song = indexed.value,
                    isCurrent = indexed.index == state.currentIndex,
                    stripeEven = indexed.index % 2 == 0,
                    menuExpanded = state.openMenuSongId == indexed.value.id,
                    onSelect = { viewModel.selectSong(indexed.index) },
                    onOpenPlayer = { viewModel.selectSongAndOpenPlayer(indexed.index) },
                    onToggleMenu = { viewModel.toggleSongMenu(indexed.value.id) },
                    onDismissMenu = viewModel::closeMenus,
                    onEdit = { viewModel.openEdit(indexed.value.id) },
                    onChangeCover = { viewModel.openChangeCover(indexed.value.id) },
                    onSetRingtone = { viewModel.setRingtone(indexed.value.id) },
                    onDelete = { viewModel.requestDelete(indexed.value.id) },
                )
            }
        }
    }
}
