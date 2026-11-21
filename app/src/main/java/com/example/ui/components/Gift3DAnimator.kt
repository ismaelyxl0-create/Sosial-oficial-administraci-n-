package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GiftAnimationStyle
import com.example.model.GiftCatalog
import com.example.model.GiftItem
import com.example.model.RoomGiftEvent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RoleAdminGold
import com.example.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders an individual gift icon with its own hardware-accelerated 3D perspective motion
 * (`cameraDistance`, `rotationX`, `rotationY`, `rotationZ`, `scale`) matching its animationStyle.
 */
@Composable
fun Gift3DAnimatedArtwork(
    gift: GiftItem,
    size: Dp,
    intense: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "gift_3d_${gift.id}")
    val cycleMs = when (gift.animationStyle) {
        GiftAnimationStyle.HEART_BEAT_ARROW, GiftAnimationStyle.FINGER_HEART_POP -> 950
        GiftAnimationStyle.MONEY_GUN_BLAST, GiftAnimationStyle.CHILI_FIRE_BURST -> 750
        GiftAnimationStyle.FOOTBALL_SPIN_BOUNCE, GiftAnimationStyle.CROWN_ROYAL_ORBIT -> 1800
        GiftAnimationStyle.PARIS_EIFFEL_SHOW, GiftAnimationStyle.STARLIGHT_PORTAL -> 2600
        else -> 2100
    }
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(cycleMs, easing = LinearEasing), RepeatMode.Restart),
        label = "phase_${gift.id}"
    )
    val wave by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(cycleMs / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wave_${gift.id}"
    )

    val amp = if (intense) 1.65f else 1.0f
    val accent = Color(gift.accentHex)
    val rad = phase * 2f * PI.toFloat()

    // Compute per-gift 3D transform parameters
    var rotX = 0f
    var rotY = 0f
    var rotZ = 0f
    var scale = 1f
    var transY = 0f
    var transX = 0f

    when (gift.animationStyle) {
        GiftAnimationStyle.ROSE_PETAL_SPIN -> {
            rotY = sin(rad) * 26f * amp
            rotZ = cos(rad) * 8f * amp
            transY = -sin(rad * 2f) * 3.5f * amp
            scale = 1f + wave * 0.08f * amp
        }
        GiftAnimationStyle.MONEY_GUN_BLAST -> {
            rotZ = -14f + sin(rad * 2f) * 10f * amp
            rotY = -22f + cos(rad) * 14f * amp
            transX = -sin(rad * 2f) * 4f * amp
            scale = 1.02f + wave * 0.12f * amp
        }
        GiftAnimationStyle.HEART_BEAT_ARROW -> {
            val beat = abs(sin(rad))
            scale = 0.94f + beat * 0.22f * amp
            rotY = sin(rad) * 18f * amp
            rotX = cos(rad) * 10f * amp
        }
        GiftAnimationStyle.HEART_FLAME_BLAZE -> {
            scale = 0.96f + wave * 0.18f * amp
            rotY = sin(rad * 1.5f) * 24f * amp
            rotZ = sin(rad * 3f) * 6f * amp
            transY = -wave * 4f * amp
        }
        GiftAnimationStyle.HEART_PUFF_FLOAT -> {
            transY = -sin(rad) * 5f * amp
            rotX = sin(rad) * 16f * amp
            rotY = cos(rad) * 16f * amp
            scale = 0.97f + wave * 0.10f * amp
        }
        GiftAnimationStyle.FINGER_HEART_POP -> {
            rotY = sin(rad) * 20f * amp
            rotZ = -6f + wave * 12f * amp
            scale = 0.95f + wave * 0.14f * amp
        }
        GiftAnimationStyle.CROWN_ROYAL_ORBIT -> {
            rotY = phase * 360f
            rotX = 14f * amp
            transY = -wave * 4f * amp
            scale = 1.0f + wave * 0.10f * amp
        }
        GiftAnimationStyle.PARIS_EIFFEL_SHOW -> {
            rotY = sin(rad) * 28f * amp
            rotX = 12f
            scale = 0.98f + wave * 0.12f * amp
        }
        GiftAnimationStyle.SNOW_GLOBE_SWIRL -> {
            rotZ = sin(rad) * 12f * amp
            rotY = cos(rad) * 24f * amp
            rotX = sin(rad) * 12f * amp
            scale = 1f + wave * 0.09f * amp
        }
        GiftAnimationStyle.RAINBOW_SWEEP -> {
            rotY = sin(rad) * 22f * amp
            rotZ = sin(rad) * 10f * amp
            transY = -wave * 4f * amp
        }
        GiftAnimationStyle.MUSIC_SPHERE_PULSE -> {
            rotY = phase * 360f
            scale = 0.92f + wave * 0.18f * amp
            rotX = sin(rad) * 15f * amp
        }
        GiftAnimationStyle.NEON_PRISM_EXPLODE -> {
            rotY = sin(rad) * 32f * amp
            rotX = cos(rad) * 20f * amp
            scale = 0.95f + wave * 0.15f * amp
        }
        GiftAnimationStyle.PERFUME_MIST_SPRAY -> {
            rotZ = -8f + wave * 16f * amp
            rotY = sin(rad) * 18f * amp
            scale = 1f + wave * 0.08f * amp
        }
        GiftAnimationStyle.CHILI_FIRE_BURST -> {
            rotZ = sin(rad * 4f) * 14f * amp
            rotY = cos(rad * 2f) * 20f * amp
            scale = 0.98f + wave * 0.14f * amp
        }
        GiftAnimationStyle.FOOTBALL_SPIN_BOUNCE -> {
            rotZ = phase * 360f
            rotY = sin(rad) * 22f * amp
            transY = -abs(sin(rad)) * 7f * amp
        }
        GiftAnimationStyle.CAT_CUTE_HOP -> {
            transY = -abs(sin(rad)) * 6.5f * amp
            rotZ = sin(rad) * 10f * amp
            rotY = cos(rad) * 18f * amp
        }
        GiftAnimationStyle.SUNBEAM_RADIANCE -> {
            rotY = sin(rad) * 20f * amp
            rotZ = sin(rad) * 8f * amp
            scale = 0.97f + wave * 0.12f * amp
        }
        GiftAnimationStyle.SWEET_3D_FLIP -> {
            rotX = sin(rad) * 24f * amp
            rotY = cos(rad) * 28f * amp
            transY = -wave * 3.5f * amp
        }
        GiftAnimationStyle.STARLIGHT_PORTAL -> {
            rotY = phase * 360f * 0.5f
            rotX = 16f * sin(rad) * amp
            scale = 0.96f + wave * 0.14f * amp
        }
    }

    val particleEmoji = when (gift.animationStyle) {
        GiftAnimationStyle.ROSE_PETAL_SPIN -> "🌸"
        GiftAnimationStyle.MONEY_GUN_BLAST -> "💵"
        GiftAnimationStyle.HEART_FLAME_BLAZE, GiftAnimationStyle.CHILI_FIRE_BURST -> "🔥"
        GiftAnimationStyle.SNOW_GLOBE_SWIRL -> "❄️"
        GiftAnimationStyle.MUSIC_SPHERE_PULSE -> "🎶"
        GiftAnimationStyle.PERFUME_MIST_SPRAY -> "✨"
        GiftAnimationStyle.CROWN_ROYAL_ORBIT, GiftAnimationStyle.PARIS_EIFFEL_SHOW,
        GiftAnimationStyle.STARLIGHT_PORTAL -> "⭐"
        else -> "✨"
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 3D volumetric backdrop glow & orbiting particles canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accent.copy(alpha = if (intense) 0.55f else 0.34f), Color.Transparent)
                ),
                radius = r
            )
            // 3D tilt orbital ellipse ring
            drawCircle(
                color = accent.copy(alpha = (0.28f + wave * 0.25f).coerceIn(0f, 0.7f)),
                radius = r * (0.68f + wave * 0.14f),
                style = Stroke(width = if (intense) 3.5f else 1.8f)
            )
            // Orbiting micro sparkles in 3D elliptical arc
            val count = if (intense) 8 else 3
            for (i in 0 until count) {
                val theta = rad + (i * (2f * PI.toFloat() / count))
                val px = center.x + cos(theta) * r * 0.82f
                val py = center.y + sin(theta) * r * 0.38f
                drawCircle(
                    color = if (i % 2 == 0) Color.White else accent,
                    radius = (if (intense) 4.2f else 2.0f) * (0.7f + 0.5f * ((sin(theta) + 1f) / 2f)),
                    center = Offset(px, py)
                )
            }
        }

        // Floating secondary micro-particle specific to each gift
        val particleAlpha = (0.35f + wave * 0.65f).coerceIn(0f, 1f)
        Text(
            text = particleEmoji,
            fontSize = if (intense) 22.sp else 11.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .graphicsLayer {
                    alpha = particleAlpha
                    translationY = -wave * (if (intense) 18f else 5f)
                    translationX = cos(rad) * (if (intense) 12f else 3f)
                    rotationZ = sin(rad) * 20f
                }
        )

        if (intense) {
            Text(
                text = particleEmoji,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer {
                        alpha = particleAlpha
                        translationY = -((1f - wave) * 16f)
                        translationX = -sin(rad) * 14f
                        rotationZ = -cos(rad) * 22f
                    }
            )
        }

        // Main 3D perspective hero emoji
        Text(
            text = gift.emoji,
            fontSize = (size.value * 0.56f).sp,
            modifier = Modifier.graphicsLayer {
                cameraDistance = 12f * density
                rotationX = rotX
                rotationY = rotY
                rotationZ = rotZ
                scaleX = scale
                scaleY = scale
                translationY = transY * density
                translationX = transX * density
            }
        )
    }
}

