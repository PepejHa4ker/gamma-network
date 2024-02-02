package com.pepej.gammanetwork.messages

import PlayerMessage

import com.pepej.gammanetwork.GammaNetwork
import com.pepej.gammanetwork.utils.getChannel
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.gammanetwork.utils.metadata
import com.pepej.gammanetwork.utils.typeTokenOf
import com.pepej.papi.command.Commands
import com.pepej.papi.events.Events
import com.pepej.papi.gson.GsonProvider
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.metadata.MetadataKey
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule
import com.pepej.papi.text.Text.colorize
import org.bukkit.Bukkit
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.util.*



object AdminChatMessageSystem : TerminableModule {

    private val messenger: Messenger = getServiceUnchecked()
    private val channel = messenger.getChannel<PlayerMessage>("admin-channel")

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(AsyncPlayerChatEvent::class.java, EventPriority.HIGHEST)
            .filter {
                it.player.metadata().has(CHAT) && it.player.metadata().getOrDefault(CHAT, ChatType.NOT_PRESENT) == ChatType.ADMIN
            }
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
                    val isIsAdminChat = it.sender().metadata().has(CHAT) && it.sender().metadata().getOrDefault(CHAT, ChatType.NOT_PRESENT) == ChatType.ADMIN
                    if (!isIsAdminChat) {
                        it.sender().metadata().put(CHAT, ChatType.ADMIN)
                        it.replyAnnouncement("Админ чат включен.")
                    } else {
                        it.sender().metadata().put(CHAT, ChatType.NOT_PRESENT)
                        it.replyAnnouncement("Админ чат&c выключен.")
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