package com.pepej.gammanetwork.redirect

import com.google.gson.JsonElement
import com.pepej.gammanetwork.messages.CHAT
import com.pepej.papi.gson.GsonProvider
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.metadata.MetadataKey
import com.pepej.papi.network.redirect.RedirectParameterProvider
import com.pepej.papi.profiles.Profile
import java.util.*

object RedirectNetworkMetadataParameterProvider : RedirectParameterProvider {

    override fun provide(profile: Profile, serverId: String): MutableMap<String, JsonElement> {
        val params = mutableMapOf<String, JsonElement>()
        provideForKey(profile.uniqueId, CHAT, params)

        return params
    }

    private fun <T> provideForKey(
        uuid: UUID,
        key: MetadataKey<T>,
        map: MutableMap<String, JsonElement>
    ): MutableMap<String, JsonElement> {
        val metadataMap = Metadata.provideForPlayer(uuid)
        val metadata = metadataMap.get(key)
         metadata
            .map { GsonProvider.standard().toJsonTree(it) }
            .ifPresent { map[key.id] = it }

        return map
    }
}
