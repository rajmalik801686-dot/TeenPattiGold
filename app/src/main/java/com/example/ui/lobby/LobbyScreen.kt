package com.example.ui.lobby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.GameVariation
import com.example.domain.TableTier
import com.example.ui.components.ChipBadge
import com.example.ui.components.HandRankingsSheet
import com.example.ui.components.ProfileAndBonusDialog
import com.example.ui.theme.EmeraldFeltDark
import com.example.ui.theme.EmeraldFeltLight
import com.example.ui.theme.EmeraldFeltMid
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MonoChipFontFamily
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldBright
import com.example.ui.theme.RoyalGoldDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ChipFormatter

@Composable
fun LobbyScreen(
    viewModel: LobbyViewModel,
    onEnterTable: (TableTier, GameVariation) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showRulesSheet by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.bonusMessage) {
        val msg = uiState.bonusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBonusMessage()
        }
    }

    Scaffold(
        containerColor = ObsidianNight,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(EmeraldFeltDark, ObsidianNight, ObsidianNight)
                    )
                )
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                val isWide = maxWidth >= 680.dp

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 900.dp)
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Bar
                    item {
                        LobbyTopBar(
                            playerName = uiState.profile.playerName,
                            chips = uiState.profile.chips,
                            canClaimBonus = uiState.profile.canClaimDailyBonus,
                            onProfileClick = { showProfileDialog = true },
                            onBonusClick = {
                                if (uiState.profile.canClaimDailyBonus) {
                                    viewModel.claimDailyBonus()
                                } else {
                                    showProfileDialog = true
                                }
                            },
                            onRulesClick = { showRulesSheet = true }
                        )
                    }

                    // Hero Banner Card
                    item {
                        HeroLobbyBanner(
                            selectedVariation = uiState.selectedVariation,
                            chips = uiState.profile.chips,
                            onQuickPlay = {
                                val bestTier = TableTier.entries
                                    .lastOrNull { uiState.profile.chips >= it.minEntryChips }
                                    ?: TableTier.BRONZE
                                if (uiState.profile.chips < TableTier.BRONZE.bootAmount) {
                                    showProfileDialog = true
                                } else {
                                    onEnterTable(bestTier, uiState.selectedVariation)
                                }
                            }
                        )
                    }

                    // Low chips prompt
                    item {
                        AnimatedVisibility(visible = uiState.profile.chips < TableTier.BRONZE.minEntryChips) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EmeraldFeltMid,
                                border = BorderStroke(1.dp, RoyalGold)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Running Low on Virtual Chips?",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = RoyalGoldBright
                                        )
                                        Text(
                                            text = "Claim a free 5,000 chip refill anytime.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = { viewModel.claimEmergencyRefill() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RoyalGold,
                                            contentColor = ObsidianNight
                                        ),
                                        modifier = Modifier.testTag("lobby_refill_button")
                                    ) {
                                        Text("+5,000", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Game Variations Selector
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Variation",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = uiState.selectedVariation.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoyalGold
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(GameVariation.entries) { variation ->
                                    VariationChipCard(
                                        variation = variation,
                                        selected = variation == uiState.selectedVariation,
                                        onClick = { viewModel.selectVariation(variation) }
                                    )
                                }
                            }

                            Text(
                                text = uiState.selectedVariation.ruleSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    // Table Stakes Section Header
                    item {
                        Text(
                            text = "Choose Your Table",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }

                    // Table Cards (1 column on phone, 2 columns on tablet/wide)
                    val tiers = TableTier.entries
                    if (isWide) {
                        val rows = tiers.chunked(2)
                        items(rows) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { tier ->
                                    TableTierCard(
                                        tier = tier,
                                        playerChips = uiState.profile.chips,
                                        onJoin = { onEnterTable(tier, uiState.selectedVariation) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(tiers) { tier ->
                            TableTierCard(
                                tier = tier,
                                playerChips = uiState.profile.chips,
                                onJoin = { onEnterTable(tier, uiState.selectedVariation) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Footer entertainment disclaimer
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Virtual Chips Only • Entertainment Game • No Real-Money Gambling",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRulesSheet) {
        HandRankingsSheet(onDismiss = { showRulesSheet = false })
    }

    if (showProfileDialog) {
        ProfileAndBonusDialog(
            profile = uiState.profile,
            onDismiss = { showProfileDialog = false },
            onSaveName = { viewModel.updatePlayerName(it) },
            onClaimDailyBonus = { viewModel.claimDailyBonus() },
            onClaimEmergencyRefill = { viewModel.claimEmergencyRefill() },
            onSoundChanged = { viewModel.setSoundEnabled(it) },
            onHapticsChanged = { viewModel.setHapticsEnabled(it) }
        )
    }
}

@Composable
private fun LobbyTopBar(
    playerName: String,
    chips: Long,
    canClaimBonus: Boolean,
    onProfileClick: () -> Unit,
    onBonusClick: () -> Unit,
    onRulesClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Player profile pill
        Surface(
            onClick = onProfileClick,
            shape = RoundedCornerShape(50),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.4f)),
            modifier = Modifier.testTag("profile_button")
        ) {
            Row(
                modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(EmeraldFeltLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Player profile",
                        tint = RoyalGoldBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = playerName,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Stats & Lounge",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChipBadge(amountText = ChipFormatter.formatCompact(chips))

            // Daily Bonus IconButton
            Surface(
                onClick = onBonusClick,
                shape = CircleShape,
                color = if (canClaimBonus) RoyalGold else ObsidianSurface,
                border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.6f)),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("daily_bonus_icon_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Daily Bonus",
                        tint = if (canClaimBonus) ObsidianNight else RoyalGold
                    )
                }
            }

            IconButton(
                onClick = onRulesClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("lobby_rules_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Hand Rankings and Rules",
                    tint = RoyalGoldBright
                )
            }
        }
    }
}

@Composable
private fun HeroLobbyBanner(
    selectedVariation: GameVariation,
    chips: Long,
    onQuickPlay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, RoyalGold.copy(alpha = 0.65f)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_royal_lobby_banner_1795239850098),
                contentDescription = "Royal Teen Patti Gold lounge banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xE6061A13),
                                Color(0x99061A13),
                                Color(0x33061A13)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = RoyalGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "5-PLAYER TABLE • ${selectedVariation.title.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalGoldBright,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TEEN PATTI GOLD",
                        style = MaterialTheme.typography.displaySmall,
                        color = RoyalGoldBright
                    )
                    Text(
                        text = "Royal 3-Card Indian Poker • Virtual Chips",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary.copy(alpha = 0.88f)
                    )
                }

                Button(
                    onClick = onQuickPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = ObsidianNight
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("quick_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (chips < TableTier.BRONZE.bootAmount) "Claim Refill to Play" else "Deal Now",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VariationChipCard(
    variation: GameVariation,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("variation_${variation.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) EmeraldFeltMid else ObsidianSurface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) RoyalGold else Color(0x33FFFFFF)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Style,
                contentDescription = null,
                tint = if (selected) RoyalGoldBright else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = variation.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) RoyalGoldBright else TextPrimary
                )
                Text(
                    text = variation.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) EmeraldSuccess else TextMuted
                )
            }
        }
    }
}

@Composable
private fun TableTierCard(
    tier: TableTier,
    playerChips: Long,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canAfford = playerChips >= tier.minEntryChips
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = canAfford, onClick = onJoin)
            .testTag("table_tier_${tier.name.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        color = if (canAfford) ObsidianSurface else ObsidianElevated.copy(alpha = 0.65f),
        border = BorderStroke(
            width = 1.2.dp,
            color = if (canAfford) RoyalGold.copy(alpha = 0.45f) else Color(0x22FFFFFF)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tier.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (canAfford) RoyalGoldBright else TextMuted
                    )
                    if (!canAfford) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = tier.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StakeMeta(label = "Boot", value = ChipFormatter.formatCompact(tier.bootAmount))
                    StakeMeta(label = "Max Pot", value = ChipFormatter.formatCompact(tier.potLimit))
                    StakeMeta(label = "Entry", value = ChipFormatter.formatCompact(tier.minEntryChips))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onJoin,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = ObsidianNight,
                    disabledContainerColor = ObsidianElevated,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = if (canAfford) "Play" else "Locked",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StakeMeta(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            fontFamily = MonoChipFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = TextPrimary
        )
    }
}
