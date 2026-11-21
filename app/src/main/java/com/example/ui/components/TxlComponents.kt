package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.staticCompositionLocalOf
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AppRank
import com.example.model.AvatarFrame
import com.example.model.CommunityRole
import com.example.model.CustomFrame
import com.example.model.CustomFrameStyle
import com.example.model.UserProfile
import com.example.ui.theme.BorderGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerRose
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.RoleAdminGold
import com.example.ui.theme.RoleCoAdminCyan
import com.example.ui.theme.RoleMemberLilac
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val LocalCustomFrames = staticCompositionLocalOf<Map<String, CustomFrame>> { emptyMap() }

@Composable
fun TxlBackdrop(
    neonGlowEnabled: Boolean = true,
    backgroundUri: String? = null,
    backgroundDim: Float = 0.62f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepVoid)
    ) {
        if (!backgroundUri.isNullOrBlank()) {
            AsyncImage(
                model = File(backgroundUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepVoid.copy(alpha = backgroundDim.coerceIn(0.15f, 0.90f)))
            )
        }
        if (neonGlowEnabled) {
            val glowScale = if (!backgroundUri.isNullOrBlank()) 0.65f else 1f
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonPurple.copy(alpha = 0.24f * glowScale), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.width * 0.7f
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.85f, size.height * 0.12f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonPink.copy(alpha = 0.16f * glowScale), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.78f),
                        radius = size.width * 0.65f
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(size.width * 0.12f, size.height * 0.78f)
                )
            }
        }
        content()
    }
}

@Composable
fun TxlLogoMark(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val iconSize = if (compact) 30.dp else 40.dp
        Canvas(modifier = Modifier.size(iconSize)) {
            val w = size.width
            val h = size.height
            val hex = Path().apply {
                moveTo(w * 0.5f, h * 0.06f)
                lineTo(w * 0.92f, h * 0.30f)
                lineTo(w * 0.92f, h * 0.74f)
                lineTo(w * 0.5f, h * 0.94f)
                lineTo(w * 0.08f, h * 0.74f)
                lineTo(w * 0.08f, h * 0.30f)
                close()
            }
            drawPath(
                path = hex,
                brush = Brush.linearGradient(listOf(NeonPink, NeonPurple))
            )
            // Inner soundwave bars
            val barXs = listOf(0.32f, 0.44f, 0.56f, 0.68f)
            val barHeights = listOf(0.28f, 0.46f, 0.38f, 0.24f)
            barXs.forEachIndexed { idx, xFrac ->
                val halfBar = h * barHeights[idx] * 0.5f
                drawLine(
                    color = DeepVoid,
                    start = Offset(w * xFrac, h * 0.52f - halfBar),
                    end = Offset(w * xFrac, h * 0.52f + halfBar),
                    strokeWidth = w * 0.075f,
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))
        Text(
            text = "NexoLive",
            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
    }
}

@Composable
fun NeonGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = ""
) {
    val brush = Brush.horizontalGradient(
        colors = if (enabled) listOf(NeonPurple, NeonPink)
        else listOf(SurfaceElevated, SurfaceElevated)
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .height(52.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) TextPrimary else TextSecondary
            )
        }
    }
}

@Composable
fun NeonOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonPurple, NeonPink))),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceCard.copy(alpha = 0.7f),
            contentColor = TextPrimary
        ),
        modifier = modifier
            .height(52.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    borderColor: Color = BorderGlow,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = SurfaceCard.copy(alpha = 0.88f),
        shape = shape,
        tonalElevation = 4.dp
    ) {
        Column(content = content)
    }
}

fun presetDrawableForKey(key: String): Int = when (key) {
    "fps" -> R.drawable.img_stream_fps_1795022688314
    "gaming" -> R.drawable.img_stream_rpg_chill_1795022690411
    "music" -> R.drawable.img_stream_irl_music_1795022689303
    else -> R.drawable.img_stream_irl_tokyo_1795022688753
}

