package com.pepej.gammanetwork.party

import com.pepej.papi.text.Text.colorize
import com.pepej.papi.utils.Players
import java.util.*

abstract class AbstractParty(
    override val id: UUID,
    override val creator: UUID,
    override var owner: UUID,
    override val members: MutableList<UUID> = mutableListOf()) : Party {


    override fun sendMessage(author: UUID, message: String) {
        val authorName = Players.getNullable(author)?.name ?: ""
       this.members.mapNotNull(Players::getNullable)
           .forEach {
               it.sendMessage(colorize("&e[Party]&a${(" $authorName")}&6: &r$message")) }
    }


    override fun transferOwnership(to: UUID): Boolean {
        if (!isOwner(to)) {
            this.owner = to
            return true
        }
        return false
    }

    override fun isOwner(id: UUID): Boolean {
        return this.owner == id
    }

    override fun addMember(id: UUID) {
        this.members.add(id)
    }

    override fun removeMember(id: UUID) {
        this.members.remove(id)
        if (members.isEmpty()) {
            Parties.removeParty(this)
        }
    }
}