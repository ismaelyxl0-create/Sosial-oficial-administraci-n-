package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppNotification
import com.example.model.NotificationCategory
import com.example.ui.components.FramedAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onMarkRead: (Long) -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenCommunity: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by rememberSaveable { mutableStateOf("Todas") }
    val categories = listOf("Todas", "Seguimiento", "Mención", "Comunidad", "Drifting Bottle")

    val filtered = remember(notifications, selectedCategory) {
        if (selectedCategory == "Todas") notifications
        else notifications.filter { it.category.label == selectedCategory }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notificaciones",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SurfaceElevated,
                        border = BorderStroke(1.dp, BorderGlow),
                        modifier = Modifier
                            .clickable(onClick = onMarkAllRead)
                            .testTag("mark_all_read_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.DoneAll, contentDescription = null, tint = NeonPink, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Marcar todas como leídas", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val selected = selectedCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
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

        if (filtered.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.NotificationsNone, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Sin notificaciones en esta categoría", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { notif ->
                val pillBrush = when (notif.category) {
                    NotificationCategory.FOLLOW -> Brush.horizontalGradient(listOf(NeonPurple, NeonPink))
                    NotificationCategory.MENTION -> Brush.horizontalGradient(listOf(NeonPink, NeonPurple))
                    NotificationCategory.COMMUNITY -> Brush.horizontalGradient(listOf(NeonPurple, NeonCyan))
                    NotificationCategory.DRIFTING_BOTTLE -> Brush.horizontalGradient(listOf(NeonCyan, NeonPink))
                }
                GlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("notification_card_${notif.id}"),
                    borderColor = if (!notif.isRead) NeonPink.copy(alpha = 0.7f) else BorderSubtle,
                    onClick = {
                        onMarkRead(notif.id)
                        notif.targetCommunityId?.let(onOpenCommunity)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FramedAvatar(
                            avatarUri = notif.actorAvatarUri,
                            presetAvatarKey = notif.actorAvatarKey,
                            frame = notif.actorFrame,
                            size = 48.dp,
                            contentDescription = notif.actorName
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = notif.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!notif.isRead) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(NeonPink)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notif.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(pillBrush)
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = notif.category.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = formatRelativeTime(notif.timestampMillis),
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
