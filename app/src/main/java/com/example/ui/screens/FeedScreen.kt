package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.AppRank
import com.example.model.Community
import com.example.model.CommunityPost
import com.example.model.PostComment
import com.example.model.UserProfile
import com.example.ui.components.FramedAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.TxlLogoMark
import com.example.ui.components.UserProfilePreviewSheet
import com.example.ui.components.VerifiedRankBadge
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerRose
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
import kotlinx.coroutines.flow.Flow
import java.io.File

@Composable
fun FeedScreen(
    posts: List<CommunityPost>,
    communities: List<Community>,
    currentUserId: String,
    currentUserRank: AppRank,
    currentUserIsDeveloper: Boolean = false,
    observeComments: (String) -> Flow<List<PostComment>>,
    onPickPostImage: (Uri, (String) -> Unit) -> Unit,
    onCreatePost: (communityId: String?, content: String, imagePath: String?, onDone: () -> Unit) -> Unit,
    onLikePost: (String) -> Unit,
    onCommentOnPost: (String, String, () -> Unit) -> Unit,
    onDeletePost: (String) -> Unit,
    onAssignAppRank: (String, AppRank) -> Unit,
    onOpenCommunity: (String) -> Unit,
    onGrantCoins: (targetUserId: String, amount: Int) -> Unit = { _, _ -> },
    onBanUser: (String, String) -> Unit = { _, _ -> },
    onUnbanUser: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable { mutableStateOf("Todas") }
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }
    var activePostForComments by remember { mutableStateOf<CommunityPost?>(null) }
    var pendingDeletePost by remember { mutableStateOf<CommunityPost?>(null) }
    var selectedAuthor by remember { mutableStateOf<UserProfile?>(null) }

    val filters = listOf("Todas", "Con foto", "Mis posts")
    val filteredPosts = remember(posts, selectedFilter, currentUserId) {
        when (selectedFilter) {
            "Con foto" -> posts.filter { !it.imageUri.isNullOrBlank() }
            "Mis posts" -> posts.filter { it.author.id == currentUserId }
            else -> posts
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TxlLogoMark(compact = true)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, BorderGlow)
                        ) {
                            Text(
                                text = "${posts.size} publicaciones",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonPink,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Publicaciones",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                                )
                            )
                        }
                    }
                }
            }

            if (filteredPosts.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DynamicFeed,
                                contentDescription = null,
                                tint = NeonPurpleLight,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Sin publicaciones todavía",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toca \"+ Nueva publicación\" abajo para compartir texto o fotos con la comunidad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    val canDelete = post.author.id == currentUserId ||
                        currentUserIsDeveloper ||
                        currentUserRank == AppRank.OWNER
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_card_${post.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.clickable { selectedAuthor = post.author }) {
                                    FramedAvatar(
                                        avatarUri = post.author.avatarUri,
                                        presetAvatarKey = post.author.presetAvatarKey,
                                        frame = post.author.avatarFrame,
                                        frameId = post.author.avatarFrameId,
                                        size = 46.dp,
                                        contentDescription = post.author.username
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = post.author.username,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            modifier = Modifier.clickable { selectedAuthor = post.author }
                                        )
                                        if (post.author.appRank != AppRank.NONE) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerifiedRankBadge(rank = post.author.appRank, size = 16.dp)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatRelativeTime(post.createdAtMillis),
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                        if (!post.communityName.isNullOrBlank() && !post.communityId.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = NeonPurple.copy(alpha = 0.22f),
                                                modifier = Modifier.clickable { onOpenCommunity(post.communityId) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Filled.Tag, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(11.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = post.communityName,
                                                        fontSize = 10.sp,
                                                        color = NeonCyan,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (canDelete) {
                                    IconButton(
                                        onClick = { pendingDeletePost = post },
                                        modifier = Modifier.size(40.dp).testTag("delete_post_${post.id}")
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar publicación", tint = DangerRose, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            if (post.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = post.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }

                            if (!post.imageUri.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                AsyncImage(
                                    model = File(post.imageUri),
                                    contentDescription = "Imagen de publicación",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = SurfaceDark.copy(alpha = 0.72f),
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .clickable { onLikePost(post.id) }
                                        .testTag("like_post_${post.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Favorite, contentDescription = "Me gusta", tint = NeonPink, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${post.likesCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = SurfaceDark.copy(alpha = 0.72f),
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .clickable { activePostForComments = post }
                                        .testTag("comments_post_${post.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comentarios", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${post.commentsCount} comentarios", fontSize = 12.sp, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        NeonGradientButton(
            text = "+ Nueva publicación",
            onClick = { showCreateSheet = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 40.dp, vertical = 16.dp)
                .fillMaxWidth(),
            testTag = "create_post_button"
        )
    }

    if (showCreateSheet) {
        CreatePostSheet(
            communities = communities,
            onPickImage = onPickPostImage,
            onDismiss = { showCreateSheet = false },
            onPublish = { cid, content, imgPath ->
                onCreatePost(cid, content, imgPath) {
                    showCreateSheet = false
                }
            }
        )
    }

    activePostForComments?.let { targetPost ->
        val comments by observeComments(targetPost.id).collectAsStateWithLifecycle(initialValue = emptyList())
        PostCommentsSheet(
            post = targetPost,
            comments = comments,
            onDismiss = { activePostForComments = null },
            onSendComment = { text, onSent ->
                onCommentOnPost(targetPost.id, text, onSent)
            },
            onAuthorClick = { selectedAuthor = it }
        )
    }

    pendingDeletePost?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeletePost = null },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Eliminar publicación") },
            text = { Text("¿Seguro que deseas eliminar esta publicación permanentemente?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePost(target.id)
                    pendingDeletePost = null
                }) {
                    Text("Eliminar", color = DangerRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePost = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    selectedAuthor?.let { author ->
        UserProfilePreviewSheet(
            user = author,
            roleInRoom = null,
            myRoleInRoom = null,
            currentUserRank = currentUserRank,
            currentUserIsDeveloper = currentUserIsDeveloper,
            isCurrentUser = author.id == currentUserId,
            onDismiss = { selectedAuthor = null },
            onAssignAppRank = { rank -> onAssignAppRank(author.id, rank) },
            onGrantCoins = { amount -> onGrantCoins(author.id, amount) },
            onBanUser = { reason -> onBanUser(author.id, reason) },
            onUnbanUser = { onUnbanUser(author.id) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostSheet(
    communities: List<Community>,
    onPickImage: (Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onPublish: (communityId: String?, content: String, imagePath: String?) -> Unit
) {
    var content by rememberSaveable { mutableStateOf("") }
    var imagePath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCommunityId by rememberSaveable { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickImage(uri) { savedPath -> imagePath = savedPath }
        }
    }

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
            Text("Nueva publicación", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= 500) content = it },
                label = { Text("¿Qué quieres compartir hoy? (${content.length}/500)") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("post_content_input")
            )

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
                        .height(44.dp)
                        .clickable {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .testTag("pick_post_image_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = NeonPink, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (imagePath == null) "Agregar foto" else "Cambiar foto", fontSize = 13.sp, color = TextPrimary)
                    }
                }
                if (imagePath != null) {
                    TextButton(onClick = { imagePath = null }) {
                        Text("Quitar foto", color = DangerRose)
                    }
                }
            }

            if (!imagePath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = File(imagePath!!),
                    contentDescription = "Vista previa",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            if (communities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text("Etiquetar comunidad (opcional):", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCommunityId == null,
                            onClick = { selectedCommunityId = null },
                            label = { Text("General") }
                        )
                    }
                    items(communities, key = { it.id }) { comm ->
                        FilterChip(
                            selected = selectedCommunityId == comm.id,
                            onClick = { selectedCommunityId = comm.id },
                            label = { Text(comm.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            NeonGradientButton(
                text = "Publicar ahora",
                enabled = content.trim().isNotEmpty() || !imagePath.isNullOrBlank(),
                onClick = { onPublish(selectedCommunityId, content, imagePath) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "confirm_create_post_button"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCommentsSheet(
    post: CommunityPost,
    comments: List<PostComment>,
    onDismiss: () -> Unit,
    onSendComment: (String, () -> Unit) -> Unit,
    onAuthorClick: (UserProfile) -> Unit
) {
    var commentInput by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Comentarios (${comments.size})", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (comments.isEmpty()) {
                Text(
                    text = "Aún no hay comentarios. ¡Sé el primero en opinar!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(modifier = Modifier.clickable { onAuthorClick(comment.author) }) {
                                    FramedAvatar(
                                        avatarUri = comment.author.avatarUri,
                                        presetAvatarKey = comment.author.presetAvatarKey,
                                        frame = comment.author.avatarFrame,
                                        frameId = comment.author.avatarFrameId,
                                        size = 36.dp,
                                        contentDescription = comment.author.username
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(comment.author.username, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                                        if (comment.author.appRank != AppRank.NONE) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerifiedRankBadge(rank = comment.author.appRank, size = 13.dp)
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(formatRelativeTime(comment.createdAtMillis), fontSize = 10.sp, color = TextMuted)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(comment.content, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { if (it.length <= 280) commentInput = it },
                    placeholder = { Text("Escribe un comentario...", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPink,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f).testTag("post_comment_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NeonPurple, NeonPink)))
                        .clickable {
                            if (commentInput.isNotBlank()) {
                                onSendComment(commentInput) { commentInput = "" }
                            }
                        }
                        .testTag("post_comment_send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Comentar", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
