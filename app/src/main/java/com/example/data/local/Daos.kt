package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:identifier) OR LOWER(username) = LOWER(:identifier) LIMIT 1")
    suspend fun findByEmailOrUsername(identifier: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET bio = :bio, customStatus = :status, avatarFrameId = :frameId, avatarUri = :avatarUri, presetAvatarKey = :presetAvatarKey, username = :username WHERE id = :userId")
    suspend fun updateProfileCustomization(
        userId: String,
        username: String,
        bio: String,
        status: String,
        frameId: String,
        avatarUri: String?,
        presetAvatarKey: String
    )

    @Query("UPDATE users SET postsCount = postsCount + 1 WHERE id = :userId")
    suspend fun incrementPostsCount(userId: String)

    @Query("UPDATE users SET followersCount = followersCount + 1 WHERE id = :userId")
    suspend fun incrementFollowersCount(userId: String)

    @Query("UPDATE users SET communitiesCount = :count WHERE id = :userId")
    suspend fun updateCommunitiesCount(userId: String, count: Int)

    @Query("UPDATE users SET coinsCount = :coins WHERE id = :userId")
    suspend fun updateCoins(userId: String, coins: Int)

    @Query("UPDATE users SET coinsCount = coinsCount + :delta WHERE id = :userId")
    suspend fun incrementCoins(userId: String, delta: Int)

    @Query("UPDATE users SET giftsReceivedCount = giftsReceivedCount + 1 WHERE id = :userId")
    suspend fun incrementGiftsReceived(userId: String)

    @Query("UPDATE users SET coinsCount = :coins, donorExp = :exp, donorLevel = :level, totalCoinsDonated = :totalDonated, giftCoinsSentProgress = :giftCoinsSentProgress, inventoryJson = :inventoryJson WHERE id = :userId")
    suspend fun updateDonorAndBagProgress(
        userId: String,
        coins: Int,
        exp: Int,
        level: Int,
        totalDonated: Int,
        giftCoinsSentProgress: Int,
        inventoryJson: String
    )

    @Query("UPDATE users SET coinsCount = :coins, donorExp = :exp, donorLevel = :level, dailyStreakDay = :streakDay, lastDailyCheckInAtMillis = :checkInMillis, followHostsProgress = :followProg, watchLiveMinutesProgress = :watchProg, gameCoinsEarnedProgress = :gameProg, giftCoinsSentProgress = :giftProg, claimedTaskIds = :claimedIds, inventoryJson = :inventoryJson, mePlusMember = :mePlus, lastMePlusDrawAtMillis = :lastDrawMillis WHERE id = :userId")
    suspend fun updateRewardsProgression(
        userId: String,
        coins: Int,
        exp: Int,
        level: Int,
        streakDay: Int,
        checkInMillis: Long,
        followProg: Int,
        watchProg: Int,
        gameProg: Int,
        giftProg: Int,
        claimedIds: String,
        inventoryJson: String,
        mePlus: Boolean,
        lastDrawMillis: Long
    )

    @Query("UPDATE users SET appRankId = :rankId WHERE id = :userId")
    suspend fun updateAppRank(userId: String, rankId: String)

    @Query("UPDATE users SET isDeveloper = :isDeveloper WHERE id = :userId")
    suspend fun updateIsDeveloper(userId: String, isDeveloper: Boolean)

    @Query("SELECT COUNT(*) FROM users WHERE isDeveloper = 1")
    suspend fun countDevelopers(): Int

    @Query("SELECT * FROM users ORDER BY rowid ASC LIMIT 1")
    suspend fun getFirstUser(): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Query("UPDATE users SET isBanned = :isBanned, banReason = :banReason WHERE id = :userId")
    suspend fun updateBanStatus(userId: String, isBanned: Boolean, banReason: String?)

    @Query("UPDATE users SET coinsCount = :coins, maxFrameSlots = :maxSlots WHERE id = :userId")
    suspend fun updateCoinsAndMaxSlots(userId: String, coins: Int, maxSlots: Int)

    @Query("UPDATE users SET favoriteFrameIdsCsv = :favoritesCsv WHERE id = :userId")
    suspend fun updateFavoriteFrames(userId: String, favoritesCsv: String)

    @Query("UPDATE users SET coinsCount = coinsCount + :coinsDelta, creatorSalesCount = creatorSalesCount + 1, creatorEarningsCoins = creatorEarningsCoins + :earningsDelta WHERE id = :userId")
    suspend fun recordCreatorSale(userId: String, coinsDelta: Int, earningsDelta: Int)

    @Query("UPDATE users SET creatorSalesCount = creatorSalesCount + 1, creatorEarningsCoins = creatorEarningsCoins + :earningsDelta WHERE id = :userId")
    suspend fun recordOwnerCreatorSale(userId: String, earningsDelta: Int)
}

@Dao
interface CommunityDao {
    @Query("SELECT * FROM communities ORDER BY memberCount DESC")
    fun observeCommunities(): Flow<List<CommunityEntity>>

