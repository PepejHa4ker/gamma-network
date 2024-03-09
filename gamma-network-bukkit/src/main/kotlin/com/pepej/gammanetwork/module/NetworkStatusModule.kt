package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.utils.GAMMA_GREEN
import com.pepej.gammanetwork.utils.GAMMA_RED
import com.pepej.gammanetwork.utils.broadcast
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.event.bus.api.Subscribers
import com.pepej.papi.network.Network
import com.pepej.papi.network.event.ServerConnectEvent
import com.pepej.papi.network.event.ServerDisconnectEvent
import com.pepej.papi.terminable.composite.CompositeTerminable

object NetworkStatusModule : NetworkModule {

    private val network: Network = getServiceUnchecked()
    override var scope: CompositeTerminable? = CompositeTerminable.create()
    override val name = "status"
    override var enabled = false

    override fun onEnable() {
        scope?.let { scope ->
            val bus = network.eventBus
            Subscribers.register(bus, ServerConnectEvent::class.java) {
                network.broadcast("$GAMMA_GREEN &b${it.id} &7подключен.")
            }
                .bindWith(scope)
            Subscribers.register(bus, ServerDisconnectEvent::class.java) {
                if (it.reason?.isEmpty() == false) {
                    network.broadcast("$GAMMA_RED &b${it.id} &7отключен. (причина: ${it.reason})")
                } else {
                    network.broadcast("$GAMMA_RED &b${it.id} &7отключен. (причина неизвестна)")
                }
            }.bindWith(scope)
        }
    }

}
