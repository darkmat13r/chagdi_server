package extension.zone


import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler
import data.UserRepository
import exceptions.InsufficientFundsException
import extension.RoomExtension

class RoomHandler : BaseClientRequestHandler() {

    override fun handleClientRequest(p0: User?, p1: ISFSObject?) {
        val zoneExt = parentExtension as ZoneExtension
        p0?.let {
            RoomExtension.createRoomAndJoin(1, p0, zoneExt.parentZone)
        }
    }
}

