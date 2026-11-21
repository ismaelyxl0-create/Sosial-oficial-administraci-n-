package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

const val NEXOLIVE_GLOBAL_URL = "https://ais-pre-xadmx2hoheottpbykwt77l-688927905799.us-east1.run.app"
const val NEXOLIVE_APK_VERSION_LABEL = "NexoLive v2.1 APK"

fun buildInviteMessage(
    roomName: String? = null,
    inviterUsername: String? = null,
    customWebUrl: String = ""
): String {
    val webUrl = customWebUrl.trim().ifBlank { NEXOLIVE_GLOBAL_URL }
    val inviterPart = inviterUsername?.takeIf { it.isNotBlank() }?.let { "Te invita @$it" } ?: "¡Únete a NexoLive ($NEXOLIVE_APK_VERSION_LABEL)!"
    val deepLink = if (!inviterUsername.isNullOrBlank()) {
        "\nAbrir directo en la app: nexolive://invite?by=${Uri.encode(inviterUsername)}"
    } else ""
    return if (!roomName.isNullOrBlank()) {
        "$inviterPart a la sala \"$roomName\" en NexoLive.\nDescarga e instala el APK desde el sitio web para hablar por voz en vivo con personas de todo el mundo:\n$webUrl$deepLink"
    } else {
        "$inviterPart Descarga e instala el APK desde el sitio web para encontrarnos, hablar por voz en vivo, enviar regalos 3D y crear marcos en la Tienda:\n$webUrl$deepLink"
    }
}

fun shareNexoLiveApp(
    context: Context,
    roomName: String? = null,
    inviterUsername: String? = null,
    customWebUrl: String = ""
) {
    val message = buildInviteMessage(roomName, inviterUsername, customWebUrl)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Invitación a NexoLive ($NEXOLIVE_APK_VERSION_LABEL)")
        putExtra(Intent.EXTRA_TEXT, message)
    }
    val chooser = Intent.createChooser(sendIntent, "Compartir invitación web de NexoLive")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteWebSheet(
    inviterUsername: String?,
    currentWebUrl: String,
    canEditGlobalUrl: Boolean,
    onSaveWebUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    var urlInput by rememberSaveable(currentWebUrl) {
        mutableStateOf(currentWebUrl.ifBlank { NEXOLIVE_GLOBAL_URL })
    }
    var copiedNotice by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Public, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Invitación Web Global", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("$NEXOLIVE_APK_VERSION_LABEL • Multiusuario mundial", fontSize = 12.sp, color = NeonPink)
                }
            }

            Text(
                text = "Comparte tu enlace web para que cualquier persona descargue el APK, inicie sesión con su correo de Google y te encuentre en el buscador o en salas de voz.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (!inviterUsername.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NeonPurple.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, BorderGlow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tu usuario público para encontrarte: @$inviterUsername",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = urlInput,
                onValueChange = { if (canEditGlobalUrl) urlInput = it },
                enabled = canEditGlobalUrl,
                label = { Text("Enlace del sitio web de descarga APK") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated
                ),
                modifier = Modifier.fillMaxWidth().testTag("invite_web_url_input")
            )

            if (canEditGlobalUrl) {
                OutlinedButton(
                    onClick = { onSaveWebUrl(urlInput) },
                    border = BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar enlace web de invitación", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }

            val message = buildInviteMessage(
                roomName = null,
                inviterUsername = inviterUsername,
                customWebUrl = urlInput
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (copiedNotice) {
                Text("✓ Invitación y enlace copiados al portapapeles", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(message))
                        copiedNotice = true
                    },
                    border = BorderStroke(1.dp, NeonPink),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = NeonPink, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                NeonGradientButton(
                    text = "Compartir Web",
                    onClick = {
                        if (canEditGlobalUrl) onSaveWebUrl(urlInput)
                        shareNexoLiveApp(context, null, inviterUsername, urlInput)
                    },
                    modifier = Modifier.weight(1.3f),
                    testTag = "invite_web_share_button"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
