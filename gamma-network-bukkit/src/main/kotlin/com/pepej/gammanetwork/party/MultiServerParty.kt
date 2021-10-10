package com.pepej.gammanetwork.party

import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.messaging.bungee.BungeeCord
import com.pepej.papi.network.Server
import com.pepej.papi.profiles.Profile
import com.pepej.papi.utils.Players
import java.util.*

class MultiServerParty(
    creator: UUID,
    _owner: UUID,
    members: MutableList<UUID> = mutableListOf(),
) : AbstractParty(creator, _owner, members) {

    @Transient
    private val bungeeCord = getServiceUnchecked<BungeeCord>()

    override fun isMultiServerSupported(): Boolean {
        return true
    }

    override fun onOwnerServerChange(server: Server) {


        this.getMembers().mapNotNull(Players::getNullable)
            .forEach { p ->
                bungeeCord.connect(p, server.id)
            }
    }
}