/**
 * Full 3D Gift Celebration Theater banner shown inside Voice Rooms and Group Chats when someone
 * donates a gift — runs the gift's individual 3D perspective animation and particle burst.
 */
@Composable
fun GiftCelebrationOverlay(
    event: RoomGiftEvent?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = event != null,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        if (event != null) {
            val catalogItem = GiftCatalog.byId(event.giftId).let { found ->
                if (found.id == event.giftId) found
                else GiftItem(
                    id = event.giftId,
                    name = event.giftName,
                    emoji = event.giftEmoji,
                    coinCost = event.coinCost,
                    category = "Mi bolsa",
                    accentHex = 0xFFFF4EC2,
                    animationStyle = GiftAnimationStyle.NEON_PRISM_EXPLODE
                )
            }
            val accent = Color(catalogItem.accentHex)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("gift_celebration_banner"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF1A0F2E).copy(alpha = 0.95f),
                    border = BorderStroke(
                        width = 1.8.dp,
                        brush = Brush.horizontalGradient(listOf(RoleAdminGold, accent, NeonCyan))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FramedAvatar(
                            avatarUri = null,
                            presetAvatarKey = event.senderAvatarKey,
                            frameId = event.senderFrameId,
                            size = 40.dp,
                            contentDescription = event.senderName
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${event.senderName} donó ${event.giftName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Para ${event.recipientName} • Efecto 3D",
                                fontSize = 11.sp,
                                color = NeonCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Gift3DAnimatedArtwork(
                            gift = catalogItem,
                            size = 64.dp,
                            intense = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = NeonPink
                        ) {
                            Text(
                                text = "×${event.comboCount}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