    @Query("SELECT * FROM communities WHERE id = :communityId LIMIT 1")
    fun observeCommunity(communityId: String): Flow<CommunityEntity?>

    @Query("SELECT * FROM communities WHERE id = :communityId LIMIT 1")
    suspend fun getCommunityById(communityId: String): CommunityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCommunity(community: CommunityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCommunities(communities: List<CommunityEntity>)

    @Query("UPDATE communities SET isVoiceLocked = :locked WHERE id = :communityId")
    suspend fun setVoiceLocked(communityId: String, locked: Boolean)

    @Query("UPDATE communities SET memberCount = memberCount + :delta WHERE id = :communityId")
    suspend fun adjustMemberCount(communityId: String, delta: Int)

    @Query("UPDATE communities SET activeVoiceCount = :count WHERE id = :communityId")
    suspend fun updateActiveVoiceCount(communityId: String, count: Int)

    @Query("UPDATE communities SET backgroundUri = :backgroundUri WHERE id = :communityId")
    suspend fun updateBackground(communityId: String, backgroundUri: String?)

    @Query("DELETE FROM communities WHERE id = :communityId")
    suspend fun deleteCommunityById(communityId: String)

    @Query("DELETE FROM communities WHERE id NOT IN (:activeIds)")
    suspend fun pruneMissing(activeIds: List<String>)
}

@Dao
interface MembershipDao {
    @Query("SELECT * FROM memberships WHERE userId = :userId")
    fun observeMembershipsForUser(userId: String): Flow<List<MembershipEntity>>

    @Query("SELECT * FROM memberships WHERE communityId = :communityId")
    fun observeMembershipsForCommunity(communityId: String): Flow<List<MembershipEntity>>

    @Query("SELECT * FROM memberships WHERE communityId = :communityId AND userId = :userId LIMIT 1")
    suspend fun getMembership(communityId: String, userId: String): MembershipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembership(membership: MembershipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemberships(memberships: List<MembershipEntity>)

    @Query("DELETE FROM memberships WHERE communityId = :communityId AND userId = :userId")
    suspend fun deleteMembership(communityId: String, userId: String)

    @Query("UPDATE memberships SET role = :newRole WHERE communityId = :communityId AND userId = :userId")
    suspend fun updateRole(communityId: String, userId: String, newRole: String)

    @Query("SELECT COUNT(*) FROM memberships WHERE userId = :userId")
    suspend fun countMemberships(userId: String): Int

    @Query("SELECT userId FROM memberships WHERE communityId = :communityId")
    suspend fun getUserIdsByCommunity(communityId: String): List<String>

    @Query("DELETE FROM memberships WHERE communityId = :communityId")
    suspend fun deleteAllForCommunity(communityId: String)
}

@Dao
interface VoiceRoomDao {
    @Query("SELECT * FROM voice_participants WHERE communityId = :communityId")
    fun observeParticipants(communityId: String): Flow<List<VoiceParticipantEntity>>

