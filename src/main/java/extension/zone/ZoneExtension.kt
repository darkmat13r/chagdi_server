package extension.zone

import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.Zone
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.extensions.SFSExtension
import java.util.concurrent.locks.ReentrantLock

class ZoneExtension : SFSExtension() {
    companion object {
        val mutex = ReentrantLock(true)
    }

    override fun init() {
        addRequestHandler("login", LoginHandler::class.java)
        addRequestHandler("join_room", RoomHandler::class.java)

    }


}