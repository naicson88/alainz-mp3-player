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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.ui.theme.AccentBlue
import com.naicson.alainz_mp3player.ui.theme.AccentCyan
import com.naicson.alainz_mp3player.ui.theme.AccentDeepBlue
import com.naicson.alainz_mp3player.ui.theme.AppTextStyles
import com.naicson.alainz_mp3player.ui.theme.PopoverSurface
import com.naicson.alainz_mp3player.ui.theme.TextMuted
import com.naicson.alainz_mp3player.ui.theme.TextPrimary

private val AccentOptions = listOf(AccentBlue, AccentDeepBlue, AccentCyan)

@Composable
fun SettingsDialog(currentAccent: Color, onSelectAccent: (Color) -> Unit, onClose: () -> Unit) {
    ModalScrim(onDismiss = onClose) {
        Column(
            modifier = Modifier
                .background(PopoverSurface, RoundedCornerShape(12.dp))
                .padding(20.dp),
        ) {
            Text("Configurações", style = AppTextStyles.modalTitle, color = TextPrimary)

            Text(
                "Cor de destaque",
                style = AppTextStyles.modalFieldLabel,
                color = TextMuted,
                modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AccentOptions.forEach { color ->
                    val selected = color == currentAccent
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color, CircleShape)
                            .border(2.dp, if (selected) TextPrimary else Color.Transparent, CircleShape)
                            .clickable { onSelectAccent(color) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Filled.Check, contentDescription = "Selecionada", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(text = "Fechar", onClick = onClose, modifier = Modifier.weight(1f))
            }
        }
    }
}
