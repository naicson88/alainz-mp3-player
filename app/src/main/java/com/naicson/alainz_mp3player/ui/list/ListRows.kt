package com.naicson.alainz_mp3player.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.data.model.Song
import com.naicson.alainz_mp3player.data.model.folderDisplayName
import com.naicson.alainz_mp3player.ui.components.CoverArt
import com.naicson.alainz_mp3player.ui.components.SongOptionsMenu
import com.naicson.alainz_mp3player.ui.theme.AccentGold
import com.naicson.alainz_mp3player.ui.theme.AlainzMp3PlayerTheme
import com.naicson.alainz_mp3player.ui.theme.AppTextStyles
import com.naicson.alainz_mp3player.ui.theme.ElevatedSurface
import com.naicson.alainz_mp3player.ui.theme.FolderFront
import com.naicson.alainz_mp3player.ui.theme.PopoverSurface
import com.naicson.alainz_mp3player.ui.theme.SectionHeaderBackground
import com.naicson.alainz_mp3player.ui.theme.TextFaint
import com.naicson.alainz_mp3player.ui.theme.TextHint
import com.naicson.alainz_mp3player.ui.theme.TextMuted
import com.naicson.alainz_mp3player.ui.theme.TextPrimary

const val ListRowHeight = 62

private val PreviewSong = Song(id = 1, title = "Título da Música", artist = "Artista Exemplo", album = "Álbum", genre = "Pop", durationSec = 215, filePath = "/music/song.mp3")

@Preview(showBackground = true, backgroundColor = 0xFF2A2A2A)
@Composable
private fun HeaderRowPreview() {
    AlainzMp3PlayerTheme {
        HeaderRow(letter = "M")
    }
}

@Composable
fun HeaderRow(letter: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().background(SectionHeaderBackground).padding(horizontal = 20.dp, vertical = 6.dp)) {
        Text(letter, style = AppTextStyles.sectionLetter, color = TextHint)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2A2A2A)
@Composable
private fun SongRowPreview() {
    AlainzMp3PlayerTheme {
        SongRow(
            song = PreviewSong,
            isCurrent = true,
            stripeEven = true,
            menuExpanded = false,
            onSelect = {},
            onOpenPlayer = {},
            onToggleMenu = {},
            onDismissMenu = {},
            onEdit = {},
            onChangeCover = {},
            onSetRingtone = {},
            onDelete = {},
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    stripeEven: Boolean,
    menuExpanded: Boolean,
    onSelect: () -> Unit,
    onOpenPlayer: () -> Unit,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onEdit: () -> Unit,
    onChangeCover: () -> Unit,
    onSetRingtone: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ListRowHeight.dp)
            .background(if (stripeEven) ElevatedSurface else PopoverSurface)
            .combinedClickable(onClick = onSelect, onDoubleClick = onOpenPlayer)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoverArt(song = song, modifier = Modifier.size(40.dp), shape = RoundedCornerShape(4.dp), iconFraction = 0.45f)

        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = AppTextStyles.rowTitle, color = if (isCurrent) AccentGold else TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = AppTextStyles.rowSubtitle, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Text(formatDuration(song.durationSec), style = AppTextStyles.rowDuration, color = TextFaint)

        Box {
            IconButton(onClick = onToggleMenu) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Opções", tint = TextMuted)
            }
            SongOptionsMenu(
                expanded = menuExpanded,
                onDismiss = onDismissMenu,
                onEdit = onEdit,
                onChangeCover = onChangeCover,
                onSetRingtone = onSetRingtone,
                onDelete = onDelete,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2A2A2A)
@Composable
private fun FolderRowPreview() {
    AlainzMp3PlayerTheme {
        FolderRow(summary = FolderSummary(path = "/music/Rock", count = 12, stripeIndex = 0), stripeEven = true, onOpen = {})
    }
}

@Composable
fun FolderRow(summary: FolderSummary, stripeEven: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ListRowHeight.dp)
            .background(if (stripeEven) ElevatedSurface else PopoverSurface)
            .clickable(onClick = onOpen)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(ElevatedSurface), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = FolderFront, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(folderDisplayName(summary.path), style = AppTextStyles.rowTitle, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (summary.count == 1) "1 música" else "${summary.count} músicas",
                style = AppTextStyles.rowSubtitle,
                color = TextMuted,
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextFaint)
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
