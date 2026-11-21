package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.VoiceAudioEngine
import com.example.audio.VoiceNotePlayer
import com.example.audio.VoiceNoteRecorder
import com.example.data.MePlusDrawOutcome
import com.example.data.TxlRepository
import com.example.data.local.PreferencesStore
import com.example.data.local.TxlDatabase
import com.example.data.remote.FirestoreSyncRepository
import com.example.model.AppNotification
import com.example.model.AppRank
import com.example.model.AppSettings
import com.example.model.ChatMessage
import com.example.model.Community
import com.example.model.CommunityPost
import com.example.model.CommunityRole
import com.example.model.CustomFrame
import com.example.model.CustomFrameStyle
import com.example.model.DriftingBottle
import com.example.model.PostComment
import com.example.model.RoomEventLog
import com.example.model.RoomGiftEvent
import com.example.model.UserProfile
import com.example.model.VoiceParticipant
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = TxlDatabase.getInstance(appContext)
    private val prefs = PreferencesStore(appContext)
    private val remoteSync = FirestoreSyncRepository(appContext)
    val repository = TxlRepository(appContext, database, prefs, remoteSync)
}

class AuthViewModel(private val repository: TxlRepository) : ViewModel() {
    val currentUser: StateFlow<UserProfile?> = repository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val hasAnyUser: StateFlow<Boolean?> = repository.observeHasAnyUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeSeedData()
            repository.restoreVerifiedRemoteSession()
        }
    }

    fun clearError() {
        _authError.value = null
    }

    fun setAuthError(message: String?) {
        _authError.value = message
    }

    fun setBusy(busy: Boolean) {
        _isBusy.value = busy
    }

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _isBusy.value = true
            _authError.value = null
            repository.signInWithGoogleIdToken(idToken)
                .onFailure { _authError.value = it.message ?: "No se pudo verificar tu cuenta de Google." }
            _isBusy.value = false
        }
    }
}

