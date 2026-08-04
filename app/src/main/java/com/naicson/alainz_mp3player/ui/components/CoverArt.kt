package com.naicson.alainz_mp3player.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.naicson.alainz_mp3player.data.local.EmbeddedArtwork
import com.naicson.alainz_mp3player.data.model.Song
import com.naicson.alainz_mp3player.ui.theme.ElevatedSurface
import com.naicson.alainz_mp3player.ui.theme.TextFaint

/**
 * Real cover art, in priority order: a user-picked image (`customCoverUri`, from the system
 * photo picker) beats the artwork embedded in the file's own tags, which beats a plain
 * music-note placeholder when neither is available.
 */
@Composable
fun CoverArt(
    song: Song,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    iconFraction: Float = 0.23f,
) {
    val customUri = song.customCoverUri
    val embedded = if (customUri == null) rememberEmbeddedArtwork(song.id, song.filePath) else null

    Box(
        modifier = modifier.clip(shape).background(ElevatedSurface),
        contentAlignment = Alignment.Center,
    ) {
        when {
            customUri != null -> AsyncImage(
                model = customUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            embedded != null -> Image(
                bitmap = embedded.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = TextFaint,
                modifier = Modifier.fillMaxSize(iconFraction),
            )
        }
    }
}

@Composable
private fun rememberEmbeddedArtwork(songId: Long, filePath: String): Bitmap? {
    val state = produceState<Bitmap?>(initialValue = null, key1 = songId, key2 = filePath) {
        value = EmbeddedArtwork.load(songId, filePath)
    }
    return state.value
}
