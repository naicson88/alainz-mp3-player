package com.naicson.alainz_mp3player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// The design specifies the Inter font (Google Fonts). Using the platform default
// sans-serif until Inter's font files (or a downloadable-fonts setup) are added.

val Typography = Typography()

object AppTextStyles {
    val sectionLabel = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    val playerTitle = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 1.25.em)
    val playerArtist = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
    val playerAlbum = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)
    val timeLabel = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)

    val screenTitle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    val folderTitle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
    val listSummary = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Normal)
    val sectionLetter = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

    val rowTitle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val rowSubtitle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
    val rowDuration = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)

    val pillButtonLabel = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    val menuItemLabel = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)

    val modalTitle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
    val modalFieldLabel = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal)
    val modalInputText = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)
    val modalButtonLabel = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

    val miniPlayerTitle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    val miniPlayerSubtitle = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Normal)

    val navLabel = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    val toast = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    val emptyStateHint = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)
}
