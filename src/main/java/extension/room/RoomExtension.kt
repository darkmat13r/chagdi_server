package extension.room

import com.smartfoxserver.v2.api.CreateRoomSettings
import com.smartfoxserver.v2.core.SFSEventType
import com.smartfoxserver.v2.entities.Room
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.Zone
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.entities.variables.RoomVariable
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable
import com.smartfoxserver.v2.extensions.BaseSFSExtension
import com.smartfoxserver.v2.extensions.SFSExtension
import engine.*
import extension.zone.ZoneExtension
import utils.getRandomString
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.min

class RoomExtension : SFSExtension(), Client {

    lateinit var table: Table
    private lateinit var roomName: String
    private val names = arrayListOf(
        "Blabbermouth",
        "Hercules",
        "Konversation",
        "Lollipop",
        "Smart Bot",
        "Christina",
        "Aida",
        "Oscar",
        "Hanna",
        "Gadget",
        "Commander",
        "Connie",
    )

    override fun init() {
        addRequestHandler("join_team", JoinTeamHandler::class.java)
        addRequestHandler("request_join_room", GameJoinRequestHandler::class.java)
        addRequestHandler("action_bet", ActionBetHandler::class.java)
        addRequestHandler("action_select_trump", ActionSelectTrumpHandler::class.java)
        addRequestHandler("action_draw_card", ActionDrawCardHandler::class.java)

        addEventHandler(SFSEventType.USER_JOIN_ROOM, JoinRoomHandler::class.java)
        addEventHandler(SFSEventType.USER_LEAVE_ROOM, LeaveHandler::class.java)
        addEventHandler(SFSEventType.USER_DISCONNECT, LeaveHandler::class.java)

        table = Table(this)
        roomName = this.getGameRoom()?.getName() ?: ""
    }


    fun getGameRoom(): Room? {
        return parentRoom
    }

    fun requestJoinGame(user: User) {
        if (!parentZone.userList.isNullOrEmpty())
            parentZone.extension.send("game_join_request", SFSObject().apply {
                putUtfString("room", roomName)
                putUtfString("name", user.name)
            }, parentZone.userList.map { it!! })
    }

    fun joinTeam(name: String, team: Int) {
        table.joinTeam(name, team)

        if (table.players.size == Table.MAX_PLAYERS && !table.isRunning) {
            table.run()
        }
    }

    fun error(error: String, playerToNotify: String) {
        send("game_error", SFSObject().apply {
            putUtfString("reason", error)
        }, parentRoom.getUserByName(playerToNotify))
    }

    fun updatePlayers() {
        try {
            send("update_players", SFSObject().apply {
                val playerInfos = SFSArray()
                table.players.forEach {
                    playerInfos.addSFSObject(it.toSFSObject())
                }
                putSFSArray("player_infos", playerInfos)
            }, parentRoom.userList)
        } catch (ex: Exception) {
            LogOutput.traceLog("================================> error is shere ${ex.message}")
        }
    }

    fun dealCard(card: Card?, player: Player) {
        val otherPlayers = getOtherUsers(player)
        val params = SFSObject()
        params.putInt("pos", player.pos ?: -1)
        params.putInt("actor_pos", table.dealerPos)
        params.putUtfString("username", player.getUsername())
        params.putInt("card", card.hashCode())
        params.putInt("card_pos", player.hand.size() - 1)
        if (!player.isBot) {
            send("deal_card", params, parentRoom.getUserByName(player.getUsername()))
        }
        params.putInt("card", CARD_BACK_NUM)
        send("deal_card", params, otherPlayers)
    }

    private fun getOtherUsers(player: Player?) = parentRoom.userList.filter { it.name != player?.getName() }

    fun join(name: String) {
        val player = Player(name, name, this)
        table.addPlayer(player)
        if (table.players.size == 2) {
            addBots()
        }
        send("join_team", SFSObject(), parentRoom.getUserByName(name))
    }

    private fun addBots() {
        for (i in 0 until 2) {
            val rd = Random()
            var newName = names[rd.nextInt(names.size)].lowercase().replace(" ", "_")
            if (table.getPlayerByUsername(newName) != null) {
                newName = "${newName}_${1}"
            }
            val botPlayer = Player(newName, newName, BasicBot(this))
            botPlayer.isBot = true
            table.addPlayer(botPlayer)
            table.joinTeam(newName, 1)

        }
        for (i in 0 until 2) {
            val rd = Random()
            var newName = names[rd.nextInt(names.size)].lowercase().replace(" ", "_")
            if (table.getPlayerByUsername(newName) != null) {
                newName = "${newName}_${1}"
            }
            val botPlayer = Player(newName, newName, BasicBot(this))
            botPlayer.isBot = true
            table.addPlayer(botPlayer)
            table.joinTeam(newName, 2)
        }
    }

    fun leavePlayer(name: String?, i: Int): Boolean {
        table.removePlayerByUsername(name)
        send("game_leave", SFSObject().apply {
            putUtfString("name", name)
        }, parentRoom.userList)
        return true
    }

    override fun askToBet(player: Player) {
        val minTarget = getMinBet(player)
        setPlayerActive(player.pos)
        send("ask_bet", SFSObject().apply {
            putInt("pos", player.pos)
            putInt("min_bet", minTarget)
            putIntArray("allowed_bets", table.getAllowedBets())
            putUtfString("username", player.getUsername())
        }, parentRoom.getUserByName(player.getUsername()))
    }

    override fun askForTrump(player: Player) {
        setPlayerActive(player.pos)
        send("ask_trump", SFSObject().apply {
            putInt("pos", player.pos)
            putUtfString("username", player.getUsername())
        }, parentRoom.getUserByName(player.getUsername()))
    }

