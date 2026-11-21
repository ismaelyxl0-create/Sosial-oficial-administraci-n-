package com.example.model

enum class CommunityRole(val label: String, val priority: Int) {
    ADMIN("Admin", 3),
    CO_ADMIN("Co-Admin", 2),
    MEMBER("Miembro", 1);

    companion object {
        fun fromRaw(raw: String): CommunityRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MEMBER
    }
}

enum class AvatarFrame(
    val id: String,
    val displayName: String,
    val description: String
) {
    NONE("none", "Sin marco", "Estilo minimalista limpio"),
    NEON_PULSE("neon_pulse", "Pulso Neón", "Aro degradado #9D4EDD y #FF4EC2"),
    CYBER_PRISM("cyber_prism", "Prisma Cyber", "Segmentos futuristas cian y violeta"),
    STARLIGHT_HALO("starlight_halo", "Halo Estelar", "Órbita luminosa con destellos"),
    VOID_FLAME("void_flame", "Llama Astral", "Aura eléctrica giratoria"),
    ROYAL_CROWN("royal_crown", "Cresta Real", "Resplandor dorado y magenta");

    companion object {
        fun fromId(id: String): AvatarFrame =
            entries.firstOrNull { it.id == id } ?: NEON_PULSE
    }
}

enum class CustomFrameStyle(val id: String, val displayName: String) {
    RING_GLOW("ring_glow", "Aro Neón"),
    DOUBLE_ORBIT("double_orbit", "Doble Órbita"),
    NEON_DASH("neon_dash", "Segmentos"),
    STAR_BURST("star_burst", "Destello"),
    CROWN_SPIKES("crown_spikes", "Corona"),
    CYBER_HEX("cyber_hex", "Hexágono");

    companion object {
        fun fromId(id: String): CustomFrameStyle =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: RING_GLOW
    }
}

enum class StoreFrameCategory(
    val label: String,
    val emoji: String,
    val publishFeeCoins: Int
) {
    PROFILE_FRAME("Marco de perfil", "🖼️", 50),
    MESSAGE_FRAME("Marco de mensajes", "💬", 50),
    AURA("Aura", "✨", 50),
    BACKGROUND("Fondo", "🌌", 50),
    PACKAGE("Paquete", "📦", 150);

    companion object {
        fun fromLabel(raw: String): StoreFrameCategory =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true) }
                ?: PROFILE_FRAME
    }
}

enum class FrameSlotTier(
    val level: Int,
    val title: String,
    val emoji: String,
    val maxSlots: Int,
    val unlockCost: Int,
    val addedSlots: Int
) {
    INITIAL(0, "Inicial", "🟢", 10, 0, 10),
    LEVEL_1(1, "Nivel 1", "🟡", 20, 150, 10),
    LEVEL_2(2, "Nivel 2", "🟠", 35, 400, 15),
    LEVEL_3(3, "Nivel 3", "🔴", 55, 800, 20),
    LEVEL_4(4, "Nivel 4", "🟣", 80, 1400, 25),
    MAX(5, "Máximo", "🔵", 120, 2500, 40);

    companion object {
        fun forMaxSlots(slots: Int): FrameSlotTier =
            entries.lastOrNull { slots >= it.maxSlots } ?: INITIAL

        fun nextFor(currentSlots: Int): FrameSlotTier? =
            entries.firstOrNull { it.maxSlots > currentSlots }
    }
}

enum class CreatorRank(
    val level: Int,
    val title: String,
    val minSales: Int,
    val publishDiscountPct: Int,
    val platformFeePct: Int,
    val freeHighlightUnlocked: Boolean
) {
    RANK_1(1, "Nivel 1", 0, 0, 20, false),
    RANK_2(2, "Nivel 2", 11, 10, 20, false),
    RANK_3(3, "Nivel 3", 51, 25, 20, true),
    RANK_4(4, "Nivel 4", 201, 40, 15, true);

    val creatorSharePct: Int get() = 100 - platformFeePct

    fun publishCostFor(category: StoreFrameCategory, featured: Boolean): Int {
        val baseDiscounted = (category.publishFeeCoins * (100 - publishDiscountPct)) / 100
        val highlightCost = if (featured && !freeHighlightUnlocked) 200 else 0
        return baseDiscounted + highlightCost
    }

    companion object {
        fun forSales(sales: Int): CreatorRank =
            entries.lastOrNull { sales >= it.minSales } ?: RANK_1
    }
}

