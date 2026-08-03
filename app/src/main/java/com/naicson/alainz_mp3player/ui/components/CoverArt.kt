package com.naicson.alainz_mp3player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.ui.theme.ElevatedSurface
import com.naicson.alainz_mp3player.ui.theme.TextFaint
import kotlin.math.abs

/**
 * Placeholder cover art matching the design: a flat surface with a centered music-note
 * icon when no artwork is set, or a gradient brush once the user picks one via
 * "Alterar capa" (there's no real image storage yet — [coverVariant] stands in for it).
 */
@Composable
fun CoverArt(
    coverVariant: Pair<Long, Int>?,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    iconFraction: Float = 0.23f,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (coverVariant != null) Color.Transparent else ElevatedSurface)
            .then(if (coverVariant != null) Modifier.background(coverGradientBrush(coverVariant.first, coverVariant.second)) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (coverVariant == null) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = TextFaint,
                modifier = Modifier.fillMaxSize(iconFraction),
            )
        }
    }
}

/** Ported from the design's `coverGradientVariant(seed, variantIndex)` mock-cover generator. */
fun coverGradientBrush(seed: Long, variantIndex: Int): Brush {
    val hue = (200 + (seed * 13 + variantIndex * 41) % 150).toFloat()
    val shade = variantIndex % 3
    val start = hslColor(hue, 0.40f, (24 + shade * 7) / 100f)
    val end = hslColor(hue, 0.50f, (38 + shade * 7) / 100f)
    return Brush.linearGradient(listOf(start, end))
}

private fun hslColor(hueDeg: Float, saturation: Float, lightness: Float): Color {
    val c = (1 - abs(2 * lightness - 1)) * saturation
    val x = c * (1 - abs((hueDeg / 60f) % 2 - 1))
    val m = lightness - c / 2
    val (r, g, b) = when {
        hueDeg < 60 -> Triple(c, x, 0f)
        hueDeg < 120 -> Triple(x, c, 0f)
        hueDeg < 180 -> Triple(0f, c, x)
        hueDeg < 240 -> Triple(0f, x, c)
        hueDeg < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
