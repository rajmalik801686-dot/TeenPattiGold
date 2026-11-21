package com.example.ui.table

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.GamePhase
import com.example.domain.GameVariation
import com.example.domain.HandEvaluator
import com.example.domain.SeatPlayer
import com.example.domain.TableUiState
import com.example.ui.components.ChipBadge
import com.example.ui.components.FannedHandView
import com.example.ui.components.HandRankingsSheet
import com.example.ui.theme.AmberBlind
import com.example.ui.theme.CrimsonFold
import com.example.ui.theme.EmeraldFeltDark
import com.example.ui.theme.EmeraldFeltLight
import com.example.ui.theme.EmeraldFeltMid
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MahoganyRail
import com.example.ui.theme.MahoganyRailDark
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

private val avatarPalette = listOf(
    Color(0xFF8E24AA),
    Color(0xFF00897B),
    Color(0xFFD84315),
    Color(0xFF1E88E5),
    Color(0xFFC49222)
)

@Composable
fun TableScreen(
    viewModel: TableViewModel,
    onLeaveTable: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by viewModel.profileFlow.collectAsStateWithLifecycle(initialValue = null)
    var showRulesSheet by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = profile?.hapticsEnabled ?: true

    fun pulseHaptic() {
        if (hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    BackHandler {
        onLeaveTable()
    }

    Scaffold(
        containerColor = ObsidianNight,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianNight)
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Top HUD
            TableTopHud(
                state = state,
                onLeaveClick = onLeaveTable,
                onRulesClick = { showRulesSheet = true }
            )

            // Main Table Felt Arena
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                val isCompactHeight = maxHeight < 520.dp

                // Custom Canvas Felt Oval + Mahogany Rail + Gold Trim
                FeltTableCanvas(modifier = Modifier.fillMaxSize())

                val aiSeats = state.seats.filter { !it.isHuman }
                // Top Row AI Seats: Seat 2 (Top-Left) and Seat 3 (Top-Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 6.dp else 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    aiSeats.getOrNull(1)?.let { seat ->
                        AiSeatCard(
                            seat = seat,
                            isActiveTurn = state.activeSeat == seat.seatIndex && state.phase != GamePhase.ROUND_OVER,
                            isDealer = state.dealerSeat == seat.seatIndex,
                            revealCards = state.phase == GamePhase.ROUND_OVER && !seat.isPacked,
                            variation = state.variation,
                            jokerRank = state.jokerRank
                        )
                    }
                    aiSeats.getOrNull(2)?.let { seat ->
                        AiSeatCard(
                            seat = seat,
                            isActiveTurn = state.activeSeat == seat.seatIndex && state.phase != GamePhase.ROUND_OVER,
                            isDealer = state.dealerSeat == seat.seatIndex,
                            revealCards = state.phase == GamePhase.ROUND_OVER && !seat.isPacked,
                            variation = state.variation,
                            jokerRank = state.jokerRank
                        )
                    }
                }

                // Middle Row AI Seats: Seat 1 (Left) & Seat 4 (Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 6.dp)
                        .padding(bottom = if (isCompactHeight) 28.dp else 44.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    aiSeats.getOrNull(0)?.let { seat ->
                        AiSeatCard(
                            seat = seat,
                            isActiveTurn = state.activeSeat == seat.seatIndex && state.phase != GamePhase.ROUND_OVER,
                            isDealer = state.dealerSeat == seat.seatIndex,
                            revealCards = state.phase == GamePhase.ROUND_OVER && !seat.isPacked,
                            variation = state.variation,
                            jokerRank = state.jokerRank
                        )
                    }
                    aiSeats.getOrNull(3)?.let { seat ->
                        AiSeatCard(
                            seat = seat,
                            isActiveTurn = state.activeSeat == seat.seatIndex && state.phase != GamePhase.ROUND_OVER,
                            isDealer = state.dealerSeat == seat.seatIndex,
                            revealCards = state.phase == GamePhase.ROUND_OVER && !seat.isPacked,
                            variation = state.variation,
                            jokerRank = state.jokerRank
                        )
                    }
                }

                // Center Pot & Status Ticker
                CenterPotHub(
                    state = state,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = if (isCompactHeight) 24.dp else 40.dp)
                )

                // Human Player Seat at Bottom of Felt
                state.humanSeat?.let { human ->
                    HumanSeatArea(
                        seat = human,
                        isActiveTurn = state.activeSeat == 0 && state.phase == GamePhase.PLAYER_TURN,
                        isDealer = state.dealerSeat == 0,
                        variation = state.variation,
                        jokerRank = state.jokerRank,
                        phase = state.phase,
                        onSeeCards = {
                            pulseHaptic()
                            viewModel.humanSeeCards()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (isCompactHeight) 6.dp else 12.dp)
                    )
                }

                // Round Over Banner Overlay (above human seat or centered cleanly)
                this@Column.AnimatedVisibility(
                    visible = state.phase == GamePhase.ROUND_OVER && state.winnerBanner != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xEE0D151B),
                        border = BorderStroke(1.5.dp, RoyalGold),
                        shadowElevation = 10.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = RoyalGoldBright,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.winnerBanner.orEmpty(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = RoyalGoldBright,
                                textAlign = TextAlign.Center
                            )
                            state.winningHandLabel?.let { handText ->
                                Text(
                                    text = handText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Control Deck
            TableActionDeck(
                state = state,
                canSideshow = viewModel.canRequestSideshow(0, state),
                onPack = {
                    pulseHaptic()
                    viewModel.humanPack()
                },
                onSeeCards = {
                    pulseHaptic()
                    viewModel.humanSeeCards()
                },
                onBet = {
                    pulseHaptic()
                    viewModel.humanBet()
                },
                onToggleMultiplier = {
                    pulseHaptic()
                    viewModel.toggleRaiseMultiplier()
                },
                onSideshow = {
                    pulseHaptic()
                    viewModel.humanRequestSideshow()
                },
                onShow = {
                    pulseHaptic()
                    viewModel.humanRequestShow()
                },
                onNextHand = {
                    pulseHaptic()
                    viewModel.startNewRound()
                },
                onRefillAndDeal = {
                    pulseHaptic()
                    viewModel.claimEmergencyRefillAndDeal()
                },
                onLeaveTable = onLeaveTable
            )
        }
    }

    // Sideshow Incoming Prompt Dialog
    val sideshowOffer = state.sideshowOffer
    if (state.phase == GamePhase.SIDESHOW_PROMPT && sideshowOffer != null) {
        val requesterName = state.seats.getOrNull(sideshowOffer.requesterSeat)?.name ?: "Opponent"
        AlertDialog(
            onDismissRequest = {},
            containerColor = ObsidianSurface,
            titleContentColor = RoyalGoldBright,
            textContentColor = TextPrimary,
            title = {
                Text("Sideshow Request", style = MaterialTheme.typography.headlineMedium)
            },
            text = {
                Text(
                    "$requesterName wants to compare cards with you. The lower hand will pack automatically.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.respondToSideshow(accepted = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = ObsidianNight),
                    modifier = Modifier.testTag("accept_sideshow_button")
                ) {
                    Text("Accept", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.respondToSideshow(accepted = false) },
                    modifier = Modifier.testTag("decline_sideshow_button")
                ) {
                    Text("Decline", color = CrimsonFold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showRulesSheet) {
        HandRankingsSheet(onDismiss = { showRulesSheet = false })
    }
}

@Composable
private fun TableTopHud(
    state: TableUiState,
    onLeaveClick: () -> Unit,
    onRulesClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onLeaveClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("leave_table_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Leave table",
                    tint = TextPrimary
                )
            }

            Column {
                Text(
                    text = state.tier.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = RoyalGoldBright
                )
                Text(
                    text = "${state.variation.title} • Boot ${ChipFormatter.formatCompact(state.tier.bootAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (state.variation == GameVariation.JOKER && state.jokerRank != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = RoyalGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, RoyalGold)
                ) {
                    Text(
                        text = "WILD: ${state.jokerRank.symbol}",
                        fontFamily = MonoChipFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = RoyalGoldBright,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            } else if (state.variation == GameVariation.AK47) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = RoyalGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, RoyalGold)
                ) {
                    Text(
                        text = "WILD: A K 4 7",
                        fontFamily = MonoChipFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = RoyalGoldBright,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            IconButton(
                onClick = onRulesClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("table_rules_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Hand Rankings",
                    tint = RoyalGoldBright
                )
            }
        }
    }
}

@Composable
private fun FeltTableCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val padX = size.width * 0.04f
        val padY = size.height * 0.03f
        val tableW = size.width - padX * 2
        val tableH = size.height - padY * 2
        val radius = minOf(tableW, tableH) * 0.42f

        // Mahogany Outer Rail
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(MahoganyRail, MahoganyRailDark)
            ),
            topLeft = Offset(padX, padY),
            size = Size(tableW, tableH),
            cornerRadius = CornerRadius(radius, radius)
        )

        // Felt Interior
        val railThickness = 18f
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(EmeraldFeltLight, EmeraldFeltMid, EmeraldFeltDark),
                center = Offset(size.width / 2f, size.height * 0.44f),
                radius = maxOf(tableW, tableH) * 0.58f
            ),
            topLeft = Offset(padX + railThickness, padY + railThickness),
            size = Size(tableW - railThickness * 2, tableH - railThickness * 2),
            cornerRadius = CornerRadius(radius - 10f, radius - 10f)
        )

        // Burnished Gold Inner Line
        val goldInset = railThickness + 10f
        drawRoundRect(
            color = RoyalGold.copy(alpha = 0.45f),
            topLeft = Offset(padX + goldInset, padY + goldInset),
            size = Size(tableW - goldInset * 2, tableH - goldInset * 2),
            cornerRadius = CornerRadius(radius - 18f, radius - 18f),
            style = Stroke(width = 2.5f)
        )
    }
}

