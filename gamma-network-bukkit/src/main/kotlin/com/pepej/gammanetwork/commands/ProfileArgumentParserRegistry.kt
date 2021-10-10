package com.pepej.gammanetwork.commands

import com.pepej.papi.command.argument.ArgumentParserRegistry
import com.pepej.papi.network.Network
import com.pepej.papi.profiles.Profile
import java.util.*

object ProfileArgumentParserRegistry {

    fun register(registry: ArgumentParserRegistry, network: Network) {
        registry.register(Profile::class.java) { arg ->
            Optional.ofNullable(network.onlinePlayers.values.find { it.name.orElse("").equals(arg, true) })
        }
    }
}