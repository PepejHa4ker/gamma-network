package com.pepej.gammanetwork.commands.registry

import com.pepej.papi.command.argument.ArgumentParserRegistry
import com.pepej.papi.network.Network
import com.pepej.papi.network.Server
import com.pepej.papi.profiles.Profile
import java.util.*

object Arguments {

    fun profile(registry: ArgumentParserRegistry, network: Network) {
        registry.register(Profile::class.java) { arg ->
            Optional.ofNullable(network.onlinePlayers.values.find { it.name.orElse("").equals(arg, true) })
        }
    }

    fun server(registry: ArgumentParserRegistry, network: Network) {
        registry.register(Server::class.java) { arg ->
            Optional.ofNullable(network.servers.values.find { it.id.equals(arg, true) })
        }
    }
}