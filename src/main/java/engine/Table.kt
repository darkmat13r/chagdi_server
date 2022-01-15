package engine

import extension.room.RoomExtension
import java.util.*

class Table (private val gameExt : RoomExtension){
    companion object{
        const val MAX_PLAYERS = 6
    }
    val players  = arrayListOf<Player>()
    val deck = Deck()
    var board = arrayListOf<Card>()
    var team1 = Team("team_1")
    var team2 = Team("team_2")
    var trump :  Int = Card.ACE
    var dealerPos :  Int = -1
    var actorPos :  Int = -1
    var isRunning: Boolean = false

    private var timer: Timer? = null
    private var delayFlag = false
    private val monitor = Object()

    public fun run(){
        isRunning = true
        dealerPos = -1
        actorPos = -1
        while(true){
            resetHand()
            rotateActor()

            dealCards()

            chooseHandTarget()


        }
    }

    private fun dealCards() {
        deck.shuffle()
        var noOfCardsDealt = 0
        while(noOfCardsDealt < 48){
            val card = deck.deal()
            players[actorPos].addCard(card)
            gameExt.dealCard(card, players[actorPos])
            rotateActor()
            noOfCardsDealt++
        }
    }

    private fun chooseHandTarget() {
        val playersToChoose = MAX_PLAYERS
        while (playersToChoose > 0){
            rotateActor()
           // gameExt.askToChooseHandTarget()
        }
    }

    private fun rotateActor() {
        actorPos = getNextPos(actorPos)
    }

    private fun getNextPos(pos : Int) : Int{
        if(pos + 1 >= MAX_PLAYERS){
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
        LogOutput.traceLog("[table->delayTimer] begins")
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
        LogOutput.traceLog("[table->delayTimer] ends")
    }
    fun joinTeam(player: Player, team: Int) {
        if(team == 1){
            team1.addPlayer(player)
        }else{
            team2.addPlayer(player)

        }
        players.add(player)
        gameExt.sendPlayerJoined(player)
    }

}