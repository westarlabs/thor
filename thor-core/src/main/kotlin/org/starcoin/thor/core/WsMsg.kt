package org.starcoin.thor.core

import kotlinx.serialization.*
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.sign.SignService
import java.nio.ByteBuffer
import java.security.PrivateKey
import java.security.PublicKey

enum class MsgType {
    NONCE,
    CREATE_ROOM_REQ,
    CREATE_ROOM_RESP,
    JOIN_ROOM_REQ,
    JOIN_ROOM_RESP,
    HASH_REQ,
    HASH_RESP,
    INVOICE_REQ,
    INVOICE_RESP,
    READY_REQ,
    READY_RESP,
    GAME_BEGIN,
    SURRENDER_REQ,
    SURRENDER_RESP,
    CHALLENGE_REQ,
    ROOM_GAME_DATA_MSG,
    ROOM_COMMON_DATA_MSG,
    UNKNOWN;
}

@Serializable
class Nonce(@SerialId(1) val nonce: Long, @SerialId(2) val pubKey: ByteArrayWrapper) : Data()

@Serializable
data class CreateRoomReq(@SerialId(1) val gameHash: String, @SerialId(2) val deposit: Long = 0, @SerialId(3) val time: Long = 0) : Data()

@Serializable
data class CreateRoomResp(@SerialId(1) val room: Room?) : Data()

@Serializable
data class JoinRoomReq(@SerialId(1) val roomId: String) : Data()

@Serializable
data class JoinRoomResp(@SerialId(1) val succ: Boolean, @SerialId(2) val room: Room? = null) : Data()

@Serializable
data class HashReq(@SerialId(1) val roomId: String, @SerialId(2) val rhash: String, @SerialId(3) val cost: Long) : Data()

@Serializable
data class HashResp(@SerialId(1) val roomId: String, @SerialId(2) val paymentRequest: String) : Data()

@Serializable
data class InvoiceReq(@SerialId(1) val roomId: String, @SerialId(2) val paymentRequest: String, val value: Long) : Data()

@Serializable
class InvoiceResp : Data()

@Serializable
data class ReadyReq(@SerialId(1) val roomId: String) : Data()

@Serializable
class ReadyResp : Data()

@Serializable
data class BeginMsg(@SerialId(1) val room: Room, @SerialId(2) var timestamp: Long, @SerialId(3) var keys: List<ByteArrayWrapper>? = null) : Data()

@Serializable
data class SurrenderReq(@SerialId(1) val roomId: String) : Data()

@Serializable
data class SurrenderResp(@SerialId(1) val r: String) : Data()

@Serializable
data class ChallengeReq(@SerialId(1) val roomId: String) : Data()

@Serializable
data class RoomGameData(@SerialId(1) val to: String, @SerialId(2) val witness: WitnessData) : Data()

@Serializable
data class CommonRoomData(@SerialId(1) val to: String, @SerialId(2) val data: String) : Data()

@Serializable
data class WitnessData(@SerialId(1) var preSign: String, @SerialId(2) val data: ByteArrayWrapper, @SerialId(3) var timestamp: Long? = null, @SerialId(4) var arbiterSign: String? = null, @SerialId(5) var sign: String? = null) {

    fun doSign(privateKey: PrivateKey) {
        sign = SignService.sign(data.bytes, "", privateKey)
    }

    fun doArbiterSign(privateKey: PrivateKey) {
        timestamp = System.currentTimeMillis()
        val signData = data.bytes + longToBytes(timestamp!!)
        arbiterSign = SignService.sign(signData, "", privateKey)
    }

    fun checkArbiterSign(publicKey: PublicKey): Boolean {
        val signData = data.bytes + longToBytes(timestamp!!)
        return SignService.verifySign(signData, arbiterSign!!, publicKey)
    }

    fun checkSign(publicKey: PublicKey): Boolean {
        if (sign != null) {
            return SignService.verifySign(data.bytes, sign!!, publicKey)
        }
        return false
    }
}

@Serializable
data class WsMsg(@SerialId(1) val type: MsgType, @SerialId(2) var userId: String, @SerialId(3) val data: Data) : MsgObject()

@Serializable
data class SignMsg(@SerialId(1) val msg: WsMsg, @SerialId(2) val sign: String) : MsgObject()

fun longToBytes(l: Long): ByteArray {
    val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
    buffer.putLong(l)
    return buffer.array()
}