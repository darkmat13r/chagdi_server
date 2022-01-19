package extension.room

import com.smartfoxserver.v2.core.ISFSEvent
import com.smartfoxserver.v2.core.SFSEventParam
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.exceptions.SFSException
import com.smartfoxserver.v2.extensions.BaseServerEventHandler

class LeaveHandler : BaseServerEventHandler() {
    @Throws(SFSException::class)
    override fun handleServerEvent(event: ISFSEvent) {
//		System.out.println("Leave");
        val gameExt = parentExtension as RoomExtension
        val user = event.getParameter(SFSEventParam.USER) as User
        if (gameExt.leavePlayer(user.name, 0)) {
            if (gameExt.parentRoom.userList.size == 0) {
                api.removeRoom(gameExt.parentRoom)
            }
        }
    }
}

