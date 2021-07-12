package com.pepej.gammachat.messages

import com.pepej.papi.messaging.conversation.ConversationChannel
import com.pepej.gammachat.messages.PrivateMessageSystem.PrivateMessage
import com.pepej.gammachat.messages.PrivateMessageSystem.PrivateMessageReply
import com.pepej.papi.messaging.Messenger
import org.bukkit.entity.Player
import com.pepej.papi.messaging.conversation.ConversationReplyListener
import com.pepej.papi.messaging.conversation.ConversationReplyListener.RegistrationAction
import com.pepej.papi.messaging.conversation.ConversationMessage
import com.pepej.papi.messaging.conversation.ConversationChannelAgent
import com.pepej.papi.messaging.conversation.ConversationChannelListener
import com.pepej.papi.scheduler.Schedulers
import org.bukkit.Bukkit
import com.pepej.papi.messaging.conversation.ConversationReply
import com.pepej.papi.text.Text
import com.pepej.papi.text.Text.colorize
import java.util.*
import java.util.concurrent.TimeUnit

class PrivateMessageSystem(messenger: Messenger) {
    private val channel: ConversationChannel<PrivateMessage, PrivateMessageReply>
    fun sendMessage(from: Player, to: String, message: String) {

        val pm = PrivateMessage(from.name, message, to)
        channel.sendMessage(pm, object : ConversationReplyListener<PrivateMessageReply> {
            override fun onReply(reply: PrivateMessageReply): RegistrationAction {
                val player = Bukkit.getPlayerExact(reply.deliveredTo)
                val name = player?.displayName ?: "&a" + reply.deliveredTo
                from.sendMessage(String.format(colorize("&7[&eВы&7 -> %s]&f: ") + "%s", name,
                    message))
                return RegistrationAction.STOP_LISTENING
            }

            override fun onTimeout(replies: List<PrivateMessageReply>) {
                from.sendMessage(colorize("&cИгрок не найден"))
            }
        }, 2, TimeUnit.SECONDS)
    }

    private class PrivateMessage(from: String, message: String, to: String) : ConversationMessage {
        private val conversationId: UUID
        val from: String
        val message: String
        val to: String
        override fun getConversationId(): UUID {
            return conversationId
        }

        init {
            conversationId = UUID.randomUUID()
            this.from = from
            this.message = message
            this.to = to
        }
    }

    private class PrivateMessageReply(
        private val conversationId: UUID,
        val deliveredTo: String
    ) : ConversationMessage {
        override fun getConversationId(): UUID {
            return conversationId
        }
    }

    init {
        channel = messenger.getConversationChannel("private-messages",
            PrivateMessage::class.java,
            PrivateMessageReply::class.java)
        val channelAgent = channel.newAgent()
        channelAgent.addListener { agent: ConversationChannelAgent<PrivateMessage, PrivateMessageReply>?, message: PrivateMessage ->
            val reply = Schedulers.sync().supply {
                val player = Bukkit.getPlayer(message.to) ?: return@supply null
                player.sendMessage(String.format(colorize("&7[&a%s&7 -> &eВам]&f: ") + "%s",
                    player.displayName,
                    message.message))
                PrivateMessageReply(message.conversationId, player.name)
            }
            ConversationReply.ofPromise(reply)
        }
    }
}