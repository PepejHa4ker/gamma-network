package com.pepej.gammanetwork.messages

import com.pepej.gammanetwork.party.LocalServerParty
import com.pepej.gammanetwork.party.Parties
import com.pepej.gammanetwork.utils.getConversationChannel
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.messaging.conversation.ConversationMessage
import com.pepej.papi.messaging.conversation.ConversationReply
import com.pepej.papi.messaging.conversation.ConversationReplyListener
import com.pepej.papi.network.Server
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.module.TerminableModule
import java.util.*
import java.util.concurrent.TimeUnit

object PartiesMessageSystem : TerminableModule {

    private val messenger: Messenger = getServiceUnchecked()
    private val redirectSystem: RedirectSystem = getServiceUnchecked()
    private val channel = messenger.getConversationChannel<PartyRequest, PartyResponse>("network-parties")
    private val agent = channel.newAgent().apply {
        addListener { _, request ->
            val handlerResp = request.action.handleRequest(request)
            ConversationReply.of(handlerResp)

        }
    }

    data class PartyRequest(
        private val conversationId: UUID = UUID.randomUUID(),
        val partyId: UUID,
        val actor: UUID,
        val action: PartyAction,
        val data: PartyActionData
    ) : ConversationMessage {
        override fun getConversationId(): UUID = conversationId

    }

    data class PartyResponse(
        private val conversationId: UUID = UUID.randomUUID(),
        val partyId: UUID
    ) : ConversationMessage {
        override fun getConversationId(): UUID = conversationId

    }

    override fun setup(consumer: TerminableConsumer) {
        agent.bindWith(consumer)

        TODO("Not yet implemented")
    }

    sealed class PartyAction {

        abstract fun handleRequest(request: PartyRequest): PartyResponse?
    }

    object CreatePartyAction : PartyAction() {
        override fun handleRequest(request: PartyRequest): PartyResponse? {
            val party = LocalServerParty(
                request.partyId,
                creator = request.actor,
                owner = request.actor,
            )
            Parties.createParty(party)
            return PartyResponse(partyId = party.id)
        }
    }

    class AddNewMemberPartyAction(val memberId: UUID) : PartyAction() {
        override fun handleRequest(request: PartyRequest): PartyResponse? {
            val party = Parties.getParty(request.partyId) ?: return null
            party.addMember(memberId)
            return PartyResponse(partyId = party.id)
        }
    }

    class RemoveMemberPartyAction(val memberId: UUID) : PartyAction() {
        override fun handleRequest(request: PartyRequest): PartyResponse? {
            val party = Parties.getParty(request.partyId) ?: return null
            party.removeMember(memberId)
            return PartyResponse(partyId = party.id)
        }
    }

    class ChangeServerPartyAction(private val targetServer: Server) : PartyAction() {
        override fun handleRequest(request: PartyRequest): PartyResponse? {
            val party = Parties.getParty(request.partyId) ?: return null
            party.onOwnerServerChange(targetServer)
            return PartyResponse(partyId = party.id)
        }
    }

    class PartyActionData(val memberId: UUID)

    fun sendPartyRequest(partyId: UUID, actor: UUID, action: PartyAction, data: PartyActionData) {
        val request = PartyRequest(UUID.randomUUID(), actor, partyId, action, data)
        channel.sendMessage(request, object : ConversationReplyListener<PartyResponse> {
            override fun onReply(resp: PartyResponse): ConversationReplyListener.RegistrationAction {
                return ConversationReplyListener.RegistrationAction.STOP_LISTENING

            }

            override fun onTimeout(replies: List<PartyResponse>) {
                TODO("Not yet implemented")
            }

        }, 5, TimeUnit.SECONDS)
    }


}


