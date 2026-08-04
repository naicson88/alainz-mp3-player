package com.naicson.alainz_mp3player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.data.model.Song
import com.naicson.alainz_mp3player.ui.theme.AlainzMp3PlayerTheme
import com.naicson.alainz_mp3player.ui.theme.AppTextStyles
import com.naicson.alainz_mp3player.ui.theme.LocalAccentColor
import com.naicson.alainz_mp3player.ui.theme.TextPrimary
import com.naicson.alainz_mp3player.ui.theme.TextSecondary

private val MiniPlayerGradient = Brush.linearGradient(listOf(Color(0xFF6C4AB6), Color(0xFFB39DDB), Color(0xFFF3EFFB)))

@Preview(showBackground = true, backgroundColor = 0xFF2A2A2A)
@Composable
private fun MiniPlayerPreview() {
    AlainzMp3PlayerTheme {
        MiniPlayer(
            song = Song(id = 1, title = "Título da Música", artist = "Artista Exemplo", album = "Álbum", genre = "Pop", durationSec = 180, filePath = "/music/song.mp3"),
            playing = true,
            onOpenPlayer = {},
            onTogglePlay = {},
        )
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    playing: Boolean,
    onOpenPlayer: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccentColor.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MiniPlayerGradient)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, accent.copy(alpha = 0.33f), RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenPlayer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(song = song, modifier = Modifier.size(44.dp), shape = RoundedCornerShape(8.dp), iconFraction = 0.45f)

        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = AppTextStyles.miniPlayerTitle, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = AppTextStyles.miniPlayerSubtitle, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent)
                .clickable(onClick = onTogglePlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pausar" else "Tocar",
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}
