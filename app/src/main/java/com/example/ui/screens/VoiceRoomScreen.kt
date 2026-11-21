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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.model.RoomEventLog
import com.example.model.RoomGiftEvent
import com.example.model.UserProfile
import com.example.model.VoiceParticipant
import com.example.ui.components.AudioMessageBubbleContent
import com.example.ui.components.CommunityCoverImage
import com.example.ui.components.FramedAvatar
import com.example.ui.components.GiftCelebrationOverlay
import com.example.ui.components.GiftGallerySheet
import com.example.ui.components.GlassCard
import com.example.ui.components.LiveCameraPreview
import com.example.ui.components.RoleBadge
import com.example.ui.components.StyledGroupChatBubble
import com.example.ui.components.TxlBackdrop
import com.example.ui.components.UserProfilePreviewSheet
import com.example.ui.components.VerifiedRankBadge
import com.example.ui.components.formatAudioDuration
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.shareNexoLiveApp
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerRose
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.RoleAdminGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRoomScreen(
    community: Community?,
    participants: List<VoiceParticipant>,
    events: List<RoomEventLog>,
    messages: List<ChatMessage>,
    currentUserId: String,
    currentUserRank: AppRank,
    currentUserIsDeveloper: Boolean = false,
    micAmplitude: Float,
    settings: AppSettings,
    roomNotice: String?,
    onClearNotice: () -> Unit,
    onEnterRoom: () -> Unit,
    onLeaveRoom: () -> Unit,
    onToggleMute: (currentlyMuted: Boolean, hasPermission: Boolean) -> Unit,
    onToggleHandRaised: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onModerateMute: (String, Boolean) -> Unit,
    onKickParticipant: (String) -> Unit,
    onChangeRole: (String, CommunityRole) -> Unit,
    onAssignAppRank: (String, AppRank) -> Unit,
    onToggleRoomLock: (Boolean) -> Unit,
    onClearEvents: () -> Unit,
    onUpdateRoomBackground: (Uri) -> Unit = {},
    onResetRoomBackground: () -> Unit = {},
    onDeleteRoom: () -> Unit = {},
    onOpenGroupChat: () -> Unit,
    onBack: () -> Unit,
    isRecordingVoiceNote: Boolean = false,
    recordingElapsedMs: Long = 0L,
    playingAudioMessageId: Long? = null,
    playingAudioProgress: Float = 0f,
    onStartVoiceNote: (hasPermission: Boolean) -> Unit = {},
    onStopAndSendVoiceNote: () -> Unit = {},
    onCancelVoiceNote: () -> Unit = {},
    onTogglePlayAudio: (ChatMessage) -> Unit = {},
    onBanUser: (String, String) -> Unit = { _, _ -> },
    onUnbanUser: (String) -> Unit = {},
    currentUser: UserProfile? = null,
    allUsers: List<UserProfile> = emptyList(),
    remoteSpeakingLevels: Map<String, Float> = emptyMap(),
    activeGiftBanner: RoomGiftEvent? = null,
    onSendGift: (recipientId: String, giftId: String, comboCount: Int, useBagItemId: String?) -> Unit = { _, _, _, _ -> },
    onClaimFreeCoins: () -> Unit = {},
    onFollowHost: (String) -> Unit = {},
    onGrantCoins: (targetUserId: String, amount: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var messageInput by rememberSaveable { mutableStateOf("") }
    var selectedUserForSheet by remember { mutableStateOf<Pair<UserProfile, CommunityRole>?>(null) }
    var showRoomAdminSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showGiftSheet by rememberSaveable { mutableStateOf(false) }
    var isIrlCameraMode by rememberSaveable { mutableStateOf(false) }
    var useFrontCamera by rememberSaveable { mutableStateOf(true) }

    val roomBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateRoomBackground(uri)
        }
    }

    val meParticipant = participants.firstOrNull { it.user.id == currentUserId }
    val myRole = community?.myRole ?: meParticipant?.role ?: CommunityRole.MEMBER

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            isIrlCameraMode = true
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        val currentlyMuted = meParticipant?.isMuted ?: true
        if (currentlyMuted) {
            onToggleMute(true, granted)
        }
    }

    val voiceNotePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        onStartVoiceNote(granted)
    }

    LaunchedEffect(Unit) {
        onEnterRoom()
    }

    LaunchedEffect(roomNotice) {
        if (!roomNotice.isNullOrBlank()) {
            snackbarHostState.showSnackbar(roomNotice)
            onClearNotice()
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp).testTag("voice_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = community?.name ?: "Sala de Voz",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (community?.isVoiceLocked == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Filled.Lock, contentDescription = "Sala bloqueada", tint = RoleAdminGold, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = "${community?.category ?: "En Vivo"} • ${participants.size} espectadores • ${myRole.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = { shareNexoLiveApp(context, community?.name) },
                        modifier = Modifier.size(44.dp).testTag("voice_share_room_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Compartir sala", tint = NeonCyan)
                    }

                    // Switch to full Group Text Chat
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SurfaceElevated,
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable(onClick = onOpenGroupChat)
                            .testTag("voice_to_group_chat_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat Grupal", tint = NeonCyan, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Grupal", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (myRole == CommunityRole.ADMIN || myRole == CommunityRole.CO_ADMIN ||
                        currentUserIsDeveloper || currentUserRank == AppRank.OWNER
                    ) {
                        IconButton(
                            onClick = { showRoomAdminSheet = true },
                            modifier = Modifier.size(48.dp).testTag("voice_admin_controls_button")
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = "Control y fondo de Sala", tint = RoleAdminGold)
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark.copy(alpha = 0.96f))
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Integrated voice controls bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isMuted = meParticipant?.isMuted ?: true
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isMuted) SurfaceElevated else NeonPurple.copy(alpha = 0.3f),
                            border = BorderStroke(1.5.dp, if (isMuted) BorderSubtle else NeonPink),
                            modifier = Modifier
                                .height(48.dp)
                                .clickable {
                                    if (isMuted && !hasAudioPermission) {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        onToggleMute(isMuted, hasAudioPermission)
                                    }
                                }
                                .testTag("voice_mute_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                    contentDescription = if (isMuted) "Activar micrófono" else "Silenciar micrófono",
                                    tint = if (isMuted) TextSecondary else NeonPink,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isMuted) "Micro off" else "Hablando",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary
                                )
                            }
                        }

                        val isHandRaised = meParticipant?.isHandRaised ?: false
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isHandRaised) RoleAdminGold.copy(alpha = 0.22f) else SurfaceElevated,
                            border = BorderStroke(1.dp, if (isHandRaised) RoleAdminGold else BorderSubtle),
                            modifier = Modifier
                                .height(48.dp)
                                .clickable(onClick = onToggleHandRaised)
                                .testTag("voice_raise_hand_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PanTool,
                                    contentDescription = "Levantar mano",
                                    tint = if (isHandRaised) RoleAdminGold else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHandRaised) "Mano arriba" else "Pedir turno",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = NeonPink.copy(alpha = 0.22f),
                            border = BorderStroke(1.2.dp, NeonPink),
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { showGiftSheet = true }
                                .testTag("voice_open_gifts_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.CardGiftcard, contentDescription = "Enviar regalo 3D", tint = NeonPink)
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = DangerRose.copy(alpha = 0.24f),
                            border = BorderStroke(1.dp, DangerRose),
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    onLeaveRoom()
                                    onBack()
                                }
                                .testTag("voice_leave_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.CallEnd, contentDescription = "Salir de sala", tint = DangerRose)
                            }
                        }
                    }

                    // Integrated real-time text & audio message composer
                    if (isRecordingVoiceNote) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = DangerRose.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, DangerRose.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onCancelVoiceNote,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("voice_cancel_audio_note_button")
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Cancelar audio", tint = DangerRose)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(DangerRose)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Grabando audio • ${formatAudioDuration(recordingElapsedMs)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(NeonPurple, NeonPink)))
                                        .clickable(onClick = onStopAndSendVoiceNote)
                                        .testTag("voice_send_audio_note_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar nota de voz", tint = TextPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = messageInput,
                                onValueChange = { messageInput = it },
                                placeholder = { Text("Escribe o envía audio...", color = TextMuted) },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonPink,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = SurfaceCard,
                                    unfocusedContainerColor = SurfaceCard,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f).testTag("voice_chat_input")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = SurfaceElevated,
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        if (hasAudioPermission) {
                                            onStartVoiceNote(true)
                                        } else {
                                            voiceNotePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                    .testTag("voice_record_audio_note_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Mic, contentDescription = "Grabar mensaje de audio", tint = NeonCyan, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(NeonPurple, NeonPink)))
                                    .clickable {
                                        if (messageInput.isNotBlank()) {
                                            onSendMessage(messageInput)
                                            messageInput = ""
                                        }
                                    }
                                    .testTag("voice_chat_send_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar mensaje", tint = TextPrimary, modifier = Modifier.size(20.dp))
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Live Stream Broadcast Stage (IRL CameraX & Gamer HUD)
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = NeonPink.copy(alpha = 0.55f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DeepVoid)
                            ) {
                                if (isIrlCameraMode && hasCameraPermission) {
                                    LiveCameraPreview(
                                        useFrontCamera = useFrontCamera,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    CommunityCoverImage(
                                        coverKey = community?.coverKey ?: "fps",
                                        customBackgroundUri = community?.backgroundUri,
                                        contentDescription = community?.name ?: "Directo en vivo",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        DeepVoid.copy(alpha = 0.25f),
                                                        DeepVoid.copy(alpha = 0.72f)
                                                    )
                                                )
                                            )
                                    )
                                }

                                // Top HUD: EN VIVO + Mode pill + Camera switch
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = NeonPink,
                                        modifier = Modifier
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "EN VIVO • ${if (isIrlCameraMode) "IRL CAM" else "GAMER"}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TextPrimary
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (isIrlCameraMode && hasCameraPermission) {
                                            Surface(
                                                shape = CircleShape,
                                                color = DeepVoid.copy(alpha = 0.75f),
                                                border = BorderStroke(1.dp, BorderGlow),
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clickable { useFrontCamera = !useFrontCamera }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Cameraswitch,
                                                        contentDescription = "Cambiar cámara",
                                                        tint = TextPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = DeepVoid.copy(alpha = 0.78f),
                                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
                                        ) {
                                            Text(
                                                text = "60 FPS • HD",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonCyan,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Bottom Overlay: Stream title & Mode Toggle
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = community?.category ?: "IRL & Gaming",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonPurpleLight
                                        )
                                        Text(
                                            text = if (isIrlCameraMode && !hasCameraPermission) {
                                                "Activa el permiso de cámara para transmitir IRL"
                                            } else {
                                                "${participants.size} espectadores viendo ahora"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (!isIrlCameraMode) NeonPurple else DeepVoid.copy(alpha = 0.8f),
                                            border = BorderStroke(1.dp, if (!isIrlCameraMode) NeonPurpleLight else BorderSubtle),
                                            modifier = Modifier.clickable { isIrlCameraMode = false }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.SportsEsports, contentDescription = "Modo Gamer", tint = TextPrimary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Gamer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (isIrlCameraMode) NeonPink else DeepVoid.copy(alpha = 0.8f),
                                            border = BorderStroke(1.dp, if (isIrlCameraMode) NeonPink else BorderSubtle),
                                            modifier = Modifier.clickable {
                                                if (hasCameraPermission) {
                                                    isIrlCameraMode = true
                                                } else {
                                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.Videocam, contentDescription = "Cámara IRL", tint = TextPrimary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("IRL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Speaker & Streamer Stage Card
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Escenario en Vivo", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("Toca un avatar para ver perfil / rol", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(participants, key = { it.id }) { vp ->
                                    val isMe = vp.user.id == currentUserId
                                    val remoteRms = remoteSpeakingLevels[vp.user.id] ?: 0f
                                    val speakingAmp = if (isMe && !vp.isMuted) micAmplitude else if (remoteRms > 0.02f) remoteRms else if (vp.isSpeaking) 0.55f else 0f
                                    val isCurrentlySpeaking = (isMe && !vp.isMuted && micAmplitude > 0.04f) ||
                                        (!isMe && remoteRms > 0.04f) ||
                                        (vp.isSpeaking && !vp.isMuted)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(96.dp)
                                            .clickable { selectedUserForSheet = vp.user to vp.role }
                                            .testTag("voice_participant_${vp.user.id}")
                                    ) {
                                        Box(contentAlignment = Alignment.BottomEnd) {
                                            FramedAvatar(
                                                avatarUri = vp.user.avatarUri,
                                                presetAvatarKey = vp.user.presetAvatarKey,
                                                frame = vp.user.avatarFrame,
                                                frameId = vp.user.avatarFrameId,
                                                size = 64.dp,
                                                isSpeaking = isCurrentlySpeaking,
                                                speakingAmplitude = speakingAmp,
                                                contentDescription = vp.user.username
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = if (vp.isMuted) SurfaceElevated else SuccessGreen,
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (vp.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                                        contentDescription = null,
                                                        tint = if (vp.isMuted) TextSecondary else DeepVoid,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = vp.user.username,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (vp.user.appRank != AppRank.NONE) {
                                                Spacer(modifier = Modifier.width(3.dp))
                                                VerifiedRankBadge(rank = vp.user.appRank, size = 14.dp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        RoleBadge(role = vp.role)
                                        if (vp.user.customStatus.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = vp.user.customStatus,
                                                fontSize = 10.sp,
                                                color = NeonPurpleLight,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Event Logs Timeline
                if (events.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Registro de eventos", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            events.take(4).forEach { ev ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceCard.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatRelativeTime(ev.timestampMillis),
                                            fontSize = 11.sp,
                                            color = NeonPink,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${ev.actorName} ${ev.description}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Integrated Text Chat Header
                item {
                    Text("Chat en vivo de la sala", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }

                if (messages.isEmpty()) {
                    item {
                        Text(
                            text = "Sin mensajes en la sala todavía. Escribe el primero.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        val isMine = msg.sender.id == currentUserId
                        val canDelete = myRole == CommunityRole.ADMIN ||
                            (myRole == CommunityRole.CO_ADMIN && msg.senderRole != CommunityRole.ADMIN) ||
                            isMine
                        StyledGroupChatBubble(
                            message = msg,
                            isMine = isMine,
                            showUserStatus = settings.showStatusInChat,
                            canDelete = canDelete,
                            isPlayingAudio = playingAudioMessageId == msg.id,
                            audioProgress = if (playingAudioMessageId == msg.id) playingAudioProgress else 0f,
                            onAvatarOrNameClick = { selectedUserForSheet = msg.sender to msg.senderRole },
                            onReact = { onSendMessage("❤️") },
                            onReply = { messageInput = "@${msg.sender.username} " },
                            onDelete = { onDeleteMessage(msg) },
                            onTogglePlayAudio = { onTogglePlayAudio(msg) }
                        )
                    }
                }
            }
            GiftCelebrationOverlay(
                event = activeGiftBanner,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            }
        }
    }

    if (showGiftSheet) {
        val recipients = remember(participants, allUsers) {
            (participants.map { it.user } + allUsers).distinctBy { it.id }
        }
        GiftGallerySheet(
            currentUser = currentUser,
            recipients = recipients,
            onDismiss = { showGiftSheet = false },
            onSendGift = onSendGift,
            onClaimFreeCoins = onClaimFreeCoins
        )
    }

    selectedUserForSheet?.let { (user, role) ->
        val vp = participants.firstOrNull { it.user.id == user.id }
        UserProfilePreviewSheet(
            user = user,
            roleInRoom = role,
            myRoleInRoom = myRole,
            currentUserRank = currentUserRank,
            currentUserIsDeveloper = currentUserIsDeveloper,
            isCurrentUser = user.id == currentUserId,
            onDismiss = { selectedUserForSheet = null },
            onMuteToggle = if (vp != null) { mute -> onModerateMute(user.id, mute) } else null,
            isParticipantMuted = vp?.isMuted ?: false,
            onPromoteCoAdmin = { onChangeRole(user.id, CommunityRole.CO_ADMIN) },
            onDemoteMember = { onChangeRole(user.id, CommunityRole.MEMBER) },
            onKick = if (vp != null) { { onKickParticipant(user.id) } } else null,
            onAssignAppRank = { rank -> onAssignAppRank(user.id, rank) },
            onGrantCoins = { amount -> onGrantCoins(user.id, amount) },
            onBanUser = { reason -> onBanUser(user.id, reason) },
            onUnbanUser = { onUnbanUser(user.id) },
            onFollowUser = { onFollowHost(user.id) }
        )
    }

    if (showRoomAdminSheet && community != null) {
        val isAdminOrOwner = myRole == CommunityRole.ADMIN || currentUserIsDeveloper || currentUserRank == AppRank.OWNER
        ModalBottomSheet(
            onDismissRequest = { showRoomAdminSheet = false },
            containerColor = SurfaceCard,
            contentColor = TextPrimary
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text("Control y Fondo de Sala", style = MaterialTheme.typography.headlineMedium, color = RoleAdminGold)
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
                        .testTag("voice_pick_room_bg_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Wallpaper, contentDescription = null, tint = NeonPink)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Personalizar fondo de la sala", style = MaterialTheme.typography.titleSmall)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (community.isVoiceLocked) Icons.Filled.Lock else Icons.Filled.LockOpen, contentDescription = null, tint = NeonPink)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Bloquear sala de voz", style = MaterialTheme.typography.titleSmall)
                                Text("Solo miembros invitados podrán hablar", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Switch(
                            checked = community.isVoiceLocked,
                            onCheckedChange = { onToggleRoomLock(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                onClearEvents()
                                showRoomAdminSheet = false
                            }
                        ) {
                            Text("Limpiar eventos", color = TextSecondary)
                        }
                        TextButton(
                            onClick = {
                                showRoomAdminSheet = false
                                showDeleteConfirm = true
                            },
                            modifier = Modifier.testTag("voice_delete_room_button")
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = DangerRose, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar sala", color = DangerRose, fontWeight = FontWeight.Bold)
                        }
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
            title = { Text("Eliminar sala") },
            text = { Text("¿Seguro que deseas eliminar \"${community.name}\"? Esta acción no se puede deshacer.") },
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

@Composable
private fun VoiceChatBubble(
    message: ChatMessage,
    showStatus: Boolean,
    canDelete: Boolean,
    isPlayingAudio: Boolean,
    audioProgress: Float,
    onTogglePlayAudio: () -> Unit,
    onUserClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, BorderGlow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.clickable(onClick = onUserClick)) {
                FramedAvatar(
                    avatarUri = message.sender.avatarUri,
                    presetAvatarKey = message.sender.presetAvatarKey,
                    frame = message.sender.avatarFrame,
                    frameId = message.sender.avatarFrameId,
                    size = 40.dp,
                    contentDescription = message.sender.username
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.sender.username,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        modifier = Modifier.clickable(onClick = onUserClick)
                    )
                    if (message.sender.appRank != AppRank.NONE) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedRankBadge(rank = message.sender.appRank, size = 15.dp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    RoleBadge(role = message.senderRole)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(formatRelativeTime(message.timestampMillis), fontSize = 10.sp, color = TextMuted)
                }
                if (showStatus && message.sender.customStatus.isNotBlank()) {
                    Text(
                        text = message.sender.customStatus,
                        fontSize = 11.sp,
                        color = NeonPurpleLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (message.isAudioMessage) {
                    AudioMessageBubbleContent(
                        messageId = message.id,
                        durationMs = message.audioDurationMs,
                        isPlaying = isPlayingAudio,
                        progress = audioProgress,
                        onTogglePlay = onTogglePlayAudio
                    )
                } else {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