data class CustomFrame(
    val id: String,
    val creatorId: String,
    val creatorName: String,
    val title: String,
    val frameType: String, // "PNG" or "STUDIO"
    val pngPath: String? = null,
    val primaryColorHex: Long = 0xFF9D4EDD,
    val secondaryColorHex: Long = 0xFFFF4EC2,
    val stylePattern: CustomFrameStyle = CustomFrameStyle.RING_GLOW,
    val thicknessDp: Float = 4f,
    val hasSparkles: Boolean = true,
    val priceCoins: Int = 0,
    val description: String = "",
    val category: String = StoreFrameCategory.PROFILE_FRAME.label,
    val isFeatured: Boolean = false,
    val salesCount: Int = 0,
    val favoriteCount: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
)

enum class AppRank(
    val id: String,
    val label: String,
    val badgeTitle: String
) {
    NONE("none", "Miembro", "Sin verificado"),
    OWNER("owner", "Dueño", "Verificado Negro • Dueño"),
    MODERATOR("moderator", "Moderador", "Verificado Rojo • Moderador"),
    INFLUENCER("influencer", "Influencer", "Verificado Azul • Influencer");

    companion object {
        fun fromId(id: String): AppRank =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NONE
    }
}

enum class NotificationCategory(val label: String) {
    FOLLOW("Seguimiento"),
    MENTION("Mención"),
    COMMUNITY("Comunidad"),
    DRIFTING_BOTTLE("Drifting Bottle");

    companion object {
        fun fromRaw(raw: String): NotificationCategory =
            entries.firstOrNull { it.name == raw } ?: COMMUNITY
    }
}

enum class DonorTier(
    val level: Int,
    val title: String,
    val minExp: Int,
    val coinBonusOnReach: Int,
    val benefitDescription: String,
    val colorHex: Long,
    val emoji: String
) {
    APPRENTICE(1, "Aprendiz", 0, 0, "Acceso a regalos básicos y misiones diarias", 0xFF64B5F6, "🌱"),
    BRONZE_VIP(2, "Bronce VIP", 100, 50, "Insignia Nv.2 en chat + +50 monedas bono", 0xFFCD7F32, "🥉"),
    SILVER_ELITE(3, "Plata Elite", 350, 120, "Destello Plata + prioridad en sala + +120 monedas", 0xFFC0C0C0, "🥈"),
    GOLD_MASTER(4, "Oro Maestro", 800, 300, "Insignia Dorada + +10% EXP extra + +300 monedas", 0xFFFFD166, "🥇"),
    ROYAL_DIAMOND(5, "Diamante Real", 2000, 600, "Aura Diamante + entrada VIP en directo + +600 monedas", 0xFF38BDF8, "💎"),
    COSMIC_LEGEND(6, "Leyenda Cósmica", 5000, 1500, "Corona Suprema Cósmica + +1,500 monedas bono", 0xFFFF4EC2, "👑");

    companion object {
        fun forExp(exp: Int): DonorTier =
            entries.lastOrNull { exp >= it.minExp } ?: APPRENTICE

        fun forLevel(level: Int): DonorTier =
            entries.firstOrNull { it.level == level } ?: entries.last()

        fun nextTier(current: DonorTier): DonorTier? =
            entries.firstOrNull { it.level == current.level + 1 }
    }
}

