package com.pepej.gammanetwork.redirect


import com.pepej.gammanetwork.utils.getChannel
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.events.Events
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object RedirectContext {
    val tokens: MutableMap<UUID, String> = ConcurrentHashMap()
}

object RedirectLeaveListenerModule : TerminableModule {

    private val messenger: Messenger = getServiceUnchecked()

    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(PlayerQuitEvent::class.java)
            .handler {
                val playerId = it.player.uniqueId
                val token = RedirectContext.tokens.remove(playerId) ?: return@handler

                val channel = messenger.getChannel<GammaNetworkRequestHandler.RedirectLeftAck>("redirect-left")
                channel.sendMessage(GammaNetworkRequestHandler.RedirectLeftAck(playerId, token))
            }
            .bindWith(consumer)
    }
}