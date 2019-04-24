package org.starcoin.thor.manager

import com.google.common.collect.HashBiMap
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import org.starcoin.thor.core.UserInfo
import org.starcoin.thor.core.UserStatus
import org.starcoin.thor.utils.randomString

data class CommonUser(val userInfo: UserInfo, var stat: UserStatus = UserStatus.NORMAL, var currentRoomId: String? = null)

//all sessionId
class SessionManager {

    private val sessions = mutableMapOf<String, DefaultWebSocketSession>()//SessionId -> Socket
    private val sessionLock = java.lang.Object()

    private val nonces = mutableMapOf<String, String>()//SessionId -> nonce
    private val nonceLock = java.lang.Object()

    private val users = HashBiMap.create<String, String>()//SessionId -> UserId

    fun storeSocket(sessionId: String, session: DefaultWebSocketSession) {
        synchronized(sessionLock) {
            sessions[sessionId] = session
        }
    }

    fun querySocketBySessionId(sessionId: String): DefaultWebSocketSession? {
        return sessions[sessionId]
    }

    fun querySocketByUserId(userId: String): DefaultWebSocketSession? {
        val sessionId = querySessionIdByUserId(userId)
        return sessionId?.let { sessions[sessionId] }
    }

    fun createNonce(sessionId: String): String {
        val nonce = randomString()
        synchronized(nonceLock) {
            nonces[sessionId] = nonce
        }
        return nonce
    }

    fun queryNonce(sessionId: String): String? {
        return nonces[sessionId]
    }

    fun storeUserId(sessionId: String, userId: String) {
        synchronized(this) {
            nonces.remove(sessionId)
            if (users.contains(sessionId)) {
                users.remove(sessionId)
            }
            users[sessionId] = userId
        }
    }

    fun queryUserIdBySessionId(sessionId: String): String? {
        return users[sessionId]
    }

    fun querySessionIdByUserId(userId: String): String? {
        return users.inverse()[userId]
    }

    fun validUser(sessionId: String): Boolean {
        val userId = queryUserIdBySessionId(sessionId)
        return when (userId) {
            null -> false
            else -> true
        }
    }

    fun clearSession(sessionId: String) {
        synchronized(this) {
            sessions.remove(sessionId)
            nonces.remove(sessionId)
            users.remove(sessionId)
        }
    }
}

//all userId
class CommonUserManager {
    private val users = mutableMapOf<String, CommonUser>()//userId -> CommonUser
    private val userLock = java.lang.Object()

    fun storeUser(userInfo: UserInfo) {
        synchronized(userLock) {
            if (!users.containsKey(userInfo.id)) {
                users[userInfo.id] = CommonUser(userInfo)
            }
        }
    }

    fun queryUser(userId: String): UserInfo? {
        return users[userId]?.let { users[userId]!!.userInfo }
    }

    fun queryDetailUser(userId: String): CommonUser? {
        return users[userId]
    }

    fun queryCurrentRoom(userId: String): String? {
        return users[userId]?.let { users[userId]!!.currentRoomId }
    }

    fun setCurrentRoom(userId: String, roomId: String) {
        synchronized(userLock) {
            users[userId]?.let {
                users[userId]!!.currentRoomId = roomId
                users[userId]!!.stat = UserStatus.ROOM
            }
        }
    }

    fun gameBegin(addrs: Pair<String, String>) {
        synchronized(userLock) {
            if (users[addrs.first]!!.stat == UserStatus.ROOM && users[addrs.second]!!.stat == UserStatus.ROOM) {
                users[addrs.first]!!.stat = UserStatus.PLAYING
                users[addrs.first]!!.stat = UserStatus.PLAYING
            }
        }
    }

    fun clearRoom(userId: String) {
        synchronized(userLock) {
            users[userId]?.let {
                users[userId]!!.currentRoomId = null
                users[userId]!!.stat = UserStatus.NORMAL
            }
        }
    }
}
