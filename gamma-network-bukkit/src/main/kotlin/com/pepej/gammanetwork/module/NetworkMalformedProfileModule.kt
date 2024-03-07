package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.utils.asCraft
import com.pepej.papi.events.Events
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerLoginEvent
import java.util.UUID

object NetworkMalformedProfileModule : TerminableModule {

    private val EMPTY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(AsyncPlayerPreLoginEvent::class.java)
            .filter { it.uniqueId.equals(EMPTY_UUID) }
            .handler { it.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, "anlak") }
            .bindWith(consumer)

        Events.subscribe(PlayerLoginEvent::class.java)
            .filter { it.player.asCraft().profile.properties["twinks"] == null }
            .handler { it.disallow(PlayerLoginEvent.Result.KICK_FULL, "anlakich") }
            .bindWith(consumer)
    }

}