package com.pepej.gammanetwork.party

import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.messaging.bungee.BungeeCord
import com.pepej.papi.network.Server
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.profiles.Profile
import com.pepej.papi.utils.Players
import java.util.*

class MultiServerParty(
    id: UUID,
    creator: UUID,
    owner: UUID,
    members: MutableList<UUID> = mutableListOf(),
) : AbstractParty(id, creator, owner, members) {

    @Transient
    private val redirectSystem = getServiceUnchecked<RedirectSystem>()

    override fun isMultiServerSupported(): Boolean {
        return true
    }

    override fun onOwnerServerChange(server: Server) {


        this.members.mapNotNull(Players::getNullable)
            .forEach { p ->
                redirectSystem.redirectPlayer(server.id, p, mutableMapOf())
            }
    }
}