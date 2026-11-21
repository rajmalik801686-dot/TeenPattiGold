package com.example.domain

object HandEvaluator {

    private val allDeckCards: List<Card> by lazy {
        Suit.entries.flatMap { suit ->
            Rank.entries.map { rank -> Card(suit, rank) }
        }
    }

    fun createShuffledDeck(): List<Card> = allDeckCards.shuffled()

    fun isWildCard(card: Card, variation: GameVariation, jokerRank: Rank?): Boolean {
        return when (variation) {
            GameVariation.CLASSIC, GameVariation.MUFLIS -> false
            GameVariation.AK47 -> card.rank == Rank.ACE || card.rank == Rank.KING ||
                card.rank == Rank.FOUR || card.rank == Rank.SEVEN
            GameVariation.JOKER -> jokerRank != null && card.rank == jokerRank
        }
    }

    fun evaluate(
        cards: List<Card>,
        variation: GameVariation = GameVariation.CLASSIC,
        jokerRank: Rank? = null
    ): HandEvaluation {
        require(cards.size == 3) { "Teen Patti hand must have exactly 3 cards" }

        val wilds = cards.filter { isWildCard(it, variation, jokerRank) }
        val naturals = cards.filterNot { isWildCard(it, variation, jokerRank) }

        val bestConcrete = when (wilds.size) {
            0 -> evaluateConcrete(cards)
            3 -> evaluateConcrete(
                listOf(
                    Card(Suit.SPADES, Rank.ACE),
                    Card(Suit.HEARTS, Rank.ACE),
                    Card(Suit.DIAMONDS, Rank.ACE)
                )
            )
            2 -> {
                val n = naturals.first()
                val otherSuits = Suit.entries.filter { it != n.suit }
                evaluateConcrete(
                    listOf(
                        n,
                        Card(otherSuits[0], n.rank),
                        Card(otherSuits[1], n.rank)
                    )
                )
            }
            else -> {
                // 1 wild card + 2 naturals: test all 52 card substitutions for optimal hand
                var best: ConcreteEval? = null
                for (sub in allDeckCards) {
                    val candidate = evaluateConcrete(listOf(naturals[0], naturals[1], sub))
                    if (best == null || candidate.score > best.score) {
                        best = candidate
                    }
                }
                best!!
            }
        }

        val effectiveScore = if (variation == GameVariation.MUFLIS) {
            10_000_000 - bestConcrete.score
        } else {
            bestConcrete.score
        }

        val basePercentile = categoryPercentile(bestConcrete.category, bestConcrete.subScore)
        val percentile = if (variation == GameVariation.MUFLIS) {
            (1f - basePercentile).coerceIn(0.05f, 0.99f)
        } else {
            basePercentile
        }

        val label = if (wilds.isNotEmpty()) {
            "${bestConcrete.label} (Wild)"
        } else {
            bestConcrete.label
        }

        return HandEvaluation(
            category = bestConcrete.category,
            standardScore = bestConcrete.score,
            effectiveScore = effectiveScore,
            label = label,
            percentile = percentile
        )
    }

    private data class ConcreteEval(
        val category: HandCategory,
        val score: Int,
        val subScore: Int,
        val label: String
    )

