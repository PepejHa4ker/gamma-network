package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.GammaNetworkPlugin.Companion.instance
import com.pepej.gammanetwork.utils.getChannel
import com.pepej.papi.command.Commands
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.metadata.MetadataKey
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.text.Text.colorize
import org.bukkit.Bukkit
import org.bukkit.Location

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

                var visibleWithoutSpy = false
                when (message.type) {
                    SpyType.GLOBAL -> {
                        visibleWithoutSpy = player.hasPermission("gammachat.global.notify")
                    }
                    SpyType.LOCAL -> {
                        if (message.server.equals(instance.serverId, true)) {
                            val w = message.world
                            val r = message.radius
                            val pos = message.position
                            if (w != null && r != null && pos != null) {
                                if (player.world.name.equals(w, true)) {
                                    val dist = player.location.distance(Location(player.world, pos.x, pos.y, pos.z))
                                    visibleWithoutSpy = dist < r
                                }
                            }
                        }
                    }
                    SpyType.PRIVATE -> visibleWithoutSpy = false
                }

                if (visibleWithoutSpy) continue

                when (message.type) {
                    SpyType.PRIVATE -> player.sendMessage(colorize("&8[SPY] &7[&a${message.from}&7 -> &e${message.to}&7]&f: ${message.text}"))
                    SpyType.GLOBAL -> player.sendMessage(colorize("&8[SPY: L] &7${message.from} &a[${message.server}]&f: ${message.text}"))
                    SpyType.LOCAL -> player.sendMessage(colorize("&8[SPY: G] &7${message.from} &a[${message.server}]&f: ${message.text}"))
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
    val server: String,
    val world: String? = null,
    val position: Position? = null,
    val radius: Int? = null
)

data class Position(val x: Double, val y: Double, val z: Double)

enum class SpyType {
    GLOBAL,
    LOCAL,
    PRIVATE
}
