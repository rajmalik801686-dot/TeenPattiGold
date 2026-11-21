package com.example.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PlayerProfile
import com.example.data.UserPreferencesRepository
import com.example.domain.AiDecisionEngine
import com.example.domain.AiMove
import com.example.domain.GamePhase
import com.example.domain.GameVariation
import com.example.domain.HandEvaluator
import com.example.domain.Rank
import com.example.domain.SeatPlayer
import com.example.domain.SideshowOffer
import com.example.domain.TableTier
import com.example.domain.TableUiState
import com.example.util.ChipFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min

class TableViewModel(
    private val repository: UserPreferencesRepository,
    private val tier: TableTier,
    private val variation: GameVariation
) : ViewModel() {

    private val _uiState = MutableStateFlow(TableUiState(tier = tier, variation = variation))
    val uiState: StateFlow<TableUiState> = _uiState.asStateFlow()

    val profileFlow = repository.profileFlow

    private var roundJob: Job? = null
    private var initialized = false

    init {
        initializeTable()
    }

    private fun initializeTable() {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val profile: PlayerProfile = repository.profileFlow.first()
            val humanInitials = profile.playerName.take(2).uppercase().ifEmpty { "RJ" }
            val personas = AiDecisionEngine.defaultPersonas
            val seats = buildList {
                add(
                    SeatPlayer(
                        seatIndex = 0,
                        id = "human",
                        name = profile.playerName,
                        initials = humanInitials,
                        colorSlot = 4,
                        isHuman = true,
                        chips = profile.chips
                    )
                )
                personas.forEachIndexed { idx, persona ->
                    add(
                        SeatPlayer(
                            seatIndex = idx + 1,
                            id = "ai_${idx + 1}",
                            name = persona.name,
                            initials = persona.initials,
                            colorSlot = persona.colorSlot,
                            isHuman = false,
                            chips = tier.aiStartingChips,
                            persona = persona
                        )
                    )
                }
            }
            _uiState.value = TableUiState(
                tier = tier,
                variation = variation,
                seats = seats,
                dealerSeat = 0,
                roundNumber = 0,
                phase = GamePhase.IDLE
            )
            startNewRound()
        }
    }

    fun startNewRound() {
        roundJob?.cancel()
        roundJob = viewModelScope.launch {
            val latestProfile = repository.profileFlow.first()
            val currentState = _uiState.value
            if (latestProfile.chips < tier.bootAmount) {
                _uiState.update { state ->
                    state.copy(
                        phase = GamePhase.ROUND_OVER,
                        seats = state.seats.map {
                            if (it.isHuman) it.copy(chips = latestProfile.chips, cards = emptyList(), isPacked = true)
                            else it
                        },
                        statusBanner = "Need ${ChipFormatter.formatCompact(tier.bootAmount)} chips to deal",
                        winnerBanner = "Low Chips",
                        winningHandLabel = "Claim a free refill or choose a lower table"
                    )
                }
                return@launch
            }

            val nextRound = currentState.roundNumber + 1
            val nextDealer = (currentState.dealerSeat + 1) % currentState.seats.size
            val jokerRank = if (variation == GameVariation.JOKER) Rank.entries.random() else null
            val deck = HandEvaluator.createShuffledDeck()

            var potTotal = 0L
            val updatedSeats = currentState.seats.mapIndexed { idx, seat ->
                // Rebuy AI players if below boot
                val bankroll = when {
                    seat.isHuman -> latestProfile.chips
                    seat.chips < tier.bootAmount * 4L -> tier.aiStartingChips
                    else -> seat.chips
                }
                val bootPaid = min(bankroll, tier.bootAmount)
                potTotal += bootPaid
                val hand = deck.subList(idx * 3, idx * 3 + 3)
                val evaluation = HandEvaluator.evaluate(hand, variation, jokerRank)
                seat.copy(
                    chips = bankroll - bootPaid,
                    cards = hand,
                    isBlind = true,
                    isPacked = false,
                    isAllIn = (bankroll - bootPaid) == 0L,
                    totalBetInRound = bootPaid,
                    lastAction = "Boot ${ChipFormatter.formatCompact(bootPaid)}",
                    isWinner = false,
                    evaluation = evaluation
                )
            }

            val humanAfterBoot = updatedSeats.first { it.isHuman }.chips
            repository.updateChips(humanAfterBoot)

            val firstTurnSeat = (nextDealer + 1) % updatedSeats.size
            val jokerNotice = if (jokerRank != null) " • Joker: ${jokerRank.symbol}" else ""

            _uiState.value = currentState.copy(
                jokerRank = jokerRank,
                pot = potTotal,
                currentBlindStake = tier.bootAmount,
                dealerSeat = nextDealer,
                activeSeat = firstTurnSeat,
                roundNumber = nextRound,
                phase = GamePhase.DEALING,
                seats = updatedSeats,
                statusBanner = "Round #$nextRound dealt$jokerNotice",
                winnerBanner = null,
                winningHandLabel = null,
                sideshowOffer = null,
                actionHistory = listOf("Boot ${ChipFormatter.formatCompact(tier.bootAmount)} collected from all seats"),
                raiseMultiplier = 1
            )

            delay(550L)
            advanceToTurn(firstTurnSeat)
        }
    }

    fun claimEmergencyRefillAndDeal() {
        viewModelScope.launch {
            repository.claimEmergencyRefill(5_000L)
            startNewRound()
        }
    }

    fun toggleRaiseMultiplier() {
        _uiState.update { state ->
            val nextMult = if (state.raiseMultiplier == 1) 2 else 1
            state.copy(raiseMultiplier = nextMult)
        }
    }

    fun humanSeeCards() {
        val state = _uiState.value
        val human = state.humanSeat ?: return
        if (!human.isBlind || human.isPacked) return

        _uiState.update { current ->
            val updated = current.seats.map { seat ->
                if (seat.isHuman) {
                    seat.copy(isBlind = false, lastAction = "Seen")
                } else seat
            }
            current.copy(
                seats = updated,
                statusBanner = "Your hand: ${human.evaluation?.label ?: "Seen"}",
                actionHistory = (listOf("${human.name} saw cards") + current.actionHistory).take(8)
            )
        }
    }

    fun humanBet(isRaise: Boolean = _uiState.value.raiseMultiplier == 2) {
        val state = _uiState.value
        if (state.phase != GamePhase.PLAYER_TURN || state.activeSeat != 0) return
        performBet(seatIndex = 0, isRaise = isRaise)
    }

    fun humanPack() {
        val state = _uiState.value
        if (state.phase != GamePhase.PLAYER_TURN || state.activeSeat != 0) return
        performPack(seatIndex = 0)
    }

    fun humanRequestSideshow() {
        val state = _uiState.value
        if (state.phase != GamePhase.PLAYER_TURN || state.activeSeat != 0) return
        if (!canRequestSideshow(0, state)) return
        performSideshowRequest(seatIndex = 0)
    }

    fun humanRequestShow() {
        val state = _uiState.value
        if (state.phase != GamePhase.PLAYER_TURN || state.activeSeat != 0) return
        if (state.activeCount != 2) return
        performShow(seatIndex = 0)
    }

    fun respondToSideshow(accepted: Boolean) {
        val state = _uiState.value
        val offer = state.sideshowOffer ?: return
        if (state.phase != GamePhase.SIDESHOW_PROMPT) return
        resolveSideshow(offer, accepted)
    }

    fun canRequestSideshow(seatIndex: Int, state: TableUiState = _uiState.value): Boolean {
        if (state.activeCount < 3) return false
        val seat = state.seats.getOrNull(seatIndex) ?: return false
        if (seat.isBlind || seat.isPacked) return false
        val prevSeat = findPreviousActiveSeat(seatIndex, state.seats) ?: return false
        val seenCost = state.currentBlindStake * 2L
        return !prevSeat.isBlind && prevSeat.seatIndex != seatIndex && seat.chips >= seenCost
    }

    private fun advanceToTurn(seatIndex: Int) {
        val state = _uiState.value
        if (state.phase == GamePhase.ROUND_OVER) return

        // Check if only 1 player remains active -> automatic win
        val active = state.activePlayers
        if (active.size <= 1) {
            concludeRoundByLastStanding()
            return
        }

        // Check pot limit auto-show
        if (state.pot >= tier.potLimit) {
            concludeRoundByShowdown("Pot limit reached! Showdown!")
            return
        }

        val nextActiveIndex = findNextActiveSeatIndex(seatIndex, state.seats)
        val seat = state.seats[nextActiveIndex]

        if (seat.isHuman) {
            _uiState.update {
                it.copy(
                    activeSeat = nextActiveIndex,
                    phase = GamePhase.PLAYER_TURN,
                    raiseMultiplier = 1,
                    statusBanner = "Your turn (${if (seat.isBlind) "Blind" else "Seen"})"
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    activeSeat = nextActiveIndex,
                    phase = GamePhase.AI_THINKING,
                    statusBanner = "${seat.name} is thinking…"
                )
            }
            scheduleAiTurn(nextActiveIndex)
        }
    }

    private fun scheduleAiTurn(seatIndex: Int) {
        roundJob?.cancel()
        roundJob = viewModelScope.launch {
            delay(850L)
            var state = _uiState.value
            var seat = state.seats.getOrNull(seatIndex) ?: return@launch
            if (!seat.isActiveInRound || state.phase == GamePhase.ROUND_OVER) return@launch

            val canSideshow = canRequestSideshow(seatIndex, state)
            val canShow = state.activeCount == 2

            var move = AiDecisionEngine.chooseMove(seat, state, canSideshow, canShow)

            // If AI decides to look at cards first, update blind -> seen, pause briefly, then act!
            if (move is AiMove.SeeCards) {
                _uiState.update { current ->
                    current.copy(
                        seats = current.seats.map {
                            if (it.seatIndex == seatIndex) it.copy(isBlind = false, lastAction = "Seen") else it
                        },
                        statusBanner = "${seat.name} checked their cards",
                        actionHistory = (listOf("${seat.name} saw cards") + current.actionHistory).take(8)
                    )
                }
                delay(600L)
                state = _uiState.value
                seat = state.seats[seatIndex]
                move = AiDecisionEngine.chooseMove(
                    seat = seat,
                    state = state,
                    canSideshow = canRequestSideshow(seatIndex, state),
                    canShow = state.activeCount == 2
                )
            }

            when (move) {
                is AiMove.Bet -> performBet(seatIndex, move.isRaise)
                AiMove.Pack -> performPack(seatIndex)
                AiMove.RequestSideshow -> performSideshowRequest(seatIndex)
                AiMove.RequestShow -> performShow(seatIndex)
                AiMove.SeeCards -> performBet(seatIndex, isRaise = false)
            }
        }
    }

    private fun performBet(seatIndex: Int, isRaise: Boolean) {
        val state = _uiState.value
        val seat = state.seats[seatIndex]
        val baseStake = if (seat.isBlind) state.currentBlindStake else state.currentBlindStake * 2L
        val proposedStake = if (isRaise) baseStake * 2L else baseStake
        val maxAllowed = if (seat.isBlind) state.tier.maxChaal / 2L else state.tier.maxChaal
        val cappedStake = proposedStake.coerceAtMost(maxAllowed)
        val newBlindStake = if (isRaise && cappedStake > baseStake) {
            (state.currentBlindStake * 2L).coerceAtMost(state.tier.maxChaal / 2L)
        } else {
            state.currentBlindStake
        }

        val paid = min(seat.chips, cappedStake)
        val allIn = (seat.chips - paid) == 0L
        val verb = when {
            allIn -> "All-In"
            isRaise && cappedStake > baseStake -> if (seat.isBlind) "Blind 2x" else "Raise"
            seat.isBlind -> "Blind"
            else -> "Chaal"
        }
        val actionLabel = "$verb ${ChipFormatter.formatCompact(paid)}"

        var updatedHumanChips: Long? = null
        val updatedSeats = state.seats.map { s ->
            if (s.seatIndex == seatIndex) {
                val left = s.chips - paid
                if (s.isHuman) updatedHumanChips = left
                s.copy(
                    chips = left,
                    isAllIn = allIn,
                    totalBetInRound = s.totalBetInRound + paid,
                    lastAction = actionLabel
                )
            } else s
        }

        val newPot = state.pot + paid
        _uiState.update {
            it.copy(
                pot = newPot,
                currentBlindStake = newBlindStake,
                seats = updatedSeats,
                statusBanner = "${seat.name}: $actionLabel",
                actionHistory = (listOf("${seat.name}: $actionLabel") + it.actionHistory).take(8),
                raiseMultiplier = 1
            )
        }

        updatedHumanChips?.let { chips ->
            viewModelScope.launch { repository.updateChips(chips) }
        }

        advanceToTurn((seatIndex + 1) % updatedSeats.size)
    }

    private fun performPack(seatIndex: Int) {
        val state = _uiState.value
        val seat = state.seats[seatIndex]
        val updatedSeats = state.seats.map { s ->
            if (s.seatIndex == seatIndex) {
                s.copy(isPacked = true, lastAction = "Pack")
            } else s
        }

        _uiState.update {
            it.copy(
                seats = updatedSeats,
                statusBanner = "${seat.name} packed",
                actionHistory = (listOf("${seat.name} packed") + it.actionHistory).take(8),
                raiseMultiplier = 1
            )
        }

        val activeAfterPack = updatedSeats.filter { it.isActiveInRound }
        if (activeAfterPack.size <= 1) {
            concludeRoundByLastStanding()
        } else {
            advanceToTurn((seatIndex + 1) % updatedSeats.size)
        }
    }

    private fun performSideshowRequest(seatIndex: Int) {
        val state = _uiState.value
        val requester = state.seats[seatIndex]
        val target = findPreviousActiveSeat(seatIndex, state.seats) ?: return
        val stake = min(requester.chips, state.currentBlindStake * 2L)

        var updatedHumanChips: Long? = null
        val updatedSeats = state.seats.map { s ->
            if (s.seatIndex == seatIndex) {
                val left = s.chips - stake
                if (s.isHuman) updatedHumanChips = left
                s.copy(
                    chips = left,
                    totalBetInRound = s.totalBetInRound + stake,
                    lastAction = "Sideshow"
                )
            } else s
        }

        updatedHumanChips?.let { chips ->
            viewModelScope.launch { repository.updateChips(chips) }
        }

        val offer = SideshowOffer(requesterSeat = seatIndex, targetSeat = target.seatIndex, stakePaid = stake)
        _uiState.update {
            it.copy(
                pot = it.pot + stake,
                seats = updatedSeats,
                sideshowOffer = offer,
                statusBanner = "${requester.name} asked ${target.name} for a Sideshow",
                actionHistory = (listOf("${requester.name} → Sideshow ${target.name}") + it.actionHistory).take(8)
            )
        }

        if (target.isHuman) {
            _uiState.update { it.copy(phase = GamePhase.SIDESHOW_PROMPT) }
        } else {
            roundJob?.cancel()
            roundJob = viewModelScope.launch {
                delay(750L)
                val accept = AiDecisionEngine.shouldAcceptSideshow(target)
                resolveSideshow(offer, accept)
            }
        }
    }

    private fun resolveSideshow(offer: SideshowOffer, accepted: Boolean) {
        val state = _uiState.value
        val requester = state.seats[offer.requesterSeat]
        val target = state.seats[offer.targetSeat]

        if (!accepted) {
            _uiState.update {
                it.copy(
                    sideshowOffer = null,
                    statusBanner = "${target.name} declined Sideshow",
                    actionHistory = (listOf("${target.name} declined Sideshow") + it.actionHistory).take(8)
                )
            }
            advanceToTurn((offer.requesterSeat + 1) % state.seats.size)
            return
        }

        val reqScore = requester.evaluation?.effectiveScore ?: 0
        val tgtScore = target.evaluation?.effectiveScore ?: 0
        // If requester beats target strictly, target packs; otherwise (tie or loss) requester packs.
        val loserIndex = if (reqScore > tgtScore) offer.targetSeat else offer.requesterSeat
        val loserName = state.seats[loserIndex].name
        val winnerName = if (loserIndex == offer.targetSeat) requester.name else target.name

        val updatedSeats = state.seats.map { s ->
            if (s.seatIndex == loserIndex) {
                s.copy(isPacked = true, lastAction = "Lost Show")
            } else s
        }

        _uiState.update {
            it.copy(
                seats = updatedSeats,
                sideshowOffer = null,
                statusBanner = "Sideshow: $winnerName beat $loserName!",
                actionHistory = (listOf("Sideshow: $winnerName outranked $loserName") + it.actionHistory).take(8)
            )
        }

        val remaining = updatedSeats.filter { it.isActiveInRound }
        if (remaining.size <= 1) {
            concludeRoundByLastStanding()
        } else {
            advanceToTurn((offer.requesterSeat + 1) % updatedSeats.size)
        }
    }

    private fun performShow(seatIndex: Int) {
        val state = _uiState.value
        val caller = state.seats[seatIndex]
        val baseStake = if (caller.isBlind) state.currentBlindStake else state.currentBlindStake * 2L
        val paid = min(caller.chips, baseStake)

        var updatedHumanChips: Long? = null
        val updatedSeats = state.seats.map { s ->
            if (s.seatIndex == seatIndex) {
                val left = s.chips - paid
                if (s.isHuman) updatedHumanChips = left
                s.copy(
                    chips = left,
                    totalBetInRound = s.totalBetInRound + paid,
                    lastAction = "Show"
                )
            } else s
        }

        _uiState.update {
            it.copy(
                pot = it.pot + paid,
                seats = updatedSeats
            )
        }

        updatedHumanChips?.let { chips ->
            viewModelScope.launch { repository.updateChips(chips) }
        }

        concludeRoundByShowdown("${caller.name} called Show!")
    }

    private fun concludeRoundByLastStanding() {
        val state = _uiState.value
        val winner = state.activePlayers.firstOrNull() ?: return
        val potWon = state.pot

        val finalSeats = state.seats.map { seat ->
            if (seat.seatIndex == winner.seatIndex) {
                seat.copy(
                    chips = seat.chips + potWon,
                    isWinner = true,
                    lastAction = "Won ${ChipFormatter.formatCompact(potWon)}"
                )
            } else seat
        }

        val humanSeat = finalSeats.first { it.isHuman }
        val humanWon = winner.isHuman

        _uiState.update {
            it.copy(
                phase = GamePhase.ROUND_OVER,
                seats = finalSeats,
                sideshowOffer = null,
                statusBanner = "${winner.name} wins ${ChipFormatter.formatCompact(potWon)} uncontested!",
                winnerBanner = if (humanWon) "You Won ${ChipFormatter.formatCompact(potWon)}!" else "${winner.name} Won ${ChipFormatter.formatCompact(potWon)}",
                winningHandLabel = "All other players packed",
                actionHistory = (listOf("${winner.name} took pot ${ChipFormatter.formatCompact(potWon)}") + it.actionHistory).take(8)
            )
        }

        viewModelScope.launch {
            repository.recordHandResult(
                newChips = humanSeat.chips,
                won = humanWon,
                potWon = if (humanWon) potWon else 0L,
                handLabel = if (!humanSeat.isBlind || humanWon) humanSeat.evaluation?.label else null
            )
        }
    }

    private fun concludeRoundByShowdown(header: String) {
        val state = _uiState.value
        val contenders = state.activePlayers
        if (contenders.isEmpty()) return

        val topScore = contenders.maxOf { it.evaluation?.effectiveScore ?: Int.MIN_VALUE }
        val winners = contenders.filter { (it.evaluation?.effectiveScore ?: Int.MIN_VALUE) == topScore }
        val share = state.pot / winners.size
        val remainder = state.pot % winners.size

        val finalSeats = state.seats.map { seat ->
            val winIdx = winners.indexOfFirst { it.seatIndex == seat.seatIndex }
            if (winIdx >= 0) {
                val prize = share + if (winIdx == 0) remainder else 0L
                seat.copy(
                    chips = seat.chips + prize,
                    isBlind = false, // reveal cards at showdown
                    isWinner = true,
                    lastAction = "Won ${ChipFormatter.formatCompact(prize)}"
                )
            } else if (seat.isActiveInRound) {
                seat.copy(isBlind = false) // reveal all showdown hands
            } else {
                seat
            }
        }

        val humanSeat = finalSeats.first { it.isHuman }
        val humanWon = winners.any { it.isHuman }
        val primaryWinner = winners.first()
        val winHandText = primaryWinner.evaluation?.label ?: "Showdown"
        val winnerTitle = when {
            winners.size > 1 -> "Split Pot! (${winners.joinToString(" & ") { it.name }})"
            humanWon -> "You Won ${ChipFormatter.formatCompact(state.pot)}!"
            else -> "${primaryWinner.name} Won ${ChipFormatter.formatCompact(state.pot)}"
        }

        _uiState.update {
            it.copy(
                phase = GamePhase.ROUND_OVER,
                seats = finalSeats,
                sideshowOffer = null,
                statusBanner = header,
                winnerBanner = winnerTitle,
                winningHandLabel = winHandText,
                actionHistory = (listOf("$winnerTitle — $winHandText") + it.actionHistory).take(8)
            )
        }

        viewModelScope.launch {
            repository.recordHandResult(
                newChips = humanSeat.chips,
                won = humanWon,
                potWon = if (humanWon) share else 0L,
                handLabel = humanSeat.evaluation?.label
            )
        }
    }

    private fun findNextActiveSeatIndex(startIndex: Int, seats: List<SeatPlayer>): Int {
        for (offset in 0 until seats.size) {
            val idx = (startIndex + offset) % seats.size
            if (seats[idx].isActiveInRound) return idx
        }
        return startIndex
    }

    private fun findPreviousActiveSeat(fromIndex: Int, seats: List<SeatPlayer>): SeatPlayer? {
        for (offset in 1 until seats.size) {
            val idx = (fromIndex - offset + seats.size) % seats.size
            val candidate = seats[idx]
            if (candidate.isActiveInRound) return candidate
        }
        return null
    }

    override fun onCleared() {
        roundJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val repository: UserPreferencesRepository,
        private val tier: TableTier,
        private val variation: GameVariation
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TableViewModel(repository, tier, variation) as T
        }
    }
}