enum class GiftAnimationStyle {
    HEART_BEAT_ARROW,
    HEART_FLAME_BLAZE,
    HEART_PUFF_FLOAT,
    FINGER_HEART_POP,
    ROSE_PETAL_SPIN,
    MONEY_GUN_BLAST,
    CROWN_ROYAL_ORBIT,
    PARIS_EIFFEL_SHOW,
    SNOW_GLOBE_SWIRL,
    RAINBOW_SWEEP,
    MUSIC_SPHERE_PULSE,
    NEON_PRISM_EXPLODE,
    PERFUME_MIST_SPRAY,
    CHILI_FIRE_BURST,
    FOOTBALL_SPIN_BOUNCE,
    CAT_CUTE_HOP,
    SUNBEAM_RADIANCE,
    SWEET_3D_FLIP,
    STARLIGHT_PORTAL
}

data class GiftItem(
    val id: String,
    val name: String,
    val emoji: String,
    val coinCost: Int,
    val category: String, // "Popular", "Destacados", "Exclusivos"
    val accentHex: Long,
    val isExclusive: Boolean = false,
    val animationStyle: GiftAnimationStyle = GiftAnimationStyle.HEART_BEAT_ARROW
)

object GiftCatalog {
    val items: List<GiftItem> = listOf(
        // Popular (Row 1-4 from user images)
        GiftItem("rose", "Rose", "🌹", 1, "Popular", 0xFFFF4D6D, animationStyle = GiftAnimationStyle.ROSE_PETAL_SPIN),
        GiftItem("doughnut", "Doughnut", "🍩", 30, "Popular", 0xFF64DFDF, animationStyle = GiftAnimationStyle.SWEET_3D_FLIP),
        GiftItem("finger_heart", "Finger Heart", "🫰", 5, "Popular", 0xFFFF85A1, animationStyle = GiftAnimationStyle.FINGER_HEART_POP),
        GiftItem("heart_me", "Heart Me", "❤️", 1, "Popular", 0xFFFF5D8F, animationStyle = GiftAnimationStyle.HEART_BEAT_ARROW),
        GiftItem("flame_heart", "Flame Heart", "❤️‍🔥", 1, "Popular", 0xFFFF6B35, animationStyle = GiftAnimationStyle.HEART_FLAME_BLAZE),
        GiftItem("heart_puff", "Heart Puff", "🩷", 1, "Popular", 0xFFFFB5E8, animationStyle = GiftAnimationStyle.HEART_PUFF_FLOAT),
        GiftItem("ice_cream", "Ice Cream Cone", "🍦", 1, "Popular", 0xFFFFC8DD, animationStyle = GiftAnimationStyle.SWEET_3D_FLIP),
        GiftItem("baguette", "Baguette", "🥖", 10, "Popular", 0xFFF4A261, animationStyle = GiftAnimationStyle.SWEET_3D_FLIP),
        GiftItem("brat", "Brat", "🍏", 1, "Popular", 0xFF8ACE00, animationStyle = GiftAnimationStyle.SWEET_3D_FLIP),
        GiftItem("rainbow", "Rainbow", "🌈", 1, "Popular", 0xFF4CC9F0, animationStyle = GiftAnimationStyle.RAINBOW_SWEEP),
        GiftItem("music_play", "Music Play", "🎵", 1, "Popular", 0xFF00F5D4, animationStyle = GiftAnimationStyle.MUSIC_SPHERE_PULSE),
        GiftItem("gg", "GG", "🎮", 1, "Popular", 0xFF9D4EDD, animationStyle = GiftAnimationStyle.NEON_PRISM_EXPLODE),
        GiftItem("tgif", "TGIF", "💎", 1, "Popular", 0xFF38BDF8, animationStyle = GiftAnimationStyle.NEON_PRISM_EXPLODE),
        GiftItem("tofu", "Tofu Cat", "🐱", 5, "Popular", 0xFFF4A261, animationStyle = GiftAnimationStyle.CAT_CUTE_HOP),
        GiftItem("football", "Football", "⚽", 1, "Popular", 0xFFE2E8F0, animationStyle = GiftAnimationStyle.FOOTBALL_SPIN_BOUNCE),
        GiftItem("dalgona", "Dalgona Candy", "🪙", 1, "Popular", 0xFFE9C46A, animationStyle = GiftAnimationStyle.SWEET_3D_FLIP),
        GiftItem("snowflake", "Snowflake Key", "❄️", 5, "Popular", 0xFF90E0EF, animationStyle = GiftAnimationStyle.SNOW_GLOBE_SWIRL),
        GiftItem("gimme_vote", "Gimme The Vote", "🗳️", 1, "Popular", 0xFF38BDF8, animationStyle = GiftAnimationStyle.NEON_PRISM_EXPLODE),
        GiftItem("rosa", "Rosa", "🌺", 10, "Popular", 0xFFE63946, animationStyle = GiftAnimationStyle.ROSE_PETAL_SPIN),
        GiftItem("perfume", "Perfume", "🧴", 20, "Popular", 0xFFFF70A6, animationStyle = GiftAnimationStyle.PERFUME_MIST_SPRAY),

        // Destacados
        GiftItem("money_gun", "Money Gun", "🔫", 500, "Destacados", 0xFFFF4EC2, isExclusive = true, animationStyle = GiftAnimationStyle.MONEY_GUN_BLAST),
        GiftItem("little_crown", "Little Crown", "👑", 99, "Destacados", 0xFFFFD166, animationStyle = GiftAnimationStyle.CROWN_ROYAL_ORBIT),
        GiftItem("chili", "Chili", "🌶️", 10, "Destacados", 0xFFE63946, animationStyle = GiftAnimationStyle.CHILI_FIRE_BURST),
        GiftItem("night_star", "Night Star", "🌌", 199, "Destacados", 0xFF7B2CBF, animationStyle = GiftAnimationStyle.STARLIGHT_PORTAL),
        GiftItem("wooly_hat", "Wooly Hat", "🧸", 199, "Destacados", 0xFFB08968, animationStyle = GiftAnimationStyle.CAT_CUTE_HOP),
        GiftItem("good_morning", "Good Morning", "☕", 399, "Destacados", 0xFFFFD166, animationStyle = GiftAnimationStyle.SUNBEAM_RADIANCE),
        GiftItem("good_afternoon", "Good Afternoon", "🌞", 399, "Destacados", 0xFFFDB813, animationStyle = GiftAnimationStyle.SUNBEAM_RADIANCE),
        GiftItem("happy_friday", "Happy Friday", "🥳", 399, "Destacados", 0xFFEC4899, animationStyle = GiftAnimationStyle.NEON_PRISM_EXPLODE),
        GiftItem("happy_weekend", "Happy Weekend", "🛹", 599, "Destacados", 0xFF38E59B, animationStyle = GiftAnimationStyle.NEON_PRISM_EXPLODE),
        GiftItem("potato_paris", "Potato in Paris", "🥔", 199, "Destacados", 0xFFF4A261, animationStyle = GiftAnimationStyle.PARIS_EIFFEL_SHOW),

        // Exclusivos
        GiftItem("animated_gift", "Animated Gift", "💘", 1, "Exclusivos", 0xFFFF4EC2, isExclusive = true, animationStyle = GiftAnimationStyle.HEART_BEAT_ARROW),
        GiftItem("blob_paris", "Blob in Paris", "🎨", 5000, "Exclusivos", 0xFF38E59B, isExclusive = true, animationStyle = GiftAnimationStyle.PARIS_EIFFEL_SHOW),
        GiftItem("paris", "Paris", "🗼", 15000, "Exclusivos", 0xFFFFD166, isExclusive = true, animationStyle = GiftAnimationStyle.PARIS_EIFFEL_SHOW),
        GiftItem("celebrate_2025", "Celebrate 2025", "🎂", 2025, "Exclusivos", 0xFFFF4EC2, isExclusive = true, animationStyle = GiftAnimationStyle.CROWN_ROYAL_ORBIT),
        GiftItem("gingerbread", "Gingerbread Party", "🍪", 1200, "Exclusivos", 0xFFD97706, isExclusive = true, animationStyle = GiftAnimationStyle.SWEET_3D_FLIP),
        GiftItem("merry_crustmas", "Merry Crust-mas", "🎄", 6999, "Exclusivos", 0xFF22C55E, isExclusive = true, animationStyle = GiftAnimationStyle.SNOW_GLOBE_SWIRL),
        GiftItem("santa_town", "Santa Town", "🏘️", 9999, "Exclusivos", 0xFF38BDF8, isExclusive = true, animationStyle = GiftAnimationStyle.SNOW_GLOBE_SWIRL),
        GiftItem("winter_magic", "Winter Magic", "🔮", 19999, "Exclusivos", 0xFF90E0EF, isExclusive = true, animationStyle = GiftAnimationStyle.SNOW_GLOBE_SWIRL)
    )

