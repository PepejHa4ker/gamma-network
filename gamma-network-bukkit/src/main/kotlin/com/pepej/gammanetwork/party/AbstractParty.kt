package com.pepej.gammanetwork.party

import com.pepej.papi.text.Text.colorize
import com.pepej.papi.utils.Players
import java.util.*

abstract class AbstractParty(
    private val creator: UUID,
    private var owner: UUID,
    private val members: MutableList<UUID> = mutableListOf()) : Party {


    override fun sendMessage(author: UUID, message: String) {
        val authorName = Players.getNullable(author)?.name ?: ""
       this.members.mapNotNull(Players::getNullable)
           .forEach {
               it.sendMessage(colorize("&e[Party]&a${(" $authorName")}&6: &r$message")) }
    }

    override fun getOwner(): UUID {
        return owner
    }

    override fun getCreator(): UUID {
        return creator
    }

    override fun getMembers(): List<UUID> {
        return members
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