    override fun actionDrawCard(player: Player, boardCards: HashMap<Player, Card>, firstCard: Card?) {
        //Not required for clients
    }

    fun actionDrawCard(card: Int, name: String?) {
        LogOutput.traceLog("====================>>>>>ActionDrawCard ${name}")
        val player = table.getPlayerByUsername(name)
        if (player?.pos == table.actorPos) {
            table.cardDrawn(card, name)
            updatePlayers()
            send("action_card_drawn", SFSObject().apply {
                putInt("card", card)
                putInt("pos", table.getPlayerByUsername(name ?: "")?.pos ?: 0)
                putInt("place_at", table.boardCards.size - 1)
            }, getOtherUsers(table.getPlayerByUsername(name)))
        }
    }

    private fun getMinBet(player: Player): Int {
        val maxBetPlaced = table.players.maxOf { it.bet ?: -1 }
        var nextPlayerPos = table.dealerPos + 1
        if (nextPlayerPos >= Table.MAX_PLAYERS) {
            nextPlayerPos = 0
        }
        var minTarget = Table.allowedBets.minOf { it }
        if (player.pos != nextPlayerPos) {
            minTarget = Table.allowedBets.maxOf { it }
        }
        if (maxBetPlaced == -1) {
            minTarget = maxBetPlaced
        }
        return min(0, minTarget)
    }


    fun actionBet(bet: Int, name: String) {
        table.getPlayerByUsername(name)?.apply {
            LogOutput.traceLog("No Player found with ${table.getAllowedBets()} ${bet}")
            if (table.getAllowedBets().contains(bet)) {
                this.bet = bet
            } else {
                askToBet(this)
                error("Not allow to bet this amount", name)
            }
            updatePlayers()
        } ?: run {
            LogOutput.traceLog("No Player found with ${name} ${bet}")
        }
    }

    fun setPlayerActive(actorPos: Int) {
        send("set_actor", SFSObject().apply {
            putInt("pos", actorPos)
        }, parentRoom.userList)
    }


    fun actionSetTrump(trump: Int, name: String?) {
        table.trump = trump
        table.primaryTeam = table.getPlayerByUsername(name ?: "")?.team
        updatePlayers()
        send("action_set_trump", SFSObject().apply {
            putInt("trump", trump)
        }, parentRoom.userList)
    }


    fun wonHand(topPlayer: Player?, hand: WonHand) {
        topPlayer?.let {
            send("won", SFSObject().apply {
                putUtfString("team", topPlayer.team?.getName() ?: "")
                putIntArray("hand", hand.getCardsIds())
            }, parentRoom.userList)
        }
    }

    fun updateCards(player: Player) {
        val enabledCards = arrayListOf<Int>()
        val firstCard = table.boardCards.values.firstOrNull()
        val firstCardSuit = firstCard?.suit
        val containsFirstCardSuit = if (firstCardSuit == null) false else player.hand.containsSuit(firstCardSuit)
        player.hand.getCards().forEach {
            LogOutput.traceLog(
                "UdpateCards ${player.getUsername()} first ${firstCard?.toDescriptionString()} ${it?.toDescriptionString()} - ${
                    player.hand.containsSuit(
                        firstCardSuit ?: -1
                    )
                }"
            )
            if (it != null) {
                if (firstCardSuit == null) {
                    enabledCards.add(it.hashCode())
                } else if (firstCardSuit == it.suit) {
                    enabledCards.add(it.hashCode())
                } else if (!player.hand.containsSuit(firstCardSuit)) {
                    enabledCards.add(it.hashCode())
                }
            }
        }

        if (!player.isBot) {
            send("update_cards", SFSObject().apply {
                putIntArray("cards", enabledCards)
                putInt("pos", player.pos)
            }, parentRoom.getUserByName(player.getUsername()))
        }

    }

    companion object {
        private const val CARD_BACK_NUM = 52
        fun autoDeleteRooms(zone: Zone) {
            ZoneExtension.mutex.lock()
            try {
                zone.roomList?.forEach {
                    if (it.userList.isEmpty()) {
                        (zone.extension as BaseSFSExtension).api.removeRoom(it)
                    }
                }
            } finally {
                ZoneExtension.mutex.unlock()
            }
        }

        fun createRoomAndJoin(user: User, zone: Zone) {
            LogOutput.traceLog(":::::::::::::::::::::Creating new room ${RoomExtension::class.java.name}")
            val roomList = zone.roomList
            var room: Room? = null
            val roomName = getRandomString(6)
            roomList?.forEach loop@{
                if (it.isGame && !it.isFull) {
                    val roomId = it.getVariable("group")
                    if (roomId != null && roomId.stringValue == roomName) {
                        room = it
                        return@loop
                    }
                }
            }
            if (room == null) {
                room = createRoom(roomName, zone)
            }
            val res = SFSObject()
            res.putUtfString("room", room?.name)
            zone.extension.send("auto_join", res, user)
        }


        fun createRoom(roomId: String, zone: Zone): Room? {
            val groupId = "chagdi"
            val roomSettings = CreateRoomSettings().apply {
                isGame = true
                name = "${groupId}_${roomId}}"
                this.groupId = groupId
                isDynamic = true
                maxUsers = 6
                val roomVariables = arrayListOf<RoomVariable>()
                roomVariables.add(SFSRoomVariable("group", roomId))
                autoRemoveMode = SFSRoomRemoveMode.WHEN_EMPTY
                setRoomVariables(roomVariables.toList())
                extension = CreateRoomSettings.RoomExtensionSettings(groupId, RoomExtension::class.java.name)
            }

            return (zone.extension as BaseSFSExtension).api.createRoom(zone, roomSettings, null)
        }
    }


}