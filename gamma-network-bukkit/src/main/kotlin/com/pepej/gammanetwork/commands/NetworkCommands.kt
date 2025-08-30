package com.pepej.gammanetwork.commands

import com.pepej.gammanetwork.GammaNetworkPlugin.Companion.instance
import com.pepej.gammanetwork.commands.Tabs.players
import com.pepej.gammanetwork.commands.Tabs.servers
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.gammanetwork.utils.parseOrFail
import com.pepej.papi.command.Commands
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule
import com.pepej.papi.utils.TabHandlers
import com.pepej.papi.utils.TabHandlers.players
import org.bukkit.Bukkit
import org.slf4j.LoggerFactory
import kotlin.jvm.optionals.getOrElse

object NetworkCommands : TerminableModule {

    private val redirectSystem: RedirectSystem = getServiceUnchecked()
    private val logger = LoggerFactory.getLogger(NetworkCommands::class.java)

    override fun setup(consumer: TerminableConsumer) {
        Commands.create()
            .assertPlayer()
            .handler { ctx ->
                if (instance.serverId == "Hub") {
                    ctx.replyError("You are already on the hub!")
                    return@handler
                }
                redirectSystem.redirectPlayer("Hub", ctx.sender(), mutableMapOf())
                    .thenAcceptAsync {
                        logger.debug(
                            "Redirected with status {} reason {} and params: {}}!",
                            it.status,
                            it.reason,
                            it.params
                        )
                    }
            }
            .registerAndBind(consumer, "hub", "хаб")


        Commands.create()
            .assertPlayer()
            .assertUsage("<server>")
            .assertPermission("network.commands.goto")
            .tabHandler { ctx -> ctx.arg(0).servers(instance.network) }
            .handler { ctx ->
                val serverId: String = ctx.arg(0).parseOrFail()
                if (instance.serverId.equals(serverId, ignoreCase = true)) {
                    ctx.replyError("Вы уже на сервере $serverId")
                    return@handler
                }
                redirectSystem.redirectPlayer(serverId, ctx.sender(), mutableMapOf())
                    .thenAcceptAsync {
                        logger.debug(
                            "Goto: redirected {} to {} -> status={} reason={} params={}",
                            ctx.sender().name,
                            serverId,
                            it.status,
                            it.reason,
                            it.params
                        )
                    }
            }
            .registerAndBind(consumer, "goto")

        Commands.create()
            .assertPlayer()
            .assertPermission("network.commands.send")
            .assertUsage("<player/all/current> <server>")
            .tabHandler { ctx ->
                when (ctx.args().size) {
                    1 -> {
                        val arg = ctx.arg(0)
                        val argStr = arg.parseOrFail<String>()
                        TabHandlers.of(argStr, "all", "current", *instance.network.onlinePlayers.map { it.value.name.get() }.toTypedArray())
                    }

                    2 -> {
                        ctx.arg(1).servers(instance.network)
                    }

                    else -> emptyList()
                }
            }
            .handler { ctx ->
                val target = ctx.arg(0).parseOrFail<String>()
                val serverId = ctx.arg(1).parseOrFail<String>()

                when (target.toLowerCase()) {
                    "current" -> {
                        val online = Bukkit.getOnlinePlayers()

                        online.forEach { player ->
                            redirectSystem.redirectPlayer(serverId, player, mutableMapOf())
                                .thenAcceptAsync {
                                    logger.debug(
                                        "Send-current: {} -> {} status={} reason={}",
                                        player.name, serverId, it.status, it.reason
                                    )
                                }
                        }
                        ctx.reply("&aОтправлено игроков: ${online.size} на $serverId")
                    }
                    "all" -> {
                        val players = instance.network.onlinePlayers.values
                        players.forEach { player ->
                            redirectSystem.redirectPlayer(serverId, player, mutableMapOf())
                                .thenAcceptAsync {
                                    logger.debug(
                                        "Send-all: {} -> {} status={} reason={}",
                                        player.name, serverId, it.status, it.reason
                                    )
                                }
                        }
                    }
                    else -> {
                        val player = instance.network.onlinePlayers.values.find { it.name.get().equals(target, true) }
                        if (player == null) {
                            ctx.replyError("Игрок &a$target &cне найден!")
                            return@handler
                        }
                        redirectSystem.redirectPlayer(serverId, player, mutableMapOf())
                            .thenAcceptAsync {
                                logger.debug(
                                    "Send: {} -> {} status={} reason={}",
                                    player.name, serverId, it.status, it.reason
                                )
                            }
                    }
                }

            }
            .registerAndBind(consumer, "send")
    }


}
