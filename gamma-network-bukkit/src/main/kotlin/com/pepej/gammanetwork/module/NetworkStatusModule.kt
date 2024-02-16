package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.utils.GAMMA_GREEN
import com.pepej.gammanetwork.utils.GAMMA_RED
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.event.bus.api.Subscribers
import com.pepej.papi.network.Network
import com.pepej.papi.network.event.ServerConnectEvent
import com.pepej.papi.network.event.ServerDisconnectEvent
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule
import com.pepej.papi.utils.Players

object NetworkStatusModule : TerminableModule {

    private val network: Network = getServiceUnchecked()
    override fun setup(consumer: TerminableConsumer) {
        val bus = network.eventBus
        val s =
        Subscribers.register(bus, ServerConnectEvent::class.java) {
            broadcast("$GAMMA_GREEN &b${it.id} &7подключен.") }
            .bindWith(consumer)
        Subscribers.register(bus, ServerDisconnectEvent::class.java) {
            if (it.reason?.isEmpty() == false) {
                broadcast("$GAMMA_RED &b${it.id} &7отключен. (причина: ${it.reason})")
            } else {
                broadcast("$GAMMA_RED &b${it.id} &7отключен. (причина неизвестна)")
            }
        }.bindWith(consumer)
    }

    private fun broadcast(message: String) {
        Players.stream()
            .filter { it.hasPermission("network.status.alerts") }
            .forEach { Players.msg(it,  message) }
    }
}
