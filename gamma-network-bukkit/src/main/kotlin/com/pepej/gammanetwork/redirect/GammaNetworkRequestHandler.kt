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
import java.lang.reflect.Type

object GammaNetworkRequestHandler : RequestHandler {
    override fun handle(request: Request): Promise<RedirectSystem.Response> {
        instance.logger.info("Handling request for profile ${request.profile} with params ${request.params}")
        val metadata = Metadata.provideForPlayer(request.profile.uniqueId)
        val jsonElement: JsonElement? = request.params[CHAT.id]
        val chatType = GsonProvider.standard().fromJson(jsonElement, ChatType::class.java)
        if (chatType != null) {
            metadata.put(CHAT, chatType)
        }
        return Promise.completed(RedirectSystem.Response(true, "Redirect via network", request.params))

    }




}






