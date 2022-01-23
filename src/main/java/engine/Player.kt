package engine

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class Player(private val name: String, private val username: String,  val client: Client) {
    var pos: Int = -1
    var teamPos: Int = -1
    var isBot: Boolean = false
    var playerState: PlayerStatus = PlayerStatus.NONE
    var hand: Hand = Hand()
    var team: Team? = null
    var action: Action? = null
    var bet: Int = -1

    fun getUsername(): String = username
    fun getName(): String = name

    fun resetHand() {
        bet = -1
        hand.removeAllCards()
    }

    fun addCard(card: Card?) {
        hand.addCard(card)
    }

    fun toSFSObject(): ISFSObject? {
        return SFSObject().apply {
            putInt("pos", pos)
            putInt("bet", bet)
            putBool("is_bot", isBot)
            putInt("teamPos", teamPos)
            putUtfString("name", name)
            putUtfString("username", username)
            if (team != null)
                putUtfString("team", team?.getName())
        }
    }


    sealed class Action{
        object Idle: Action()
        data class DrawCard(private val card : Int) : Action()
    }

}
