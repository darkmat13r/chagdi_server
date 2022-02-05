package engine

import com.smartfoxserver.v2.entities.data.SFSObject
import extension.room.RoomExtension
import java.util.*
import kotlin.math.abs


class Table(private val gameExt: RoomExtension) {
    companion object {
        const val NEXT_GAME_WAIT: Float = 10f
        const val MAX_PLAYERS = 6
        private const val MAX_HAND = 8
        val allowedBets = arrayOf(5, 6, 7, MAX_HAND, 48)
    }

    enum class State {
        NONE, STARTED, BETTING, FINAL_BETTING, RUNNING, SHOW_RESULT
    }

    private var maxBet: Int = 0
    val players = arrayListOf<Player>()
    val deck = Deck()
    val boardCards = hashMapOf<Int, Card>()
    var team1 = Team("team_1")
    var team2 = Team("team_2")
    var trump: Int = -1
    var primaryTeam: Team? = null
    var dealerPos: Int = -1
    var actorPos: Int = -1
    var firstPlayerPos: Int = -1
    var maxBetPlayerPos: Int = -1
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
        whereis()
        LogOutput.traceLog("Game is running")
        rotateDealer()
        while (true) {
            state = State.RUNNING
            joinTeamWait()
            resetHand()
            rotateActor()
            dealCards()
            chooseBet()
            chooseTrump()
            startGame()
            evaluateResult()
            RoomExtension.autoDeleteRooms(gameExt.parentZone)
        }
    }

    private fun joinTeamWait() {
        var playerJoinedCount = players.size
        while (players.filter { it.pos >= 0 }.size != MAX_PLAYERS) {
            if (playerJoinedCount != players.size) {
                gameExt.updatePlayers()
                playerJoinedCount = players.size
            }
        }
        gameExt.send("start_game", SFSObject(), gameExt.parentRoom.userList)
    }

    private fun evaluateResult() {
        val player = players[maxBetPlayerPos]
        val dealer = players[dealerPos]
        if ((player.team?.getWonCount() ?: 0) >= player.bet) {
            dealer.points += player.bet
        } else {
            dealer.points -= player.bet
        }
        if (dealer.points < 0) {
            val points = dealer.points
            rotateDealer()
            players[dealerPos].points = abs(points)
            player.points = 0
        } else if (dealer.points >= 48) {
            dealerPos += 1
            rotateDealer()
            dealer.points = 0
        }
        gameExt.updatePlayers()
        gameExt.waitForNextGame()
        delayTimer(NEXT_GAME_WAIT)
    }

    private fun startGame() {
        var round = 0
        while (round < MAX_HAND && state == State.RUNNING) {
            LogOutput.traceLog("====================>>>>>Start Game  ${round}")
            for (i in 0 until players.size) {
                LogOutput.traceLog("====================>>>>>Aciton Required for ${players[actorPos].toSFSObject()?.dump}")
                gameExt.setPlayerActive(actorPos)
                gameExt.updateCards(players[actorPos])
                if (players[actorPos].isBot) {
                    var firstCard: Card? = null
                    if (boardCards.isNotEmpty()) {
                        firstCard = boardCards.values.first()
                    }
                    delayTimer(0.6f)
                    players[actorPos].client.actionDrawCard(players[actorPos], boardCards, firstCard)
                }
                while (players[actorPos].action !is Player.Action.DrawCard) {
                    //Wait for player action
                    delayTimer(0.1f)
                    // LogOutput.traceLog("====================>>>>>Watiting for player to draw card ${players[actorPos].toSFSObject()?.dump}")
                }
                LogOutput.traceLog("====================>>>>>Aciton Required End Rotate for ${players[actorPos].toSFSObject()?.dump} ${players[actorPos].action}")
                rotateActor()
                // gameExt.setPlayerActive(actorPos)
            }
            players.forEach {
                it.action = Player.Action.Idle
            }
            delayTimer(3f)
            evaluateHand()
            boardCards.clear()
            gameExt.updatePlayers()
            round++
        }
    }

    private fun evaluateHand() {
        val firstCard: Card = boardCards.values.first()
        val firstPlayer: Player = players[boardCards.keys.first()]
        var topCard: Card = firstCard
        var topPlayer: Player = firstPlayer
        val hand = WonHand()

        boardCards.forEach { player, card ->
            if (card.suit == trump) {
                if (topCard.suit != trump || card.rank > topCard.rank) {
                    topCard = card
                    topPlayer = players[player]
                }
            } else if (card.suit == firstCard.suit && card.rank > topCard.rank) {
                topCard = card
                topPlayer = players[player]
            }
        }
        actorPos = topPlayer.pos ?: 0
        topPlayer.team?.addHand(hand)
        gameExt.wonHand(topPlayer, hand)
        //Check if other team manage to won required hand
        if (topPlayer.team != players[maxBetPlayerPos].team) {
            val wonCount = topPlayer.team?.getWonCount() ?: 0
            if (wonCount >= MAX_HAND - maxBet) {
                state = State.SHOW_RESULT
            }
        } else {
            val wonCount = topPlayer.team?.getWonCount() ?: 0
            if (wonCount >= maxBet) {
                state = State.SHOW_RESULT
            }
        }
    }

    private fun chooseTrump() {
        players.forEach {
            if (it.bet > maxBet) {
                maxBet = it.bet
                maxBetPlayerPos = it.pos
            }
        }
        actorPos = maxBetPlayerPos
        players[actorPos].client.askForTrump(players[actorPos])
        LogOutput.traceLog("============================> Waiting for trump game ${players[actorPos].toSFSObject()?.dump}=======================")
        while (trump == -1) {
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
            if (firstPlayerPos == -1) {
                firstPlayerPos = actorPos
            }
            players[actorPos].client.askToBet(players[actorPos])
            LogOutput.traceLog("============================= Ask Bet ${actorPos} player to choose ${playersToChoose}")
            while (!isValidBet()) {
                delayTimer(0.1f)
                LogOutput.traceLog("============================= check Bet ${actorPos} player to choose ${players[actorPos].toSFSObject()?.dump}")
            }
            gameExt.updatePlayers()
            playersToChoose--
        }
        val maxBet = players.maxOf { it.bet }
        LogOutput.traceLog("============================= ${maxBet}")
        gameExt.updatePlayers()
        if (maxBet == 0) {
            state = State.FINAL_BETTING
            actorPos = firstPlayerPos
            players[actorPos].client.askToBet(players[actorPos])
            LogOutput.traceLog("============================= current bet ${players[actorPos].bet}")
            while (players[actorPos].bet <= 0) {
                LogOutput.traceLog("============================= waiting for bet ${players[actorPos].bet}")
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
        firstPlayerPos = -1
        boardCards.clear()
        players.forEach {
            it.resetHand()
        }
        gameExt.reset()
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

    fun joinTeam(username: String, pos: Int = 0) {
        getPlayerByUsername(username)?.let { player ->
            LogOutput.traceLog("Join New Player ${player.getName()}")
            val isFirstTeamPos = arrayOf(0, 2, 4).contains(pos)
            val oldTeamPos = player.pos
            val checkPos = players.filter { it.pos == pos }.isEmpty()
            if (checkPos) {
                player.team?.removeIfAlreadyJoined(player)
                if (isFirstTeamPos) {
                    team1.addPlayer(player)
                    player.team = team1
                } else {
                    team2.addPlayer(player)
                    player.team = team2
                }
                player.pos = pos
            }
            gameExt.send("team_joined", SFSObject().apply {
                putInt("old_pos", oldTeamPos)
                putInt("pos", pos)
                putSFSObject("player", player.toSFSObject())
            }, gameExt.parentRoom.userList)
            /* if (isFirstTeamPos) {
                 team1.addPlayer(player)
                 player.pos =pos
                 player.team = team1
             } else {
                 team2.addPlayer(player)
                 player.pos =pos
                 player.team = team2
             }*/
            LogOutput.traceLog("Join New Player at ${player.pos}")
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
        val bets = arrayListOf<Int>().apply {
            addAll(allowedBets.filter { players.maxOf { it.bet } < it }
                .toMutableList())
        }.apply {
            if (state != State.FINAL_BETTING) {
                add(0)
            }
        }
        if (actorPos != firstPlayerPos) {
            bets.remove(5)
        }
        return bets
    }

    fun cardDrawn(cardHashCode: Int, pos: Int) {
        deck.getCard(cardHashCode)?.let { card ->
            players[pos].let {
                boardCards[pos] = card
                it.action = Player.Action.DrawCard(card.hashCode())
                it.hand.removeCard(cardHashCode)
            }
            LogOutput.traceLog("BoardsCards ${boardCards.maxOf { it.value.toDescriptionString() + " - " + players[it.key] }}")
        }
    }
}