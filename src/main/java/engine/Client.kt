package engine

interface Client {
    fun askToBet(player: Player)
    fun askForTrump(player: Player)
    fun actionDrawCard(player: Player, boardCards: HashMap<Player, Card>, firstCard: Card?)
}