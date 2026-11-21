package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AppRank
import com.example.model.AppSettings
import com.example.model.ChatMessage
import com.example.model.Community
import com.example.model.CommunityRole
import com.example.model.RoomGiftEvent
import com.example.model.UserProfile
import com.example.ui.components.GiftCelebrationOverlay
import com.example.ui.components.GiftGallerySheet
import com.example.ui.components.GroupChatPanel
import com.example.ui.components.TxlBackdrop
import com.example.ui.components.UserProfilePreviewSheet
import com.example.ui.components.shareNexoLiveApp
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.DangerRose
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RoleAdminGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    community: Community?,
    messages: List<ChatMessage>,
    currentUserId: String,
    currentUserRank: AppRank,
    currentUserIsDeveloper: Boolean = false,
    settings: AppSettings,
    roomNotice: String?,
    onClearNotice: () -> Unit,
    onSendMessage: (content: String, replyTo: String?) -> Unit,
    onReactMessage: (Long) -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onPromoteCoAdmin: (String) -> Unit,
    onDemoteMember: (String) -> Unit,
    onAssignAppRank: (String, AppRank) -> Unit,
    onUpdateRoomBackground: (Uri) -> Unit = {},
    onResetRoomBackground: () -> Unit = {},
    onDeleteRoom: () -> Unit = {},
    onOpenVoiceRoom: () -> Unit,
    onBack: () -> Unit,
    isRecordingVoiceNote: Boolean = false,
    recordingElapsedMs: Long = 0L,
    playingAudioMessageId: Long? = null,
    playingAudioProgress: Float = 0f,
    onStartVoiceNote: (hasPermission: Boolean) -> Unit = {},
    onStopAndSendVoiceNote: (replyTo: String?) -> Unit = {},
    onCancelVoiceNote: () -> Unit = {},
    onTogglePlayAudio: (ChatMessage) -> Unit = {},
    onBanUser: (String, String) -> Unit = { _, _ -> },
    onUnbanUser: (String) -> Unit = {},
    currentUser: UserProfile? = null,
    allUsers: List<UserProfile> = emptyList(),
    activeGiftBanner: RoomGiftEvent? = null,
    onEnsureSync: () -> Unit = {},
    onSendGift: (recipientId: String, giftId: String, comboCount: Int, useBagItemId: String?) -> Unit = { _, _, _, _ -> },
    onClaimFreeCoins: () -> Unit = {},
    onFollowHost: (String) -> Unit = {},
    onGrantCoins: (targetUserId: String, amount: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by rememberSaveable { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedUser by remember { mutableStateOf<Pair<UserProfile, CommunityRole>?>(null) }
    var showRoomSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showGiftSheet by rememberSaveable { mutableStateOf(false) }
    val myRole = community?.myRole ?: CommunityRole.MEMBER

    LaunchedEffect(Unit) {
        onEnsureSync()
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val voiceNotePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        onStartVoiceNote(granted)
    }

    val roomBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateRoomBackground(uri)
        }
    }

    LaunchedEffect(roomNotice) {
        if (!roomNotice.isNullOrBlank()) {
            snackbarHostState.showSnackbar(roomNotice)
            onClearNotice()
        }
    }

    TxlBackdrop(
        neonGlowEnabled = settings.neonGlowEnabled,
        backgroundUri = community?.backgroundUri ?: settings.appBackgroundUri,
        backgroundDim = settings.appBackgroundDim
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Surface(
                    color = SurfaceDark.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, BorderGlow.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("group_chat_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NeonPink)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = community?.name ?: "Chat del Directo",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${community?.category ?: "IRL & Gaming"} • ${community?.memberCount ?: 0} seguidores",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        IconButton(
                            onClick = { showGiftSheet = true },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("group_open_gifts_button")
                        ) {
                            Icon(Icons.Filled.CardGiftcard, contentDescription = "Enviar regalo", tint = NeonPink)
                        }
                        IconButton(
                            onClick = { shareNexoLiveApp(context, community?.name) },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("group_share_room_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Compartir sala", tint = NeonCyan)
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = NeonPurple.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onOpenVoiceRoom)
                                .testTag("group_to_voice_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Videocam, contentDescription = null, tint = NeonPink, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("En Vivo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                        if (myRole == CommunityRole.ADMIN || myRole == CommunityRole.CO_ADMIN ||
                            currentUserIsDeveloper || currentUserRank == AppRank.OWNER
                        ) {
                            IconButton(
                                onClick = { showRoomSettingsSheet = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("group_room_settings_button")
                            ) {
                                Icon(Icons.Filled.Settings, contentDescription = "Personalizar o eliminar sala", tint = RoleAdminGold)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                GroupChatPanel(
                    messages = messages,
                    currentUserId = currentUserId,
                    myRole = myRole,
                    showUserStatus = settings.showStatusInChat,
                    inputText = inputText,
                    onInputTextChange = { inputText = it },
                    replyingTo = replyingTo,
                    onSetReplyingTo = { replyingTo = it },
                    onSendMessage = onSendMessage,
                    onReactMessage = onReactMessage,
                    onDeleteMessage = onDeleteMessage,
                    onSelectUser = { user, role -> selectedUser = user to role },
                    headerBannerText = community?.description ?: "Chat grupal del stream IRL & Gamer en tiempo real.",
                    isRecordingVoiceNote = isRecordingVoiceNote,
                    recordingElapsedMs = recordingElapsedMs,
                    playingAudioMessageId = playingAudioMessageId,
                    playingAudioProgress = playingAudioProgress,
                    onRequestStartVoiceNote = {
                        if (hasAudioPermission) {
                            onStartVoiceNote(true)
                        } else {
                            voiceNotePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopAndSendVoiceNote = onStopAndSendVoiceNote,
                    onCancelVoiceNote = onCancelVoiceNote,
                    onTogglePlayAudio = onTogglePlayAudio,
                    applyBottomInsets = true,
                    modifier = Modifier.fillMaxSize()
                )
                GiftCelebrationOverlay(
                    event = activeGiftBanner,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }

    if (showGiftSheet) {
        val recipients = remember(messages, allUsers) {
            (messages.map { it.sender } + allUsers).distinctBy { it.id }
        }
        GiftGallerySheet(
            currentUser = currentUser,
            recipients = recipients,
            onDismiss = { showGiftSheet = false },
            onSendGift = onSendGift,
            onClaimFreeCoins = onClaimFreeCoins
        )
    }

    selectedUser?.let { (user, role) ->
        UserProfilePreviewSheet(
            user = user,
            roleInRoom = role,
            myRoleInRoom = myRole,
            currentUserRank = currentUserRank,
            currentUserIsDeveloper = currentUserIsDeveloper,
            isCurrentUser = user.id == currentUserId,
            onDismiss = { selectedUser = null },
            onPromoteCoAdmin = { onPromoteCoAdmin(user.id) },
            onDemoteMember = { onDemoteMember(user.id) },
            onAssignAppRank = { rank -> onAssignAppRank(user.id, rank) },
            onGrantCoins = { amount -> onGrantCoins(user.id, amount) },
            onBanUser = { reason -> onBanUser(user.id, reason) },
            onUnbanUser = { onUnbanUser(user.id) },
            onFollowUser = { onFollowHost(user.id) }
        )
    }

    if (showRoomSettingsSheet && community != null) {
        val isAdminOrOwner = myRole == CommunityRole.ADMIN || currentUserIsDeveloper || currentUserRank == AppRank.OWNER
        ModalBottomSheet(
            onDismissRequest = { showRoomSettingsSheet = false },
            containerColor = SurfaceCard,
            contentColor = TextPrimary
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text("Personalizar Sala", style = MaterialTheme.typography.headlineMedium, color = RoleAdminGold)
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            roomBgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .testTag("group_pick_room_bg_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Wallpaper, contentDescription = null, tint = NeonPink)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Personalizar fondo del chat", style = MaterialTheme.typography.titleSmall)
                            Text("Elige cualquier foto de tu galería", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
                if (!community.backgroundUri.isNullOrBlank()) {
                    TextButton(onClick = { onResetRoomBackground() }) {
                        Text("Restaurar fondo predeterminado", color = NeonCyan)
                    }
                }
                if (isAdminOrOwner) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            showRoomSettingsSheet = false
                            showDeleteConfirm = true
                        },
                        modifier = Modifier.testTag("group_delete_room_button")
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = DangerRose, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar canal permanentemente", color = DangerRose, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteConfirm && community != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Eliminar canal") },
            text = { Text("¿Seguro que deseas eliminar \"${community.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteRoom()
                }) {
                    Text("Eliminar", color = DangerRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}
