package com.pepej.gammanetwork.party

import com.pepej.papi.network.Server
import com.pepej.papi.profiles.Profile
import java.util.*

interface Party {

    fun getOwner(): UUID

    fun getCreator(): UUID

    fun getMembers(): List<UUID>

    /**
     * Transfers the party ownerships to the given profile.
     * Returns true if the transfer was successful
     * @return true if the transfer was successful
     */
    fun transferOwnership(to: UUID): Boolean

    fun isOwner(id: UUID): Boolean

    fun addMember(id: UUID)

    fun removeMember(id: UUID)

    fun isMultiServerSupported(): Boolean

    fun onOwnerServerChange(server: Server)

    fun sendMessage(author: UUID, message: String)

}