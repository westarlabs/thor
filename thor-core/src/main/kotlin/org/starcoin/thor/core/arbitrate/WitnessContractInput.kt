package org.starcoin.thor.core.arbitrate

import org.starcoin.thor.core.WitnessData
import java.security.PublicKey

data class WitnessContractInput(val userList: List<String>, val userId: String, val publicKeys: Triple<PublicKey, PublicKey, PublicKey>, val data: List<WitnessData>) : ContractInput {
    private var current = 0
    private val size = data.size
    private var flag = true

    override fun getUser(): String {
        return this.userId
    }

    override fun hasNext(): Boolean {
        //TODO("verify sign")
        return synchronized(this) {
            current < size && flag
        }
    }

    override fun reset() {
        synchronized(this) {
            current = 0
        }
    }

    override fun next(): ArbitrateData {

        synchronized(this) {
            val arbitrateData = ArbitrateDataImpl(userList, data[current])
            current = ++current
            return arbitrateData
        }
    }
}