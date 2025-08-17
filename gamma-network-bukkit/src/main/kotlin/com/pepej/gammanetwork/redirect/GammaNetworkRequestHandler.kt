package com.pepej.gammanetwork.redirect

import com.google.gson.JsonElement
import com.pepej.gammanetwork.event.player.ProfileRedirectEvent
import com.pepej.gammanetwork.messages.CHAT
import com.pepej.gammanetwork.messages.ChatType
import com.pepej.gammanetwork.utils.REDIRECT_TOKEN
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.gammanetwork.utils.targetServer
import com.pepej.papi.gson.GsonProvider
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.network.Network
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.network.redirect.RedirectSystem.Request
import com.pepej.papi.network.redirect.RedirectSystem.RequestHandler
import com.pepej.papi.promise.Promise
import com.pepej.papi.scheduler.Schedulers
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

object GammaNetworkRequestHandler : RequestHandler {

    private val log = LoggerFactory.getLogger(GammaNetworkRequestHandler::class.java)
    private val network: Network = getServiceUnchecked()
    private val messenger: Messenger = getServiceUnchecked()

    private val redirectChannel = messenger.getChannel("redirect-left", RedirectLeftAck::class.java)

    data class RedirectLeftAck(
        val profileId: UUID,
        val redirectToken: String
    )

    private val ackTimeout: Duration = Duration.ofSeconds(5)

    override fun handle(request: Request): Promise<RedirectSystem.Response> {
        log.debug("Handling request for profile {} with params {}", request.profile.name.get(), request.params)

        val token = (request.params[REDIRECT_TOKEN] as? String)?.takeIf { it.isNotBlank() }
        if (token == null) {
            extractMetadata(request)
            val event = ProfileRedirectEvent(request.targetServer, request.profile, request.params, true)
            network.eventBus.post(event)
            return Promise.completed(RedirectSystem.Response(event.allowed, "Redirect (no token)", request.params))
        }

        val promise: Promise<RedirectSystem.Response> = Promise.empty()

        val subscription = redirectChannel.newAgent { _, msg ->
            if (promise.isDone) return@newAgent
            if (msg.profileId == request.profile.uniqueId && msg.redirectToken == token) {
                try {
                    extractMetadata(request)
                    val event = ProfileRedirectEvent(request.targetServer, request.profile, request.params, true)
                    network.eventBus.post(event)
                    promise.supply(RedirectSystem.Response(event.allowed, "Redirect (ack received)", request.params))
                } catch (t: Throwable) {
                    promise.supplyException(t)
                }
            }
        }

        Schedulers.async().runLater({
            if (!promise.isDone) {
                log.warn("Redirect ACK not received on time for {}. Proceeding.", request.profile.uniqueId)
                try {
                    extractMetadata(request)
                    val event = ProfileRedirectEvent(request.targetServer, request.profile, request.params, true)
                    network.eventBus.post(event)
                    promise.supply(RedirectSystem.Response(event.allowed, "Redirect (ack timeout)", request.params))
                } catch (t: Throwable) {
                    promise.supplyException(t)
                }
            }
        }, ackTimeout.toMillis(), TimeUnit.MILLISECONDS)

        promise.toCompletableFuture().whenComplete { _, _ ->
            try {
                subscription.close()
            } catch (_: Exception) {}
        }

        return promise
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