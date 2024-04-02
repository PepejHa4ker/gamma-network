package com.pepej.gammanetwork.party

import com.pepej.gammanetwork.GammaNetworkPlugin.Companion.instance
import com.pepej.gammanetwork.utils.getChannel
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.events.Events
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.concurrent.TimeUnit

data class PartyCreateMessage(val party: MultiServerParty)
data class PartyRemoveMessage(val party: MultiServerParty)
data class AddMemberMessage(val party: MultiServerParty, val member: UUID)
data class RemoveMemberMessage(val party: MultiServerParty, val member: UUID)

object Parties : TerminableModule {

    private val messenger: Messenger = getServiceUnchecked()
    private val parties: MutableList<Party> = mutableListOf()
    private val createPartyChannel = messenger.getChannel<PartyCreateMessage>("parties-create")
        .also {
            it.newAgent { _, message ->
                println("Received $message")
                this.parties.add(message.party)

            }
        }
    private val removePartyChannel = messenger.getChannel<PartyRemoveMessage>("parties-remove")
        .also {
            it.newAgent { _, message ->
                println("Received $message")
                this.parties.remove(message.party)

            }
        }


    private val addMemberChannel = messenger.getChannel<AddMemberMessage>("parties-add-member")
        .also {
            it.newAgent { _, message ->
                println("Received $message")
                this.getParty(message.member)?.addMember(message.member)
            }
        }
    private val removeMemberChannel = messenger.getChannel<RemoveMemberMessage>("parties-rm-member")
        .also {
            it.newAgent { _, message ->
                println("Received $message")
                this.getParty(message.member)?.removeMember(message.member)

            }
        }


    fun getParties(): List<Party> {
        return this.parties.toList()
    }

    fun createParty(party: Party) {
        if (party is MultiServerParty) {
            createPartyChannel.sendMessage(PartyCreateMessage(party))
        } else {
            this.parties.add(party)
        }
    }

    fun removeParty(party: Party) {
        if (party is MultiServerParty) {
            removePartyChannel.sendMessage(PartyRemoveMessage(party))
        } else {
            this.parties.remove(party)
        }
    }

    fun addMember(party: Party, uuid: UUID) {
        if (party is MultiServerParty) {
            addMemberChannel.sendMessage(AddMemberMessage(party, uuid))
        } else {
            party.addMember(uuid)
        }
    }

    fun removeMember(party: Party, uuid: UUID) {
        if (party is MultiServerParty) {
            removeMemberChannel.sendMessage(RemoveMemberMessage(party, uuid))
        } else {
            party.removeMember(uuid)
        }
    }

    fun getParty(id: UUID): Party? {
        return this.parties.find { id in it.members }
    }


    override fun setup(consumer: TerminableConsumer) {
        Events.subscribe(PlayerQuitEvent::class.java)
            .handler {
                val party = getParty(it.player.uniqueId) ?: return@handler
                if (party is MultiServerParty) {
                    Schedulers.async().runLater({
                        if (!instance.network.onlinePlayers.containsKey(it.player.uniqueId)) {
                            removeMemberChannel.sendMessage(RemoveMemberMessage(party, it.player.uniqueId))

                        }
                    }, 5, TimeUnit.SECONDS)
                } else {
                    party.removeMember(it.player.uniqueId)
                }

            }
            .bindWith(consumer)
    }
}