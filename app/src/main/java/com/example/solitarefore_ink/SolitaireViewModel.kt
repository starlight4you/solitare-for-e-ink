package com.example.solitarefore_ink

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SolitaireViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    init {
        startNewGame()
    }

    fun startNewGame() {
        val deck = DeckManager.createDeck().toMutableList()
        val newTableau = List(7) { mutableListOf<Card>() }
        
        // Deal cards to tableau
        for (i in 0 until 7) {
            for (j in i until 7) {
                val card = deck.removeAt(deck.size - 1)
                if (i == j) {
                    card.isFaceUp = true
                }
                newTableau[j].add(card)
            }
        }

        _state.value = GameState(
            stock = deck,
            waste = emptyList(),
            foundation = List(4) { emptyList() },
            tableau = newTableau,
            isGameWon = false
        )
    }

    fun drawCard() {
        _state.update { currentState ->
            if (currentState.stock.isNotEmpty()) {
                val cardToDraw = currentState.stock.last()
                val newStock = currentState.stock.dropLast(1)
                val newWaste = currentState.waste + cardToDraw.apply { isFaceUp = true }
                currentState.copy(stock = newStock, waste = newWaste)
            } else if (currentState.waste.isNotEmpty()) {
                // Recycle waste to stock
                val newStock = currentState.waste.reversed().map { it.apply { isFaceUp = false } }
                currentState.copy(stock = newStock, waste = emptyList())
            } else {
                currentState
            }
        }
    }

    fun canMoveToFoundation(card: Card, foundationPile: List<Card>): Boolean {
        if (foundationPile.isEmpty()) {
            return card.rank == Rank.ACE
        }
        val topCard = foundationPile.last()
        return topCard.suit == card.suit && topCard.rank.value + 1 == card.rank.value
    }

    fun canMoveToTableau(card: Card, tableauPile: List<Card>): Boolean {
        if (tableauPile.isEmpty()) {
            return card.rank == Rank.KING
        }
        val topCard = tableauPile.last()
        return topCard.isFaceUp && topCard.suit.isRed != card.suit.isRed && topCard.rank.value - 1 == card.rank.value
    }

    private fun checkWinCondition(foundation: List<List<Card>>): Boolean {
        return foundation.all { it.size == 13 }
    }
    
    // UI Interaction - click to move (auto-move to foundation or best tableau)
    fun autoMoveCard(card: Card, sourcePileType: PileType, sourcePileIndex: Int = -1) {
        _state.update { currentState ->
            // Try foundation first
            for (i in 0 until 4) {
                if (canMoveToFoundation(card, currentState.foundation[i])) {
                    return@update moveCardImplementation(currentState, sourcePileType, sourcePileIndex, PileType.FOUNDATION, i, 1)
                }
            }

            // If not foundation, try tableau
            // Note: If moving from tableau, we might be moving a stack. Auto-move only moves the selected card and anything on top of it.
            val cardsToMove = getCardsToMove(currentState, sourcePileType, sourcePileIndex, card)
            if (cardsToMove.isEmpty()) return@update currentState

            val bottomCardOfStack = cardsToMove.first()
            
            for (i in 0 until 7) {
                if (sourcePileType == PileType.TABLEAU && i == sourcePileIndex) continue
                if (canMoveToTableau(bottomCardOfStack, currentState.tableau[i])) {
                     return@update moveCardImplementation(currentState, sourcePileType, sourcePileIndex, PileType.TABLEAU, i, cardsToMove.size)
                }
            }
            currentState
        }
    }
    
    private fun getCardsToMove(state: GameState, sourcePileType: PileType, sourcePileIndex: Int, targetCard: Card): List<Card> {
        return when (sourcePileType) {
            PileType.WASTE -> if (state.waste.isNotEmpty() && state.waste.last() == targetCard) listOf(targetCard) else emptyList()
            PileType.FOUNDATION -> if (state.foundation[sourcePileIndex].isNotEmpty() && state.foundation[sourcePileIndex].last() == targetCard) listOf(targetCard) else emptyList()
            PileType.TABLEAU -> {
                val pile = state.tableau[sourcePileIndex]
                val index = pile.indexOf(targetCard)
                if (index != -1 && targetCard.isFaceUp) {
                    pile.subList(index, pile.size)
                } else {
                    emptyList()
                }
            }
            PileType.STOCK -> emptyList()
        }
    }

    private fun moveCardImplementation(
        state: GameState,
        sourceType: PileType,
        sourceIndex: Int,
        destType: PileType,
        destIndex: Int,
        count: Int
    ): GameState {
        val newWaste = state.waste.toMutableList()
        val newFoundation = state.foundation.map { it.toMutableList() }.toMutableList()
        val newTableau = state.tableau.map { it.toMutableList() }.toMutableList()

        val cardsToMove = mutableListOf<Card>()

        // Remove from source
        when (sourceType) {
            PileType.WASTE -> {
                for(i in 0 until count) {
                     cardsToMove.add(0, newWaste.removeAt(newWaste.size - 1))
                }
            }
            PileType.FOUNDATION -> {
                 for(i in 0 until count) {
                     cardsToMove.add(0, newFoundation[sourceIndex].removeAt(newFoundation[sourceIndex].size - 1))
                }
            }
            PileType.TABLEAU -> {
                val pile = newTableau[sourceIndex]
                 for(i in 0 until count) {
                     cardsToMove.add(0, pile.removeAt(pile.size - 1))
                }
                // Flip the new top card if needed
                if (pile.isNotEmpty() && !pile.last().isFaceUp) {
                    pile.last().isFaceUp = true
                }
            }
            PileType.STOCK -> return state // shouldn't happen directly
        }

        // Add to destination
        when (destType) {
            PileType.FOUNDATION -> newFoundation[destIndex].addAll(cardsToMove)
            PileType.TABLEAU -> newTableau[destIndex].addAll(cardsToMove)
            else -> {}
        }

        return state.copy(
            waste = newWaste,
            foundation = newFoundation,
            tableau = newTableau,
            isGameWon = checkWinCondition(newFoundation)
        )
    }
}

enum class PileType {
    STOCK, WASTE, FOUNDATION, TABLEAU
}
