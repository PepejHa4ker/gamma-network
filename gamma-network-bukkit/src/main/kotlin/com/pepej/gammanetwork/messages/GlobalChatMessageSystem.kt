package com.pepej.gammanetwork.messages

import PlayerMessage
import com.pepej.gammanetwork.GammaNetwork
import com.pepej.gammanetwork.utils.distance
import com.pepej.gammanetwork.utils.getChannel
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.gammanetwork.utils.metadata
import com.pepej.papi.command.Commands
import com.pepej.papi.events.Events
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule
import com.pepej.papi.text.Text.colorize
import com.pepej.papi.utils.Players
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerChatEvent


object GlobalChatMessageSystem : TerminableModule {

    private val messenger: Messenger = getServiceUnchecked()
    private val luckPerms: LuckPerms = getServiceUnchecked()
    private val config = GammaNetwork.instance.configuration.chat
    private val channel = messenger.getChannel<PlayerMessage>("global-chat-channel")

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(AsyncPlayerChatEvent::class.java, EventPriority.HIGHEST)
            .filter {
                it.player.metadata().has(CHAT) && it.player.metadata()
                    .getOrDefault(CHAT, ChatType.NOT_PRESENT) == ChatType.GLOBAL
            }
            .handler {
                it.isCancelled = true
                val player = it.player
                channel.sendMessage(
                    PlayerMessage(
                        player.uniqueId,
                        player.displayName,
                        GammaNetwork.instance.serverId,
                        it.message
                    )
                )
            }.bindWith(consumer)
        Events.subscribe(AsyncPlayerChatEvent::class.java)
            .filter { GammaNetwork.instance.configuration.chat.enableSplitting }
            .filter {
                it.player.metadata().has(CHAT) && it.player.metadata()
                    .getOrDefault(CHAT, ChatType.NOT_PRESENT) != ChatType.NOT_PRESENT
            }
            .handler {
                it.isCancelled = true
                var message = it.message
                val sender = it.player
                val luckPermsUser = luckPerms.userManager.getUser(sender.uniqueId) ?: return@handler
                val metadata = luckPermsUser.cachedData.metaData
                if (message.isNotEmpty() && message.first() == '!') {
                    message = message.substring(1)
                    if (sender.hasPermission("gammachat.chat.admin")) {
                        message = colorize(config.adminMessageColor.format + message)
                    }
                    Players.all()
                        .forEach { p -> p.sendMessage(colorize(
                            config.globalFormat.format
                                .replace("{suffix}", metadata.suffix ?: "")
                                .replace("{prefix}", metadata.prefix ?: "")
                                .replace("{username}", sender.name)
                        )
                            .replace("{message}", message))

                        }
                } else {
                    if (sender.hasPermission("gammachat.chat.admin")) {
                        message = colorize(config.adminMessageColor.format + message)
                    }
                    Players.all()
                        .filter { r ->
                            r.location.world.name == sender.location.world.name &&
                                    r.location distance sender.location < config.localChatRadius
                        }
                        .forEach { player -> player.sendMessage(colorize(
                            config.localFormat.format
                                .replace("{suffix}", metadata.suffix ?: "")
                                .replace("{prefix}", metadata.prefix ?: "")
                                .replace("{username}", sender.name)
                        )
                            .replace("{message}", message)) }



                }

            }.bindWith(consumer)
        Commands.create()
            .assertPlayer()
            .assertPermission("gammachat.globalchat")
            .assertUsage("[message]")
            .handler {
                val player = it.sender()
                if (it.args().isNotEmpty()) {
                    val message = it.args().joinToString(" ")
                    channel.sendMessage(
                        PlayerMessage(
                            player.uniqueId,
                            player.displayName,
                            GammaNetwork.instance.serverId,
                            message
                        )
                    )

                } else {
                    val isInGlobalChat = it.sender().metadata().has(CHAT) && it.sender().metadata().getOrDefault(CHAT, ChatType.NOT_PRESENT) == ChatType.GLOBAL

                    if (!isInGlobalChat) {
                        it.sender().metadata().put(CHAT, ChatType.GLOBAL)
                        it.replyAnnouncement("Глоабльный чат включен.")
                    } else {
                        it.sender().metadata().put(CHAT, ChatType.NOT_PRESENT)
                        it.replyAnnouncement("Глоабльный чат&c выключен.")
                    }
                }
            }
            .registerAndBind(consumer, "g", "global")

        channel.newAgent { agent, message ->
            Schedulers.sync().run {
                Bukkit.broadcast(
                    colorize("&c[Gamma] &a[${message.server}] ${message.displayName}&f: ${message.message}"),
                    "gammachat.global.notify"
                )
            }
        }.bindWith(consumer)
    }

}