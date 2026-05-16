package com.example.solitarefore_ink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solitarefore_ink.ui.theme.PureBlack
import com.example.solitarefore_ink.ui.theme.PureWhite

@Composable
fun SolitaireScreen(viewModel: SolitaireViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .padding(4.dp)
    ) {
        // Calculate maximum card size to fit 7 columns on the screen.
        // Screen width - padding (8.dp total) - 6 spaces between columns (4.dp each = 24.dp)
        val availableWidth = maxWidth - 32.dp
        val cardWidth = availableWidth / 7
        val cardHeight = cardWidth * 1.45f
        val verticalOffset = cardHeight * 0.35f
        
        // Font sizes scale proportionally to card width
        val smallFontSize = (cardWidth.value * 0.42f).sp
        val largeFontSize = (cardWidth.value * 0.55f).sp

        Column(modifier = Modifier.fillMaxSize()) {
            if (state.isGameWon) {
                Text(
                    text = "YOU WIN!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                )
            }

            // Top Row: Stock, Waste, Empty space, Foundation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stock & Waste
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Stock
                    if (state.stock.isNotEmpty()) {
                        CardBack(cardWidth, cardHeight, modifier = Modifier.clickable { viewModel.drawCard() })
                    } else {
                        EmptyCardSlot(cardWidth, cardHeight, modifier = Modifier.clickable { viewModel.drawCard() })
                    }

                    // Waste
                    if (state.waste.isNotEmpty()) {
                        CardItem(
                            card = state.waste.last(),
                            cardWidth = cardWidth,
                            cardHeight = cardHeight,
                            smallFontSize = smallFontSize,
                            largeFontSize = largeFontSize,
                            modifier = Modifier.clickable { viewModel.autoMoveCard(state.waste.last(), PileType.WASTE) }
                        )
                    } else {
                        EmptyCardSlot(cardWidth, cardHeight)
                    }
                }

                // Foundation
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0 until 4) {
                        val pile = state.foundation[i]
                        if (pile.isNotEmpty()) {
                            CardItem(
                                card = pile.last(),
                                cardWidth = cardWidth,
                                cardHeight = cardHeight,
                                smallFontSize = smallFontSize,
                                largeFontSize = largeFontSize,
                                modifier = Modifier.clickable { viewModel.autoMoveCard(pile.last(), PileType.FOUNDATION, i) }
                            )
                        } else {
                            EmptyCardSlot(cardWidth, cardHeight)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tableau
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until 7) {
                    val pile = state.tableau[i]
                    Box(modifier = Modifier.width(cardWidth).fillMaxHeight()) {
                        if (pile.isEmpty()) {
                            EmptyCardSlot(cardWidth, cardHeight)
                        } else {
                            pile.forEachIndexed { index, card ->
                                val modifier = Modifier
                                    .offset(y = verticalOffset * index)
                                    .clickable {
                                        if (card.isFaceUp) {
                                            viewModel.autoMoveCard(card, PileType.TABLEAU, i)
                                        }
                                    }
                                if (card.isFaceUp) {
                                    CardItem(card, cardWidth, cardHeight, smallFontSize, largeFontSize, modifier)
                                } else {
                                    CardBack(cardWidth, cardHeight, modifier)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardItem(
    card: Card,
    cardWidth: Dp,
    cardHeight: Dp,
    smallFontSize: TextUnit,
    largeFontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .background(PureWhite)
            .border(1.dp, PureBlack)
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = card.rank.symbol,
                color = PureBlack,
                fontSize = smallFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                lineHeight = smallFontSize
            )
            Text(
                text = card.suit.symbol,
                color = PureBlack,
                fontSize = smallFontSize,
                fontFamily = FontFamily.Serif,
                lineHeight = smallFontSize
            )
        }
        Text(
            text = card.suit.symbol,
            color = PureBlack,
            fontSize = largeFontSize,
            fontFamily = FontFamily.Serif,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = cardHeight * 0.15f)
        )
    }
}

@Composable
fun CardBack(
    cardWidth: Dp,
    cardHeight: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .background(PureWhite)
            .border(1.dp, PureBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.dp.toPx()
            for (i in 0..size.width.toInt() step 12) {
                drawLine(
                    color = PureBlack,
                    start = Offset(i.toFloat(), 0f),
                    end = Offset(0f, i.toFloat()),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = PureBlack,
                    start = Offset(i.toFloat(), size.height),
                    end = Offset(size.width, size.height - size.width + i.toFloat()),
                    strokeWidth = strokeWidth
                )
            }
        }
        Text(
            text = "X",
            color = PureBlack,
            fontSize = (cardWidth.value * 0.85f).sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun EmptyCardSlot(
    cardWidth: Dp,
    cardHeight: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .background(PureWhite)
            .border(1.dp, PureBlack)
    )
}