package com.naicson.alainz_mp3player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.naicson.alainz_mp3player.ui.theme.AlainzMp3PlayerTheme
import com.naicson.alainz_mp3player.ui.theme.AppTextStyles
import com.naicson.alainz_mp3player.ui.theme.Border
import com.naicson.alainz_mp3player.ui.theme.LocalAccentColor
import com.naicson.alainz_mp3player.ui.theme.PopoverSurface
import com.naicson.alainz_mp3player.ui.theme.ScreenBackground
import com.naicson.alainz_mp3player.ui.theme.TextFaint
import com.naicson.alainz_mp3player.ui.theme.TextPrimary

/**
 * There's no free, ToS-compliant way to search Google Images from the app, so this searches
 * cover art by artist/track through [com.naicson.alainz_mp3player.data.remote.AlbumArtSearchService]
 * (the iTunes catalog) instead — real official artwork, no API key, no scraping.
 */
@Preview(showBackground = true, backgroundColor = 0xFF2A2A2A)
@Composable
private fun ChangeCoverDialogPreview() {
    AlainzMp3PlayerTheme {
        ChangeCoverDialog(
            query = "Nome da música",
            onQueryChange = {},
            onSearch = {},
            isLoading = false,
            results = emptyList(),
            selectedUrl = null,
            onSelectResult = {},
            onCancel = {},
            onConfirm = {},
        )
    }
}

@Composable
fun ChangeCoverDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isLoading: Boolean,
    results: List<String>,
    selectedUrl: String?,
    onSelectResult: (String) -> Unit,
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
                    .padding(top = 10.dp, bottom = 12.dp)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .background(ScreenBackground, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle.Default.copy(color = TextPrimary, fontSize = AppTextStyles.modalInputText.fontSize),
                    singleLine = true,
                    cursorBrush = SolidColor(TextPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Buscar",
                    tint = TextFaint,
                    modifier = Modifier.size(18.dp).clickable(onClick = onSearch),
                )
            }

            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp), contentAlignment = Alignment.Center) {
                when {
                    isLoading -> CircularProgressIndicator(color = accent, modifier = Modifier.size(28.dp))
                    results.isEmpty() -> Text(
                        "Nenhum resultado encontrado",
                        style = AppTextStyles.modalFieldLabel,
                        color = TextFaint,
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        results.chunked(3).forEach { rowUrls ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowUrls.forEach { url ->
                                    val selected = selectedUrl == url
                                    AsyncImage(
                                        model = url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(3.dp, if (selected) accent else Color.Transparent, RoundedCornerShape(6.dp))
                                            .clickable { onSelectResult(url) },
                                    )
                                }
                                repeat(3 - rowUrls.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(text = "Cancelar", onClick = onCancel, modifier = Modifier.weight(1f))
                PrimaryButton(text = "Usar como capa", onClick = onConfirm, enabled = selectedUrl != null, modifier = Modifier.weight(1f))
            }
        }
    }
}
