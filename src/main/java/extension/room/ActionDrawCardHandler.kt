package extension.room

import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler

class ActionDrawCardHandler : BaseClientRequestHandler() {
    override fun handleClientRequest(p0: User?, params: ISFSObject?) {
        val zoneExt = parentExtension as RoomExtension
        p0?.let {
            zoneExt.actionDrawCard( params?.getInt("card") ?: -1, it.name)
        }
    }
}