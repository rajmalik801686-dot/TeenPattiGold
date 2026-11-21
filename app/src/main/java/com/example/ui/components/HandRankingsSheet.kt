package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.Card
import com.example.domain.GameVariation
import com.example.domain.HandCategory
import com.example.domain.Rank
import com.example.domain.Suit
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private data class HandExample(
    val category: HandCategory,
    val cards: List<Card>
)

private val handExamples = listOf(
    HandExample(
        HandCategory.TRAIL,
        listOf(Card(Suit.SPADES, Rank.ACE), Card(Suit.HEARTS, Rank.ACE), Card(Suit.DIAMONDS, Rank.ACE))
    ),
    HandExample(
        HandCategory.PURE_SEQUENCE,
        listOf(Card(Suit.HEARTS, Rank.ACE), Card(Suit.HEARTS, Rank.KING), Card(Suit.HEARTS, Rank.QUEEN))
    ),
    HandExample(
        HandCategory.SEQUENCE,
        listOf(Card(Suit.SPADES, Rank.ACE), Card(Suit.DIAMONDS, Rank.TWO), Card(Suit.CLUBS, Rank.THREE))
    ),
    HandExample(
        HandCategory.COLOR,
        listOf(Card(Suit.CLUBS, Rank.KING), Card(Suit.CLUBS, Rank.NINE), Card(Suit.CLUBS, Rank.FOUR))
    ),
    HandExample(
        HandCategory.PAIR,
        listOf(Card(Suit.DIAMONDS, Rank.QUEEN), Card(Suit.SPADES, Rank.QUEEN), Card(Suit.HEARTS, Rank.SEVEN))
    ),
    HandExample(
        HandCategory.HIGH_CARD,
        listOf(Card(Suit.SPADES, Rank.ACE), Card(Suit.HEARTS, Rank.JACK), Card(Suit.CLUBS, Rank.SIX))
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandRankingsSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hand Rankings & Rules",
                    style = MaterialTheme.typography.headlineMedium,
                    color = RoyalGold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close rules",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(handExamples) { example ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianElevated,
                        border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.22f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${7 - example.category.weight}. ${example.category.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = RoyalGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = example.category.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                example.cards.forEach { card ->
                                    PlayingCardView(
                                        card = card,
                                        faceUp = true,
                                        width = 36.dp,
                                        height = 50.dp,
                                        compact = true
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Table Rules & Variations",
                        style = MaterialTheme.typography.titleMedium,
                        color = RoyalGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianElevated,
                        border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "• Blind vs. Seen: Blind players bet 1x stake; Seen players pay 2x the Blind stake.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "• Sideshow: When 3+ players remain, a Seen player can compare cards with the previous Seen player.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "• Show: Available when only 2 players remain to reveal hands and claim the pot.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            GameVariation.entries.forEach { variation ->
                                Text(
                                    text = "• ${variation.title}: ${variation.ruleSummary}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