@Composable
fun CommunityCoverImage(
    coverKey: String,
    contentDescription: String,
    customBackgroundUri: String? = null,
    modifier: Modifier = Modifier
) {
    if (!customBackgroundUri.isNullOrBlank()) {
        AsyncImage(
            model = File(customBackgroundUri),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            error = painterResource(id = presetDrawableForKey(coverKey)),
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = presetDrawableForKey(coverKey)),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun FramedAvatar(
    avatarUri: String?,
    presetAvatarKey: String,
    frame: AvatarFrame = AvatarFrame.NEON_PULSE,
    size: Dp = 56.dp,
    isSpeaking: Boolean = false,
    speakingAmplitude: Float = 0f,
    contentDescription: String = "Avatar de usuario",
    modifier: Modifier = Modifier,
    frameId: String? = null,
    customFrameOverride: CustomFrame? = null
) {
    val customFrames = LocalCustomFrames.current
    val resolvedCustom = customFrameOverride ?: frameId?.let { customFrames[it] }
    val resolvedBuiltIn = if (resolvedCustom == null) {
        if (!frameId.isNullOrBlank() && !frameId.startsWith("custom_frame_")) {
            AvatarFrame.fromId(frameId)
        } else {
            frame
        }
    } else {
        AvatarFrame.NONE
    }

    val infiniteTransition = rememberInfiniteTransition(label = "frame_anim")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frame_rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val isPngOverlay = resolvedCustom != null && resolvedCustom.frameType == "PNG" && !resolvedCustom.pngPath.isNullOrBlank()
    val outerSize = if (isPngOverlay) size + 28.dp else size + 16.dp
    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        // Optional speaking acoustic ring
        if (isSpeaking) {
            val dynamicAlpha = (0.35f + speakingAmplitude * 0.5f).coerceIn(0.3f, 0.85f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            NeonPink.copy(alpha = dynamicAlpha),
                            NeonPurple.copy(alpha = dynamicAlpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    radius = (this.size.minDimension / 2f) * pulse
                )
            }
        }

        // Studio or built-in Canvas frame behind/around the avatar
        if (resolvedCustom != null && resolvedCustom.frameType != "PNG") {
            CustomStudioFrameCanvas(
                custom = resolvedCustom,
                angle = angle,
                size = size
            )
        } else if (resolvedCustom == null) {
            Canvas(modifier = Modifier.size(size + 8.dp)) {
                val strokePx = (size.toPx() * 0.085f).coerceIn(3f, 9f)
                val radius = (this.size.minDimension - strokePx) / 2f
                when (resolvedBuiltIn) {
                    AvatarFrame.NONE -> {
                        drawCircle(
                            color = BorderGlow,
                            radius = radius,
                            style = Stroke(width = 2f)
                        )
                    }
                    AvatarFrame.NEON_PULSE -> {
                        rotate(angle * 0.5f) {
                            drawCircle(
                                brush = Brush.sweepGradient(listOf(NeonPurple, NeonPink, NeonPurpleLight, NeonPurple)),
                                radius = radius,
                                style = Stroke(width = strokePx)
                            )
                        }
                    }
                    AvatarFrame.CYBER_PRISM -> {
                        rotate(angle) {
                            val segSweep = 48f
                            for (i in 0 until 6) {
                                val start = i * 60f
                                val color = if (i % 2 == 0) NeonCyan else NeonPurple
                                drawArc(
                                    color = color,
                                    startAngle = start,
                                    sweepAngle = segSweep,
                                    useCenter = false,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                    AvatarFrame.STARLIGHT_HALO -> {
                        drawCircle(
                            brush = Brush.linearGradient(listOf(NeonPink, RoleAdminGold, NeonPurple)),
                            radius = radius,
                            style = Stroke(width = strokePx * 0.85f)
                        )
                        val cx = this.size.width / 2f
                        val cy = this.size.height / 2f
                        for (i in 0 until 4) {
                            val rad = ((angle + i * 90f) * PI / 180.0)
                            val sx = cx + cos(rad).toFloat() * radius
                            val sy = cy + sin(rad).toFloat() * radius
                            drawCircle(
                                color = Color.White,
                                radius = strokePx * 0.7f,
                                center = Offset(sx, sy)
                            )
                            drawCircle(
                                color = NeonPink,
                                radius = strokePx * 0.45f,
                                center = Offset(sx, sy)
                            )
                        }
                    }
                    AvatarFrame.VOID_FLAME -> {
                        rotate(-angle) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    listOf(NeonPink, Color(0xFF7209B7), NeonCyan, NeonPink)
                                ),
                                radius = radius,
                                style = Stroke(width = strokePx * 1.15f)
                            )
                        }
                        drawCircle(
                            color = NeonPurpleLight.copy(alpha = 0.45f),
                            radius = radius - strokePx * 0.8f,
                            style = Stroke(width = 2f)
                        )
                    }
                    AvatarFrame.ROYAL_CROWN -> {
                        drawCircle(
                            brush = Brush.linearGradient(listOf(RoleAdminGold, NeonPink, RoleAdminGold)),
                            radius = radius,
                            style = Stroke(width = strokePx)
                        )
                        val cx = this.size.width / 2f
                        val topY = strokePx * 0.3f
                        val crest = Path().apply {
                            moveTo(cx - radius * 0.34f, topY + strokePx * 1.2f)
                            lineTo(cx - radius * 0.24f, topY - strokePx * 0.3f)
                            lineTo(cx - radius * 0.08f, topY + strokePx * 0.6f)
                            lineTo(cx, topY - strokePx * 0.7f)
                            lineTo(cx + radius * 0.08f, topY + strokePx * 0.6f)
                            lineTo(cx + radius * 0.24f, topY - strokePx * 0.3f)
                            lineTo(cx + radius * 0.34f, topY + strokePx * 1.2f)
                            close()
                        }
                        drawPath(crest, color = RoleAdminGold)
                    }
                }
            }
        }

        // Actual user image (scaled slightly inside transparent PNG frame cutout)
        val innerAvatarSize = if (isPngOverlay) size * 0.84f else size
        val imageModifier = Modifier
            .size(innerAvatarSize)
            .clip(CircleShape)
            .background(SurfaceElevated)

        if (!avatarUri.isNullOrBlank()) {
            AsyncImage(
                model = File(avatarUri),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                error = painterResource(id = presetDrawableForKey(presetAvatarKey)),
                modifier = imageModifier
            )
        } else {
            Image(
                painter = painterResource(id = presetDrawableForKey(presetAvatarKey)),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
        }

        // Transparent PNG frame overlay on top (supports oval/ornate character & emblem frames)
        if (isPngOverlay && resolvedCustom != null) {
            AsyncImage(
                model = File(resolvedCustom.pngPath!!),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size + 26.dp)
            )
        }
    }
}

@Composable
private fun CustomStudioFrameCanvas(
    custom: CustomFrame,
    angle: Float,
    size: Dp
) {
    val primary = Color(custom.primaryColorHex)
    val secondary = Color(custom.secondaryColorHex)
    Canvas(modifier = Modifier.size(size + 10.dp)) {
        val scaleFactor = (size.value / 56f).coerceIn(0.65f, 1.8f)
        val strokePx = (custom.thicknessDp.dp.toPx() * scaleFactor).coerceIn(3f, 16f)
        val radius = (this.size.minDimension - strokePx) / 2f
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f

        when (custom.stylePattern) {
            CustomFrameStyle.RING_GLOW -> {
                rotate(angle * 0.5f) {
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(primary, secondary, primary.copy(alpha = 0.7f), primary)),
                        radius = radius,
                        style = Stroke(width = strokePx)
                    )
                }
            }
            CustomFrameStyle.DOUBLE_ORBIT -> {
                rotate(angle * 0.6f) {
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(primary, secondary, primary)),
                        radius = radius,
                        style = Stroke(width = strokePx * 0.75f)
                    )
                }
                rotate(-angle * 0.8f) {
                    drawCircle(
                        color = secondary.copy(alpha = 0.75f),
                        radius = (radius - strokePx * 0.85f).coerceAtLeast(1f),
                        style = Stroke(width = (strokePx * 0.45f).coerceAtLeast(2f))
                    )
                }
            }
            CustomFrameStyle.NEON_DASH -> {
                rotate(angle) {
                    for (i in 0 until 6) {
                        val color = if (i % 2 == 0) primary else secondary
                        drawArc(
                            color = color,
                            startAngle = i * 60f,
                            sweepAngle = 42f,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }
            }
            CustomFrameStyle.STAR_BURST -> {
                val starPath = buildStarburstPath(cx, cy, radius + strokePx * 0.35f, radius - strokePx * 0.35f, points = 16)
                rotate(angle * 0.25f) {
                    drawPath(
                        path = starPath,
                        brush = Brush.linearGradient(listOf(primary, secondary)),
                        style = Stroke(width = strokePx * 0.75f, join = StrokeJoin.Round)
                    )
                }
            }
            CustomFrameStyle.CROWN_SPIKES -> {
                drawCircle(
                    brush = Brush.linearGradient(listOf(primary, secondary, primary)),
                    radius = radius,
                    style = Stroke(width = strokePx * 0.9f)
                )
                val topY = strokePx * 0.35f
                val crest = Path().apply {
                    moveTo(cx - radius * 0.36f, topY + strokePx * 1.15f)
                    lineTo(cx - radius * 0.22f, topY - strokePx * 0.35f)
                    lineTo(cx - radius * 0.08f, topY + strokePx * 0.55f)
                    lineTo(cx, topY - strokePx * 0.75f)
                    lineTo(cx + radius * 0.08f, topY + strokePx * 0.55f)
                    lineTo(cx + radius * 0.22f, topY - strokePx * 0.35f)
                    lineTo(cx + radius * 0.36f, topY + strokePx * 1.15f)
                    close()
                }
                drawPath(crest, brush = Brush.linearGradient(listOf(primary, secondary)))
            }
            CustomFrameStyle.CYBER_HEX -> {
                val hexPath = Path()
                for (i in 0 until 6) {
                    val theta = (PI * i / 3.0) - PI / 2.0
                    val x = cx + radius * cos(theta).toFloat()
                    val y = cy + radius * sin(theta).toFloat()
                    if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                }
                hexPath.close()
                rotate(angle * 0.2f) {
                    drawPath(
                        path = hexPath,
                        brush = Brush.linearGradient(listOf(primary, secondary)),
                        style = Stroke(width = strokePx, join = StrokeJoin.Round)
                    )
                }
            }
        }

        if (custom.hasSparkles) {
            for (i in 0 until 4) {
                val rad = ((angle + i * 90f) * PI / 180.0)
                val sx = cx + cos(rad).toFloat() * radius
                val sy = cy + sin(rad).toFloat() * radius
                drawCircle(color = Color.White, radius = (strokePx * 0.58f).coerceAtLeast(2.5f), center = Offset(sx, sy))
                drawCircle(color = secondary, radius = (strokePx * 0.35f).coerceAtLeast(1.5f), center = Offset(sx, sy))
            }
        }
    }
}

