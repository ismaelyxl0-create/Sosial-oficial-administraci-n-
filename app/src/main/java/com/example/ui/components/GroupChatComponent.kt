package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppRank
import com.example.model.ChatMessage
import com.example.model.CommunityRole
import com.example.model.UserProfile
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerRose
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Componente reutilizable de Chat Grupal con diseño oscuro compatible con el tema de la app,
 * burbujas de mensaje estilizadas (direccionales), reacciones rápidas IRL/Gamer, notas de voz
 * y barra de entrada de texto.
 */
@Composable
fun GroupChatPanel(
    messages: List<ChatMessage>,
    currentUserId: String,
    myRole: CommunityRole,
    showUserStatus: Boolean,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    replyingTo: ChatMessage?,
    onSetReplyingTo: (ChatMessage?) -> Unit,
    onSendMessage: (content: String, replyToSnippet: String?) -> Unit,
    onReactMessage: (Long) -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onSelectUser: (UserProfile, CommunityRole) -> Unit,
    modifier: Modifier = Modifier,
    headerBannerText: String? = null,
    isRecordingVoiceNote: Boolean = false,
    recordingElapsedMs: Long = 0L,
    playingAudioMessageId: Long? = null,
    playingAudioProgress: Float = 0f,
    onRequestStartVoiceNote: () -> Unit = {},
    onStopAndSendVoiceNote: (replyToSnippet: String?) -> Unit = {},
    onCancelVoiceNote: () -> Unit = {},
    onTogglePlayAudio: (ChatMessage) -> Unit = {},
    applyBottomInsets: Boolean = true
) {
    val listState = rememberLazyListState()
    val quickEmotes = listOf("GG", "🔥", "IRL", "OP", "❤️", "🎮")

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepVoid.copy(alpha = 0.72f),
                        SurfaceDark.copy(alpha = 0.90f),
                        DeepVoid.copy(alpha = 0.96f)
                    )
                )
            )
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("group_chat_message_list"),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!headerBannerText.isNullOrBlank()) {
                item(key = "chat_header_banner") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceCard.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, BorderGlow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonPink)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = headerBannerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (messages.isEmpty()) {
                item(key = "chat_empty_state") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceCard.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = NeonPurpleLight,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chat del directo abierto",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Saluda al streamer, manda un GG o comparte un audio con el grupo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val isMine = msg.sender.id == currentUserId
                val canDelete = myRole == CommunityRole.ADMIN ||
                    (myRole == CommunityRole.CO_ADMIN && msg.senderRole != CommunityRole.ADMIN) ||
                    isMine
                StyledGroupChatBubble(
                    message = msg,
                    isMine = isMine,
                    showUserStatus = showUserStatus,
                    canDelete = canDelete,
                    isPlayingAudio = playingAudioMessageId == msg.id,
                    audioProgress = if (playingAudioMessageId == msg.id) playingAudioProgress else 0f,
                    onAvatarOrNameClick = { onSelectUser(msg.sender, msg.senderRole) },
                    onReact = { onReactMessage(msg.id) },
                    onReply = { onSetReplyingTo(msg) },
                    onDelete = { onDeleteMessage(msg) },
                    onTogglePlayAudio = { onTogglePlayAudio(msg) }
                )
            }
        }

        GroupChatInputBar(
            inputText = inputText,
            onInputTextChange = onInputTextChange,
            quickEmotes = quickEmotes,
            replyingTo = replyingTo,
            onClearReply = { onSetReplyingTo(null) },
            isRecordingVoiceNote = isRecordingVoiceNote,
            recordingElapsedMs = recordingElapsedMs,
            onQuickEmoteClick = { emote ->
                onSendMessage(emote, replyingTo?.content?.take(60))
                onSetReplyingTo(null)
            },
            onSendClick = {
                if (inputText.isNotBlank()) {
                    onSendMessage(inputText, replyingTo?.content?.take(60))
                    onInputTextChange("")
                    onSetReplyingTo(null)
                }
            },
            onRequestStartVoiceNote = onRequestStartVoiceNote,
            onStopAndSendVoiceNote = {
                onStopAndSendVoiceNote(replyingTo?.content?.take(60))
                onSetReplyingTo(null)
            },
            onCancelVoiceNote = onCancelVoiceNote,
            applyBottomInsets = applyBottomInsets
        )
    }
}

