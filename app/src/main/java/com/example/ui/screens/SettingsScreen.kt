package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AppRank
import com.example.model.AppSettings
import com.example.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.InviteWebSheet
import com.example.ui.components.LocalCustomFrames
import com.example.ui.components.NEXOLIVE_APK_VERSION_LABEL
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.NeonOutlinedButton
import com.example.ui.components.TxlLogoMark
import com.example.ui.theme.DangerRose
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

private data class SettingTile(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: UserProfile?,
    allUsers: List<UserProfile>,
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onPickAppBackgroundUri: (Uri) -> Unit,
    onResetAppBackground: () -> Unit,
    onAssignAppRank: (String, AppRank) -> Unit,
    onGrantCoins: (targetUserId: String, amount: Int) -> Unit = { _, _ -> },
    onBanUser: (String, String) -> Unit = { _, _ -> },
    onUnbanUser: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val customFrames = LocalCustomFrames.current
    var activeTileKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showGrantSheet by rememberSaveable { mutableStateOf(false) }
    var showInviteSheet by rememberSaveable { mutableStateOf(false) }

    val appBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickAppBackgroundUri(uri)
        }
    }

    val appearanceSub = if (!settings.appBackgroundUri.isNullOrBlank()) {
        "Fondo personalizado"
    } else if (settings.neonGlowEnabled) {
        "Neón intenso"
    } else {
        "Neón suave"
    }

    val tiles = listOf(
        SettingTile("account", "Cuenta", user?.email ?: "Perfil activo", Icons.Filled.AccountCircle, NeonCyan),
        SettingTile("privacy", "Privacidad", if (settings.privateProfileMode) "Modo privado" else "Visible en salas", Icons.Filled.Lock, NeonPurpleLight),
        SettingTile("notifications", "Notificaciones", if (settings.notificationsEnabled) "Activadas" else "Silenciadas", Icons.Filled.NotificationsActive, NeonPink),
        SettingTile("appearance", "Apariencia", appearanceSub, Icons.Filled.ColorLens, NeonPurple),
        SettingTile("language", "Idioma", settings.languageLabel, Icons.Filled.Language, NeonCyan),
        SettingTile("help", "Ayuda y soporte", "Reglas y roles", Icons.Filled.HelpOutline, NeonPink)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(44.dp).testTag("settings_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        TxlLogoMark(compact = true)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Configuración", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                    Text("Personaliza tu experiencia • $NEXOLIVE_APK_VERSION_LABEL", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            items(tiles, key = { it.key }) { tile ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .testTag("settings_tile_${tile.key}"),
                    onClick = { activeTileKey = tile.key }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tile.accent.copy(alpha = 0.18f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(tile.icon, contentDescription = tile.title, tint = tile.accent, modifier = Modifier.size(22.dp))
                            }
                        }
                        Column {
                            Text(tile.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(tile.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                        }
                    }
                }
            }
        }

        NeonOutlinedButton(
            text = "🌐 Invitar al mundo por Sitio Web ($NEXOLIVE_APK_VERSION_LABEL)",
            onClick = { showInviteSheet = true },
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            testTag = "settings_invite_web_button"
        )

        if (user?.canManageVerifications == true) {
            NeonOutlinedButton(
                text = "Panel del Dueño: Verificados, Monedas y Baneos",
                onClick = { showGrantSheet = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                testTag = "settings_grant_badges_button"
            )
        }

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = DangerRose),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("logout_button")
        ) {
            Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }

    if (showInviteSheet) {
        InviteWebSheet(
            inviterUsername = user?.username,
            currentWebUrl = settings.inviteWebUrl,
            canEditGlobalUrl = user?.isPrimaryVerifiedOwner == true,
            onSaveWebUrl = { newUrl -> onUpdateSettings { it.copy(inviteWebUrl = newUrl) } },
            onDismiss = { showInviteSheet = false }
        )
    }

    if (showGrantSheet && user?.canManageVerifications == true) {
        GrantVerificationSheet(
            users = allUsers,
            currentUserId = user.id,
            onAssignRank = onAssignAppRank,
            onGrantCoins = onGrantCoins,
            onBanUser = onBanUser,
            onUnbanUser = onUnbanUser,
            onDismiss = { showGrantSheet = false }
        )
    }

    activeTileKey?.let { key ->
        ModalBottomSheet(
            onDismissRequest = { activeTileKey = null },
            containerColor = SurfaceCard,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                when (key) {
                    "account" -> {
                        val activeFrameTitle = user?.let { u ->
                            customFrames[u.avatarFrameId]?.title ?: u.avatarFrame.displayName
                        }.orEmpty()

                        Text(
                            text = "Cuenta NexoLive",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.testTag("account_sheet_title")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Usuario: ${user?.username.orEmpty()}", color = TextPrimary)
                        Text("Correo Google verificado: ${user?.email.orEmpty()}", color = TextSecondary)
                        Text("Marco activo: $activeFrameTitle", color = NeonPink)
                        Text(
                            text = if (user?.hasInfiniteCoins == true) "Monedas: ∞ (Dueño único)" else "Monedas: 💎 ${user?.coinsCount ?: 0}",
                            color = NeonCyan
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (user?.isPrimaryVerifiedOwner == true) {
                                "Cuenta maestra verificada: solo tú posees controles globales, insignias y regalo de monedas."
                            } else {
                                "Cuenta personal verificada por Google. Nadie más puede entrar a tu sesión ni al panel del dueño."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (user?.isPrimaryVerifiedOwner == true) SuccessGreen else TextSecondary
                        )
                    }
                    "privacy" -> {
                        Text("Privacidad y chat", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingSwitchRow(
                            title = "Mostrar estado personalizado en chat",
                            checked = settings.showStatusInChat,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(showStatusInChat = v) } }
                        )
                        SettingSwitchRow(
                            title = "Modo perfil privado",
                            checked = settings.privateProfileMode,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(privateProfileMode = v) } }
                        )
                    }
                    "notifications" -> {
                        Text("Alertas y notificaciones", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingSwitchRow(
                            title = "Recibir notificaciones en la app",
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(notificationsEnabled = v) } }
                        )
                    }
                    "appearance" -> {
                        Text("Apariencia y Fondo de Interfaz", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingSwitchRow(
                            title = "Resplandor neón de fondo",
                            checked = settings.neonGlowEnabled,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(neonGlowEnabled = v) } }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Fondo personalizado de la aplicación", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Elige cualquier foto de tu galería como fondo global de NexoLive.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))

                        if (!settings.appBackgroundUri.isNullOrBlank()) {
                            AsyncImage(
                                model = File(settings.appBackgroundUri),
                                contentDescription = "Vista previa fondo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = NeonPurple.copy(alpha = 0.24f),
                            border = BorderStroke(1.dp, NeonPink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clickable {
                                    appBgPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("pick_app_background_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = NeonPink, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (settings.appBackgroundUri == null) "Subir fondo de interfaz" else "Cambiar foto de fondo",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        if (!settings.appBackgroundUri.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Oscuridad de contraste (${(settings.appBackgroundDim * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Slider(
                                value = settings.appBackgroundDim,
                                onValueChange = { dim -> onUpdateSettings { it.copy(appBackgroundDim = dim) } },
                                valueRange = 0.20f..0.85f,
                                colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPurple)
                            )
                            TextButton(
                                onClick = onResetAppBackground,
                                modifier = Modifier.align(Alignment.CenterHorizontally).testTag("reset_app_background_button")
                            ) {
                                Text("Restaurar fondo oscuro predeterminado", color = DangerRose)
                            }
                        }
                    }
                    "language" -> {
                        Text("Idioma", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        listOf("Español (ES)", "Español (LATAM)", "English (EN)").forEach { lang ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (settings.languageLabel == lang) NeonPurple.copy(alpha = 0.25f) else SurfaceElevated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onUpdateSettings { it.copy(languageLabel = lang) }
                                        activeTileKey = null
                                    }
                            ) {
                                Text(lang, modifier = Modifier.padding(14.dp), color = TextPrimary)
                            }
                        }
                    }
                    "help" -> {
                        Text("Roles y Ayuda", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("• Admin: Control total de la sala, bloqueos, asignar Co-Admins, personalizar fondo o eliminar sala.", color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Co-Admin: Moderación de sala, silenciar y eliminar mensajes.", color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Miembro: Acceso a voz, levantar la mano, chat grupal, publicaciones y Drifting Bottle.", color = TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
