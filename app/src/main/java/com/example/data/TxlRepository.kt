package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.local.ChatMessageEntity
import com.example.data.local.CommunityEntity
import com.example.data.local.CustomFrameEntity
import com.example.data.local.DriftingBottleEntity
import com.example.data.local.MembershipEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.PostCommentEntity
import com.example.data.local.PostEntity
import com.example.data.local.PreferencesStore
import com.example.data.local.RoomEventEntity
import com.example.data.local.SeedData
import com.example.data.local.TxlDatabase
import com.example.data.local.UserEntity
import com.example.data.local.VoiceParticipantEntity
import com.example.data.remote.FirestoreSyncRepository
import com.example.model.AppNotification
import com.example.model.AppRank
import com.example.model.AppSettings
import com.example.model.AvatarFrame
import com.example.model.BagCatalog
import com.example.model.ChatMessage
import com.example.model.Community
import com.example.model.CommunityPost
import com.example.model.CommunityRole
import com.example.model.CreatorRank
import com.example.model.CustomFrame
import com.example.model.CustomFrameStyle
import com.example.model.DailyCheckInCatalog
import com.example.model.DailyCheckInReward
import com.example.model.DailyTaskCatalog
import com.example.model.DonorTier
import com.example.model.DriftingBottle
import com.example.model.FrameSlotTier
import com.example.model.GiftCatalog
import com.example.model.NotificationCategory
import com.example.model.PostComment
import com.example.model.RoomEventLog
import com.example.model.RoomGiftEvent
import com.example.model.StoreFrameCategory
import com.example.model.UserProfile
import com.example.model.VoiceParticipant
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class GiftSendOutcome(
    val event: RoomGiftEvent,
    val expGained: Int,
    val leveledUpTo: DonorTier?,
    val bonusCoinsAwarded: Int
)

data class CheckInOutcome(
    val day: Int,
    val reward: DailyCheckInReward,
    val leveledUpTo: DonorTier?,
    val bonusCoinsAwarded: Int
)

data class TaskClaimOutcome(
    val taskTitle: String,
    val expGained: Int,
    val leveledUpTo: DonorTier?,
    val bonusCoinsAwarded: Int
)

data class MePlusDrawOutcome(
    val prizeTitle: String,
    val coinsWon: Int,
    val expWon: Int,
    val bagItemWonName: String?,
    val leveledUpTo: DonorTier?
)

