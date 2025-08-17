package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.GammaNetworkPlugin
import com.pepej.gammanetwork.messages.CHAT
import com.pepej.gammanetwork.messages.ChatType
import com.pepej.gammanetwork.messages.PlayerMessage
import com.pepej.gammanetwork.utils.*
import com.pepej.papi.command.Commands
import com.pepej.papi.events.Events
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.text.Text.colorize
import com.pepej.papi.utils.Players
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerChatEvent


object GlobalChatModule : NetworkModule("GlobalChat") {

    private val luckPerms: LuckPerms = getServiceUnchecked()
    private val config = GammaNetworkPlugin.instance.configuration.chat
    private val channel = messenger.getChannel<PlayerMessage>("global-chat-channel")

    override fun onEnable(consumer: TerminableConsumer) {
        Events.subscribe(AsyncPlayerChatEvent::class.java, EventPriority.HIGHEST)
            .filter {
                it.player.metadata().has(CHAT) && it.player.metadata()
                    .getOrDefault(CHAT, ChatType.NOT_PRESENT) == ChatType.GLOBAL
            }
            .handler {
                it.isCancelled = true
                val (uuid, name) = it.player.wrapAsPlayer()
                channel.sendMessage(
                    PlayerMessage(
                        uuid,
                        name,
                        it.message,
                        GammaNetworkPlugin.instance.serverId
                    )
                )
            }.bindWith(consumer)
        Events.subscribe(AsyncPlayerChatEvent::class.java)
            .filter { config.enable }
            .filter {
                !it.player.metadata().has(CHAT) || it.player.metadata().getOrDefault(
                    CHAT,
                    ChatType.NOT_PRESENT
                ) == ChatType.NOT_PRESENT
            }
            .handler { event ->
                event.isCancelled = true
                var message = event.message
                val sender = event.player
                val luckPermsUser = luckPerms.userManager.getUser(sender.uniqueId) ?: return@handler
                val metadata = luckPermsUser.cachedData.metaData
                val global = message.isNotEmpty() && message.first() == '!'
                if (sender.hasPermission("gammachat.chat.admin")) {
                    message = colorize(config.adminMessageColor.format + message)
                }
                if (config.enableSplitting) {
                    if (global) {
                        message = message.substring(1)
                        if (sender.hasPermission("gammachat.chat.admin")) {
                            message = colorize(config.adminMessageColor.format + message)
                        }
                        Bukkit.getConsoleSender().sendMessage(message)
                        Players.all()
                            .forEach { p ->
                                p.sendMessage(
                                    colorize(
                                        config.globalFormat.format
                                            .replace("{suffix}", metadata.suffix ?: "")
                                            .replace("{prefix}", metadata.prefix ?: "")
                                            .replace("{username}", sender.name)
                                    )
                                        .replace("{message}", message)
                                )

                            }
                    } else {
                        val message = colorize(
                            config.localFormat.format
                                .replace("{suffix}", metadata.suffix ?: "")
                                .replace("{prefix}", metadata.prefix ?: "")
                                .replace("{username}", sender.name)
                        )
                            .replace("{message}", message)
                        Players.all()
                            .filter { it.location.world.name == sender.location.world.name }
                            .filter { it.location distance sender.location < config.localChatRadius }
                            .forEach { player ->
                                player.sendMessage(message)
                            }
                        Bukkit.getConsoleSender().sendMessage(message)

                    }
                }

            }.bindWith(consumer)
        Commands.create()
            .assertPermission("gammachat.globalchat")
            .assertUsage("[message]")
            .handler {
                val (uuid, name) = it.sender().wrapAsPlayer()
                if (it.args().isNotEmpty()) {
                    val message = it.args().joinToString(" ")
                    channel.sendMessage(
                        PlayerMessage(
                            uuid,
                            name,
                            message,
                            GammaNetworkPlugin.instance.serverId
                        )
                    )

                } else {
                    val isInGlobalChat = it.sender().metadata().has(CHAT) && it.sender().metadata()
                        .getOrDefault(CHAT, ChatType.NOT_PRESENT) == ChatType.GLOBAL

                    if (!isInGlobalChat) {
                        it.sender().metadata().put(CHAT, ChatType.GLOBAL)
                        it.replyAnnouncement("Глобальный чат включен.")
                    } else {
                        it.sender().metadata().put(CHAT, ChatType.NOT_PRESENT)
                        it.replyAnnouncement("Глобальный чат&c выключен.")
                    }
                }
            }
            .registerAndBind(consumer, "g", "global")

        channel.newAgent { agent, message ->
            Schedulers.sync().run {
                val message = colorize("&c[Gamma] &a[${message.server}] ${message.displayName}&f: ${message.message}")
                Bukkit.getConsoleSender().sendMessage(message)
                Bukkit.broadcast(
                    message,
                    "gammachat.global.notify"
                )
            }
        }.bindWith(consumer)
    }

}