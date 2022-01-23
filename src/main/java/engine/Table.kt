package engine

import extension.room.RoomExtension
import java.util.*

class Table(private val gameExt: RoomExtension) {
    companion object {
        const val MAX_PLAYERS = 6
        const val WAITING_TIME = 30f
        const val MIN_BET_TARGET_FIRST_PLAYER = 5
        const val MIN_BET_TARGET_OTHER = 6
        val allowedBets = arrayOf(5, 6, 7, 8, 48)
    }

    enum class State {
        NONE, STARTED, BETTING, FINAL_BETTING, RUNNING
    }

    private var maxBet: Int = 0
    val players = arrayListOf<Player>()
    val deck = Deck()
    val boardCards = hashMapOf<Player, Card>()
    var team1 = Team("team_1")
    var team2 = Team("team_2")
    var trump: Int = -1
    var primaryTeam: Team? = null
    var dealerPos: Int = -1
    var actorPos: Int = -1
    var isRunning: Boolean = false

    private var timer: Timer? = null
    private var delayFlag = false
    private var state = State.NONE
    private val monitor = Object()

    fun getState() = state

    fun whereis(): String? {
        val ste = Thread.currentThread().stackTrace[2]
        //		System.out.println(where);
        return gameExt.parentRoom.name + ": " + ste.className + " " + ste.methodName + " " + ste.lineNumber + " "
    }

    public fun run() {
        state = State.STARTED
        isRunning = true
        dealerPos = -1
        actorPos = -1
        boardCards.clear()
        whereis()
        LogOutput.traceLog("Game is running")
        resetHand()
        rotateDealer()
        while (true) {
            rotateActor()
            dealCards()
            chooseBet()
            chooseTrump()

            startGame()

            RoomExtension.autoDeleteRooms(gameExt.parentZone)
        }
    }

    private fun startGame() {
        var round = 0

        while (round < 8) {
            LogOutput.traceLog("====================>>>>>Start Game  ${round}")
            for (i in 0 until players.size){
                LogOutput.traceLog("====================>>>>>Aciton Required for ${players[actorPos].toSFSObject()?.dump}")
                gameExt.setPlayerActive(actorPos)
                gameExt.updateCards(players[actorPos])
                if(players[actorPos].isBot){
                    var firstCard : Card? = null
                    if(boardCards.isNotEmpty()){
                        firstCard = boardCards.values.first()
                    }
                    delayTimer(0.6f)
                    players[actorPos].client.actionDrawCard(players[actorPos], boardCards, firstCard)
                }
                while(players[actorPos].action  !is Player.Action.DrawCard){
                    //Wait for player action
                    delayTimer(0.1f)
                    // LogOutput.traceLog("====================>>>>>Watiting for player to draw card ${players[actorPos].toSFSObject()?.dump}")
                }
                LogOutput.traceLog("====================>>>>>Aciton Required End Rotate for ${players[actorPos].toSFSObject()?.dump} ${players[actorPos].action}")
                rotateActor()
               // gameExt.setPlayerActive(actorPos)
            }
            delayTimer(3f)
            evaluateHand()
            boardCards.clear()
            players.forEach {
                it.action = Player.Action.Idle
            }
            gameExt.updatePlayers()
            delayTimer(2f)
            round++
        }
    }

    private fun evaluateHand() {


        val firstCard: Card = boardCards.values.first()
        val firstPlayer: Player = boardCards.keys.first()
        var topCard: Card = firstCard
        var topPlayer: Player = firstPlayer
        val hand =  WonHand()

        boardCards.forEach { player, card ->
            if(card.suit == trump){
                topCard = card
                topPlayer = player
            }else if(card.suit == topCard.suit && card.rank > topCard.rank){
                topCard = card
                topPlayer = player
            }
        }


        /*boardCards.forEach { player, card ->
            if (firstCard != null) {
                topCard = card
                firstCard = card
                topPlayer = player
                firstPlayer = player
            } else {
                if ((firstCard?.suit == card.suit && (topCard?.rank
                        ?: 0) > card.rank) || (topCard?.suit != card.suit && card.suit == trump)
                ) {
                    topCard = card
                    topPlayer =  player
                }
            }
            hand.addCard(card)
        }*/
        actorPos = topPlayer?.pos ?: 0
        topPlayer?.team?.addHand(hand)
        gameExt.wonHand(topPlayer, hand)

    }

    private fun chooseTrump() {
        players.forEach {
            if (it.bet > maxBet) {
                maxBet = it.bet
                actorPos = it.pos
            }
        }
        players[actorPos].client.askForTrump(players[actorPos])
        LogOutput.traceLog("============================> Waiting for trump game ${players[actorPos].toSFSObject()?.dump}=======================")
        while (trump == -1){
            //Wait while user selects trump
            LogOutput.traceLog("============================> Waiting for trump game ${trump}=======================")
        }
        LogOutput.traceLog("============================> Waiting end for trump game ${players[actorPos].toSFSObject()?.dump}=======================")
        if (trump == -1) {
            gameExt.actionSetTrump(Card.SPADES, players[actorPos].getUsername())
        }
        state = State.RUNNING
        LogOutput.traceLog("============================> Starting game=======================")
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
        while (playersToChoose >= 0) {
            rotateActor()
            players[actorPos].client.askToBet(players[actorPos])
            while (!isValidBet()) {
                delayTimer(0.1f)
            }
            gameExt.updatePlayers()
            playersToChoose--
        }
        val maxBet = players.maxOf { it.bet }
        if (maxBet <= 0) {
            state = State.FINAL_BETTING
            rotateActor()
            players[actorPos].client.askToBet(players[actorPos])
            while (!isValidBet()) {
                delayTimer(0.1f)
            }
        }
        gameExt.updatePlayers()
        LogOutput.traceLog("S===========================> Start choose trump card  <===================================")
    }

    private fun isValidBet() = players[actorPos].bet == 0 || allowedBets.indexOf(players[actorPos].bet ?: -1) >= 0

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

    fun getPlayerByUsername(name: String?): Player? {
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
                val index = team2.addPlayer(player)
                player.pos = index * 1 + index + 1
                player.team = team2
            }
            LogOutput.traceLog("Join New Player at ${player.team}")
            players.sortBy { it.pos }
            if (player.pos > -1) {
                gameExt.updatePlayers()
            }
        } ?: run {
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

    fun removePlayerByUsername(name: String?): Boolean {
        if (name != null) {
            getPlayerByUsername(name)?.let {
                players.remove(it)
                return true
            }
        }
        return false
    }

    fun getAllowedBets(): List<Int> {
        return allowedBets.filter { players.maxOf { it.bet } < it }.toMutableList().apply {
            if (state != State.FINAL_BETTING) {
                add(0)
            }
        }
    }

    fun cardDrawn(cardHashCode: Int, name: String?) {
        deck.getCard(cardHashCode)?.let { card ->
            getPlayerByUsername(name ?: "")?.let {
                boardCards[it] = card
                it.action = Player.Action.DrawCard(card.hashCode())
                it.hand.removeCard(cardHashCode)
            }
        }
    }
}