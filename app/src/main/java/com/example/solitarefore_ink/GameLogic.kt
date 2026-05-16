package com.example.solitarefore_ink

enum class Suit(val isRed: Boolean, val symbol: String) {
    SPADES(false, "♠"),
    CLUBS(false, "♣"),
    HEARTS(true, "♡"), // Outline for red suits
    DIAMONDS(true, "♢"); // Outline for red suits
}

enum class Rank(val value: Int, val symbol: String) {
    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K")
}

data class Card(
    val suit: Suit,
    val rank: Rank,
    var isFaceUp: Boolean = false
) {
    val id: String get() = "${rank.symbol}${suit.symbol}"
}

data class GameState(
    val stock: List<Card> = emptyList(),
    val waste: List<Card> = emptyList(),
    val foundation: List<List<Card>> = List(4) { emptyList() },
    val tableau: List<List<Card>> = List(7) { emptyList() },
    val isGameWon: Boolean = false
)

object DeckManager {
    fun createDeck(): List<Card> {
        val cards = mutableListOf<Card>()
        for (suit in Suit.values()) {
            for (rank in Rank.values()) {
                cards.add(Card(suit, rank))
            }
        }
        return cards.shuffled()
    }
}
