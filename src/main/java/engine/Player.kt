package engine

class Player(private val name: String, private val username: String) {
    var pos: Int? = null
    var teamPos: Int? = null
    var client: Client? = null
    var isBot: Boolean = false
    var playerState: PlayerStatus = PlayerStatus.NONE
    var hand: Hand = Hand()
    var team: Team? = null

    fun getUsername() : String = username
    fun getName() :String = name

    fun resetHand() {
        hand.removeAllCards()
    }

    fun addCard(card: Card?) {
        hand.addCard(card)
    }

}
