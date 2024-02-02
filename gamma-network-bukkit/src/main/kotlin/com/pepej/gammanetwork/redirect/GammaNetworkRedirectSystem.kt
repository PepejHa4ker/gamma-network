package com.pepej.gammanetwork.redirect

import com.google.common.collect.ImmutableMap
import com.google.gson.JsonElement
import com.pepej.gammanetwork.GammaNetwork.Companion.instance
import com.pepej.papi.events.Events
import com.pepej.papi.messaging.InstanceData
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.messaging.conversation.*
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
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class GammaNetworkRedirectSystem(
    messenger: Messenger,
    private val instanceData: InstanceData,
    private val redirector: PlayerRedirector
) :
    RedirectSystem {
    private val channel =
        messenger.getConversationChannel("network-redirect", RequestMessage::class.java, ResponseMessage::class.java)


    private val agent = channel.newAgent().apply {
        addListener { _, message ->
            if (!message.targetServer.equals(instanceData.id, true)) {
                return@addListener ConversationReply.noReply()
            }
            instance.logger.info("Received a message $message")
            // call the handler
            val response = handler.handle(message)

            // process the redirect
            response.thenAcceptAsync { resp ->
                if (!resp.isAllowed) {
                    instance.logger.info("Not allowed")
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
                instance.logger.info("Sending response $resp")
                resp
            })
        }
    }

    private val expectedPlayers = ExpiringMap.builder()
        .expiration(5, TimeUnit.SECONDS)
        .expirationPolicy(ExpirationPolicy.CREATED)
        .build<UUID, RedirectSystem.Response>()

    private var ensureJoinedViaQueue = true
    private val loginEventListener = Events.subscribe(AsyncPlayerPreLoginEvent::class.java)
        .filter { this.ensureJoinedViaQueue && !this.instanceData.id.equals("Hub", true) }
        .handler {
            val response = expectedPlayers.remove(it.uniqueId)
            if (response == null || !response.isAllowed) {
                instance.logger.info("Unable to process")
                it.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    colorize("&cSorry! The server is unable to process your login at this time. (queue error)")
                )
            }
            instance.logger.info("Player ${it.uniqueId} joined via queue with params ${response?.params}")
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
                instance.logger.info("Putting value $value for key $key")
                req.params.putIfAbsent(key, value)
            }
        }
        instance.logger.info("Builded a request parameters: ${req.params}")

        val promise = Promise.empty<RedirectSystem.ReceivedResponse>()

        instance.logger.info("Sending request $req")
        // send req and await reply.
        channel.sendMessage(req, object : ConversationReplyListener<ResponseMessage> {
            override fun onReply(reply: ResponseMessage): ConversationReplyListener.RegistrationAction {
                instance.logger.info("Got reply $reply")
                if (reply.allowed) {
                    redirector.redirectPlayer(serverId, profile)
                }
                promise.supply(reply)
                return ConversationReplyListener.RegistrationAction.STOP_LISTENING
            }

            override fun onTimeout(replies: MutableList<ResponseMessage>) {
                promise.supply(MissingResponse.INSTANCE)
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

    private class MissingResponse : RedirectSystem.ReceivedResponse {
        override fun getStatus(): RedirectSystem.ReceivedResponse.Status {
            return RedirectSystem.ReceivedResponse.Status.NO_REPLY
        }

        override fun getReason(): Optional<String> {
            return Optional.empty()
        }

        override fun getParams(): Map<String, JsonElement> {
            return mutableMapOf()
        }

        companion object {
            val INSTANCE: MissingResponse = MissingResponse()
        }
    }

    private class AllowAllHandler : RequestHandler {
        override fun handle(request: RedirectSystem.Request): Promise<RedirectSystem.Response> {
            return Promise.completed(RedirectSystem.Response.allow())
        }
    }
}
