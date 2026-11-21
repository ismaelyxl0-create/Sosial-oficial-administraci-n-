package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CreatorRank
import com.example.model.CustomFrame
import com.example.model.CustomFrameStyle
import com.example.model.FrameSlotTier
import com.example.model.StoreFrameCategory
import com.example.model.UserProfile
import com.example.ui.components.FramedAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.VerifiedRankBadge
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerRose
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
import kotlin.math.roundToInt

private enum class StoreSubTab(val label: String) {
    SHOP("🛒 Tienda"),
    WORKSHOP("🎨 Mi Taller"),
    SALES("📊 Mis Ventas")
}

private enum class StoreSortMode(val label: String) {
    ALL("Todos"),
    BEST_SELLING("🔥 Más Vendidos"),
    NEWEST("✨ Nuevos"),
    FEATURED("🌟 Destacados")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiendaScreen(
    user: UserProfile?,
    allUsers: List<UserProfile>,
    customFrames: List<CustomFrame>,
    onBack: () -> Unit,
    onBuyFrame: (String) -> Unit,
    onEquipFrame: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onUnlockNextSlotTier: () -> Unit,
    onPublishPngDesign: (
        title: String,
        uri: Uri,
        priceCoins: Int,
        description: String,
        category: String,
        isFeatured: Boolean,
        onDone: () -> Unit
    ) -> Unit,
    onPublishStudioDesign: (
        title: String,
        primaryHex: Long,
        secondaryHex: Long,
        stylePattern: CustomFrameStyle,
        thicknessDp: Float,
        hasSparkles: Boolean,
        priceCoins: Int,
        description: String,
        category: String,
        isFeatured: Boolean,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteFrame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (user == null) return

    var activeTab by rememberSaveable { mutableStateOf(StoreSubTab.SHOP) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(StoreSortMode.ALL) }
    var selectedCategory by rememberSaveable { mutableStateOf("Todas") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }

    var showCreateWizard by rememberSaveable { mutableStateOf(false) }
    var showSlotTierSheet by rememberSaveable { mutableStateOf(false) }
    var pendingPurchaseFrame by remember { mutableStateOf<CustomFrame?>(null) }
    var pendingDeleteFrame by remember { mutableStateOf<CustomFrame?>(null) }

    val myPublishedFrames = remember(customFrames, user.id) {
        customFrames.filter { it.creatorId == user.id }
    }
    val publishedCount = myPublishedFrames.size
    val maxSlots = user.effectiveMaxFrameSlots
    val currentSlotTier = FrameSlotTier.forMaxSlots(maxSlots)
    val nextSlotTier = FrameSlotTier.nextFor(maxSlots)
    val isSlotFull = !user.isPrimaryVerifiedOwner && publishedCount >= maxSlots

    val creatorRank = user.creatorRank
    val nextCreatorRank = CreatorRank.entries.firstOrNull { it.level == creatorRank.level + 1 }

    val featuredFrames = remember(customFrames) {
        customFrames.filter { it.isFeatured }.sortedByDescending { it.salesCount }
    }

    val topCreators = remember(customFrames, allUsers) {
        val grouped = customFrames.groupBy { it.creatorId }
        grouped.entries
            .map { (cid, list) ->
                val profile = allUsers.firstOrNull { it.id == cid }
                Triple(
                    profile,
                    list.firstOrNull()?.creatorName ?: profile?.username ?: "Creador",
                    list.sumOf { it.salesCount }
                )
            }
            .sortedByDescending { it.third }
            .take(8)
    }

    val displayedFrames = remember(customFrames, searchQuery, sortMode, selectedCategory, favoritesOnly, user.favoriteFrameIds) {
        val base = customFrames.filter { frame ->
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                frame.title.contains(q, ignoreCase = true) ||
                frame.creatorName.contains(q, ignoreCase = true) ||
                frame.description.contains(q, ignoreCase = true)
            val matchesCategory = selectedCategory == "Todas" || frame.category.equals(selectedCategory, ignoreCase = true)
            val matchesFav = !favoritesOnly || user.favoriteFrameIds.contains(frame.id)
            val matchesSortFilter = if (sortMode == StoreSortMode.FEATURED) frame.isFeatured else true
            matchesSearch && matchesCategory && matchesFav && matchesSortFilter
        }
        when (sortMode) {
            StoreSortMode.ALL -> base.sortedWith(compareByDescending<CustomFrame> { it.isFeatured }.thenByDescending { it.createdAtMillis })
            StoreSortMode.BEST_SELLING -> base.sortedByDescending { it.salesCount }
            StoreSortMode.NEWEST -> base.sortedByDescending { it.createdAtMillis }
            StoreSortMode.FEATURED -> base.sortedByDescending { it.salesCount }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Discord-inspired Store Header Bar
        Surface(
            color = SurfaceDark.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, BorderSubtle.copy(alpha = 0.55f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(44.dp).testTag("store_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                        }
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = NeonPink, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Tienda de Creaciones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Cupo: $publishedCount/$maxSlots • ${creatorRank.title}",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = RoleAdminGold.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, RoleAdminGold.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = if (user.hasInfiniteCoins) "💎 ∞" else "💎 ${user.coinsCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RoleAdminGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (favoritesOnly) NeonPink.copy(alpha = 0.25f) else SurfaceElevated,
                            border = BorderStroke(1.dp, if (favoritesOnly) NeonPink else BorderSubtle),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    activeTab = StoreSubTab.SHOP
                                    favoritesOnly = !favoritesOnly
                                }
                                .testTag("store_favorites_filter_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favoritos",
                                    tint = if (favoritesOnly) NeonPink else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                // 3 Sub-Tabs: Tienda / Mi Taller / Mis Ventas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StoreSubTab.entries.forEach { tab ->
                        val selected = activeTab == tab
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) NeonPurple else SurfaceElevated,
                            border = BorderStroke(1.dp, if (selected) NeonPink else BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { activeTab = tab }
                                .testTag("store_subtab_${tab.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = tab.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        when (activeTab) {
            StoreSubTab.SHOP -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search Bar
                    item(span = { GridItemSpan(2) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar por nombre de creador o diseño...", color = TextMuted, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = NeonCyan) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Limpiar", tint = TextSecondary)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(22.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonPink,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = SurfaceCard,
                                    unfocusedContainerColor = SurfaceCard,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("store_search_input")
                            )

                            // Sort chips: Todos, Más Vendidos, Nuevos, Destacados
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(StoreSortMode.entries) { mode ->
                                    val active = sortMode == mode
                                    FilterChip(
                                        selected = active,
                                        onClick = { sortMode = mode },
                                        label = { Text(mode.label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonPurple,
                                            selectedLabelColor = Color.White,
                                            containerColor = SurfaceCard,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }

                            // Category filter chips
                            val categoryFilters = listOf("Todas") + StoreFrameCategory.entries.map { it.label }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(categoryFilters) { cat ->
                                    val active = selectedCategory == cat
                                    val emoji = StoreFrameCategory.entries.firstOrNull { it.label == cat }?.emoji?.plus(" ") ?: ""
                                    FilterChip(
                                        selected = active,
                                        onClick = { selectedCategory = cat },
                                        label = { Text("$emoji$cat", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                                            selectedLabelColor = NeonCyan,
                                            containerColor = SurfaceElevated,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Featured Hero Banners (Discord Shop inspired)
                    if (featuredFrames.isNotEmpty() && searchQuery.isBlank() && !favoritesOnly) {
                        item(span = { GridItemSpan(2) }) {
                            Column {
                                Text(
                                    text = "🌟 Destacados en Portada",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RoleAdminGold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(featuredFrames, key = { "hero_${it.id}" }) { hero ->
                                        val isEquipped = user.avatarFrameId == hero.id
                                        val isCreator = hero.creatorId == user.id
                                        Box(
                                            modifier = Modifier
                                                .width(290.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            Color(hero.primaryColorHex).copy(alpha = 0.35f),
                                                            Color(0xFF171326),
                                                            Color(hero.secondaryColorHex).copy(alpha = 0.28f)
                                                        )
                                                    )
                                                )
                                                .border(1.5.dp, Brush.horizontalGradient(listOf(RoleAdminGold, NeonPink)), RoundedCornerShape(20.dp))
                                                .clickable {
                                                    if (isEquipped || isCreator || hero.priceCoins <= 0) {
                                                        onEquipFrame(hero.id)
                                                    } else {
                                                        pendingPurchaseFrame = hero
                                                    }
                                                }
                                                .padding(14.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                FramedAvatar(
                                                    avatarUri = user.avatarUri,
                                                    presetAvatarKey = user.presetAvatarKey,
                                                    frameId = hero.id,
                                                    customFrameOverride = hero,
                                                    size = 66.dp,
                                                    contentDescription = hero.title
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = RoleAdminGold.copy(alpha = 0.22f)
                                                    ) {
                                                        Text(
                                                            text = "DESTACADO • ${hero.category}",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = RoleAdminGold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = hero.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = TextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "por @${hero.creatorName}",
                                                        fontSize = 11.sp,
                                                        color = NeonCyan
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text(
                                                            text = if (hero.priceCoins > 0) "💎 ${hero.priceCoins}" else "Gratis",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = RoleAdminGold
                                                        )
                                                        Text(
                                                            text = "• ${hero.salesCount} ventas",
                                                            fontSize = 11.sp,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Top Creators Horizontal Row
                    if (topCreators.isNotEmpty() && searchQuery.isBlank() && !favoritesOnly) {
                        item(span = { GridItemSpan(2) }) {
                            Column {
                                Text(
                                    text = "🧑‍🎨 Creadores Top",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(topCreators, key = { "creator_${it.second}" }) { (profile, name, totalSales) ->
                                        val rank = CreatorRank.forSales(totalSales)
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = SurfaceCard,
                                            border = BorderStroke(1.dp, BorderGlow.copy(alpha = 0.6f)),
                                            modifier = Modifier.clickable { searchQuery = name }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                FramedAvatar(
                                                    avatarUri = profile?.avatarUri,
                                                    presetAvatarKey = profile?.presetAvatarKey ?: "anime",
                                                    frameId = profile?.avatarFrameId ?: "neon_pulse",
                                                    size = 34.dp,
                                                    contentDescription = name
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("@$name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                        if (profile != null && profile.appRank != com.example.model.AppRank.NONE) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            VerifiedRankBadge(rank = profile.appRank, size = 14.dp)
                                                        }
                                                    }
                                                    Text(
                                                        text = "${rank.title} • $totalSales ventas",
                                                        fontSize = 10.sp,
                                                        color = NeonPink
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Grid Section Title
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = if (favoritesOnly) "❤️ Tus Favoritos (${displayedFrames.size})"
                            else "Encuentra tu estilo (${displayedFrames.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    if (displayedFrames.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = NeonPink, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (favoritesOnly) "No tienes diseños favoritos guardados" else "No hay diseños en esta categoría todavía",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Sé el primero en publicar un marco, aura o paquete en Mi Taller y gana 80% de cada venta.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    NeonGradientButton(
                                        text = "Ir a Mi Taller",
                                        onClick = { activeTab = StoreSubTab.WORKSHOP },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    } else {
                        items(displayedFrames, key = { it.id }) { item ->
                            StoreFrameGridCard(
                                frame = item,
                                currentUser = user,
                                isFavorite = user.favoriteFrameIds.contains(item.id),
                                isEquipped = user.avatarFrameId == item.id,
                                onToggleFavorite = { onToggleFavorite(item.id) },
                                onActionClick = {
                                    val isCreator = item.creatorId == user.id
                                    if (isCreator || item.priceCoins <= 0) {
                                        onEquipFrame(item.id)
                                    } else {
                                        pendingPurchaseFrame = item
                                    }
                                }
                            )
                        }
                    }
                }
            }

            StoreSubTab.WORKSHOP -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Quota Card (Sistema de Cupos)
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (isSlotFull) DangerRose else BorderGlow
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${currentSlotTier.emoji} Cupo de Venta: $publishedCount / $maxSlots",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSlotFull) DangerRose else TextPrimary
                                        )
                                        Text(
                                            text = "Nivel ${currentSlotTier.title} • ${if (user.isPrimaryVerifiedOwner) "Dueño (Cupos Máximos)" else "Límite permanente en tu tienda"}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = NeonPurple.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, NeonPink),
                                        modifier = Modifier
                                            .clickable { showSlotTierSheet = true }
                                            .testTag("open_slot_tiers_button")
                                    ) {
                                        Text(
                                            text = if (nextSlotTier != null) "📈 Ampliar" else "✓ Máximo",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { (publishedCount.toFloat() / maxSlots.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                                    color = if (isSlotFull) DangerRose else NeonCyan,
                                    trackColor = SurfaceElevated,
                                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50))
                                )

                                if (isSlotFull) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = DangerRose.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, DangerRose.copy(alpha = 0.65f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = DangerRose, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "¡Tu cupo está lleno! Tienes $publishedCount/$maxSlots marcos en venta. Retira uno antiguo o amplía tu cupo.",
                                                fontSize = 11.sp,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Create New Design CTA + Publication Fees Overview
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🎨 Crear Nuevo Diseño", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(
                                    text = "Sube PNG transparente o diseña un marco neón en vivo. Gana ${creatorRank.creatorSharePct}% automático por cada compra.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    FeeMiniPill("🖼️ Marco", "${creatorRank.publishCostFor(StoreFrameCategory.PROFILE_FRAME, false)} 💎", Modifier.weight(1f))
                                    FeeMiniPill("📦 Paquete", "${creatorRank.publishCostFor(StoreFrameCategory.PACKAGE, false)} 💎", Modifier.weight(1f))
                                    FeeMiniPill("🌟 Portada", if (creatorRank.freeHighlightUnlocked) "Gratis Nv.3+" else "+200 💎", Modifier.weight(1f))
                                }
                                NeonGradientButton(
                                    text = if (isSlotFull) "Cupo lleno ($publishedCount/$maxSlots) — Ampliar primero" else "+ Crear Nuevo Diseño",
                                    onClick = {
                                        if (isSlotFull) showSlotTierSheet = true else showCreateWizard = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "store_create_design_button"
                                )
                            }
                        }
                    }

                    // Rules Card
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("⚖️ Reglas de la Tienda VozMundo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NeonCyan)
                                Text("• Diseños propios y originales, sin contenido ofensivo ni con derechos de autor.", fontSize = 11.sp, color = TextSecondary)
                                Text("• La tarifa de publicación no es reembolsable.", fontSize = 11.sp, color = TextSecondary)
                                Text("• Si retiras un diseño de la tienda, liberas 1 espacio pero quienes ya lo compraron lo conservan.", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Creator's active designs
                    item {
                        Text(
                            text = "Mis diseños publicados (${myPublishedFrames.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    if (myPublishedFrames.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Todavía no has publicado creaciones en la tienda.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(myPublishedFrames, key = { "mine_${it.id}" }) { mine ->
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FramedAvatar(
                                        avatarUri = user.avatarUri,
                                        presetAvatarKey = user.presetAvatarKey,
                                        frameId = mine.id,
                                        customFrameOverride = mine,
                                        size = 54.dp,
                                        contentDescription = mine.title
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(mine.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                            if (mine.isFeatured) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("🌟", fontSize = 12.sp)
                                            }
                                        }
                                        Text(
                                            text = "${mine.category} • ${if (mine.priceCoins > 0) "💎 ${mine.priceCoins}" else "Gratis"} • ${mine.salesCount} ventas",
                                            fontSize = 11.sp,
                                            color = NeonCyan
                                        )
                                    }
                                    TextButton(onClick = { onEquipFrame(mine.id) }) {
                                        Text(
                                            text = if (user.avatarFrameId == mine.id) "En uso" else "Usar",
                                            color = if (user.avatarFrameId == mine.id) SuccessGreen else NeonPink,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { pendingDeleteFrame = mine },
                                        modifier = Modifier.testTag("workshop_delete_frame_${mine.id}")
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Retirar de tienda", tint = DangerRose)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            StoreSubTab.SALES -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = RoleAdminGold.copy(alpha = 0.7f)) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = RoleAdminGold, modifier = Modifier.size(26.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("🏆 Rango de Creador: ${creatorRank.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = RoleAdminGold)
                                        Text(
                                            text = "Reparto actual: ${creatorRank.creatorSharePct}% para ti • ${creatorRank.platformFeePct}% plataforma",
                                            fontSize = 12.sp,
                                            color = NeonCyan
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    FeeMiniPill("Ventas totales", "${user.creatorSalesCount}", Modifier.weight(1f))
                                    FeeMiniPill("Ganancias netas", "💎 ${user.creatorEarningsCoins}", Modifier.weight(1f))
                                    FeeMiniPill("Descuento tarifa", "${creatorRank.publishDiscountPct}%", Modifier.weight(1f))
                                }
                                if (nextCreatorRank != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val span = (nextCreatorRank.minSales - creatorRank.minSales).coerceAtLeast(1)
                                    val prog = ((user.creatorSalesCount - creatorRank.minSales).toFloat() / span.toFloat()).coerceIn(0f, 1f)
                                    Text(
                                        text = "Progreso a ${nextCreatorRank.title}: ${user.creatorSalesCount}/${nextCreatorRank.minSales} ventas",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { prog },
                                        color = RoleAdminGold,
                                        trackColor = SurfaceElevated,
                                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50))
                                    )
                                }
                            }
                        }
                    }

                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📈 Niveles de Creador", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                CreatorRank.entries.forEach { rank ->
                                    val unlocked = user.creatorSalesCount >= rank.minSales
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${if (unlocked) "✅" else "🔒"} ${rank.title} (${rank.minSales}+ ventas)",
                                            fontSize = 12.sp,
                                            fontWeight = if (unlocked) FontWeight.Bold else FontWeight.Normal,
                                            color = if (unlocked) SuccessGreen else TextSecondary
                                        )
                                        Text(
                                            text = buildString {
                                                append("-${rank.publishDiscountPct}% tarifa")
                                                if (rank.freeHighlightUnlocked) append(" • Destacado gratis")
                                                if (rank.platformFeePct < 20) append(" • 85/15")
                                            },
                                            fontSize = 11.sp,
                                            color = if (unlocked) NeonCyan else TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 6-Tier Permanent Cupo Expansion Sheet
    if (showSlotTierSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSlotTierSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceCard,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("📈 Sistema de Cupos Permanentes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = "Pagas una vez y ese límite de marcos en venta es tuyo para siempre. Actual: $publishedCount/$maxSlots.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                FrameSlotTier.entries.forEach { tier ->
                    val unlocked = maxSlots >= tier.maxSlots
                    val isNext = nextSlotTier == tier
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isNext) NeonPurple.copy(alpha = 0.22f) else SurfaceElevated,
                        border = BorderStroke(1.dp, if (isNext) NeonPink else if (unlocked) SuccessGreen.copy(alpha = 0.6f) else BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${tier.emoji} ${tier.title} — ${tier.maxSlots} marcos",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (tier == FrameSlotTier.INITIAL) "Gratis al empezar (10 espacios)"
                                    else "+${tier.addedSlots} espacios adicionales",
                                    fontSize = 11.sp,
                                    color = NeonCyan
                                )
                            }
                            if (unlocked) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Desbloqueado", tint = SuccessGreen)
                            } else if (isNext) {
                                Button(
                                    onClick = {
                                        onUnlockNextSlotTier()
                                        showSlotTierSheet = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                    modifier = Modifier.testTag("unlock_slot_tier_${tier.maxSlots}")
                                ) {
                                    Text("💎 ${tier.unlockCost}", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("💎 ${tier.unlockCost}", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Create New Design Wizard Sheet
    if (showCreateWizard) {
        CreateStoreDesignSheet(
            user = user,
            onDismiss = { showCreateWizard = false },
            onPublishPng = { title, uri, price, desc, cat, feat ->
                onPublishPngDesign(title, uri, price, desc, cat, feat) { showCreateWizard = false }
            },
            onPublishStudio = { title, pHex, sHex, style, thick, sparkles, price, desc, cat, feat ->
                onPublishStudioDesign(title, pHex, sHex, style, thick, sparkles, price, desc, cat, feat) {
                    showCreateWizard = false
                }
            }
        )
    }

    // Confirm Purchase Dialog
    pendingPurchaseFrame?.let { item ->
        val creatorSharePct = 80
        AlertDialog(
            onDismissRequest = { pendingPurchaseFrame = null },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Comprar diseño en Tienda", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    FramedAvatar(
                        avatarUri = user.avatarUri,
                        presetAvatarKey = user.presetAvatarKey,
                        frameId = item.id,
                        customFrameOverride = item,
                        size = 76.dp,
                        contentDescription = item.title
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("${item.category} • por @${item.creatorName}", fontSize = 12.sp, color = NeonCyan)
                    if (item.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.description, fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Costo: 💎 ${item.priceCoins} monedas\n(${creatorSharePct}%+ va directo a @${item.creatorName})",
                        fontSize = 12.sp,
                        color = RoleAdminGold,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = item.id
                        pendingPurchaseFrame = null
                        onBuyFrame(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                ) {
                    Text("Comprar 💎 ${item.priceCoins}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurchaseFrame = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    pendingDeleteFrame?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteFrame = null },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Retirar de la tienda") },
            text = {
                Text("¿Retirar \"${target.title}\"? Liberarás 1 espacio en tu cupo, pero los usuarios que ya lo compraron conservarán el diseño.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteFrame(target.id)
                    pendingDeleteFrame = null
                }) {
                    Text("Retirar", color = DangerRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteFrame = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StoreFrameGridCard(
    frame: CustomFrame,
    currentUser: UserProfile,
    isFavorite: Boolean,
    isEquipped: Boolean,
    onToggleFavorite: () -> Unit,
    onActionClick: () -> Unit
) {
    val isCreator = frame.creatorId == currentUser.id
    GlassCard(
        modifier = Modifier.fillMaxWidth().testTag("store_item_card_${frame.id}"),
        cornerRadius = 18.dp,
        borderColor = if (isEquipped) NeonPink else if (frame.isFeatured) RoleAdminGold.copy(alpha = 0.7f) else BorderSubtle
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(frame.primaryColorHex).copy(alpha = 0.22f),
                                Color(0xFF171524)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                FramedAvatar(
                    avatarUri = currentUser.avatarUri,
                    presetAvatarKey = currentUser.presetAvatarKey,
                    frameId = frame.id,
                    customFrameOverride = frame,
                    size = 70.dp,
                    contentDescription = frame.title
                )
                Surface(
                    shape = CircleShape,
                    color = SurfaceDark.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .clickable(onClick = onToggleFavorite)
                        .testTag("store_fav_${frame.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) NeonPink else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (frame.isFeatured) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = RoleAdminGold.copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    ) {
                        Text(
                            text = "🌟 TOP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1A1205),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = frame.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "🧑‍🎨 @${frame.creatorName}",
                fontSize = 11.sp,
                color = NeonCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${frame.category} • ${frame.salesCount} ventas",
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isEquipped -> SuccessGreen.copy(alpha = 0.25f)
                        isCreator || frame.priceCoins <= 0 -> NeonPurple
                        else -> NeonPink
                    }
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                if (!isEquipped && !isCreator && frame.priceCoins > 0) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = when {
                        isEquipped -> "Equipado ✓"
                        isCreator -> "Equipar (Tuyo)"
                        frame.priceCoins <= 0 -> "Usar Gratis"
                        else -> "💎 ${frame.priceCoins} • Comprar"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEquipped) SuccessGreen else Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateStoreDesignSheet(
    user: UserProfile,
    onDismiss: () -> Unit,
    onPublishPng: (title: String, uri: Uri, priceCoins: Int, description: String, category: String, isFeatured: Boolean) -> Unit,
    onPublishStudio: (
        title: String,
        primaryHex: Long,
        secondaryHex: Long,
        stylePattern: CustomFrameStyle,
        thicknessDp: Float,
        hasSparkles: Boolean,
        priceCoins: Int,
        description: String,
        category: String,
        isFeatured: Boolean
    ) -> Unit
) {
    var modePng by rememberSaveable { mutableStateOf(false) }
    var pickedPngUri by remember { mutableStateOf<Uri?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by remember { mutableStateOf(StoreFrameCategory.PROFILE_FRAME) }
    var priceCoins by rememberSaveable { mutableFloatStateOf(100f) }
    var isFeatured by rememberSaveable { mutableStateOf(false) }

    var primaryHex by rememberSaveable { mutableLongStateOf(0xFF9D4EDD) }
    var secondaryHex by rememberSaveable { mutableLongStateOf(0xFFFF4EC2) }
    var style by remember { mutableStateOf(CustomFrameStyle.RING_GLOW) }
    var thickness by rememberSaveable { mutableFloatStateOf(4.5f) }
    var sparkles by rememberSaveable { mutableStateOf(true) }

    val pngPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pickedPngUri = uri
            modePng = true
        }
    }

    val creatorRank = user.creatorRank
    val publishFee = if (user.hasInfiniteCoins) 0 else creatorRank.publishCostFor(category, isFeatured)
    val canPublish = title.trim().length in 2..30 &&
        (!modePng || pickedPngUri != null) &&
        (user.hasInfiniteCoins || user.coinsCount >= publishFee)

    val previewFrame = CustomFrame(
        id = "store_preview",
        creatorId = user.id,
        creatorName = user.username,
        title = title.ifBlank { "Vista Previa" },
        frameType = if (modePng && pickedPngUri != null) "PNG" else "STUDIO",
        pngPath = pickedPngUri?.toString(),
        primaryColorHex = primaryHex,
        secondaryColorHex = secondaryHex,
        stylePattern = style,
        thicknessDp = thickness,
        hasSparkles = sparkles,
        priceCoins = priceCoins.roundToInt(),
        category = category.label,
        isFeatured = isFeatured
    )

    val colorSwatches = listOf(0xFF9D4EDD, 0xFFFF4EC2, 0xFF4CC9F0, 0xFFFFD166, 0xFF38E59B, 0xFFFF5D8F, 0xFFFFFFFF)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🎨 Nuevo Diseño para Tienda", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FramedAvatar(
                    avatarUri = user.avatarUri,
                    presetAvatarKey = user.presetAvatarKey,
                    customFrameOverride = previewFrame,
                    size = 82.dp,
                    contentDescription = "Vista previa"
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !modePng,
                            onClick = { modePng = false },
                            label = { Text("✨ Estudio", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = modePng,
                            onClick = {
                                modePng = true
                                if (pickedPngUri == null) {
                                    pngPicker.launch(arrayOf("image/png", "image/*"))
                                }
                            },
                            label = { Text("🖼️ PNG", fontSize = 11.sp) }
                        )
                    }
                    if (modePng) {
                        OutlinedButton(
                            onClick = { pngPicker.launch(arrayOf("image/png", "image/*")) },
                            border = BorderStroke(1.dp, NeonCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (pickedPngUri != null) "PNG listo ✓ (Cambiar)" else "Elegir archivo PNG", fontSize = 11.sp, color = NeonCyan)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 30) title = it },
                label = { Text("Nombre del diseño (${title.length}/30)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("store_design_title_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 100) description = it },
                label = { Text("Descripción / Estilo (${description.length}/100)") },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("store_design_desc_input")
            )

            Text("Categoría", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StoreFrameCategory.entries) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text("${cat.emoji} ${cat.label}", fontSize = 11.sp) }
                    )
                }
            }

            if (!modePng) {
                Text("Patrón y colores", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CustomFrameStyle.entries) { st ->
                        FilterChip(
                            selected = style == st,
                            onClick = { style = st },
                            label = { Text(st.displayName, fontSize = 11.sp) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorSwatches.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(if (primaryHex == hex) 2.5.dp else 1.dp, if (primaryHex == hex) Color.White else BorderSubtle, CircleShape)
                                .clickable { primaryHex = hex }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Precio de venta: 💎 ${priceCoins.roundToInt()}", fontWeight = FontWeight.Bold, color = RoleAdminGold)
                Text("Tú ganas ${creatorRank.creatorSharePct}%", fontSize = 12.sp, color = SuccessGreen)
            }
            Slider(
                value = priceCoins,
                onValueChange = { priceCoins = (it / 10f).roundToInt() * 10f },
                valueRange = 20f..500f,
                colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPurple)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🌟 Destacar en portada (7 días)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (creatorRank.freeHighlightUnlocked || user.hasInfiniteCoins) "Desbloqueado gratis en tu rango" else "+200 💎 para aparecer primero",
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                }
                Switch(checked = isFeatured, onCheckedChange = { isFeatured = it })
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, BorderGlow.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tarifa de publicación (${creatorRank.title}):", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        text = if (user.hasInfiniteCoins) "Gratis (Dueño)" else "💎 $publishFee",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (canPublish) RoleAdminGold else DangerRose
                    )
                }
            }

            NeonGradientButton(
                text = if (!canPublish && !user.hasInfiniteCoins && user.coinsCount < publishFee) {
                    "Saldo insuficiente (Necesitas 💎 $publishFee)"
                } else {
                    "Publicar en la Tienda"
                },
                enabled = canPublish,
                onClick = {
                    val cleanPrice = priceCoins.roundToInt().coerceIn(20, 500)
                    if (modePng && pickedPngUri != null) {
                        onPublishPng(title, pickedPngUri!!, cleanPrice, description, category.label, isFeatured)
                    } else {
                        onPublishStudio(title, primaryHex, secondaryHex, style, thickness, sparkles, cleanPrice, description, category.label, isFeatured)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "store_publish_design_submit"
            )
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
private fun FeeMiniPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = RoleAdminGold, maxLines = 1)
            Text(label, fontSize = 10.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
