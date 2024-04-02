package com.pepej.gammanetwork.redirect

import com.google.gson.JsonElement
import com.pepej.gammanetwork.event.player.ProfileRedirectEvent
import com.pepej.gammanetwork.messages.CHAT
import com.pepej.gammanetwork.messages.ChatType
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.gammanetwork.utils.targetServer
import com.pepej.papi.gson.GsonProvider
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.network.Network
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.network.redirect.RedirectSystem.Request
import com.pepej.papi.network.redirect.RedirectSystem.RequestHandler
import com.pepej.papi.promise.Promise
import org.slf4j.LoggerFactory

object GammaNetworkRequestHandler : RequestHandler {

    private val log = LoggerFactory.getLogger(GammaNetworkRequestHandler::class.java)
    private val network: Network = getServiceUnchecked()
    override fun handle(request: Request): Promise<RedirectSystem.Response> {
        log.debug("Handling request for profile {} with params {}", request.profile.name.get(), request.params)
        extractMetadata(request)
        val event = ProfileRedirectEvent(request.targetServer, request.profile, request.params, true)
        network.eventBus.post(event)
        return Promise.completed(RedirectSystem.Response(event.allowed, "Redirect via network", request.params))

    }

    private fun extractMetadata(request: Request) {
        log.debug("Extracting metadata for request {}", request)
        val metadata = Metadata.provideForPlayer(request.profile.uniqueId)
        val jsonElement: JsonElement? = request.params[CHAT.id]
        val chatType = GsonProvider.standard().fromJson(jsonElement, ChatType::class.java)
        if (chatType != null) {
            metadata.put(CHAT, chatType)
        }
    }




}






