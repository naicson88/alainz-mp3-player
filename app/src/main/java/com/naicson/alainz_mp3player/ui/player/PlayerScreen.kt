package com.naicson.alainz_mp3player.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.ui.components.CoverArt
import com.naicson.alainz_mp3player.ui.components.SongOptionsMenu
import com.naicson.alainz_mp3player.ui.music.MusicViewModel
import com.naicson.alainz_mp3player.ui.theme.AppTextStyles
import com.naicson.alainz_mp3player.ui.theme.ElevatedSurface
import com.naicson.alainz_mp3player.ui.theme.LocalAccentColor
import com.naicson.alainz_mp3player.ui.theme.PlayerGradientEnd
import com.naicson.alainz_mp3player.ui.theme.PlayerGradientMid
import com.naicson.alainz_mp3player.ui.theme.PlayerGradientStart
import com.naicson.alainz_mp3player.ui.theme.TextFaint
import com.naicson.alainz_mp3player.ui.theme.TextMuted
import com.naicson.alainz_mp3player.ui.theme.TextPrimary
import com.naicson.alainz_mp3player.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(viewModel: MusicViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val song = state.currentSong ?: return
    val accent = LocalAccentColor.current
    val customCover = state.customCovers[song.id]?.let { song.id to it }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PlayerGradientStart, PlayerGradientMid, PlayerGradientEnd)))
            .background(Color.Black.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 22.dp, bottom = 20.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "NOW PLAYING",
                    style = AppTextStyles.sectionLabel,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.Center),
                )
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    IconButton(onClick = viewModel::togglePlayerMenu) {
                        Icon(Icons.Filled.MoreHoriz, contentDescription = "Opções", tint = TextSecondary)
                    }
                    SongOptionsMenu(
                        expanded = state.playerMenuOpen,
                        onDismiss = viewModel::closeMenus,
                        onEdit = { viewModel.openEdit(song.id) },
                        onChangeCover = { viewModel.openChangeCover(song.id) },
                        onSetRingtone = { viewModel.setRingtone(song.id) },
                        onDelete = { viewModel.requestDelete(song.id) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            CoverArt(
                coverVariant = customCover,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.77f)
                    .aspectRatio(1f),
            )

            Spacer(modifier = Modifier.height(26.dp))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(song.title, style = AppTextStyles.playerTitle, color = TextPrimary, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(6.dp))
                Text(song.artist, style = AppTextStyles.playerArtist, color = TextSecondary)
                Spacer(modifier = Modifier.height(3.dp))
                Text(song.album, style = AppTextStyles.playerAlbum, color = TextFaint)
            }

            Spacer(modifier = Modifier.weight(1f))

            SeekSection(
                progressPercent = state.progressPercent,
                currentLabel = formatTime(song.durationSec * state.progressPercent / 100f),
                totalLabel = formatTime(song.durationSec.toFloat()),
                accent = accent,
                onSkipBack = viewModel::skipBack10,
                onSkipForward = viewModel::skipForward10,
                onSeek = viewModel::seekTo,
            )

            Spacer(modifier = Modifier.height(26.dp))

            TransportRow(
                shuffle = state.shuffle,
                repeat = state.repeat,
                playing = state.playing,
                accent = accent,
                onToggleShuffle = viewModel::toggleShuffle,
                onToggleRepeat = viewModel::toggleRepeat,
                onPrev = viewModel::prevSong,
                onTogglePlay = viewModel::togglePlay,
                onNext = viewModel::nextSong,
            )

            Spacer(modifier = Modifier.height(22.dp))

            VolumeBar(volumePercent = state.volumePercent, onSetVolume = viewModel::setVolume)
        }
    }
}

@Composable
private fun SeekSection(
    progressPercent: Float,
    currentLabel: String,
    totalLabel: String,
    accent: Color,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onSeek: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = onSkipBack) {
                Icon(Icons.Filled.Replay10, contentDescription = "Voltar 10s", tint = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ElevatedSurface)
                    .pointerInput(Unit) {
                        detectTapGestures { offset -> onSeek(offset.x / size.width * 100f) }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((progressPercent / 100f).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent),
                )
            }
            IconButton(onClick = onSkipForward) {
                Icon(Icons.Filled.Forward10, contentDescription = "Avançar 10s", tint = TextSecondary)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text(currentLabel, style = AppTextStyles.timeLabel, color = TextMuted)
            Text(totalLabel, style = AppTextStyles.timeLabel, color = TextMuted)
        }
    }
}

@Composable
private fun TransportRow(
    shuffle: Boolean,
    repeat: Boolean,
    playing: Boolean,
    accent: Color,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onPrev: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "playPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (playing) 1.045f else 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "playPulseScale",
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleShuffle) {
            Icon(Icons.Filled.Shuffle, contentDescription = "Aleatório", tint = if (shuffle) accent else TextFaint)
        }
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Anterior", tint = TextSecondary, modifier = Modifier.size(26.dp))
        }
        Box(
            modifier = Modifier
                .size(70.dp)
                .scale(if (playing) scale else 1f)
                .clip(CircleShape)
                .background(accent)
                .clickable(onClick = onTogglePlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pausar" else "Tocar",
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Próxima", tint = TextSecondary, modifier = Modifier.size(26.dp))
        }
        IconButton(onClick = onToggleRepeat) {
            Icon(Icons.Filled.Repeat, contentDescription = "Repetir", tint = if (repeat) accent else TextFaint)
        }
    }
}

@Composable
private fun VolumeBar(volumePercent: Float, onSetVolume: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 64.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(ElevatedSurface)
            .pointerInput(Unit) {
                detectTapGestures { offset -> onSetVolume(offset.x / size.width * 100f) }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth((volumePercent / 100f).coerceIn(0f, 1f))
                .background(Color(0xFFB0B0B0)),
        )
        Row(modifier = Modifier.fillMaxHeight().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.VolumeUp, contentDescription = "Volume", tint = Color.White, modifier = Modifier.size(13.dp))
        }
    }
}

private fun formatTime(totalSeconds: Float): String {
    val sec = totalSeconds.roundToInt().coerceAtLeast(0)
    val m = sec / 60
    val s = sec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