@Composable
private fun AiSeatCard(
    seat: SeatPlayer,
    isActiveTurn: Boolean,
    isDealer: Boolean,
    revealCards: Boolean,
    variation: GameVariation,
    jokerRank: com.example.domain.Rank?
) {
    val alpha = if (seat.isPacked) 0.45f else 1f
    val borderColor = when {
        seat.isWinner -> RoyalGoldBright
        isActiveTurn -> RoyalGold
        else -> Color(0x44FFFFFF)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(104.dp)
            .alpha(alpha)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ObsidianSurface.copy(alpha = 0.92f),
            border = BorderStroke(if (isActiveTurn || seat.isWinner) 2.dp else 1.dp, borderColor),
            shadowElevation = if (isActiveTurn) 8.dp else 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(avatarPalette[seat.colorSlot % avatarPalette.size])
                            .border(1.dp, RoyalGold.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = seat.initials,
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = seat.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = ChipFormatter.formatCompact(seat.chips),
                            fontFamily = MonoChipFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalGoldBright
                        )
                    }

                    if (isDealer) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(RoyalGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("D", color = ObsidianNight, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (seat.cards.isNotEmpty() && !seat.isPacked) {
                    FannedHandView(
                        cards = seat.cards,
                        faceUp = revealCards,
                        wildChecker = { HandEvaluator.isWildCard(it, variation, jokerRank) },
                        compact = true
                    )
                } else {
                    Text(
                        text = if (seat.isPacked) "PACKED" else "WAITING",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonFold
                    )
                }
            }
        }

        // Floating Action / Blind / Seen Pill
        seat.lastAction?.let { action ->
            Spacer(modifier = Modifier.height(3.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = when {
                    seat.isWinner -> RoyalGold
                    seat.isPacked -> CrimsonFold.copy(alpha = 0.85f)
                    seat.isBlind -> AmberBlind.copy(alpha = 0.9f)
                    else -> ObsidianElevated
                }
            ) {
                Text(
                    text = action,
                    color = if (seat.isWinner || seat.isBlind) ObsidianNight else TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CenterPotHub(
    state: TableUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.widthIn(max = 190.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xCC06150F),
            border = BorderStroke(1.5.dp, RoyalGold.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL POT",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoyalGold
                )
                Text(
                    text = ChipFormatter.formatFull(state.pot),
                    fontFamily = MonoChipFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = RoyalGoldBright
                )
                Spacer(modifier = Modifier.height(4.dp))
                val potProgress = (state.pot.toFloat() / state.tier.potLimit.coerceAtLeast(1L)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { potProgress },
                    modifier = Modifier
                        .width(120.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = RoyalGold,
                    trackColor = Color(0x33FFFFFF)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Blind: ${ChipFormatter.formatCompact(state.currentBlindStake)} • Chaal: ${ChipFormatter.formatCompact(state.currentBlindStake * 2L)}",
                    fontFamily = MonoChipFontFamily,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xBB0B1218)
        ) {
            Text(
                text = state.statusBanner,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun HumanSeatArea(
    seat: SeatPlayer,
    isActiveTurn: Boolean,
    isDealer: Boolean,
    variation: GameVariation,
    jokerRank: com.example.domain.Rank?,
    phase: GamePhase,
    onSeeCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Cards (tap to reveal if Blind)
        if (seat.cards.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = seat.isBlind && !seat.isPacked && phase != GamePhase.ROUND_OVER) {
                        onSeeCards()
                    }
                    .testTag("human_cards_area")
            ) {
                FannedHandView(
                    cards = seat.cards,
                    faceUp = !seat.isBlind || phase == GamePhase.ROUND_OVER,
                    wildChecker = { HandEvaluator.isWildCard(it, variation, jokerRank) },
                    compact = false
                )
            }
        }

        // Evaluation or Blind status pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChipBadge(amountText = ChipFormatter.formatCompact(seat.chips))

            val badgeText = when {
                seat.isPacked -> "PACKED"
                !seat.isBlind -> seat.evaluation?.label ?: "SEEN"
                else -> "BLIND (Tap to See)"
            }
            val badgeColor = when {
                seat.isPacked -> CrimsonFold
                !seat.isBlind -> EmeraldSuccess
                isActiveTurn -> RoyalGold
                else -> ObsidianElevated
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = badgeColor.copy(alpha = if (!seat.isBlind || seat.isPacked) 0.22f else 0.9f),
                border = BorderStroke(1.dp, badgeColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isDealer) {
                        Text("D •", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = RoyalGoldBright)
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (seat.isBlind && isActiveTurn && !seat.isPacked) ObsidianNight else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TableActionDeck(
    state: TableUiState,
    canSideshow: Boolean,
    onPack: () -> Unit,
    onSeeCards: () -> Unit,
    onBet: () -> Unit,
    onToggleMultiplier: () -> Unit,
    onSideshow: () -> Unit,
    onShow: () -> Unit,
    onNextHand: () -> Unit,
    onRefillAndDeal: () -> Unit,
    onLeaveTable: () -> Unit
) {
    val human = state.humanSeat
    val isHumanTurn = state.phase == GamePhase.PLAYER_TURN && state.activeSeat == 0 && human != null && !human.isPacked

    Surface(
        color = ObsidianSurface,
        border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.phase == GamePhase.ROUND_OVER) {
                val needsRefill = (human?.chips ?: 0L) < state.tier.bootAmount
                Row(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onLeaveTable,
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("round_over_leave_button")
                    ) {
                        Text("Lobby", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = if (needsRefill) onRefillAndDeal else onNextHand,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = ObsidianNight
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(2f)
                            .height(50.dp)
                            .testTag("next_hand_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (needsRefill) "Free Refill (+5,000) & Deal" else "Next Hand",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            } else {
                val isBlind = human?.isBlind ?: true
                val baseCost = if (isBlind) state.currentBlindStake else state.currentBlindStake * 2L
                val maxForSeat = if (isBlind) state.tier.maxChaal / 2L else state.tier.maxChaal
                val canDouble = (baseCost * 2L) <= maxForSeat && (human?.chips ?: 0L) >= (baseCost * 2L)
                val effectiveMult = if (state.raiseMultiplier == 2 && canDouble) 2 else 1
                val betAmount = (baseCost * effectiveMult).coerceAtMost(human?.chips ?: 0L)

                Row(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pack Button
                    OutlinedButton(
                        onClick = onPack,
                        enabled = isHumanTurn,
                        border = BorderStroke(1.2.dp, if (isHumanTurn) CrimsonFold else Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("pack_button")
                    ) {
                        Text(
                            text = "Pack",
                            color = if (isHumanTurn) CrimsonFold else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Middle Action: See Cards / Show / Sideshow
                    if (isBlind && human != null && !human.isPacked) {
                        OutlinedButton(
                            onClick = onSeeCards,
                            border = BorderStroke(1.2.dp, RoyalGold),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("see_cards_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = RoyalGoldBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("See", color = RoyalGoldBright, fontWeight = FontWeight.Bold)
                        }
                    } else if (state.activeCount == 2) {
                        Button(
                            onClick = onShow,
                            enabled = isHumanTurn,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldFeltLight,
                                contentColor = RoyalGoldBright,
                                disabledContainerColor = ObsidianElevated,
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("show_button")
                        ) {
                            Text("Show", fontWeight = FontWeight.ExtraBold)
                        }
                    } else if (canSideshow) {
                        OutlinedButton(
                            onClick = onSideshow,
                            enabled = isHumanTurn,
                            border = BorderStroke(1.2.dp, if (isHumanTurn) RoyalGold else Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("sideshow_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompareArrows,
                                contentDescription = null,
                                tint = if (isHumanTurn) RoyalGoldBright else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sideshow",
                                color = if (isHumanTurn) RoyalGoldBright else TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 1x / 2x Toggle
                    if (canDouble) {
                        Surface(
                            onClick = { if (isHumanTurn) onToggleMultiplier() },
                            shape = RoundedCornerShape(14.dp),
                            color = if (effectiveMult == 2) RoyalGoldDeep else ObsidianElevated,
                            border = BorderStroke(
                                1.dp,
                                if (effectiveMult == 2) RoyalGoldBright else RoyalGold.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .height(52.dp)
                                .width(52.dp)
                                .testTag("raise_multiplier_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${effectiveMult}x",
                                    fontFamily = MonoChipFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (effectiveMult == 2) ObsidianNight else RoyalGoldBright
                                )
                            }
                        }
                    }

                    // Primary Blind / Chaal Bet Button
                    Button(
                        onClick = onBet,
                        enabled = isHumanTurn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = ObsidianNight,
                            disabledContainerColor = ObsidianElevated,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("bet_button")
                    ) {
                        val verb = when {
                            effectiveMult == 2 && isBlind -> "Blind 2x"
                            effectiveMult == 2 -> "Raise"
                            isBlind -> "Blind"
                            else -> "Chaal"
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = verb,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                lineHeight = 15.sp
                            )
                            Text(
                                text = ChipFormatter.formatCompact(betAmount),
                                fontFamily = MonoChipFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
