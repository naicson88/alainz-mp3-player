package com.naicson.alainz_mp3player.ui.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naicson.alainz_mp3player.data.model.Song
import com.naicson.alainz_mp3player.ui.theme.AlainzMp3PlayerTheme
import com.naicson.alainz_mp3player.ui.theme.LocalAccentColor
import com.naicson.alainz_mp3player.ui.theme.PopoverSurface
import com.naicson.alainz_mp3player.ui.theme.TextFaint
import kotlin.math.roundToInt

private val Alphabet = ('A'..'Z').map { it.toString() }

/**
 * Fast-scroll A-Z index for the songs tab, ported from the classic Contacts/Photos pattern:
 * touching anywhere on the strip jumps straight there, dragging up/down keeps tracking the
 * finger. Hidden by default — [visible] should be true only while the list itself is scrolling
 * or a finger is already down on this strip (the latter needs its own flag: the very first
 * touch, before any list movement, wouldn't otherwise flip `LazyListState.isScrollInProgress`
 * in time to reveal the strip that touch just landed on).
 */
@Composable
fun AlphabetScrollbar(
    visible: Boolean,
    availableLetters: Set<String>,
    activeLetter: String?,
    onLetterSelected: (String) -> Unit,
    onDragStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccentColor.current
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "alphabetScrollbarAlpha")

    Column(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                fun letterAt(y: Float): String {
                    val fraction = (y / size.height).coerceIn(0f, 1f)
                    val index = (fraction * (Alphabet.size - 1)).roundToInt().coerceIn(0, Alphabet.size - 1)
                    return Alphabet[index]
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onDragStateChanged(true)
                    onLetterSelected(letterAt(down.position.y))
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        onLetterSelected(letterAt(change.position.y))
                        change.consume()
                    }
                    onDragStateChanged(false)
                }
            }
            .alpha(alpha)
            .background(PopoverSurface, RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Alphabet.forEach { letter ->
            val available = letter in availableLetters
            Text(
                letter,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 9.sp,
                fontWeight = if (letter == activeLetter) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    letter == activeLetter -> accent
                    available -> TextFaint
                    else -> TextFaint.copy(alpha = 0.35f)
                },
            )
        }
    }
}

/** Nearest letter to [target] that actually has songs — tapping "Q" with no Q titles should
 * still land somewhere sensible instead of doing nothing. */
internal fun nearestAvailableLetter(target: String, available: Set<String>): String? {
    if (available.isEmpty()) return null
    if (target in available) return target
    val targetIndex = Alphabet.indexOf(target)
    if (targetIndex < 0) return available.minOrNull()
    var offset = 1
    while (offset < Alphabet.size) {
        Alphabet.getOrNull(targetIndex - offset)?.takeIf { it in available }?.let { return it }
        Alphabet.getOrNull(targetIndex + offset)?.takeIf { it in available }?.let { return it }
        offset++
    }
    return available.firstOrNull()
}

@Preview(showBackground = true, backgroundColor = 0xFF2A2A2A)
@Composable
private fun AlphabetScrollbarPreview() {
    AlainzMp3PlayerTheme {
        Box(modifier = Modifier.height(320.dp).padding(8.dp)) {
            AlphabetScrollbar(
                visible = true,
                availableLetters = setOf("A", "B", "C", "M", "S", "T"),
                activeLetter = "M",
                onLetterSelected = {},
                onDragStateChanged = {},
            )
        }
    }
}

/** Flat LazyColumn item index of each group's sticky header — one header item plus each group's
 * songs precede the next group, in order. */
internal fun headerFlatIndices(groups: List<Pair<String, List<IndexedValue<Song>>>>): Map<String, Int> {
    var index = 0
    val map = LinkedHashMap<String, Int>()
    for ((letter, songs) in groups) {
        map[letter] = index
        index += 1 + songs.size
    }
    return map
}
