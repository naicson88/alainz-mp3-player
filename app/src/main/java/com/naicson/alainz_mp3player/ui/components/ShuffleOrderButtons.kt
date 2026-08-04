package com.naicson.alainz_mp3player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.ui.theme.AccentGold
import com.naicson.alainz_mp3player.ui.theme.AlainzMp3PlayerTheme
import com.naicson.alainz_mp3player.ui.theme.AppTextStyles
import com.naicson.alainz_mp3player.ui.theme.ElevatedSurface
import com.naicson.alainz_mp3player.ui.theme.TextSecondary

@Preview(showBackground = true, backgroundColor = 0xFF2A2A2A)
@Composable
private fun ShuffleOrderButtonsPreview() {
    AlainzMp3PlayerTheme {
        ShuffleOrderButtons(onShuffle = {}, onOrder = {})
    }
}

/** The "Aleatório" / "Em ordem" pill pair shown on the songs, folder-detail and search-results headers. */
@Composable
fun ShuffleOrderButtons(onShuffle: () -> Unit, onOrder: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(label = "Aleatório", icon = Icons.Filled.Shuffle, onClick = onShuffle)
        Pill(label = "Em ordem", icon = Icons.Filled.PlayArrow, onClick = onOrder)
    }
}

@Composable
private fun Pill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ElevatedSurface)
            .border(1.dp, AccentGold, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        Text(label, style = AppTextStyles.pillButtonLabel, color = TextSecondary)
    }
}
