package com.pepej.gammanetwork.redirect

import com.google.gson.JsonElement
import com.pepej.gammanetwork.event.player.ProfileRedirectEvent
import com.pepej.gammanetwork.metadata.SharedMetadata
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.gammanetwork.utils.targetServer
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.metadata.MetadataKey
import com.pepej.papi.metadata.MetadataMap
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
        for (entry in sharedMetadataEntries()) {
            val param = request.params[entry.id] ?: continue
            try {
                val value = entry.deserialize(param)
                entry.put(metadata, value)
            } catch (t: Throwable) {
                log.warn("Failed to deserialize shared metadata for key {}: {}", entry.id, t.message)
            }
        }
    }

    private data class SharedEntryOps<T>(
        val id: String,
        val put: (MetadataMap, T) -> Unit,
        val deserialize: (JsonElement) -> T
    )

    @Suppress("UNCHECKED_CAST")
    private fun sharedMetadataEntries(): List<SharedEntryOps<Any>> {
        return SharedMetadata.all().map { e ->
            val key = e.key
            SharedEntryOps(
                id = key.id,
                put = { map, v -> map.put(key as MetadataKey<Any>, v) },
                deserialize = { je -> (e.deserializer as (JsonElement) -> Any).invoke(je) }
            )
        }
    }
}