    fun byId(id: String): GiftItem =
        items.firstOrNull { it.id == id } ?: items.first()
}

data class RoomGiftEvent(
    val id: String,
    val communityId: String,
    val channelType: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarKey: String,
    val senderFrameId: String,
    val recipientId: String,
    val recipientName: String,
    val giftId: String,
    val giftName: String,
    val giftEmoji: String,
    val coinCost: Int,
    val comboCount: Int = 1,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class DailyCheckInReward(
    val day: Int,
    val title: String,
    val badgeText: String,
    val emoji: String,
    val expGrant: Int = 0,
    val coinGrant: Int = 0,
    val itemId: String? = null,
    val itemName: String? = null,
    val itemCount: Int = 0,
    val secondaryEmoji: String? = null,
    val secondaryBadge: String? = null
)

object DailyCheckInCatalog {
    val days: List<DailyCheckInReward> = listOf(
        DailyCheckInReward(
            day = 1,
            title = "Adorno de Navidad",
            badgeText = "X1",
            emoji = "⛄",
            itemId = "ornament_xmas",
            itemName = "Adorno de Navidad",
            itemCount = 1
        ),
        DailyCheckInReward(
            day = 2,
            title = "EXP ×20 + Paleta ×1",
            badgeText = "EXP ×20 + 🍦 ×1",
            emoji = "⭐",
            expGrant = 20,
            itemId = "popsicle",
            itemName = "Paleta Refrescante",
            itemCount = 1,
            secondaryEmoji = "🍦",
            secondaryBadge = "X1"
        ),
        DailyCheckInReward(
            day = 3,
            title = "Monedas ×50",
            badgeText = "🪙 ×50",
            emoji = "🪙",
            coinGrant = 50
        ),
        DailyCheckInReward(
            day = 4,
            title = "EXP ×25",
            badgeText = "X25",
            emoji = "⭐",
            expGrant = 25
        ),
        DailyCheckInReward(
            day = 5,
            title = "EXP ×80",
            badgeText = "X80",
            emoji = "⭐",
            expGrant = 80
        ),
        DailyCheckInReward(
            day = 6,
            title = "Monedas ×100",
            badgeText = "🪙 ×100",
            emoji = "🪙",
            coinGrant = 100
        ),
        DailyCheckInReward(
            day = 7,
            title = "Monedas + Caja Regalo + Moto",
            badgeText = "🪙×150 + 📦×1 + 🏍️×1",
            emoji = "📦",
            coinGrant = 150,
            itemId = "moto",
            itemName = "Moto Deportiva",
            itemCount = 1,
            secondaryEmoji = "🏍️",
            secondaryBadge = "X1"
        )
    )
}

data class DailyTaskDefinition(
    val id: String,
    val title: String,
    val target: Int,
    val expReward: Int,
    val coinReward: Int = 0,
    val itemId: String? = null,
    val itemEmoji: String? = null,
    val itemName: String? = null,
    val itemQty: Int = 0,
    val secondaryItemId: String? = null,
    val secondaryItemEmoji: String? = null,
    val secondaryItemName: String? = null,
    val secondaryItemQty: Int = 0,
    val iconKey: String = "follow"
)

object DailyTaskCatalog {
    const val TASK_FOLLOW_HOST = "task_follow_host"
    const val TASK_WATCH_LIVE_5M = "task_watch_live_5m"
    const val TASK_GAME_10K_COINS = "task_game_10k_coins"
    const val TASK_SEND_200_GIFTS = "task_send_200_gifts"

    val tasks: List<DailyTaskDefinition> = listOf(
        DailyTaskDefinition(
            id = TASK_FOLLOW_HOST,
            title = "Sigue a 1 emisores",
            target = 1,
            expReward = 20,
            itemId = "drink",
            itemEmoji = "🧃",
            itemName = "Bebida Energética",
            itemQty = 1,
            iconKey = "follow"
        ),
        DailyTaskDefinition(
            id = TASK_WATCH_LIVE_5M,
            title = "Mira el directo 5 minutos",
            target = 5,
            expReward = 30,
            iconKey = "live"
        ),
        DailyTaskDefinition(
            id = TASK_GAME_10K_COINS,
            title = "Acumula 10,000 monedas en juegos",
            target = 10_000,
            expReward = 80,
            itemId = "drink",
            itemEmoji = "🧃",
            itemName = "Bebida Energética",
            itemQty = 4,
            iconKey = "game"
        ),
        DailyTaskDefinition(
            id = TASK_SEND_200_GIFTS,
            title = "Envía regalos por valor de 200 monedas esta semana",
            target = 200,
            expReward = 300,
            itemId = "clap",
            itemEmoji = "👏",
            itemName = "Aplausos",
            itemQty = 7,
            secondaryItemId = "plane",
            secondaryItemEmoji = "✈️",
            secondaryItemName = "Avión VIP",
            secondaryItemQty = 1,
            iconKey = "gift"
        )
    )
}

data class BagItemMeta(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val giftExpValue: Int
)

object BagCatalog {
    val all: Map<String, BagItemMeta> = listOf(
        BagItemMeta("ornament_xmas", "Adorno de Navidad", "⛄", "Premio Día 1 • Envíalo en sala gratis", 15),
        BagItemMeta("popsicle", "Paleta Helada", "🍦", "Premio Día 2 • Refresca el directo", 20),
        BagItemMeta("gift_box", "Caja de Regalo", "📦", "Cofre especial de racha semanal", 60),
        BagItemMeta("moto", "Moto Deportiva", "🏍️", "Vehículo exclusivo Día 7", 150),
        BagItemMeta("drink", "Bebida Jugo", "🧃", "Recompensa de misiones exclusivas", 25),
        BagItemMeta("clap", "Aplausos", "👏", "Anima al anfitrión en vivo", 15),
        BagItemMeta("plane", "Avión VIP", "✈️", "Regalo aéreo especial de misión", 120),
        BagItemMeta("meplus_crown", "Corona Me+", "👑", "Premio exclusivo del Sorteo Me+", 250)
    ).associateBy { it.id }

    fun metaFor(itemId: String): BagItemMeta =
        all[itemId] ?: BagItemMeta(itemId, itemId.replace("_", " ").replaceFirstChar { it.uppercase() }, "🎁", "Artículo especial de tu bolsa", 20)
}

data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val emailVerified: Boolean = false,
    val bio: String,
    val customStatus: String,
    val avatarFrame: AvatarFrame,
    val avatarUri: String?,
    val presetAvatarKey: String,
    val communitiesCount: Int,
    val followersCount: Int,
    val postsCount: Int,
    val coinsCount: Int = 5000,
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
    val claimedTaskIds: Set<String> = emptySet(),
    val inventoryMap: Map<String, Int> = emptyMap(),
    val mePlusMember: Boolean = false,
    val lastMePlusDrawAtMillis: Long = 0L,
    val maxFrameSlots: Int = 10,
    val creatorSalesCount: Int = 0,
    val creatorEarningsCoins: Int = 0,
    val favoriteFrameIds: Set<String> = emptySet(),
    val appRank: AppRank = AppRank.NONE,
    val avatarFrameId: String = avatarFrame.id,
    val isDeveloper: Boolean = false,
    val isBanned: Boolean = false,
    val banReason: String? = null
) {
    val isVerified: Boolean get() = appRank != AppRank.NONE
    val isPrimaryVerifiedOwner: Boolean
        get() = email.equals("abukeje26@gmail.com", ignoreCase = true) && emailVerified
    val hasInfiniteCoins: Boolean get() = isPrimaryVerifiedOwner
    val canManageVerifications: Boolean get() = isPrimaryVerifiedOwner
    val canBanUsers: Boolean get() = isPrimaryVerifiedOwner
    val donorTier: DonorTier get() = DonorTier.forExp(donorExp)
    val effectiveMaxFrameSlots: Int get() = if (isPrimaryVerifiedOwner) 120 else maxFrameSlots.coerceIn(10, 120)
    val creatorRank: CreatorRank get() = CreatorRank.forSales(creatorSalesCount)
}

