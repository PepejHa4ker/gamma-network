package com.pepej.gammanetwork.redirect

import com.pepej.gammanetwork.GammaNetwork.Companion.instance
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.messaging.bungee.BungeeCord
import com.pepej.papi.network.redirect.AbstractRedirectSystem
import com.pepej.papi.network.redirect.PlayerRedirector
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.profiles.Profile


object VelocityPlayerRedirector: PlayerRedirector {

    private val bungeeCord: BungeeCord = getServiceUnchecked()

    override fun redirectPlayer(server: String, profile: Profile) {
        instance.logger.info("Attempt to redirect player ${profile.name} to $server")
        bungeeCord.connectOther(profile.name.orElse(null), server)

    }

}



