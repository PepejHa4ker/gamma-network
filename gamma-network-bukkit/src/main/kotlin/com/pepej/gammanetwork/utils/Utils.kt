package com.pepej.gammanetwork.utils

import com.google.common.reflect.TypeToken
import com.pepej.gammanetwork.rcon.RconCommandSender
import com.pepej.gammanetwork.redirect.GammaNetworkRedirectSystem
import com.pepej.papi.command.argument.Argument
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.messaging.conversation.ConversationChannel
import com.pepej.papi.messaging.conversation.ConversationMessage
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.network.Network
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.profiles.Profile
import com.pepej.papi.services.Services
import com.pepej.papi.shadow.bukkit.player.CraftPlayerShadow
import com.pepej.papi.text.Text
import com.pepej.papi.utils.Players
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.util.*


const val GAMMA_RED: String = "&7[&cGamma&7]"
const val GAMMA_GREEN: String = "&7[&aGamma&7]"
const val REDIRECT_TOKEN = "redirect-token"
val EMPTY_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

inline fun <reified T> Messenger.getChannel(channel: String) = this.getChannel(channel, T::class.java)
fun Player.asProfile(network: Network): Profile? {
    return network.onlinePlayers[this.uniqueId]
}

inline fun <reified T> Argument.parseOrFail(): T {
    return this.parseOrFail(T::class.java)
}

inline fun <reified T> getServiceUnchecked(): T {
    return Services.getNullable(T::class.java)!!
}

inline fun <reified T> typeTokenOf() = object : TypeToken<T>() {}

fun broadcast(message: String) {
    Players.stream()
        .filter { it.hasPermission("network.status.alerts") }
        .forEach { msg(it, "$GAMMA_GREEN &r$message") }
}

fun msg(sender: CommandSender, msg: String) {
    sender.sendMessage(Text.colorize(msg))
}


data class UUIDAndName(val uuid: UUID, val name: String)

fun CommandSender.wrapAsPlayer(): UUIDAndName =
    when (this) {
        is Player -> UUIDAndName(uniqueId, displayName)
        is ConsoleCommandSender -> UUIDAndName(EMPTY_UUID, "Gamma")
        is RconCommandSender -> UUIDAndName(EMPTY_UUID, "RCon")
        else -> throw IllegalArgumentException("Unexpected sender")
}

infix fun Location.distance(other: Location): Double {
    return this.distance(other)
}

val RedirectSystem.Request.targetServer: String
    get() {
        return (this as GammaNetworkRedirectSystem.RequestMessage).targetServer
    }


inline fun <reified T, reified R> Messenger.getConversationChannel(channel: String): ConversationChannel<T, R>
        where T : ConversationMessage, R : ConversationMessage {
    return this.getConversationChannel(channel, T::class.java, R::class.java)
}

fun CommandSender.metadata() = Metadata.provideForPlayer(wrapAsPlayer().uuid)
fun Player.asCraft(): CraftPlayerShadow = CraftPlayerShadow.create(this)