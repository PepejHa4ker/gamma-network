//package com.pepej.gammanetwork.commands
//
//import com.pepej.gammanetwork.GammaNetwork
//import com.pepej.gammanetwork.messenger.GammaChatNetwork
//import com.pepej.gammanetwork.utils.asCraft
//import com.pepej.papi.command.CommandInterruptException
//import com.pepej.papi.command.Commands
//import com.pepej.papi.command.functional.FunctionalCommandBuilder
//import com.pepej.papi.profiles.Profile
//import com.pepej.papi.terminable.TerminableConsumer
//import com.pepej.papi.terminable.module.TerminableModule
//import com.pepej.papi.text.Text.colorize
//import com.pepej.papi.utils.Players
//import com.pepej.papi.utils.TabHandlers
//import kotlinx.serialization.*
//import kotlinx.serialization.descriptors.PrimitiveKind
//import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
//import kotlinx.serialization.descriptors.SerialDescriptor
//import kotlinx.serialization.encoding.Decoder
//import kotlinx.serialization.encoding.Encoder
//import kotlinx.serialization.json.Json
//import org.bukkit.entity.Player
//import java.util.*
//
//object TwinksCommand : TerminableModule {
//
//    override fun setup(consumer: TerminableConsumer) {
//        Commands.create()
//            .assertPlayer()
//            .assertUsage("[player]")
//
//            .assertPermission("twinks.command")
//            .tabHandler { ctx ->
//                TabHandlers.players(ctx.args().joinToString().toLowerCase())
//            }
//            .handler {
//                val target = if (it.args().isEmpty()) {
//                    Profile.create(it.sender().uniqueId, it.sender().name, it.sender().asCraft().profile)
//                } else {
//                    if (!it.sender().hasPermission("twinks.command.other")) {
//                        throw CommandInterruptException(FunctionalCommandBuilder.DEFAULT_NO_PERMISSION_MESSAGE)
//                    }
//                    it.arg(0).parseOrFail(Profile::class.java)
//                }
//                val twinks = getTwinksInfo(target) ?: return@handler
//                sendTwinksInfo(it.sender(), twinks)
//            }
//            .registerAndBind(consumer, "twinks")
//    }
//
//    private fun sendTwinksInfo(to: Player, twinks: List<Twink>) {
//        val info = StringBuilder().apply {
//            append("&aОнлайн &cЗабанен &7Оффлайн")
//            append("\n")
//        }
//
//        for (twink in twinks) {
//            val color = if (twink.ban.banned) "&c" else if (Players.get(twink.uuid).isPresent) "&a" else "&7"
//            info
//                .append("${color + twink.username} &7(&eIp: &7${twink.ip} &eOffences: &7${twink.offences})")
//            if (to.hasPermission("twinks.command.full")) {
//                info.append("\n")
//                    .append("&eUid: &7${twink.uuid} &eHwid: &7: ${twink.machineId}")
//            }
//            info.append("\n")
//        }
//        to.sendMessage(colorize(info.toString()))
//    }
//
//
//    private fun getTwinksInfo(p: Profile): List<Twink>? {
//        val profile = p.profile ?: return null
//        val twinksProperties = profile.properties["twinks"]
//        if (twinksProperties != null && twinksProperties.isNotEmpty()) {
//            val twinksProperty = twinksProperties.first()
//            val value = twinksProperty.value
//            if (value.isNotEmpty()) {
//                val raw = String(Base64.getDecoder().decode(value));
//                return Json { ignoreUnknownKeys = true }.decodeFromString(raw)
//            }
//        }
//        return null
//    }
//
//
//    @Serializable
//    data class Twink(
//        val username: String,
//        val ip: String,
//        @Serializable(with = UUIDSerializer::class)
//        val uuid: UUID,
//        @Serializable(with = UUIDSerializer::class)
//        val userId: UUID,
//        @Serializable(with = UUIDSerializer::class)
//        val machineId: UUID,
//        val offences: Int,
//        @SerialName("banned")
//        val ban: Ban
//
//    )
//
//    @Serializable
//    data class Ban(
//        @SerialName("secs_since_epoch")
//        val bannedUntil: Long,
//        @Transient
//        val banned: Boolean = System.currentTimeMillis() / 1000L < bannedUntil
//    )
//
//
//    private object UUIDSerializer : KSerializer<UUID> {
//        override fun deserialize(decoder: Decoder): UUID {
//            return UUID.fromString(decoder.decodeString())
//        }
//
//        override val descriptor: SerialDescriptor
//            get() = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
//
//        override fun serialize(encoder: Encoder, value: UUID) {
//            encoder.encodeString(value.toString())
//        }
//
//    }
//}