class TxlRepository(
    private val context: Context,
    private val db: TxlDatabase,
    private val prefs: PreferencesStore,
    private val remoteSync: FirestoreSyncRepository? = null
) {
    companion object {
        const val PRIMARY_OWNER_EMAIL = "abukeje26@gmail.com"
        private const val CHECK_IN_COOLDOWN_MS = 20L * 3_600_000L
        private const val CHECK_IN_STREAK_RESET_MS = 48L * 3_600_000L
        private const val ME_PLUS_DRAW_COOLDOWN_MS = 20L * 3_600_000L
    }

    val appContext: Context get() = context
    val currentUserId: Flow<String?> = prefs.currentUserId
    val settings: Flow<AppSettings> = prefs.settings

    private val localGiftsByCommunity = MutableStateFlow<Map<String, List<RoomGiftEvent>>>(emptyMap())
    private var globalSyncJob: Job? = null

    suspend fun initializeSeedData() {
        SeedData.ensureSeeded(db)
        db.driftingBottleDao().purgeExpired(System.currentTimeMillis())
        db.userDao().findByEmailOrUsername(PRIMARY_OWNER_EMAIL)?.let { owner ->
            if (owner.emailVerified && (!owner.isDeveloper || owner.appRankId == AppRank.NONE.id)) {
                db.userDao().updateIsDeveloper(owner.id, true)
            }
        }
    }

    // --- GLOBAL REALTIME FIRESTORE SYNC ---

    suspend fun restoreVerifiedRemoteSession(): UserProfile? {
        val sync = remoteSync ?: return null
        val fbUser = sync.currentAuthUser() ?: return null
        val reloaded = runCatching {
            fbUser.reload()
            sync.currentAuthUser()
        }.getOrNull() ?: fbUser

        if (!reloaded.isEmailVerified || reloaded.email.isNullOrBlank()) {
            sync.signOut()
            prefs.setCurrentUserId(null)
            return null
        }

        val priv = sync.ensureRemoteUserDocuments(reloaded).getOrNull() ?: return null
        val isOwner = isEmailVerifiedOwner(priv.email, priv.emailVerified)
        val localExisting = db.userDao().getUserById(priv.userId)
        val entity = UserEntity(
            id = priv.userId,
            username = priv.username.ifBlank { reloaded.displayName ?: "Usuario" },
            email = priv.email,
            emailVerified = priv.emailVerified,
            passwordHash = localExisting?.passwordHash ?: "",
            bio = priv.bio,
            customStatus = priv.customStatus,
            avatarFrameId = priv.avatarFrameId,
            avatarUri = localExisting?.avatarUri,
            presetAvatarKey = priv.presetAvatarKey,
            communitiesCount = priv.communitiesCount,
            followersCount = priv.followersCount,
            postsCount = priv.postsCount,
            coinsCount = priv.coinsCount,
            giftsReceivedCount = priv.giftsReceivedCount,
            donorExp = priv.donorExp,
            donorLevel = priv.donorLevel,
            totalCoinsDonated = priv.totalCoinsDonated,
            dailyStreakDay = priv.dailyStreakDay,
            lastDailyCheckInAtMillis = priv.lastDailyCheckInAtMillis,
            followHostsProgress = priv.followHostsProgress,
            watchLiveMinutesProgress = priv.watchLiveMinutesProgress,
            gameCoinsEarnedProgress = priv.gameCoinsEarnedProgress,
            giftCoinsSentProgress = priv.giftCoinsSentProgress,
            claimedTaskIds = priv.claimedTaskIds,
            inventoryJson = priv.inventoryJson,
            mePlusMember = priv.mePlusMember,
            lastMePlusDrawAtMillis = priv.lastMePlusDrawAtMillis,
            maxFrameSlots = if (isOwner) 120 else priv.maxFrameSlots.coerceIn(10, 120),
            creatorSalesCount = priv.creatorSalesCount,
            creatorEarningsCoins = priv.creatorEarningsCoins,
            favoriteFrameIdsCsv = priv.favoriteFrameIds,
            appRankId = if (!isOwner && priv.appRankId == AppRank.OWNER.id) AppRank.NONE.id else priv.appRankId,
            isDeveloper = isOwner,
            isBanned = priv.banned,
            banReason = priv.banReason
        )
        db.userDao().upsertUser(entity)
        prefs.setCurrentUserId(entity.id)
        return entity.toDomain()
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<UserProfile> {
        val sync = remoteSync
            ?: return Result.failure(IllegalStateException("Sincronización en la nube no disponible."))
        return sync.signInWithGoogleIdToken(idToken).mapCatching { fbUser ->
            val priv = sync.ensureRemoteUserDocuments(fbUser).getOrThrow()
            if (priv.banned) {
                val reason = priv.banReason?.takeIf { it.isNotBlank() } ?: "actividad ilegal o incumplimiento grave"
                sync.signOut()
                throw IllegalStateException("Tu cuenta está baneada permanentemente: $reason")
            }
            val isOwner = isEmailVerifiedOwner(priv.email, priv.emailVerified)
            val existing = db.userDao().getUserById(priv.userId)
            val entity = UserEntity(
                id = priv.userId,
                username = priv.username,
                email = priv.email,
                emailVerified = priv.emailVerified,
                passwordHash = existing?.passwordHash ?: "",
                bio = priv.bio,
                customStatus = priv.customStatus,
                avatarFrameId = priv.avatarFrameId,
                avatarUri = existing?.avatarUri,
                presetAvatarKey = priv.presetAvatarKey,
                communitiesCount = priv.communitiesCount,
                followersCount = priv.followersCount,
                postsCount = priv.postsCount,
                coinsCount = priv.coinsCount,
                giftsReceivedCount = priv.giftsReceivedCount,
                donorExp = priv.donorExp,
                donorLevel = priv.donorLevel,
                totalCoinsDonated = priv.totalCoinsDonated,
                dailyStreakDay = priv.dailyStreakDay,
                lastDailyCheckInAtMillis = priv.lastDailyCheckInAtMillis,
                followHostsProgress = priv.followHostsProgress,
                watchLiveMinutesProgress = priv.watchLiveMinutesProgress,
                gameCoinsEarnedProgress = priv.gameCoinsEarnedProgress,
                giftCoinsSentProgress = priv.giftCoinsSentProgress,
                claimedTaskIds = priv.claimedTaskIds,
                inventoryJson = priv.inventoryJson,
                mePlusMember = priv.mePlusMember,
                lastMePlusDrawAtMillis = priv.lastMePlusDrawAtMillis,
                maxFrameSlots = if (isOwner) 120 else priv.maxFrameSlots.coerceIn(10, 120),
                creatorSalesCount = priv.creatorSalesCount,
                creatorEarningsCoins = priv.creatorEarningsCoins,
                favoriteFrameIdsCsv = priv.favoriteFrameIds,
                appRankId = if (!isOwner && priv.appRankId == AppRank.OWNER.id) AppRank.NONE.id else priv.appRankId,
                isDeveloper = isOwner,
                isBanned = priv.banned,
                banReason = priv.banReason
            )
            db.userDao().upsertUser(entity)
            prefs.setCurrentUserId(entity.id)
            entity.toDomain()
        }
    }

    fun startGlobalRealtimeSync(scope: CoroutineScope, uid: String) {
        val sync = remoteSync ?: return
        globalSyncJob?.cancel()
        globalSyncJob = scope.launch {
            launch {
                sync.observePrivateUser(uid).collect { priv ->
                    if (priv == null) return@collect
                    val existing = db.userDao().getUserById(uid)
                    val isOwner = isEmailVerifiedOwner(priv.email, priv.emailVerified)
                    val merged = (existing ?: UserEntity(
                        id = uid,
                        username = priv.username,
                        email = priv.email,
                        emailVerified = priv.emailVerified,
                        passwordHash = "",
                        bio = priv.bio,
                        customStatus = priv.customStatus,
                        avatarFrameId = priv.avatarFrameId,
                        avatarUri = null,
                        presetAvatarKey = priv.presetAvatarKey,
                        communitiesCount = priv.communitiesCount,
                        followersCount = priv.followersCount,
                        postsCount = priv.postsCount
                    )).copy(
                        username = priv.username.ifBlank { existing?.username ?: "Usuario" },
                        email = priv.email.ifBlank { existing?.email.orEmpty() },
                        emailVerified = priv.emailVerified,
                        bio = priv.bio,
                        customStatus = priv.customStatus,
                        avatarFrameId = priv.avatarFrameId,
                        presetAvatarKey = priv.presetAvatarKey,
                        communitiesCount = priv.communitiesCount,
                        followersCount = priv.followersCount,
                        postsCount = priv.postsCount,
                        coinsCount = priv.coinsCount,
                        giftsReceivedCount = priv.giftsReceivedCount,
                        donorExp = priv.donorExp,
                        donorLevel = priv.donorLevel,
                        totalCoinsDonated = priv.totalCoinsDonated,
                        dailyStreakDay = priv.dailyStreakDay,
                        lastDailyCheckInAtMillis = priv.lastDailyCheckInAtMillis,
                        followHostsProgress = priv.followHostsProgress,
                        watchLiveMinutesProgress = priv.watchLiveMinutesProgress,
                        gameCoinsEarnedProgress = priv.gameCoinsEarnedProgress,
                        giftCoinsSentProgress = priv.giftCoinsSentProgress,
                        claimedTaskIds = priv.claimedTaskIds,
                        inventoryJson = priv.inventoryJson,
                        mePlusMember = priv.mePlusMember,
                        lastMePlusDrawAtMillis = priv.lastMePlusDrawAtMillis,
                        maxFrameSlots = if (isOwner) 120 else priv.maxFrameSlots.coerceIn(10, 120),
                        creatorSalesCount = priv.creatorSalesCount,
                        creatorEarningsCoins = priv.creatorEarningsCoins,
                        favoriteFrameIdsCsv = priv.favoriteFrameIds,
                        appRankId = if (!isOwner && priv.appRankId == AppRank.OWNER.id) AppRank.NONE.id else priv.appRankId,
                        isDeveloper = isOwner,
                        isBanned = priv.banned,
                        banReason = priv.banReason
                    )
                    db.userDao().upsertUser(merged)
                }
            }

            launch {
                sync.observeDiscoverableUsers().collect { publicUsers ->
                    val entities = publicUsers.mapNotNull { pub ->
                        val targetId = pub.userId.ifBlank { pub.id }
                        if (targetId.isBlank()) return@mapNotNull null
                        val existing = db.userDao().getUserById(targetId)
                        if (targetId == uid && existing != null) {
                            // Keep private fields intact on current user, update public counters
                            existing.copy(
                                followersCount = pub.followersCount,
                                giftsReceivedCount = pub.giftsReceivedCount,
                                appRankId = if (!existing.isDeveloper && pub.appRankId == AppRank.OWNER.id) AppRank.NONE.id else pub.appRankId,
                                isBanned = pub.banned,
                                banReason = pub.banReason
                            )
                        } else {
                            UserEntity(
                                id = targetId,
                                username = pub.username.ifBlank { "Usuario" },
                                email = existing?.email.orEmpty(),
                                emailVerified = existing?.emailVerified ?: false,
                                passwordHash = "",
                                bio = pub.bio,
                                customStatus = pub.customStatus,
                                avatarFrameId = pub.avatarFrameId,
                                avatarUri = existing?.avatarUri,
                                presetAvatarKey = pub.presetAvatarKey,
                                communitiesCount = pub.communitiesCount,
                                followersCount = pub.followersCount,
                                postsCount = pub.postsCount,
                                coinsCount = existing?.coinsCount ?: (if (pub.appRankId == AppRank.OWNER.id) 999_999_999 else 100),
                                giftsReceivedCount = pub.giftsReceivedCount,
                                donorExp = pub.donorExp,
                                donorLevel = pub.donorLevel,
                                appRankId = if (pub.appRankId == AppRank.OWNER.id) AppRank.OWNER.id else pub.appRankId,
                                isDeveloper = existing?.isDeveloper ?: false,
                                isBanned = pub.banned,
                                banReason = pub.banReason
                            )
                        }
                    }
                    if (entities.isNotEmpty()) {
                        db.userDao().upsertUsers(entities)
                    }
                }
            }

            launch {
                sync.observePublicCommunities().collect { remoteComms ->
                    val entities = remoteComms.map { c ->
                        val local = db.communityDao().getCommunityById(c.id)
                        CommunityEntity(
                            id = c.id,
                            name = c.name,
                            category = c.category,
                            description = c.description,
                            coverKey = c.coverKey,
                            badgeLabel = c.badgeLabel,
                            memberCount = c.memberCount,
                            activeVoiceCount = c.activeVoiceCount,
                            isVoiceLocked = c.voiceLocked,
                            backgroundUri = local?.backgroundUri ?: c.backgroundUri
                        )
                    }
                    if (entities.isNotEmpty()) {
                        db.communityDao().upsertCommunities(entities)
                        db.communityDao().pruneMissing(entities.map { it.id })
                    }
                }
            }

            launch {
                sync.observePublicPosts().collect { remotePosts ->
                    val entities = remotePosts.map { p ->
                        PostEntity(
                            id = p.id,
                            authorId = p.authorId,
                            communityId = p.communityId,
                            content = p.content,
                            imageUri = p.imageUri,
                            likesCount = p.likesCount,
                            createdAtMillis = p.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                    if (entities.isNotEmpty()) {
                        db.postDao().insertPosts(entities)
                    }
                }
            }

            launch {
                sync.observePublicCustomFrames().collect { frames ->
                    val entities = frames.map { f ->
                        CustomFrameEntity(
                            id = f.id,
                            creatorId = f.creatorId,
                            creatorName = f.creatorName,
                            title = f.title,
                            frameType = f.frameType,
                            pngPath = f.pngPath,
                            primaryColorHex = f.primaryColorHex,
                            secondaryColorHex = f.secondaryColorHex,
                            stylePatternId = f.stylePatternId,
                            thicknessDp = f.thicknessDp.toFloat(),
                            hasSparkles = f.sparkles,
                            priceCoins = f.priceCoins.coerceAtLeast(0),
                            description = f.description,
                            category = f.category.ifBlank { StoreFrameCategory.PROFILE_FRAME.label },
                            isFeatured = f.isFeatured,
                            salesCount = f.salesCount.coerceAtLeast(0),
                            favoriteCount = f.favoriteCount.coerceAtLeast(0),
                            createdAtMillis = f.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                    if (entities.isNotEmpty()) {
                        db.customFrameDao().insertFrames(entities)
                    }
                }
            }

            launch {
                sync.observePublicBottles().collect { bottles ->
                    val entities = bottles.map { b ->
                        DriftingBottleEntity(
                            id = b.id,
                            authorId = b.authorId,
                            anonymousAlias = b.anonymousAlias,
                            moodTag = b.moodTag,
                            content = b.content,
                            replyContent = b.replyContent,
                            isPickedByMe = false,
                            createdAtMillis = b.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                            expiresAtMillis = b.expiresAtMillis
                        )
                    }
                    if (entities.isNotEmpty()) {
                        db.driftingBottleDao().insertBottles(entities)
                    }
                }
            }

            launch {
                sync.observeUserNotifications(uid).collect { notifs ->
                    for (n in notifs) {
                        val existing = db.notificationDao().getByRemoteId(n.id)
                        if (existing == null) {
                            db.notificationDao().insertNotification(
                                NotificationEntity(
                                    userId = uid,
                                    actorName = n.actorName,
                                    actorAvatarKey = n.actorAvatarKey,
                                    actorAvatarUri = null,
                                    actorFrameId = n.actorFrameId,
                                    title = n.title,
                                    subtitle = n.subtitle,
                                    category = n.category,
                                    targetCommunityId = n.targetCommunityId,
                                    isRead = n.read,
                                    timestampMillis = n.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                                    remoteId = n.id
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun startRoomRealtimeSync(scope: CoroutineScope, communityId: String): Job? {
        val sync = remoteSync ?: return null
        return scope.launch {
            launch {
                sync.observeCommunityMembers(communityId).collect { members ->
                    val entities = members.map { m ->
                        MembershipEntity(
                            communityId = communityId,
                            userId = m.userId,
                            role = m.role,
                            joinedAtMillis = m.joinedAt?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                    if (entities.isNotEmpty()) {
                        db.membershipDao().upsertMemberships(entities)
                    }
                }
            }

            launch {
                sync.observeVoiceParticipants(communityId).collect { remoteVps ->
                    for (vp in remoteVps) {
                        if (db.userDao().getUserById(vp.userId) == null) {
                            db.userDao().upsertUser(
                                UserEntity(
                                    id = vp.userId,
                                    username = vp.username.ifBlank { "Miembro" },
                                    email = "",
                                    emailVerified = true,
                                    passwordHash = "",
                                    bio = "",
                                    customStatus = "",
                                    avatarFrameId = vp.avatarFrameId,
                                    avatarUri = null,
                                    presetAvatarKey = vp.presetAvatarKey,
                                    communitiesCount = 0,
                                    followersCount = 0,
                                    postsCount = 0,
                                    appRankId = if (vp.appRankId == AppRank.OWNER.id) AppRank.OWNER.id else vp.appRankId
                                )
                            )
                        }
                        db.voiceRoomDao().upsertParticipant(
                            VoiceParticipantEntity(
                                id = "${communityId}_${vp.userId}",
                                communityId = communityId,
                                userId = vp.userId,
                                isMuted = vp.muted,
                                isSpeaking = vp.speaking,
                                isHandRaised = vp.handRaised
                            )
                        )
                    }
                    syncActiveVoiceCount(communityId)
                }
            }

            launch {
                sync.observeMessages(communityId).collect { remoteMsgs ->
                    for (rm in remoteMsgs) {
                        if (db.userDao().getUserById(rm.senderId) == null) {
                            db.userDao().upsertUser(
                                UserEntity(
                                    id = rm.senderId,
                                    username = rm.senderName.ifBlank { "Usuario" },
                                    email = "",
                                    emailVerified = true,
                                    passwordHash = "",
                                    bio = "",
                                    customStatus = rm.senderStatus.orEmpty(),
                                    avatarFrameId = rm.senderFrameId,
                                    avatarUri = null,
                                    presetAvatarKey = rm.senderAvatarKey,
                                    communitiesCount = 0,
                                    followersCount = 0,
                                    postsCount = 0,
                                    appRankId = rm.senderAppRankId
                                )
                            )
                        }
                        val existing = db.chatDao().getMessageByRemoteId(rm.id)
                        if (existing == null) {
                            db.chatDao().insertMessage(
                                ChatMessageEntity(
                                    communityId = rm.communityId,
                                    channelType = rm.channelType,
                                    senderId = rm.senderId,
                                    content = rm.content,
                                    replyToSnippet = rm.replyToSnippet,
                                    reactionCount = rm.reactionCount,
                                    timestampMillis = rm.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                                    audioUri = rm.audioUri,
                                    audioDataB64 = rm.audioDataB64,
                                    audioDurationMs = rm.audioDurationMs,
                                    remoteId = rm.id
                                )
                            )
                        } else if (existing.reactionCount != rm.reactionCount) {
                            db.chatDao().updateReactionByRemoteId(rm.id, rm.reactionCount)
                        }
                    }
                }
            }

            launch {
                sync.observeRoomEvents(communityId).collect { remoteEvents ->
                    val existingLocal = db.voiceRoomDao().observeRoomEvents(communityId).first()
                    val existingKeys = existingLocal.map { "${it.actorName}_${it.description}" }.toSet()
                    for (ev in remoteEvents) {
                        val key = "${ev.actorName}_${ev.description}"
                        if (key !in existingKeys) {
                            db.voiceRoomDao().insertEvent(
                                RoomEventEntity(
                                    communityId = ev.communityId,
                                    actorName = ev.actorName,
                                    description = ev.description,
                                    timestampMillis = ev.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun observeRoomGifts(communityId: String): Flow<List<RoomGiftEvent>> {
        val localFlow = localGiftsByCommunity.map { it[communityId].orEmpty() }
        val remoteFlow = remoteSync?.observeRoomGifts(communityId) ?: flowOf(emptyList())
        return combine(localFlow, remoteFlow) { local, remote ->
            (remote + local)
                .distinctBy { it.id }
                .sortedByDescending { it.timestampMillis }
        }
    }

    fun observeVoiceFrames(
        communityId: String,
        myUserId: String,
        onFrame: (senderId: String, seq: Long, payloadB64: String, rms: Float) -> Unit
    ): ListenerRegistration? =
        remoteSync?.observeVoiceFrames(communityId, myUserId, onFrame)

    fun pushVoiceFrameAsync(
        communityId: String,
        senderId: String,
        seq: Long,
        payloadB64: String,
        rms: Float
    ) {
        remoteSync?.pushVoiceFrameAsync(communityId, senderId, seq, payloadB64, rms)
    }

    // --- ROOM STREAMS ---

    fun observeAllUsersMap(): Flow<Map<String, UserProfile>> =
        db.userDao().observeAllUsers().map { list ->
            list.associate { it.id to it.toDomain() }
        }

    fun observeAllUsersList(): Flow<List<UserProfile>> =
        db.userDao().observeAllUsers().map { list ->
            list.map { it.toDomain() }
        }

    fun observeHasAnyUser(): Flow<Boolean> =
        db.userDao().observeAllUsers().map { it.isNotEmpty() }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCurrentUser(): Flow<UserProfile?> =
        currentUserId.flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(null)
            else db.userDao().observeUserById(uid).map { it?.toDomain() }
        }

    fun observeCommunities(userId: String): Flow<List<Community>> =
        combine(
            db.communityDao().observeCommunities(),
            db.membershipDao().observeMembershipsForUser(userId)
        ) { communities, memberships ->
            val roleByCommunity = memberships.associateBy { it.communityId }
            communities.map { entity ->
                val membership = roleByCommunity[entity.id]
                entity.toDomain(
                    myRole = membership?.let { CommunityRole.fromRaw(it.role) },
                    isJoined = membership != null
                )
            }
        }

    fun observeCommunity(communityId: String, userId: String): Flow<Community?> =
        combine(
            db.communityDao().observeCommunity(communityId),
            db.membershipDao().observeMembershipsForUser(userId)
        ) { entity, memberships ->
            val membership = memberships.firstOrNull { it.communityId == communityId }
            entity?.toDomain(
                myRole = membership?.let { CommunityRole.fromRaw(it.role) },
                isJoined = membership != null
            )
        }

    fun observeVoiceParticipants(communityId: String): Flow<List<VoiceParticipant>> =
        combine(
            db.voiceRoomDao().observeParticipants(communityId),
            observeAllUsersMap(),
            db.membershipDao().observeMembershipsForCommunity(communityId)
        ) { participants, users, memberships ->
            val roleMap = memberships.associate { it.userId to CommunityRole.fromRaw(it.role) }
            participants.mapNotNull { vp ->
                val user = users[vp.userId] ?: return@mapNotNull null
                VoiceParticipant(
                    id = vp.id,
                    communityId = vp.communityId,
                    user = user,
                    role = roleMap[vp.userId] ?: CommunityRole.MEMBER,
                    isMuted = vp.isMuted,
                    isSpeaking = vp.isSpeaking,
                    isHandRaised = vp.isHandRaised
                )
            }.sortedByDescending { it.role.priority }
        }

    fun observeRoomEvents(communityId: String): Flow<List<RoomEventLog>> =
        db.voiceRoomDao().observeRoomEvents(communityId).map { list ->
            list.map {
                RoomEventLog(
                    id = it.id,
                    communityId = it.communityId,
                    actorName = it.actorName,
                    description = it.description,
                    timestampMillis = it.timestampMillis
                )
            }
        }

    fun observeMessages(communityId: String, channelType: String): Flow<List<ChatMessage>> =
        combine(
            db.chatDao().observeMessages(communityId, channelType),
            observeAllUsersMap(),
            db.membershipDao().observeMembershipsForCommunity(communityId)
        ) { messages, users, memberships ->
            val roleMap = memberships.associate { it.userId to CommunityRole.fromRaw(it.role) }
            messages.map { msg ->
                val sender = users[msg.senderId] ?: UserProfile(
                    id = msg.senderId,
                    username = "Usuario",
                    email = "",
                    bio = "",
                    customStatus = "",
                    avatarFrame = AvatarFrame.NONE,
                    avatarUri = null,
                    presetAvatarKey = "anime",
                    communitiesCount = 0,
                    followersCount = 0,
                    postsCount = 0
                )
                ChatMessage(
                    id = msg.id,
                    communityId = msg.communityId,
                    channelType = msg.channelType,
                    sender = sender,
                    senderRole = roleMap[msg.senderId] ?: CommunityRole.MEMBER,
                    content = msg.content,
                    replyToSnippet = msg.replyToSnippet,
                    reactionCount = msg.reactionCount,
                    timestampMillis = msg.timestampMillis,
                    audioUri = msg.audioUri,
                    audioDataB64 = msg.audioDataB64,
                    audioDurationMs = msg.audioDurationMs,
                    remoteId = msg.remoteId
                )
            }
        }

    fun observeDriftingBottles(): Flow<List<DriftingBottle>> =
        db.driftingBottleDao().observeActiveBottles(System.currentTimeMillis()).map { list ->
            list.map { it.toDomain() }
        }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        db.notificationDao().observeNotifications(userId).map { list ->
            list.map {
                AppNotification(
                    id = it.id,
                    userId = it.userId,
                    actorName = it.actorName,
                    actorAvatarKey = it.actorAvatarKey,
                    actorAvatarUri = it.actorAvatarUri,
                    actorFrame = AvatarFrame.fromId(it.actorFrameId),
                    title = it.title,
                    subtitle = it.subtitle,
                    category = NotificationCategory.fromRaw(it.category),
                    targetCommunityId = it.targetCommunityId,
                    isRead = it.isRead,
                    timestampMillis = it.timestampMillis
                )
            }
        }

    fun observeUnreadNotificationsCount(userId: String): Flow<Int> =
        db.notificationDao().observeUnreadCount(userId)

    fun observeCustomFrames(): Flow<List<CustomFrame>> =
        db.customFrameDao().observeAllFrames().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getCustomFrameById(frameId: String): CustomFrame? =
        db.customFrameDao().getFrameById(frameId)?.toDomain()

    fun observePosts(): Flow<List<CommunityPost>> =
        combine(
            db.postDao().observeAllPosts(),
            observeAllUsersMap(),
            db.communityDao().observeCommunities(),
            db.postDao().observeAllComments()
        ) { posts, users, communities, comments ->
            val commMap = communities.associateBy { it.id }
            val commentCountByPost = comments.groupingBy { it.postId }.eachCount()
            posts.mapNotNull { post ->
                val author = users[post.authorId] ?: return@mapNotNull null
                CommunityPost(
                    id = post.id,
                    author = author,
                    communityId = post.communityId,
                    communityName = post.communityId?.let { commMap[it]?.name },
                    content = post.content,
                    imageUri = post.imageUri,
                    likesCount = post.likesCount,
                    commentsCount = commentCountByPost[post.id] ?: 0,
                    createdAtMillis = post.createdAtMillis
                )
            }
        }

    fun observePostComments(postId: String): Flow<List<PostComment>> =
        combine(
            db.postDao().observeCommentsForPost(postId),
            observeAllUsersMap()
        ) { comments, users ->
            comments.mapNotNull { c ->
                val author = users[c.authorId] ?: return@mapNotNull null
                PostComment(
                    id = c.id,
                    postId = c.postId,
                    author = author,
                    content = c.content,
                    createdAtMillis = c.createdAtMillis
                )
            }
        }

    suspend fun getPostById(postId: String): PostEntity? =
        db.postDao().getPostById(postId)

    // --- LOCAL SESSION HELPERS & LOGOUT ---

    suspend fun registerLocalAccountForTest(
        username: String,
        email: String,
        emailVerified: Boolean = true
    ): Result<UserProfile> {
        val cleanUser = username.trim()
        val cleanEmail = email.trim()
        if (cleanUser.length < 3) {
            return Result.failure(IllegalArgumentException("El nombre de usuario debe tener al menos 3 caracteres."))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido."))
        }
        val existingByUser = db.userDao().findByEmailOrUsername(cleanUser)
        if (existingByUser != null) {
            val msg = if (existingByUser.isBanned) {
                "Esta cuenta está baneada permanentemente y no puede volver a usarse."
            } else {
                "Ese nombre de usuario ya está en uso."
            }
            return Result.failure(IllegalArgumentException(msg))
        }
        val existingByEmail = db.userDao().findByEmailOrUsername(cleanEmail)
        if (existingByEmail != null) {
            val msg = if (existingByEmail.isBanned) {
                "Este correo está baneado permanentemente y no puede volver a usarse."
            } else {
                "Ese correo ya está registrado."
            }
            return Result.failure(IllegalArgumentException(msg))
        }

        val isPrimaryOwner = isEmailVerifiedOwner(cleanEmail, emailVerified)
        val uid = "user_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val newUser = UserEntity(
            id = uid,
            username = cleanUser,
            email = cleanEmail,
            emailVerified = emailVerified,
            passwordHash = "",
            bio = "",
            customStatus = "",
            avatarFrameId = AvatarFrame.NEON_PULSE.id,
            avatarUri = null,
            presetAvatarKey = "anime",
            communitiesCount = 0,
            followersCount = 0,
            postsCount = 0,
            coinsCount = if (isPrimaryOwner) 999_999_999 else 100,
            maxFrameSlots = if (isPrimaryOwner) 120 else 10,
            appRankId = if (isPrimaryOwner) AppRank.OWNER.id else AppRank.NONE.id,
            isDeveloper = isPrimaryOwner
        )
        db.userDao().upsertUser(newUser)
        if (isPrimaryOwner) {
            db.notificationDao().insertNotification(
                NotificationEntity(
                    userId = uid,
                    actorName = "NexoLive",
                    actorAvatarKey = "anime",
                    actorAvatarUri = null,
                    actorFrameId = AvatarFrame.ROYAL_CROWN.id,
                    title = "Cuenta Dueño activada (Verificado Negro)",
                    subtitle = "Solo tu cuenta abukeje26@gmail.com controla verificaciones y baneos",
                    category = NotificationCategory.COMMUNITY.name,
                    targetCommunityId = null,
                    isRead = false,
                    timestampMillis = now
                )
            )
        }
        return Result.success(newUser.toDomain())
    }

    suspend fun logout() {
        globalSyncJob?.cancel()
        globalSyncJob = null
        remoteSync?.signOut()
        prefs.setCurrentUserId(null)
    }

    // --- GIFTS, DONOR LEVELS, TASKS, DAILY CHECK-IN & BAG ---

    suspend fun sendRoomGift(
        communityId: String,
        channelType: String,
        senderId: String,
        recipientId: String,
        giftId: String,
        comboCount: Int = 1,
        useBagItemId: String? = null
    ): Result<GiftSendOutcome> {
        val sender = db.userDao().getUserById(senderId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        if (sender.isBanned) {
            return Result.failure(IllegalStateException("Tu cuenta está suspendida."))
        }
        val safeCombo = comboCount.coerceIn(1, 99)
        val recipient = db.userDao().getUserById(recipientId)
        val recipientName = recipient?.username ?: "Sala"

        val inventory = parseInventory(sender.inventoryJson).toMutableMap()
        val giftEventName: String
        val giftEventEmoji: String
        val totalCost: Int
        val baseExp: Int
        val resolvedGiftId: String

        val isOwnerSender = isEmailVerifiedOwner(sender.email, sender.emailVerified)
        if (!useBagItemId.isNullOrBlank()) {
            val currentQty = inventory[useBagItemId] ?: 0
            if (currentQty <= 0) {
                return Result.failure(IllegalArgumentException("No te quedan unidades de este premio en Mi bolsa."))
            }
            val meta = BagCatalog.metaFor(useBagItemId)
            if (currentQty == 1) inventory.remove(useBagItemId) else inventory[useBagItemId] = currentQty - 1
            giftEventName = meta.name
            giftEventEmoji = meta.emoji
            totalCost = 0
            baseExp = meta.giftExpValue
            resolvedGiftId = useBagItemId
        } else {
            val gift = GiftCatalog.byId(giftId)
            totalCost = gift.coinCost * safeCombo
            if (!isOwnerSender && sender.coinsCount < totalCost) {
                return Result.failure(
                    IllegalArgumentException("Necesitas $totalCost monedas para enviar ${gift.name}. Completa misiones, vende marcos o recibe regalos.")
                )
            }
            giftEventName = gift.name
            giftEventEmoji = gift.emoji
            baseExp = totalCost.coerceAtLeast(5)
            resolvedGiftId = gift.id
        }

        val oldTier = DonorTier.forExp(sender.donorExp)
        val expMultiplier = if (oldTier.level >= DonorTier.GOLD_MASTER.level) 1.10 else 1.0
        val expGained = (baseExp * expMultiplier).toInt().coerceAtLeast(1)

        val newExp = sender.donorExp + expGained
        val newTier = DonorTier.forExp(newExp)
        val leveledUpTo = if (newTier.level > oldTier.level) newTier else null
        val bonusCoins = leveledUpTo?.coinBonusOnReach ?: 0
        val newCoins = if (isOwnerSender) {
            999_999_999
        } else {
            (sender.coinsCount - totalCost + bonusCoins).coerceAtLeast(0)
        }
        val newTotalDonated = sender.totalCoinsDonated + totalCost
        val newGiftProgress = sender.giftCoinsSentProgress + totalCost
        val newInvJson = serializeInventory(inventory)

        db.userDao().updateDonorAndBagProgress(
            userId = senderId,
            coins = newCoins,
            exp = newExp,
            level = newTier.level,
            totalDonated = newTotalDonated,
            giftCoinsSentProgress = newGiftProgress,
            inventoryJson = newInvJson
        )
        if (recipientId != senderId && recipient != null) {
            db.userDao().incrementGiftsReceived(recipientId)
            if (totalCost > 0) {
                val isRecipientOwner = isEmailVerifiedOwner(recipient.email, recipient.emailVerified)
                if (!isRecipientOwner) {
                    db.userDao().incrementCoins(recipientId, totalCost)
                }
                remoteSync?.creditUserCoins(recipientId, totalCost)
                val giftNotifTitle = "¡Recibiste un regalo de ${sender.username}!"
                val giftNotifSub = "$giftEventEmoji $giftEventName • Ganaste +$totalCost monedas"
                db.notificationDao().insertNotification(
                    NotificationEntity(
                        userId = recipientId,
                        actorName = sender.username,
                        actorAvatarKey = sender.presetAvatarKey,
                        actorAvatarUri = sender.avatarUri,
                        actorFrameId = sender.avatarFrameId,
                        title = giftNotifTitle,
                        subtitle = giftNotifSub,
                        category = NotificationCategory.COMMUNITY.name,
                        targetCommunityId = communityId,
                        isRead = false,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
                remoteSync?.sendNotification(
                    targetUserId = recipientId,
                    actorId = senderId,
                    actorName = sender.username,
                    actorAvatarKey = sender.presetAvatarKey,
                    actorFrameId = sender.avatarFrameId,
                    title = giftNotifTitle,
                    subtitle = giftNotifSub,
                    category = NotificationCategory.COMMUNITY.name,
                    targetCommunityId = communityId
                )
            }
        }

        val event = RoomGiftEvent(
            id = "gift_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
            communityId = communityId,
            channelType = channelType,
            senderId = senderId,
            senderName = sender.username,
            senderAvatarKey = sender.presetAvatarKey,
            senderFrameId = sender.avatarFrameId,
            recipientId = recipientId,
            recipientName = recipientName,
            giftId = resolvedGiftId,
            giftName = giftEventName,
            giftEmoji = giftEventEmoji,
            coinCost = totalCost,
            comboCount = safeCombo,
            timestampMillis = System.currentTimeMillis()
        )
        val prevMap = localGiftsByCommunity.value.toMutableMap()
        val updatedList = (listOf(event) + prevMap[communityId].orEmpty()).take(40)
        prevMap[communityId] = updatedList
        localGiftsByCommunity.value = prevMap

        val comboText = if (safeCombo > 1) " x$safeCombo" else ""
        val msgText = "$giftEventEmoji envió $giftEventName$comboText a $recipientName (+$expGained EXP Donador)"
        val now = System.currentTimeMillis()
        db.voiceRoomDao().insertEvent(
            RoomEventEntity(
                communityId = communityId,
                actorName = sender.username,
                description = "envió $giftEventEmoji $giftEventName$comboText a $recipientName",
                timestampMillis = now
            )
        )
        db.chatDao().insertMessage(
            ChatMessageEntity(
                communityId = communityId,
                channelType = channelType,
                senderId = senderId,
                content = msgText,
                replyToSnippet = null,
                reactionCount = 0,
                timestampMillis = now
            )
        )

        syncRewardsToRemote(sender.copy(
            coinsCount = newCoins,
            donorExp = newExp,
            donorLevel = newTier.level,
            totalCoinsDonated = newTotalDonated,
            giftCoinsSentProgress = newGiftProgress,
            inventoryJson = newInvJson
        ))

        remoteSync?.sendRoomGift(
            communityId = communityId,
            channelType = channelType,
            senderId = senderId,
            senderName = sender.username,
            senderAvatarKey = sender.presetAvatarKey,
            senderFrameId = sender.avatarFrameId,
            recipientId = recipientId,
            recipientName = recipientName,
            giftId = resolvedGiftId,
            giftName = giftEventName,
            giftEmoji = giftEventEmoji,
            coinCost = totalCost,
            comboCount = safeCombo
        )

        return Result.success(
            GiftSendOutcome(
                event = event,
                expGained = expGained,
                leveledUpTo = leveledUpTo,
                bonusCoinsAwarded = bonusCoins
            )
        )
    }

    suspend fun claimFreeCoins(userId: String, amount: Int = 1000): Result<Int> {
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        if (!isEmailVerifiedOwner(user.email, user.emailVerified)) {
            return Result.failure(
                IllegalStateException("Solo el Dueño tiene monedas infinitas. Consigue monedas completando misiones, vendiendo tus marcos o recibiendo regalos.")
            )
        }
        val infiniteCoins = 999_999_999
        db.userDao().updateCoins(userId, infiniteCoins)
        syncRewardsToRemote(user.copy(coinsCount = infiniteCoins))
        return Result.success(infiniteCoins)
    }

    suspend fun grantCoinsToUser(actorId: String, targetUserId: String, amount: Int): Result<Int> {
        val actor = db.userDao().getUserById(actorId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        if (!isEmailVerifiedOwner(actor.email, actor.emailVerified) || actor.isBanned) {
            return Result.failure(IllegalStateException("Solo el Dueño principal (${PRIMARY_OWNER_EMAIL}) puede regalar monedas."))
        }
        if (amount !in 1..100_000) {
            return Result.failure(IllegalArgumentException("Ingresa una cantidad entre 1 y 100,000 monedas."))
        }
        val target = db.userDao().getUserById(targetUserId)
            ?: return Result.failure(IllegalStateException("Usuario destinatario no encontrado."))
        if (targetUserId == actorId) {
            return Result.success(999_999_999)
        }
        db.userDao().incrementCoins(targetUserId, amount)
        val updatedBalance = target.coinsCount + amount
        remoteSync?.creditUserCoins(targetUserId, amount)
        val title = "🎁 ¡El Dueño te regaló monedas!"
        val subtitle = "${actor.username} te envió +$amount monedas para crear marcos y regalos"
        db.notificationDao().insertNotification(
            NotificationEntity(
                userId = targetUserId,
                actorName = actor.username,
                actorAvatarKey = actor.presetAvatarKey,
                actorAvatarUri = actor.avatarUri,
                actorFrameId = actor.avatarFrameId,
                title = title,
                subtitle = subtitle,
                category = NotificationCategory.COMMUNITY.name,
                targetCommunityId = null,
                isRead = false,
                timestampMillis = System.currentTimeMillis()
            )
        )
        remoteSync?.sendNotification(
            targetUserId = targetUserId,
            actorId = actorId,
            actorName = actor.username,
            actorAvatarKey = actor.presetAvatarKey,
            actorFrameId = actor.avatarFrameId,
            title = title,
            subtitle = subtitle,
            category = NotificationCategory.COMMUNITY.name
        )
        return Result.success(updatedBalance)
    }

    suspend fun followHost(actorId: String, targetUserId: String): Result<Unit> {
        val actor = db.userDao().getUserById(actorId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        if (actorId == targetUserId) {
            return Result.failure(IllegalArgumentException("No puedes seguirte a ti mismo."))
        }
        val target = db.userDao().getUserById(targetUserId)
            ?: return Result.failure(IllegalStateException("Emisor no encontrado."))
        db.userDao().incrementFollowersCount(targetUserId)
        val newFollowProg = actor.followHostsProgress + 1
        db.userDao().updateRewardsProgression(
            userId = actorId,
            coins = actor.coinsCount,
            exp = actor.donorExp,
            level = actor.donorLevel,
            streakDay = actor.dailyStreakDay,
            checkInMillis = actor.lastDailyCheckInAtMillis,
            followProg = newFollowProg,
            watchProg = actor.watchLiveMinutesProgress,
            gameProg = actor.gameCoinsEarnedProgress,
            giftProg = actor.giftCoinsSentProgress,
            claimedIds = actor.claimedTaskIds,
            inventoryJson = actor.inventoryJson,
            mePlus = actor.mePlusMember,
            lastDrawMillis = actor.lastMePlusDrawAtMillis
        )
        syncRewardsToRemote(actor.copy(followHostsProgress = newFollowProg))
        remoteSync?.incrementTargetFollowers(targetUserId)
        remoteSync?.sendNotification(
            targetUserId = targetUserId,
            actorId = actorId,
            actorName = actor.username,
            actorAvatarKey = actor.presetAvatarKey,
            actorFrameId = actor.avatarFrameId,
            title = "${actor.username} comenzó a seguirte",
            subtitle = "Ahora tienes un nuevo seguidor en NexoLive",
            category = NotificationCategory.FOLLOW.name
        )
        return Result.success(Unit)
    }

    suspend fun recordLiveWatchMinute(userId: String) {
        val user = db.userDao().getUserById(userId) ?: return
        val updatedProg = (user.watchLiveMinutesProgress + 1).coerceAtMost(60)
        if (updatedProg == user.watchLiveMinutesProgress) return
        db.userDao().updateRewardsProgression(
            userId = userId,
            coins = user.coinsCount,
            exp = user.donorExp,
            level = user.donorLevel,
            streakDay = user.dailyStreakDay,
            checkInMillis = user.lastDailyCheckInAtMillis,
            followProg = user.followHostsProgress,
            watchProg = updatedProg,
            gameProg = user.gameCoinsEarnedProgress,
            giftProg = user.giftCoinsSentProgress,
            claimedIds = user.claimedTaskIds,
            inventoryJson = user.inventoryJson,
            mePlus = user.mePlusMember,
            lastDrawMillis = user.lastMePlusDrawAtMillis
        )
        syncRewardsToRemote(user.copy(watchLiveMinutesProgress = updatedProg))
    }

    suspend fun playCoinMiniGame(userId: String, betCoins: Int, rewardCoins: Int): Result<Int> {
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        val isOwner = isEmailVerifiedOwner(user.email, user.emailVerified)
        if (betCoins <= 0 || (!isOwner && user.coinsCount < betCoins)) {
            return Result.failure(IllegalArgumentException("Monedas insuficientes para apostar $betCoins."))
        }
        val newCoins = if (isOwner) 999_999_999 else (user.coinsCount - betCoins + rewardCoins).coerceAtLeast(0)
        val newGameProg = user.gameCoinsEarnedProgress + betCoins
        db.userDao().updateRewardsProgression(
            userId = userId,
            coins = newCoins,
            exp = user.donorExp,
            level = user.donorLevel,
            streakDay = user.dailyStreakDay,
            checkInMillis = user.lastDailyCheckInAtMillis,
            followProg = user.followHostsProgress,
            watchProg = user.watchLiveMinutesProgress,
            gameProg = newGameProg,
            giftProg = user.giftCoinsSentProgress,
            claimedIds = user.claimedTaskIds,
            inventoryJson = user.inventoryJson,
            mePlus = user.mePlusMember,
            lastDrawMillis = user.lastMePlusDrawAtMillis
        )
        syncRewardsToRemote(user.copy(coinsCount = newCoins, gameCoinsEarnedProgress = newGameProg))
        return Result.success(newCoins)
    }

    suspend fun claimDailyCheckIn(userId: String, nowMillis: Long = System.currentTimeMillis()): Result<CheckInOutcome> {
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        val last = user.lastDailyCheckInAtMillis
        if (last > 0L && nowMillis - last < CHECK_IN_COOLDOWN_MS) {
            return Result.failure(IllegalStateException("Ya reclamaste tu premio diario hoy. Vuelve cuando termine el contador."))
        }
        val nextDay = if (last == 0L || nowMillis - last > CHECK_IN_STREAK_RESET_MS) {
            1
        } else {
            (user.dailyStreakDay % 7) + 1
        }
        val reward = DailyCheckInCatalog.days.first { it.day == nextDay }
        val inv = parseInventory(user.inventoryJson).toMutableMap()
        if (!reward.itemId.isNullOrBlank() && reward.itemCount > 0) {
            inv[reward.itemId] = (inv[reward.itemId] ?: 0) + reward.itemCount
        }
        if (nextDay == 7) {
            inv["gift_box"] = (inv["gift_box"] ?: 0) + 1
        }
        val oldTier = DonorTier.forExp(user.donorExp)
        val newExp = user.donorExp + reward.expGrant
        val newTier = DonorTier.forExp(newExp)
        val leveledUpTo = if (newTier.level > oldTier.level) newTier else null
        val levelBonus = leveledUpTo?.coinBonusOnReach ?: 0
        val newCoins = if (isEmailVerifiedOwner(user.email, user.emailVerified)) {
            999_999_999
        } else {
            user.coinsCount + reward.coinGrant + levelBonus
        }
        val newInvJson = serializeInventory(inv)

        db.userDao().updateRewardsProgression(
            userId = userId,
            coins = newCoins,
            exp = newExp,
            level = newTier.level,
            streakDay = nextDay,
            checkInMillis = nowMillis,
            followProg = user.followHostsProgress,
            watchProg = user.watchLiveMinutesProgress,
            gameProg = user.gameCoinsEarnedProgress,
            giftProg = user.giftCoinsSentProgress,
            claimedIds = user.claimedTaskIds,
            inventoryJson = newInvJson,
            mePlus = user.mePlusMember,
            lastDrawMillis = user.lastMePlusDrawAtMillis
        )
        syncRewardsToRemote(user.copy(
            coinsCount = newCoins,
            donorExp = newExp,
            donorLevel = newTier.level,
            dailyStreakDay = nextDay,
            lastDailyCheckInAtMillis = nowMillis,
            inventoryJson = newInvJson
        ))
        return Result.success(CheckInOutcome(nextDay, reward, leveledUpTo, levelBonus))
    }

    suspend fun claimDailyTaskReward(userId: String, taskId: String): Result<TaskClaimOutcome> {
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        val task = DailyTaskCatalog.tasks.firstOrNull { it.id == taskId }
            ?: return Result.failure(IllegalArgumentException("Tarea desconocida."))
        val claimedSet = user.claimedTaskIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        if (taskId in claimedSet) {
            return Result.failure(IllegalStateException("Ya reclamaste esta recompensa."))
        }
        val progress = when (taskId) {
            DailyTaskCatalog.TASK_FOLLOW_HOST -> user.followHostsProgress
            DailyTaskCatalog.TASK_WATCH_LIVE_5M -> user.watchLiveMinutesProgress
            DailyTaskCatalog.TASK_GAME_10K_COINS -> user.gameCoinsEarnedProgress
            DailyTaskCatalog.TASK_SEND_200_GIFTS -> user.giftCoinsSentProgress
            else -> 0
        }
        if (progress < task.target) {
            return Result.failure(IllegalArgumentException("Aún te falta completar el objetivo ($progress/${task.target})."))
        }

        claimedSet.add(taskId)
        val inv = parseInventory(user.inventoryJson).toMutableMap()
        if (!task.itemId.isNullOrBlank() && task.itemQty > 0) {
            inv[task.itemId] = (inv[task.itemId] ?: 0) + task.itemQty
        }
        if (!task.secondaryItemId.isNullOrBlank() && task.secondaryItemQty > 0) {
            inv[task.secondaryItemId] = (inv[task.secondaryItemId] ?: 0) + task.secondaryItemQty
        }
        val oldTier = DonorTier.forExp(user.donorExp)
        val newExp = user.donorExp + task.expReward
        val newTier = DonorTier.forExp(newExp)
        val leveledUpTo = if (newTier.level > oldTier.level) newTier else null
        val levelBonus = leveledUpTo?.coinBonusOnReach ?: 0
        val newCoins = if (isEmailVerifiedOwner(user.email, user.emailVerified)) {
            999_999_999
        } else {
            user.coinsCount + task.coinReward + levelBonus
        }
        val newClaimedStr = claimedSet.joinToString(",")
        val newInvJson = serializeInventory(inv)

        db.userDao().updateRewardsProgression(
            userId = userId,
            coins = newCoins,
            exp = newExp,
            level = newTier.level,
            streakDay = user.dailyStreakDay,
            checkInMillis = user.lastDailyCheckInAtMillis,
            followProg = user.followHostsProgress,
            watchProg = user.watchLiveMinutesProgress,
            gameProg = user.gameCoinsEarnedProgress,
            giftProg = user.giftCoinsSentProgress,
            claimedIds = newClaimedStr,
            inventoryJson = newInvJson,
            mePlus = user.mePlusMember,
            lastDrawMillis = user.lastMePlusDrawAtMillis
        )
        syncRewardsToRemote(user.copy(
            coinsCount = newCoins,
            donorExp = newExp,
            donorLevel = newTier.level,
            claimedTaskIds = newClaimedStr,
            inventoryJson = newInvJson
        ))
        return Result.success(TaskClaimOutcome(task.title, task.expReward, leveledUpTo, levelBonus))
    }

    suspend fun activateMePlusAndDraw(userId: String, nowMillis: Long = System.currentTimeMillis()): Result<MePlusDrawOutcome> {
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        if (user.lastMePlusDrawAtMillis > 0L && nowMillis - user.lastMePlusDrawAtMillis < ME_PLUS_DRAW_COOLDOWN_MS) {
            return Result.failure(IllegalStateException("Ya giraste el Sorteo Me+ hoy. Vuelve mañana por otro cofre."))
        }
        val inv = parseInventory(user.inventoryJson).toMutableMap()
        val prizes = listOf(
            MePlusDrawOutcome("Corona Me+ Exclusiva + 300 Monedas", 300, 120, "Corona Me+", null),
            MePlusDrawOutcome("Turbo Avión VIP + 200 EXP", 150, 200, "Avión VIP", null),
            MePlusDrawOutcome("Moto Deportiva + 250 Monedas", 250, 100, "Moto Deportiva", null),
            MePlusDrawOutcome("Cofre Dorado Me+ (500 Monedas)", 500, 150, "Caja de Regalo", null)
        )
        val picked = prizes.random()
        val bagItemId = when (picked.bagItemWonName) {
            "Corona Me+" -> "meplus_crown"
            "Avión VIP" -> "plane"
            "Moto Deportiva" -> "moto"
            else -> "gift_box"
        }
        inv[bagItemId] = (inv[bagItemId] ?: 0) + 1

        val oldTier = DonorTier.forExp(user.donorExp)
        val newExp = user.donorExp + picked.expWon
        val newTier = DonorTier.forExp(newExp)
        val leveledUpTo = if (newTier.level > oldTier.level) newTier else null
        val levelBonus = leveledUpTo?.coinBonusOnReach ?: 0
        val newCoins = if (isEmailVerifiedOwner(user.email, user.emailVerified)) {
            999_999_999
        } else {
            user.coinsCount + picked.coinsWon + levelBonus
        }
        val newInvJson = serializeInventory(inv)

        db.userDao().updateRewardsProgression(
            userId = userId,
            coins = newCoins,
            exp = newExp,
            level = newTier.level,
            streakDay = user.dailyStreakDay,
            checkInMillis = user.lastDailyCheckInAtMillis,
            followProg = user.followHostsProgress,
            watchProg = user.watchLiveMinutesProgress,
            gameProg = user.gameCoinsEarnedProgress,
            giftProg = user.giftCoinsSentProgress,
            claimedIds = user.claimedTaskIds,
            inventoryJson = newInvJson,
            mePlus = true,
            lastDrawMillis = nowMillis
        )
        syncRewardsToRemote(user.copy(
            coinsCount = newCoins,
            donorExp = newExp,
            donorLevel = newTier.level,
            inventoryJson = newInvJson,
            mePlusMember = true,
            lastMePlusDrawAtMillis = nowMillis
        ))
        return Result.success(picked.copy(leveledUpTo = leveledUpTo))
    }

    private suspend fun syncRewardsToRemote(user: UserEntity) {
        remoteSync?.syncUserRewardsAndDonor(
            uid = user.id,
            coinsCount = user.coinsCount,
            donorExp = user.donorExp,
            donorLevel = user.donorLevel,
            totalCoinsDonated = user.totalCoinsDonated,
            dailyStreakDay = user.dailyStreakDay,
            lastDailyCheckInAtMillis = user.lastDailyCheckInAtMillis,
            followHostsProgress = user.followHostsProgress,
            watchLiveMinutesProgress = user.watchLiveMinutesProgress,
            gameCoinsEarnedProgress = user.gameCoinsEarnedProgress,
            giftCoinsSentProgress = user.giftCoinsSentProgress,
            claimedTaskIds = user.claimedTaskIds,
            inventoryJson = user.inventoryJson,
            mePlusMember = user.mePlusMember,
            lastMePlusDrawAtMillis = user.lastMePlusDrawAtMillis
        )
    }

    // --- PROFILE CUSTOMIZATION ---

    suspend fun copyPickedImageToInternalStorage(
        uri: Uri,
        folder: String,
        prefix: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(context.filesDir, folder).apply { mkdirs() }
            val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
            val ext = if (mime.contains("png")) "png" else "jpg"
            val destFile = File(targetDir, "${prefix}_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("No se pudo leer la imagen"))
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyPickedAvatarToInternalStorage(uri: Uri): Result<String> =
        copyPickedImageToInternalStorage(uri, "avatars", "avatar")

    suspend fun saveBase64AudioToInternalStorage(
        base64OrDataUrl: String,
        folder: String = "audio_notes",
        prefix: String = "voice"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val header = base64OrDataUrl.substringBefore("base64,", "").lowercase()
            val ext = when {
                header.contains("webm") -> "webm"
                header.contains("ogg") -> "ogg"
                header.contains("wav") -> "wav"
                header.contains("mpeg") || header.contains("mp3") -> "mp3"
                else -> "m4a"
            }
            val rawBase64 = base64OrDataUrl.substringAfter("base64,", base64OrDataUrl).trim()
            val decoded = android.util.Base64.decode(rawBase64, android.util.Base64.DEFAULT)
            if (decoded.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Audio vacío o inválido."))
            }
            val targetDir = File(context.filesDir, folder).apply { mkdirs() }
            val destFile = File(targetDir, "${prefix}_${UUID.randomUUID().toString().take(8)}.$ext")
            destFile.writeBytes(decoded)
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfileCustomization(
        userId: String,
        username: String,
        bio: String,
        customStatus: String,
        frameId: String,
        avatarUri: String?,
        presetAvatarKey: String
    ): Result<Unit> {
        val cleanName = username.trim()
        if (cleanName.length < 3) {
            return Result.failure(IllegalArgumentException("El nombre debe tener al menos 3 caracteres."))
        }
        val cleanBio = bio.trim().take(160)
        val cleanStatus = customStatus.trim().take(60)
        val cleanFrame = frameId.ifBlank { AvatarFrame.NEON_PULSE.id }
        db.userDao().updateProfileCustomization(
            userId = userId,
            username = cleanName.take(28),
            bio = cleanBio,
            status = cleanStatus,
            frameId = cleanFrame,
            avatarUri = avatarUri,
            presetAvatarKey = presetAvatarKey
        )
        remoteSync?.updateProfile(userId, cleanName.take(28), cleanBio, cleanStatus, cleanFrame, presetAvatarKey)
        return Result.success(Unit)
    }

    // --- TIENDA DE CREACIONES, CUPOS DE VENTA & CUSTOM AVATAR FRAMES ---

    suspend fun publishPngFrame(
        creatorId: String,
        title: String,
        pngPath: String,
        priceCoins: Int = 0,
        description: String = "",
        category: String = StoreFrameCategory.PROFILE_FRAME.label,
        isFeatured: Boolean = false,
        chargeStoreFee: Boolean = false
    ): Result<CustomFrame> {
        val cleanTitle = title.trim()
        if (cleanTitle.length < 2) {
            return Result.failure(IllegalArgumentException("Escribe un nombre de al menos 2 caracteres para el diseño."))
        }
        val creator = db.userDao().getUserById(creatorId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        if (creator.isBanned) {
            return Result.failure(IllegalStateException("Tu cuenta está suspendida."))
        }
        val isOwner = isEmailVerifiedOwner(creator.email, creator.emailVerified)
        val publishedCount = db.customFrameDao().countByCreator(creatorId)
        val maxSlots = if (isOwner) 120 else creator.maxFrameSlots.coerceIn(10, 120)
        if (publishedCount >= maxSlots) {
            return Result.failure(
                IllegalStateException("⚠️ ¡Tu cupo está lleno! Tienes $publishedCount/$maxSlots diseños en venta. Retira uno antiguo o amplía tu cupo.")
            )
        }
        val isStoreListing = chargeStoreFee || priceCoins > 0
        if (isStoreListing && priceCoins !in 20..500) {
            return Result.failure(IllegalArgumentException("El precio en la Tienda debe estar entre 💎 20 y 💎 500 monedas."))
        }
        val categoryEnum = StoreFrameCategory.fromLabel(category)
        val creatorRank = CreatorRank.forSales(creator.creatorSalesCount)
        val publishFee = if (!isStoreListing || isOwner) 0 else creatorRank.publishCostFor(categoryEnum, isFeatured)
        if (publishFee > 0 && creator.coinsCount < publishFee) {
            return Result.failure(
                IllegalArgumentException("Necesitas 💎 $publishFee monedas para publicar este diseño (tarifa de tienda no reembolsable).")
            )
        }
        val updatedCoins = if (isOwner) 999_999_999 else (creator.coinsCount - publishFee).coerceAtLeast(0)
        if (publishFee > 0) {
            db.userDao().updateCoins(creatorId, updatedCoins)
            remoteSync?.syncStoreProfileState(creatorId, updatedCoins, maxSlots, creator.favoriteFrameIdsCsv)
        }

        val safePrice = if (isStoreListing) priceCoins.coerceIn(20, 500) else 0
        val cleanDesc = description.trim().take(100)
        val entity = CustomFrameEntity(
            id = "custom_frame_${UUID.randomUUID().toString().take(8)}",
            creatorId = creatorId,
            creatorName = creator.username,
            title = cleanTitle.take(30),
            frameType = "PNG",
            pngPath = pngPath,
            primaryColorHex = 0xFF9D4EDD,
            secondaryColorHex = 0xFFFF4EC2,
            stylePatternId = CustomFrameStyle.RING_GLOW.id,
            thicknessDp = 4f,
            hasSparkles = false,
            priceCoins = safePrice,
            description = cleanDesc,
            category = categoryEnum.label,
            isFeatured = isFeatured,
            salesCount = 0,
            favoriteCount = 0,
            createdAtMillis = System.currentTimeMillis()
        )
        db.customFrameDao().insertFrame(entity)
        remoteSync?.publishCustomFrame(
            frameId = entity.id,
            creatorId = creatorId,
            creatorName = creator.username,
            title = entity.title,
            frameType = "PNG",
            pngPath = pngPath.takeIf { it.startsWith("http") },
            primaryColorHex = entity.primaryColorHex,
            secondaryColorHex = entity.secondaryColorHex,
            stylePatternId = entity.stylePatternId,
            thicknessDp = entity.thicknessDp,
            sparkles = false,
            priceCoins = safePrice,
            description = cleanDesc,
            category = categoryEnum.label,
            isFeatured = isFeatured
        )
        return Result.success(entity.toDomain())
    }

    suspend fun publishStudioFrame(
        creatorId: String,
        title: String,
        primaryColorHex: Long,
        secondaryColorHex: Long,
        stylePattern: CustomFrameStyle,
        thicknessDp: Float,
        hasSparkles: Boolean,
        priceCoins: Int = 0,
        description: String = "",
        category: String = StoreFrameCategory.PROFILE_FRAME.label,
        isFeatured: Boolean = false,
        chargeStoreFee: Boolean = false
    ): Result<CustomFrame> {
        val cleanTitle = title.trim()
        if (cleanTitle.length < 2) {
            return Result.failure(IllegalArgumentException("Escribe un nombre de al menos 2 caracteres para el diseño."))
        }
        val creator = db.userDao().getUserById(creatorId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        if (creator.isBanned) {
            return Result.failure(IllegalStateException("Tu cuenta está suspendida."))
        }
        val isOwner = isEmailVerifiedOwner(creator.email, creator.emailVerified)
        val publishedCount = db.customFrameDao().countByCreator(creatorId)
        val maxSlots = if (isOwner) 120 else creator.maxFrameSlots.coerceIn(10, 120)
        if (publishedCount >= maxSlots) {
            return Result.failure(
                IllegalStateException("⚠️ ¡Tu cupo está lleno! Tienes $publishedCount/$maxSlots diseños en venta. Retira uno antiguo o amplía tu cupo.")
            )
        }
        val isStoreListing = chargeStoreFee || priceCoins > 0
        if (isStoreListing && priceCoins !in 20..500) {
            return Result.failure(IllegalArgumentException("El precio en la Tienda debe estar entre 💎 20 y 💎 500 monedas."))
        }
        val categoryEnum = StoreFrameCategory.fromLabel(category)
        val creatorRank = CreatorRank.forSales(creator.creatorSalesCount)
        val publishFee = if (!isStoreListing || isOwner) 0 else creatorRank.publishCostFor(categoryEnum, isFeatured)
        if (publishFee > 0 && creator.coinsCount < publishFee) {
            return Result.failure(
                IllegalArgumentException("Necesitas 💎 $publishFee monedas para publicar este diseño (tarifa de tienda no reembolsable).")
            )
        }
        val updatedCoins = if (isOwner) 999_999_999 else (creator.coinsCount - publishFee).coerceAtLeast(0)
        if (publishFee > 0) {
            db.userDao().updateCoins(creatorId, updatedCoins)
            remoteSync?.syncStoreProfileState(creatorId, updatedCoins, maxSlots, creator.favoriteFrameIdsCsv)
        }

        val safePrice = if (isStoreListing) priceCoins.coerceIn(20, 500) else 0
        val cleanDesc = description.trim().take(100)
        val entity = CustomFrameEntity(
            id = "custom_frame_${UUID.randomUUID().toString().take(8)}",
            creatorId = creatorId,
            creatorName = creator.username,
            title = cleanTitle.take(30),
            frameType = "STUDIO",
            pngPath = null,
            primaryColorHex = primaryColorHex,
            secondaryColorHex = secondaryColorHex,
            stylePatternId = stylePattern.id,
            thicknessDp = thicknessDp.coerceIn(2f, 8f),
            hasSparkles = hasSparkles,
            priceCoins = safePrice,
            description = cleanDesc,
            category = categoryEnum.label,
            isFeatured = isFeatured,
            salesCount = 0,
            favoriteCount = 0,
            createdAtMillis = System.currentTimeMillis()
        )
        db.customFrameDao().insertFrame(entity)
        remoteSync?.publishCustomFrame(
            frameId = entity.id,
            creatorId = creatorId,
            creatorName = creator.username,
            title = entity.title,
            frameType = "STUDIO",
            pngPath = null,
            primaryColorHex = primaryColorHex,
            secondaryColorHex = secondaryColorHex,
            stylePatternId = stylePattern.id,
            thicknessDp = entity.thicknessDp,
            sparkles = hasSparkles,
            priceCoins = safePrice,
            description = cleanDesc,
            category = categoryEnum.label,
            isFeatured = isFeatured
        )
        return Result.success(entity.toDomain())
    }

    suspend fun unlockNextFrameSlotTier(userId: String): Result<FrameSlotTier> {
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        if (user.isBanned) {
            return Result.failure(IllegalStateException("Tu cuenta está suspendida."))
        }
        val isOwner = isEmailVerifiedOwner(user.email, user.emailVerified)
        val nextTier = FrameSlotTier.nextFor(user.maxFrameSlots)
            ?: return Result.failure(IllegalStateException("Ya desbloqueaste el cupo máximo de 120 espacios."))
        if (!isOwner && user.coinsCount < nextTier.unlockCost) {
            return Result.failure(
                IllegalArgumentException("Necesitas 💎 ${nextTier.unlockCost} monedas para ampliar a ${nextTier.maxSlots} espacios.")
            )
        }
        val newCoins = if (isOwner) 999_999_999 else (user.coinsCount - nextTier.unlockCost).coerceAtLeast(0)
        db.userDao().updateCoinsAndMaxSlots(userId, newCoins, nextTier.maxSlots)
        remoteSync?.syncStoreProfileState(userId, newCoins, nextTier.maxSlots, user.favoriteFrameIdsCsv)
        return Result.success(nextTier)
    }

    suspend fun toggleFavoriteFrame(userId: String, frameId: String): Result<Boolean> {
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        val favs = user.favoriteFrameIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        val nowFavorited = if (frameId in favs) {
            favs.remove(frameId)
            false
        } else {
            favs.add(frameId)
            true
        }
        val newCsv = favs.joinToString(",")
        val delta = if (nowFavorited) 1 else -1
        db.userDao().updateFavoriteFrames(userId, newCsv)
        db.customFrameDao().adjustFavoriteCount(frameId, delta)
        remoteSync?.syncStoreProfileState(userId, user.coinsCount, user.maxFrameSlots, newCsv)
        remoteSync?.adjustFrameFavorite(frameId, delta)
        return Result.success(nowFavorited)
    }

    suspend fun buyCustomFrame(buyerId: String, frameId: String): Result<CustomFrame> {
        val buyer = db.userDao().getUserById(buyerId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        val frameEntity = db.customFrameDao().getFrameById(frameId)
            ?: return Result.failure(IllegalStateException("Marco no encontrado."))
        val domainFrame = frameEntity.toDomain()
        val inv = parseInventory(buyer.inventoryJson).toMutableMap()
        val ownedKey = "frame_${frameEntity.id}"
        val alreadyOwned = frameEntity.creatorId == buyerId || frameEntity.priceCoins <= 0 || (inv[ownedKey] ?: 0) > 0
        val isOwnerBuyer = isEmailVerifiedOwner(buyer.email, buyer.emailVerified)

        val price = if (alreadyOwned) 0 else frameEntity.priceCoins
        if (!alreadyOwned && !isOwnerBuyer && buyer.coinsCount < price) {
            return Result.failure(
                IllegalArgumentException("Necesitas 💎 $price monedas para comprar \"${frameEntity.title}\". Consigue monedas en misiones o regalos.")
            )
        }

        inv[ownedKey] = 1
        val newInvJson = serializeInventory(inv)
        val newBuyerCoins = if (isOwnerBuyer) 999_999_999 else (buyer.coinsCount - price).coerceAtLeast(0)

        db.userDao().updateDonorAndBagProgress(
            userId = buyerId,
            coins = newBuyerCoins,
            exp = buyer.donorExp,
            level = buyer.donorLevel,
            totalDonated = buyer.totalCoinsDonated,
            giftCoinsSentProgress = buyer.giftCoinsSentProgress,
            inventoryJson = newInvJson
        )
        db.userDao().updateProfileCustomization(
            userId = buyerId,
            username = buyer.username,
            bio = buyer.bio,
            status = buyer.customStatus,
            frameId = frameEntity.id,
            avatarUri = buyer.avatarUri,
            presetAvatarKey = buyer.presetAvatarKey
        )
        remoteSync?.updateProfile(buyerId, buyer.username, buyer.bio, buyer.customStatus, frameEntity.id, buyer.presetAvatarKey)
        syncRewardsToRemote(buyer.copy(coinsCount = newBuyerCoins, inventoryJson = newInvJson))

        if (price > 0 && frameEntity.creatorId != buyerId) {
            db.customFrameDao().incrementSalesCount(frameId)
            remoteSync?.incrementFrameSales(frameId)

            val creator = db.userDao().getUserById(frameEntity.creatorId)
            val creatorRank = CreatorRank.forSales(creator?.creatorSalesCount ?: 0)
            val creatorShare = ((price * creatorRank.creatorSharePct) / 100).coerceAtLeast(1)
            val platformFee = (price - creatorShare).coerceAtLeast(0)
            val isCreatorOwner = creator != null && isEmailVerifiedOwner(creator.email, creator.emailVerified)
            if (creator != null) {
                if (isCreatorOwner) {
                    db.userDao().recordOwnerCreatorSale(frameEntity.creatorId, creatorShare)
                } else {
                    db.userDao().recordCreatorSale(frameEntity.creatorId, creatorShare, creatorShare)
                }
            }
            remoteSync?.creditCreatorFrameSale(frameEntity.creatorId, creatorShare, creatorShare)

            val saleTitle = "¡Vendiste \"${frameEntity.title}\"!"
            val saleSub = "${buyer.username} pagó 💎$price • Ganaste 💎$creatorShare (${creatorRank.creatorSharePct}%, comisión $platformFee)"
            db.notificationDao().insertNotification(
                NotificationEntity(
                    userId = frameEntity.creatorId,
                    actorName = buyer.username,
                    actorAvatarKey = buyer.presetAvatarKey,
                    actorAvatarUri = buyer.avatarUri,
                    actorFrameId = buyer.avatarFrameId,
                    title = saleTitle,
                    subtitle = saleSub,
                    category = NotificationCategory.COMMUNITY.name,
                    targetCommunityId = null,
                    isRead = false,
                    timestampMillis = System.currentTimeMillis()
                )
            )
            remoteSync?.sendNotification(
                targetUserId = frameEntity.creatorId,
                actorId = buyerId,
                actorName = buyer.username,
                actorAvatarKey = buyer.presetAvatarKey,
                actorFrameId = buyer.avatarFrameId,
                title = saleTitle,
                subtitle = saleSub,
                category = NotificationCategory.COMMUNITY.name
            )
        }
        return Result.success(domainFrame)
    }

    suspend fun deleteCustomFrame(frameId: String, actorId: String): Result<Unit> {
        val frame = db.customFrameDao().getFrameById(frameId)
            ?: return Result.failure(IllegalStateException("Marco no encontrado."))
        val actor = db.userDao().getUserById(actorId)
        val isOwner = actor != null && isEmailVerifiedOwner(actor.email, actor.emailVerified)
        if (frame.creatorId != actorId && !isOwner) {
            return Result.failure(IllegalStateException("Solo el creador del marco o el Dueño pueden eliminarlo."))
        }
        db.customFrameDao().deleteFrameById(frameId)
        remoteSync?.deleteCustomFrame(frameId)
        return Result.success(Unit)
    }

    // --- SINGLE STRICT OWNER VERIFICATION & BAN CONTROLS ---

    suspend fun assignAppRank(
        actorId: String,
        targetUserId: String,
        newRank: AppRank
    ): Result<Unit> {
        val actor = db.userDao().getUserById(actorId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        if (!isEmailVerifiedOwner(actor.email, actor.emailVerified) || actor.isBanned) {
            return Result.failure(IllegalStateException("Solo el único Dueño verificado (${PRIMARY_OWNER_EMAIL}) puede gestionar insignias."))
        }
        val target = db.userDao().getUserById(targetUserId)
            ?: return Result.failure(IllegalStateException("Usuario destino no encontrado."))
        if (targetUserId != actorId && newRank == AppRank.OWNER) {
            return Result.failure(IllegalStateException("Solo puede existir un único Dueño en NexoLive. Usa Moderador o Influencer."))
        }
        db.userDao().updateAppRank(targetUserId, newRank.id)
        remoteSync?.assignAppRank(targetUserId, newRank.id)
        if (targetUserId != actorId) {
            val title = if (newRank == AppRank.NONE) "Insignia actualizada" else "¡Recibiste una insignia de verificación!"
            val sub = if (newRank == AppRank.NONE) "Tu insignia de perfil fue actualizada" else "Ya aparece junto a tu nombre en perfiles y salas"
            db.notificationDao().insertNotification(
                NotificationEntity(
                    userId = targetUserId,
                    actorName = actor.username,
                    actorAvatarKey = actor.presetAvatarKey,
                    actorAvatarUri = actor.avatarUri,
                    actorFrameId = actor.avatarFrameId,
                    title = title,
                    subtitle = sub,
                    category = NotificationCategory.COMMUNITY.name,
                    targetCommunityId = null,
                    isRead = false,
                    timestampMillis = System.currentTimeMillis()
                )
            )
            remoteSync?.sendNotification(
                targetUserId = targetUserId,
                actorId = actorId,
                actorName = actor.username,
                actorAvatarKey = actor.presetAvatarKey,
                actorFrameId = actor.avatarFrameId,
                title = title,
                subtitle = sub,
                category = NotificationCategory.COMMUNITY.name
            )
        }
        return Result.success(Unit)
    }

    suspend fun banUser(
        actorId: String,
        targetUserId: String,
        reason: String
    ): Result<Unit> {
        val actor = db.userDao().getUserById(actorId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        if (!isEmailVerifiedOwner(actor.email, actor.emailVerified) || actor.isBanned) {
            return Result.failure(IllegalStateException("Solo el Dueño principal (${PRIMARY_OWNER_EMAIL}) puede banear usuarios."))
        }
        if (actorId == targetUserId) {
            return Result.failure(IllegalArgumentException("No puedes banearte a ti mismo."))
        }
        val target = db.userDao().getUserById(targetUserId)
            ?: return Result.failure(IllegalStateException("Usuario objetivo no encontrado."))
        if (target.email.equals(PRIMARY_OWNER_EMAIL, ignoreCase = true)) {
            return Result.failure(IllegalStateException("No se puede banear la cuenta principal."))
        }
        val cleanReason = reason.trim().ifBlank { "Actividad ilegal o incumplimiento grave de normas" }.take(180)
        db.userDao().updateBanStatus(targetUserId, true, cleanReason)
        remoteSync?.setBanStatus(targetUserId, true, cleanReason)

        val roomIds = db.voiceRoomDao().getCommunityIdsForParticipant(targetUserId)
        db.voiceRoomDao().removeParticipantFromAllRooms(targetUserId)
        val now = System.currentTimeMillis()
        roomIds.forEach { cid ->
            syncActiveVoiceCount(cid)
            db.voiceRoomDao().insertEvent(
                RoomEventEntity(
                    communityId = cid,
                    actorName = target.username,
                    description = "fue baneado permanentemente de NexoLive",
                    timestampMillis = now
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun unbanUser(
        actorId: String,
        targetUserId: String
    ): Result<Unit> {
        val actor = db.userDao().getUserById(actorId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        if (!isEmailVerifiedOwner(actor.email, actor.emailVerified) || actor.isBanned) {
            return Result.failure(IllegalStateException("Solo el Dueño principal (${PRIMARY_OWNER_EMAIL}) puede quitar baneos."))
        }
        db.userDao().getUserById(targetUserId)
            ?: return Result.failure(IllegalStateException("Usuario objetivo no encontrado."))
        db.userDao().updateBanStatus(targetUserId, false, null)
        remoteSync?.setBanStatus(targetUserId, false, null)
        return Result.success(Unit)
    }

    // --- COMMUNITIES ---

    suspend fun createCommunity(
        creatorId: String,
        name: String,
        category: String,
        description: String,
        coverKey: String,
        backgroundUri: String? = null
    ): Result<String> {
        if (isUserBannedOrMissing(creatorId)) {
            return Result.failure(IllegalStateException("Tu cuenta está suspendida."))
        }
        val cleanName = name.trim()
        if (cleanName.length < 3) {
            return Result.failure(IllegalArgumentException("El nombre debe tener al menos 3 caracteres."))
        }
        val cid = "comm_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val resolvedCategory = category.ifBlank { "Comunidad" }
        val resolvedDesc = description.trim().ifBlank { "Nuevo círculo abierto para charlar en tiempo real." }
        db.communityDao().upsertCommunity(
            CommunityEntity(
                id = cid,
                name = cleanName.take(36),
                category = resolvedCategory,
                description = resolvedDesc,
                coverKey = coverKey,
                badgeLabel = "nueva",
                memberCount = 1,
                activeVoiceCount = 0,
                isVoiceLocked = false,
                backgroundUri = backgroundUri
            )
        )
        db.membershipDao().upsertMembership(
            MembershipEntity(cid, creatorId, CommunityRole.ADMIN.name, now)
        )
        val count = db.membershipDao().countMemberships(creatorId)
        db.userDao().updateCommunitiesCount(creatorId, count)
        val creator = db.userDao().getUserById(creatorId)
        db.voiceRoomDao().insertEvent(
            RoomEventEntity(
                communityId = cid,
                actorName = creator?.username ?: "Admin",
                description = "creó la comunidad como Admin",
                timestampMillis = now
            )
        )
        remoteSync?.createCommunity(cid, creatorId, cleanName.take(36), resolvedCategory, resolvedDesc, coverKey)
        return Result.success(cid)
    }

    suspend fun updateCommunityBackground(
        communityId: String,
        actorId: String,
        backgroundUri: String?
    ): Result<Unit> {
        val actor = db.userDao().getUserById(actorId)
        val isOwner = actor != null && isEmailVerifiedOwner(actor.email, actor.emailVerified)
        val actorRole = getRole(communityId, actorId)
        if (actorRole != CommunityRole.ADMIN && actorRole != CommunityRole.CO_ADMIN && !isOwner) {
            return Result.failure(IllegalStateException("Solo Admin o Co-Admin pueden cambiar el fondo de la sala."))
        }
        db.communityDao().updateBackground(communityId, backgroundUri)
        db.voiceRoomDao().insertEvent(
            RoomEventEntity(
                communityId = communityId,
                actorName = actor?.username ?: "Admin",
                description = if (backgroundUri != null) "personalizó el fondo de la sala" else "restauró el fondo predeterminado de la sala",
                timestampMillis = System.currentTimeMillis()
            )
        )
        return Result.success(Unit)
    }

    suspend fun deleteCommunity(communityId: String, actorId: String): Result<Unit> {
        val actor = db.userDao().getUserById(actorId)
        val isOwner = actor != null && isEmailVerifiedOwner(actor.email, actor.emailVerified)
        val actorRole = getRole(communityId, actorId)
        if (actorRole != CommunityRole.ADMIN && !isOwner) {
            return Result.failure(IllegalStateException("Solo el Admin de la sala o el Dueño pueden eliminarla."))
        }
        val affectedUserIds = db.membershipDao().getUserIdsByCommunity(communityId)
        db.voiceRoomDao().deleteAllParticipantsForCommunity(communityId)
        db.voiceRoomDao().clearRoomEvents(communityId)
        db.chatDao().deleteAllMessagesForCommunity(communityId)
        db.postDao().clearCommunityFromPosts(communityId)
        db.membershipDao().deleteAllForCommunity(communityId)
        db.communityDao().deleteCommunityById(communityId)
        affectedUserIds.distinct().forEach { uid ->
            val newCount = db.membershipDao().countMemberships(uid)
            db.userDao().updateCommunitiesCount(uid, newCount)
        }
        remoteSync?.deleteCommunity(communityId)
        return Result.success(Unit)
    }

    suspend fun toggleJoinCommunity(communityId: String, userId: String) {
        val existing = db.membershipDao().getMembership(communityId, userId)
        val now = System.currentTimeMillis()
        if (existing == null) {
            db.membershipDao().upsertMembership(
                MembershipEntity(communityId, userId, CommunityRole.MEMBER.name, now)
            )
            db.communityDao().adjustMemberCount(communityId, 1)
            remoteSync?.joinCommunity(communityId, userId)
        } else if (existing.role != CommunityRole.ADMIN.name) {
            db.membershipDao().deleteMembership(communityId, userId)
            db.voiceRoomDao().removeParticipant(communityId, userId)
            syncActiveVoiceCount(communityId)
            db.communityDao().adjustMemberCount(communityId, -1)
            remoteSync?.leaveCommunity(communityId, userId)
        }
        val count = db.membershipDao().countMemberships(userId)
        db.userDao().updateCommunitiesCount(userId, count)
    }

    // --- VOICE ROOM & ROLES ---

    suspend fun joinVoiceRoom(communityId: String, userId: String) {
        if (isUserBannedOrMissing(userId)) return
        val now = System.currentTimeMillis()
        if (db.membershipDao().getMembership(communityId, userId) == null) {
            db.membershipDao().upsertMembership(
                MembershipEntity(communityId, userId, CommunityRole.MEMBER.name, now)
            )
            db.communityDao().adjustMemberCount(communityId, 1)
            val count = db.membershipDao().countMemberships(userId)
            db.userDao().updateCommunitiesCount(userId, count)
            remoteSync?.joinCommunity(communityId, userId)
        }
        val user = db.userDao().getUserById(userId)
        val role = getRole(communityId, userId)
        if (db.voiceRoomDao().getParticipant(communityId, userId) == null) {
            db.voiceRoomDao().upsertParticipant(
                VoiceParticipantEntity(
                    id = "${communityId}_$userId",
                    communityId = communityId,
                    userId = userId,
                    isMuted = true,
                    isSpeaking = false,
                    isHandRaised = false
                )
            )
            syncActiveVoiceCount(communityId)
            db.voiceRoomDao().insertEvent(
                RoomEventEntity(
                    communityId = communityId,
                    actorName = user?.username ?: "Miembro",
                    description = "entró a la sala de voz",
                    timestampMillis = now
                )
            )
            remoteSync?.createRoomEvent(communityId, userId, user?.username ?: "Miembro", "entró a la sala de voz")
        }
        if (user != null) {
            remoteSync?.upsertVoiceParticipant(
                communityId = communityId,
                uid = userId,
                username = user.username,
                presetAvatarKey = user.presetAvatarKey,
                avatarFrameId = user.avatarFrameId,
                appRankId = user.appRankId,
                role = role.name,
                muted = true,
                speaking = false,
                handRaised = false
            )
        }
    }

    suspend fun leaveVoiceRoom(communityId: String, userId: String) {
        db.voiceRoomDao().removeParticipant(communityId, userId)
        syncActiveVoiceCount(communityId)
        val user = db.userDao().getUserById(userId)
        db.voiceRoomDao().insertEvent(
            RoomEventEntity(
                communityId = communityId,
                actorName = user?.username ?: "Miembro",
                description = "salió de la sala de voz",
                timestampMillis = System.currentTimeMillis()
            )
        )
        remoteSync?.removeVoiceParticipant(communityId, userId)
    }

    suspend fun setSelfMuted(communityId: String, userId: String, muted: Boolean) {
        db.voiceRoomDao().setMuted(communityId, userId, muted)
        val vp = db.voiceRoomDao().getParticipant(communityId, userId)
        val user = db.userDao().getUserById(userId) ?: return
        val role = getRole(communityId, userId)
        remoteSync?.upsertVoiceParticipant(
            communityId = communityId,
            uid = userId,
            username = user.username,
            presetAvatarKey = user.presetAvatarKey,
            avatarFrameId = user.avatarFrameId,
            appRankId = user.appRankId,
            role = role.name,
            muted = muted,
            speaking = !muted,
            handRaised = vp?.isHandRaised ?: false
        )
    }

    suspend fun setSelfSpeaking(communityId: String, userId: String, speaking: Boolean) {
        db.voiceRoomDao().setSpeaking(communityId, userId, speaking)
    }

    suspend fun toggleHandRaised(communityId: String, userId: String) {
        val current = db.voiceRoomDao().getParticipant(communityId, userId) ?: return
        val next = !current.isHandRaised
        db.voiceRoomDao().setHandRaised(communityId, userId, next)
        val user = db.userDao().getUserById(userId)
        if (next) {
            db.voiceRoomDao().insertEvent(
                RoomEventEntity(
                    communityId = communityId,
                    actorName = user?.username ?: "Miembro",
                    description = "levantó la mano para pedir turno",
                    timestampMillis = System.currentTimeMillis()
                )
            )
        }
        if (user != null) {
            val role = getRole(communityId, userId)
            remoteSync?.upsertVoiceParticipant(
                communityId = communityId,
                uid = userId,
                username = user.username,
                presetAvatarKey = user.presetAvatarKey,
                avatarFrameId = user.avatarFrameId,
                appRankId = user.appRankId,
                role = role.name,
                muted = current.isMuted,
                speaking = current.isSpeaking,
                handRaised = next
            )
        }
    }

    suspend fun moderateMuteParticipant(
        communityId: String,
        actorId: String,
        targetUserId: String,
        mute: Boolean
    ): Result<Unit> {
        val actorRole = getRole(communityId, actorId)
        val targetRole = getRole(communityId, targetUserId)
        if (actorRole.priority <= CommunityRole.MEMBER.priority || actorRole.priority <= targetRole.priority) {
            return Result.failure(IllegalStateException("Sin permisos suficientes para silenciar a este usuario."))
        }
        db.voiceRoomDao().setMuted(communityId, targetUserId, mute)
        val actor = db.userDao().getUserById(actorId)
        val target = db.userDao().getUserById(targetUserId)
        db.voiceRoomDao().insertEvent(
            RoomEventEntity(
                communityId = communityId,
                actorName = actor?.username ?: "Moderador",
                description = if (mute) "silenció a ${target?.username}" else "habilitó el micro de ${target?.username}",
                timestampMillis = System.currentTimeMillis()
            )
        )
        return Result.success(Unit)
    }

    suspend fun kickParticipant(
        communityId: String,
        actorId: String,
        targetUserId: String
    ): Result<Unit> {
        val actorRole = getRole(communityId, actorId)
        if (actorRole != CommunityRole.ADMIN) {
            return Result.failure(IllegalStateException("Solo el Admin puede expulsar participantes de la sala."))
        }
        db.voiceRoomDao().removeParticipant(communityId, targetUserId)
        remoteSync?.removeVoiceParticipant(communityId, targetUserId)
        syncActiveVoiceCount(communityId)
        val target = db.userDao().getUserById(targetUserId)
        db.voiceRoomDao().insertEvent(
            RoomEventEntity(
                communityId = communityId,
                actorName = target?.username ?: "Usuario",
                description = "fue expulsado de la sala por el Admin",
                timestampMillis = System.currentTimeMillis()
            )
        )
        return Result.success(Unit)
    }

    suspend fun changeParticipantRole(
        communityId: String,
        actorId: String,
        targetUserId: String,
        newRole: CommunityRole
    ): Result<Unit> {
        val actorRole = getRole(communityId, actorId)
        if (actorRole != CommunityRole.ADMIN) {
            return Result.failure(IllegalStateException("Solo el Admin tiene control total para asignar Co-Admins."))
        }
        if (newRole == CommunityRole.ADMIN) {
            return Result.failure(IllegalStateException("Solo puede haber un Admin principal por sala."))
        }
        db.membershipDao().updateRole(communityId, targetUserId, newRole.name)
        val actor = db.userDao().getUserById(actorId)
        val target = db.userDao().getUserById(targetUserId)
        val actionText = if (newRole == CommunityRole.CO_ADMIN) {
            "promovió a ${target?.username} a Co-Admin"
        } else {
            "cambió el rol de ${target?.username} a Miembro"
        }
        db.voiceRoomDao().insertEvent(
            RoomEventEntity(
                communityId = communityId,
                actorName = actor?.username ?: "Admin",
                description = actionText,
                timestampMillis = System.currentTimeMillis()
            )
        )
        return Result.success(Unit)
    }

    suspend fun toggleRoomLock(communityId: String, actorId: String, lock: Boolean): Result<Unit> {
        if (getRole(communityId, actorId) != CommunityRole.ADMIN) {
            return Result.failure(IllegalStateException("Solo el Admin puede bloquear o desbloquear la sala."))
        }
        db.communityDao().setVoiceLocked(communityId, lock)
        val actor = db.userDao().getUserById(actorId)
        db.voiceRoomDao().insertEvent(
            RoomEventEntity(
                communityId = communityId,
                actorName = actor?.username ?: "Admin",
                description = if (lock) "bloqueó la sala de voz" else "desbloqueó la sala de voz",
                timestampMillis = System.currentTimeMillis()
            )
        )
        return Result.success(Unit)
    }

    suspend fun clearRoomEvents(communityId: String, actorId: String): Result<Unit> {
        if (getRole(communityId, actorId) != CommunityRole.ADMIN) {
            return Result.failure(IllegalStateException("Solo el Admin puede limpiar el registro de eventos."))
        }
        db.voiceRoomDao().clearRoomEvents(communityId)
        return Result.success(Unit)
    }

    // --- CHAT MESSAGES ---

    suspend fun sendMessage(
        communityId: String,
        channelType: String,
        senderId: String,
        content: String,
        replyToSnippet: String? = null
    ) {
        if (isUserBannedOrMissing(senderId)) return
        val clean = content.trim()
        if (clean.isEmpty()) return
        val remoteId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val sender = db.userDao().getUserById(senderId)
        val role = getRole(communityId, senderId)
        db.chatDao().insertMessage(
            ChatMessageEntity(
                communityId = communityId,
                channelType = channelType,
                senderId = senderId,
                content = clean,
                replyToSnippet = replyToSnippet,
                reactionCount = 0,
                timestampMillis = System.currentTimeMillis(),
                remoteId = remoteId
            )
        )
        db.userDao().incrementPostsCount(senderId)
        if (sender != null) {
            remoteSync?.sendChatMessage(
                remoteId = remoteId,
                communityId = communityId,
                channelType = channelType,
                senderId = senderId,
                senderName = sender.username,
                senderAvatarKey = sender.presetAvatarKey,
                senderFrameId = sender.avatarFrameId,
                senderAppRankId = sender.appRankId,
                senderRole = role.name,
                senderStatus = sender.customStatus,
                content = clean,
                replyToSnippet = replyToSnippet
            )
        }
    }

    suspend fun sendAudioMessage(
        communityId: String,
        channelType: String,
        senderId: String,
        audioUri: String,
        durationMs: Long,
        audioDataB64: String? = null,
        replyToSnippet: String? = null
    ): Result<Unit> {
        if (isUserBannedOrMissing(senderId)) {
            return Result.failure(IllegalStateException("Tu cuenta está suspendida."))
        }
        if (audioUri.isBlank() && audioDataB64.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("El mensaje de audio está vacío."))
        }
        val remoteId = "audio_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val sender = db.userDao().getUserById(senderId)
        val role = getRole(communityId, senderId)
        db.chatDao().insertMessage(
            ChatMessageEntity(
                communityId = communityId,
                channelType = channelType,
                senderId = senderId,
                content = "Nota de voz",
                replyToSnippet = replyToSnippet,
                reactionCount = 0,
                timestampMillis = System.currentTimeMillis(),
                audioUri = audioUri,
                audioDataB64 = audioDataB64,
                audioDurationMs = durationMs.coerceAtLeast(1000L),
                remoteId = remoteId
            )
        )
        db.userDao().incrementPostsCount(senderId)
        if (sender != null) {
            remoteSync?.sendChatMessage(
                remoteId = remoteId,
                communityId = communityId,
                channelType = channelType,
                senderId = senderId,
                senderName = sender.username,
                senderAvatarKey = sender.presetAvatarKey,
                senderFrameId = sender.avatarFrameId,
                senderAppRankId = sender.appRankId,
                senderRole = role.name,
                senderStatus = sender.customStatus,
                content = "Nota de voz",
                replyToSnippet = replyToSnippet,
                audioDataB64 = audioDataB64,
                audioDurationMs = durationMs.coerceAtLeast(1000L)
            )
        }
        return Result.success(Unit)
    }

    suspend fun getMessageById(messageId: Long): ChatMessageEntity? =
        db.chatDao().getMessageById(messageId)

    suspend fun deleteMessage(communityId: String, actorId: String, message: ChatMessage): Result<Unit> {
        val actorRole = getRole(communityId, actorId)
        val canModerate = actorRole == CommunityRole.ADMIN ||
            (actorRole == CommunityRole.CO_ADMIN && message.senderRole != CommunityRole.ADMIN) ||
            message.sender.id == actorId
        if (!canModerate) {
            return Result.failure(IllegalStateException("Solo Admin o Co-Admin pueden eliminar este mensaje."))
        }
        db.chatDao().deleteMessage(message.id)
        message.remoteId?.let { rid ->
            remoteSync?.deleteChatMessage(communityId, rid)
        }
        message.audioUri?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching {
                val root = File(context.filesDir, "audio_notes").canonicalPath
                val f = File(path)
                if (f.exists() && f.canonicalPath.startsWith(root)) f.delete()
            }
        }
        if (message.sender.id != actorId) {
            val actor = db.userDao().getUserById(actorId)
            db.voiceRoomDao().insertEvent(
                RoomEventEntity(
                    communityId = communityId,
                    actorName = actor?.username ?: "Moderador",
                    description = "eliminó un mensaje por moderación",
                    timestampMillis = System.currentTimeMillis()
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun deleteMessageById(communityId: String, actorId: String, messageId: Long): Result<Unit> {
        val msgEntity = db.chatDao().getMessageById(messageId)
            ?: return Result.failure(IllegalStateException("Mensaje no encontrado."))
        val senderRole = getRole(communityId, msgEntity.senderId)
        val sender = db.userDao().getUserById(msgEntity.senderId)?.toDomain()
            ?: UserProfile(
                id = msgEntity.senderId,
                username = "Usuario",
                email = "",
                bio = "",
                customStatus = "",
                avatarFrame = AvatarFrame.NONE,
                avatarUri = null,
                presetAvatarKey = "anime",
                communitiesCount = 0,
                followersCount = 0,
                postsCount = 0
            )
        val msg = ChatMessage(
            id = msgEntity.id,
            communityId = msgEntity.communityId,
            channelType = msgEntity.channelType,
            sender = sender,
            senderRole = senderRole,
            content = msgEntity.content,
            replyToSnippet = msgEntity.replyToSnippet,
            reactionCount = msgEntity.reactionCount,
            timestampMillis = msgEntity.timestampMillis,
            audioUri = msgEntity.audioUri,
            audioDataB64 = msgEntity.audioDataB64,
            audioDurationMs = msgEntity.audioDurationMs,
            remoteId = msgEntity.remoteId
        )
        return deleteMessage(communityId, actorId, msg)
    }

    suspend fun reactToMessage(messageId: Long) {
        val msg = db.chatDao().getMessageById(messageId)
        db.chatDao().incrementReaction(messageId)
        if (msg?.remoteId != null) {
            remoteSync?.reactChatMessage(msg.communityId, msg.remoteId)
        }
    }

    // --- DRIFTING BOTTLE ---

    suspend fun launchDriftingBottle(
        authorId: String,
        moodTag: String,
        content: String
    ): Result<Unit> {
        if (isUserBannedOrMissing(authorId)) {
            return Result.failure(IllegalStateException("Tu cuenta está suspendida."))
        }
        val clean = content.trim()
        if (clean.length < 4) {
            return Result.failure(IllegalArgumentException("Escribe un mensaje de al menos 4 caracteres."))
        }
        val now = System.currentTimeMillis()
        val aliases = listOf(
            "Cometa Neón",
            "Sombra Violeta",
            "Pulso Estelar",
            "Eco Lunar",
            "Brisa Cyber",
            "Nébula Rosa"
        )
        val alias = "${aliases.random()} #${(100..999).random()}"
        val bottleId = "bottle_${UUID.randomUUID().toString().take(8)}"
        val expiresAt = now + 24 * 3_600_000L
        db.driftingBottleDao().purgeExpired(now)
        db.driftingBottleDao().insertBottle(
            DriftingBottleEntity(
                id = bottleId,
                authorId = authorId,
                anonymousAlias = alias,
                moodTag = moodTag.ifBlank { "Anónimo" },
                content = clean.take(280),
                replyContent = null,
                isPickedByMe = false,
                createdAtMillis = now,
                expiresAtMillis = expiresAt
            )
        )
        db.userDao().incrementPostsCount(authorId)
        remoteSync?.launchDriftingBottle(bottleId, authorId, alias, moodTag.ifBlank { "Anónimo" }, clean.take(280), expiresAt)
        return Result.success(Unit)
    }

    suspend fun pickRandomDriftingBottle(userId: String): Result<DriftingBottle> {
        val now = System.currentTimeMillis()
        db.driftingBottleDao().purgeExpired(now)
        val picked = db.driftingBottleDao().pickRandomBottle(now, userId)
            ?: db.driftingBottleDao().pickAnyActiveBottle(now)
            ?: return Result.failure(IllegalStateException("El océano neón está tranquilo. ¡Lanza la primera botella!"))

        db.driftingBottleDao().markPickedWithReply(picked.id, picked.replyContent)
        db.notificationDao().insertNotification(
            NotificationEntity(
                userId = userId,
                actorName = picked.anonymousAlias,
                actorAvatarKey = "music",
                actorAvatarUri = null,
                actorFrameId = AvatarFrame.STARLIGHT_HALO.id,
                title = "Recogiste una Drifting Bottle",
                subtitle = "\"${picked.content.take(55)}...\"",
                category = NotificationCategory.DRIFTING_BOTTLE.name,
                targetCommunityId = null,
                isRead = false,
                timestampMillis = now
            )
        )
        return Result.success(picked.copy(isPickedByMe = true).toDomain())
    }

    suspend fun replyToBottle(bottleId: String, reply: String) {
        val clean = reply.trim()
        if (clean.isEmpty()) return
        db.driftingBottleDao().markPickedWithReply(bottleId, clean.take(200))
        remoteSync?.replyBottle(bottleId, clean.take(200))
    }

    // --- COMMUNITY POSTS ---

    suspend fun createPost(
        authorId: String,
        communityId: String?,
        content: String,
        imageUri: String?
    ): Result<CommunityPost> {
        if (isUserBannedOrMissing(authorId)) {
            return Result.failure(IllegalStateException("Tu cuenta está suspendida."))
        }
        val clean = content.trim()
        if (clean.isEmpty() && imageUri.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Escribe un mensaje o agrega una foto para publicar."))
        }
        val author = db.userDao().getUserById(authorId)?.toDomain()
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        val communityName = if (!communityId.isNullOrBlank()) {
            db.communityDao().getCommunityById(communityId)?.name
        } else null
        val now = System.currentTimeMillis()
        val entity = PostEntity(
            id = "post_${UUID.randomUUID().toString().take(8)}",
            authorId = authorId,
            communityId = communityId?.takeIf { it.isNotBlank() },
            content = clean.take(500),
            imageUri = imageUri,
            likesCount = 0,
            createdAtMillis = now
        )
        db.postDao().insertPost(entity)
        db.userDao().incrementPostsCount(authorId)
        remoteSync?.createPost(
            postId = entity.id,
            authorId = authorId,
            authorName = author.username,
            authorAvatarKey = author.presetAvatarKey,
            authorFrameId = author.avatarFrameId,
            authorAppRankId = author.appRank.id,
            communityId = entity.communityId,
            communityName = communityName,
            content = entity.content
        )
        return Result.success(
            CommunityPost(
                id = entity.id,
                author = author,
                communityId = entity.communityId,
                communityName = communityName,
                content = entity.content,
                imageUri = entity.imageUri,
                likesCount = 0,
                commentsCount = 0,
                createdAtMillis = now
            )
        )
    }

    suspend fun likePost(postId: String) {
        db.postDao().incrementLikes(postId)
        remoteSync?.likePost(postId)
    }

    suspend fun addPostComment(postId: String, authorId: String, content: String): Result<Unit> {
        val clean = content.trim()
        if (clean.isEmpty()) {
            return Result.failure(IllegalArgumentException("Escribe un comentario."))
        }
        val post = db.postDao().getPostById(postId)
            ?: return Result.failure(IllegalStateException("Publicación no encontrada."))
        val actor = db.userDao().getUserById(authorId)
            ?: return Result.failure(IllegalStateException("Usuario no encontrado."))
        val now = System.currentTimeMillis()
        db.postDao().insertComment(
            PostCommentEntity(
                postId = postId,
                authorId = authorId,
                content = clean.take(280),
                createdAtMillis = now
            )
        )
        if (post.authorId != authorId) {
            db.notificationDao().insertNotification(
                NotificationEntity(
                    userId = post.authorId,
                    actorName = actor.username,
                    actorAvatarKey = actor.presetAvatarKey,
                    actorAvatarUri = actor.avatarUri,
                    actorFrameId = actor.avatarFrameId,
                    title = "${actor.username} comentó tu publicación",
                    subtitle = clean.take(60),
                    category = NotificationCategory.MENTION.name,
                    targetCommunityId = post.communityId,
                    isRead = false,
                    timestampMillis = now
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun deletePost(postId: String, actorId: String): Result<Unit> {
        val post = db.postDao().getPostById(postId)
            ?: return Result.failure(IllegalStateException("Publicación no encontrada."))
        val actor = db.userDao().getUserById(actorId)
        val isOwner = actor != null && isEmailVerifiedOwner(actor.email, actor.emailVerified)
        if (post.authorId != actorId && !isOwner) {
            return Result.failure(IllegalStateException("Solo el autor o el Dueño pueden eliminar esta publicación."))
        }
        db.postDao().deleteCommentsForPost(postId)
        db.postDao().deletePost(postId)
        remoteSync?.deletePost(postId)
        return Result.success(Unit)
    }

    // --- NOTIFICATIONS & SETTINGS ---

    suspend fun markNotificationRead(id: Long) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsRead(userId: String) {
        db.notificationDao().markAllAsRead(userId)
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        prefs.updateSettings(transform)
    }

    private suspend fun syncActiveVoiceCount(communityId: String) {
        val count = db.voiceRoomDao().countParticipants(communityId)
        db.communityDao().updateActiveVoiceCount(communityId, count)
    }

    private suspend fun getRole(communityId: String, userId: String): CommunityRole {
        val m = db.membershipDao().getMembership(communityId, userId)
        return m?.let { CommunityRole.fromRaw(it.role) } ?: CommunityRole.MEMBER
    }

    private suspend fun isUserBannedOrMissing(userId: String): Boolean {
        val user = db.userDao().getUserById(userId) ?: return true
        return user.isBanned
    }

    private fun isEmailVerifiedOwner(email: String, emailVerified: Boolean): Boolean =
        email.equals(PRIMARY_OWNER_EMAIL, ignoreCase = true) && emailVerified

    private fun parseInventory(json: String): Map<String, Int> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, Int>()
            obj.keys().forEach { key ->
                val count = obj.optInt(key, 0)
                if (count > 0) result[key] = count
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun serializeInventory(map: Map<String, Int>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> if (v > 0) obj.put(k, v) }
        return obj.toString()
    }

    private fun UserEntity.toDomain(): UserProfile {
        val isVerifiedOwner = isEmailVerifiedOwner(email, emailVerified)
        val parsedRank = AppRank.fromId(appRankId)
        val safeRank = if (!isVerifiedOwner && parsedRank == AppRank.OWNER) AppRank.NONE else parsedRank
        return UserProfile(
            id = id,
            username = username,
            email = email,
            emailVerified = emailVerified,
            bio = bio,
            customStatus = customStatus,
            avatarFrame = if (avatarFrameId.startsWith("custom_frame_")) AvatarFrame.NEON_PULSE else AvatarFrame.fromId(avatarFrameId),
            avatarUri = avatarUri,
            presetAvatarKey = presetAvatarKey,
            communitiesCount = communitiesCount,
            followersCount = followersCount,
            postsCount = postsCount,
            coinsCount = if (isVerifiedOwner) 999_999_999 else coinsCount,
            giftsReceivedCount = giftsReceivedCount,
            donorExp = donorExp,
            donorLevel = DonorTier.forExp(donorExp).level,
            totalCoinsDonated = totalCoinsDonated,
            dailyStreakDay = dailyStreakDay,
            lastDailyCheckInAtMillis = lastDailyCheckInAtMillis,
            followHostsProgress = followHostsProgress,
            watchLiveMinutesProgress = watchLiveMinutesProgress,
            gameCoinsEarnedProgress = gameCoinsEarnedProgress,
            giftCoinsSentProgress = giftCoinsSentProgress,
            claimedTaskIds = claimedTaskIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
            inventoryMap = parseInventory(inventoryJson),
            mePlusMember = mePlusMember,
            lastMePlusDrawAtMillis = lastMePlusDrawAtMillis,
            maxFrameSlots = if (isVerifiedOwner) 120 else maxFrameSlots.coerceIn(10, 120),
            creatorSalesCount = creatorSalesCount,
            creatorEarningsCoins = creatorEarningsCoins,
            favoriteFrameIds = favoriteFrameIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
            appRank = safeRank,
            avatarFrameId = avatarFrameId,
            isDeveloper = isVerifiedOwner,
            isBanned = isBanned,
            banReason = banReason
        )
    }

    private fun CommunityEntity.toDomain(myRole: CommunityRole?, isJoined: Boolean) = Community(
        id = id,
        name = name,
        category = category,
        description = description,
        coverKey = coverKey,
        badgeLabel = badgeLabel,
        memberCount = memberCount,
        activeVoiceCount = activeVoiceCount,
        isVoiceLocked = isVoiceLocked,
        myRole = myRole,
        isJoined = isJoined,
        backgroundUri = backgroundUri
    )

    private fun CustomFrameEntity.toDomain() = CustomFrame(
        id = id,
        creatorId = creatorId,
        creatorName = creatorName,
        title = title,
        frameType = frameType,
        pngPath = pngPath,
        primaryColorHex = primaryColorHex,
        secondaryColorHex = secondaryColorHex,
        stylePattern = CustomFrameStyle.fromId(stylePatternId),
        thicknessDp = thicknessDp,
        hasSparkles = hasSparkles,
        priceCoins = priceCoins,
        description = description,
        category = category,
        isFeatured = isFeatured,
        salesCount = salesCount,
        favoriteCount = favoriteCount,
        createdAtMillis = createdAtMillis
    )

    private fun DriftingBottleEntity.toDomain() = DriftingBottle(
        id = id,
        authorId = authorId,
        anonymousAlias = anonymousAlias,
        moodTag = moodTag,
        content = content,
        replyContent = replyContent,
        isPickedByMe = isPickedByMe,
        createdAtMillis = createdAtMillis,
        expiresAtMillis = expiresAtMillis
    )

    suspend fun getUserById(userId: String): UserProfile? =
        db.userDao().getUserById(userId)?.toDomain()

    suspend fun getCommunityById(communityId: String): CommunityEntity? =
        db.communityDao().getCommunityById(communityId)

    suspend fun getWebSnapshot(userId: String?, communityId: String?): WebSnapshot {
        val allUsers = observeAllUsersList().first()
        val activeUser = if (!userId.isNullOrBlank()) allUsers.firstOrNull { it.id == userId } else null
        val effectiveUid = activeUser?.id ?: ""
        val communities = observeCommunities(effectiveUid).first()
        val bottles = observeDriftingBottles().first()
        val notifications = if (effectiveUid.isNotEmpty()) observeNotifications(effectiveUid).first() else emptyList()
        val customFrames = observeCustomFrames().first()
        val posts = observePosts().first()
        val usersMap = allUsers.associateBy { it.id }
        val postComments = db.postDao().observeAllComments().first().mapNotNull { c ->
            val author = usersMap[c.authorId] ?: return@mapNotNull null
            PostComment(c.id, c.postId, author, c.content, c.createdAtMillis)
        }
        val selectedCommunity = if (!communityId.isNullOrBlank()) communities.firstOrNull { it.id == communityId } else null
        val participants = if (selectedCommunity != null) observeVoiceParticipants(selectedCommunity.id).first() else emptyList()
        val events = if (selectedCommunity != null) observeRoomEvents(selectedCommunity.id).first() else emptyList()
        val voiceMessages = if (selectedCommunity != null) observeMessages(selectedCommunity.id, "VOICE").first() else emptyList()
        val groupMessages = if (selectedCommunity != null) observeMessages(selectedCommunity.id, "GROUP").first() else emptyList()
        return WebSnapshot(
            hasAnyUser = allUsers.isNotEmpty(),
            user = activeUser,
            allUsers = allUsers,
            communities = communities,
            bottles = bottles,
            notifications = notifications,
            customFrames = customFrames,
            posts = posts,
            postComments = postComments,
            selectedCommunity = selectedCommunity,
            participants = participants,
            events = events,
            voiceMessages = voiceMessages,
            groupMessages = groupMessages
        )
    }
}

data class WebSnapshot(
    val hasAnyUser: Boolean,
    val user: UserProfile?,
    val allUsers: List<UserProfile>,
    val communities: List<Community>,
    val bottles: List<DriftingBottle>,
    val notifications: List<AppNotification>,
    val customFrames: List<CustomFrame>,
    val posts: List<CommunityPost>,
    val postComments: List<PostComment>,
    val selectedCommunity: Community?,
    val participants: List<VoiceParticipant>,
    val events: List<RoomEventLog>,
    val voiceMessages: List<ChatMessage>,
    val groupMessages: List<ChatMessage>
)