    private fun evaluateConcrete(cards: List<Card>): ConcreteEval {
        val sorted = cards.sortedByDescending { it.rank.value }
        val r0 = sorted[0].rank
        val r1 = sorted[1].rank
        val r2 = sorted[2].rank

        val isFlush = sorted[0].suit == sorted[1].suit && sorted[1].suit == sorted[2].suit
        val seqScore = sequenceScore(r0.value, r1.value, r2.value)

        // 1. Trail / Trio
        if (r0 == r1 && r1 == r2) {
            val sub = r0.value
            return ConcreteEval(
                category = HandCategory.TRAIL,
                score = HandCategory.TRAIL.weight * 1_000_000 + sub,
                subScore = sub,
                label = "Trail of ${pluralRank(r0)}"
            )
        }

        // 2. Pure Sequence
        if (isFlush && seqScore > 0) {
            return ConcreteEval(
                category = HandCategory.PURE_SEQUENCE,
                score = HandCategory.PURE_SEQUENCE.weight * 1_000_000 + seqScore,
                subScore = seqScore,
                label = "Pure Sequence (${sequenceLabel(r0, r1, r2)})"
            )
        }

        // 3. Sequence
        if (seqScore > 0) {
            return ConcreteEval(
                category = HandCategory.SEQUENCE,
                score = HandCategory.SEQUENCE.weight * 1_000_000 + seqScore,
                subScore = seqScore,
                label = "Sequence (${sequenceLabel(r0, r1, r2)})"
            )
        }

        // 4. Color / Flush
        if (isFlush) {
            val sub = r0.value * 400 + r1.value * 20 + r2.value
            return ConcreteEval(
                category = HandCategory.COLOR,
                score = HandCategory.COLOR.weight * 1_000_000 + sub,
                subScore = sub,
                label = "Color (${r0.symbol} High)"
            )
        }

        // 5. Pair
        if (r0 == r1 || r1 == r2 || r0 == r2) {
            val pairRank = when {
                r0 == r1 -> r0
                r1 == r2 -> r1
                else -> r0
            }
            val kicker = when {
                r0 == r1 -> r2
                r1 == r2 -> r0
                else -> r1
            }
            val sub = pairRank.value * 20 + kicker.value
            return ConcreteEval(
                category = HandCategory.PAIR,
                score = HandCategory.PAIR.weight * 1_000_000 + sub,
                subScore = sub,
                label = "Pair of ${pluralRank(pairRank)}"
            )
        }

        // 6. High Card
        val sub = r0.value * 400 + r1.value * 20 + r2.value
        return ConcreteEval(
            category = HandCategory.HIGH_CARD,
            score = HandCategory.HIGH_CARD.weight * 1_000_000 + sub,
            subScore = sub,
            label = "High Card ${r0.displayName}"
        )
    }

    /**
     * Teen Patti sequence ranking:
     * A-K-Q (15) > A-2-3 (14) > K-Q-J (13) > ... > 4-3-2 (4).
     * Note: K-A-2 is NOT a valid sequence in Teen Patti.
     */
    private fun sequenceScore(r0: Int, r1: Int, r2: Int): Int {
        if (r0 == 14 && r1 == 3 && r2 == 2) return 14
        if (r0 == r1 + 1 && r1 == r2 + 1) {
            return if (r0 == 14) 15 else r0
        }
        return 0
    }

    private fun sequenceLabel(r0: Rank, r1: Rank, r2: Rank): String {
        return if (r0 == Rank.ACE && r1 == Rank.THREE && r2 == Rank.TWO) {
            "A-2-3"
        } else {
            "${r0.symbol}-${r1.symbol}-${r2.symbol}"
        }
    }

    private fun pluralRank(rank: Rank): String = when (rank) {
        Rank.SIX -> "6s"
        Rank.ACE -> "Aces"
        Rank.KING -> "Kings"
        Rank.QUEEN -> "Queens"
        Rank.JACK -> "Jacks"
        else -> "${rank.symbol}s"
    }

    private fun categoryPercentile(category: HandCategory, subScore: Int): Float {
        return when (category) {
            HandCategory.TRAIL -> 0.96f + (subScore / 14f) * 0.04f
            HandCategory.PURE_SEQUENCE -> 0.89f + (subScore / 15f) * 0.07f
            HandCategory.SEQUENCE -> 0.78f + (subScore / 15f) * 0.10f
            HandCategory.COLOR -> 0.65f + (subScore / 6000f) * 0.12f
            HandCategory.PAIR -> 0.42f + (subScore / 300f) * 0.22f
            HandCategory.HIGH_CARD -> 0.08f + (subScore / 6000f) * 0.32f
        }
    }
}
