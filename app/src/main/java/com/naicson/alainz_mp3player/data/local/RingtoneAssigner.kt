package com.naicson.alainz_mp3player.data.local

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Setting a song as the device ringtone means writing to [Settings.System], which is gated
 * behind the WRITE_SETTINGS special-access permission — unlike normal dangerous permissions,
 * this one can't be requested through a runtime dialog; the user must flip it on in a system
 * settings screen we redirect them to.
 */
@Singleton
class RingtoneAssigner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun canWrite(): Boolean = Settings.System.canWrite(context)

    fun manageWriteSettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))

    fun setAsRingtone(uri: Uri): Boolean = try {
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, uri)
        true
    } catch (e: SecurityException) {
        false
    }
}
