package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.utils.asCraft
import com.pepej.papi.events.Events
import com.pepej.papi.terminable.composite.CompositeTerminable
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerLoginEvent
import java.util.*

object NetworkMalformedProfileModule : NetworkModule {

    private val EMPTY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    override var scope: CompositeTerminable? = CompositeTerminable.create()
    override val name = "MalformedProfile"
    override var enabled = false

    override fun onEnable() {
        scope?.let { scope ->
            Events.subscribe(AsyncPlayerPreLoginEvent::class.java)
                .filter { it.uniqueId.equals(EMPTY_UUID) }
                .handler { it.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, "anlak") }
                .bindWith(scope)

            Events.subscribe(PlayerLoginEvent::class.java)
                .filter { it.player.asCraft().profile.properties["twinks"] == null }
                .handler { it.disallow(PlayerLoginEvent.Result.KICK_FULL, "anlakich") }
                .bindWith(scope)
        }
    }

}