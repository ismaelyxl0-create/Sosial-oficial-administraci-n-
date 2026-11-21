package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val emailVerified: Boolean = false,
    val passwordHash: String,
    val bio: String,
    val customStatus: String,
    val avatarFrameId: String,
    val avatarUri: String?,
    val presetAvatarKey: String,
    val communitiesCount: Int,
    val followersCount: Int,
    val postsCount: Int,
    val coinsCount: Int = 100,
    val giftsReceivedCount: Int = 0,
    val donorExp: Int = 0,
    val donorLevel: Int = 1,
    val totalCoinsDonated: Int = 0,
    val dailyStreakDay: Int = 0,
    val lastDailyCheckInAtMillis: Long = 0L,
    val followHostsProgress: Int = 0,
    val watchLiveMinutesProgress: Int = 0,
    val gameCoinsEarnedProgress: Int = 0,
    val giftCoinsSentProgress: Int = 0,
    val claimedTaskIds: String = "",
    val inventoryJson: String = "{}",
    val mePlusMember: Boolean = false,
    val lastMePlusDrawAtMillis: Long = 0L,
    val maxFrameSlots: Int = 10,
    val creatorSalesCount: Int = 0,
    val creatorEarningsCoins: Int = 0,
    val favoriteFrameIdsCsv: String = "",
    val appRankId: String = "none",
    val isDeveloper: Boolean = false,
    val isBanned: Boolean = false,
    val banReason: String? = null
)

@Entity(tableName = "communities")
data class CommunityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val description: String,
    val coverKey: String,
    val badgeLabel: String,
    val memberCount: Int,
    val activeVoiceCount: Int,
    val isVoiceLocked: Boolean = false,
    val backgroundUri: String? = null
)

@Entity(
    tableName = "memberships",
    primaryKeys = ["communityId", "userId"]
)
data class MembershipEntity(
    val communityId: String,
    val userId: String,
    val role: String, // ADMIN, CO_ADMIN, MEMBER
    val joinedAtMillis: Long
)

@Entity(tableName = "voice_participants")
data class VoiceParticipantEntity(
    @PrimaryKey val id: String,
    val communityId: String,
    val userId: String,
    val isMuted: Boolean,
    val isSpeaking: Boolean,
    val isHandRaised: Boolean
)

@Entity(tableName = "room_events")
data class RoomEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val communityId: String,
    val actorName: String,
    val description: String,
    val timestampMillis: Long
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val communityId: String,
    val channelType: String, // "VOICE" or "GROUP"
    val senderId: String,
    val content: String,
    val replyToSnippet: String?,
    val reactionCount: Int = 0,
    val timestampMillis: Long,
    val audioUri: String? = null,
    val audioDataB64: String? = null,
    val audioDurationMs: Long = 0L,
    val remoteId: String? = null
)

@Entity(tableName = "drifting_bottles")
data class DriftingBottleEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val anonymousAlias: String,
    val moodTag: String,
    val content: String,
    val replyContent: String?,
    val isPickedByMe: Boolean,
    val createdAtMillis: Long,
    val expiresAtMillis: Long
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val actorName: String,
    val actorAvatarKey: String,
    val actorAvatarUri: String?,
    val actorFrameId: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val targetCommunityId: String?,
    val isRead: Boolean,
    val timestampMillis: Long,
    val remoteId: String? = null
)

@Entity(tableName = "custom_frames")
data class CustomFrameEntity(
    @PrimaryKey val id: String,
    val creatorId: String,
    val creatorName: String,
    val title: String,
    val frameType: String, // "PNG" or "STUDIO"
    val pngPath: String?,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val stylePatternId: String,
    val thicknessDp: Float,
    val hasSparkles: Boolean,
    val priceCoins: Int = 0,
    val description: String = "",
    val category: String = "Marco de perfil",
    val isFeatured: Boolean = false,
    val salesCount: Int = 0,
    val favoriteCount: Int = 0,
    val createdAtMillis: Long
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val communityId: String?,
    val content: String,
    val imageUri: String?,
    val likesCount: Int = 0,
    val createdAtMillis: Long
)

@Entity(tableName = "post_comments")
data class PostCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: String,
    val authorId: String,
    val content: String,
    val createdAtMillis: Long
)
