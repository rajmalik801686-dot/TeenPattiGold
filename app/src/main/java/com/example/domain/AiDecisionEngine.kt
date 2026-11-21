package com.example.domain

import kotlin.random.Random

sealed interface AiMove {
    data object SeeCards : AiMove
    data class Bet(val isRaise: Boolean) : AiMove
    data object Pack : AiMove
    data object RequestSideshow : AiMove
    data object RequestShow : AiMove
}

object AiDecisionEngine {

    val defaultPersonas = listOf(
        AiPersona(
            name = "Vikram",
            tagline = "Calculated Shark",
            initials = "VK",
            aggression = 0.72f,
            bluffRate = 0.24f,
            blindTendency = 0.35f,
            colorSlot = 0
        ),
        AiPersona(
            name = "Ananya",
            tagline = "Royal Strategist",
            initials = "AN",
            aggression = 0.55f,
            bluffRate = 0.14f,
            blindTendency = 0.25f,
            colorSlot = 1
        ),
        AiPersona(
            name = "Kabir",
            tagline = "Blind Daredevil",
            initials = "KB",
            aggression = 0.68f,
            bluffRate = 0.30f,
            blindTendency = 0.68f,
            colorSlot = 2
        ),
        AiPersona(
            name = "Meera",
            tagline = "Velvet Closer",
            initials = "MR",
            aggression = 0.45f,
            bluffRate = 0.12f,
            blindTendency = 0.20f,
            colorSlot = 3
        )
    )

    fun chooseMove(
        seat: SeatPlayer,
        state: TableUiState,
        canSideshow: Boolean,
        canShow: Boolean,
        random: Random = Random.Default
    ): AiMove {
        val persona = seat.persona ?: defaultPersonas.first()
        val blindStake = state.currentBlindStake
        val requiredBet = if (seat.isBlind) blindStake else blindStake * 2L
        val raiseBet = requiredBet * 2L
        val canRaise = raiseBet <= state.tier.maxChaal && seat.chips >= raiseBet

        // If AI has very few chips left relative to required bet, decide Show or Pack or All-in call
        if (seat.chips < requiredBet) {
            return if (canShow && random.nextFloat() < 0.7f) AiMove.RequestShow else AiMove.Bet(isRaise = false)
        }

        // If currently Blind, decide whether to stay Blind or See Cards
        if (seat.isBlind) {
            val stakeRatio = blindStake.toFloat() / state.tier.bootAmount.coerceAtLeast(1L)
            val stayBlindChance = (persona.blindTendency - (stakeRatio - 1f) * 0.12f).coerceIn(0.08f, 0.75f)
            if (random.nextFloat() < stayBlindChance) {
                val shouldRaise = canRaise && random.nextFloat() < (persona.aggression * 0.28f)
                return AiMove.Bet(isRaise = shouldRaise)
            }
            return AiMove.SeeCards
        }

        // Seen player decision based on hand percentile and pot escalation
        val percentile = seat.evaluation?.percentile ?: 0.35f
        val stakePressure = (requiredBet.toFloat() / state.tier.maxChaal.coerceAtLeast(1L)).coerceIn(0f, 1f)
        val isBluffing = random.nextFloat() < persona.bluffRate

        // Head-to-head (2 players left)
        if (canShow) {
            if (percentile >= 0.78f && canRaise && state.pot < state.tier.potLimit * 0.6f && random.nextFloat() < persona.aggression) {
                return AiMove.Bet(isRaise = true)
            }
            if (percentile >= 0.48f && random.nextFloat() < 0.45f) {
                return AiMove.RequestShow
            }
            if (percentile < 0.35f && !isBluffing && stakePressure > 0.15f) {
                return AiMove.Pack
            }
            return if (random.nextFloat() < 0.55f) AiMove.RequestShow else AiMove.Bet(isRaise = false)
        }

        // 3+ players active
        if (percentile >= 0.74f) {
            val shouldRaise = canRaise && random.nextFloat() < (persona.aggression * 0.55f)
            return AiMove.Bet(isRaise = shouldRaise)
        }

        if (percentile >= 0.46f) {
            if (canSideshow && stakePressure > 0.12f && random.nextFloat() < 0.35f) {
                return AiMove.RequestSideshow
            }
            return AiMove.Bet(isRaise = false)
        }

        // Weak hand
        if (isBluffing && stakePressure < 0.30f) {
            val shouldRaise = canRaise && random.nextFloat() < 0.35f
            return AiMove.Bet(isRaise = shouldRaise)
        }

        if (canSideshow && random.nextFloat() < 0.25f) {
            return AiMove.RequestSideshow
        }

        val foldThreshold = (0.35f + stakePressure * 0.65f) - persona.aggression * 0.18f
        return if (random.nextFloat() < foldThreshold) {
            AiMove.Pack
        } else {
            AiMove.Bet(isRaise = false)
        }
    }

    fun shouldAcceptSideshow(seat: SeatPlayer, random: Random = Random.Default): Boolean {
        val percentile = seat.evaluation?.percentile ?: 0.5f
        val persona = seat.persona ?: defaultPersonas.first()
        return when {
            percentile >= 0.68f -> true
            percentile <= 0.30f -> random.nextFloat() < 0.25f
            else -> random.nextFloat() < (0.45f + persona.aggression * 0.3f)
        }
    }
}
