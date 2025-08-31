@file:Suppress("UNCHECKED_CAST")
package com.pepej.gammanetwork.redirect

import com.google.gson.JsonElement
import com.pepej.gammanetwork.metadata.SharedMetadata
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.metadata.MetadataKey
import com.pepej.papi.network.redirect.RedirectParameterProvider
import com.pepej.papi.profiles.Profile

object RedirectNetworkMetadataParameterProvider : RedirectParameterProvider {

    override fun provide(profile: Profile, serverId: String): MutableMap<String, JsonElement> {
        val params = mutableMapOf<String, JsonElement>()
        val meta = Metadata.provideForPlayer(profile.uniqueId)
        for (entry in SharedMetadata.sharedEntries()) {
            val valueOpt = meta.get(entry.keyAny)
            valueOpt.ifPresent { value ->
                params[entry.id] = entry.serializeAny(value)
            }
        }

        return params
    }

    private interface SharedMetadataEntryAny {
        val id: String
        val keyAny: MetadataKey<Any>
        fun serializeAny(value: Any): JsonElement
    }

    private fun SharedMetadata.sharedEntries(): List<SharedMetadataEntryAny> {
        return this.all().map { e ->
            object : SharedMetadataEntryAny {
                override val keyAny: MetadataKey<Any> = (e.key as MetadataKey<Any>)
                override val id: String = e.key.id
                override fun serializeAny(value: Any): JsonElement {
                    val ser = (e.serializer as (Any) -> JsonElement)
                    return ser(value)
                }
            }
        }
    }

}