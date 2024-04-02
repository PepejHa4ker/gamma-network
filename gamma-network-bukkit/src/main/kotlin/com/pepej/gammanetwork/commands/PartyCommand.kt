package com.pepej.gammanetwork.commands

//object PartyCommand : TerminableModule {
//    override fun setup(consumer: TerminableConsumer) {
//        Commands.create()
//            .assertPlayer()
//            .assertUsage("<player>")
//            .tabHandler {
//                instance.network.onlinePlayers.values.map { it.name.get() }
//
//            }
//            .handler {
//                val profile = it.arg(0).parseOrFail<Profile>()
//                if (Parties.getParty(profile.uniqueId) != null) {
////                    throw CommandInterruptException("&cИгрок уже состоит в пати")
//                }
//
//                val owner = it.sender().uniqueId
//                if (owner == profile.uniqueId) {
////                    throw CommandInterruptException("&cВы не можете пригласить себя")
//                }
//
//                val party = MultiServerParty(owner, owner)
//                party.addMember(profile.uniqueId)
//                Parties.createParty(party)
//
//            }
//            .registerAndBind(consumer, "pinvite")
//        Commands.create()
//            .assertPlayer()
//            .handler {
//                for (party in Parties.getParties()) {
//                    it.reply("Owner : ${Players.getNullable(party.getOwner())?.name} with ${party.getMembers().size} members")
//                }
//
//            }
//            .registerAndBind(consumer, "parties")
//
//        Commands.create()
//            .assertPlayer()
//            .assertUsage("<player>")
//            .tabHandler {
//                instance.network.onlinePlayers.values.map { it.name.get() }
//
//            }
//            .handler {
//                val profile = it.arg(0).parseOrFail<Profile>()
//                val senderParty = Parties.getParty(it.sender().uniqueId) ?: return@handler
//                if (senderParty.isOwner(it.sender().uniqueId)) {
//                    Parties.addMember(senderParty, profile.uniqueId)
//                }
//            }
//            .registerAndBind(consumer, "padd")
//    }

//}