@Composable
fun RoleBadge(
    role: CommunityRole,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = when (role) {
        CommunityRole.ADMIN -> Triple(RoleAdminGold.copy(alpha = 0.20f), RoleAdminGold, Icons.Filled.AdminPanelSettings)
        CommunityRole.CO_ADMIN -> Triple(RoleCoAdminCyan.copy(alpha = 0.20f), RoleCoAdminCyan, Icons.Filled.Shield)
        CommunityRole.MEMBER -> Triple(RoleMemberLilac.copy(alpha = 0.16f), RoleMemberLilac, Icons.Filled.Star)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = role.label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VerifiedRankBadge(
    rank: AppRank,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    if (rank == AppRank.NONE) return
    val contentDesc = rank.badgeTitle
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val rOuter = this.size.minDimension * 0.48f

        when (rank) {
            AppRank.OWNER -> {
                // Scalloped 10-lobe Black Owner badge with subtle silver rim for dark-theme contrast
                val path = buildScallopedPath(cx, cy, rOuter, lobes = 10, depth = 0.14f)
                drawPath(path = path, color = Color(0xFF0A0A0D))
                drawPath(path = path, color = Color.White.copy(alpha = 0.65f), style = Stroke(width = this.size.minDimension * 0.06f))
            }
            AppRank.MODERATOR -> {
                // Scalloped 10-lobe Red Moderator badge
                val path = buildScallopedPath(cx, cy, rOuter, lobes = 10, depth = 0.14f)
                drawPath(path = path, color = Color(0xFFF43F40))
            }
            AppRank.INFLUENCER -> {
                // 12-point Starburst Blue-to-Purple Influencer badge with crisp white ring
                val outerStar = buildStarburstPath(cx, cy, rOuter, rOuter * 0.78f, points = 12)
                drawPath(
                    path = outerStar,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF1D6BFF), Color(0xFF60A5FA), Color(0xFFA855F7))
                    )
                )
                val innerStar = buildStarburstPath(cx, cy, rOuter * 0.84f, rOuter * 0.66f, points = 12)
                drawPath(
                    path = innerStar,
                    color = Color.White,
                    style = Stroke(width = this.size.minDimension * 0.07f, join = StrokeJoin.Round)
                )
            }
            AppRank.NONE -> Unit
        }

        // White crisp checkmark in center
        val check = Path().apply {
            moveTo(this@Canvas.size.width * 0.30f, this@Canvas.size.height * 0.52f)
            lineTo(this@Canvas.size.width * 0.44f, this@Canvas.size.height * 0.66f)
            lineTo(this@Canvas.size.width * 0.72f, this@Canvas.size.height * 0.36f)
        }
        drawPath(
            path = check,
            color = Color.White,
            style = Stroke(
                width = this.size.minDimension * 0.13f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun buildScallopedPath(
    cx: Float,
    cy: Float,
    radius: Float,
    lobes: Int,
    depth: Float
): Path {
    val steps = lobes * 12
    val path = Path()
    for (i in 0..steps) {
        val theta = (2.0 * PI * i) / steps
        val wave = cos(lobes * theta).toFloat()
        val r = radius * (1f - depth * (1f - wave) * 0.5f)
        val x = cx + r * cos(theta).toFloat()
        val y = cy + r * sin(theta).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun buildStarburstPath(
    cx: Float,
    cy: Float,
    outerR: Float,
    innerR: Float,
    points: Int
): Path {
    val path = Path()
    val total = points * 2
    for (i in 0 until total) {
        val theta = (PI * i) / points - PI / 2.0
        val r = if (i % 2 == 0) outerR else innerR
        val x = cx + r * cos(theta).toFloat()
        val y = cy + r * sin(theta).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

@Composable
fun AppRankPill(
    rank: AppRank,
    modifier: Modifier = Modifier
) {
    if (rank == AppRank.NONE) return
    val (bg, border, textColor) = when (rank) {
        AppRank.OWNER -> Triple(Color(0xFF0A0A0D), Color.White.copy(alpha = 0.5f), Color.White)
        AppRank.MODERATOR -> Triple(Color(0xFFF43F40).copy(alpha = 0.2f), Color(0xFFF43F40), Color(0xFFFF8A8A))
        AppRank.INFLUENCER -> Triple(Color(0xFF2563EB).copy(alpha = 0.22f), Color(0xFF60A5FA), Color(0xFF93C5FD))
        AppRank.NONE -> Triple(Color.Transparent, Color.Transparent, TextSecondary)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VerifiedRankBadge(rank = rank, size = 15.dp)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = rank.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfilePreviewSheet(
    user: UserProfile,
    roleInRoom: CommunityRole?,
    myRoleInRoom: CommunityRole?,
    currentUserRank: AppRank = AppRank.NONE,
    currentUserIsDeveloper: Boolean = false,
    isCurrentUser: Boolean,
    onDismiss: () -> Unit,
    onMuteToggle: ((Boolean) -> Unit)? = null,
    isParticipantMuted: Boolean = false,
    onPromoteCoAdmin: (() -> Unit)? = null,
    onDemoteMember: (() -> Unit)? = null,
    onKick: (() -> Unit)? = null,
    onAssignAppRank: ((AppRank) -> Unit)? = null,
    onBanUser: ((String) -> Unit)? = null,
    onUnbanUser: (() -> Unit)? = null,
    onFollowUser: (() -> Unit)? = null,
    onGrantCoins: ((Int) -> Unit)? = null
) {
    val canManageRanks = currentUserIsDeveloper || currentUserRank == AppRank.OWNER
    val canBanThisUser = canManageRanks && !isCurrentUser &&
        !user.email.equals(com.example.data.TxlRepository.PRIMARY_OWNER_EMAIL, ignoreCase = true)
    var showBanConfirm by remember(user.id) { mutableStateOf(false) }
    var banReasonInput by remember(user.id) { mutableStateOf("Actividad ilegal o incumplimiento grave de normas") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val resolvedFrameName = LocalCustomFrames.current[user.avatarFrameId]?.title ?: user.avatarFrame.displayName
            FramedAvatar(
                avatarUri = user.avatarUri,
                presetAvatarKey = user.presetAvatarKey,
                frame = user.avatarFrame,
                frameId = user.avatarFrameId,
                size = 86.dp,
                contentDescription = "Foto de perfil de ${user.username}"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (user.appRank != AppRank.NONE) {
                    Spacer(modifier = Modifier.width(6.dp))
                    VerifiedRankBadge(rank = user.appRank, size = 22.dp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tier = user.donorTier
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(tier.colorHex).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color(tier.colorHex).copy(alpha = 0.65f))
                ) {
                    Text(
                        text = "${tier.emoji} Nv.${tier.level} ${tier.title}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(tier.colorHex),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                if (canManageRanks && user.appRank != AppRank.NONE) {
                    AppRankPill(rank = user.appRank)
                }
                if (roleInRoom != null) {
                    RoleBadge(role = roleInRoom)
                }
            }
            if (!isCurrentUser && onFollowUser != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onFollowUser,
                    border = BorderStroke(1.dp, NeonPink),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("+ Seguir emisor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonPink)
                }
            }
            if (user.customStatus.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = NeonPurple.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = user.customStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = user.bio.ifBlank { "Sin biografía personalizada todavía." },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip(label = "Círculos", value = user.communitiesCount.toString())
                StatChip(label = "Seguidores", value = user.followersCount.toString())
                StatChip(label = "Marco", value = resolvedFrameName)
            }

            // Owner/Developer-only global verification grant controls
            if (canManageRanks && onAssignAppRank != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Cambiar Verificado (Desarrollador)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                RankVerificationSelectorGrid(
                    currentRank = user.appRank,
                    targetUserId = user.id,
                    isSelf = isCurrentUser,
                    onSelectRank = onAssignAppRank
                )
            }

            // Owner-only coin gift controls (Owner has infinite coins and can fund creators/users)
            if (canManageRanks && !isCurrentUser && onGrantCoins != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "🎁 Regalar monedas (Dueño)",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoleAdminGold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100, 500, 2000).forEach { giftAmt ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RoleAdminGold.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, RoleAdminGold.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onGrantCoins(giftAmt) }
                                .testTag("owner_grant_coins_${giftAmt}_${user.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "+$giftAmt 💎",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoleAdminGold
                                )
                            }
                        }
                    }
                }
            }

            // Role-based moderation controls for Admin / Co-Admin
            if (!isCurrentUser && myRoleInRoom != null && roleInRoom != null &&
                myRoleInRoom.priority > roleInRoom.priority
            ) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Moderación de sala (${myRoleInRoom.label})",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onMuteToggle != null) {
                        OutlinedButton(
                            onClick = { onMuteToggle(!isParticipantMuted); onDismiss() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            border = BorderStroke(1.dp, NeonCyan)
                        ) {
                            Icon(
                                imageVector = if (isParticipantMuted) Icons.Filled.VolumeUp else Icons.Filled.MicOff,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isParticipantMuted) "Reactivar micro" else "Silenciar", fontSize = 12.sp)
                        }
                    }
                    if (myRoleInRoom == CommunityRole.ADMIN) {
                        if (roleInRoom == CommunityRole.MEMBER && onPromoteCoAdmin != null) {
                            OutlinedButton(
                                onClick = { onPromoteCoAdmin(); onDismiss() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                border = BorderStroke(1.dp, RoleAdminGold)
                            ) {
                                Text("Hacer Co-Admin", color = RoleAdminGold, fontSize = 12.sp)
                            }
                        } else if (roleInRoom == CommunityRole.CO_ADMIN && onDemoteMember != null) {
                            OutlinedButton(
                                onClick = { onDemoteMember(); onDismiss() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                border = BorderStroke(1.dp, TextSecondary)
                            ) {
                                Text("Pasar a Miembro", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
                if (myRoleInRoom == CommunityRole.ADMIN && onKick != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onKick(); onDismiss() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        border = BorderStroke(1.dp, DangerRose)
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, tint = DangerRose, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Expulsar de la sala", color = DangerRose)
                    }
                }
            }
            if (canBanThisUser && (onBanUser != null || onUnbanUser != null)) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DangerRose.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, DangerRose.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Gavel, contentDescription = null, tint = DangerRose, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Seguridad (Solo Desarrollador)",
                                style = MaterialTheme.typography.titleSmall,
                                color = DangerRose,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (user.isBanned) {
                            Text(
                                text = "Usuario baneado permanentemente: ${user.banReason ?: "Actividad ilegal"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if (onUnbanUser != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        onUnbanUser()
                                        onDismiss()
                                    },
                                    border = BorderStroke(1.dp, SuccessGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("unban_user_${user.id}")
                                ) {
                                    Text("Quitar baneo (Permitir acceso)", color = SuccessGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (onBanUser != null) {
                            if (!showBanConfirm) {
                                Button(
                                    onClick = { showBanConfirm = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRose),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("ban_user_${user.id}")
                                ) {
                                    Icon(Icons.Filled.Block, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Banear por actividad ilegal", color = TextPrimary, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = "El usuario quedará bloqueado y no podrá volver a usar NexoLive.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = banReasonInput,
                                    onValueChange = { if (it.length <= 180) banReasonInput = it },
                                    label = { Text("Motivo del baneo") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ban_reason_input_${user.id}")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showBanConfirm = false },
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Cancelar", color = TextSecondary)
                                    }
                                    Button(
                                        onClick = {
                                            onBanUser(banReasonInput)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DangerRose),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("confirm_ban_user_${user.id}")
                                    ) {
                                        Text("Banear ahora", color = TextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RankVerificationSelectorGrid(
    currentRank: AppRank,
    targetUserId: String,
    isSelf: Boolean = false,
    onSelectRank: (AppRank) -> Unit
) {
    val rows = if (isSelf) {
        listOf(
            listOf(
                AppRank.OWNER to "Negro (Dueño)",
                AppRank.MODERATOR to "Rojo (Mod)"
            ),
            listOf(
                AppRank.INFLUENCER to "Azul (Influencer)",
                AppRank.NONE to "Quitar"
            )
        )
    } else {
        listOf(
            listOf(
                AppRank.MODERATOR to "Rojo (Mod)",
                AppRank.INFLUENCER to "Azul (Influencer)"
            ),
            listOf(
                AppRank.NONE to "Quitar"
            )
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { (rank, label) ->
                    val selected = currentRank == rank
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) NeonPurple.copy(alpha = 0.28f) else SurfaceElevated,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) NeonPink else BorderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectRank(rank) }
                            .testTag("grant_${rank.id}_$targetUserId")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (rank != AppRank.NONE) {
                                VerifiedRankBadge(rank = rank, size = 16.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) TextPrimary else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

fun formatRelativeTime(timestampMillis: Long): String {
    val diff = (System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L)
    val mins = diff / 60_000L
    return when {
        mins < 1 -> "Ahora"
        mins < 60 -> "${mins}m"
        mins < 1440 -> "${mins / 60}h"
        else -> "${mins / 1440}d"
    }
}

fun formatAudioDuration(durationMs: Long): String {
    val totalSecs = (durationMs.coerceAtLeast(0L) / 1000L).toInt()
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

@Composable
fun AudioMessageBubbleContent(
    messageId: Long,
    durationMs: Long,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val waveHeights = listOf(
        0.35f, 0.72f, 0.48f, 0.95f, 0.62f, 0.88f, 0.42f,
        0.92f, 0.56f, 0.78f, 0.44f, 0.84f, 0.60f, 0.36f
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceElevated.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, if (isPlaying) NeonPink else NeonPurple.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPurple, NeonPink)))
                    .clickable(onClick = onTogglePlay)
                    .testTag("audio_play_$messageId"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar nota de voz" else "Reproducir nota de voz",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    waveHeights.forEachIndexed { index, ratio ->
                        val frac = index.toFloat() / waveHeights.size.toFloat()
                        val filled = isPlaying && frac <= progress
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((22f * ratio).dp.coerceAtLeast(5.dp))
                                .clip(RoundedCornerShape(50))
                                .background(if (filled) NeonCyan else NeonPurpleLight.copy(alpha = 0.62f))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = NeonPink,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPlaying) "Reproduciendo..." else "Nota de voz",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = formatAudioDuration(durationMs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPlaying) NeonCyan else TextPrimary
                    )
                }
            }
        }
    }
}

