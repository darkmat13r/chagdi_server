package engine

import extension.room.RoomExtension
import java.util.*

class Table(private val gameExt: RoomExtension) {
    companion object {
        const val MAX_PLAYERS = 6
        const val MIN_BET_TARGET_FIRST_PLAYER = 5
        const val MIN_BET_TARGET_OTHER = 6
    }

    val players = arrayListOf<Player>()
    val deck = Deck()
    var board = arrayListOf<Card>()
    var team1 = Team("team_1")
    var team2 = Team("team_2")
    var trump: Int = Card.ACE
    var dealerPos: Int = -1
    var actorPos: Int = -1
    var isRunning: Boolean = false

    private var timer: Timer? = null
    private var delayFlag = false
    private val monitor = Object()
    fun whereis(): String? {
        val ste = Thread.currentThread().stackTrace[2]
        //		System.out.println(where);
        return gameExt.parentRoom.name + ": " + ste.className + " " + ste.methodName + " " + ste.lineNumber + " "
    }

    public fun run() {
        isRunning = true
        dealerPos = -1
        actorPos = -1
        whereis()
        LogOutput.traceLog("Game is running")
        resetHand()
        rotateDealer()
        while (true) {
            rotateActor()
            dealCards()
            chooseBet()

            if (gameExt.parentRoom.userList.size == 0) {
                gameExt.api.removeRoom(gameExt.parentRoom)
            }
        }
    }

    private fun dealCards() {
        deck.shuffle()
        var noOfCardsDealt = 0
        while (noOfCardsDealt < 48) {
            val card = deck.deal()
            players[actorPos].addCard(card)
            gameExt.dealCard(card, players[actorPos])
            rotateActor()
            noOfCardsDealt++
            delayTimer(0.2f)
        }
    }

    private fun chooseBet() {
        var playersToChoose = MAX_PLAYERS
        while (playersToChoose > 0) {
            rotateActor()
            players[actorPos].client.askToBet(players[actorPos])
            while(players[actorPos].bet < 0 ){
                delayTimer(0.1f)
            }
            LogOutput.traceLog("=========>After Waiting for player to bet ${players[actorPos].getName()} ${players[actorPos].bet}")
            LogOutput.traceLog("==========>After  Waiting for player to bet ${players.map { "${it.pos} :  ${it.getName()}  ${it.bet}" }} ")
            playersToChoose--
            //gameExt.askToChooseHandTarget()
        }

    }

    private fun rotateActor() {
        actorPos = getNextPos(actorPos)

    }
    private fun rotateDealer() {
        dealerPos = getNextPos(dealerPos)
    }

    private fun getNextPos(pos: Int): Int {
        if (pos + 1 >= MAX_PLAYERS) {
            return 0
        }
        return pos + 1
    }

    private fun resetHand() {
        actorPos = -1
        dealerPos = -1
        players.forEach {
            it.resetHand()
        }
    }

    private fun delayTimer(_t: Float) {
        //for log trace
        delayFlag = true
        // SetTimer
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                // Your database code here
                if (delayFlag) {
                    delayFlag = false
                    synchronized(monitor) { monitor.notifyAll() }
                }
            }
        }, (_t * 1000).toInt().toLong())
        while (delayFlag) {
            // Wait for the user to select an action.
            synchronized(monitor) {
                try {
                    monitor.wait()
                } catch (e: InterruptedException) {
                    // Ignore.
                }
            }
        }
        timer?.cancel()
        //for log trace
    }

    fun getPlayerByUsername(name: String): Player? {
        return players.firstOrNull { it.getUsername() == name }
    }

    fun joinTeam(username: String, team: Int, pos: Int = 0) {
        getPlayerByUsername(username)?.let { player ->
            LogOutput.traceLog("Join New Player ${player.getName()}")
            if (team == 1) {
               val index = team1.addPlayer(player)
                player.pos = index * 1 + index
                player.team = team1
            } else {
                val index =  team2.addPlayer(player)
                player.pos = index * 1 + index + 1
                player.team = team2
            }
            LogOutput.traceLog("Join New Player at ${player.team}")
            if(player.pos > -1){
                gameExt.updatePlayers()
            }
        }?:run{
            LogOutput.traceLog("Error : ${username} not found ${players.map { it.getUsername() }.joinToString(",")}")
        }

    }

    fun addPlayer(player: Player) {
        whereis()
        if (getPlayerByUsername(player.getUsername()) == null)
            players.add(player)
        else
            LogOutput.traceLog("Player already added =========> ${player.getUsername()} ")
    }

    fun removePlayerByUsername(name: String?) : Boolean{
        if (name != null) {
            getPlayerByUsername(name)?.let{
                players.remove(it)
                return true
            }
        }
        return false
    }
}