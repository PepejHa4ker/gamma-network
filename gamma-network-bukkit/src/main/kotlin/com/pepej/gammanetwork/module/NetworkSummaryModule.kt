package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.utils.GAMMA_GREEN
import com.pepej.gammanetwork.utils.GAMMA_RED
import com.pepej.papi.command.Commands
import com.pepej.papi.event.filter.EventFilters
import com.pepej.papi.events.Events
import com.pepej.papi.network.Server
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.time.DurationFormatter
import com.pepej.papi.time.Time
import com.pepej.papi.utils.Players
import org.bukkit.command.CommandSender
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import java.time.Instant

internal object NetworkSummaryModule : NetworkModule("Summary") {

    override fun onEnable(consumer: TerminableConsumer) {
            Commands.create()
                .assertPermission("network.summary")
                .assertUsage("[server]")
                .handler { ctx ->
                    if (ctx.args().isNotEmpty()) {
                        val server = ctx.arg(0).parse(Server::class.java)
                        if (server.isPresent) {
                            sendSummary(ctx.sender(), server.get())
                        }

                    } else {
                        sendAllSeversSummary(ctx.sender(), network.servers.values)
                    }

                }.registerAndBind(consumer, "online", "netsum")

            Events.subscribe(PlayerJoinEvent::class.java, EventPriority.MONITOR)
                .filter(EventFilters.playerHasPermission("network.summary.onjoin"))
                .handler {
                    Schedulers.sync().runLater({
                        sendAllSeversSummary(it.player, network.servers.values)
                    }, 1L)
                }.bindWith(consumer)
    }
    private fun sendAllSeversSummary(sender: CommandSender, servers: Collection<Server>) {
        sendHeader(sender)
        servers
            .sortedBy { it.onlinePlayers.size }
            .forEach { sendSummary(sender, it) }
    }

    private fun sendHeader(sender: CommandSender) {
        Players.msg(sender, Players.MessageType.NONE, *arrayOf("$GAMMA_GREEN &f<Онлайн>."))
        Players.msg(sender, Players.MessageType.NONE, "$GAMMA_GREEN &7${network.overallPlayerCount} игроков онлайн.")
        Players.msg(sender, Players.MessageType.NONE, GAMMA_GREEN)
    }

    private fun sendSummary(sender: CommandSender, server: Server) {
        val id = server.id
        if (!server.isOnline) {
            val lastPing = server.lastPing
            if (lastPing == 0L) {
                return
            }
            val lastSeen = DurationFormatter.CONCISE.format(Time.diffToNow(Instant.ofEpochMilli(lastPing)))
            Players.msg(sender, Players.MessageType.NONE,"$GAMMA_RED &a$id &7- был в сети $lastSeen назад")
        } else {
            Players.msg(sender, Players.MessageType.NONE, "$GAMMA_GREEN &a$id &7- &b${server.onlinePlayers.size}&7/${server.maxPlayers}")
            if (server.onlinePlayers.isNotEmpty()) {
                Players.msg(sender, Players.MessageType.NONE, "    ${server.onlinePlayers.values.joinToString("&r, ") { "&7${it.name.get()}" }}")
            }


        }
    }
}