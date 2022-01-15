package engine

import java.lang.IllegalArgumentException

class Card : Comparable<Card> {
    /**
     * Returns the rank.
     *
     * @return The rank.
     */
    /** The rank.  */
    val rank: Int
    /**
     * Returns the suit.
     *
     * @return The suit.
     */
    /** The suit.  */
    val suit: Int

    /**
     * Constructor based on rank and suit.
     *
     * @param rank
     * The rank.
     * @param suit
     * The suit.
     *
     * @throws IllegalArgumentException
     * If the rank or suit is invalid.
     */
    constructor(rank: Int, suit: Int) {
        var rank = rank
        var suit = suit
        if (rank < 0 || rank > NO_OF_RANKS - 1) {
//			throw new IllegalArgumentException("Invalid rank");
            println("Invalid rank: $rank")
            rank = if (rank < 0) 0 else if (rank > NO_OF_RANKS - 1) NO_OF_RANKS - 1 else rank
        }
        if (suit < 0 || suit > NO_OF_SUITS - 1) {
//			throw new IllegalArgumentException("Invalid suit");
            println("Invalid suit: $suit")
            suit = if (suit < 0) 0 else if (suit > NO_OF_SUITS - 1) NO_OF_SUITS - 1 else suit
        }
        this.rank = rank
        this.suit = suit
    }

    /**
     * Constructor based on a string representing a card.
     *
     * The string must consist of a rank character and a suit character, in that
     * order.
     *
     * @param s
     * The string representation of the card, e.g. "As", "Td", "7h".
     *
     * @throws IllegalArgumentException
     * If the card string is null or of invalid length, or the rank
     * or suit could not be parsed.
     */
    constructor(s: String?) {
        var s = s ?: throw IllegalArgumentException("Null string or of invalid length")
        s = s.trim { it <= ' ' }
        require(s.length == 2) { "Empty string or invalid length" }

        // Parse the rank character.
        val rankSymbol = s.substring(0, 1)
        val suitSymbol = s[1]
        var rank = -1
        for (i in 0 until NO_OF_RANKS) {
            if (rankSymbol == RANK_SYMBOLS[i]) {
                rank = i
                break
            }
        }
        require(rank != -1) { "Unknown rank: $rankSymbol" }
        // Parse the suit character.
        var suit = -1
        for (i in 0 until NO_OF_SUITS) {
            if (suitSymbol == SUIT_SYMBOLS[i]) {
                suit = i
                break
            }
        }
        require(suit != -1) { "Unknown suit: $suitSymbol" }
        this.rank = rank
        this.suit = suit
    }

    /** {@inheritDoc}  */
    override fun hashCode(): Int {
        return rank * NO_OF_SUITS + suit
    }

    /** {@inheritDoc}  */
    override fun equals(obj: Any?): Boolean {
        return obj is Card && obj.hashCode() == hashCode()
    }

    /** {@inheritDoc}  */
    override fun compareTo(card: Card): Int {
        val thisValue = hashCode()
        val otherValue = card.hashCode()
        return if (thisValue < otherValue) {
            -1
        } else if (thisValue > otherValue) {
            1
        } else {
            0
        }
    }

    /** {@inheritDoc}  */
    override fun toString(): String {
        return RANK_SYMBOLS[rank] + SUIT_SYMBOLS[suit]
    }

    fun toDescriptionString(): String {
        return RANK_COMPLETED_SYMBOLS[rank] + SUIT_SYMBOLS[suit]
    }

    companion object {
        /** The number of ranks in a deck.  */
        const val NO_OF_RANKS = 13

        /** The number of suits in a deck.  */
        const val NO_OF_SUITS = 4

        // The ranks.
        const val ACE = 12
        const val KING = 11
        const val QUEEN = 10
        const val JACK = 9
        const val TEN = 8
        const val NINE = 7
        const val EIGHT = 6
        const val SEVEN = 5
        const val SIX = 4
        const val FIVE = 3
        const val FOUR = 2
        const val THREE = 1
        const val DEUCE = 0

        // The suits.
        const val SPADES = 3
        const val HEARTS = 2
        const val CLUBS = 1
        const val DIAMONDS = 0

        /** The rank symbols.  */
        val RANK_SYMBOLS = arrayOf(
            "2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"
        )
        val RANK_COMPLETED_SYMBOLS = arrayOf(
            "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"
        )

        /** The suit symbols.  */
        val SUIT_SYMBOLS = charArrayOf('d', 'c', 'h', 's')
    }
}