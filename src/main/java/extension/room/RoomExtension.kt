package extension.room

import com.smartfoxserver.v2.api.CreateRoomSettings
import com.smartfoxserver.v2.entities.Room
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.Zone
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.entities.variables.RoomVariable
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable
import com.smartfoxserver.v2.extensions.BaseSFSExtension
import com.smartfoxserver.v2.extensions.SFSExtension
import engine.Card
import engine.Player
import engine.Table
import extension.zone.ZoneExtension

class RoomExtension : SFSExtension() {

    private lateinit var table : Table

    override fun init() {
        addRequestHandler("join_team", JoinRoomHandler::class.java )
        table = Table(this)
    }

    fun joinTeam(name: String, team: Int) {
        val player = Player(name, name)
        table.joinTeam(player, team)
        if(table.players.size == Table.MAX_PLAYERS && !table.isRunning){
            table.run()
        }
    }

    fun error(error : String, playerToNotify: String){
        send("game_error", SFSObject().apply {
            putUtfString("reason", error)
        }, parentRoom.getUserByName(playerToNotify))
    }

    fun sendPlayerJoined(player: Player) {
        val params = SFSObject()
        params.putUtfString("name", player.getUsername())
        params.putUtfString("team", player.team?.getName())
        params.putInt("team_pos", player.teamPos  ?: -1)
        params.putInt("pos", player.pos  ?: -1)
        send("player_joined", params, parentRoom.userList)
    }

    fun dealCard(card: Card?, player: Player) {
        val otherPlayers = parentRoom.userList.filter { it.name != player.getName() }
        val params = SFSObject()
        params.putInt("pos", player.pos  ?: -1)

        params.putInt("card", card.hashCode())
        send("deal_card", params, parentRoom.getUserByName(player.getUsername()))

        params.putInt("card", CARD_BACK_NUM)
        send("deal_card", params, otherPlayers)
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

        fun createRoomAndJoin(tournamentId: Int, user: User, zone: Zone) {
            val roomList = zone.roomList
            var room: Room? = null
            roomList?.forEach loop@{
                if (it.isGame && !it.isFull) {
                    val roomTournamentId = it.getVariable("group")
                    if (roomTournamentId != null && roomTournamentId.intValue == tournamentId) {
                        room = it
                        return@loop
                    }
                }
            }
            if (room == null) {
                room = createRoom(tournamentId, zone)
            }
            val res = SFSObject()
            res.putUtfString("room", room?.name)
            zone.extension.send("auto_join", res, user)
        }


        private fun createRoom(tournamentId: Int, zone: Zone): Room? {
            val groupId = "chagdi"
            val roomSettings = CreateRoomSettings().apply {
                isGame = true
                name = "${groupId}_${tournamentId}}"
                this.groupId = groupId
                isDynamic = true
                maxUsers = 6
                val roomVariables = arrayListOf<RoomVariable>()
                roomVariables.add(SFSRoomVariable("group", tournamentId))
                autoRemoveMode = SFSRoomRemoveMode.WHEN_EMPTY
                setRoomVariables(roomVariables.toList())
                extension = CreateRoomSettings.RoomExtensionSettings(groupId, RoomExtension::class.java.name)
            }
            return (zone.extension as BaseSFSExtension).api.createRoom(zone, roomSettings, null)
        }
    }


}