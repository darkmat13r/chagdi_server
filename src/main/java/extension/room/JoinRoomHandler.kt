package extension.room

import com.smartfoxserver.v2.core.ISFSEvent
import com.smartfoxserver.v2.core.SFSEventParam
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.extensions.BaseServerEventHandler


class JoinRoomHandler : BaseServerEventHandler() {
    override fun handleServerEvent(event: ISFSEvent) {
        val roomExtension = parentExtension as RoomExtension
        val user = event.getParameter(SFSEventParam.USER) as User
        if(user.isPlayer){
            roomExtension.join(user.name)
        }else{
            roomExtension.error("Please select a team to join", user?.name ?: "" )
        }
    }
}