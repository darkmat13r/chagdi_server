package extension.room

import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler

class JoinRoomHandler : BaseClientRequestHandler() {
    override fun handleClientRequest(user: User?, params: ISFSObject?) {
        val roomExtension = parentExtension as RoomExtension
        if(user?.isPlayer == true && params?.containsKey("team") == true){
            roomExtension.joinTeam(user.name, params.getInt("team") )
        }else{
            roomExtension.error("Please select a team to join", user?.name ?: "" )
        }
    }
}