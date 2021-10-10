package com.pepej.gammanetwork

import com.pepej.gammanetwork.reqresp.eco.EconomyRequester
import com.pepej.gammanetwork.reqresp.eco.GetBalanceReqResp
import com.pepej.papi.services.Services

class GammaNetworkApi {

    fun init() {
        Services.provide(EconomyRequester::class.java, GetBalanceReqResp)
    }
}