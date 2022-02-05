package engine

import extension.room.RoomExtension

class BasicBot(private val ext: RoomExtension) : Client {
    override fun askToBet(player: Player) {
        LogOutput.traceLog("Bot ask to bet ${player.getName()}")
        ext.setPlayerActive(player.pos)
        val allowedBets = ext.table.getAllowedBets().sortedBy { it }

        val cards = player.hand.getCards()
        val kindsOfCards = hashMapOf<Int, Int>()
        cards.forEach {
            it?.let {
                kindsOfCards[it.suit] = kindsOfCards[it.suit]?.plus(1) ?: 1
            }
        }
        val numberOfHands = kindsOfCards.maxOf { it.value }
        LogOutput.traceLog("${player.getName()}  can make ${numberOfHands} hands")
        allowedBets.forEach {
            if (numberOfHands <= it) {
                ext.actionBet(it, player.getUsername())
                return@forEach
            }
        }
        ext.actionBet(0, player.getUsername())

    }

    override fun askForTrump(player: Player) {
        ext.actionSetTrump(Card.SPADES, player.getUsername())
    }

    override fun actionDrawCard(player: Player, boardCards: HashMap<Int, Card>, firstCard: Card?) {
        //TODO improve bot
        var drawCard: Card? = null

        //Condition 1 : When first chance is not for bot
        ////
        //// First card is not trump : Check if teammate played card other than first card drawn and not trump,
        //// and opponent didn't played trump - Play bigger card or play trump or play smaller card if dont have card of above type
        ////
        //// FIrst card is trump : Check whoes team has highest trump: play highest, play lowest or play the least count card
        ////
        ////


        if (firstCard?.suit != null && player.hand.containsSuit(firstCard.suit)) {
            player.hand.getCards().filter { it?.suit == firstCard.suit }.sortedBy { it?.rank }
                .firstOrNull { it?.suit == firstCard.suit }?.let {
                drawCard = it
            }
        }
        if (drawCard == null) {
            if ( player.hand.containsSuit(ext.table.trump)) {
                player.hand.getCards().filter { it?.suit == ext.table.trump }.sortedBy { it?.rank }
                    .firstOrNull { it?.suit == ext.table.trump }?.let {
                        drawCard = it
                    }
            }
        }
        if(drawCard == null){
            drawCard = player.hand.getCards().sortedBy { it?.rank }.first()
        }
        player.action = Player.Action.DrawCard(drawCard.hashCode())
        ext.actionDrawCard(drawCard.hashCode(), player.getUsername())
    }


}