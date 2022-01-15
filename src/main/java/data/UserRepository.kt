package data

import com.smartfoxserver.v2.db.IDBManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import exceptions.InsufficientFundsException

class UserRepository(private val db: IDBManager) {

    fun getUserByUsername(email: String): ISFSObject? {
        val connection = db.connection
        try {
            val query = "SELECT * FROM users WHERE username=? LIMIT 1";
            val sql = connection.prepareStatement(query)
            sql.setString(1, email)
            val resultSet = sql.executeQuery()
            val row = SFSArray.newFromResultSet(resultSet)
            connection.close()
            if (row != null && row.size() > 0) {
                return row.getElementAt(0) as ISFSObject
            }

        } catch (ex: Exception) {
            connection.close()
        }
        return null
    }

    fun getUserByBy(email: String): ISFSObject? {
        val connection = db.connection
        try {
            val query = "SELECT * FROM users WHERE id=? LIMIT 1";
            val sql = db.connection.prepareStatement(query)
            sql.setString(1, email)
            val resultSet = sql.executeQuery()
            val row = SFSArray.newFromResultSet(resultSet)
            connection.close()
            if (row != null && row.size() > 0) {
                return row.getElementAt(0) as ISFSObject
            }
            return null
        } catch (ex: Exception) {
            connection.close()
        }
        return null
    }

    fun getCurrentCoins(username: String): Int {
        val user = getUserByUsername(username)
        return user?.getInt("coin") ?: 0
    }

    fun getCurrentGems(username: String): Int {
        val user = getUserByUsername(username)
        return user?.getInt("gem") ?: 0
    }

    fun debitCoin(username: String, amount: Int) {
        val connection = db.connection
        try {
            val currentCoins = getCurrentCoins(username)
            if (currentCoins < amount) {
                throw InsufficientFundsException("Don\'t have sufficient coins")
            }
            val query = "UPDATE users SET coin=? WHERE username=?"
            val sql = connection.prepareStatement(query)
            sql.setInt(1, currentCoins - amount)
            sql.setString(2, username)
            sql.executeUpdate()
            connection.close()
        } catch (ex: Exception) {
            connection.close()
        }
    }

    fun creditCoin(username: String, amount: Int) {
        val connection = db.connection
        try {
            val currentCoins = getCurrentCoins(username)

            val query = "UPDATE users SET coin=? WHERE username=?"
            val sql = connection.prepareStatement(query)
            sql.setInt(1, currentCoins + amount)
            sql.setString(2, username)
            sql.executeUpdate()
            connection.close()
        } catch (ex: Exception) {
            connection.close()
        }

    }

    fun debitGem(username: String, amount: Int) {
        val connection = db.connection
        try {
            val currentGems = getCurrentGems(username)
            if (currentGems < amount) {
                throw InsufficientFundsException("Don\'t have sufficient gems")
            }
            val query = "UPDATE users SET gem=? WHERE username=?"
            val sql = connection.prepareStatement(query)
            sql.setInt(1, currentGems - amount)
            sql.setString(2, username)
            sql.executeUpdate()
            connection.close()
        } catch (ex: Exception) {
            connection.close()
        }
    }

    fun creditGem(username: String, amount: Int) {
        val connection = db.connection
        try {
            val currentCoins = getCurrentGems(username)
            val query = "UPDATE users SET gem=? WHERE username=?"
            val sql = connection.prepareStatement(query)
            sql.setInt(1, currentCoins + amount)
            sql.setString(2, username)
            sql.executeUpdate()
            connection.close()
        } catch (ex: Exception) {
            connection.close()
        }

    }

    fun createUser(params: ISFSObject) {
        val connection = db.connection
        try {
            val query =
                "INSERT INTO users(username, password,token, name, coin, gem, profile_image, type) VALUE (?,?,?,?,?,?,?,?)"
            val sql = connection.prepareStatement(query)
            sql.setString(1, params.getUtfString("username"))
            sql.setString(2, params.getUtfString("password"))
            if (params.containsKey("token")) {
                sql.setString(3, params.getUtfString("token"))
            } else {
                sql.setString(3, null)
            }
            sql.setString(4, params.getUtfString("name"))
            sql.setInt(5, params.getInt("coin"))
            sql.setInt(6, params.getInt("gem"))
            sql.setString(7, params.getUtfString("profile_image"))
            if (params.containsKey("type"))
                sql.setString(8, params.getUtfString("type"))
            else
                sql.setString(8, "guest")
            sql.executeUpdate()
            connection.close()
        } catch (ex: Exception) {
            connection.close()
        }

    }

    fun updateToken(username: String, token: String) {
        val connection =  db.connection
        try {
            val query = "UPDATE users SET token=? WHERE username=?"
            val sql = connection.prepareStatement(query)
            sql.setString(1, token)
            sql.setString(2, username)
            sql.executeUpdate()
            connection.close()
        } catch (ex: Exception) {
            connection.close()
        }
    }

    //jdbc:mysql://localhost:3306/rpc?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC
    //com.mysql.jdbc.Driver
    //SELECT COUNT(*) FROM information_schema.tables

}