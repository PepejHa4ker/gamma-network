package com.pepej.gammanetwork.reqresp.eco

import com.pepej.papi.messaging.reqresp.ReqRespChannel
import com.pepej.papi.promise.Promise
import java.util.*

interface EconomyRequester {

    val channel: ReqRespChannel<GetBalanceReqResp.GetBalanceRequest, GetBalanceReqResp.GetBalanceResponse>

    fun requestBalance(id: UUID): Promise<GetBalanceReqResp.GetBalanceResponse>

}



