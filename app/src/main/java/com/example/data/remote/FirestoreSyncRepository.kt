package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.TxlRepository
import com.example.model.AppRank
import com.example.model.RoomGiftEvent
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(private val context: Context) {

    companion object {
        private const val TAG = "FirestoreSyncRepo"
        private const val VOICE_RING_SLOTS = 16
    }

    val auth: FirebaseAuth by lazy { Firebase.auth }

    val firestore: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        Firebase.firestore(Firebase.app, dbId)
    }

    fun currentAuthUser(): FirebaseUser? = auth.currentUser

    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: throw IllegalStateException("No se pudo obtener el usuario de Google.")
        user.reload().await()
        val refreshed = auth.currentUser ?: user
        if (!refreshed.isEmailVerified || refreshed.email.isNullOrBlank()) {
            auth.signOut()
            throw IllegalStateException("Tu correo de Google debe estar verificado para usar NexoLive.")
        }
        refreshed
    }

    fun signOut() {
        runCatching { auth.signOut() }
    }

    suspend fun ensureRemoteUserDocuments(firebaseUser: FirebaseUser): Result<FsPrivateUser> = runCatching {
        val uid = firebaseUser.uid
        val email = firebaseUser.email.orEmpty().trim()
        val emailVerified = firebaseUser.isEmailVerified
        val isPrimaryOwner = email.equals(TxlRepository.PRIMARY_OWNER_EMAIL, ignoreCase = true) && emailVerified

        val privateRef = firestore.collection("users").document(uid)
        val publicRef = firestore.collection("users_public").document(uid)

        val existingPrivSnap = privateRef.get().await()
        val existingPriv = existingPrivSnap.toObject(FsPrivateUser::class.java)

        val defaultUsername = (firebaseUser.displayName?.trim()?.takeIf { it.length >= 2 }
            ?: email.substringBefore("@").ifBlank { "Usuario" })
            .take(28)

        val resolvedRank = if (isPrimaryOwner) {
            existingPriv?.appRankId?.takeIf { it in listOf("owner", "moderator", "influencer", "none") } ?: AppRank.OWNER.id
        } else {
            val existingRank = existingPriv?.appRankId ?: AppRank.NONE.id
            if (existingRank == AppRank.OWNER.id) AppRank.NONE.id else existingRank
        }

        if (!existingPrivSnap.exists()) {
            val privPayload = hashMapOf<String, Any?>(
                "userId" to uid,
                "username" to defaultUsername,
                "email" to email,
                "emailVerified" to emailVerified,
                "bio" to "",
                "customStatus" to "",
                "avatarFrameId" to "neon_pulse",
                "presetAvatarKey" to "anime",
                "communitiesCount" to 0,
                "followersCount" to 0,
                "postsCount" to 0,
                "coinsCount" to (if (isPrimaryOwner) 999_999_999 else 100),
                "giftsReceivedCount" to 0,
                "donorExp" to 0,
                "donorLevel" to 1,
                "totalCoinsDonated" to 0,
                "dailyStreakDay" to 0,
                "lastDailyCheckInAtMillis" to 0L,
                "followHostsProgress" to 0,
                "watchLiveMinutesProgress" to 0,
                "gameCoinsEarnedProgress" to 0,
                "giftCoinsSentProgress" to 0,
                "claimedTaskIds" to "",
                "inventoryJson" to "{}",
                "mePlusMember" to false,
                "lastMePlusDrawAtMillis" to 0L,
                "maxFrameSlots" to (if (isPrimaryOwner) 120 else 10),
                "creatorSalesCount" to 0,
                "creatorEarningsCoins" to 0,
                "favoriteFrameIds" to "",
                "appRankId" to resolvedRank,
                "banned" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            privateRef.set(privPayload).await()
        } else {
            privateRef.update(
                mapOf(
                    "emailVerified" to emailVerified,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val publicSnap = publicRef.get().await()
        if (!publicSnap.exists()) {
            val pubPayload = hashMapOf<String, Any?>(
                "userId" to uid,
                "username" to (existingPriv?.username ?: defaultUsername),
                "discoverable" to true,
                "bio" to (existingPriv?.bio ?: ""),
                "customStatus" to (existingPriv?.customStatus ?: ""),
                "avatarFrameId" to (existingPriv?.avatarFrameId ?: "neon_pulse"),
                "presetAvatarKey" to (existingPriv?.presetAvatarKey ?: "anime"),
                "communitiesCount" to (existingPriv?.communitiesCount ?: 0),
                "followersCount" to (existingPriv?.followersCount ?: 0),
                "postsCount" to (existingPriv?.postsCount ?: 0),
                "giftsReceivedCount" to (existingPriv?.giftsReceivedCount ?: 0),
                "donorExp" to (existingPriv?.donorExp ?: 0),
                "donorLevel" to (existingPriv?.donorLevel ?: 1),
                "appRankId" to resolvedRank,
                "banned" to (existingPriv?.banned ?: false),
                "banReason" to existingPriv?.banReason,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            publicRef.set(pubPayload).await()
        }

        privateRef.get().await().toObject(FsPrivateUser::class.java)
            ?: FsPrivateUser(
                id = uid,
                userId = uid,
                username = defaultUsername,
                email = email,
                emailVerified = emailVerified,
                appRankId = resolvedRank
            )
    }

    // --- REALTIME STREAMS ---

    fun observePrivateUser(uid: String): Flow<FsPrivateUser?> = callbackFlow {
        val reg = firestore.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observePrivateUser", err)
                    return@addSnapshotListener
                }
                trySend(snap?.toObject(FsPrivateUser::class.java))
            }
        awaitClose { reg.remove() }
    }

    fun observeDiscoverableUsers(): Flow<List<FsPublicUser>> = callbackFlow {
        val reg = firestore.collection("users_public")
            .whereEqualTo("discoverable", true)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observeDiscoverableUsers", err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(FsPublicUser::class.java) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observePublicCommunities(): Flow<List<FsCommunity>> = callbackFlow {
        val reg = firestore.collection("communities")
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observePublicCommunities", err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(FsCommunity::class.java) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeCommunityMembers(communityId: String): Flow<List<FsMembership>> = callbackFlow {
        val reg = firestore.collection("communities").document(communityId)
            .collection("members")
            .whereEqualTo("roomVisibility", "public")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observeCommunityMembers", err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(FsMembership::class.java) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeVoiceParticipants(communityId: String): Flow<List<FsVoiceParticipant>> = callbackFlow {
        val reg = firestore.collection("communities").document(communityId)
            .collection("voice_participants")
            .whereEqualTo("roomVisibility", "public")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observeVoiceParticipants", err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(FsVoiceParticipant::class.java) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeVoiceFrames(
        communityId: String,
        myUserId: String,
        onFrame: (senderId: String, seq: Long, payloadB64: String, rms: Float) -> Unit
    ): ListenerRegistration {
        return firestore.collection("communities").document(communityId)
            .collection("voice_frames")
            .whereEqualTo("roomVisibility", "public")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observeVoiceFrames", err)
                    return@addSnapshotListener
                }
                snap?.documentChanges?.forEach { change ->
                    val frame = change.document.toObject(FsVoiceFrame::class.java)
                    if (frame.senderId.isNotBlank() &&
                        frame.senderId != myUserId &&
                        frame.payloadB64.isNotBlank()
                    ) {
                        onFrame(frame.senderId, frame.seq, frame.payloadB64, frame.rms.toFloat())
                    }
                }
            }
    }

    fun observeRoomGifts(communityId: String): Flow<List<RoomGiftEvent>> = callbackFlow {
        val reg = firestore.collection("communities").document(communityId)
            .collection("gifts")
            .whereEqualTo("roomVisibility", "public")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observeRoomGifts", err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    val g = doc.toObject(FsRoomGift::class.java) ?: return@mapNotNull null
                    RoomGiftEvent(
                        id = doc.id,
                        communityId = g.communityId,
                        channelType = g.channelType,
                        senderId = g.senderId,
                        senderName = g.senderName,
                        senderAvatarKey = g.senderAvatarKey,
                        senderFrameId = g.senderFrameId,
                        recipientId = g.recipientId,
                        recipientName = g.recipientName,
                        giftId = g.giftId,
                        giftName = g.giftName,
                        giftEmoji = g.giftEmoji,
                        coinCost = g.coinCost,
                        comboCount = g.comboCount,
                        timestampMillis = g.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeMessages(communityId: String): Flow<List<FsChatMessage>> = callbackFlow {
        val reg = firestore.collection("communities").document(communityId)
            .collection("messages")
            .whereEqualTo("roomVisibility", "public")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observeMessages", err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(FsChatMessage::class.java) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeRoomEvents(communityId: String): Flow<List<FsRoomEvent>> = callbackFlow {
        val reg = firestore.collection("communities").document(communityId)
            .collection("events")
            .whereEqualTo("roomVisibility", "public")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observeRoomEvents", err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(FsRoomEvent::class.java) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observePublicPosts(): Flow<List<FsPost>> = callbackFlow {
        val reg = firestore.collection("posts")
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observePublicPosts", err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(FsPost::class.java) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observePostComments(postId: String): Flow<List<FsPostComment>> = callbackFlow {
        val reg = firestore.collection("posts").document(postId)
            .collection("comments")
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observePostComments", err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(FsPostComment::class.java) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observePublicCustomFrames(): Flow<List<FsCustomFrame>> = callbackFlow {
        val reg = firestore.collection("custom_frames")
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observePublicCustomFrames", err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(FsCustomFrame::class.java) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observePublicBottles(): Flow<List<FsDriftingBottle>> = callbackFlow {
        val reg = firestore.collection("drifting_bottles")
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observePublicBottles", err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(FsDriftingBottle::class.java) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeUserNotifications(uid: String): Flow<List<FsNotification>> = callbackFlow {
        val reg = firestore.collection("users").document(uid)
            .collection("notifications")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    FirestoreErrorHandler.log(TAG, "observeUserNotifications", err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(FsNotification::class.java) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    // --- MUTATIONS ---

    suspend fun updateProfile(
        uid: String,
        username: String,
        bio: String,
        customStatus: String,
        frameId: String,
        presetAvatarKey: String
    ) {
        val updates = mapOf(
            "username" to username,
            "bio" to bio,
            "customStatus" to customStatus,
            "avatarFrameId" to frameId,
            "presetAvatarKey" to presetAvatarKey,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        runCatching {
            firestore.collection("users").document(uid).update(updates).await()
            firestore.collection("users_public").document(uid).update(updates).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "updateProfile", it) }
    }

    suspend fun syncUserRewardsAndDonor(
        uid: String,
        coinsCount: Int,
        donorExp: Int,
        donorLevel: Int,
        totalCoinsDonated: Int,
        dailyStreakDay: Int,
        lastDailyCheckInAtMillis: Long,
        followHostsProgress: Int,
        watchLiveMinutesProgress: Int,
        gameCoinsEarnedProgress: Int,
        giftCoinsSentProgress: Int,
        claimedTaskIds: String,
        inventoryJson: String,
        mePlusMember: Boolean,
        lastMePlusDrawAtMillis: Long
    ) {
        runCatching {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "coinsCount" to coinsCount,
                    "donorExp" to donorExp,
                    "donorLevel" to donorLevel,
                    "totalCoinsDonated" to totalCoinsDonated,
                    "dailyStreakDay" to dailyStreakDay,
                    "lastDailyCheckInAtMillis" to lastDailyCheckInAtMillis,
                    "followHostsProgress" to followHostsProgress,
                    "watchLiveMinutesProgress" to watchLiveMinutesProgress,
                    "gameCoinsEarnedProgress" to gameCoinsEarnedProgress,
                    "giftCoinsSentProgress" to giftCoinsSentProgress,
                    "claimedTaskIds" to claimedTaskIds,
                    "inventoryJson" to inventoryJson,
                    "mePlusMember" to mePlusMember,
                    "lastMePlusDrawAtMillis" to lastMePlusDrawAtMillis,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            firestore.collection("users_public").document(uid).update(
                mapOf(
                    "donorExp" to donorExp,
                    "donorLevel" to donorLevel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "syncUserRewardsAndDonor", it) }
    }

    suspend fun assignAppRank(targetUserId: String, rankId: String): Result<Unit> = runCatching {
        val updates = mapOf(
            "appRankId" to rankId,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users_public").document(targetUserId).update(updates).await()
        runCatching {
            firestore.collection("users").document(targetUserId).update(updates).await()
        }
        Unit
    }

    suspend fun setBanStatus(targetUserId: String, banned: Boolean, reason: String?): Result<Unit> = runCatching {
        val updates = hashMapOf<String, Any?>(
            "banned" to banned,
            "banReason" to (reason ?: ""),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users_public").document(targetUserId).update(updates).await()
        runCatching {
            firestore.collection("users").document(targetUserId).update(updates).await()
        }
        Unit
    }

    suspend fun creditUserCoins(targetUserId: String, delta: Int): Result<Unit> = runCatching {
        if (delta <= 0) return@runCatching Unit
        firestore.collection("users").document(targetUserId).update(
            mapOf(
                "coinsCount" to FieldValue.increment(delta.toLong()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    suspend fun syncStoreProfileState(
        uid: String,
        coinsCount: Int,
        maxFrameSlots: Int,
        favoriteFrameIds: String
    ) {
        runCatching {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "coinsCount" to coinsCount,
                    "maxFrameSlots" to maxFrameSlots.coerceIn(10, 120),
                    "favoriteFrameIds" to favoriteFrameIds.take(1500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "syncStoreProfileState", it) }
    }

    suspend fun creditCreatorFrameSale(
        creatorId: String,
        coinsDelta: Int,
        earningsDelta: Int
    ) {
        if (coinsDelta <= 0) return
        runCatching {
            firestore.collection("users").document(creatorId).update(
                mapOf(
                    "coinsCount" to FieldValue.increment(coinsDelta.toLong()),
                    "creatorSalesCount" to FieldValue.increment(1L),
                    "creatorEarningsCoins" to FieldValue.increment(earningsDelta.toLong()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "creditCreatorFrameSale", it) }
    }

    suspend fun incrementTargetFollowers(targetUserId: String) {
        runCatching {
            firestore.collection("users_public").document(targetUserId).update(
                mapOf(
                    "followersCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "incrementTargetFollowers", it) }
    }

    suspend fun createCommunity(
        cid: String,
        creatorId: String,
        name: String,
        category: String,
        description: String,
        coverKey: String
    ): Result<Unit> = runCatching {
        val commData = mapOf(
            "creatorId" to creatorId,
            "name" to name,
            "category" to category,
            "description" to description,
            "coverKey" to coverKey,
            "badgeLabel" to "nueva",
            "visibility" to "public",
            "memberCount" to 1,
            "activeVoiceCount" to 0,
            "voiceLocked" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("communities").document(cid).set(commData).await()
        firestore.collection("communities").document(cid)
            .collection("members").document(creatorId)
            .set(
                mapOf(
                    "communityId" to cid,
                    "userId" to creatorId,
                    "role" to "ADMIN",
                    "roomVisibility" to "public",
                    "joinedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }

    suspend fun deleteCommunity(communityId: String): Result<Unit> = runCatching {
        firestore.collection("communities").document(communityId).delete().await()
        Unit
    }

    suspend fun joinCommunity(communityId: String, userId: String) {
        runCatching {
            firestore.collection("communities").document(communityId)
                .collection("members").document(userId)
                .set(
                    mapOf(
                        "communityId" to communityId,
                        "userId" to userId,
                        "role" to "MEMBER",
                        "roomVisibility" to "public",
                        "joinedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            firestore.collection("communities").document(communityId).update(
                mapOf(
                    "memberCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "joinCommunity", it) }
    }

    suspend fun leaveCommunity(communityId: String, userId: String) {
        runCatching {
            firestore.collection("communities").document(communityId)
                .collection("members").document(userId)
                .delete().await()
            firestore.collection("communities").document(communityId).update(
                mapOf(
                    "memberCount" to FieldValue.increment(-1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "leaveCommunity", it) }
    }

    suspend fun upsertVoiceParticipant(
        communityId: String,
        uid: String,
        username: String,
        presetAvatarKey: String,
        avatarFrameId: String,
        appRankId: String,
        role: String,
        muted: Boolean,
        speaking: Boolean,
        handRaised: Boolean
    ) {
        runCatching {
            val ref = firestore.collection("communities").document(communityId)
                .collection("voice_participants").document(uid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "communityId" to communityId,
                        "userId" to uid,
                        "username" to username.take(30),
                        "presetAvatarKey" to presetAvatarKey,
                        "avatarFrameId" to avatarFrameId,
                        "appRankId" to appRankId,
                        "role" to role,
                        "roomVisibility" to "public",
                        "muted" to muted,
                        "speaking" to speaking,
                        "handRaised" to handRaised,
                        "joinedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "username" to username.take(30),
                        "presetAvatarKey" to presetAvatarKey,
                        "avatarFrameId" to avatarFrameId,
                        "role" to role,
                        "muted" to muted,
                        "speaking" to speaking,
                        "handRaised" to handRaised,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }.onFailure { FirestoreErrorHandler.log(TAG, "upsertVoiceParticipant", it) }
    }

    suspend fun removeVoiceParticipant(communityId: String, uid: String) {
        runCatching {
            firestore.collection("communities").document(communityId)
                .collection("voice_participants").document(uid)
                .delete().await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "removeVoiceParticipant", it) }
    }

    fun pushVoiceFrameAsync(
        communityId: String,
        senderId: String,
        seq: Long,
        payloadB64: String,
        rms: Float
    ) {
        val slot = (seq % VOICE_RING_SLOTS).toInt().coerceAtLeast(0)
        val docId = "${senderId}_$slot"
        firestore.collection("communities").document(communityId)
            .collection("voice_frames").document(docId)
            .set(
                mapOf(
                    "communityId" to communityId,
                    "senderId" to senderId,
                    "roomVisibility" to "public",
                    "seq" to seq,
                    "sampleRate" to 8000,
                    "codec" to "ULAW",
                    "payloadB64" to payloadB64,
                    "rms" to rms.toDouble().coerceIn(0.0, 1.0),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener { e -> Log.w(TAG, "voice_frame tx: ${e.message}") }
    }

    suspend fun sendRoomGift(
        communityId: String,
        channelType: String,
        senderId: String,
        senderName: String,
        senderAvatarKey: String,
        senderFrameId: String,
        recipientId: String,
        recipientName: String,
        giftId: String,
        giftName: String,
        giftEmoji: String,
        coinCost: Int,
        comboCount: Int
    ): Result<Unit> = runCatching {
        val giftDocId = "gift_${System.currentTimeMillis()}_${senderId.take(6)}"
        firestore.collection("communities").document(communityId)
            .collection("gifts").document(giftDocId)
            .set(
                mapOf(
                    "communityId" to communityId,
                    "channelType" to channelType,
                    "roomVisibility" to "public",
                    "senderId" to senderId,
                    "senderName" to senderName.take(30),
                    "senderAvatarKey" to senderAvatarKey,
                    "senderFrameId" to senderFrameId,
                    "recipientId" to recipientId,
                    "recipientName" to recipientName.take(30),
                    "giftId" to giftId,
                    "giftName" to giftName.take(40),
                    "giftEmoji" to giftEmoji,
                    "coinCost" to coinCost,
                    "comboCount" to comboCount,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        if (recipientId != senderId) {
            runCatching {
                firestore.collection("users_public").document(recipientId).update(
                    mapOf(
                        "giftsReceivedCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        Unit
    }

    suspend fun sendChatMessage(
        remoteId: String,
        communityId: String,
        channelType: String,
        senderId: String,
        senderName: String,
        senderAvatarKey: String,
        senderFrameId: String,
        senderAppRankId: String,
        senderRole: String,
        senderStatus: String?,
        content: String,
        replyToSnippet: String?,
        audioDataB64: String? = null,
        audioDurationMs: Long = 0L
    ): Result<Unit> = runCatching {
        val data = hashMapOf<String, Any?>(
            "communityId" to communityId,
            "channelType" to channelType,
            "roomVisibility" to "public",
            "senderId" to senderId,
            "senderName" to senderName.take(30),
            "senderAvatarKey" to senderAvatarKey,
            "senderFrameId" to senderFrameId,
            "senderAppRankId" to senderAppRankId,
            "senderRole" to senderRole,
            "senderStatus" to senderStatus?.takeIf { it.isNotBlank() }?.take(80),
            "content" to content.take(1000),
            "replyToSnippet" to replyToSnippet?.takeIf { it.isNotBlank() }?.take(180),
            "reactionCount" to 0,
            "audioDataB64" to audioDataB64,
            "audioDurationMs" to audioDurationMs,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        firestore.collection("communities").document(communityId)
            .collection("messages").document(remoteId)
            .set(data).await()
        Unit
    }

    suspend fun deleteChatMessage(communityId: String, remoteId: String) {
        runCatching {
            firestore.collection("communities").document(communityId)
                .collection("messages").document(remoteId)
                .delete().await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "deleteChatMessage", it) }
    }

    suspend fun reactChatMessage(communityId: String, remoteId: String) {
        runCatching {
            firestore.collection("communities").document(communityId)
                .collection("messages").document(remoteId)
                .update(
                    mapOf(
                        "reactionCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "reactChatMessage", it) }
    }

    suspend fun createRoomEvent(communityId: String, actorId: String, actorName: String, description: String) {
        runCatching {
            val eid = "ev_${System.currentTimeMillis()}_${actorId.take(5)}"
            firestore.collection("communities").document(communityId)
                .collection("events").document(eid)
                .set(
                    mapOf(
                        "communityId" to communityId,
                        "actorId" to actorId,
                        "actorName" to actorName.take(40),
                        "description" to description.take(180),
                        "roomVisibility" to "public",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "createRoomEvent", it) }
    }

    suspend fun createPost(
        postId: String,
        authorId: String,
        authorName: String,
        authorAvatarKey: String,
        authorFrameId: String,
        authorAppRankId: String,
        communityId: String?,
        communityName: String?,
        content: String
    ) {
        runCatching {
            val data = hashMapOf<String, Any?>(
                "authorId" to authorId,
                "authorName" to authorName.take(30),
                "authorAvatarKey" to authorAvatarKey,
                "authorFrameId" to authorFrameId,
                "authorAppRankId" to authorAppRankId,
                "communityId" to communityId,
                "communityName" to communityName?.take(60),
                "visibility" to "public",
                "content" to content.take(500),
                "likesCount" to 0,
                "commentsCount" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            firestore.collection("posts").document(postId).set(data).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "createPost", it) }
    }

    suspend fun likePost(postId: String) {
        runCatching {
            firestore.collection("posts").document(postId).update(
                mapOf(
                    "likesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun deletePost(postId: String) {
        runCatching {
            firestore.collection("posts").document(postId).delete().await()
        }
    }

    suspend fun publishCustomFrame(
        frameId: String,
        creatorId: String,
        creatorName: String,
        title: String,
        frameType: String,
        pngPath: String?,
        primaryColorHex: Long,
        secondaryColorHex: Long,
        stylePatternId: String,
        thicknessDp: Float,
        sparkles: Boolean,
        priceCoins: Int,
        description: String = "",
        category: String = "Marco de perfil",
        isFeatured: Boolean = false
    ) {
        runCatching {
            val data = hashMapOf<String, Any?>(
                "creatorId" to creatorId,
                "creatorName" to creatorName.take(30),
                "title" to title.take(30),
                "frameType" to frameType,
                "pngPath" to pngPath?.take(500),
                "primaryColorHex" to primaryColorHex,
                "secondaryColorHex" to secondaryColorHex,
                "stylePatternId" to stylePatternId.take(32),
                "thicknessDp" to thicknessDp.toDouble(),
                "sparkles" to sparkles,
                "priceCoins" to priceCoins.coerceIn(0, 100_000),
                "description" to description.take(100),
                "category" to category.take(32),
                "isFeatured" to isFeatured,
                "salesCount" to 0,
                "favoriteCount" to 0,
                "visibility" to "public",
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            firestore.collection("custom_frames").document(frameId).set(data).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "publishCustomFrame", it) }
    }

    suspend fun incrementFrameSales(frameId: String) {
        runCatching {
            firestore.collection("custom_frames").document(frameId).update(
                mapOf("salesCount" to FieldValue.increment(1L))
            ).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "incrementFrameSales", it) }
    }

    suspend fun adjustFrameFavorite(frameId: String, delta: Int) {
        runCatching {
            firestore.collection("custom_frames").document(frameId).update(
                mapOf("favoriteCount" to FieldValue.increment(delta.toLong()))
            ).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "adjustFrameFavorite", it) }
    }

    suspend fun deleteCustomFrame(frameId: String) {
        runCatching {
            firestore.collection("custom_frames").document(frameId).delete().await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "deleteCustomFrame", it) }
    }

    suspend fun launchDriftingBottle(
        bottleId: String,
        authorId: String,
        anonymousAlias: String,
        moodTag: String,
        content: String,
        expiresAtMillis: Long
    ) {
        runCatching {
            firestore.collection("drifting_bottles").document(bottleId).set(
                mapOf(
                    "authorId" to authorId,
                    "anonymousAlias" to anonymousAlias,
                    "moodTag" to moodTag,
                    "content" to content,
                    "visibility" to "public",
                    "expiresAtMillis" to expiresAtMillis,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "launchDriftingBottle", it) }
    }

    suspend fun replyBottle(bottleId: String, reply: String) {
        runCatching {
            firestore.collection("drifting_bottles").document(bottleId).update(
                mapOf(
                    "replyContent" to reply,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun sendNotification(
        targetUserId: String,
        actorId: String,
        actorName: String,
        actorAvatarKey: String,
        actorFrameId: String,
        title: String,
        subtitle: String,
        category: String,
        targetCommunityId: String? = null
    ) {
        runCatching {
            val nid = "notif_${System.currentTimeMillis()}_${actorId.take(5)}"
            val data = hashMapOf<String, Any?>(
                "userId" to targetUserId,
                "actorId" to actorId,
                "actorName" to actorName.take(40),
                "actorAvatarKey" to actorAvatarKey,
                "actorFrameId" to actorFrameId,
                "title" to title.take(100),
                "subtitle" to subtitle.take(160),
                "category" to category,
                "targetCommunityId" to targetCommunityId,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            firestore.collection("users").document(targetUserId)
                .collection("notifications").document(nid)
                .set(data).await()
        }.onFailure { FirestoreErrorHandler.log(TAG, "sendNotification", it) }
    }
}
