package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.GammaNetworkPlugin.Companion.instance
import com.pepej.gammanetwork.messages.CHAT
import com.pepej.gammanetwork.messages.ChatType
import com.pepej.gammanetwork.messages.PlayerMessage
import com.pepej.gammanetwork.utils.distance
import com.pepej.gammanetwork.utils.getChannel
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.gammanetwork.utils.metadata
import com.pepej.gammanetwork.utils.wrapAsPlayer
import com.pepej.papi.command.Commands
import com.pepej.papi.command.context.CommandContext
import com.pepej.papi.events.Events
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.text.Text.colorize
import com.pepej.papi.utils.Players
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerChatEvent

object GlobalChatModule : NetworkModule("GlobalChat") {

    private val luckPerms: LuckPerms = getServiceUnchecked()
    private val config = instance.configuration.chat
    private val channel = messenger.getChannel<PlayerMessage>("global-chat")

    override fun onEnable(consumer: TerminableConsumer) {
        registerGlobalChatModeListener(consumer)
        registerDefaultChatListener(consumer)
        registerGlobalCommand(consumer)
        registerGlobalChannelListener(consumer)
    }

    private fun registerGlobalChatModeListener(consumer: TerminableConsumer) {
        Events.subscribe(AsyncPlayerChatEvent::class.java, EventPriority.HIGH)
            .filter { !it.isCancelled }
            .filter { !isInAdminMode(it.player) }
            .filter { isInGlobalMode(it.player) }
            .handler {
                it.isCancelled = true
                val (uuid, name) = it.player.wrapAsPlayer()
                channel.sendMessage(PlayerMessage(uuid, name, it.message, instance.serverId))
            }
            .bindWith(consumer)
    }

    private fun registerDefaultChatListener(consumer: TerminableConsumer) {
        Events.subscribe(AsyncPlayerChatEvent::class.java, EventPriority.LOWEST)
            .filter { !it.isCancelled }
            .filter { config.enable }
            .filter { !isInAdminMode(it.player) }
            .filter { !isInGlobalMode(it.player) }
            .handler { event ->
                event.isCancelled = true
                handleDefaultChatMessage(event.player, event.message)
            }
            .bindWith(consumer)
    }

    private fun registerGlobalCommand(consumer: TerminableConsumer) {
        Commands.create()
            .assertPermission("gammachat.globalchat")
            .assertUsage("[message]")
            .handler {
                val (uuid, name) = it.sender().wrapAsPlayer()
                if (it.args().isNotEmpty()) {
                    val text = it.args().joinToString(" ")
                    channel.sendMessage(PlayerMessage(uuid, name, text, instance.serverId))
                } else {
                    toggleGlobalChat(it)
                }
            }
            .registerAndBind(consumer, "g", "global")
    }

    private fun registerGlobalChannelListener(consumer: TerminableConsumer) {
        channel.newAgent { _, message ->
            Schedulers.sync().run {
                val formatted = colorize("&c[Gamma] &a[${message.server}] ${message.displayName}&f: ${message.message}")
                Bukkit.getConsoleSender().sendMessage(formatted)
                Bukkit.broadcast(formatted, "gammachat.global.notify")
            }
        }.bindWith(consumer)
    }

    private fun isInGlobalMode(sender: CommandSender): Boolean {
        val meta = sender.metadata()
        return meta.has(CHAT) && meta.getOrDefault(CHAT, ChatType.NOT_PRESENT) == ChatType.GLOBAL
    }

    private fun isInAdminMode(sender: CommandSender): Boolean {
        val meta = sender.metadata()
        return meta.has(CHAT) && meta.getOrDefault(CHAT, ChatType.NOT_PRESENT) == ChatType.ADMIN
    }

    private fun toggleGlobalChat(ctx: CommandContext<CommandSender>) {
        val sender = ctx.sender()
        val nowGlobal = !isInGlobalMode(sender)
        sender.metadata().put(CHAT, if (nowGlobal) ChatType.GLOBAL else ChatType.NOT_PRESENT)
        if (nowGlobal) {
            ctx.replyAnnouncement("Глобальный чат включен.")
        } else {
            ctx.replyAnnouncement("Глобальный чат&c выключен.")
        }
    }

    private fun handleDefaultChatMessage(sender: Player, rawMessage: String) {
        val user = luckPerms.userManager.getUser(sender.uniqueId) ?: return
        val meta = user.cachedData.metaData

        val isGlobal = rawMessage.isNotEmpty() && rawMessage.first() == '!'
        val text = if (isGlobal) rawMessage.substring(1) else rawMessage
        val withAdmin = applyAdminColorIfNeeded(sender, text)

        if (!config.enableSplitting) return

        if (isGlobal) {
            broadcastGlobal(sender, meta.prefix, meta.suffix, withAdmin)
            publishSpy(SpyType.GLOBAL, sender.name, text)
        } else {
            broadcastLocal(sender, meta.prefix, meta.suffix, withAdmin)
            publishSpy(SpyType.LOCAL, sender.name, text)
        }
    }

    private fun applyAdminColorIfNeeded(player: Player, message: String): String {
        return if (player.hasPermission("gammachat.chat.admin")) {
            colorize(config.adminMessageColor.format + message)
        } else {
            message
        }
    }

    private fun broadcastGlobal(sender: Player, prefix: String?, suffix: String?, message: String) {
        val formatted = buildFormatted(config.globalFormat.format, sender.name, prefix, suffix, message)
        Players.all().forEach { it.sendMessage(formatted) }
        Bukkit.getConsoleSender().sendMessage(formatted)
    }

    private fun broadcastLocal(sender: Player, prefix: String?, suffix: String?, message: String) {
        val formatted = buildFormatted(config.localFormat.format, sender.name, prefix, suffix, message)
        Players.all()
            .filter { it.location.world.name == sender.location.world.name }
            .filter { it.location distance sender.location < config.localChatRadius }
            .forEach { it.sendMessage(formatted) }
        Bukkit.getConsoleSender().sendMessage(formatted)
    }

    private fun buildFormatted(
        baseFormat: String,
        username: String,
        prefix: String?,
        suffix: String?,
        message: String
    ): String {
        return colorize(
            baseFormat
                .replace("{suffix}", suffix ?: "")
                .replace("{prefix}", prefix ?: "")
                .replace("{username}", username)
        ).replace("{message}", message)
    }

    private fun publishSpy(type: SpyType, from: String, text: String) {
        SpyModule.publish(
            SpyMessage(
                type = type,
                from = from,
                to = null,
                text = text,
                server = instance.serverId
            )
        )
    }
}