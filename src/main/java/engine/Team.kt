package engine

class Team(private val name: String) {

    companion object {
        public const val MAX_PLAYERS = 3
    }

    private val players = arrayOfNulls<Player>(MAX_PLAYERS)
    private var nextIndex = 0
    private val won = arrayListOf<WonHand>()
    private val points = 0
    private val targetHands = 5

    fun getName() = name

    fun addPlayer(player: Player): Int {
        if (nextIndex < MAX_PLAYERS) {
            player.team = this
            player.teamPos = nextIndex
            player.playerState = PlayerStatus.READY
            val index = nextIndex
            players[nextIndex] = player
            nextIndex++
            return index
        }
        return -1
    }

    fun isInTeam(player: Player): Boolean {
        return player.team != null && player.teamPos != -1 && player.teamPos < MAX_PLAYERS
    }

    fun removeIfAlreadyJoined(player: Player): Boolean {
        if (isInTeam(player)) {
            player.team!!.players[player.teamPos] = null
            nextIndex--
            return true
        }
        return false
    }

    fun addPlayerAt(player: Player, pos: Int) {
        if (pos < MAX_PLAYERS) {
            removeIfAlreadyJoined(player)
            player.team = this
            player.teamPos = players.size
            player.playerState = PlayerStatus.READY
            players[pos] = player
        }
    }


}