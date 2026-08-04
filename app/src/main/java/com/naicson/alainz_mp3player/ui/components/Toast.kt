package com.naicson.alainz_mp3player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.ui.theme.AppTextStyles
import com.naicson.alainz_mp3player.ui.theme.NavBarBackground
import com.naicson.alainz_mp3player.ui.theme.TextPrimary

@Composable
fun Toast(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .background(NavBarBackground, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(message, style = AppTextStyles.toast, color = TextPrimary)
        }
    }
}
