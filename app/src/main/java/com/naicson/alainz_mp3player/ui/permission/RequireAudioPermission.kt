package com.naicson.alainz_mp3player.ui.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.naicson.alainz_mp3player.ui.theme.LocalAccentColor
import com.naicson.alainz_mp3player.ui.theme.ScreenBackground
import com.naicson.alainz_mp3player.ui.theme.TextMuted
import com.naicson.alainz_mp3player.ui.theme.TextPrimary

private val audioPermission =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequireAudioPermission(content: @Composable () -> Unit) {
    val permissionState = rememberPermissionState(audioPermission)

    // Asking once is automatic on first entry; `shouldShowRationale` alone can't tell "never
    // asked" apart from "denied permanently" (both read false) — tracking that we've already
    // asked at least once resolves the ambiguity.
    var requested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted && !requested) {
            requested = true
            permissionState.launchPermissionRequest()
        }
    }

    when {
        permissionState.status.isGranted -> content()
        requested && !permissionState.status.shouldShowRationale -> PermanentlyDenied()
        else -> PermissionRationale(
            shouldShowRationale = permissionState.status.shouldShowRationale,
            onRequestPermission = { permissionState.launchPermissionRequest() },
        )
    }
}

@Composable
private fun PermissionRationale(shouldShowRationale: Boolean, onRequestPermission: () -> Unit) {
    val accent = LocalAccentColor.current
    Box(
        modifier = Modifier.fillMaxSize().background(ScreenBackground).padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Precisamos acessar sua música",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (shouldShowRationale) {
                    "Sem essa permissão não conseguimos listar os arquivos de áudio do seu dispositivo."
                } else {
                    "Toque no botão abaixo para permitir o acesso aos arquivos de áudio."
                },
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                Text("Permitir acesso")
            }
        }
    }
}

/** Permission denied with "don't ask again" — the system dialog won't show up anymore, so the
 * only way forward is the app's own settings screen. */
@Composable
private fun PermanentlyDenied() {
    val accent = LocalAccentColor.current
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize().background(ScreenBackground).padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Acesso à música bloqueado",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Você negou permanentemente o acesso aos arquivos de áudio. Ative a permissão de Música nas configurações do app para continuar.",
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                Text("Abrir configurações")
            }
        }
    }
}
