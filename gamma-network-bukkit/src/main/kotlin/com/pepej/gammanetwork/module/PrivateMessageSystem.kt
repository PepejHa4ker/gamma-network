package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.GammaNetworkPlugin.Companion.instance
import com.pepej.gammanetwork.commands.Tabs.players
import com.pepej.gammanetwork.utils.getConversationChannel
import com.pepej.gammanetwork.utils.metadata
import com.pepej.papi.command.Commands
import com.pepej.papi.messaging.conversation.ConversationMessage
import com.pepej.papi.messaging.conversation.ConversationReply
import com.pepej.papi.messaging.conversation.ConversationReplyListener
import com.pepej.papi.messaging.conversation.ConversationReplyListener.RegistrationAction
import com.pepej.papi.metadata.MetadataKey
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.text.Text.colorize
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.TimeUnit

val LAST_PM: MetadataKey<UUID> = MetadataKey.create("last-pm", UUID::class.java)

object PrivateMessageSystem : NetworkModule("PrivateMessages") {


    private val channel = messenger.getConversationChannel<PrivateMessage, PrivateMessageReply>("private-messages")

    private fun sendMessage(from: Player, to: String, message: String) {

        val pm = PrivateMessage(from = from.name, fromId = from.uniqueId, message = message, to = to)
        channel.sendMessage(pm, object : ConversationReplyListener<PrivateMessageReply> {
            override fun onReply(reply: PrivateMessageReply): RegistrationAction {
                val player = Bukkit.getPlayerExact(reply.deliveredTo)
                val name = player?.displayName ?: ("&a" + reply.deliveredTo)
                from.sendMessage(String.format(colorize("&7[&eВы&7 -> $name&7]&f: ") + "%s", message))
                from.metadata().put(LAST_PM, reply.deliveredToId)

                val spyMsg = SpyMessage(
                    type = SpyType.PRIVATE,
                    from = from.name,
                    to = reply.deliveredTo,
                    text = message,
                    server = instance.id
                )
                SpyModule.publish(spyMsg)

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
        val fromId: UUID,
        val message: String,
        val to: String
    ) : ConversationMessage {
        override fun getConversationId(): UUID {
            return conversationId
        }
    }

    private class PrivateMessageReply(
        private val conversationId: UUID,
        val deliveredTo: String,
        val deliveredToId: UUID
    ) : ConversationMessage {
        override fun getConversationId(): UUID {
            return conversationId
        }
    }

    override fun onEnable(consumer: TerminableConsumer) {
        channel.newAgent { _, message ->
            val reply = Schedulers.sync().supply {
                val player = Bukkit.getPlayer(message.to) ?: return@supply null
                val sender = Bukkit.getPlayerExact(message.from)
                val displayName = sender?.displayName ?: message.from
                player.sendMessage(String.format(colorize("&7[&a$displayName&7 -> &eВам&7]&f: ") + "%s", message.message))
                player.metadata().put(LAST_PM, message.fromId)
                PrivateMessageReply(message.conversationId, player.name, player.uniqueId)
            }
            ConversationReply.ofPromise(reply)
        }.bindWith(consumer)

        Commands.create()
            .assertPlayer()
            .assertUsage("<player> <message>")
            .tabHandler { context -> context.arg(0).players(network) }
            .handler { ctx ->
                val message = ctx.args().drop(1).joinToString(" ")
                if (message.isEmpty()) {
                    ctx.replyError("Сообщение не может быть пустым")
                    return@handler
                }
                sendMessage(ctx.sender(), ctx.arg(0).parseOrFail(String::class.java), message)
            }
            .registerAndBind(consumer, "ms", "msg", "сообщение")

        Commands.create()
            .assertPlayer()
            .assertUsage("<message>")
            .handler { ctx ->
                val sender = ctx.sender()
                val target = sender.metadata().getOrNull(LAST_PM)
                if (target == null) {
                    ctx.replyError("Нет последнего собеседника для отправки")
                    return@handler
                }
                val targetName = instance.network.nameByUUID(target)
                val message = ctx.args().joinToString(" ").trim()
                if (message.isEmpty()) {
                    ctx.replyError("Сообщение не может быть пустым")
                    return@handler
                }
                sendMessage(sender, targetName, message)
            }
            .registerAndBind(consumer, "reply", "r")

    }

}