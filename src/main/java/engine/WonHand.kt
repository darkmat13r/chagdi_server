package engine

class WonHand {


    private val cards = arrayListOf<Card>()

    fun addCard(card: Card) {
        cards.add(card)
    }

    fun getCardsIds() = cards.map { it.hashCode() }
}