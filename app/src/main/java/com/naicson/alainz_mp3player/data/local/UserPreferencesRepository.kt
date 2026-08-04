package com.naicson.alainz_mp3player.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.naicson.alainz_mp3player.ui.theme.AccentBlue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")
private val ACCENT_COLOR_KEY = intPreferencesKey("accent_color_argb")

/** The only user preference so far is the accent color — kept in DataStore so it survives
 * app restarts, unlike the in-memory-only `LocalAccentColor` default. */
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
}
