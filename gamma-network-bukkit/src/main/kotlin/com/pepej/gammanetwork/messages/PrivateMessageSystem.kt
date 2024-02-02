package com.pepej.gammanetwork.messages

import com.pepej.gammanetwork.GammaNetwork
import com.pepej.gammanetwork.utils.getConversationChannel
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.command.Commands
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.messaging.conversation.ConversationMessage
import com.pepej.papi.messaging.conversation.ConversationReply
import com.pepej.papi.messaging.conversation.ConversationReplyListener
import com.pepej.papi.messaging.conversation.ConversationReplyListener.RegistrationAction
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule
import com.pepej.papi.text.Text.colorize
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.TimeUnit

object PrivateMessageSystem  : TerminableModule {

    private val messenger: Messenger = getServiceUnchecked()
    private val channel = messenger.getConversationChannel<PrivateMessage, PrivateMessageReply>("private-messages")
    private fun sendMessage(from: Player, to: String, message: String) {

        val pm = PrivateMessage(from = from.name, message = message, to = to)
        channel.sendMessage(pm, object : ConversationReplyListener<PrivateMessageReply> {
            override fun onReply(reply: PrivateMessageReply): RegistrationAction {
                val player = Bukkit.getPlayerExact(reply.deliveredTo)
                val name = player?.displayName ?: ("&a" + reply.deliveredTo)
                from.sendMessage(String.format(colorize("&7[&eВы&7 -> $name&7]&f: ") + "%s", message))
                return RegistrationAction.STOP_LISTENING
            }

            override fun onTimeout(replies: List<PrivateMessageReply>) {
                from.sendMessage(colorize("&cИгрок не найден"))
            }
        }, 2, TimeUnit.SECONDS)
    }

    private class PrivateMessage(
        private val conversationId: UUID = UUID.randomUUID(),
        val from: String,
        val message: String,
        val to: String
    ) : ConversationMessage {
        override fun getConversationId(): UUID {
            return conversationId
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

    override fun setup(consumer: TerminableConsumer) {
        channel.newAgent { _, message ->
            val reply = Schedulers.sync().supply {
                val player = Bukkit.getPlayer(message.to) ?: return@supply null

                val sender = Bukkit.getPlayerExact(message.from)
                val displayName = sender?.displayName ?: message.from
                player.sendMessage(String.format(colorize("&7[&a$displayName&7 -> &eВам&7]&f: ") + "%s", message.message))
                PrivateMessageReply(message.conversationId, player.name)
            }
            ConversationReply.ofPromise(reply)
        }.bindWith(consumer)

        Commands.create()
            .assertPlayer()
            .tabHandler {
                GammaNetwork.instance.network.onlinePlayers.values.map { it.name.get() }
            }
            .handler {
                sendMessage(it.sender(), it.arg(0).parseOrFail(String::class.java), it.args().drop(1).joinToString(" "))
            }
            .registerAndBind(consumer, "ms", "msg", "сообщение")
    }

}