package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppRank
import com.example.model.AvatarFrame
import com.example.model.Community
import com.example.model.CustomFrame
import com.example.model.CustomFrameStyle
import com.example.model.UserProfile
import com.example.ui.components.AppRankPill
import com.example.ui.components.CommunityCoverImage
import com.example.ui.components.FramedAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.NeonOutlinedButton
import com.example.ui.components.RoleBadge
import com.example.ui.components.VerifiedRankBadge
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerRose
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.RoleAdminGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    user: UserProfile?,
    allUsers: List<UserProfile>,
    myCommunities: List<Community>,
    customFrames: List<CustomFrame>,
    onPickPhotoUri: (Uri, onPathReady: (String) -> Unit) -> Unit,
    onSaveCustomization: (
        username: String,
        bio: String,
        customStatus: String,
        frameId: String,
        avatarUri: String?,
        presetKey: String,
        onDone: () -> Unit
    ) -> Unit,
    onPublishPngFrame: (title: String, uri: Uri, onDone: () -> Unit) -> Unit,
    onPublishStudioFrame: (
        title: String,
        primaryColorHex: Long,
        secondaryColorHex: Long,
        stylePattern: CustomFrameStyle,
        thicknessDp: Float,
        hasSparkles: Boolean,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteCustomFrame: (String) -> Unit,
    onBuyCustomFrame: (String) -> Unit = {},
    onAssignAppRank: (targetUserId: String, newRank: AppRank) -> Unit,
    onGrantCoins: (targetUserId: String, amount: Int) -> Unit = { _, _ -> },
    onOpenStore: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onExploreCircles: () -> Unit,
    onOpenCommunityVoice: (String) -> Unit,
    onBanUser: (targetUserId: String, reason: String) -> Unit = { _, _ -> },
    onUnbanUser: (targetUserId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (user == null) return
    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    var showGrantSheet by rememberSaveable { mutableStateOf(false) }
    var showFrameStudio by rememberSaveable { mutableStateOf(false) }
    var pendingPngUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPngTitle by rememberSaveable { mutableStateOf("") }

    val pngPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingPngUri = uri
            pendingPngTitle = ""
        }
    }

    val publishedByMeCount = remember(customFrames, user.id) {
        customFrames.count { it.creatorId == user.id }
    }

    val activeFrameName = remember(user.avatarFrameId, user.avatarFrame, customFrames) {
        customFrames.firstOrNull { it.id == user.avatarFrameId }?.title
            ?: user.avatarFrame.displayName
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Neon Cyber Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(106.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonPurple, NeonPink)), RoundedCornerShape(24.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF1A0938), Color(0xFF391068), Color(0xFF280B42))
                        )
                    )
                    val step = size.width / 8f
                    for (i in 1..7) {
                        drawLine(
                            color = NeonPink.copy(alpha = 0.14f),
                            start = Offset(step * i, 0f),
                            end = Offset(step * i - 30f, size.height),
                            strokeWidth = 2f
                        )
                    }
                    drawCircle(
                        brush = Brush.radialGradient(listOf(NeonPink.copy(alpha = 0.35f), Color.Transparent)),
                        radius = size.height * 0.9f,
                        center = Offset(size.width * 0.5f, size.height * 0.5f)
                    )
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "NexoLive",
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Perfil & Identidad Neón",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(48.dp)
                        .testTag("open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Configuración y cerrar sesión",
                        tint = TextPrimary
                    )
                }
            }
        }

        // Main Profile Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        FramedAvatar(
                            avatarUri = user.avatarUri,
                            presetAvatarKey = user.presetAvatarKey,
                            frame = user.avatarFrame,
                            frameId = user.avatarFrameId,
                            size = 94.dp,
                            contentDescription = "Tu foto de perfil"
                        )
                        Surface(
                            shape = CircleShape,
                            color = NeonPink,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { showEditSheet = true }
                                .testTag("profile_quick_edit_avatar_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Edit, contentDescription = "Personalizar perfil", tint = TextPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.username,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        if (user.appRank != AppRank.NONE) {
                            Spacer(modifier = Modifier.width(6.dp))
                            VerifiedRankBadge(rank = user.appRank, size = 24.dp)
                        }
                    }
                    if (user.appRank != AppRank.NONE && user.canManageVerifications) {
                        Spacer(modifier = Modifier.height(6.dp))
                        AppRankPill(rank = user.appRank)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = RoleAdminGold.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, RoleAdminGold.copy(alpha = 0.65f))
                        ) {
                            Text(
                                text = if (user.hasInfiniteCoins) "💎 ∞ Monedas" else "🪙 ${user.coinsCount} monedas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoleAdminGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        val donorTier = user.donorTier
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(donorTier.colorHex).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color(donorTier.colorHex).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "${donorTier.emoji} Nv.${donorTier.level} ${donorTier.title}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(donorTier.colorHex),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Marco activo: $activeFrameName",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPurpleLight
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = NeonPurple.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { showEditSheet = true }
                    ) {
                        Text(
                            text = user.customStatus.ifBlank { "💬 Toca para establecer un estado personalizado" },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = user.bio.ifBlank { "Agrega una bio corta para presentarte en tus círculos." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileMetricCard(
                            value = myCommunities.size.toString(),
                            label = "comunidades",
                            highlighted = false,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileMetricCard(
                            value = user.followersCount.toString(),
                            label = "seguidores",
                            highlighted = true,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileMetricCard(
                            value = user.postsCount.toString(),
                            label = "publicaciones",
                            highlighted = false,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NeonOutlinedButton(
                            text = "Editar perfil",
                            onClick = { showEditSheet = true },
                            modifier = Modifier.weight(1f),
                            testTag = "edit_profile_button"
                        )
                        NeonGradientButton(
                            text = "Ver círculos",
                            onClick = onExploreCircles,
                            modifier = Modifier.weight(1f),
                            testTag = "profile_explore_circles_button"
                        )
                    }

                    if (user.canManageVerifications) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF0A0A0D),
                            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.55f), NeonPink))),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGrantSheet = true }
                                .testTag("owner_grant_badges_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VerifiedRankBadge(rank = AppRank.OWNER, size = 24.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Entregar Verificados", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text("Asigna Negro (Dueño), Rojo (Mod) o Azul (Influencer)", fontSize = 11.sp, color = TextSecondary)
                                }
                                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = NeonPink)
                            }
                        }
                    }
                }
            }
        }

        // Store & Cupos Showcase Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_open_store_card"),
                borderColor = NeonPink.copy(alpha = 0.7f),
                onClick = onOpenStore
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NeonPurple.copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, NeonPink),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Storefront, contentDescription = null, tint = NeonPink)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🏪 Tienda de Creaciones VozMundo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Cupo: $publishedByMeCount/${user.effectiveMaxFrameSlots} • Publica y gana 80% (${user.creatorRank.title})",
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    }
                    Text("Abrir →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonPink)
                }
            }
        }

        // Avatar Frames Showcase + Free Studio + PNG Publisher
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = NeonPink, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Marcos de Avatar", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SuccessGreen.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Personal o Tienda",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NeonPurple.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, NeonPink),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { showFrameStudio = true }
                                .testTag("open_free_frame_studio_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Brush, contentDescription = null, tint = NeonPink, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Crear Marco Gratis", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable {
                                    pngPickerLauncher.launch(arrayOf("image/png", "image/*"))
                                }
                                .testTag("publish_png_frame_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publicar PNG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    if (customFrames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Marcos creados por la comunidad (Gratis)", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(customFrames, key = { it.id }) { custom ->
                                val selected = user.avatarFrameId == custom.id
                                val canDelete = custom.creatorId == user.id || user.canManageVerifications
                                Box {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(108.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (selected) NeonPurple.copy(alpha = 0.24f) else SurfaceElevated)
                                            .border(
                                                width = if (selected) 1.5.dp else 1.dp,
                                                color = if (selected) NeonPink else BorderSubtle,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                if (custom.priceCoins > 0 && custom.creatorId != user.id && !user.hasInfiniteCoins) {
                                                    onBuyCustomFrame(custom.id)
                                                } else {
                                                    onSaveCustomization(
                                                        user.username,
                                                        user.bio,
                                                        user.customStatus,
                                                        custom.id,
                                                        user.avatarUri,
                                                        user.presetAvatarKey
                                                    ) {}
                                                }
                                            }
                                            .padding(10.dp)
                                            .testTag("custom_frame_${custom.id}")
                                    ) {
                                        FramedAvatar(
                                            avatarUri = user.avatarUri,
                                            presetAvatarKey = user.presetAvatarKey,
                                            frameId = custom.id,
                                            customFrameOverride = custom,
                                            size = 52.dp,
                                            contentDescription = custom.title
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = custom.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) NeonPink else TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (custom.priceCoins > 0) "💎 ${custom.priceCoins} • ${custom.creatorName}"
                                            else if (custom.frameType == "PNG") "PNG • Gratis"
                                            else "Estudio • ${custom.creatorName}",
                                            fontSize = 9.sp,
                                            color = if (custom.priceCoins > 0) RoleAdminGold else TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (canDelete) {
                                        IconButton(
                                            onClick = { onDeleteCustomFrame(custom.id) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(28.dp)
                                                .testTag("delete_custom_frame_${custom.id}")
                                        ) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar marco", tint = DangerRose, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Marcos clásicos NexoLive", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(AvatarFrame.entries) { frame ->
                            val selected = user.avatarFrameId == frame.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) NeonPurple.copy(alpha = 0.22f) else Color.Transparent)
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) NeonPink else BorderSubtle,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        onSaveCustomization(
                                            user.username,
                                            user.bio,
                                            user.customStatus,
                                            frame.id,
                                            user.avatarUri,
                                            user.presetAvatarKey
                                        ) {}
                                    }
                                    .padding(10.dp)
                                    .testTag("quick_frame_${frame.id}")
                            ) {
                                FramedAvatar(
                                    avatarUri = user.avatarUri,
                                    presetAvatarKey = user.presetAvatarKey,
                                    frame = frame,
                                    size = 52.dp,
                                    contentDescription = frame.displayName
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = frame.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) NeonPink else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // User's Communities Section
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Groups, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tus círculos (${myCommunities.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
        }

        if (myCommunities.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Aún no te has unido a ningún círculo. Explora o crea uno nuevo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(myCommunities, key = { it.id }) { comm ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenCommunityVoice(comm.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CommunityCoverImage(
                            coverKey = comm.coverKey,
                            customBackgroundUri = comm.backgroundUri,
                            contentDescription = comm.name,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(comm.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("${comm.memberCount} miembros • ${comm.activeVoiceCount} en voz", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        comm.myRole?.let { RoleBadge(role = it) }
                    }
                }
            }
        }
    }

    if (showEditSheet) {
        EditProfileSheet(
            user = user,
            customFrames = customFrames,
            onDismiss = { showEditSheet = false },
            onPickPhotoUri = onPickPhotoUri,
            onSave = { username, bio, status, frameId, avatarUri, presetKey ->
                onSaveCustomization(username, bio, status, frameId, avatarUri, presetKey) {
                    showEditSheet = false
                }
            }
        )
    }

    if (showFrameStudio) {
        CreateFreeFrameSheet(
            user = user,
            onDismiss = { showFrameStudio = false },
            onPublish = { title, primaryHex, secondaryHex, style, thickness, sparkles ->
                onPublishStudioFrame(title, primaryHex, secondaryHex, style, thickness, sparkles) {
                    showFrameStudio = false
                }
            }
        )
    }

    pendingPngUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingPngUri = null },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Publicar marco PNG") },
            text = {
                Column {
                    Text("Tu marco PNG transparente quedará disponible gratis para todos.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pendingPngTitle,
                        onValueChange = { if (it.length <= 28) pendingPngTitle = it },
                        label = { Text("Nombre del marco") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("png_frame_title_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pendingPngTitle.trim().length >= 2,
                    onClick = {
                        val title = pendingPngTitle
                        pendingPngUri = null
                        onPublishPngFrame(title, uri) {}
                    }
                ) {
                    Text("Publicar gratis", color = NeonPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPngUri = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    if (showGrantSheet && user.canManageVerifications) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFreeFrameSheet(
    user: UserProfile,
    onDismiss: () -> Unit,
    onPublish: (
        title: String,
        primaryColorHex: Long,
        secondaryColorHex: Long,
        stylePattern: CustomFrameStyle,
        thicknessDp: Float,
        hasSparkles: Boolean
    ) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var primaryHex by rememberSaveable { mutableLongStateOf(0xFF9D4EDD) }
    var secondaryHex by rememberSaveable { mutableLongStateOf(0xFFFF4EC2) }
    var style by remember { mutableStateOf(CustomFrameStyle.RING_GLOW) }
    var thickness by rememberSaveable { mutableFloatStateOf(4.5f) }
    var sparkles by rememberSaveable { mutableStateOf(true) }

    val colorSwatches = listOf(
        0xFF9D4EDD,
        0xFFFF4EC2,
        0xFF4CC9F0,
        0xFFFFD166,
        0xFF38E59B,
        0xFFFF5D8F,
        0xFFFFFFFF
    )

    val previewFrame = CustomFrame(
        id = "custom_frame_preview",
        creatorId = user.id,
        creatorName = user.username,
        title = title.ifBlank { "Mi Marco" },
        frameType = "STUDIO",
        primaryColorHex = primaryHex,
        secondaryColorHex = secondaryHex,
        stylePattern = style,
        thicknessDp = thickness,
        hasSparkles = sparkles
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Estudio de Marcos Gratis", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text("Diseña y publica marcos gratuitos para tu avatar", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(14.dp))

            FramedAvatar(
                avatarUri = user.avatarUri,
                presetAvatarKey = user.presetAvatarKey,
                customFrameOverride = previewFrame,
                size = 94.dp,
                contentDescription = "Vista previa de tu marco"
            )

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 28) title = it },
                label = { Text("Nombre del marco") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("studio_frame_title_input")
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("Estilo del diseño", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CustomFrameStyle.entries) { itemStyle ->
                    FilterChip(
                        selected = style == itemStyle,
                        onClick = { style = itemStyle },
                        label = { Text(itemStyle.displayName, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Color principal", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colorSwatches.forEach { hex ->
                    val selected = primaryHex == hex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else BorderSubtle, CircleShape)
                            .clickable { primaryHex = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Color secundario", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colorSwatches.forEach { hex ->
                    val selected = secondaryHex == hex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else BorderSubtle, CircleShape)
                            .clickable { secondaryHex = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Grosor del aro (${thickness.toInt()} dp)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            Slider(
                value = thickness,
                onValueChange = { thickness = it },
                valueRange = 2f..8f,
                colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPurple)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Destellos orbitales animados", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Switch(checked = sparkles, onCheckedChange = { sparkles = it })
            }

            Spacer(modifier = Modifier.height(16.dp))
            NeonGradientButton(
                text = "Publicar y usar gratis",
                enabled = title.trim().length >= 2,
                onClick = { onPublish(title, primaryHex, secondaryHex, style, thickness, sparkles) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "publish_studio_frame_button"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrantVerificationSheet(
    users: List<UserProfile>,
    currentUserId: String? = null,
    onAssignRank: (targetUserId: String, newRank: AppRank) -> Unit,
    onGrantCoins: (targetUserId: String, amount: Int) -> Unit = { _, _ -> },
    onBanUser: (targetUserId: String, reason: String) -> Unit = { _, _ -> },
    onUnbanUser: (targetUserId: String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var lastChangeFeedback by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("Panel del Dueño: Verificados, Monedas y Baneos", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(
                text = "Solo Dueño • Insignias, regalo de monedas y seguridad",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            if (!lastChangeFeedback.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessGreen.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lastChangeFeedback!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = SuccessGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            val orderedUsers = remember(users, currentUserId) {
                if (currentUserId == null) users
                else users.sortedByDescending { it.id == currentUserId }
            }

            orderedUsers.forEach { target ->
                val isSelf = target.id == currentUserId
                val isPrimaryOwner = target.email.equals(com.example.data.TxlRepository.PRIMARY_OWNER_EMAIL, ignoreCase = true)
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    cornerRadius = 18.dp,
                    borderColor = if (target.isBanned) DangerRose.copy(alpha = 0.65f) else BorderGlow
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FramedAvatar(
                                avatarUri = target.avatarUri,
                                presetAvatarKey = target.presetAvatarKey,
                                frame = target.avatarFrame,
                                frameId = target.avatarFrameId,
                                size = 44.dp,
                                contentDescription = target.username
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isSelf) "${target.username} (Tú)" else target.username,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    if (target.appRank != AppRank.NONE) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        VerifiedRankBadge(rank = target.appRank, size = 16.dp)
                                    }
                                    if (target.isBanned) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("BANEADO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DangerRose)
                                    }
                                }
                                Text(
                                    text = if (target.isBanned) "Bloqueado: ${target.banReason ?: "Actividad ilegal"}" else "${target.appRank.badgeTitle} • ${if (target.hasInfiniteCoins) "∞ 💎" else "${target.coinsCount} 💎"}",
                                    fontSize = 11.sp,
                                    color = if (target.isBanned) DangerRose else NeonPurpleLight
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        com.example.ui.components.RankVerificationSelectorGrid(
                            currentRank = target.appRank,
                            targetUserId = target.id,
                            isSelf = isSelf,
                            onSelectRank = { chosen ->
                                onAssignRank(target.id, chosen)
                                lastChangeFeedback = if (chosen == AppRank.NONE) {
                                    "Verificado quitado a ${target.username}"
                                } else {
                                    "${target.username} ahora tiene ${chosen.badgeTitle}"
                                }
                            }
                        )
                        if (!isSelf && !isPrimaryOwner) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(100, 500, 2000).forEach { giftAmt ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = RoleAdminGold.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, RoleAdminGold.copy(alpha = 0.7f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clickable {
                                                onGrantCoins(target.id, giftAmt)
                                                lastChangeFeedback = "+$giftAmt monedas regaladas a ${target.username}"
                                            }
                                            .testTag("sheet_grant_coins_${giftAmt}_${target.id}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("+$giftAmt 💎", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoleAdminGold)
                                        }
                                    }
                                }
                            }
                        }
                        if (!isSelf && !isPrimaryOwner) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (target.isBanned) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SuccessGreen.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, SuccessGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clickable {
                                            onUnbanUser(target.id)
                                            lastChangeFeedback = "${target.username} fue desbaneado"
                                        }
                                        .testTag("sheet_unban_${target.id}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("Quitar baneo (Restaurar acceso)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DangerRose.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, DangerRose),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clickable {
                                            onBanUser(target.id, "Actividad ilegal o incumplimiento grave de normas")
                                            lastChangeFeedback = "${target.username} fue baneado permanentemente"
                                        }
                                        .testTag("sheet_ban_${target.id}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("Banear usuario por actividad ilegal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerRose)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileMetricCard(
    value: String,
    label: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (highlighted) {
        Brush.horizontalGradient(listOf(NeonPurple, NeonPink))
    } else {
        Brush.horizontalGradient(listOf(SurfaceElevated, SurfaceElevated))
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgBrush)
            .border(1.dp, if (highlighted) Color.Transparent else BorderGlow, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text(label, fontSize = 11.sp, color = if (highlighted) TextPrimary else TextSecondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    user: UserProfile,
    customFrames: List<CustomFrame>,
    onDismiss: () -> Unit,
    onPickPhotoUri: (Uri, (String) -> Unit) -> Unit,
    onSave: (String, String, String, String, String?, String) -> Unit
) {
    var username by rememberSaveable { mutableStateOf(user.username) }
    var bio by rememberSaveable { mutableStateOf(user.bio) }
    var customStatus by rememberSaveable { mutableStateOf(user.customStatus) }
    var selectedFrameId by rememberSaveable { mutableStateOf(user.avatarFrameId) }
    var avatarUri by rememberSaveable { mutableStateOf(user.avatarUri) }
    var presetKey by rememberSaveable { mutableStateOf(user.presetAvatarKey) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickPhotoUri(uri) { savedPath ->
                avatarUri = savedPath
            }
        }
    }

    val statusPresets = listOf(
        "🎧 En sala de voz",
        "🎮 Buscando escuadra",
        "✨ Lanzando botellas",
        "🌙 Modo nocturno"
    )
    val avatarPresets = listOf("anime" to "Anime", "gaming" to "Cyber", "music" to "DJ Neón")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Personalizar Perfil", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(14.dp))

            FramedAvatar(
                avatarUri = avatarUri,
                presetAvatarKey = presetKey,
                frame = AvatarFrame.fromId(selectedFrameId),
                frameId = selectedFrameId,
                size = 92.dp,
                contentDescription = "Vista previa de avatar"
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = NeonPurple.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, NeonPink),
                modifier = Modifier
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("upload_profile_photo_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = NeonPink, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subir foto desde galería", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("O elige un avatar neón:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                avatarPresets.forEach { (key, label) ->
                    FilterChip(
                        selected = avatarUri == null && presetKey == key,
                        onClick = {
                            avatarUri = null
                            presetKey = key
                        },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { if (it.length <= 28) username = it },
                label = { Text("Nombre de usuario") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("edit_username_input")
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = customStatus,
                onValueChange = { if (it.length <= 60) customStatus = it },
                label = { Text("Estado personalizado (visible en chat)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("edit_status_input")
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) {
                items(statusPresets) { preset ->
                    FilterChip(
                        selected = customStatus == preset,
                        onClick = { customStatus = preset },
                        label = { Text(preset, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 160) bio = it },
                label = { Text("Bio corta (${bio.length}/160)") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("edit_bio_input")
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Elige tu Marco de Avatar",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(customFrames, key = { it.id }) { custom ->
                    val selected = selectedFrameId == custom.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) NeonPurple.copy(alpha = 0.25f) else SurfaceElevated)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) NeonPink else BorderSubtle,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFrameId = custom.id }
                            .padding(8.dp)
                    ) {
                        FramedAvatar(
                            avatarUri = avatarUri,
                            presetAvatarKey = presetKey,
                            frameId = custom.id,
                            customFrameOverride = custom,
                            size = 48.dp,
                            contentDescription = custom.title
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = custom.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) NeonPink else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                items(AvatarFrame.entries) { frame ->
                    val selected = selectedFrameId == frame.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) NeonPurple.copy(alpha = 0.25f) else SurfaceElevated)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) NeonPink else BorderSubtle,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFrameId = frame.id }
                            .padding(8.dp)
                            .testTag("select_frame_${frame.id}")
                    ) {
                        FramedAvatar(
                            avatarUri = avatarUri,
                            presetAvatarKey = presetKey,
                            frame = frame,
                            size = 48.dp,
                            contentDescription = frame.displayName
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = frame.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) NeonPink else TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            NeonGradientButton(
                text = "Guardar cambios",
                enabled = username.trim().length >= 3,
                onClick = { onSave(username, bio, customStatus, selectedFrameId, avatarUri, presetKey) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "save_profile_changes_button"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
