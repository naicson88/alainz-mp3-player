package com.naicson.alainz_mp3player.data.local

import android.content.IntentSender

sealed interface DeleteOutcome {
    data object Deleted : DeleteOutcome
    data class RequiresConfirmation(val intentSender: IntentSender) : DeleteOutcome
    data class Failed(val message: String) : DeleteOutcome
}
