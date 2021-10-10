package com.pepej.gammanetwork

import com.pepej.gammanetwork.reqresp.eco.EconomyRequester
import com.pepej.gammanetwork.reqresp.eco.GetBalanceReqResp
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.ap.Plugin
import com.pepej.papi.ap.PluginDependency
import com.pepej.papi.plugin.PapiJavaPlugin
import com.pepej.papi.promise.Promise

@Plugin(
    name = "gamma-network-hub",
    version = "1.0.0",
    authors = ["pepej"],
    depends = [
        PluginDependency("papi"),
        PluginDependency("gammanetwork"),
    ]
)
class GammaNetworkHub : PapiJavaPlugin() {

    override fun onPluginEnable() {
        val requester = getServiceUnchecked<EconomyRequester>()
        requester.channel.asyncResponseHandler {
            Promise.completed(GetBalanceReqResp.GetBalanceResponse(100.0))


        }
    }
}