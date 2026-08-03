package com.naicson.alainz_mp3player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.data.model.Song
import com.naicson.alainz_mp3player.ui.theme.AppTextStyles
import com.naicson.alainz_mp3player.ui.theme.Border
import com.naicson.alainz_mp3player.ui.theme.LocalAccentColor
import com.naicson.alainz_mp3player.ui.theme.PopoverSurface
import com.naicson.alainz_mp3player.ui.theme.ScreenBackground
import com.naicson.alainz_mp3player.ui.theme.TextFaint
import com.naicson.alainz_mp3player.ui.theme.TextMuted
import com.naicson.alainz_mp3player.ui.theme.TextPrimary

@Composable
fun ChangeCoverDialog(
    song: Song,
    selectedResultIndex: Int?,
    onSelectResult: (Int) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val accent = LocalAccentColor.current
    ModalScrim(onDismiss = onCancel) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(PopoverSurface, RoundedCornerShape(12.dp))
                .padding(18.dp),
        ) {
            Text("Alterar capa", style = AppTextStyles.modalTitle, color = TextPrimary)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .background(ScreenBackground, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = TextFaint, modifier = Modifier.size(15.dp))
                Text("${song.title} ${song.artist}", style = AppTextStyles.modalInputText, color = TextPrimary, maxLines = 1)
            }

            Text(
                "Resultados de imagem na web",
                style = AppTextStyles.modalFieldLabel,
                color = TextFaint,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..1).forEach { rowIndex ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0..2).forEach { colIndex ->
                            val index = rowIndex * 3 + colIndex
                            val selected = selectedResultIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(coverGradientBrush(song.id, index))
                                    .border(3.dp, if (selected) accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable { onSelectResult(index) },
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(text = "Cancelar", onClick = onCancel, modifier = Modifier.weight(1f))
                PrimaryButton(text = "Usar como capa", onClick = onConfirm, enabled = selectedResultIndex != null, modifier = Modifier.weight(1f))
            }
        }
    }
}
