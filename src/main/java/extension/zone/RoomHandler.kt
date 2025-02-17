package extension.zone


import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler
import extension.room.RoomExtension
import utils.getRandomString

class RoomHandler : BaseClientRequestHandler() {

    override fun handleClientRequest(p0: User?, p1: ISFSObject?) {
        val zoneExt = parentExtension as ZoneExtension
        p0?.let {
            RoomExtension.createRoomAndJoin( p0, zoneExt.parentZone)
        }
    }
}