    @Query("SELECT * FROM voice_participants WHERE communityId = :communityId AND userId = :userId LIMIT 1")
    suspend fun getParticipant(communityId: String, userId: String): VoiceParticipantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParticipant(participant: VoiceParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParticipants(participants: List<VoiceParticipantEntity>)

    @Query("DELETE FROM voice_participants WHERE communityId = :communityId AND userId = :userId")
    suspend fun removeParticipant(communityId: String, userId: String)

    @Query("SELECT COUNT(*) FROM voice_participants WHERE communityId = :communityId")
    suspend fun countParticipants(communityId: String): Int

    @Query("UPDATE voice_participants SET isMuted = :muted, isSpeaking = CASE WHEN :muted = 1 THEN 0 ELSE isSpeaking END WHERE communityId = :communityId AND userId = :userId")
    suspend fun setMuted(communityId: String, userId: String, muted: Boolean)

    @Query("UPDATE voice_participants SET isSpeaking = :speaking WHERE communityId = :communityId AND userId = :userId")
    suspend fun setSpeaking(communityId: String, userId: String, speaking: Boolean)

    @Query("UPDATE voice_participants SET isHandRaised = :raised WHERE communityId = :communityId AND userId = :userId")
    suspend fun setHandRaised(communityId: String, userId: String, raised: Boolean)

    @Query("SELECT * FROM room_events WHERE communityId = :communityId ORDER BY timestampMillis DESC LIMIT 30")
    fun observeRoomEvents(communityId: String): Flow<List<RoomEventEntity>>

    @Insert
    suspend fun insertEvent(event: RoomEventEntity)

    @Insert
    suspend fun insertEvents(events: List<RoomEventEntity>)

    @Query("DELETE FROM room_events WHERE communityId = :communityId")
    suspend fun clearRoomEvents(communityId: String)

    @Query("DELETE FROM voice_participants WHERE communityId = :communityId")
    suspend fun deleteAllParticipantsForCommunity(communityId: String)

    @Query("SELECT DISTINCT communityId FROM voice_participants WHERE userId = :userId")
    suspend fun getCommunityIdsForParticipant(userId: String): List<String>

    @Query("DELETE FROM voice_participants WHERE userId = :userId")
    suspend fun removeParticipantFromAllRooms(userId: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE communityId = :communityId AND channelType = :channelType ORDER BY timestampMillis ASC")
    fun observeMessages(communityId: String, channelType: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Long): ChatMessageEntity?

    @Query("SELECT * FROM chat_messages WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getMessageByRemoteId(remoteId: String): ChatMessageEntity?

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET reactionCount = :reactionCount WHERE remoteId = :remoteId")
    suspend fun updateReactionByRemoteId(remoteId: String, reactionCount: Int)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)

    @Query("DELETE FROM chat_messages WHERE remoteId = :remoteId")
    suspend fun deleteMessageByRemoteId(remoteId: String)

    @Query("UPDATE chat_messages SET reactionCount = reactionCount + 1 WHERE id = :messageId")
    suspend fun incrementReaction(messageId: Long)

    @Query("DELETE FROM chat_messages WHERE communityId = :communityId")
    suspend fun deleteAllMessagesForCommunity(communityId: String)

    @Query("DELETE FROM chat_messages WHERE communityId = :communityId AND channelType = :channelType AND remoteId IS NOT NULL")
    suspend fun deleteRemoteMessages(communityId: String, channelType: String)
}

@Dao
interface DriftingBottleDao {
    @Query("SELECT * FROM drifting_bottles WHERE expiresAtMillis > :nowMillis ORDER BY createdAtMillis DESC")
    fun observeActiveBottles(nowMillis: Long): Flow<List<DriftingBottleEntity>>

    @Query("SELECT * FROM drifting_bottles WHERE expiresAtMillis > :nowMillis AND authorId != :userId ORDER BY RANDOM() LIMIT 1")
    suspend fun pickRandomBottle(nowMillis: Long, userId: String): DriftingBottleEntity?

    @Query("SELECT * FROM drifting_bottles WHERE expiresAtMillis > :nowMillis LIMIT 1")
    suspend fun pickAnyActiveBottle(nowMillis: Long): DriftingBottleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBottle(bottle: DriftingBottleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBottles(bottles: List<DriftingBottleEntity>)

    @Query("UPDATE drifting_bottles SET isPickedByMe = 1, replyContent = :reply WHERE id = :bottleId")
    suspend fun markPickedWithReply(bottleId: String, reply: String?)

    @Query("DELETE FROM drifting_bottles WHERE expiresAtMillis <= :nowMillis")
    suspend fun purgeExpired(nowMillis: Long)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestampMillis DESC")
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun observeUnreadCount(userId: String): Flow<Int>

    @Query("SELECT * FROM notifications WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): NotificationEntity?

    @Insert
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE remoteId = :remoteId")
    suspend fun markAsReadByRemoteId(remoteId: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)
}

@Dao
interface CustomFrameDao {
    @Query("SELECT * FROM custom_frames ORDER BY createdAtMillis DESC")
    fun observeAllFrames(): Flow<List<CustomFrameEntity>>

    @Query("SELECT * FROM custom_frames WHERE id = :id LIMIT 1")
    suspend fun getFrameById(id: String): CustomFrameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrame(frame: CustomFrameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrames(frames: List<CustomFrameEntity>)

    @Query("DELETE FROM custom_frames WHERE id = :id")
    suspend fun deleteFrameById(id: String)

    @Query("UPDATE custom_frames SET salesCount = salesCount + 1 WHERE id = :id")
    suspend fun incrementSalesCount(id: String)

    @Query("UPDATE custom_frames SET favoriteCount = MAX(0, favoriteCount + :delta) WHERE id = :id")
    suspend fun adjustFavoriteCount(id: String, delta: Int)

    @Query("SELECT COUNT(*) FROM custom_frames WHERE creatorId = :creatorId")
    suspend fun countByCreator(creatorId: String): Int
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY createdAtMillis DESC")
    fun observeAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("UPDATE posts SET likesCount = likesCount + 1 WHERE id = :postId")
    suspend fun incrementLikes(postId: String)

    @Query("UPDATE posts SET communityId = NULL WHERE communityId = :communityId")
    suspend fun clearCommunityFromPosts(communityId: String)

    @Query("SELECT * FROM post_comments ORDER BY createdAtMillis ASC")
    fun observeAllComments(): Flow<List<PostCommentEntity>>

    @Query("SELECT * FROM post_comments WHERE postId = :postId ORDER BY createdAtMillis ASC")
    fun observeCommentsForPost(postId: String): Flow<List<PostCommentEntity>>

    @Insert
    suspend fun insertComment(comment: PostCommentEntity)

    @Query("DELETE FROM post_comments WHERE postId = :postId")
    suspend fun deleteCommentsForPost(postId: String)
}
