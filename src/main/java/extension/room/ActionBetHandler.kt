package extension.room

import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler

class ActionBetHandler : BaseClientRequestHandler() {
    override fun handleClientRequest(p0: User?, params: ISFSObject?) {
        val zoneExt = parentExtension as RoomExtension
        p0?.let {
            zoneExt.actionBet( params?.getInt("bet") ?: -1, it.name)
        }
    }


}
