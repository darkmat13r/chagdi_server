package engine

import extension.room.RoomExtension

class BasicBot(private val ext : RoomExtension) : Client {
    override fun askToBet(player: Player) {
        LogOutput.traceLog("Bot ask to bet ${player.getName()}" )
        ext.setPlayerActive(player.pos)
        ext.actionBet(0, player.getName())
    }
}