package extension.zone

import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler
import data.UserRepository
import java.security.SecureRandom


class LoginHandler  : BaseClientRequestHandler(){
    override fun handleClientRequest(user: User, params: ISFSObject) {
        val userRepo = UserRepository(parentExtension.parentZone.dbManager)
        if(params.containsKey("username") && !params.containsKey("token")){
            loginWithUsernameAndPassword(params, userRepo, user)
        }else if(params.containsKey("username") && params.containsKey("token")){
            loginWithToken(params, userRepo, user)
        }
    }

    private fun loginWithToken(params: ISFSObject, userRepo: UserRepository, user: User) {
        val email = params.getUtfString("username")!!
        val token = params.getUtfString("token")!!
        val registeredUser = userRepo.getUserByUsername(email)
        trace("Login via token ${params.dump}")
        if(registeredUser != null && registeredUser.getUtfString("token") == token){
            val response = SFSObject()
            response.putBool("success", true)
            response.putSFSObject("info", registeredUser)
            send("login", response, user)
        }
    }

    private fun loginWithUsernameAndPassword(
        params: ISFSObject,
        userRepo: UserRepository,
        user: User
    ) {
        trace("Login via username and password ${params.dump}")
        val email = params.getUtfString("username")!!
        val registeredUser = userRepo.getUserByUsername(email)
        val response = SFSObject()
        val type = params.getUtfString("type")
        if (registeredUser == null) {
            if (type != null && type == "facebook") {
                createFacebookUser(params, userRepo)
            } else {
                createGuestUser(params, userRepo)
            }
        }else{
            if(registeredUser.getUtfString("token").isNullOrEmpty()){
                userRepo.updateToken(email, createToken())
            }
        }
        response.putBool("success", true)
        response.putSFSObject("info", userRepo.getUserByUsername(email))
        trace("Send Login response Login create fb user ${registeredUser?.dump}")
        send("login", response, user)
    }

    private fun createGuestUser(params: ISFSObject, userRepo: UserRepository) {

        val data = SFSObject().apply {
            putUtfString("name", params.getUtfString("name"))
            putUtfString("username", params.getUtfString("username"))
            putUtfString("password", params.getUtfString("password"))
            putUtfString("profile_image", "")
            putUtfString("token", createToken())
            putInt("coin", 20)
            putInt("gem", 2)
        }
        userRepo.createUser(data)
    }

    private fun createToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        return bytes.toString()
    }

    private fun createFacebookUser(params: ISFSObject, userRepo: UserRepository) {
        val data = SFSObject().apply {
            putUtfString("name", params.getUtfString("name"))
            putUtfString("username", params.getUtfString("username"))
            putUtfString("password", "")
            putUtfString("token", params.getUtfString("fb_token"))
            putUtfString("type", "facebook")
            putUtfString("profile_image", "")
            putInt("coin", 25)
            putInt("gem", 5)
        }
        trace("Login create fb user")
        userRepo.createUser(data)
        trace("Couldnt Login create fb user")
    }

}