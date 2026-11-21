package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.BagCatalog
import com.example.model.DonorTier
import com.example.model.GiftCatalog
import com.example.model.GiftItem
import com.example.model.RoomGiftEvent
import com.example.model.UserProfile
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleLight
import com.example.ui.theme.RoleAdminGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftGallerySheet(
    currentUser: UserProfile?,
    recipients: List<UserProfile>,
    onDismiss: () -> Unit,
    onSendGift: (recipientId: String, giftId: String, comboCount: Int, useBagItemId: String?) -> Unit,
    onClaimFreeCoins: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categories = listOf("Popular", "Destacados", "Exclusivos", "Mi bolsa")
    var selectedCategory by rememberSaveable { mutableStateOf("Popular") }
    var selectedGiftId by rememberSaveable { mutableStateOf(GiftCatalog.items.first().id) }
    var selectedBagItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var comboCount by rememberSaveable { mutableIntStateOf(1) }

    val availableRecipients = remember(recipients, currentUser) {
        val others = recipients.filter { it.id != currentUser?.id }
        if (others.isNotEmpty()) others else listOfNotNull(currentUser)
    }
    var selectedRecipientId by rememberSaveable(availableRecipients.firstOrNull()?.id) {
        mutableStateOf(availableRecipients.firstOrNull()?.id ?: currentUser?.id.orEmpty())
    }

    val tier = currentUser?.donorTier ?: DonorTier.APPRENTICE
    val nextTier = DonorTier.nextTier(tier)
    val donorExp = currentUser?.donorExp ?: 0
    val progressToNext = if (nextTier == null) 1f else {
        val span = (nextTier.minExp - tier.minExp).coerceAtLeast(1)
        ((donorExp - tier.minExp).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    }

    val filteredGifts = remember(selectedCategory) {
        GiftCatalog.items.filter { it.category == selectedCategory }
    }
    val bagEntries = remember(currentUser?.inventoryMap) {
        currentUser?.inventoryMap.orEmpty().entries.filter { it.value > 0 }.toList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14131B),
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Top Donor Level & Coins header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = NeonPink, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Galería de Regalos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = RoleAdminGold.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, RoleAdminGold.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = if (currentUser?.hasInfiniteCoins == true) "💎 ∞" else "🪙 ${currentUser?.coinsCount ?: 0}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoleAdminGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (currentUser?.isPrimaryVerifiedOwner == true) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = NeonPurple.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, NeonPink),
                            modifier = Modifier
                                .clickable(onClick = onClaimFreeCoins)
                                .testTag("gift_claim_free_coins")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.AddCircleOutline, contentDescription = "Recargar", tint = NeonPink, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("+1,000", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = "Misiones • Regalos",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            // Donor Level & EXP Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1F1B2E),
                border = BorderStroke(1.dp, Color(tier.colorHex).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${tier.emoji} Nivel Donador ${tier.level}: ${tier.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(tier.colorHex)
                        )
                        Text(
                            text = if (nextTier != null) "$donorExp / ${nextTier.minExp} EXP" else "$donorExp EXP • MÁX",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { progressToNext },
                        color = Color(tier.colorHex),
                        trackColor = Color(0xFF2D2742),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = nextTier?.let { "Al subir a Nv.${it.level} (${it.title}): +${it.coinBonusOnReach} 🪙" }
                            ?: tier.benefitDescription,
                        fontSize = 10.sp,
                        color = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            // Recipient picker
            if (availableRecipients.isNotEmpty()) {
                Text("Enviar regalo a:", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableRecipients, key = { it.id }) { target ->
                        val selected = selectedRecipientId == target.id
                        FilterChip(
                            selected = selected,
                            onClick = { selectedRecipientId = target.id },
                            label = {
                                Text(
                                    text = if (target.id == currentUser?.id) "${target.username} (Sala)" else target.username,
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPink.copy(alpha = 0.28f),
                                selectedLabelColor = TextPrimary,
                                containerColor = SurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            // Category tabs
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val selected = selectedCategory == cat
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedCategory = cat
                            if (cat == "Mi bolsa") {
                                selectedBagItemId = bagEntries.firstOrNull()?.key
                            } else {
                                selectedBagItemId = null
                                val firstInCat = GiftCatalog.items.firstOrNull { it.category == cat }
                                if (firstInCat != null) selectedGiftId = firstInCat.id
                            }
                        },
                        label = {
                            val suffix = if (cat == "Mi bolsa") " (${bagEntries.sumOf { it.value }})" else ""
                            Text("$cat$suffix", fontSize = 12.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonPurple,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF211F2B),
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedCategory == "Mi bolsa") {
                if (bagEntries.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1D27),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("👜", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tu bolsa de regalos está vacía",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Completa el Inicio de Sesión Diario o Tareas para ganar artículos gratis.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bagEntries, key = { it.key }) { (itemId, qty) ->
                            val meta = BagCatalog.metaFor(itemId)
                            val isSelected = selectedBagItemId == itemId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) Color(0xFF2D2245) else Color(0xFF1C1B24))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) NeonPink else BorderSubtle.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedBagItemId = itemId }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(meta.emoji, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = meta.name,
                                        fontSize = 10.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Gratis ×$qty",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredGifts, key = { it.id }) { gift ->
                        GiftGridTile(
                            gift = gift,
                            isSelected = selectedGiftId == gift.id,
                            onSelect = { selectedGiftId = gift.id }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            // Combo selector & Send CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedCategory != "Mi bolsa") {
                    listOf(1, 5, 10).forEach { mult ->
                        val active = comboCount == mult
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) NeonPurple else Color(0xFF252333),
                            border = BorderStroke(1.dp, if (active) NeonPink else BorderSubtle),
                            modifier = Modifier
                                .height(42.dp)
                                .clickable { comboCount = mult }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "×$mult",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }
                val canSend = if (selectedCategory == "Mi bolsa") {
                    selectedBagItemId != null && selectedRecipientId.isNotBlank()
                } else {
                    selectedRecipientId.isNotBlank()
                }
                val sendLabel = if (selectedCategory == "Mi bolsa") {
                    "Enviar gratis de Mi bolsa"
                } else {
                    val gift = GiftCatalog.byId(selectedGiftId)
                    "Enviar ${gift.emoji} (${gift.coinCost * comboCount} 🪙)"
                }
                NeonGradientButton(
                    text = sendLabel,
                    enabled = canSend,
                    onClick = {
                        if (selectedCategory == "Mi bolsa" && selectedBagItemId != null) {
                            onSendGift(selectedRecipientId, selectedBagItemId!!, 1, selectedBagItemId)
                        } else {
                            onSendGift(selectedRecipientId, selectedGiftId, comboCount, null)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "send_room_gift_confirm_button"
                )
            }
        }
    }
}

@Composable
private fun GiftGridTile(
    gift: GiftItem,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val accent = Color(gift.accentHex)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFF282438) else Color(0xFF1B1A23))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) NeonPink else BorderSubtle.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("gift_tile_${gift.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Gift3DAnimatedArtwork(
                gift = gift,
                size = 50.dp,
                intense = isSelected
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = gift.name,
                fontSize = 10.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("🪙", fontSize = 9.sp)
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = gift.coinCost.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoleAdminGold
                )
            }
        }
    }
}
