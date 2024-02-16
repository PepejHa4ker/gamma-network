package com.pepej.gammanetwork.commands

import com.pepej.gammanetwork.shadow.entity.CraftPlayerShadow
import com.pepej.papi.command.Commands
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule

object Ping : TerminableModule {
    override fun setup(consumer: TerminableConsumer) {
        Commands.create()
            .assertPlayer()
            .handler {
                it.reply("Your ping: " + CraftPlayerShadow.create(it.sender()).getHandle().getPing())
            }
            .registerAndBind(consumer,"piing")
    }
}