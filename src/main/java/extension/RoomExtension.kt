package extension

import com.smartfoxserver.v2.api.CreateRoomSettings
import com.smartfoxserver.v2.entities.Room
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.Zone
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.entities.variables.RoomVariable
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable
import com.smartfoxserver.v2.extensions.BaseSFSExtension
import extension.zone.ZoneExtension
import kotlin.random.Random

class RoomExtension {


    companion object {
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