class MainViewModel(
    private val repository: TxlRepository,
    val currentUserId: String
) : ViewModel() {

    init {
        repository.startGlobalRealtimeSync(viewModelScope, currentUserId)
    }

    val currentUser: StateFlow<UserProfile?> = repository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<UserProfile>> = repository.observeAllUsersList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communities: StateFlow<List<Community>> = repository.observeCommunities(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val driftingBottles: StateFlow<List<DriftingBottle>> = repository.observeDriftingBottles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.observeNotifications(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.observeUnreadNotificationsCount(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val customFrames: StateFlow<List<CustomFrame>> = repository.observeCustomFrames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val posts: StateFlow<List<CommunityPost>> = repository.observePosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _pickedBottle = MutableStateFlow<DriftingBottle?>(null)
    val pickedBottle: StateFlow<DriftingBottle?> = _pickedBottle.asStateFlow()

    private val _mePlusOutcome = MutableStateFlow<MePlusDrawOutcome?>(null)
    val mePlusOutcome: StateFlow<MePlusDrawOutcome?> = _mePlusOutcome.asStateFlow()

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun clearPickedBottle() {
        _pickedBottle.value = null
    }

    fun clearMePlusOutcome() {
        _mePlusOutcome.value = null
    }

    fun toggleJoinCommunity(communityId: String) {
        viewModelScope.launch {
            repository.toggleJoinCommunity(communityId, currentUserId)
        }
    }

    fun followHost(targetUserId: String) {
        viewModelScope.launch {
            repository.followHost(currentUserId, targetUserId)
                .onSuccess {
                    _statusBanner.value = "¡Emisor seguido! Revisa tu progreso en Tareas."
                }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun claimDailyCheckIn() {
        viewModelScope.launch {
            repository.claimDailyCheckIn(currentUserId)
                .onSuccess { out ->
                    val lvl = out.leveledUpTo?.let { " • ¡Subiste a Nv.${it.level} ${it.title} (+${out.bonusCoinsAwarded} monedas)!" }.orEmpty()
                    _statusBanner.value = "Día ${out.day} reclamado: ${out.reward.title}$lvl"
                }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun claimDailyTaskReward(taskId: String) {
        viewModelScope.launch {
            repository.claimDailyTaskReward(currentUserId, taskId)
                .onSuccess { out ->
                    val lvl = out.leveledUpTo?.let { " • ¡Subiste a Nv.${it.level} ${it.title} (+${out.bonusCoinsAwarded} monedas)!" }.orEmpty()
                    _statusBanner.value = "Premio reclamado (+${out.expGained} EXP Donador)$lvl"
                }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun playCoinMiniGame(betCoins: Int, rewardCoins: Int, message: String) {
        viewModelScope.launch {
            repository.playCoinMiniGame(currentUserId, betCoins, rewardCoins)
                .onSuccess { _statusBanner.value = message }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun activateMePlusAndDraw() {
        viewModelScope.launch {
            repository.activateMePlusAndDraw(currentUserId)
                .onSuccess { outcome ->
                    _mePlusOutcome.value = outcome
                    _statusBanner.value = "Me+ Sorteo: ¡Ganaste ${outcome.prizeTitle}!"
                }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun claimFreeCoins(amount: Int = 1000) {
        viewModelScope.launch {
            repository.claimFreeCoins(currentUserId, amount)
                .onSuccess { total -> _statusBanner.value = "+$amount monedas gratis recibidas (Saldo: $total)" }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun createCommunity(
        name: String,
        category: String,
        description: String,
        coverKey: String,
        backgroundUri: String? = null,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.createCommunity(currentUserId, name, category, description, coverKey, backgroundUri)
                .onSuccess { cid ->
                    _statusBanner.value = "Comunidad creada. Eres Admin de la sala."
                    onCreated(cid)
                }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun deleteCommunity(communityId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCommunity(communityId, currentUserId)
                .onSuccess {
                    _statusBanner.value = "Sala eliminada correctamente."
                    onDeleted()
                }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun saveImageFromUri(uri: Uri, folder: String, prefix: String, onSavedPath: (String) -> Unit) {
        viewModelScope.launch {
            repository.copyPickedImageToInternalStorage(uri, folder, prefix)
                .onSuccess { path -> onSavedPath(path) }
                .onFailure { _statusBanner.value = "No se pudo cargar la imagen seleccionada." }
        }
    }

    fun saveAvatarFromUri(uri: Uri, onSavedPath: (String) -> Unit) {
        saveImageFromUri(uri, "avatars", "avatar", onSavedPath)
    }

    fun saveProfileCustomization(
        username: String,
        bio: String,
        customStatus: String,
        frameId: String,
        avatarUri: String?,
        presetAvatarKey: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateProfileCustomization(
                userId = currentUserId,
                username = username,
                bio = bio,
                customStatus = customStatus,
                frameId = frameId,
                avatarUri = avatarUri,
                presetAvatarKey = presetAvatarKey
            ).onSuccess {
                _statusBanner.value = "Perfil actualizado con éxito"
                onComplete()
            }.onFailure {
                _statusBanner.value = it.message
            }
        }
    }

    fun publishPngFrame(
        title: String,
        uri: Uri,
        priceCoins: Int = 0,
        description: String = "",
        category: String = com.example.model.StoreFrameCategory.PROFILE_FRAME.label,
        isFeatured: Boolean = false,
        chargeStoreFee: Boolean = false,
        equipNow: Boolean = true,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.copyPickedImageToInternalStorage(uri, "frames", "frame")
                .onSuccess { path ->
                    repository.publishPngFrame(
                        creatorId = currentUserId,
                        title = title,
                        pngPath = path,
                        priceCoins = priceCoins,
                        description = description,
                        category = category,
                        isFeatured = isFeatured,
                        chargeStoreFee = chargeStoreFee
                    )
                        .onSuccess { created ->
                            if (equipNow) {
                                val u = currentUser.value
                                if (u != null) {
                                    repository.updateProfileCustomization(
                                        userId = u.id,
                                        username = u.username,
                                        bio = u.bio,
                                        customStatus = u.customStatus,
                                        frameId = created.id,
                                        avatarUri = u.avatarUri,
                                        presetAvatarKey = u.presetAvatarKey
                                    )
                                }
                            }
                            val priceTag = if (created.priceCoins > 0) " (💎${created.priceCoins})" else " (gratis)"
                            _statusBanner.value = "Diseño PNG \"${created.title}\" publicado$priceTag"
                            onDone()
                        }
                        .onFailure { _statusBanner.value = it.message }
                }
                .onFailure { _statusBanner.value = "No se pudo importar el archivo PNG." }
        }
    }

    fun publishStudioFrame(
        title: String,
        primaryColorHex: Long,
        secondaryColorHex: Long,
        stylePattern: CustomFrameStyle,
        thicknessDp: Float,
        hasSparkles: Boolean,
        priceCoins: Int = 0,
        description: String = "",
        category: String = com.example.model.StoreFrameCategory.PROFILE_FRAME.label,
        isFeatured: Boolean = false,
        chargeStoreFee: Boolean = false,
        equipNow: Boolean = true,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.publishStudioFrame(
                creatorId = currentUserId,
                title = title,
                primaryColorHex = primaryColorHex,
                secondaryColorHex = secondaryColorHex,
                stylePattern = stylePattern,
                thicknessDp = thicknessDp,
                hasSparkles = hasSparkles,
                priceCoins = priceCoins,
                description = description,
                category = category,
                isFeatured = isFeatured,
                chargeStoreFee = chargeStoreFee
            ).onSuccess { created ->
                if (equipNow) {
                    val u = currentUser.value
                    if (u != null) {
                        repository.updateProfileCustomization(
                            userId = u.id,
                            username = u.username,
                            bio = u.bio,
                            customStatus = u.customStatus,
                            frameId = created.id,
                            avatarUri = u.avatarUri,
                            presetAvatarKey = u.presetAvatarKey
                        )
                    }
                }
                val priceTag = if (created.priceCoins > 0) " (💎${created.priceCoins})" else " (gratis)"
                _statusBanner.value = "Diseño \"${created.title}\" publicado$priceTag"
                onDone()
            }.onFailure {
                _statusBanner.value = it.message
            }
        }
    }

    fun unlockNextFrameSlotTier() {
        viewModelScope.launch {
            repository.unlockNextFrameSlotTier(currentUserId)
                .onSuccess { tier ->
                    _statusBanner.value = "🎉 ¡Cupo ampliado permanentemente a ${tier.maxSlots} marcos en venta (${tier.title})!"
                }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun toggleFavoriteFrame(frameId: String) {
        viewModelScope.launch {
            repository.toggleFavoriteFrame(currentUserId, frameId)
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun equipFrame(frameId: String) {
        viewModelScope.launch {
            val u = currentUser.value ?: return@launch
            repository.updateProfileCustomization(
                userId = u.id,
                username = u.username,
                bio = u.bio,
                customStatus = u.customStatus,
                frameId = frameId,
                avatarUri = u.avatarUri,
                presetAvatarKey = u.presetAvatarKey
            ).onSuccess {
                _statusBanner.value = "Marco equipado en tu perfil"
            }
        }
    }

    fun buyCustomFrame(frameId: String) {
        viewModelScope.launch {
            repository.buyCustomFrame(currentUserId, frameId)
                .onSuccess { frame ->
                    _statusBanner.value = "¡Diseño \"${frame.title}\" adquirido y equipado!"
                }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun deleteCustomFrame(frameId: String) {
        viewModelScope.launch {
            repository.deleteCustomFrame(frameId, currentUserId)
                .onSuccess { _statusBanner.value = "Diseño retirado de la tienda. Quienes lo compraron lo conservan." }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun observeCommentsForPost(postId: String): Flow<List<PostComment>> =
        repository.observePostComments(postId)

    fun createPost(
        communityId: String?,
        content: String,
        imagePath: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.createPost(currentUserId, communityId, content, imagePath)
                .onSuccess {
                    _statusBanner.value = "Publicación compartida"
                    onDone()
                }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            repository.likePost(postId)
        }
    }

    fun commentOnPost(postId: String, content: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addPostComment(postId, currentUserId, content)
                .onSuccess { onDone() }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId, currentUserId)
                .onSuccess { _statusBanner.value = "Publicación eliminada." }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun launchBottle(moodTag: String, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.launchDriftingBottle(currentUserId, moodTag, content)
                .onSuccess {
                    _statusBanner.value = "Botella lanzada al océano neón (expira en 24h)"
                    onSuccess()
                }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun pickRandomBottle() {
        viewModelScope.launch {
            repository.pickRandomDriftingBottle(currentUserId)
                .onSuccess { _pickedBottle.value = it }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun replyToBottle(bottleId: String, reply: String) {
        viewModelScope.launch {
            repository.replyToBottle(bottleId, reply)
            _pickedBottle.value = _pickedBottle.value?.copy(replyContent = reply.trim())
            _statusBanner.value = "Respuesta anónima enviada a la botella"
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead(currentUserId)
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            repository.updateSettings(transform)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun assignAppRank(targetUserId: String, newRank: AppRank) {
        viewModelScope.launch {
            repository.assignAppRank(currentUserId, targetUserId, newRank)
                .onSuccess {
                    _statusBanner.value = if (newRank == AppRank.NONE) {
                        "Verificado retirado."
                    } else {
                        "Verificado actualizado: ${newRank.badgeTitle}"
                    }
                }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun banUser(targetUserId: String, reason: String) {
        viewModelScope.launch {
            repository.banUser(currentUserId, targetUserId, reason)
                .onSuccess { _statusBanner.value = "Usuario baneado permanentemente." }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun unbanUser(targetUserId: String) {
        viewModelScope.launch {
            repository.unbanUser(currentUserId, targetUserId)
                .onSuccess { _statusBanner.value = "Baneo retirado. El usuario puede volver a acceder." }
                .onFailure { _statusBanner.value = it.message }
        }
    }

    fun grantCoinsToUser(targetUserId: String, amount: Int) {
        viewModelScope.launch {
            repository.grantCoinsToUser(currentUserId, targetUserId, amount)
                .onSuccess { _statusBanner.value = "🎁 Regalo enviado: +$amount monedas." }
                .onFailure { _statusBanner.value = it.message }
        }
    }
}

class RoomViewModel(
    private val repository: TxlRepository,
    val communityId: String,
    val currentUserId: String
) : ViewModel() {

    private val audioEngine = VoiceAudioEngine(repository.appContext)
    private val voiceNoteRecorder = VoiceNoteRecorder(repository.appContext)
    private val voiceNotePlayer = VoiceNotePlayer(repository.appContext)
    private var pausedLiveMicForVoiceNote = false
    private var liveMicHasPermission = false
    private var roomSyncJob: Job? = null
    private var watchMinuteJob: Job? = null
    private var giftBannerJob: Job? = null
    private var voiceFramesListener: ListenerRegistration? = null
    private var lastShownGiftId: String? = null

    val micAmplitude: StateFlow<Float> = audioEngine.amplitudeLevel
    val remoteSpeakingLevels: StateFlow<Map<String, Float>> = audioEngine.remoteSpeakingLevels
    val isRecordingVoiceNote: StateFlow<Boolean> = voiceNoteRecorder.isRecording
    val recordingElapsedMs: StateFlow<Long> = voiceNoteRecorder.elapsedMs
    val playingAudioMessageId: StateFlow<Long?> = voiceNotePlayer.playingMessageId
    val playingAudioProgress: StateFlow<Float> = voiceNotePlayer.progress

    val currentUser: StateFlow<UserProfile?> = repository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<UserProfile>> = repository.observeAllUsersList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val community: StateFlow<Community?> = repository.observeCommunity(communityId, currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val voiceParticipants: StateFlow<List<VoiceParticipant>> = repository.observeVoiceParticipants(communityId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roomEvents: StateFlow<List<RoomEventLog>> = repository.observeRoomEvents(communityId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voiceChatMessages: StateFlow<List<ChatMessage>> = repository.observeMessages(communityId, "VOICE")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupChatMessages: StateFlow<List<ChatMessage>> = repository.observeMessages(communityId, "GROUP")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roomGifts: StateFlow<List<RoomGiftEvent>> = repository.observeRoomGifts(communityId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _roomNotice = MutableStateFlow<String?>(null)
    val roomNotice: StateFlow<String?> = _roomNotice.asStateFlow()

    private val _activeGiftBanner = MutableStateFlow<RoomGiftEvent?>(null)
    val activeGiftBanner: StateFlow<RoomGiftEvent?> = _activeGiftBanner.asStateFlow()

    init {
        viewModelScope.launch {
            roomGifts.collect { list ->
                val newest = list.firstOrNull() ?: return@collect
                if (newest.id != lastShownGiftId &&
                    System.currentTimeMillis() - newest.timestampMillis < 12_000L
                ) {
                    lastShownGiftId = newest.id
                    triggerGiftBanner(newest)
                }
            }
        }
    }

    private fun triggerGiftBanner(event: RoomGiftEvent) {
        _activeGiftBanner.value = event
        giftBannerJob?.cancel()
        giftBannerJob = viewModelScope.launch {
            delay(3600L)
            if (_activeGiftBanner.value?.id == event.id) {
                _activeGiftBanner.value = null
            }
        }
    }

    fun clearNotice() {
        _roomNotice.value = null
    }

    fun enterVoiceRoom() {
        if (roomSyncJob == null) {
            roomSyncJob = repository.startRoomRealtimeSync(viewModelScope, communityId)
        }
        if (voiceFramesListener == null) {
            voiceFramesListener = repository.observeVoiceFrames(communityId, currentUserId) { senderId, seq, payloadB64, rms ->
                audioEngine.enqueueRemoteFrame(senderId, seq, payloadB64, rms)
            }
        }
        audioEngine.startPlaybackMixer(viewModelScope)
        if (watchMinuteJob?.isActive != true) {
            watchMinuteJob = viewModelScope.launch {
                while (isActive) {
                    delay(60_000L)
                    repository.recordLiveWatchMinute(currentUserId)
                }
            }
        }
        viewModelScope.launch {
            repository.joinVoiceRoom(communityId, currentUserId)
        }
    }

    fun ensureGroupSync() {
        if (roomSyncJob == null) {
            roomSyncJob = repository.startRoomRealtimeSync(viewModelScope, communityId)
        }
    }

    fun leaveVoiceRoom() {
        pausedLiveMicForVoiceNote = false
        voiceNoteRecorder.release()
        voiceNotePlayer.stop()
        audioEngine.stopCapture()
        audioEngine.stopPlaybackMixer()
        voiceFramesListener?.remove()
        voiceFramesListener = null
        roomSyncJob?.cancel()
        roomSyncJob = null
        watchMinuteJob?.cancel()
        watchMinuteJob = null
        viewModelScope.launch {
            repository.leaveVoiceRoom(communityId, currentUserId)
        }
    }

    fun toggleSelfMute(currentlyMuted: Boolean, hasAudioPermission: Boolean) {
        liveMicHasPermission = hasAudioPermission
        val nextMuted = !currentlyMuted
        viewModelScope.launch {
            repository.setSelfMuted(communityId, currentUserId, nextMuted)
            repository.setSelfSpeaking(communityId, currentUserId, !nextMuted)
        }
        if (nextMuted) {
            pausedLiveMicForVoiceNote = false
            audioEngine.stopCapture()
        } else if (!isRecordingVoiceNote.value) {
            audioEngine.startCapture(viewModelScope, hasAudioPermission) { seq, payloadB64, rms ->
                repository.pushVoiceFrameAsync(communityId, currentUserId, seq, payloadB64, rms)
            }
        }
    }

    fun toggleHandRaised() {
        viewModelScope.launch {
            repository.toggleHandRaised(communityId, currentUserId)
        }
    }

    fun sendRoomGift(
        channelType: String,
        recipientId: String,
        giftId: String,
        comboCount: Int = 1,
        useBagItemId: String? = null
    ) {
        viewModelScope.launch {
            repository.sendRoomGift(
                communityId = communityId,
                channelType = channelType,
                senderId = currentUserId,
                recipientId = recipientId,
                giftId = giftId,
                comboCount = comboCount,
                useBagItemId = useBagItemId
            ).onSuccess { outcome ->
                lastShownGiftId = outcome.event.id
                triggerGiftBanner(outcome.event)
                outcome.leveledUpTo?.let { tier ->
                    _roomNotice.value = "¡Subiste a Nivel ${tier.level} ${tier.title}! +${outcome.bonusCoinsAwarded} monedas de bono"
                }
            }.onFailure {
                _roomNotice.value = it.message
            }
        }
    }

    fun claimFreeCoins(amount: Int = 1000) {
        viewModelScope.launch {
            repository.claimFreeCoins(currentUserId, amount)
                .onSuccess { total -> _roomNotice.value = "+$amount monedas añadidas (Saldo: $total)" }
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun followHost(targetUserId: String) {
        viewModelScope.launch {
            repository.followHost(currentUserId, targetUserId)
                .onSuccess { _roomNotice.value = "¡Siguiendo al anfitrión! Progreso de misión actualizado." }
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun sendVoiceMessage(content: String) {
        viewModelScope.launch {
            repository.sendMessage(communityId, "VOICE", currentUserId, content)
        }
    }

    fun sendGroupMessage(content: String, replyToSnippet: String? = null) {
        viewModelScope.launch {
            repository.sendMessage(communityId, "GROUP", currentUserId, content, replyToSnippet)
        }
    }

    fun startVoiceNoteRecording(hasPermission: Boolean) {
        if (!hasPermission) {
            _roomNotice.value = "Concede el permiso de micrófono para enviar mensajes de audio."
            return
        }
        voiceNotePlayer.stop()
        if (audioEngine.isMicActive.value) {
            pausedLiveMicForVoiceNote = true
            liveMicHasPermission = hasPermission
            audioEngine.stopCapture()
        } else {
            pausedLiveMicForVoiceNote = false
        }
        voiceNoteRecorder.startRecording(viewModelScope).onFailure { err ->
            restoreLiveMicIfNeeded()
            _roomNotice.value = err.message ?: "No se pudo grabar la nota de voz."
        }
    }

    fun stopAndSendVoiceNote(channelType: String, replyToSnippet: String? = null) {
        val recorded = voiceNoteRecorder.stopAndFinalize()
        restoreLiveMicIfNeeded()
        recorded
            .onSuccess { audio ->
                viewModelScope.launch {
                    repository.sendAudioMessage(
                        communityId = communityId,
                        channelType = channelType,
                        senderId = currentUserId,
                        audioUri = audio.filePath,
                        durationMs = audio.durationMs,
                        audioDataB64 = audio.audioDataB64,
                        replyToSnippet = replyToSnippet
                    ).onFailure { _roomNotice.value = it.message }
                }
            }
            .onFailure {
                _roomNotice.value = it.message
            }
    }

    fun cancelVoiceNoteRecording() {
        voiceNoteRecorder.cancelRecording()
        restoreLiveMicIfNeeded()
    }

    fun togglePlayAudioMessage(message: ChatMessage) {
        voiceNotePlayer.togglePlayback(
            messageId = message.id,
            audioPath = message.audioUri,
            audioDataB64 = message.audioDataB64,
            scope = viewModelScope,
            onError = { _roomNotice.value = it }
        )
    }

    private fun restoreLiveMicIfNeeded() {
        if (pausedLiveMicForVoiceNote) {
            pausedLiveMicForVoiceNote = false
            audioEngine.startCapture(viewModelScope, liveMicHasPermission) { seq, payloadB64, rms ->
                repository.pushVoiceFrameAsync(communityId, currentUserId, seq, payloadB64, rms)
            }
        }
    }

    fun reactToMessage(messageId: Long) {
        viewModelScope.launch {
            repository.reactToMessage(messageId)
        }
    }

    fun deleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            repository.deleteMessage(communityId, currentUserId, message)
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun moderateMute(targetUserId: String, mute: Boolean) {
        viewModelScope.launch {
            repository.moderateMuteParticipant(communityId, currentUserId, targetUserId, mute)
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun kickParticipant(targetUserId: String) {
        viewModelScope.launch {
            repository.kickParticipant(communityId, currentUserId, targetUserId)
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun setRole(targetUserId: String, newRole: CommunityRole) {
        viewModelScope.launch {
            repository.changeParticipantRole(communityId, currentUserId, targetUserId, newRole)
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun toggleRoomLock(lock: Boolean) {
        viewModelScope.launch {
            repository.toggleRoomLock(communityId, currentUserId, lock)
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun clearEvents() {
        viewModelScope.launch {
            repository.clearRoomEvents(communityId, currentUserId)
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun assignAppRank(targetUserId: String, newRank: AppRank) {
        viewModelScope.launch {
            repository.assignAppRank(currentUserId, targetUserId, newRank)
                .onSuccess {
                    _roomNotice.value = if (newRank == AppRank.NONE) {
                        "Verificado retirado."
                    } else {
                        "Entregado: ${newRank.badgeTitle}"
                    }
                }
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun banUser(targetUserId: String, reason: String) {
        viewModelScope.launch {
            repository.banUser(currentUserId, targetUserId, reason)
                .onSuccess { _roomNotice.value = "Usuario baneado permanentemente." }
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun unbanUser(targetUserId: String) {
        viewModelScope.launch {
            repository.unbanUser(currentUserId, targetUserId)
                .onSuccess { _roomNotice.value = "Baneo retirado." }
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun grantCoinsToUser(targetUserId: String, amount: Int) {
        viewModelScope.launch {
            repository.grantCoinsToUser(currentUserId, targetUserId, amount)
                .onSuccess { _roomNotice.value = "🎁 Regalo enviado: +$amount monedas." }
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun updateRoomBackgroundFromUri(uri: Uri) {
        viewModelScope.launch {
            repository.copyPickedImageToInternalStorage(uri, "backgrounds", "room_bg")
                .onSuccess { path ->
                    repository.updateCommunityBackground(communityId, currentUserId, path)
                        .onSuccess { _roomNotice.value = "Fondo de la sala actualizado" }
                        .onFailure { _roomNotice.value = it.message }
                }
                .onFailure { _roomNotice.value = "No se pudo cargar la foto de fondo." }
        }
    }

    fun resetRoomBackground() {
        viewModelScope.launch {
            repository.updateCommunityBackground(communityId, currentUserId, null)
                .onSuccess { _roomNotice.value = "Fondo predeterminado restaurado" }
                .onFailure { _roomNotice.value = it.message }
        }
    }

    fun deleteCommunity(onDeleted: () -> Unit) {
        pausedLiveMicForVoiceNote = false
        voiceNoteRecorder.release()
        voiceNotePlayer.stop()
        audioEngine.stopCapture()
        audioEngine.stopPlaybackMixer()
        voiceFramesListener?.remove()
        voiceFramesListener = null
        roomSyncJob?.cancel()
        roomSyncJob = null
        watchMinuteJob?.cancel()
        watchMinuteJob = null
        viewModelScope.launch {
            repository.deleteCommunity(communityId, currentUserId)
                .onSuccess { onDeleted() }
                .onFailure { _roomNotice.value = it.message }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pausedLiveMicForVoiceNote = false
        voiceNoteRecorder.release()
        voiceNotePlayer.stop()
        audioEngine.stopCapture()
        audioEngine.stopPlaybackMixer()
        voiceFramesListener?.remove()
        voiceFramesListener = null
        roomSyncJob?.cancel()
        roomSyncJob = null
        watchMinuteJob?.cancel()
        watchMinuteJob = null
    }
}

class TxlViewModelFactory(
    private val repository: TxlRepository,
    private val currentUserId: String = "",
    private val communityId: String = ""
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(repository) as T
        modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(repository, currentUserId) as T
        modelClass.isAssignableFrom(RoomViewModel::class.java) -> RoomViewModel(repository, communityId, currentUserId) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
