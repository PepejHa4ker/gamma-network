package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.messages.ChatType
import com.pepej.gammanetwork.utils.getChannel
import com.pepej.gammanetwork.utils.wrapAsPlayer
import com.pepej.papi.command.Commands
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.metadata.MetadataKey
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.text.Text.colorize
import org.bukkit.Bukkit

val SPY: MetadataKey<Boolean> = MetadataKey.createBooleanKey("spy")

object SpyModule : NetworkModule("Spy") {

    fun publish(message: SpyMessage) {
        if (!enabled) return
        channel.sendMessage(message)
    }

    private val channel = messenger.getChannel<SpyMessage>("spy")

    override fun onEnable(consumer: TerminableConsumer) {
        registerCommand(consumer)

        channel.newAgent { _, message ->
            for (player in Bukkit.getOnlinePlayers()) {
                val meta = Metadata.provideForPlayer(player.uniqueId)
                val spyEnabled = meta.getOrDefault(SPY, false)
                if (!spyEnabled) continue
                if (player.name.equals(message.from, true)) continue
                if (message.to != null && player.name.equals(message.to, true)) continue
                when (message.type) {
                    SpyType.PRIVATE -> player.sendMessage(colorize("&8[SPY:PM] &7${message.from} -> ${message.to}: &f${message.text}"))
                    SpyType.GLOBAL -> player.sendMessage(colorize("&8[SPY:G] &7${message.from}@${message.server}: &f${message.text}"))
                    SpyType.LOCAL -> player.sendMessage(colorize("&8[SPY:L] &7${message.from}@${message.server}: &f${message.text}"))
                }
            }
        }.bindWith(consumer)
    }


    private fun registerCommand(consumer: TerminableConsumer) {
        Commands.create()
            .assertPermission("gammachat.spy")
            .assertPlayer()
            .handler { ctx ->
                val meta = Metadata.provideForPlayer(ctx.sender().uniqueId)
                val current = meta.getOrDefault(SPY, false)
                val next = !current
                meta.put(SPY, next)
                if (next) {
                    ctx.replyAnnouncement("Прослушка включена.")
                } else {
                    ctx.replyAnnouncement("Прослушка &cвыключена.")
                }
            }
            .registerAndBind(consumer, "gspy")

    }
}

data class SpyMessage(
    val type: SpyType,
    val from: String,
    val to: String?,
    val text: String,
    val server: String
)

enum class SpyType {
    GLOBAL,
    LOCAL,
    PRIVATE
}