data class Community(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val coverKey: String,
    val badgeLabel: String, // "popular" or "nueva"
    val memberCount: Int,
    val activeVoiceCount: Int,
    val isVoiceLocked: Boolean = false,
    val myRole: CommunityRole? = null,
    val isJoined: Boolean = false,
    val backgroundUri: String? = null
)

data class VoiceParticipant(
    val id: String,
    val communityId: String,
    val user: UserProfile,
    val role: CommunityRole,
    val isMuted: Boolean,
    val isSpeaking: Boolean,
    val isHandRaised: Boolean
)

data class RoomEventLog(
    val id: Long,
    val communityId: String,
    val actorName: String,
    val description: String,
    val timestampMillis: Long
)

data class ChatMessage(
    val id: Long,
    val communityId: String,
    val channelType: String, // "VOICE" or "GROUP"
    val sender: UserProfile,
    val senderRole: CommunityRole,
    val content: String,
    val replyToSnippet: String? = null,
    val reactionCount: Int = 0,
    val timestampMillis: Long,
    val audioUri: String? = null,
    val audioDataB64: String? = null,
    val audioDurationMs: Long = 0L,
    val remoteId: String? = null
) {
    val isAudioMessage: Boolean get() = !audioUri.isNullOrBlank() || !audioDataB64.isNullOrBlank()
}

