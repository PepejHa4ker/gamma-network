package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.GammaNetworkPlugin.Companion.instance
import com.pepej.gammanetwork.VERSION
import com.pepej.gammanetwork.utils.getConversationChannel
import com.pepej.papi.messaging.conversation.ConversationMessage
import com.pepej.papi.messaging.conversation.ConversationReply
import com.pepej.papi.messaging.conversation.ConversationReplyListener
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.terminable.TerminableConsumer
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

object NetworkVersionCheckerModule : NetworkModule("VersionChecker")  {

    private val channel = messenger.getConversationChannel<VersionMessage, VersionMessageReply>("version-checker")
    private val log = LoggerFactory.getLogger(javaClass)

    data class VersionMessage(val convoId: UUID = UUID.randomUUID(), val server: String, val version: Int) : ConversationMessage {
        override fun getConversationId(): UUID = convoId
    }

    data class VersionMessageReply(val convoId: UUID, val server: String, val version: Int) : ConversationMessage {
        override fun getConversationId(): UUID = convoId
    }

    override fun onEnable(consumer: TerminableConsumer) {
        val agent = channel.newAgent()

        agent.addListener { _, versionMessage ->
            if (versionMessage.server == instance.id) {
                return@addListener ConversationReply.noReply()
            }
            if (versionMessage.version != VERSION) {
                log.warn("Got VersionChecker request with (version ${versionMessage.version} and server ${versionMessage.server}).")
            }
            ConversationReply.of(VersionMessageReply(convoId = versionMessage.convoId, version = VERSION, server = instance.id))
        }
        agent.bindWith(consumer)

        Schedulers.builder()
           .async()
           .afterAndEvery(15, TimeUnit.MINUTES)
           .run {
               channel.sendMessage(VersionMessage(server = instance.id, version = VERSION),
                   VersionMessageReplyConversationReplyListener(), 5, TimeUnit.SECONDS)
           }
            .bindWith(consumer)

    }

    class VersionMessageReplyConversationReplyListener : ConversationReplyListener<VersionMessageReply> {
        override fun onReply(reply: VersionMessageReply): ConversationReplyListener.RegistrationAction {
            if (reply.version != VERSION) {
                log.warn("VersionChecker found outdated version ${reply.version} server ${reply.server}.!")
            } else {
                log.info("VersionChecker server ${reply.server} is ok!")

            }
            return ConversationReplyListener.RegistrationAction.CONTINUE_LISTENING
        }

        override fun onTimeout(replies: List<VersionMessageReply>) {
            if (replies.isEmpty()) {
                log.warn("No replies provided for VersionChecker request.")
            }
        }

    }
}