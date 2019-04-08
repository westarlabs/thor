package org.starcoin.thor.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.daemon.common.toHexString
import org.starcoin.sirius.serialization.toByteString
import org.starcoin.sirius.util.MockUtils
import org.starcoin.thor.core.LnClient
import org.starcoin.thor.core.LnConfig
import kotlin.random.Random

fun main(args: Array<String>) {
    val hash = MockUtils.nextBytes(20).toByteString()
    val gameClient = GameClientServiceImpl()
    gameClient.start()
    gameClient.registGame(hash)
    val game = gameClient.queryGame(hash.toByteArray())

    val aliceCert = MsgClientServiceImpl::class.java.classLoader.getResourceAsStream("alice.cert")
    val alicConfig = LnConfig(aliceCert, "starcoin-firstbox", 30009)
    val aliceClient = LnClient(alicConfig)
    aliceClient.start()
    alicConfig.addr = aliceClient.syncClient.identityPubkey
    val aliceMsgClient = MsgClientServiceImpl(aliceClient)
    aliceMsgClient.start()

    val bobCert = MsgClientServiceImpl::class.java.classLoader.getResourceAsStream("bob.cert")
    val bobConfig = LnConfig(bobCert, "starcoin-firstbox", 40009)
    val bobClient = LnClient(bobConfig)
    bobClient.start()
    bobConfig.addr = bobClient.syncClient.identityPubkey
    val bobMsgClient = MsgClientServiceImpl(bobClient)
    bobMsgClient.start()

    runBlocking {
        delay(10000)
    }

    aliceMsgClient.doStartAndInviteReq(game.gameHash.bytes.toHexString(), bobConfig.addr!!)
    println("OK")

    runBlocking {
        delay(1000000)
    }

    val flag = Random(2).nextBoolean()
    if (flag)
        bobMsgClient.doSurrenderReq()
    else
        bobMsgClient.doChallenge()

    runBlocking {
        delay(100000)
    }
}
