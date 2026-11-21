package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.Card
import com.example.ui.theme.CardBackAccent
import com.example.ui.theme.CardBackDeep
import com.example.ui.theme.CardIvory
import com.example.ui.theme.MonoChipFontFamily
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldBright
import com.example.ui.theme.RoyalGoldDeep
import com.example.ui.theme.SuitCrimson
import com.example.ui.theme.SuitInk

@Composable
fun PlayingCardView(
    card: Card?,
    faceUp: Boolean,
    isWild: Boolean = false,
    width: Dp = 62.dp,
    height: Dp = 88.dp,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val flipScale by animateFloatAsState(
        targetValue = if (faceUp) 1f else 0.98f,
        animationSpec = tween(durationMillis = 240),
        label = "card_flip"
    )

    val cornerRadius = if (compact) 6.dp else 10.dp
    Surface(
        modifier = modifier
            .size(width = width, height = height)
            .graphicsLayer {
                scaleX = flipScale
                scaleY = flipScale
            }
            .shadow(elevation = if (compact) 3.dp else 6.dp, shape = RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = if (faceUp && card != null) CardIvory else CardBackDeep,
        border = BorderStroke(
            width = if (isWild && faceUp) 1.8.dp else 1.dp,
            color = if (isWild && faceUp) RoyalGold else Color(0x55D4AF37)
        )
    ) {
        if (faceUp && card != null) {
            val inkColor = if (card.suit.isRed) SuitCrimson else SuitInk
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 3.dp else 5.dp)
            ) {
                // Top-left rank & suit
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = card.rank.symbol,
                        color = inkColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (compact) 10.sp else 15.sp,
                        lineHeight = if (compact) 11.sp else 16.sp
                    )
                    Text(
                        text = card.suit.symbol,
                        color = inkColor,
                        fontSize = if (compact) 9.sp else 13.sp,
                        lineHeight = if (compact) 10.sp else 14.sp
                    )
                }

                // Center suit pip
                Text(
                    text = card.suit.symbol,
                    color = inkColor.copy(alpha = if (compact) 0.9f else 0.85f),
                    fontSize = if (compact) 18.sp else 28.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Wild badge
                if (isWild && !compact) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(4.dp))
                            .background(RoyalGold)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "WILD",
                            color = ObsidianNight,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else if (!compact) {
                    // Bottom-right rank
                    Text(
                        text = card.rank.symbol,
                        color = inkColor.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        } else {
            // Ornate Royal Filigree Card Back
            Canvas(modifier = Modifier.fillMaxSize()) {
                val inset = size.minDimension * 0.12f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF16335E), Color(0xFF0B1C38))
                    )
                )
                drawRect(
                    color = CardBackAccent.copy(alpha = 0.75f),
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - inset * 2,
                        size.height - inset * 2
                    ),
                    style = Stroke(width = if (compact) 1.5f else 2.5f)
                )
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawCircle(
                    color = CardBackAccent.copy(alpha = 0.45f),
                    radius = size.minDimension * 0.22f,
                    center = Offset(cx, cy),
                    style = Stroke(width = if (compact) 1.2f else 2f)
                )
                drawCircle(
                    color = CardBackAccent.copy(alpha = 0.75f),
                    radius = size.minDimension * 0.08f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@Composable
fun FannedHandView(
    cards: List<Card>,
    faceUp: Boolean,
    wildChecker: (Card) -> Boolean = { false },
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardW = if (compact) 34.dp else 62.dp
    val cardH = if (compact) 48.dp else 88.dp
    val stepX = if (compact) 16.dp else 36.dp
    val totalWidth = cardW + stepX * 2

    Box(
        modifier = modifier.width(totalWidth),
        contentAlignment = Alignment.CenterStart
    ) {
        cards.take(3).forEachIndexed { index, card ->
            val rotation = when (index) {
                0 -> -8f
                1 -> 0f
                else -> 8f
            }
            val offsetY = if (index == 1) (-3).dp else 2.dp
            PlayingCardView(
                card = card,
                faceUp = faceUp,
                isWild = wildChecker(card),
                width = cardW,
                height = cardH,
                compact = compact,
                modifier = Modifier
                    .offset(x = stepX * index, y = offsetY)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
fun ChipBadge(
    amountText: String,
    modifier: Modifier = Modifier,
    highlightGold: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color(0xCC090D12),
        border = BorderStroke(
            width = 1.dp,
            color = if (highlightGold) RoyalGold.copy(alpha = 0.65f) else Color(0x44FFFFFF)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(RoyalGoldBright, RoyalGoldDeep)
                        )
                    )
                    .border(1.5.dp, ObsidianNight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ObsidianNight.copy(alpha = 0.65f))
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = amountText,
                fontFamily = MonoChipFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (highlightGold) RoyalGoldBright else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
