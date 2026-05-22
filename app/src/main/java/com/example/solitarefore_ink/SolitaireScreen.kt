package com.example.solitarefore_ink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solitarefore_ink.ui.theme.PureBlack
import com.example.solitarefore_ink.ui.theme.PureWhite

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    )
}

@Composable
fun SolitaireScreen(viewModel: SolitaireViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showVictoryDialog by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current

    LaunchedEffect(state.isGameWon) {
        if (state.isGameWon) {
            showVictoryDialog = true
        }
    }

    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LandscapeSolitaireLayout(state, viewModel, { showSettingsDialog = true })
    } else {
        PortraitSolitaireLayout(state, viewModel, { showSettingsDialog = true })
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentDrawMode = state.drawMode,
            currentBackStyle = state.cardBackStyle,
            currentAutoPlace = state.autoPlace,
            currentAutoComplete = state.autoComplete,
            onDismiss = { showSettingsDialog = false },
            onDrawModeChanged = { viewModel.updateDrawMode(it) },
            onBackStyleChanged = { viewModel.updateCardBackStyle(it) },
            onAutoPlaceChanged = { viewModel.updateAutoPlace(it) },
            onAutoCompleteChanged = { viewModel.updateAutoComplete(it) }
        )
    }

    if (showVictoryDialog) {
        VictoryDialog(
            score = state.score,
            elapsedSeconds = state.elapsedSeconds,
            onNewGame = {
                showVictoryDialog = false
                viewModel.startNewGame()
            },
            onDismiss = { showVictoryDialog = false }
        )
    }
}

