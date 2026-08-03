package com.naicson.alainz_mp3player.data.local

import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deletes a MediaStore-indexed file the app doesn't own. The mechanism differs by API level:
 * - 30+: [MediaStore.createDeleteRequest] returns a system-confirmation [android.app.PendingIntent];
 *   once the user approves it, the platform performs the delete itself.
 * - 29: a direct [android.content.ContentResolver.delete] throws [RecoverableSecurityException]
 *   carrying the confirmation intent; after the user approves it, the same delete is retried.
 * - below 29 (legacy storage): a direct delete works outright, given WRITE_EXTERNAL_STORAGE.
 */
@Singleton
class MediaDeleter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun requestDelete(uri: Uri): DeleteOutcome = withContext(Dispatchers.IO) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                DeleteOutcome.RequiresConfirmation(pendingIntent.intentSender)
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                try {
                    context.contentResolver.delete(uri, null, null)
                    DeleteOutcome.Deleted
                } catch (e: RecoverableSecurityException) {
                    DeleteOutcome.RequiresConfirmation(e.userAction.actionIntent.intentSender)
                }
            }
            else -> directDelete(uri)
        }
    }

    /** Only meaningful on API 29, where the user's approval grants a one-time retry, not an automatic delete. */
    suspend fun finishAfterConfirmation(uri: Uri): DeleteOutcome = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) DeleteOutcome.Deleted else directDelete(uri)
    }

    private fun directDelete(uri: Uri): DeleteOutcome = try {
        context.contentResolver.delete(uri, null, null)
        DeleteOutcome.Deleted
    } catch (e: SecurityException) {
        DeleteOutcome.Failed("Sem permissão para excluir este arquivo")
    }
}
