package com.naicson.alainz_mp3player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.naicson.alainz_mp3player.ui.AppRoot
import com.naicson.alainz_mp3player.ui.music.MusicViewModel
import com.naicson.alainz_mp3player.ui.permission.RequireAudioPermission
import com.naicson.alainz_mp3player.ui.theme.AlainzMp3PlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Shared instance: hiltViewModel() here and inside AppRoot() both resolve to the
            // same Activity-scoped MusicViewModel, so the accent color picked in Settings
            // (inside AppRoot) is reflected in this top-level theme wrapper immediately.
            val viewModel: MusicViewModel = hiltViewModel()
            val accentColor by viewModel.accentColor.collectAsState()
            AlainzMp3PlayerTheme(accentColor = accentColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RequireAudioPermission {
                        AppRoot(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
