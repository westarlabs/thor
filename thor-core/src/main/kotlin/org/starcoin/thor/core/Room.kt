package org.starcoin.thor.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.sign.toByteArray
import org.starcoin.thor.utils.randomString

@Serializable
data class PlayerInfo(@SerialId(1) val playerUserId: String, @SerialId(2) val playerName: String, @SerialId(3) val playerPubKey: ByteArrayWrapper, @SerialId(4) var ready: Boolean = false, @SerialId(5) var rHash: ByteArrayWrapper? = null)

@Serializable
data class Room(@SerialId(1) val roomId: String, @SerialId(2) val gameId: String, @SerialId(3) val name: String, @SerialId(4) var players: MutableList<PlayerInfo>, @SerialId(5) val capacity: Int, @SerialId(6) val cost: Long = 0, @SerialId(7) val timeout: Long = 0, @SerialId(8) var begin: Long = 0) : MsgObject() {

    @kotlinx.serialization.Transient
    val isFull: Boolean
        get() = !this.players.isNullOrEmpty() && this.players.size >= capacity

    @kotlinx.serialization.Transient
    val payment: Boolean
        get() = cost > 0


    constructor(gameId: String, name: String, cost: Long, timeout: Long) : this(randomString(), gameId, name, mutableListOf(), 2, cost, timeout, System.currentTimeMillis()) {
        check(cost >= 0)
    }

    fun addPlayer(userInfo: UserInfo) {
        synchronized(this) {
            check(!isFull)
            if (!this.players.map { playerInfo -> playerInfo.playerUserId }.contains(userInfo.id)) {
                this.players.add(PlayerInfo(userInfo.id, userInfo.name, ByteArrayWrapper(userInfo.publicKey.toByteArray())))
            }
        }
    }

    fun userReady(userId: String) {
        synchronized(this) {
            this.players.filter { playerInfo -> playerInfo.playerUserId == userId }[0].ready = true
        }
    }

    fun rHash(userId: String, rHash: ByteArrayWrapper) {
        synchronized(this) {
            this.players.filter { playerInfo -> playerInfo.playerUserId == userId }[0].rHash = rHash
        }
    }

    fun roomReady(): Boolean {
        var flag = true
        players.forEach { flag = (it.ready && flag) }
        return isFull && flag
    }

    fun deepCopy(): Room {
        return this.copy(players = this.players.map { it.copy() }.toMutableList())
    }

    fun rivalPlayer(currentUserId: String): String? {
        return this.players.firstOrNull { it.playerUserId != currentUserId }?.playerUserId
    }

    fun isInRoom(userId: String): Boolean {
        return this.players.any { it.playerUserId == userId }
    }

}