data class DriftingBottle(
    val id: String,
    val authorId: String,
    val anonymousAlias: String,
    val moodTag: String,
    val content: String,
    val replyContent: String?,
    val isPickedByMe: Boolean,
    val createdAtMillis: Long,
    val expiresAtMillis: Long
)

data class AppNotification(
    val id: Long,
    val userId: String,
    val actorName: String,
    val actorAvatarKey: String,
    val actorAvatarUri: String?,
    val actorFrame: AvatarFrame,
    val title: String,
    val subtitle: String,
    val category: NotificationCategory,
    val targetCommunityId: String?,
    val isRead: Boolean,
    val timestampMillis: Long
)

data class AppSettings(
    val neonGlowEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val showStatusInChat: Boolean = true,
    val compactVoiceGrid: Boolean = false,
    val privateProfileMode: Boolean = false,
    val languageLabel: String = "Español (ES)",
    val appBackgroundUri: String? = null,
    val appBackgroundDim: Float = 0.62f,
    val inviteWebUrl: String = ""
)

data class CommunityPost(
    val id: String,
    val author: UserProfile,
    val communityId: String? = null,
    val communityName: String? = null,
    val content: String,
    val imageUri: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class PostComment(
    val id: Long,
    val postId: String,
    val author: UserProfile,
    val content: String,
    val createdAtMillis: Long
)
