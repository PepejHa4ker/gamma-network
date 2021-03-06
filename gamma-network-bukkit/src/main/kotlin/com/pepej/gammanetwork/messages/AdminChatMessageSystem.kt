package com.pepej.gammanetwork.messages

import com.pepej.gammanetwork.GammaNetwork
import com.pepej.gammanetwork.utils.getChannel
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.gammanetwork.utils.metadata
import com.pepej.papi.command.Commands
import com.pepej.papi.event.filter.EventFilters
import com.pepej.papi.events.Events
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.metadata.MetadataKey
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule
import com.pepej.papi.text.Text.colorize
import org.bukkit.Bukkit
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.util.*

data class PlayerMessage(val uuid: UUID, val displayName: String, val server: String, val message: String)

private val IS_IN_ADMIN_CHAT = MetadataKey.createBooleanKey("is-in-admin-chat")

object AdminChatMessageSystem : TerminableModule {

    private val messenger: Messenger = getServiceUnchecked()
    private val channel = messenger.getChannel<PlayerMessage>("admin-channel")

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(AsyncPlayerChatEvent::class.java)
            .filter(EventFilters.playerHasMetadata(IS_IN_ADMIN_CHAT))
            .handler {
               it.isCancelled = true
                val player = it.player
                channel.sendMessage(PlayerMessage(player.uniqueId, player.displayName, GammaNetwork.instance.serverId, it.message))
            }.bindWith(consumer)
        Commands.create()
            .assertPlayer()
            .assertPermission("gammachat.adminchat")
            .assertUsage("[message]")
            .handler {
                val player = it.sender()
                if (it.args().isNotEmpty()) {
                    val message = it.args().joinToString(" ")
                    channel.sendMessage(PlayerMessage(player.uniqueId, player.displayName, GammaNetwork.instance.serverId, message))

                } else {
                    val isIsAdminChat = it.sender().metadata().has(IS_IN_ADMIN_CHAT)
                    if (!isIsAdminChat) {
                        it.sender().metadata().put(IS_IN_ADMIN_CHAT, true)
                        it.replyAnnouncement("?????????? ?????? ??????????????.")
                    } else {
                        it.sender().metadata().remove(IS_IN_ADMIN_CHAT)
                        it.replyAnnouncement("?????????? ??????&c ????????????????.")
                    }
                }
            }
            .registerAndBind(consumer, "a", "adm")

        channel.newAgent { agent, message ->
            Schedulers.sync().run {
                Bukkit.broadcast(colorize("&c[Admin-chat] &a[${message.server}] ${message.displayName}&f: ${message.message}"), "gammachat.notify")
            }
        }.bindWith(consumer)
    }

}