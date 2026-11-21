package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AppRank
import com.example.model.Community
import com.example.model.CommunityRole
import com.example.model.UserProfile
import com.example.ui.components.CommunityCoverImage
import com.example.ui.components.FramedAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.RoleBadge
import com.example.ui.components.TxlLogoMark
import com.example.ui.components.UserProfilePreviewSheet
import com.example.ui.components.VerifiedRankBadge
import com.example.ui.components.shareNexoLiveApp
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerRose
import java.io.File
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ExploreScreen(
    communities: List<Community>,
    allUsers: List<UserProfile> = emptyList(),
    currentUserId: String = "",
    currentUserRank: AppRank = AppRank.NONE,
    currentUserIsDeveloper: Boolean = false,
    onOpenVoiceRoom: (String) -> Unit,
    onOpenGroupChat: (String) -> Unit,
    onToggleJoin: (String) -> Unit,
    onPickBackgroundUri: (Uri, (String) -> Unit) -> Unit = { _, _ -> },
    onCreateCommunity: (name: String, category: String, description: String, coverKey: String, backgroundUri: String?) -> Unit,
    onDeleteCommunity: (String) -> Unit = {},
    onAssignAppRank: (String, AppRank) -> Unit = { _, _ -> },
    onGrantCoins: (targetUserId: String, amount: Int) -> Unit = { _, _ -> },
    onBanUser: (String, String) -> Unit = { _, _ -> },
    onUnbanUser: (String) -> Unit = {},
    onFollowHost: (String) -> Unit = {},
    onOpenStore: () -> Unit = {},
    inviteWebUrl: String = "",
    onSaveInviteWebUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var userSearchQuery by rememberSaveable { mutableStateOf("") }
    var showAllUsers by rememberSaveable { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableStateOf("Todas") }
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }
    var showInviteSheet by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteCommunity by remember { mutableStateOf<Community?>(null) }
    var selectedUserId by rememberSaveable { mutableStateOf<String?>(null) }

    val filters = listOf("Todas", "IRL", "Gamer", "Populares", "Mis Canales")
    val filtered = remember(communities, searchQuery, selectedFilter) {
        communities.filter { c ->
            val matchesQuery = searchQuery.isBlank() ||
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.category.contains(searchQuery, ignoreCase = true)
            val isIrl = c.coverKey == "irl" || c.coverKey == "music" ||
                c.category.contains("irl", ignoreCase = true) ||
                c.category.contains("vlog", ignoreCase = true) ||
                c.category.contains("música", ignoreCase = true)
            val isGamer = c.coverKey == "fps" || c.coverKey == "gaming" ||
                c.category.contains("gamer", ignoreCase = true) ||
                c.category.contains("gaming", ignoreCase = true) ||
                c.category.contains("fps", ignoreCase = true) ||
                c.category.contains("rpg", ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "IRL" -> isIrl
                "Gamer" -> isGamer
                "Populares" -> c.badgeLabel == "popular"
                "Mis Canales" -> c.isJoined
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val cleanUserQuery = userSearchQuery.trim()
    val showUserResults = cleanUserQuery.isNotEmpty() || showAllUsers
    val matchedUsers = remember(allUsers, cleanUserQuery) {
        if (cleanUserQuery.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { u ->
                u.username.contains(cleanUserQuery, ignoreCase = true) ||
                    u.customStatus.contains(cleanUserQuery, ignoreCase = true) ||
                    u.bio.contains(cleanUserQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            modifier = Modifier.weight(1f),
            columns = GridCells.Adaptive(minSize = 165.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TxlLogoMark(compact = true)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SurfaceElevated,
                                border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { onOpenStore() }
                                    .testTag("explore_open_store_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Storefront, contentDescription = "Tienda de Creaciones", tint = NeonPink, modifier = Modifier.size(18.dp))
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = SurfaceElevated,
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { showInviteSheet = true }
                                    .testTag("explore_share_app_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Share, contentDescription = "Invitar por Web", tint = NeonCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SurfaceElevated,
                                border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NeonPink)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${communities.sumOf { it.activeVoiceCount }} en vivo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "En Vivos IRL & Gamer",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar streams IRL, juegos o canales...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = NeonPurpleLight) },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPink,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard.copy(alpha = 0.8f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("explore_search_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filters) { filter ->
                            val selected = selectedFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonPurple,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = SurfaceCard,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = BorderSubtle,
                                    selectedBorderColor = NeonPink
                                ),
                                modifier = Modifier.testTag("filter_chip_$filter")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Buscar personas",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .clickable { showAllUsers = !showAllUsers }
                                .testTag("explore_toggle_all_users"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (showAllUsers) NeonCyan.copy(alpha = 0.22f) else SurfaceElevated,
                                border = BorderStroke(1.dp, if (showAllUsers) NeonCyan else BorderSubtle)
                            ) {
                                Text(
                                    text = if (showAllUsers) "Ocultar lista (${allUsers.size})" else "${allUsers.size} registrados",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (showAllUsers) NeonCyan else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userSearchQuery,
                        onValueChange = { userSearchQuery = it },
                        placeholder = { Text("Buscar usuarios registrados por nombre...", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.PersonSearch, contentDescription = "Buscar usuarios", tint = NeonCyan) },
                        trailingIcon = {
                            if (userSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { userSearchQuery = "" },
                                    modifier = Modifier.testTag("explore_user_search_clear")
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Limpiar búsqueda de usuarios", tint = TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard.copy(alpha = 0.8f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("explore_user_search_input")
                    )
                }
            }

            if (showUserResults) {
                if (matchedUsers.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("explore_user_search_empty")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.PersonSearch, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No encontramos usuarios con \"$cleanUserQuery\"",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Prueba con otro nombre o estado registrado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = matchedUsers,
                        key = { "user_${it.id}" },
                        span = { GridItemSpan(maxLineSpan) }
                    ) { targetUser ->
                        UserSearchResultCard(
                            user = targetUser,
                            isCurrentUser = targetUser.id == currentUserId,
                            showModerationStatus = currentUserIsDeveloper || currentUserRank == AppRank.OWNER,
                            onClick = { selectedUserId = targetUser.id }
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Círculos y salas",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Group,
                                contentDescription = null,
                                tint = NeonPurpleLight,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (communities.isEmpty()) {
                                    "Aún no hay directos creados"
                                } else {
                                    "No encontramos streams con ese filtro"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toca \"+ Iniciar En Vivo\" abajo para abrir tu primer stream IRL o Gamer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { community ->
                    val canDelete = community.myRole == CommunityRole.ADMIN ||
                        currentUserIsDeveloper ||
                        currentUserRank == AppRank.OWNER
                    CommunityCard(
                        community = community,
                        canDelete = canDelete,
                        onOpenVoiceRoom = { onOpenVoiceRoom(community.id) },
                        onOpenGroupChat = { onOpenGroupChat(community.id) },
                        onToggleJoin = { onToggleJoin(community.id) },
                        onRequestDelete = { pendingDeleteCommunity = community }
                    )
                }
            }
        }

        // Bottom CTA bar (non-overlapping so user/stream cards remain fully clickable)
        Surface(
            color = SurfaceDark.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, BorderSubtle.copy(alpha = 0.6f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                NeonGradientButton(
                    text = "+ Iniciar En Vivo",
                    onClick = { showCreateSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "create_community_button"
                )
            }
        }
    }

    if (showCreateSheet) {
        CreateCommunitySheet(
            onPickBackgroundUri = onPickBackgroundUri,
            onDismiss = { showCreateSheet = false },
            onCreate = { name, category, desc, cover, bgUri ->
                onCreateCommunity(name, category, desc, cover, bgUri)
                showCreateSheet = false
            }
        )
    }

    pendingDeleteCommunity?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCommunity = null },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Eliminar sala") },
            text = { Text("¿Seguro que quieres eliminar \"${target.name}\"? Se borrarán sus mensajes y registro de voz.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCommunity(target.id)
                    pendingDeleteCommunity = null
                }) {
                    Text("Eliminar", color = DangerRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCommunity = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    if (showInviteSheet) {
        val currentProfile = allUsers.firstOrNull { it.id == currentUserId }
        com.example.ui.components.InviteWebSheet(
            inviterUsername = currentProfile?.username,
            currentWebUrl = inviteWebUrl,
            canEditGlobalUrl = currentUserIsDeveloper || currentUserRank == AppRank.OWNER,
            onSaveWebUrl = onSaveInviteWebUrl,
            onDismiss = { showInviteSheet = false }
        )
    }

    selectedUserId?.let { targetId ->
        val targetUser = allUsers.firstOrNull { it.id == targetId }
        if (targetUser != null) {
            UserProfilePreviewSheet(
                user = targetUser,
                roleInRoom = null,
                myRoleInRoom = null,
                currentUserRank = currentUserRank,
                currentUserIsDeveloper = currentUserIsDeveloper,
                isCurrentUser = targetUser.id == currentUserId,
                onDismiss = { selectedUserId = null },
                onAssignAppRank = { rank -> onAssignAppRank(targetUser.id, rank) },
                onGrantCoins = { amount -> onGrantCoins(targetUser.id, amount) },
                onBanUser = { reason -> onBanUser(targetUser.id, reason) },
                onUnbanUser = { onUnbanUser(targetUser.id) },
                onFollowUser = { onFollowHost(targetUser.id) }
            )
        }
    }
}

@Composable
private fun UserSearchResultCard(
    user: UserProfile,
    isCurrentUser: Boolean,
    showModerationStatus: Boolean = false,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("explore_user_card_${user.id}"),
        cornerRadius = 18.dp,
        borderColor = if (showModerationStatus && user.isBanned) DangerRose.copy(alpha = 0.6f) else NeonCyan.copy(alpha = 0.35f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FramedAvatar(
                avatarUri = user.avatarUri,
                presetAvatarKey = user.presetAvatarKey,
                frame = user.avatarFrame,
                frameId = user.avatarFrameId,
                size = 46.dp,
                contentDescription = "Perfil de ${user.username}"
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isCurrentUser) "${user.username} (Tú)" else user.username,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (user.appRank != AppRank.NONE) {
                        Spacer(modifier = Modifier.width(5.dp))
                        VerifiedRankBadge(rank = user.appRank, size = 16.dp)
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${user.donorTier.emoji} Nv.${user.donorTier.level}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurpleLight
                    )
                    if (showModerationStatus && user.isBanned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = DangerRose.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, DangerRose)
                        ) {
                            Text(
                                text = "BANEADO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = DangerRose,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = user.customStatus.ifBlank {
                        user.bio.ifBlank { "${user.communitiesCount} círculos • ${user.postsCount} posts" }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ver perfil",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonCyan
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CommunityCard(
    community: Community,
    canDelete: Boolean,
    onOpenVoiceRoom: () -> Unit,
    onOpenGroupChat: () -> Unit,
    onToggleJoin: () -> Unit,
    onRequestDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("community_card_${community.id}"),
        onClick = onOpenVoiceRoom
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                CommunityCoverImage(
                    coverKey = community.coverKey,
                    customBackgroundUri = community.backgroundUri,
                    contentDescription = community.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                if (canDelete) {
                    Surface(
                        shape = CircleShape,
                        color = SurfaceDark.copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, DangerRose.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(30.dp)
                            .clickable(onClick = onRequestDelete)
                            .testTag("delete_community_${community.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar sala", tint = DangerRose, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                // Badge pill ("popular" or "nueva")
                val badgeBrush = if (community.badgeLabel == "popular") {
                    Brush.horizontalGradient(listOf(NeonPink, NeonPurple))
                } else {
                    Brush.horizontalGradient(listOf(NeonPurple, NeonCyan))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(badgeBrush)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = community.badgeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (community.myRole != null) {
                    Box(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)) {
                        RoleBadge(role = community.myRole)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = community.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${community.category} • ${community.memberCount} seguidores",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NeonPink.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable(onClick = onOpenVoiceRoom)
                        .testTag("open_voice_${community.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = "Ver En Vivo", tint = NeonPink, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("En Vivo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable(onClick = onOpenGroupChat)
                        .testTag("open_chat_${community.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat grupal", tint = NeonCyan, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (community.isJoined) SurfaceDark.copy(alpha = 0.7f) else NeonPurple.copy(alpha = 0.20f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clickable(onClick = onToggleJoin)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (community.isJoined) "Siguiendo ✓" else "Seguir canal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (community.isJoined) TextSecondary else NeonPurpleLight
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCommunitySheet(
    onPickBackgroundUri: (Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (name: String, category: String, description: String, coverKey: String, backgroundUri: String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("IRL Urbano") }
    var description by rememberSaveable { mutableStateOf("") }
    var coverKey by rememberSaveable { mutableStateOf("irl") }
    var customBgUri by rememberSaveable { mutableStateOf<String?>(null) }

    val bgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickBackgroundUri(uri) { savedPath -> customBgUri = savedPath }
        }
    }

    val covers = listOf(
        "irl" to "IRL Calle",
        "fps" to "Gamer FPS",
        "gaming" to "RPG & Chill",
        "music" to "IRL Estudio"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Iniciar directo IRL / Gamer",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "Abre tu sala en vivo con cámara IRL, modo Gamer y chat grupal.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Título del directo") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("new_community_name_input")
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Categoría (IRL, Gamer FPS, RPG...)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción del stream") },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Estilo del directo", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(covers) { (key, label) ->
                    val selected = customBgUri == null && coverKey == key
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(104.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) NeonPink else BorderSubtle,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                customBgUri = null
                                coverKey = key
                                category = label
                            }
                            .padding(6.dp)
                    ) {
                        CommunityCoverImage(
                            coverKey = key,
                            contentDescription = label,
                            modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(label, fontSize = 11.sp, color = if (selected) NeonPink else TextSecondary, maxLines = 1)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = NeonPurple.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, NeonPink),
                    modifier = Modifier
                        .height(42.dp)
                        .clickable {
                            bgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .testTag("pick_new_room_bg_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = NeonPink, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Portada personalizada", fontSize = 12.sp, color = TextPrimary)
                    }
                }
                if (customBgUri != null) {
                    TextButton(onClick = { customBgUri = null }) {
                        Text("Quitar foto", color = DangerRose)
                    }
                }
            }
            if (!customBgUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = File(customBgUri!!),
                    contentDescription = "Fondo de sala seleccionado",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(14.dp))
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            NeonGradientButton(
                text = "Transmitir en vivo",
                enabled = name.trim().length >= 3,
                onClick = { onCreate(name, category, description, coverKey, customBgUri) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "confirm_create_community_button"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
