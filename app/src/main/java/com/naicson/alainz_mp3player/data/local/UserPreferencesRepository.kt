package com.naicson.alainz_mp3player.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.naicson.alainz_mp3player.ui.theme.AccentBlue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")
private val ACCENT_COLOR_KEY = intPreferencesKey("accent_color_argb")
private val LAST_SONG_ID_KEY = longPreferencesKey("last_song_id")
private val LAST_POSITION_MS_KEY = longPreferencesKey("last_position_ms")

/** User preferences kept in DataStore so they survive app restarts, unlike in-memory-only
 * defaults: the accent color, and where playback last left off. */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val accentColor: Flow<Color> = context.dataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_KEY]?.let { Color(it) } ?: AccentBlue
    }

    suspend fun setAccentColor(color: Color) {
        context.dataStore.edit { it[ACCENT_COLOR_KEY] = color.toArgb() }
    }

    /** Song id + position (ms) to resume on the next app launch — null if nothing was ever played. */
    val lastPlayback: Flow<Pair<Long, Long>?> = context.dataStore.data.map { prefs ->
        val songId = prefs[LAST_SONG_ID_KEY] ?: return@map null
        songId to (prefs[LAST_POSITION_MS_KEY] ?: 0L)
    }

    suspend fun saveLastPlayback(songId: Long, positionMs: Long) {
        context.dataStore.edit {
            it[LAST_SONG_ID_KEY] = songId
            it[LAST_POSITION_MS_KEY] = positionMs
        }
    }
}
