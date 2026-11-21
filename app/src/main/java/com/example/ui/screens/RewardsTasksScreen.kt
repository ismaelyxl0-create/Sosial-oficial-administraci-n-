package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MePlusDrawOutcome
import com.example.model.BagCatalog
import com.example.model.DailyCheckInCatalog
import com.example.model.DailyCheckInReward
import com.example.model.DailyTaskCatalog
import com.example.model.DailyTaskDefinition
import com.example.model.DonorTier
import com.example.model.UserProfile
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RoleAdminGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.concurrent.TimeUnit

private val PastelBg = Color(0xFFF6F5FA)
private val LightCardBg = Color.White
private val DarkInk = Color(0xFF1F1936)
private val MutedInk = Color(0xFF6B6580)
private val VividViolet = Color(0xFF7C3AED)
private val SoftLilacBorder = Color(0xFFE7DFF8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsTasksScreen(
    user: UserProfile?,
    mePlusOutcome: MePlusDrawOutcome?,
    onClearMePlusOutcome: () -> Unit,
    onClaimDailyCheckIn: () -> Unit,
    onClaimDailyTask: (String) -> Unit,
    onPlayCoinMiniGame: (betCoins: Int, rewardCoins: Int, message: String) -> Unit,
    onActivateMePlusAndDraw: () -> Unit,
    onClaimFreeCoins: (Int) -> Unit,
    onNavigateToExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (user == null) return

    var activeSubTab by rememberSaveable { mutableStateOf("Tareas") }
    var inspectedDay by remember { mutableStateOf<DailyCheckInReward?>(null) }
    var showRulesDialog by rememberSaveable { mutableStateOf(false) }
    var showBagSheet by rememberSaveable { mutableStateOf(false) }
    var showMiniGameDialog by rememberSaveable { mutableStateOf(false) }
    var countdownText by remember { mutableStateOf("00:00:00") }

    // Live countdown until next local midnight reset
    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diff = (next.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
            val h = TimeUnit.MILLISECONDS.toHours(diff)
            val m = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            val s = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
            countdownText = String.format("%02d:%02d:%02d", h, m, s)
            delay(1000L)
        }
    }

    val isAlreadyCheckedInToday = remember(user.lastDailyCheckInAtMillis) {
        if (user.lastDailyCheckInAtMillis <= 0L) false
        else {
            val calLast = Calendar.getInstance().apply { timeInMillis = user.lastDailyCheckInAtMillis }
            val calNow = Calendar.getInstance()
            calLast.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calLast.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
        }
    }

    val currentStreakDay = user.dailyStreakDay.coerceIn(0, 7)
    val nextDayNumber = if (currentStreakDay >= 7) 1 else currentStreakDay + 1

    val infiniteTransition = rememberInfiniteTransition(label = "me_plus_gift")
    val giftScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "me_plus_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PastelBg)
    ) {
        // Top Sub-tab header ("Centro de tareas | Niveles | Me+ | Calendario")
        Surface(
            color = LightCardBg,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Tareas", "Niveles", "Me+", "Calendario").forEach { tab ->
                    val selected = activeSubTab == tab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { activeSubTab = tab }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("rewards_subtab_$tab")
                    ) {
                        Text(
                            text = tab,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (selected) DarkInk else MutedInk
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) VividViolet else Color.Transparent)
                        )
                    }
                }
            }
        }

        // Scrollable Body
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeSubTab) {
                "Niveles" -> {
                    item {
                        DonorLevelsCard(user = user, onGoGift = onNavigateToExplore)
                    }
                }
                "Me+" -> {
                    item {
                        MePlusDrawCard(
                            user = user,
                            giftScale = giftScale,
                            onDraw = onActivateMePlusAndDraw
                        )
                    }
                    item {
                        DonorLevelsCard(user = user, onGoGift = onNavigateToExplore)
                    }
                }
                "Calendario" -> {
                    item {
                        DailyCheckInSectionCard(
                            currentStreakDay = currentStreakDay,
                            nextDayNumber = nextDayNumber,
                            isAlreadyCheckedInToday = isAlreadyCheckedInToday,
                            countdownText = countdownText,
                            onClaimToday = onClaimDailyCheckIn,
                            onInspectDay = { inspectedDay = it },
                            onShowHelp = { showRulesDialog = true }
                        )
                    }
                }
                else -> {
                    item {
                        DailyCheckInSectionCard(
                            currentStreakDay = currentStreakDay,
                            nextDayNumber = nextDayNumber,
                            isAlreadyCheckedInToday = isAlreadyCheckedInToday,
                            countdownText = countdownText,
                            onClaimToday = onClaimDailyCheckIn,
                            onInspectDay = { inspectedDay = it },
                            onShowHelp = { showRulesDialog = true }
                        )
                    }
                    item {
                        MePlusDrawCard(
                            user = user,
                            giftScale = giftScale,
                            onDraw = onActivateMePlusAndDraw
                        )
                    }
                    item {
                        ExclusiveTasksCard(
                            user = user,
                            onShowHelp = { showRulesDialog = true },
                            onStartTask = { task ->
                                when (task.id) {
                                    DailyTaskCatalog.TASK_GAME_10K_COINS -> showMiniGameDialog = true
                                    else -> onNavigateToExplore()
                                }
                            },
                            onClaimTask = onClaimDailyTask
                        )
                    }
                }
            }
        }

        // Sticky Bottom Summary Bar: ⭐ Nivel | 🪙 Monedas | 👜 Mi bolsa
        Surface(
            color = LightCardBg,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, SoftLilacBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            val bagTotalCount = user.inventoryMap.values.sum()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { activeSubTab = "Niveles" }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Nv.${user.donorTier.level}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkInk
                        )
                        Text(
                            text = "${user.donorExp} EXP",
                            fontSize = 10.sp,
                            color = VividViolet,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(modifier = Modifier.width(1.dp).height(26.dp).background(SoftLilacBorder))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (user.isPrimaryVerifiedOwner) Modifier.clickable { onClaimFreeCoins(1000) }
                            else Modifier
                        )
                        .padding(vertical = 4.dp)
                        .testTag("rewards_coins_chip"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🪙", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (user.hasInfiniteCoins) "∞" else "${user.coinsCount}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkInk
                        )
                        Text(
                            text = if (user.isPrimaryVerifiedOwner) "+1,000 Dueño" else "Tus monedas",
                            fontSize = 10.sp,
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(modifier = Modifier.width(1.dp).height(26.dp).background(SoftLilacBorder))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showBagSheet = true }
                        .padding(vertical = 4.dp)
                        .testTag("rewards_my_bag_button"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👜", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mi bolsa ($bagTotalCount)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkInk
                    )
                }
            }
        }
    }

    inspectedDay?.let { dayReward ->
        val completed = dayReward.day <= currentStreakDay
        val isNextClaimable = !isAlreadyCheckedInToday && dayReward.day == nextDayNumber
        AlertDialog(
            onDismissRequest = { inspectedDay = null },
            containerColor = LightCardBg,
            titleContentColor = DarkInk,
            textContentColor = MutedInk,
            title = { Text("Día ${dayReward.day}: ${dayReward.title}", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = buildString {
                            append(dayReward.emoji)
                            if (dayReward.secondaryEmoji != null) append(" + ${dayReward.secondaryEmoji}")
                        },
                        fontSize = 40.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(dayReward.badgeText, fontWeight = FontWeight.Bold, color = VividViolet)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            completed -> "✅ Ya reclamaste este día en tu racha actual."
                            isNextClaimable -> "¡Disponible para reclamar hoy! Todos tus artículos se guardan en Mi bolsa."
                            else -> "Inicia sesión de forma consecutiva para desbloquear este día."
                        },
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                if (isNextClaimable) {
                    Button(
                        onClick = {
                            inspectedDay = null
                            onClaimDailyCheckIn()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VividViolet)
                    ) {
                        Text("Reclamar Día ${dayReward.day}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = { inspectedDay = null }) {
                        Text("Entendido", color = VividViolet, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            containerColor = LightCardBg,
            titleContentColor = DarkInk,
            textContentColor = MutedInk,
            title = { Text("Reglas de Tareas y Donador") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Inicia sesión 7 días seguidos para reclamar monedas, EXP de Donador y regalos gratis para Mi bolsa.")
                    Text("• Al enviar regalos en las salas o completar misiones sumas EXP Donador y subes de Nivel (1 a 6) con bonos automáticos de monedas.")
                    Text("• Todos los artículos ganados en Mi bolsa pueden enviarse gratis en cualquier sala de voz.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showRulesDialog = false }) {
                    Text("¡Entendido!", color = VividViolet, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    mePlusOutcome?.let { outcome ->
        AlertDialog(
            onDismissRequest = onClearMePlusOutcome,
            containerColor = LightCardBg,
            titleContentColor = DarkInk,
            textContentColor = MutedInk,
            title = { Text("🎉 ¡Premio Sorteo Me+!", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🎁", fontSize = 44.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(outcome.prizeTitle, fontWeight = FontWeight.Bold, color = VividViolet, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "+${outcome.coinsWon} monedas • +${outcome.expWon} EXP Donador • ${outcome.bagItemWonName ?: "Artículo especial"} guardado en Mi bolsa.",
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onClearMePlusOutcome,
                    colors = ButtonDefaults.buttonColors(containerColor = VividViolet)
                ) {
                    Text("¡Genial!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showMiniGameDialog) {
        CoinMiniGameDialog(
            coinsBalance = user.coinsCount,
            gameProgress = user.gameCoinsEarnedProgress,
            canClaimFreeCoins = user.isPrimaryVerifiedOwner,
            onSpin = onPlayCoinMiniGame,
            onClaimFreeCoins = { onClaimFreeCoins(1000) },
            onDismiss = { showMiniGameDialog = false }
        )
    }

    if (showBagSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBagSheet = false },
            containerColor = SurfaceCard,
            contentColor = TextPrimary
        ) {
            val entries = user.inventoryMap.entries.filter { it.value > 0 }.toList()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("👜 Mi bolsa de premios", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(
                    text = "Úsalos gratis al abrir la Galería de Regalos dentro de cualquier sala en vivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (entries.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF211C33),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎁", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Aún no tienes premios guardados", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Reclama tu Día 1 o completa tareas para llenar Mi bolsa.", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    entries.forEach { (itemId, count) ->
                        val meta = BagCatalog.metaFor(itemId)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF211C33),
                            border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(meta.emoji, fontSize = 30.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(meta.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(meta.description, fontSize = 11.sp, color = TextSecondary)
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = NeonPink.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, NeonPink)
                                ) {
                                    Text(
                                        text = "×$count",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeonPink,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun DailyCheckInSectionCard(
    currentStreakDay: Int,
    nextDayNumber: Int,
    isAlreadyCheckedInToday: Boolean,
    countdownText: String,
    onClaimToday: () -> Unit,
    onInspectDay: (DailyCheckInReward) -> Unit,
    onShowHelp: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = LightCardBg,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, SoftLilacBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Soft lilac-pink pastel header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFEFF4FF), Color(0xFFF3EAFF), Color(0xFFFDE8F3))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Registrarse",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkInk
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onShowHelp, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.HelpOutline, contentDescription = "Ayuda de registro", tint = MutedInk, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        text = "Inicia sesión por 7 días continuos para obtener monedas y EXP",
                        fontSize = 12.sp,
                        color = MutedInk
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Haz clic abajo para ver el regalo de ingreso disponible",
                    fontSize = 12.sp,
                    color = MutedInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Row 1: Days 1..4
                val days = DailyCheckInCatalog.days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    days.take(4).forEach { dayReward ->
                        CheckInDayCell(
                            reward = dayReward,
                            isCompleted = dayReward.day <= currentStreakDay,
                            isTodayActive = !isAlreadyCheckedInToday && dayReward.day == nextDayNumber,
                            onClick = { onInspectDay(dayReward) },
                            modifier = Modifier.weight(if (dayReward.day == 2) 1.35f else 1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Row 2: Days 5..7
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    days.drop(4).forEach { dayReward ->
                        CheckInDayCell(
                            reward = dayReward,
                            isCompleted = dayReward.day <= currentStreakDay,
                            isTodayActive = !isAlreadyCheckedInToday && dayReward.day == nextDayNumber,
                            onClick = { onInspectDay(dayReward) },
                            modifier = Modifier.weight(if (dayReward.day == 7) 2f else 1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onClaimToday,
                    enabled = !isAlreadyCheckedInToday,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VividViolet,
                        disabledContainerColor = Color(0xFFBDBDBD)
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("claim_daily_checkin_button")
                ) {
                    if (isAlreadyCheckedInToday) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = countdownText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Registrarse hoy (Día $nextDayNumber)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInDayCell(
    reward: DailyCheckInReward,
    isCompleted: Boolean,
    isTodayActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isCompleted -> Color(0xFF9A9A9A)
        isTodayActive -> Color.White
        else -> Color(0xFFFAF8FE)
    }
    val borderColor = if (isTodayActive) VividViolet else SoftLilacBorder
    Box(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(if (isTodayActive) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("checkin_day_${reward.day}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isTodayActive) VividViolet else Color.Transparent)
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DÍA ${reward.day}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isTodayActive -> Color.White
                        isCompleted -> Color(0xFF3F3D46)
                        else -> MutedInk
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(reward.emoji, fontSize = 23.sp)
                if (reward.secondaryEmoji != null) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(reward.secondaryEmoji, fontSize = 21.sp)
                }
            }
            Text(
                text = reward.badgeText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) Color(0xFF3F3D46) else MutedInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
        if (isCompleted) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Completado",
                tint = Color(0xFF22C55E),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        }
    }
}

@Composable
private fun MePlusDrawCard(
    user: UserProfile,
    giftScale: Float,
    onDraw: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDraw)
            .testTag("meplus_draw_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF8B3DFF), Color(0xFF9333EA), Color(0xFFC084FC))
                    )
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Me+",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sorteo privilegiado",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = if (user.mePlusMember) "Membresía Me+ activa • Toca para sortear hoy" else "Acceso exclusivo Me+ • Toca para desbloquear gratis",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
                Text(
                    text = "🎁",
                    fontSize = 36.sp,
                    modifier = Modifier.scale(giftScale)
                )
            }
        }
    }
}

@Composable
private fun ExclusiveTasksCard(
    user: UserProfile,
    onShowHelp: () -> Unit,
    onStartTask: (DailyTaskDefinition) -> Unit,
    onClaimTask: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = LightCardBg,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, SoftLilacBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFEFF4FF), Color(0xFFF3EAFF), Color(0xFFFDE8F3))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tareas exclusivas",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkInk
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onShowHelp, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.HelpOutline, contentDescription = "Ayuda tareas", tint = MutedInk, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        text = "¡Completa las tareas para reclamar las abundantes recompensas!",
                        fontSize = 12.sp,
                        color = MutedInk
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DailyTaskCatalog.tasks.forEach { task ->
                    val progress = when (task.id) {
                        DailyTaskCatalog.TASK_FOLLOW_HOST -> user.followHostsProgress
                        DailyTaskCatalog.TASK_WATCH_LIVE_5M -> user.watchLiveMinutesProgress
                        DailyTaskCatalog.TASK_GAME_10K_COINS -> user.gameCoinsEarnedProgress
                        DailyTaskCatalog.TASK_SEND_200_GIFTS -> user.giftCoinsSentProgress
                        else -> 0
                    }.coerceAtMost(task.target)
                    val claimed = task.id in user.claimedTaskIds
                    val completed = progress >= task.target

                    val (iconBg, iconTint, iconVector) = when (task.iconKey) {
                        "follow" -> Triple(Color(0xFFF3E8FF), VividViolet, Icons.Filled.AddCircle)
                        "live" -> Triple(Color(0xFFFFE4E6), Color(0xFFFB7185), Icons.Filled.Videocam)
                        "game" -> Triple(Color(0xFFDCFCE7), Color(0xFF22C55E), Icons.Filled.SportsEsports)
                        else -> Triple(Color(0xFFF3E8FF), VividViolet, Icons.Filled.CardGiftcard)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = iconBg,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(iconVector, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${task.title} ($progress/${task.target})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkInk
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress.toFloat() / task.target.toFloat() },
                                color = if (completed) Color(0xFF22C55E) else VividViolet,
                                trackColor = Color(0xFFEDE9FE),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("⭐ EXP ×${task.expReward}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VividViolet)
                                if (task.itemEmoji != null && task.itemQty > 0) {
                                    Text("${task.itemEmoji} ×${task.itemQty}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkInk)
                                }
                                if (task.secondaryItemEmoji != null && task.secondaryItemQty > 0) {
                                    Text("${task.secondaryItemEmoji} ×${task.secondaryItemQty}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkInk)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        when {
                            claimed -> {
                                OutlinedButton(
                                    onClick = {},
                                    enabled = false,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Listo", fontSize = 12.sp, color = MutedInk)
                                }
                            }
                            completed -> {
                                Button(
                                    onClick = { onClaimTask(task.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("claim_task_${task.id}")
                                ) {
                                    Text("Reclamar", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            else -> {
                                OutlinedButton(
                                    onClick = { onStartTask(task) },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.2.dp, VividViolet),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("start_task_${task.id}")
                                ) {
                                    Text("Comenzar", fontSize = 12.sp, color = VividViolet, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonorLevelsCard(
    user: UserProfile,
    onGoGift: () -> Unit
) {
    val currentTier = user.donorTier
    val nextTier = DonorTier.nextTier(currentTier)
    val frac = if (nextTier == null) 1f else {
        ((user.donorExp - currentTier.minExp).toFloat() / (nextTier.minExp - currentTier.minExp).coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = LightCardBg,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, SoftLilacBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = VividViolet, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Niveles de Donador", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = DarkInk)
            }
            Text(
                text = "Cada regalo que envías suma EXP de Donador para desbloquear insignias, prioridad y monedas extra.",
                fontSize = 12.sp,
                color = MutedInk
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1F1638),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentTier.emoji} Nivel ${currentTier.level} • ${currentTier.title}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(currentTier.colorHex)
                        )
                        Text(
                            text = "${user.donorExp} EXP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = RoleAdminGold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { frac },
                        color = Color(currentTier.colorHex),
                        trackColor = Color(0xFF332852),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (nextTier != null) {
                            "Faltan ${(nextTier.minExp - user.donorExp).coerceAtLeast(0)} EXP para Nivel ${nextTier.level} (${nextTier.title}) • Bono +${nextTier.coinBonusOnReach} 🪙"
                        } else {
                            "¡Nivel máximo alcanzado! Disfruta todos los beneficios VIP."
                        },
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total donado: ${user.totalCoinsDonated} 🪙",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DonorTier.entries.forEach { tier ->
                val unlocked = user.donorExp >= tier.minExp
                val isCurrent = tier == currentTier
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCurrent) Color(0xFFF3E8FF) else Color(0xFFFAF8FE),
                    border = BorderStroke(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = if (isCurrent) VividViolet else SoftLilacBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tier.emoji, fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nv.${tier.level} ${tier.title} (${tier.minExp} EXP)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkInk
                            )
                            Text(tier.benefitDescription, fontSize = 11.sp, color = MutedInk)
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (unlocked) Color(0xFF22C55E).copy(alpha = 0.16f) else Color(0xFFE5E7EB)
                        ) {
                            Text(
                                text = if (unlocked) "Activo" else "Bloqueado",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (unlocked) Color(0xFF15803D) else MutedInk,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onGoGift,
                colors = ButtonDefaults.buttonColors(containerColor = VividViolet),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("Ir a una sala para enviar regalos", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CoinMiniGameDialog(
    coinsBalance: Int,
    gameProgress: Int,
    canClaimFreeCoins: Boolean,
    onSpin: (betCoins: Int, rewardCoins: Int, message: String) -> Unit,
    onClaimFreeCoins: () -> Unit,
    onDismiss: () -> Unit
) {
    var lastResult by remember { mutableStateOf("Toca Girar Ruleta para sumar monedas hacia la meta de 10,000.") }
    val multipliers = listOf(500 to 1200, 1000 to 2500, 2000 to 4800)
    var selectedBetIdx by rememberSaveable { mutableIntStateOf(0) }
    val (bet, reward) = multipliers[selectedBetIdx]

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LightCardBg,
        titleContentColor = DarkInk,
        textContentColor = MutedInk,
        title = { Text("🎮 Ruleta de Monedas Neón", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Saldo: $coinsBalance 🪙  •  Progreso misión: $gameProgress / 10,000", fontWeight = FontWeight.Bold, color = VividViolet)
                LinearProgressIndicator(
                    progress = { (gameProgress / 10_000f).coerceIn(0f, 1f) },
                    color = Color(0xFF22C55E),
                    trackColor = Color(0xFFEDE9FE),
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50))
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    multipliers.forEachIndexed { idx, pair ->
                        val active = idx == selectedBetIdx
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) VividViolet else Color(0xFFF3E8FF),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedBetIdx = idx }
                        ) {
                            Text(
                                text = "${pair.first} 🪙",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else VividViolet,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                Text(lastResult, fontSize = 12.sp, color = DarkInk)
                if (coinsBalance < bet) {
                    if (canClaimFreeCoins) {
                        TextButton(onClick = onClaimFreeCoins) {
                            Text("+1,000 monedas (Dueño)", color = VividViolet, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Gana más monedas en el Check-In diario, vendiendo marcos o por regalos de otros usuarios.",
                            fontSize = 11.sp,
                            color = MutedInk
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val outcomeMsg = "¡Ganaste +$reward 🪙 en la Ruleta!"
                    lastResult = outcomeMsg
                    onSpin(bet, reward, outcomeMsg)
                },
                enabled = coinsBalance >= bet,
                colors = ButtonDefaults.buttonColors(containerColor = VividViolet)
            ) {
                Text("Girar ($bet 🪙 → $reward 🪙)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = MutedInk)
            }
        }
    )
}