@Composable
fun StyledGroupChatBubble(
    message: ChatMessage,
    isMine: Boolean,
    showUserStatus: Boolean,
    canDelete: Boolean,
    isPlayingAudio: Boolean,
    audioProgress: Float,
    onAvatarOrNameClick: () -> Unit,
    onReact: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onTogglePlayAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isMine) 20.dp else 6.dp,
        bottomEnd = if (isMine) 6.dp else 20.dp
    )
    val bubbleBrush = if (isMine) {
        Brush.linearGradient(
            listOf(
                NeonPurple.copy(alpha = 0.28f),
                SurfaceElevated.copy(alpha = 0.96f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                SurfaceCard.copy(alpha = 0.96f),
                SurfaceDark.copy(alpha = 0.94f)
            )
        )
    }
    val borderStroke = if (isMine) {
        BorderStroke(1.dp, NeonPurpleLight.copy(alpha = 0.55f))
    } else {
        BorderStroke(1.dp, BorderSubtle)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chat_bubble_${message.id}"),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 2.dp)
                    .clickable(onClick = onAvatarOrNameClick)
            ) {
                FramedAvatar(
                    avatarUri = message.sender.avatarUri,
                    presetAvatarKey = message.sender.presetAvatarKey,
                    frame = message.sender.avatarFrame,
                    frameId = message.sender.avatarFrameId,
                    size = 40.dp,
                    contentDescription = message.sender.username
                )
            }
        }

        Surface(
            shape = bubbleShape,
            color = Color.Transparent,
            border = borderStroke,
            modifier = Modifier.widthIn(min = 180.dp, max = 330.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(bubbleBrush, bubbleShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = if (isMine) "${message.sender.username} (Tú)" else message.sender.username,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isMine) NeonPurpleLight else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable(onClick = onAvatarOrNameClick)
                    )
                    if (message.sender.appRank != AppRank.NONE) {
                        VerifiedRankBadge(rank = message.sender.appRank, size = 14.dp)
                    }
                    RoleBadge(role = message.senderRole)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatRelativeTime(message.timestampMillis),
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                if (showUserStatus && message.sender.customStatus.isNotBlank()) {
                    Text(
                        text = message.sender.customStatus,
                        fontSize = 11.sp,
                        color = NeonCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!message.replyToSnippet.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = DeepVoid.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "↩ ${message.replyToSnippet}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = DeepVoid.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onReact)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Me gusta",
                                tint = NeonPink,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${message.reactionCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onReply)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Responder",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Responder",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Eliminar",
                                tint = TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isMine) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 2.dp)
                    .clickable(onClick = onAvatarOrNameClick)
            ) {
                FramedAvatar(
                    avatarUri = message.sender.avatarUri,
                    presetAvatarKey = message.sender.presetAvatarKey,
                    frame = message.sender.avatarFrame,
                    frameId = message.sender.avatarFrameId,
                    size = 36.dp,
                    contentDescription = message.sender.username
                )
            }
        }
    }
}

@Composable
fun GroupChatInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    quickEmotes: List<String>,
    replyingTo: ChatMessage?,
    onClearReply: () -> Unit,
    isRecordingVoiceNote: Boolean,
    recordingElapsedMs: Long,
    onQuickEmoteClick: (String) -> Unit,
    onSendClick: () -> Unit,
    onRequestStartVoiceNote: () -> Unit,
    onStopAndSendVoiceNote: () -> Unit,
    onCancelVoiceNote: () -> Unit,
    applyBottomInsets: Boolean = true
) {
    Surface(
        color = SurfaceDark.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, BorderSubtle.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (applyBottomInsets) Modifier.navigationBarsPadding().imePadding() else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            replyingTo?.let { target ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = NeonPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Respondiendo a ${target.sender.username}: ${target.content.take(40)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = onClearReply, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cancelar respuesta",
                                tint = TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            if (!isRecordingVoiceNote && quickEmotes.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    items(quickEmotes) { emote ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, BorderGlow),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onQuickEmoteClick(emote) }
                        ) {
                            Text(
                                text = emote,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

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
                                .testTag("group_cancel_audio_note_button")
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
                                .testTag("group_send_audio_note_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar nota de voz",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        placeholder = { Text("Escribe en el chat en vivo...", color = TextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSendClick() }),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPink,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonPink
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("group_chat_input")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = SurfaceElevated,
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onRequestStartVoiceNote)
                            .testTag("group_record_audio_note_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Grabar nota de voz",
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val canSend = inputText.isNotBlank()
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) Brush.linearGradient(listOf(NeonPurple, NeonPink))
                                else Brush.linearGradient(listOf(SurfaceElevated, SurfaceElevated))
                            )
                            .clickable(onClick = onSendClick)
                            .testTag("group_chat_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = if (canSend) TextPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
