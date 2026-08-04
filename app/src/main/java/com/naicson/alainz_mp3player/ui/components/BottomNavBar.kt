package com.naicson.alainz_mp3player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.ui.music.AppTab
import com.naicson.alainz_mp3player.ui.theme.AppTextStyles
import com.naicson.alainz_mp3player.ui.theme.LocalAccentColor
import com.naicson.alainz_mp3player.ui.theme.NavBarBackground
import com.naicson.alainz_mp3player.ui.theme.TextFaint

@Composable
fun BottomNavBar(activeTab: AppTab, onGoToPlayer: () -> Unit, onGoToList: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccentColor.current
    // The background lives on this outer Column so it bleeds through into the gesture-nav-bar
    // inset added below; the actual 60dp bar is a fixed-height child, not stretched by that
    // inset — otherwise the icons end up vertically centered across bar+inset combined and
    // look stuck near the top with dead space underneath.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NavBarBackground)
            .navigationBarsPadding(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(60.dp)) {
            NavItem(
                label = "Player",
                icon = Icons.Filled.MusicNote,
                selected = activeTab == AppTab.PLAYER,
                selectedColor = accent,
                onClick = onGoToPlayer,
                modifier = Modifier.weight(1f),
            )
            NavItem(
                label = "List",
                icon = Icons.Filled.LibraryMusic,
                selected = activeTab == AppTab.LIST,
                selectedColor = accent,
                onClick = onGoToList,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    selectedColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) selectedColor else TextFaint
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = color)
        Text(label, style = AppTextStyles.navLabel, color = color)
    }
}
