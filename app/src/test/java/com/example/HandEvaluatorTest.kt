package com.example

import com.example.domain.Card
import com.example.domain.GameVariation
import com.example.domain.HandCategory
import com.example.domain.HandEvaluator
import com.example.domain.Rank
import com.example.domain.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandEvaluatorTest {

    @Test
    fun classicHandRankingHierarchy() {
        val trail = HandEvaluator.evaluate(
            listOf(Card(Suit.SPADES, Rank.TWO), Card(Suit.HEARTS, Rank.TWO), Card(Suit.CLUBS, Rank.TWO))
        )
        val pureSeqAKQ = HandEvaluator.evaluate(
            listOf(Card(Suit.SPADES, Rank.ACE), Card(Suit.SPADES, Rank.KING), Card(Suit.SPADES, Rank.QUEEN))
        )
        val pureSeqA23 = HandEvaluator.evaluate(
            listOf(Card(Suit.HEARTS, Rank.ACE), Card(Suit.HEARTS, Rank.TWO), Card(Suit.HEARTS, Rank.THREE))
        )
        val pureSeqKQJ = HandEvaluator.evaluate(
            listOf(Card(Suit.DIAMONDS, Rank.KING), Card(Suit.DIAMONDS, Rank.QUEEN), Card(Suit.DIAMONDS, Rank.JACK))
        )
        val seq = HandEvaluator.evaluate(
            listOf(Card(Suit.SPADES, Rank.FIVE), Card(Suit.HEARTS, Rank.FOUR), Card(Suit.CLUBS, Rank.THREE))
        )
        val color = HandEvaluator.evaluate(
            listOf(Card(Suit.CLUBS, Rank.ACE), Card(Suit.CLUBS, Rank.TEN), Card(Suit.CLUBS, Rank.FOUR))
        )
        val pair = HandEvaluator.evaluate(
            listOf(Card(Suit.SPADES, Rank.KING), Card(Suit.DIAMONDS, Rank.KING), Card(Suit.CLUBS, Rank.TWO))
        )
        val highCard = HandEvaluator.evaluate(
            listOf(Card(Suit.SPADES, Rank.ACE), Card(Suit.HEARTS, Rank.QUEEN), Card(Suit.CLUBS, Rank.NINE))
        )

        assertEquals(HandCategory.TRAIL, trail.category)
        assertEquals(HandCategory.PURE_SEQUENCE, pureSeqAKQ.category)
        assertEquals(HandCategory.PURE_SEQUENCE, pureSeqA23.category)
        assertEquals(HandCategory.SEQUENCE, seq.category)
        assertEquals(HandCategory.COLOR, color.category)
        assertEquals(HandCategory.PAIR, pair.category)
        assertEquals(HandCategory.HIGH_CARD, highCard.category)

        assertTrue(trail.effectiveScore > pureSeqAKQ.effectiveScore)
        assertTrue(pureSeqAKQ.effectiveScore > pureSeqA23.effectiveScore)
        assertTrue(pureSeqA23.effectiveScore > pureSeqKQJ.effectiveScore)
        assertTrue(pureSeqKQJ.effectiveScore > seq.effectiveScore)
        assertTrue(seq.effectiveScore > color.effectiveScore)
        assertTrue(color.effectiveScore > pair.effectiveScore)
        assertTrue(pair.effectiveScore > highCard.effectiveScore)
    }

    @Test
    fun muflisLowballInvertsRankings() {
        val lowHand = HandEvaluator.evaluate(
            listOf(Card(Suit.SPADES, Rank.FIVE), Card(Suit.HEARTS, Rank.THREE), Card(Suit.CLUBS, Rank.TWO)),
            variation = GameVariation.MUFLIS
        )
        val trailOfAces = HandEvaluator.evaluate(
            listOf(Card(Suit.SPADES, Rank.ACE), Card(Suit.HEARTS, Rank.ACE), Card(Suit.CLUBS, Rank.ACE)),
            variation = GameVariation.MUFLIS
        )
        assertTrue(lowHand.effectiveScore > trailOfAces.effectiveScore)
    }

    @Test
    fun ak47WildCardsElevateHand() {
        val hand = HandEvaluator.evaluate(
            listOf(Card(Suit.SPADES, Rank.FOUR), Card(Suit.HEARTS, Rank.QUEEN), Card(Suit.DIAMONDS, Rank.QUEEN)),
            variation = GameVariation.AK47
        )
        assertEquals(HandCategory.TRAIL, hand.category)
    }

    @Test
    fun jokerModeWildCardCompletesPureSequence() {
        val hand = HandEvaluator.evaluate(
            listOf(Card(Suit.HEARTS, Rank.ACE), Card(Suit.HEARTS, Rank.KING), Card(Suit.CLUBS, Rank.NINE)),
            variation = GameVariation.JOKER,
            jokerRank = Rank.NINE
        )
        assertEquals(HandCategory.PURE_SEQUENCE, hand.category)
    }
}
