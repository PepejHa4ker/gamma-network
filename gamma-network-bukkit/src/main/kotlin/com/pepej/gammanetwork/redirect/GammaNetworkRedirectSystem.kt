package com.pepej.gammanetwork.redirect

import com.google.gson.JsonElement
import com.pepej.gammanetwork.GammaNetwork.Companion.instance
import com.pepej.papi.events.Events
import com.pepej.papi.messaging.InstanceData
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.messaging.conversation.ConversationMessage
import com.pepej.papi.messaging.conversation.ConversationReply
import com.pepej.papi.messaging.conversation.ConversationReplyListener
import com.pepej.papi.network.redirect.PlayerRedirector
import com.pepej.papi.network.redirect.RedirectParameterProvider
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.network.redirect.RedirectSystem.RequestHandler
import com.pepej.papi.profiles.Profile
import com.pepej.papi.promise.Promise
import com.pepej.papi.text.Text.colorize
import net.jodah.expiringmap.ExpirationPolicy
import net.jodah.expiringmap.ExpiringMap
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class GammaNetworkRedirectSystem(
    messenger: Messenger,
    private val instanceData: InstanceData,
    private val redirector: PlayerRedirector,
    private var connectionTimeout: Long = 5,
) : RedirectSystem {
    private val channel = messenger.getConversationChannel("network-redirect", RequestMessage::class.java, ResponseMessage::class.java)
    private val log = LoggerFactory.getLogger(GammaNetworkRedirectSystem::class.java)


    private val agent = channel.newAgent().apply {
        addListener { _, message ->
            if (!message.targetServer.equals(instanceData.id, true)) {
                return@addListener ConversationReply.noReply()
            }
            log.debug("Received a message {}", message)
            // call the handler
            val response = handler.handle(message)

            // process the redirect
            response.thenAcceptAsync { resp ->
                if (!resp.isAllowed) {
                    log.debug("Not allowed")
                    return@thenAcceptAsync
                }
                // add player to the expected players queue
                expectedPlayers[message.uuid] = resp

            }
            ConversationReply.ofPromise(response.thenApplyAsync {
                val resp = ResponseMessage(
                    message.convoId,
                    it.isAllowed,
                    it.reason ?: "",
                    it.params
                )
                log.debug("Sending response {}", resp)
                resp
            })
        }
    }

    private val expectedPlayers = ExpiringMap.builder()
        .expiration(connectionTimeout, TimeUnit.SECONDS)
        .expirationPolicy(ExpirationPolicy.CREATED)
        .build<UUID, RedirectSystem.Response>()

    private var ensureJoinedViaQueue = true
    private val loginEventListener = Events.subscribe(AsyncPlayerPreLoginEvent::class.java)
        .filter { this.ensureJoinedViaQueue && !this.instanceData.id.equals("Hub", true) }
        .handler {
            val response = expectedPlayers.remove(it.uniqueId)
            if (response == null || !response.isAllowed) {
                log.error("Unable to process player {} connection. Possible hacking attempt", it.name)
                log.debug("Player {} is not allowed to connect with response: {}", it.name, response)
                it.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    colorize("&cSorry! The server is unable to process your login at this time. (queue error)")
                )
            }
            log.info("Player {} joined via queue with params {}", it.name, response?.params)
        }

    private var handler: RequestHandler = AllowAllHandler()
    private val defaultParameters = CopyOnWriteArrayList<RedirectParameterProvider>()

    override fun redirectPlayer(
        serverId: String,
        profile: Profile,
        params: MutableMap<String, JsonElement>
    ): Promise<RedirectSystem.ReceivedResponse> {
        val req = RequestMessage(
            UUID.randomUUID(),
            serverId,
            profile.uniqueId,
            profile.name.orElse(null),
            params
        )

        for (defaultProvider in this.defaultParameters) {
            for ((key, value) in defaultProvider.provide(profile, serverId)) {
                log.debug("Putting value {} for key {}", value, key)
                req.params.putIfAbsent(key, value)
            }
        }
        log.debug("Built a request parameters: {}", req.params)

        val promise = Promise.empty<RedirectSystem.ReceivedResponse>()

        log.debug("Sending request {}", req)
        // send req and await reply.
        channel.sendMessage(req, object : ConversationReplyListener<ResponseMessage> {
            override fun onReply(reply: ResponseMessage): ConversationReplyListener.RegistrationAction {
                log.debug("Got reply {}", reply)
                if (reply.allowed) {
                    redirector.redirectPlayer(serverId, profile)
                }
                promise.supply(reply)
                return ConversationReplyListener.RegistrationAction.STOP_LISTENING
            }

            override fun onTimeout(replies: MutableList<ResponseMessage>) {
                promise.supply(MissingResponse)
            }
        }, 5, TimeUnit.SECONDS)

        return promise
    }

    override fun setHandler(handler: RequestHandler) {
        this.handler = Objects.requireNonNull(handler, "handler")
    }

    override fun addDefaultParameterProvider(provider: RedirectParameterProvider) {
        defaultParameters.add(provider)
    }

    override fun setEnsure(ensureJoinedViaQueue: Boolean) {
        this.ensureJoinedViaQueue = ensureJoinedViaQueue
    }

    override fun getExpectedConnectionsCount(): Int {
        return expectedPlayers.size
    }

    override fun close() {
        agent.close()
        loginEventListener.close()
    }

    private data class RequestMessage(
        val convoId: UUID,
        val targetServer: String,
        val uuid: UUID,
        val username: String,
        private val params: MutableMap<String, JsonElement>
    ) : ConversationMessage, RedirectSystem.Request {

        override fun getConversationId() = this.convoId

        override fun getProfile() = Profile.create(uuid, this.username)

        override fun getParams(): MutableMap<String, JsonElement> = this.params
    }

    private data class ResponseMessage(
        val convoId: UUID,
        val allowed: Boolean,
        val reason: String,
        private val params: MutableMap<String, JsonElement>
    ) : ConversationMessage, RedirectSystem.ReceivedResponse {

        override fun getStatus(): RedirectSystem.ReceivedResponse.Status {
            return if (this.allowed) RedirectSystem.ReceivedResponse.Status.ALLOWED else RedirectSystem.ReceivedResponse.Status.DENIED
        }

        override fun getReason(): Optional<String> = Optional.ofNullable(this.reason)
        override fun getParams(): MutableMap<String, JsonElement> = this.params
        override fun getConversationId(): UUID = this.convoId
    }

    private object MissingResponse : RedirectSystem.ReceivedResponse {
        override fun getStatus(): RedirectSystem.ReceivedResponse.Status {
            return RedirectSystem.ReceivedResponse.Status.NO_REPLY
        }

        override fun getReason(): Optional<String> {
            return Optional.empty()
        }

        override fun getParams(): Map<String, JsonElement> {
            return mutableMapOf()
        }

    }

    private class AllowAllHandler : RequestHandler {
        override fun handle(request: RedirectSystem.Request): Promise<RedirectSystem.Response> {
            return Promise.completed(RedirectSystem.Response.allow())
        }
    }
}
