package com.example.domain

import kotlinx.serialization.Serializable

@Serializable
enum class TableTier(
    val title: String,
    val subtitle: String,
    val bootAmount: Long,
    val maxChaal: Long,
    val potLimit: Long,
    val minEntryChips: Long,
    val aiStartingChips: Long
) {
    BRONZE(
        title = "Bronze Lounge",
        subtitle = "Casual warm-up stakes",
        bootAmount = 50L,
        maxChaal = 1_600L,
        potLimit = 12_500L,
        minEntryChips = 250L,
        aiStartingChips = 18_000L
    ),
    EMERALD(
        title = "Emerald Club",
        subtitle = "Classic club room",
        bootAmount = 250L,
        maxChaal = 8_000L,
        potLimit = 60_000L,
        minEntryChips = 1_250L,
        aiStartingChips = 55_000L
    ),
    ROYAL_GOLD(
        title = "Royal Gold",
        subtitle = "High-roller velvet felt",
        bootAmount = 1_000L,
        maxChaal = 32_000L,
        potLimit = 250_000L,
        minEntryChips = 5_000L,
        aiStartingChips = 180_000L
    ),
    MAHARAJA_VIP(
        title = "Maharaja VIP",
        subtitle = "Grand master stakes",
        bootAmount = 5_000L,
        maxChaal = 160_000L,
        potLimit = 1_250_000L,
        minEntryChips = 25_000L,
        aiStartingChips = 800_000L
    )
}

enum class GamePhase {
    IDLE,
    DEALING,
    PLAYER_TURN,
    AI_THINKING,
    SIDESHOW_PROMPT,
    ROUND_OVER
}

data class AiPersona(
    val name: String,
    val tagline: String,
    val initials: String,
    val aggression: Float,
    val bluffRate: Float,
    val blindTendency: Float,
    val colorSlot: Int
)

data class SeatPlayer(
    val seatIndex: Int,
    val id: String,
    val name: String,
    val initials: String,
    val colorSlot: Int,
    val isHuman: Boolean,
    val chips: Long,
    val cards: List<Card> = emptyList(),
    val isBlind: Boolean = true,
    val isPacked: Boolean = false,
    val isAllIn: Boolean = false,
    val totalBetInRound: Long = 0L,
    val lastAction: String? = null,
    val isWinner: Boolean = false,
    val evaluation: HandEvaluation? = null,
    val persona: AiPersona? = null
) {
    val isActiveInRound: Boolean get() = !isPacked && cards.isNotEmpty()
}

data class SideshowOffer(
    val requesterSeat: Int,
    val targetSeat: Int,
    val stakePaid: Long
)

data class TableUiState(
    val tier: TableTier = TableTier.BRONZE,
    val variation: GameVariation = GameVariation.CLASSIC,
    val jokerRank: Rank? = null,
    val pot: Long = 0L,
    val currentBlindStake: Long = 50L,
    val dealerSeat: Int = 0,
    val activeSeat: Int = 0,
    val roundNumber: Int = 0,
    val phase: GamePhase = GamePhase.IDLE,
    val seats: List<SeatPlayer> = emptyList(),
    val statusBanner: String = "Welcome to the table",
    val winnerBanner: String? = null,
    val winningHandLabel: String? = null,
    val sideshowOffer: SideshowOffer? = null,
    val actionHistory: List<String> = emptyList(),
    val raiseMultiplier: Int = 1 // 1x or 2x
) {
    val humanSeat: SeatPlayer? get() = seats.firstOrNull { it.isHuman }
    val activePlayers: List<SeatPlayer> get() = seats.filter { it.isActiveInRound }
    val activeCount: Int get() = activePlayers.size
}
