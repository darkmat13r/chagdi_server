package engine

class Team (private val name : String){

    companion object {
        public const val MAX_PLAYERS = 3
    }

     val players = arrayOfNulls<Player>(MAX_PLAYERS)
    private val won = arrayListOf<WonHand>()
    private val points = 0
    private val targetHands = 5

    fun getName() = name

    fun addPlayer(player: Player) {
        if (players.size < MAX_PLAYERS){
            player.team = this
            player.teamPos = players.size
            players[players.size] = player
        }

    }


}