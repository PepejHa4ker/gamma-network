package com.pepej.gammanetwork.redirect

import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.messaging.bungee.BungeeCord
import com.pepej.papi.network.redirect.PlayerRedirector
import com.pepej.papi.profiles.Profile
import org.slf4j.LoggerFactory


object VelocityPlayerRedirector: PlayerRedirector {

    private val bungeeCord: BungeeCord = getServiceUnchecked()
    private val log = LoggerFactory.getLogger(VelocityPlayerRedirector::class.java)

    override fun redirectPlayer(server: String, profile: Profile) {
        log.info("Attempt to redirect player ${profile.name.get()} to $server")
        bungeeCord.connectOther(profile.name.orElse(null), server)

    }

}



