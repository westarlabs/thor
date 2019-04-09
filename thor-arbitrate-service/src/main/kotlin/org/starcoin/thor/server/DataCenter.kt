package org.starcoin.thor.server

import io.ktor.http.cio.websocket.DefaultWebSocketSession
import org.starcoin.lightning.client.HashUtils
import org.starcoin.thor.core.GameInfo
import org.starcoin.thor.proto.Thor
import org.starcoin.thor.utils.encodeToBase58String
import java.util.*

enum class UserStatus {
    UNKNOWN, CONNECTED, CONFIRMED, GAME_ING
}

data class User(val session: DefaultWebSocketSession, var addr: String? = null, var stat: UserStatus = UserStatus.UNKNOWN)

data class PaymentInfo(val addr: String, val r: String, val rHash: String, var received: Boolean = false)

class UserManager {
    private val connections = mutableMapOf<String?, User>()

    fun addUser(user: User): Boolean {
        synchronized(this) {
            if (!connections.containsKey(user.addr)) {
                connections[user.addr] = user
                return true
            }
        }

        return false
    }

    fun addUserEnforce(user: User): Boolean {
        synchronized(this) {
            connections[user.addr] = user
            return true
        }
    }

    fun queryUser(addr: String?): User? {
        return connections[addr]
    }

    fun gameEnd(addrs: Pair<String, String>): Boolean {
        synchronized(this) {
            if (connections.containsKey(addrs.first) && connections[addrs.first]!!.stat == UserStatus.GAME_ING
                    && connections.containsKey(addrs.second) && connections[addrs.second]!!.stat == UserStatus.GAME_ING) {
                connections[addrs.first]!!.stat = UserStatus.CONFIRMED
                connections[addrs.second]!!.stat = UserStatus.CONFIRMED
                return true
            }
        }
        return false
    }

    fun gameBegin(addrs: Pair<String, String>): Boolean {
        synchronized(this) {
            if (connections.containsKey(addrs.first) && connections[addrs.first]!!.stat == UserStatus.CONFIRMED
                    && connections.containsKey(addrs.second) && connections[addrs.second]!!.stat == UserStatus.CONFIRMED) {
                connections[addrs.first]!!.stat = UserStatus.GAME_ING
                connections[addrs.second]!!.stat = UserStatus.GAME_ING
                return true
            }
        }
        return false
    }
}

class PaymentManager {
    private val paymentMap = mutableMapOf<String, Pair<PaymentInfo, PaymentInfo>>()

    fun generatePayments(instanceId: String, fromAddr: String, toAddr: String): Pair<PaymentInfo, PaymentInfo> {
        return when (paymentMap[instanceId]) {
            null -> {
                val first = generatePaymentInfo(fromAddr)
                val second = generatePaymentInfo(toAddr)

                val newPair = Pair(first, second)
                synchronized(this) {
                    paymentMap[instanceId] = newPair
                }
                newPair
            }
            else -> paymentMap[instanceId]!!
        }
    }

    fun changePaymentStatus(addr: String, instanceId: String): Pair<PaymentInfo, PaymentInfo>? {
        return when (paymentMap[instanceId]) {
            null -> {
                null
            }
            else -> {
                val pair = paymentMap[instanceId]!!
                when (addr) {
                    pair.first.addr -> {
                        pair.first.received = true
                    }
                    pair.second.addr -> {
                        pair.second.received = true
                    }
                }
                pair
            }
        }
    }


    fun queryPlayer(surrenderAddr: String, instanceId: String): String? {
        val pair = paymentMap[instanceId]
        return when (surrenderAddr) {
            pair!!.first.addr -> pair.second.addr
            pair.second.addr -> pair.first.addr
            else -> null
        }
    }

    fun surrenderR(surrenderAddr: String, instanceId: String): String? {
        val pair = paymentMap[instanceId]
        return when (surrenderAddr) {
            pair!!.first.addr -> pair.second.r
            pair.second.addr -> pair.first.r
            else -> null
        }
    }

    private fun generatePaymentInfo(addr: String): PaymentInfo {
        val r = randomString()
        val rHash = HashUtils.hash160(r.toByteArray()).encodeToBase58String()
        return PaymentInfo(addr, r, rHash)
    }
}

class GameManager(val adminAddr: String) {
    private val appMap = mutableMapOf<String, GameInfo>()
    private val nameSet = mutableSetOf<String>()
    private val gameHashSet = mutableSetOf<String>()
    private val count: Int = 0
    private val gameLock = java.lang.Object()
    private val instanceLock = java.lang.Object()

    private val gameInstanceMap = mutableMapOf<String, String>()

    fun createGame(game: GameInfo): Boolean {
        var flag = false
        synchronized(gameLock) {
            when (!nameSet.contains(game.name) && !gameHashSet.contains(game.gameHash)) {
                true -> {
                    nameSet.add(game.name)
                    gameHashSet.add(game.gameHash)
                    appMap[game.gameHash] = game
                    count.inc()
                    flag = true
                }
            }
        }

        return flag
    }

    fun count(): Int {
        return this.count
    }

    fun list(begin: Int, end: Int): List<Thor.ProtoGameInfo> {
        var keys = gameHashSet.toList().subList(begin, end).toSet()
        return appMap.filterKeys { keys.contains(it) }.values.map { it.toProto<Thor.ProtoGameInfo>() }
    }

    fun queryGameByHash(hash: String): GameInfo? {
        return appMap[hash]
    }

    fun generateInstance(hash: String): String {
        val id = randomString()

        synchronized(instanceLock) {
            gameInstanceMap[id] = hash
        }

        return id
    }
}

fun randomString(): String {
    return UUID.randomUUID().toString().replace("-", "")
}
