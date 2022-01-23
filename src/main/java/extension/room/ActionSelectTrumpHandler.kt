package extension.room

import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler
import engine.LogOutput

class ActionSelectTrumpHandler : BaseClientRequestHandler() {
    override fun handleClientRequest(p0: User?, params: ISFSObject?) {
        LogOutput.traceLog("Handlec ${ActionSelectTrumpHandler::class.java.simpleName} ${p0?.name} - trump ${params?.getInt("trump")}")
        val zoneExt = parentExtension as RoomExtension
        p0?.let {
            zoneExt.actionSetTrump( params?.getInt("trump") ?: -1, it.name)
        }
    }

}
