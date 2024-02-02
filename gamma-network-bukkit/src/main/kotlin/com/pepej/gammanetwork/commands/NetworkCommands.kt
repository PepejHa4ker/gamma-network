package com.pepej.gammanetwork.commands

import com.google.gson.GsonBuilder
import com.pepej.gammanetwork.GammaNetwork.Companion.instance
import com.pepej.gammanetwork.utils.asProfile
import com.pepej.gammanetwork.utils.getPlayer
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.gammanetwork.utils.parseOrFail
import com.pepej.papi.command.Commands
import com.pepej.papi.gson.GsonProvider
import com.pepej.papi.network.Network
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.profiles.Profile
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule

object NetworkCommands : TerminableModule {

    private val redirectSystem: RedirectSystem = getServiceUnchecked()
    private val network: Network = getServiceUnchecked()

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
                        instance.logger.info("Redirected with status ${it.status} reason ${it.reason} and params: ${it.params}}!")
                    }
            }
            .registerAndBind(consumer, "hub")

        Commands.create()
            .assertPermission("gammanetwork.commands.send")
            .assertUsage("<server>")
            .assertPlayer()
            .tabHandler { network.servers.values.map { it.id } }
            .handler { ctx ->
//                val profile = ctx.arg(0).parseOrFail<Profile>()
                val server = ctx.arg(0).parseOrFail<String>()
//                val profileServer = network.servers.values.find { it.onlinePlayers.containsValue(profile) } ?: return@handler ctx.replyError("Игрок не найден")
//                if (profileServer.id.equals(server, true)) {
//                    ctx.replyError("Игрок уже на сервере!")
//                    return@handler
//                }
                redirectSystem.redirectPlayer(server, ctx.sender(), mutableMapOf())
                    .thenAcceptAsync {
                        instance.logger.info("Redirected with status ${it.status} reason ${it.reason} and params: ${it.params}}!")
                    }
            }
            .registerAndBind(consumer, "connect")
    }
}