package com.naicson.alainz_mp3player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.ui.music.EditForm
import com.naicson.alainz_mp3player.ui.theme.AppTextStyles
import com.naicson.alainz_mp3player.ui.theme.Border
import com.naicson.alainz_mp3player.ui.theme.LocalAccentColor
import com.naicson.alainz_mp3player.ui.theme.PopoverSurface
import com.naicson.alainz_mp3player.ui.theme.ScreenBackground
import com.naicson.alainz_mp3player.ui.theme.TextMuted
import com.naicson.alainz_mp3player.ui.theme.TextPrimary
import com.naicson.alainz_mp3player.ui.theme.TextSecondary

@Composable
fun EditSongDialog(
    editForm: EditForm,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onAlbumChange: (String) -> Unit,
    onGenreChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    ModalScrim(onDismiss = onCancel) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(PopoverSurface, RoundedCornerShape(12.dp))
                .padding(20.dp),
        ) {
            Text("Editar informações", style = AppTextStyles.modalTitle, color = TextPrimary)
            Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField("Título", editForm.title, onTitleChange)
                LabeledField("Artista", editForm.artist, onArtistChange)
                LabeledField("Álbum", editForm.album, onAlbumChange)
                LabeledField("Gênero", editForm.genre, onGenreChange)
            }
            Row(modifier = Modifier.padding(top = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(text = "Cancelar", onClick = onCancel, modifier = Modifier.weight(1f))
                PrimaryButton(text = "Salvar", onClick = onSave, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun ModalScrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.clickable(enabled = false) {}) { content() }
    }
}

@Composable
internal fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = AppTextStyles.modalFieldLabel, color = TextMuted, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Border, RoundedCornerShape(6.dp))
                .background(ScreenBackground, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle.Default.copy(color = TextPrimary, fontSize = AppTextStyles.modalInputText.fontSize),
                cursorBrush = SolidColor(TextPrimary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppTextStyles.modalButtonLabel, color = TextSecondary)
    }
}

@Composable
internal fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val accent = LocalAccentColor.current
    Box(
        modifier = modifier
            .background(if (enabled) accent else Border, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppTextStyles.modalButtonLabel, color = Color.White)
    }
}
