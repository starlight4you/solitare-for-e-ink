package com.example.solitarefore_ink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SolitaireViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()
    
    private var timerJob: Job? = null
    
    // Stack to keep track of previous states for the Undo feature
    private val history = mutableListOf<GameState>()

    init {
        startNewGame()
    }

    fun startNewGame() {
        history.clear()
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

        _state.update { currentState ->
            GameState(
                stock = deck,
                waste = emptyList(),
                foundation = List(4) { emptyList() },
                tableau = newTableau,
                isGameWon = false,
                hintedCardId = null,
                drawMode = currentState.drawMode,
                cardBackStyle = currentState.cardBackStyle,
                score = 0,
                elapsedSeconds = 0,
                recycleCount = 0
            )
        }
        startTimer()
    }

    private fun saveStateToHistory() {
        // Deep copy the state lists so modifying current doesn't change history
        val currentState = _state.value
        val historyState = currentState.copy(
            stock = currentState.stock.map { it.copy() },
            waste = currentState.waste.map { it.copy() },
            foundation = currentState.foundation.map { pile -> pile.map { it.copy() } },
            tableau = currentState.tableau.map { pile -> pile.map { it.copy() } }
        )

        // Limit history size to prevent memory issues
        if (history.size >= 50) {
            history.removeAt(0)
        }
        history.add(historyState)
    }

    fun undo() {
        if (history.isNotEmpty()) {
            val previousState = history.removeAt(history.size - 1)
            // Restore everything except the elapsed time so the timer doesn't jump backwards weirdly
            _state.update { currentState ->
                previousState.copy(elapsedSeconds = currentState.elapsedSeconds)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { currentState ->
                    if (!currentState.isGameWon) {
                        val newSeconds = currentState.elapsedSeconds + 1
                        // -2 points every 10 seconds based on common solitaire rules
                        val newScore = if (newSeconds % 10 == 0) {
                            maxOf(0, currentState.score - 2)
                        } else {
                            currentState.score
                        }
                        currentState.copy(elapsedSeconds = newSeconds, score = newScore)
                    } else currentState
                }
            }
        }
    }

    fun drawCard() {
        val currentState = _state.value
        val cardsToDrawCount = if (currentState.drawMode == 3) {
            minOf(3, currentState.stock.size)
        } else {
            if (currentState.stock.isNotEmpty()) 1 else 0
        }

        if (cardsToDrawCount > 0) {
            saveStateToHistory()
            val drawnCards = currentState.stock.takeLast(cardsToDrawCount).reversed()
            val newStock = currentState.stock.dropLast(cardsToDrawCount)
            val newWaste = currentState.waste + drawnCards.map { it.apply { isFaceUp = true } }
            _state.value = currentState.copy(stock = newStock, waste = newWaste, hintedCardId = null)
        } else if (currentState.waste.isNotEmpty()) {
            saveStateToHistory()
            // Recycle waste to stock
            val newStock = currentState.waste.reversed().map { it.apply { isFaceUp = false } }
            
            var newScore = currentState.score
            val newRecycleCount = currentState.recycleCount + 1
            if (currentState.drawMode == 1) {
                newScore = maxOf(0, newScore - 100)
            } else {
                if (newRecycleCount > 3) {
                    newScore = maxOf(0, newScore - 20)
                }
            }
            
            _state.value = currentState.copy(
                stock = newStock, 
                waste = emptyList(), 
                hintedCardId = null,
                score = newScore,
                recycleCount = newRecycleCount
            )
        } else {
            _state.value = currentState.copy(hintedCardId = null)
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
    
    fun autoMoveCard(card: Card, sourcePileType: PileType, sourcePileIndex: Int = -1) {
        val currentState = _state.value
        val clearHintState = currentState.copy(hintedCardId = null)
        
        // Try foundation first
        for (i in 0 until 4) {
            if (canMoveToFoundation(card, clearHintState.foundation[i])) {
                saveStateToHistory()
                _state.value = moveCardImplementation(clearHintState, sourcePileType, sourcePileIndex, PileType.FOUNDATION, i, 1)
                return
            }
        }

        // If not foundation, try tableau
        val cardsToMove = getCardsToMove(clearHintState, sourcePileType, sourcePileIndex, card)
        if (cardsToMove.isEmpty()) {
            _state.value = clearHintState
            return
        }

        val bottomCardOfStack = cardsToMove.first()
        
        for (i in 0 until 7) {
            if (sourcePileType == PileType.TABLEAU && i == sourcePileIndex) continue
            if (canMoveToTableau(bottomCardOfStack, clearHintState.tableau[i])) {
                 saveStateToHistory()
                 _state.value = moveCardImplementation(clearHintState, sourcePileType, sourcePileIndex, PileType.TABLEAU, i, cardsToMove.size)
                 return
            }
        }
        _state.value = clearHintState
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
        var scoreChange = 0
        if (sourceType == PileType.WASTE && destType == PileType.FOUNDATION) scoreChange += 10
        if (sourceType == PileType.WASTE && destType == PileType.TABLEAU) scoreChange += 5
        if (sourceType == PileType.TABLEAU && destType == PileType.FOUNDATION) scoreChange += 10
        if (sourceType == PileType.FOUNDATION && destType == PileType.TABLEAU) scoreChange -= 15

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
                    scoreChange += 5
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
        
        val isWon = checkWinCondition(newFoundation)
        if (isWon) {
            timerJob?.cancel()
        }

        return state.copy(
            waste = newWaste,
            foundation = newFoundation,
            tableau = newTableau,
            isGameWon = isWon,
            score = maxOf(0, state.score + scoreChange)
        )
    }

    fun requestHint() {
        val currentState = _state.value
        if (currentState.isGameWon) return

        var hintedCard: Card? = null

        // 1. Check if waste card can move anywhere
        if (currentState.waste.isNotEmpty()) {
            val wasteCard = currentState.waste.last()
            if (canMoveToAnyFoundation(wasteCard, currentState.foundation) ||
                canMoveToAnyTableau(wasteCard, currentState.tableau)) {
                hintedCard = wasteCard
            }
        }

        // 2. Check if any tableau card can move
        if (hintedCard == null) {
            for (i in 0 until 7) {
                val pile = currentState.tableau[i]
                if (pile.isEmpty()) continue

                // Check top card for foundation
                val topCard = pile.last()
                if (canMoveToAnyFoundation(topCard, currentState.foundation)) {
                    hintedCard = topCard
                    break
                }

                // Check any face-up card for tableau move
                val faceUpCards = pile.filter { it.isFaceUp }
                for (card in faceUpCards) {
                    // Avoid moving a King from an empty space to another empty space
                    if (card.rank == Rank.KING && pile.indexOf(card) == 0) continue
                    
                    if (canMoveToAnyTableau(card, currentState.tableau, sourcePileIndex = i)) {
                        hintedCard = card
                        break
                    }
                }
                if (hintedCard != null) break
            }
        }

        _state.update { it.copy(hintedCardId = hintedCard?.id) }
    }

    private fun canMoveToAnyFoundation(card: Card, foundations: List<List<Card>>): Boolean {
        return foundations.any { canMoveToFoundation(card, it) }
    }

    private fun canMoveToAnyTableau(card: Card, tableau: List<List<Card>>, sourcePileIndex: Int = -1): Boolean {
        for (i in 0 until 7) {
            if (i == sourcePileIndex) continue
            if (canMoveToTableau(card, tableau[i])) return true
        }
        return false
    }

    fun updateDrawMode(mode: Int) {
        _state.update { it.copy(drawMode = mode) }
    }

    fun updateCardBackStyle(style: CardBackStyle) {
        _state.update { it.copy(cardBackStyle = style) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

enum class PileType {
    STOCK, WASTE, FOUNDATION, TABLEAU
}