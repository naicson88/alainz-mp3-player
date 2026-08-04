package com.naicson.alainz_mp3player.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.naicson.alainz_mp3player.ui.theme.Danger

/** Native equivalent of the design's `window.confirm(...)` before deleting a song. */
@Composable
fun DeleteConfirmDialog(songTitle: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Excluir música") },
        text = { Text("Excluir \"$songTitle\" permanentemente do dispositivo?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Excluir", color = Danger) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancelar") }
        },
    )
}
