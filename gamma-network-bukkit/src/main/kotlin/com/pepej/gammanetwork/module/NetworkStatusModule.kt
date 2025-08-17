package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.utils.broadcast
import com.pepej.papi.event.bus.api.Subscribers
import com.pepej.papi.network.event.ServerConnectEvent
import com.pepej.papi.network.event.ServerDisconnectEvent
import com.pepej.papi.terminable.TerminableConsumer

internal object NetworkStatusModule : NetworkModule("Status") {

    override fun onEnable(consumer: TerminableConsumer) {
        val bus = network.eventBus
        Subscribers.register(bus, ServerConnectEvent::class.java) {
            broadcast("&b${it.id} &7подключен.")
        }
            .bindWith(consumer)
        Subscribers.register(bus, ServerDisconnectEvent::class.java) {
            if (it.reason?.isEmpty() == false) {
                broadcast("&b${it.id} &7отключен. (причина: ${it.reason})")
            } else {
                broadcast("&b${it.id} &7отключен. (причина неизвестна)")
            }
        }.bindWith(consumer)
    }

}
