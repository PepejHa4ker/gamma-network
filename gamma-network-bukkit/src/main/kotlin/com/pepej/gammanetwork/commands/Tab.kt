package com.pepej.gammanetwork.commands

import com.pepej.gammanetwork.utils.parseOrFail
import com.pepej.papi.command.argument.Argument
import com.pepej.papi.network.Network
import com.pepej.papi.utils.TabHandlers


object Tabs {
    fun Argument.enableDisableTab(): List<String> = TabHandlers.of(parseOrFail(), "enable", "disable")

    fun Argument.servers(network: Network): List<String> = TabHandlers.of(parseOrFail(), *network.servers.map { it.value.id }.toTypedArray())

    fun Argument.players(network: Network): List<String> = TabHandlers.of(parseOrFail(), *network.onlinePlayers.map { it.value.name.get() }.toTypedArray())

}