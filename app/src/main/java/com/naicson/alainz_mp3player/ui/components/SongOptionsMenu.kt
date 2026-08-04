package com.naicson.alainz_mp3player.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.naicson.alainz_mp3player.ui.theme.Danger
import com.naicson.alainz_mp3player.ui.theme.PopoverSurface
import com.naicson.alainz_mp3player.ui.theme.TextPrimary

/** The "⋮" menu shown per song and on the Player screen: edit / change cover / set ringtone / delete. */
@Composable
fun SongOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onChangeCover: () -> Unit,
    onSetRingtone: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = PopoverSurface,
        shadowElevation = 12.dp,
    ) {
        DropdownMenuItem(
            text = { Text("Editar informações", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = TextPrimary) },
            onClick = { onDismiss(); onEdit() },
        )
        DropdownMenuItem(
            text = { Text("Alterar capa", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null, tint = TextPrimary) },
            onClick = { onDismiss(); onChangeCover() },
        )
        DropdownMenuItem(
            text = { Text("Definir como toque", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Filled.Notifications, contentDescription = null, tint = TextPrimary) },
            onClick = { onDismiss(); onSetRingtone() },
        )
        DropdownMenuItem(
            text = { Text("Excluir", color = Danger) },
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Danger) },
            onClick = { onDismiss(); onDelete() },
        )
    }
}
