package com.pepej.gammanetwork.party

import com.pepej.papi.network.Server
import com.pepej.papi.profiles.Profile
import java.util.*

class LocalServerParty(
    id: UUID,
    creator: UUID,
    owner: UUID,
    members: MutableList<UUID> = mutableListOf()
) : AbstractParty(id, creator, owner, members) {

    override fun isMultiServerSupported(): Boolean {
        return false
    }

    override fun onOwnerServerChange(server: Server) {
        throw UnsupportedOperationException()

    }

}