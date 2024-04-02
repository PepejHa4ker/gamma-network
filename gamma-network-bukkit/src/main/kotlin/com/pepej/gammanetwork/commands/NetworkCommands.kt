package com.pepej.gammanetwork.commands

import com.pepej.gammanetwork.GammaNetworkPlugin.Companion.instance
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.command.Commands
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule
import org.slf4j.LoggerFactory

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



    }



}
