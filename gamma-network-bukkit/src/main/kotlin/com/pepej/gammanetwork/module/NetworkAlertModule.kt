package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.messages.ServerMessage
import com.pepej.gammanetwork.utils.broadcast
import com.pepej.gammanetwork.utils.getChannel
import com.pepej.gammanetwork.utils.wrapAsPlayer
import com.pepej.papi.command.Commands
import com.pepej.papi.terminable.TerminableConsumer
import org.slf4j.LoggerFactory

internal object NetworkAlertModule : NetworkModule("Alert") {

    private val log = LoggerFactory.getLogger(NetworkAlertModule::class.java)

    private val channel = messenger.getChannel<ServerMessage>("alerts").apply {
        newAgent { _, (_, _, message) ->
            broadcast(message)
        }
    }

    override fun onEnable(consumer: TerminableConsumer) {
        Commands.create()
            .assertUsage("<message>")
            .assertPermission("network.commands.alert")
            .handler { ctx ->
                val message = ctx.args().joinToString(" ")
                val (uuid, name) = ctx.sender().wrapAsPlayer()
                channel.sendMessage(ServerMessage(uuid, name, message))
            }
            .registerAndBind(consumer, "alert")

    }


}


