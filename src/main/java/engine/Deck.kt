package engine

import java.lang.IllegalStateException
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.security.SecureRandom
import java.util.*

class Deck {
    /** The cards in the deck.  */
    private val cards: Array<Card?>

    /** The index of the next card to deal.  */
    private var nextCardIndex = 0

    /** Random number generator (crypographical quality).  */
    private val random: Random = SecureRandom()

    /**
     * Constructor.
     *
     * Starts as a full, ordered deck.
     */
    init {
        cards = arrayOfNulls(NO_OF_CARDS)
        var index = 0
        for (suit in Card.NO_OF_SUITS - 1 downTo 0) {
            for (rank in Card.NO_OF_RANKS - 1 downTo 0) {
                cards[index++] = Card(rank, suit)
            }
        }
        //        cards[0] = new Card(Card.JACK, Card.SPADES);
//        cards[1] = new Card(Card.JACK, Card.HEARTS);
//        cards[2] = new Card(Card.KING, Card.CLUBS);
//        cards[3] = new Card(Card.NINE, Card.DIAMONDS);
//        cards[4] = new Card(Card.QUEEN, Card.HEARTS);
//        cards[5] = new Card(Card.FIVE, Card.HEARTS);
//        cards[6] = new Card(Card.FOUR, Card.CLUBS);
//        cards[7] = new Card(Card.QUEEN, Card.SPADES);
//        cards[8] = new Card(Card.QUEEN, Card.CLUBS);
//        cards[9] = new Card(Card.FOUR, Card.HEARTS);
//        cards[10] = new Card(Card.FOUR, Card.SPADES);
    }

    /**
     * Shuffles the deck.
     */
    fun shuffle() {
        for (oldIndex in 0 until NO_OF_CARDS) {
            val newIndex = random.nextInt(NO_OF_CARDS)
            val tempCard = cards[oldIndex]
            cards[oldIndex] = cards[newIndex]
            cards[newIndex] = tempCard
        }
        nextCardIndex = 0
    }

    /**
     * Resets the deck.
     *
     * Does not re-order the cards.
     */
    fun reset() {
        nextCardIndex = 0
    }

    /**
     * Deals a single card.
     *
     * @return  the card dealt
     */
    fun deal(): Card? {
        check(nextCardIndex + 1 < NO_OF_CARDS) { "No cards left in deck" }
        return cards[nextCardIndex++]
    }

    /**
     * Deals multiple cards at once.
     *
     * @param noOfCards
     * The number of cards to deal.
     *
     * @return The cards.
     *
     * @throws IllegalArgumentException
     * If the number of cards is invalid.
     * @throws IllegalStateException
     * If there are no cards left in the deck.
     */
    fun deal(noOfCards: Int): List<Card?> {
        require(noOfCards >= 1) { "noOfCards < 1" }
        check(nextCardIndex + noOfCards < NO_OF_CARDS) { "No cards left in deck" }
        val dealtCards: MutableList<Card?> = ArrayList()
        for (i in 0 until noOfCards) {
            dealtCards.add(cards[nextCardIndex++])
        }
        return dealtCards
    }

    /**
     * Deals a specific card.
     *
     * @param rank
     * The card's rank.
     * @param suit
     * The card's suit.
     *
     * @return The card if available, otherwise null.
     *
     * @throws IllegalStateException
     * If there are no cards left in the deck.
     */
    fun deal(rank: Int, suit: Int): Card? {
        check(nextCardIndex + 1 < NO_OF_CARDS) { "No cards left in deck" }
        var card: Card? = null
        var index = -1
        for (i in nextCardIndex until NO_OF_CARDS) {
            if (cards[i]!!.rank == rank && cards[i]!!.suit == suit) {
                index = i
                break
            }
        }
        if (index != -1) {
            if (index != nextCardIndex) {
                val nextCard = cards[nextCardIndex]
                cards[nextCardIndex] = cards[index]
                cards[index] = nextCard
            }
            card = deal()
        }
        return card
    }


    fun getCard(hashCode : Int) :  Card?{
        return cards.first { it.hashCode() == hashCode }
    }
    /** {@inheritDoc}  */
    override fun toString(): String {
        val sb = StringBuilder()
        for (card in cards) {
            sb.append(card)
            sb.append(' ')
        }
        return sb.toString().trim { it <= ' ' }
    }

    companion object {
        /** The number of cards in a deck.  */
        private const val NO_OF_CARDS = Card.NO_OF_RANKS * Card.NO_OF_SUITS
    }
}