@Composable
fun PortraitSolitaireLayout(state: GameState, viewModel: SolitaireViewModel, onShowSettings: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .padding(4.dp)
    ) {
        val availableWidth = maxWidth - 32.dp
        val cardWidth = availableWidth / 7
        val cardHeight = cardWidth * 1.45f
        val verticalOffset = cardHeight * 0.35f
        
        val smallFontSize = (cardWidth.value * 0.42f).sp
        val largeFontSize = (cardWidth.value * 0.55f).sp

        Column(modifier = Modifier.fillMaxSize()) {
            
            // --- TOP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TopBarButton(
                    icon = Icons.Outlined.Refresh,
                    text = "新游戏",
                    onClick = { viewModel.startNewGame() }
                )
                TopBarButton(
                    icon = Icons.AutoMirrored.Outlined.Undo,
                    text = "撤销",
                    onClick = { viewModel.undo() }
                )
                TopBarButton(
                    icon = Icons.Outlined.Lightbulb,
                    text = "提示",
                    onClick = { viewModel.requestHint() }
                )
                TopBarButton(
                    icon = Icons.Outlined.Settings,
                    text = "设置",
                    onClick = { onShowSettings() }
                )
            }

            if (state.isGameWon) {
                Text(
                    text = "YOU WIN!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack,
                    fontFamily = FontFamily.Serif,
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
                        CardBack(cardWidth, cardHeight, state.cardBackStyle, modifier = Modifier.noRippleClickable { viewModel.drawCard() })
                    } else {
                        EmptyCardSlot(cardWidth, cardHeight, modifier = Modifier.noRippleClickable { viewModel.drawCard() })
                    }

                    // Waste (Show up to 3 cards slightly offset horizontally)
                    if (state.waste.isNotEmpty()) {
                        Box(modifier = Modifier.width(cardWidth * 1.5f)) { // Extra width to fit overlapped cards
                            val cardsToShow = if (state.drawMode == 3) {
                                // In draw 3 mode, show up to the top 3 cards in the waste pile
                                state.waste.takeLast(3)
                            } else {
                                // In draw 1 mode, just show the top card
                                listOf(state.waste.last())
                            }

                            cardsToShow.forEachIndexed { index, card ->
                                val isTopCard = index == cardsToShow.size - 1
                                val modifier = Modifier
                                    .offset(x = (index * (cardWidth.value * 0.25f)).dp) // Offset horizontally
                                    .then(
                                        if (isTopCard) {
                                            Modifier.noRippleClickable { viewModel.handleCardClick(card, PileType.WASTE) }
                                        } else {
                                            Modifier
                                        }
                                    )
                                
                                val isSelected = state.isCardSelected(card, PileType.WASTE, -1)
                                val isHighlighted = (isTopCard && card.id == state.hintedCardId) || isSelected

                                CardItem(
                                    card = card,
                                    isHinted = isHighlighted,
                                    cardWidth = cardWidth,
                                    cardHeight = cardHeight,
                                    smallFontSize = smallFontSize,
                                    largeFontSize = largeFontSize,
                                    modifier = modifier
                                )
                            }
                        }
                    } else {
                        // Empty box with the same width to maintain layout spacing
                        Box(modifier = Modifier.width(cardWidth * 1.5f)) {
                            EmptyCardSlot(cardWidth, cardHeight)
                        }
                    }
                }

                // Foundation
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0 until 4) {
                        val pile = state.foundation[i]
                        if (pile.isNotEmpty()) {
                            val topCard = pile.last()
                            val isSelected = state.isCardSelected(topCard, PileType.FOUNDATION, i)
                            val isHighlighted = topCard.id == state.hintedCardId || isSelected
                            CardItem(
                                card = topCard,
                                isHinted = isHighlighted,
                                cardWidth = cardWidth,
                                cardHeight = cardHeight,
                                smallFontSize = smallFontSize,
                                largeFontSize = largeFontSize,
                                modifier = Modifier.noRippleClickable { viewModel.handleCardClick(topCard, PileType.FOUNDATION, i) }
                            )
                        } else {
                            EmptyCardSlot(
                                cardWidth = cardWidth,
                                cardHeight = cardHeight,
                                modifier = Modifier.noRippleClickable { viewModel.handleEmptySlotClick(PileType.FOUNDATION, i) }
                            )
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
                            EmptyCardSlot(
                                cardWidth = cardWidth,
                                cardHeight = cardHeight,
                                modifier = Modifier.noRippleClickable { viewModel.handleEmptySlotClick(PileType.TABLEAU, i) }
                            )
                        } else {
                            pile.forEachIndexed { index, card ->
                                val modifier = Modifier
                                    .offset(y = verticalOffset * index)
                                    .noRippleClickable {
                                        if (card.isFaceUp) {
                                            viewModel.handleCardClick(card, PileType.TABLEAU, i)
                                        }
                                    }
                                if (card.isFaceUp) {
                                    val isSelected = state.isCardSelected(card, PileType.TABLEAU, i)
                                    val isHighlighted = card.id == state.hintedCardId || isSelected
                                    CardItem(
                                        card = card,
                                        isHinted = isHighlighted,
                                        cardWidth = cardWidth, 
                                        cardHeight = cardHeight, 
                                        smallFontSize = smallFontSize, 
                                        largeFontSize = largeFontSize, 
                                        modifier = modifier
                                    )
                                } else {
                                    CardBack(cardWidth, cardHeight, state.cardBackStyle, modifier)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Info Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val minutes = state.elapsedSeconds / 60
                val seconds = state.elapsedSeconds % 60
                val timeString = java.lang.String.format(java.util.Locale.getDefault(), "耗时: %02d:%02d", minutes, seconds)
                Text(
                    text = timeString,
                    color = PureBlack,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "分数: ${state.score}",
                    color = PureBlack,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LandscapeSolitaireLayout(state: GameState, viewModel: SolitaireViewModel, onShowSettings: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .padding(8.dp)
    ) {
        val availableWidth = maxWidth - 16.dp
        val availableHeight = maxHeight - 16.dp
        
        // Left: 1.8w, Center: 7.2w, Right: 1.2w. Spacers: 2 * 0.2w. Total = 10.6w
        val maxWidthCard = availableWidth / 10.6f
        // Tableau max depth ~ 1 face + 12 offset = 1 + 12*0.28 = 4.36
        val maxHeightCard = availableHeight / 4.4f / 1.45f 
        
        val cardWidth = minOf(maxWidthCard, maxHeightCard)
        val cardHeight = cardWidth * 1.45f
        val verticalOffset = cardHeight * 0.28f // Slightly tighter vertical spacing for landscape
        
        val smallFontSize = (cardWidth.value * 0.42f).sp
        val largeFontSize = (cardWidth.value * 0.55f).sp

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center
        ) {
            // Left Column (Controls, Stock, Waste, Info)
            Column(
                modifier = Modifier.width(cardWidth * 1.8f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TopBarButton(Icons.AutoMirrored.Outlined.Undo, "撤销") { viewModel.undo() }
                    TopBarButton(Icons.Outlined.Lightbulb, "提示") { viewModel.requestHint() }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TopBarButton(Icons.Outlined.Refresh, "新游戏") { viewModel.startNewGame() }
                    TopBarButton(Icons.Outlined.Settings, "设置") { onShowSettings() }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stock
                if (state.stock.isNotEmpty()) {
                    CardBack(cardWidth, cardHeight, state.cardBackStyle, modifier = Modifier.noRippleClickable { viewModel.drawCard() })
                } else {
                    EmptyCardSlot(cardWidth, cardHeight, modifier = Modifier.noRippleClickable { viewModel.drawCard() })
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Waste
                if (state.waste.isNotEmpty()) {
                    Box(modifier = Modifier.width(cardWidth * 1.5f).height(cardHeight)) {
                        val cardsToShow = if (state.drawMode == 3) state.waste.takeLast(3) else listOf(state.waste.last())
                        cardsToShow.forEachIndexed { index, card ->
                            val isTopCard = index == cardsToShow.size - 1
                            val modifier = Modifier
                                .offset(x = (index * (cardWidth.value * 0.25f)).dp)
                                .then(if (isTopCard) Modifier.noRippleClickable { viewModel.handleCardClick(card, PileType.WASTE) } else Modifier)
                            val isSelected = state.isCardSelected(card, PileType.WASTE, -1)
                            val isHighlighted = (isTopCard && card.id == state.hintedCardId) || isSelected
                            CardItem(card, isHighlighted, cardWidth, cardHeight, smallFontSize, largeFontSize, modifier)
                        }
                    }
                } else {
                    Box(modifier = Modifier.width(cardWidth * 1.5f).height(cardHeight)) {
                        EmptyCardSlot(cardWidth, cardHeight)
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                val timeString = java.lang.String.format(java.util.Locale.getDefault(), "耗时:\n%02d:%02d", state.elapsedSeconds / 60, state.elapsedSeconds % 60)
                Text(timeString, color = PureBlack, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("分数:\n${state.score}", color = PureBlack, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (state.isGameWon) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "YOU WIN!",
                        color = PureBlack,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            Spacer(modifier = Modifier.width(cardWidth * 0.2f))

            // Middle Column (Tableau)
            Row(
                modifier = Modifier.width(cardWidth * 7.2f).fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until 7) {
                    val pile = state.tableau[i]
                    Box(modifier = Modifier.width(cardWidth).fillMaxHeight()) {
                        if (pile.isEmpty()) {
                            EmptyCardSlot(
                                cardWidth = cardWidth,
                                cardHeight = cardHeight,
                                modifier = Modifier.noRippleClickable { viewModel.handleEmptySlotClick(PileType.TABLEAU, i) }
                            )
                        } else {
                            pile.forEachIndexed { index, card ->
                                val isSelected = state.isCardSelected(card, PileType.TABLEAU, i)
                                val isHighlighted = card.id == state.hintedCardId || isSelected
                                val mod = Modifier.offset(y = verticalOffset * index).noRippleClickable { if (card.isFaceUp) viewModel.handleCardClick(card, PileType.TABLEAU, i) }
                                if (card.isFaceUp) CardItem(card, isHighlighted, cardWidth, cardHeight, smallFontSize, largeFontSize, mod)
                                else CardBack(cardWidth, cardHeight, state.cardBackStyle, mod)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(cardWidth * 0.2f))

            // Right Column (Foundation)
            Column(
                modifier = Modifier.width(cardWidth * 1.2f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (i in 0 until 4) {
                    val pile = state.foundation[i]
                    if (pile.isNotEmpty()) {
                        val topCard = pile.last()
                        val isSelected = state.isCardSelected(topCard, PileType.FOUNDATION, i)
                        val isHighlighted = topCard.id == state.hintedCardId || isSelected
                        CardItem(topCard, isHighlighted, cardWidth, cardHeight, smallFontSize, largeFontSize, Modifier.noRippleClickable { viewModel.handleCardClick(topCard, PileType.FOUNDATION, i) })
                    } else {
                        EmptyCardSlot(
                            cardWidth = cardWidth,
                            cardHeight = cardHeight,
                            modifier = Modifier.noRippleClickable { viewModel.handleEmptySlotClick(PileType.FOUNDATION, i) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopBarButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .noRippleClickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = PureBlack,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            color = PureBlack,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsDialog(
    currentDrawMode: Int,
    currentBackStyle: CardBackStyle,
    currentAutoPlace: Boolean,
    currentAutoComplete: Boolean,
    onDismiss: () -> Unit,
    onDrawModeChanged: (Int) -> Unit,
    onBackStyleChanged: (CardBackStyle) -> Unit,
    onAutoPlaceChanged: (Boolean) -> Unit,
    onAutoCompleteChanged: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        titleContentColor = PureBlack,
        textContentColor = PureBlack,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(2.dp, PureBlack, RoundedCornerShape(8.dp)),
        title = {
            Text(text = "设置", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(text = "发牌模式", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = currentDrawMode == 1,
                        onClick = { onDrawModeChanged(1) },
                        colors = RadioButtonDefaults.colors(selectedColor = PureBlack, unselectedColor = PureBlack)
                    )
                    Text("每次 1 张")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = currentDrawMode == 3,
                        onClick = { onDrawModeChanged(3) },
                        colors = RadioButtonDefaults.colors(selectedColor = PureBlack, unselectedColor = PureBlack)
                    )
                    Text("每次 3 张")
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "放牌方式", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = currentAutoPlace,
                        onClick = { onAutoPlaceChanged(true) },
                        colors = RadioButtonDefaults.colors(selectedColor = PureBlack, unselectedColor = PureBlack)
                    )
                    Text("自动放牌")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = !currentAutoPlace,
                        onClick = { onAutoPlaceChanged(false) },
                        colors = RadioButtonDefaults.colors(selectedColor = PureBlack, unselectedColor = PureBlack)
                    )
                    Text("手动放牌")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "自动整理", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = currentAutoComplete,
                        onClick = { onAutoCompleteChanged(true) },
                        colors = RadioButtonDefaults.colors(selectedColor = PureBlack, unselectedColor = PureBlack)
                    )
                    Text("开启")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = !currentAutoComplete,
                        onClick = { onAutoCompleteChanged(false) },
                        colors = RadioButtonDefaults.colors(selectedColor = PureBlack, unselectedColor = PureBlack)
                    )
                    Text("关闭")
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "牌背纹理", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                
                // 4x2 grid of card backs
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val styles = CardBackStyle.values()
                    for (row in 0 until 2) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (col in 0 until 4) {
                                val style = styles[row * 4 + col]
                                val isSelected = style == currentBackStyle
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = PureBlack,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .noRippleClickable { onBackStyleChanged(style) }
                                ) {
                                    CardBack(cardWidth = 40.dp, cardHeight = 58.dp, style = style)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定", color = PureBlack, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun VictoryDialog(
    score: Int,
    elapsedSeconds: Int,
    onNewGame: () -> Unit,
    onDismiss: () -> Unit
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeString = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        titleContentColor = PureBlack,
        textContentColor = PureBlack,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(2.dp, PureBlack, RoundedCornerShape(8.dp)),
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "YOU WIN!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "恭喜你赢得了本局游戏！",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "游戏耗时", fontSize = 12.sp, color = Color.Gray)
                        Text(text = timeString, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "最终分数", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "$score", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onNewGame,
                colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = PureWhite),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("新游戏", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, PureBlack),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureBlack),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("查看牌局", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun CardItem(
    card: Card,
    isHinted: Boolean,
    cardWidth: Dp,
    cardHeight: Dp,
    smallFontSize: TextUnit,
    largeFontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val bgColor = PureWhite
    val fgColor = PureBlack

    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(if (isHinted) 3.dp else 1.dp, PureBlack, RoundedCornerShape(4.dp))
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = card.rank.symbol,
                color = fgColor,
                fontSize = smallFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                lineHeight = smallFontSize
            )
            Text(
                text = card.suit.symbol,
                color = fgColor,
                fontSize = smallFontSize,
                fontFamily = FontFamily.Serif,
                lineHeight = smallFontSize
            )
        }
        Text(
            text = card.suit.symbol,
            color = fgColor,
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
    style: CardBackStyle = CardBackStyle.CROSSHATCH,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(PureWhite)
            .border(1.dp, PureBlack, RoundedCornerShape(4.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            val strokeWidth = 1.dp.toPx()
            val w = size.width
            val h = size.height
            val spacing = 8.dp.toPx()
            
            when (style) {
                CardBackStyle.CROSSHATCH -> {
                    for (i in 0..w.toInt() step 12) {
                        drawLine(PureBlack, Offset(i.toFloat(), 0f), Offset(0f, i.toFloat()), strokeWidth)
                        drawLine(PureBlack, Offset(i.toFloat(), h), Offset(w, h - w + i.toFloat()), strokeWidth)
                    }
                }
                CardBackStyle.HORIZONTAL_STRIPES -> {
                    for (y in 0..h.toInt() step spacing.toInt()) {
                        drawLine(PureBlack, Offset(0f, y.toFloat()), Offset(w, y.toFloat()), strokeWidth * 2)
                    }
                }
                CardBackStyle.VERTICAL_STRIPES -> {
                    for (x in 0..w.toInt() step spacing.toInt()) {
                        drawLine(PureBlack, Offset(x.toFloat(), 0f), Offset(x.toFloat(), h), strokeWidth * 2)
                    }
                }
                CardBackStyle.CHECKERBOARD -> {
                    val squareSize = spacing
                    for (x in 0..w.toInt() step squareSize.toInt()) {
                        for (y in 0..h.toInt() step squareSize.toInt()) {
                            if ((x / squareSize.toInt() + y / squareSize.toInt()) % 2 == 0) {
                                drawRect(PureBlack, Offset(x.toFloat(), y.toFloat()), Size(squareSize, squareSize))
                            }
                        }
                    }
                }
                CardBackStyle.CONCENTRIC_RECTANGLES -> {
                    var inset = spacing / 2
                    while (inset < w / 2 && inset < h / 2) {
                        drawRect(
                            color = PureBlack,
                            topLeft = Offset(inset, inset),
                            size = Size(w - inset * 2, h - inset * 2),
                            style = Stroke(strokeWidth * 2)
                        )
                        inset += spacing
                    }
                }
                CardBackStyle.DIAGONAL_TL_BR -> {
                    for (i in -h.toInt()..w.toInt() step spacing.toInt()) {
                        drawLine(PureBlack, Offset(i.toFloat(), 0f), Offset(i.toFloat() + h, h), strokeWidth * 2)
                    }
                }
                CardBackStyle.DIAGONAL_BL_TR -> {
                    for (i in 0..(w + h).toInt() step spacing.toInt()) {
                        drawLine(PureBlack, Offset(i.toFloat(), h), Offset(i.toFloat() - h, 0f), strokeWidth * 2)
                    }
                }
                CardBackStyle.DOTS -> {
                    val radius = strokeWidth * 2
                    for (x in (spacing/2).toInt()..w.toInt() step spacing.toInt()) {
                        for (y in (spacing/2).toInt()..h.toInt() step spacing.toInt()) {
                            drawCircle(PureBlack, radius, Offset(x.toFloat(), y.toFloat()))
                        }
                    }
                }
            }
        }
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
            .clip(RoundedCornerShape(4.dp))
            .background(PureWhite)
            .border(1.dp, PureBlack, RoundedCornerShape(4.dp))
    )
}