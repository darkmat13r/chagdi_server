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
import kotlin.math.min

class RoomExtension : SFSExtension(), Client {

    private lateinit var table: Table
    private lateinit var roomName: String
    private val names = arrayListOf("Blabbermouth",
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
        send("update_players", SFSObject().apply {
            val playerInfos = SFSArray()
            table.players.forEach {
                LogOutput.traceLog(it.toSFSObject()?.dump)
                playerInfos.addSFSObject(it.toSFSObject())
            }
            putSFSArray("player_infos", playerInfos)
        }, parentRoom.userList)
    }

    fun dealCard(card: Card?, player: Player) {
        val otherPlayers = getOtherUsers(player)
        val params = SFSObject()
        params.putInt("pos", player.pos ?: -1)
        params.putInt("actor_pos", table.dealerPos)
        params.putUtfString("username", player.getUsername())
        params.putInt("card", card.hashCode())
        if(!player.isBot){
            send("deal_card", params, parentRoom.getUserByName(player.getUsername()))
        }
        params.putInt("card", CARD_BACK_NUM)
        send("deal_card", params, otherPlayers)
    }

    private fun getOtherUsers(player: Player?) = parentRoom.userList.filter { it.name != player?.getName() }

    fun join(name: String) {
        val player = Player(name, name, this)
        table.addPlayer(player)
        if(table.players.size == 2){
            addBots()
        }
        send("join_team", SFSObject(), parentRoom.getUserByName(name))
    }

    private fun addBots() {
        for (i in 0 until 2) {
            val rd = Random()
            var newName = names[rd.nextInt(names.size)].lowercase().replace(" ", "_")
            if (table.getPlayerByUsername(newName)  != null){
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
            if (table.getPlayerByUsername(newName)  != null){
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
            putUtfString("username", player.getUsername())
        }, parentRoom.getUserByName(player.getUsername()))
    }

    private fun getMinBet(player: Player): Int {
        val getMaxTarget = table.players.maxOf { it.bet ?: -1 }
        var nextPlayerPos = table.dealerPos + 1
        if (nextPlayerPos >= Table.MAX_PLAYERS) {
            nextPlayerPos = 0
        }
        var minTarget = Table.MIN_BET_TARGET_FIRST_PLAYER
        if (player.pos != nextPlayerPos) {
            minTarget = Table.MIN_BET_TARGET_OTHER
        }
        if (getMaxTarget == -1) {
            minTarget = getMaxTarget
        }
        return min(0, minTarget)
    }


    fun actionBet(bet: Int, name: String) {
        table.getPlayerByUsername(name)?.apply {
            val minBet = getMinBet(this)
            LogOutput.traceLog("No Player found with ${minBet} ${bet} ${bet in minBet until 17}")
            if(bet in minBet until 17){
                this.bet = bet
            }else{
                askToBet(this)
                error("Not allow to bet this amount", name)
            }
          updatePlayers()
        }?:run{
            LogOutput.traceLog("No Player found with ${name} ${bet}")
        }
    }

    fun setPlayerActive(actorPos: Int) {
        send("set_actor", SFSObject().apply {
            putInt("pos", actorPos)
        }, parentRoom.userList)
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