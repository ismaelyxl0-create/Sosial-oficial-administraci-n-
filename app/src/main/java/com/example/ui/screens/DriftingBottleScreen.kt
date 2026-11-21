package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DriftingBottle
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.NeonOutlinedButton
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DriftingBottleScreen(
    activeBottles: List<DriftingBottle>,
    pickedBottle: DriftingBottle?,
    onLaunchBottle: (moodTag: String, content: String) -> Unit,
    onPickBottle: () -> Unit,
    onReplyToBottle: (bottleId: String, reply: String) -> Unit,
    onDismissPickedBottle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLaunchSheet by rememberSaveable { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "bottle_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Drifting Bottle",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Envía un mensaje anónimo al azar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Glowing bottle centerpiece matching Mockup #4
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .offset(y = floatOffset.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_stream_irl_tokyo_1795022688753),
                        contentDescription = "Botella luminosa a la deriva",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth(0.76f)
                            .height(280.dp)
                            .clip(RoundedCornerShape(28.dp))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = NeonPurpleLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Los mensajes se eliminan después de 24 horas",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeonGradientButton(
                        text = "Lanzar botella",
                        onClick = { showLaunchSheet = true },
                        modifier = Modifier.weight(1f),
                        testTag = "launch_bottle_button"
                    )
                    NeonOutlinedButton(
                        text = "Recoger botella",
                        onClick = onPickBottle,
                        modifier = Modifier.weight(1f),
                        testTag = "pick_bottle_button"
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Botellas activas en el océano",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = NeonPurple.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, BorderGlow)
                ) {
                    Text(
                        text = "${activeBottles.size} activas",
                        fontSize = 11.sp,
                        color = NeonPink,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (activeBottles.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "El océano neón está vacío",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Aún no hay mensajes anónimos reales. ¡Lanza la primera botella!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeBottles.take(6), key = { it.id }) { bottle ->
            val hoursLeft = ((bottle.expiresAtMillis - System.currentTimeMillis()) / 3_600_000L).coerceAtLeast(1L)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = NeonPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(bottle.anonymousAlias, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SurfaceElevated
                        ) {
                            Text(
                                text = "${bottle.moodTag} • ${hoursLeft}h restantes",
                                fontSize = 10.sp,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (bottle.isPickedByMe) bottle.content else "Mensaje sellado en el océano • Toca \"Recoger botella\" para descubrir mensajes al azar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (bottle.isPickedByMe) TextPrimary else TextSecondary
                    )
                    if (!bottle.replyContent.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = NeonPurple.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Respuesta anónima: ${bottle.replyContent}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonPink,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLaunchSheet) {
        LaunchBottleSheet(
            onDismiss = { showLaunchSheet = false },
            onSend = { mood, text ->
                onLaunchBottle(mood, text)
                showLaunchSheet = false
            }
        )
    }

    if (pickedBottle != null) {
        PickedBottleDialog(
            bottle = pickedBottle,
            onReply = { reply -> onReplyToBottle(pickedBottle.id, reply) },
            onDismiss = onDismissPickedBottle
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LaunchBottleSheet(
    onDismiss: () -> Unit,
    onSend: (String, String) -> Unit
) {
    var content by rememberSaveable { mutableStateOf("") }
    var selectedMood by rememberSaveable { mutableStateOf("Reflexión nocturna") }
    val moods = listOf("Reflexión nocturna", "Música & Vibra", "Confesión", "Consejo", "Saludo")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        contentColor = TextPrimary
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            Text("Lanzar Drifting Bottle", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Tu identidad permanecerá anónima y la botella desaparecerá en 24 horas.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(moods) { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = { selectedMood = mood },
                        label = { Text(mood) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonPink,
                            selectedLabelColor = TextPrimary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= 280) content = it },
                label = { Text("Escribe tu mensaje anónimo...") },
                minLines = 4,
                maxLines = 6,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().testTag("bottle_message_input")
            )
            Text(
                text = "${content.length}/280",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            NeonGradientButton(
                text = "Lanzar al océano",
                enabled = content.trim().length >= 4,
                onClick = { onSend(selectedMood, content) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "confirm_launch_bottle_button"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PickedBottleDialog(
    bottle: DriftingBottle,
    onReply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var replyText by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = NeonPink)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(bottle.anonymousAlias, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${bottle.moodTag} • hace ${formatRelativeTime(bottle.createdAtMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
            }
        },
        text = {
            Column {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(NeonPurple, NeonPink)))
                ) {
                    Text(
                        text = bottle.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Deja una respuesta anónima...") },
                    trailingIcon = {
                        if (replyText.isNotBlank()) {
                            TextButton(onClick = { onReply(replyText); replyText = "" }) {
                                Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = NeonPink)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = NeonPink, fontWeight = FontWeight.Bold)
            }
        }
    )
}
