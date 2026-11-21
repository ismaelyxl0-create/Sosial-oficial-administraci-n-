package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class FsPrivateUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val emailVerified: Boolean = false,
    val bio: String = "",
    val customStatus: String = "",
    val avatarFrameId: String = "neon_pulse",
    val presetAvatarKey: String = "anime",
    val avatarUrl: String? = null,
    val communitiesCount: Int = 0,
    val followersCount: Int = 0,
    val postsCount: Int = 0,
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
    val favoriteFrameIds: String = "",
    val appRankId: String = "none",
    val banned: Boolean = false,
    val banReason: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsPublicUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val discoverable: Boolean = true,
    val bio: String = "",
    val customStatus: String = "",
    val avatarFrameId: String = "neon_pulse",
    val presetAvatarKey: String = "anime",
    val avatarUrl: String? = null,
    val communitiesCount: Int = 0,
    val followersCount: Int = 0,
    val postsCount: Int = 0,
    val giftsReceivedCount: Int = 0,
    val donorExp: Int = 0,
    val donorLevel: Int = 1,
    val appRankId: String = "none",
    val banned: Boolean = false,
    val banReason: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsCommunity(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val coverKey: String = "irl",
    val badgeLabel: String = "nueva",
    val visibility: String = "public",
    val memberCount: Int = 1,
    val activeVoiceCount: Int = 0,
    val voiceLocked: Boolean = false,
    val backgroundUri: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsMembership(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val userId: String = "",
    val role: String = "MEMBER",
    val roomVisibility: String = "public",
    val joinedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsVoiceParticipant(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val userId: String = "",
    val username: String = "",
    val presetAvatarKey: String = "anime",
    val avatarFrameId: String = "neon_pulse",
    val avatarUrl: String? = null,
    val appRankId: String = "none",
    val role: String = "MEMBER",
    val roomVisibility: String = "public",
    val muted: Boolean = true,
    val speaking: Boolean = false,
    val handRaised: Boolean = false,
    val joinedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsVoiceFrame(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val senderId: String = "",
    val roomVisibility: String = "public",
    val seq: Long = 0L,
    val sampleRate: Int = 8000,
    val codec: String = "ULAW",
    val payloadB64: String = "",
    val rms: Double = 0.0,
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsRoomGift(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val channelType: String = "VOICE",
    val roomVisibility: String = "public",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarKey: String = "anime",
    val senderFrameId: String = "neon_pulse",
    val recipientId: String = "",
    val recipientName: String = "",
    val giftId: String = "rose",
    val giftName: String = "Rose",
    val giftEmoji: String = "🌹",
    val coinCost: Int = 1,
    val comboCount: Int = 1,
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsChatMessage(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val channelType: String = "GROUP",
    val roomVisibility: String = "public",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarKey: String = "anime",
    val senderFrameId: String = "neon_pulse",
    val senderAvatarUrl: String? = null,
    val senderAppRankId: String = "none",
    val senderRole: String = "MEMBER",
    val senderStatus: String? = null,
    val content: String = "",
    val replyToSnippet: String? = null,
    val reactionCount: Int = 0,
    val audioUri: String? = null,
    val audioDataB64: String? = null,
    val audioDurationMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsRoomEvent(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val description: String = "",
    val roomVisibility: String = "public",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarKey: String = "anime",
    val authorFrameId: String = "neon_pulse",
    val authorAvatarUrl: String? = null,
    val authorAppRankId: String = "none",
    val communityId: String? = null,
    val communityName: String? = null,
    val visibility: String = "public",
    val content: String = "",
    val imageUri: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsPostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarKey: String = "anime",
    val authorFrameId: String = "neon_pulse",
    val authorAppRankId: String = "none",
    val visibility: String = "public",
    val content: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsCustomFrame(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val frameType: String = "STUDIO",
    val pngPath: String? = null,
    val primaryColorHex: Long = 0xFF9D4EDD,
    val secondaryColorHex: Long = 0xFFFF4EC2,
    val stylePatternId: String = "ring_glow",
    val thicknessDp: Double = 4.0,
    val sparkles: Boolean = true,
    val priceCoins: Int = 0,
    val description: String = "",
    val category: String = "Marco de perfil",
    val isFeatured: Boolean = false,
    val salesCount: Int = 0,
    val favoriteCount: Int = 0,
    val visibility: String = "public",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsDriftingBottle(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val anonymousAlias: String = "",
    val moodTag: String = "",
    val content: String = "",
    val replyContent: String? = null,
    val visibility: String = "public",
    val expiresAtMillis: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorAvatarKey: String = "anime",
    val actorFrameId: String = "neon_pulse",
    val title: String = "",
    val subtitle: String = "",
    val category: String = "COMMUNITY",
    val targetCommunityId: String? = null,
    val read: Boolean = false,
    val createdAt: Timestamp? = null
)
