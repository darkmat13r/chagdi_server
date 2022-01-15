package engine

class Hand {
    /** The cards in this hand.  */
    private val cards = arrayOfNulls<Card>(MAX_NO_OF_CARDS)

    /** The current number of cards in this hand.  */
    private var noOfCards = 0

    /**
     * Constructor for an empty hand.
     */
    constructor() {
        // Empty implementation.
    }

    /**
     * Constructor with an array of initial cards.
     *
     * @param cards
     * The initial cards.
     *
     * @throws IllegalArgumentException
     * If the array is null or the number of cards is invalid.
     */
    constructor(cards: Array<Card?>?) {
        addCards(cards)
    }

    /**
     * Constructor with a collection of initial cards.
     *
     * @param cards
     * The initial cards.
     */
    constructor(cards: Collection<Card?>?) {
        //for log trace
        requireNotNull(cards) { "Null array" }
        for (card in cards) {
            addCard(card)
        }
        //for log trace
    }

    /**
     * Constructor with a string representing the initial cards.
     *
     * The string must contain of one or more cards. A card must be represented by a
     * rank and a suit character. The cards must be separated by a space character.
     *
     * Example: "Kh 7d 4c As Js"
     *
     * @param s
     * The string to parse.
     *
     * @throws IllegalArgumentException
     * If the string could not be parsed or the number of cards is too
     * high.
     */
    constructor(s: String?) {
        require(!(s == null || s.length == 0)) { "Null or empty string" }
        val parts = s.split("\\s").toTypedArray()
        require(parts.size <= MAX_NO_OF_CARDS) { "Too many cards in hand" }
        for (part in parts) {
            addCard(Card(part))
        }
    }

    /**
     * Returns the number of cards.
     *
     * @return The number of cards.
     */
    fun size(): Int {
        return noOfCards
    }

    /**
     * Adds a single card.
     *
     * The card is inserted at such a position that the hand remains sorted (highest
     * ranking cards first).
     *
     * @param card
     * The card to add.
     *
     * @throws IllegalArgumentException
     * If the card is null.
     */
    fun addCard(card: Card?) {
        //for log trace
        LogOutput.traceLog("[addCard] begins")
        requireNotNull(card) { "Null card" }
        var insertIndex = -1
        for (i in 0 until noOfCards) {
            if (card.compareTo(cards[i]!!) > 0) {
                insertIndex = i
                break
            }
        }
        if (insertIndex == -1) {
            // Could not insert anywhere, so append at the end.
            cards[noOfCards++] = card
        } else {
            System.arraycopy(cards, insertIndex, cards, insertIndex + 1, noOfCards - insertIndex)
            cards[insertIndex] = card
            noOfCards++
        }
        //for log trace
        LogOutput.traceLog("[addCard] ends")
    }

    /**
     * Adds multiple cards.
     *
     * The cards are inserted at such a position that the hand remains sorted
     * (highest ranking cards first).
     *
     * @param cards
     * The cards to add.
     */
    fun addCards(cards: Array<Card?>?) {
        //for log trace
        LogOutput.traceLog("[addCards] begins")
        requireNotNull(cards) { "Null array" }
        require(cards.size <= MAX_NO_OF_CARDS) { "Too many cards" }
        for (card in cards) {
            addCard(card)
        }
        //for log trace
        LogOutput.traceLog("[addCards] ends")
    }

    /**
     * Adds multiple cards.
     *
     * The cards are inserted at such a position that the hand remains sorted
     * (highest ranking cards first).
     *
     * @param cards
     * The cards to add.
     */
    fun addCards(cards: Collection<Card?>?) {
        //for log trace
        LogOutput.traceLog("[addCards] begins")
        requireNotNull(cards) { "Null collection" }
        require(cards.size <= MAX_NO_OF_CARDS) { "Too many cards" }
        for (card in cards) {
            addCard(card)
        }
        //for log trace
        LogOutput.traceLog("[addCards] ends")
    }

    /**
     * Returns the cards.
     *
     * @return The cards.
     */
    fun getCards(): Array<Card?> {
        val dest = arrayOfNulls<Card>(noOfCards)
        System.arraycopy(cards, 0, dest, 0, noOfCards)
        return dest
    }

    /**
     * Removes all cards.
     */
    fun removeAllCards() {
        //for log trace
        LogOutput.traceLog("[removeAllCards] begins")
        noOfCards = 0
        //for log trace
        LogOutput.traceLog("[removeAllCards] ends")
    }

    /** {@inheritDoc}  */
    override fun toString(): String {
        val sb = StringBuilder()
        for (i in 0 until noOfCards) {
            sb.append(cards[i])
            if (i < noOfCards - 1) {
                sb.append(' ')
            }
        }
        return sb.toString()
    }

    companion object {
        /** The maximum number of cards in a hand.  */
        private const val MAX_NO_OF_CARDS = 7
    }
}