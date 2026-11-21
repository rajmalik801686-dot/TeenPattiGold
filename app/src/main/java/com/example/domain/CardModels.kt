package com.example.domain

import kotlinx.serialization.Serializable

enum class Suit(val symbol: String, val label: String, val isRed: Boolean) {
    SPADES("♠", "Spades", false),
    HEARTS("♥", "Hearts", true),
    DIAMONDS("♦", "Diamonds", true),
    CLUBS("♣", "Clubs", false)
}

enum class Rank(val value: Int, val symbol: String, val displayName: String) {
    TWO(2, "2", "2"),
    THREE(3, "3", "3"),
    FOUR(4, "4", "4"),
    FIVE(5, "5", "5"),
    SIX(6, "6", "6"),
    SEVEN(7, "7", "7"),
    EIGHT(8, "8", "8"),
    NINE(9, "9", "9"),
    TEN(10, "10", "10"),
    JACK(11, "J", "Jack"),
    QUEEN(12, "Q", "Queen"),
    KING(13, "K", "King"),
    ACE(14, "A", "Ace")
}

data class Card(
    val suit: Suit,
    val rank: Rank
) {
    val shortLabel: String get() = "${rank.symbol}${suit.symbol}"
}

@Serializable
enum class GameVariation(
    val title: String,
    val badge: String,
    val ruleSummary: String
) {
    CLASSIC(
        title = "Classic",
        badge = "STANDARD",
        ruleSummary = "Traditional 3-card Teen Patti. Trail of Aces is supreme."
    ),
    MUFLIS(
        title = "Muflis",
        badge = "LOWBALL",
        ruleSummary = "Rankings reversed! Lowest hand (2-3-5 offsuit) wins the pot."
    ),
    AK47(
        title = "AK47",
        badge = "4 WILDS",
        ruleSummary = "All Aces, Kings, 4s & 7s act as wild Jokers to form top hands."
    ),
    JOKER(
        title = "Joker",
        badge = "1 WILD",
        ruleSummary = "One random card rank drawn each round is wild for all players."
    )
}

enum class HandCategory(
    val weight: Int,
    val title: String,
    val description: String
) {
    TRAIL(6, "Trail (Trio)", "Three cards of the same rank. A-A-A is highest."),
    PURE_SEQUENCE(5, "Pure Sequence", "Three consecutive cards of the same suit."),
    SEQUENCE(4, "Sequence (Run)", "Three consecutive cards of mixed suits."),
    COLOR(3, "Color (Flush)", "Three cards of the same suit, non-consecutive."),
    PAIR(2, "Pair", "Two cards of the same rank plus one kicker."),
    HIGH_CARD(1, "High Card", "Highest card determines strength when no set forms.")
}

data class HandEvaluation(
    val category: HandCategory,
    val standardScore: Int,
    val effectiveScore: Int,
    val label: String,
    val percentile: Float
)
