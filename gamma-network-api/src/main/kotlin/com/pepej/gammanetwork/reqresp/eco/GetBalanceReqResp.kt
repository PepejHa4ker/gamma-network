package com.pepej.gammanetwork.reqresp.eco

import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.promise.Promise
import java.util.*


object GetBalanceReqResp : EconomyRequester {

    data class GetBalanceRequest(val id: UUID)

    data class GetBalanceResponse(val balance: Double)

    private val messenger = getServiceUnchecked<Messenger>()

    override val channel = messenger.getReqRespChannel("eco-get-balance", GetBalanceRequest::class.java, GetBalanceResponse::class.java)
    override fun requestBalance(id: UUID): Promise<GetBalanceResponse> {
        return channel.request(GetBalanceRequest(id))
    }

}