package com.pepej.gammanetwork.redirect

import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.pepej.gammanetwork.GammaNetwork.Companion.instance
import com.pepej.gammanetwork.messages.AdminChatMessageSystem
import com.pepej.gammanetwork.messages.CHAT
import com.pepej.gammanetwork.messages.ChatType
import com.pepej.gammanetwork.messages.GlobalChatMessageSystem
import com.pepej.gammanetwork.utils.getPlayer
import com.pepej.gammanetwork.utils.metadata
import com.pepej.gammanetwork.utils.typeTokenOf
import com.pepej.papi.gson.GsonProvider
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.metadata.MetadataKey
import com.pepej.papi.metadata.MetadataMap
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.network.redirect.RedirectSystem.Request
import com.pepej.papi.network.redirect.RedirectSystem.RequestHandler
import com.pepej.papi.promise.Promise
import org.slf4j.LoggerFactory
import java.lang.reflect.Type

object GammaNetworkRequestHandler : RequestHandler {

    private val log = LoggerFactory.getLogger(GammaNetworkRequestHandler::class.java)
    override fun handle(request: Request): Promise<RedirectSystem.Response> {
        log.debug("Handling request for profile {} with params {}", request.profile, request.params)
        val metadata = Metadata.provideForPlayer(request.profile.uniqueId)
        val jsonElement: JsonElement? = request.params[CHAT.id]
        val chatType = GsonProvider.standard().fromJson(jsonElement, ChatType::class.java)
        if (chatType != null) {
            metadata.put(CHAT, chatType)
        }
        return Promise.completed(RedirectSystem.Response(true, "Redirect via network", request.params))

    }




}






