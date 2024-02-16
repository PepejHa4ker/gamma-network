package com.pepej.gammanetwork.utils

import com.google.common.reflect.TypeToken
import com.pepej.papi.command.argument.Argument
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.messaging.conversation.ConversationChannel
import com.pepej.papi.messaging.conversation.ConversationMessage
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.network.Network
import com.pepej.papi.profiles.Profile
import com.pepej.papi.services.Services
import com.pepej.papi.shadow.bukkit.player.CraftPlayerShadow
import com.pepej.papi.utils.Players
import org.bukkit.Location
import org.bukkit.entity.Player


const val GAMMA_RED: String = "&7[&cGamma&7]"
const val GAMMA_GREEN: String = "&7[&aGamma&7]"

inline fun <reified T> Messenger.getChannel(channel: String) = this.getChannel(channel, T::class.java)
fun Profile.getPlayer(): Player? {
    return Players.getNullable(this.uniqueId)
}

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

infix fun Location.distance(other: Location): Double {
    return this.distance(other)
}



inline fun <reified T, reified R> Messenger.getConversationChannel(channel: String): ConversationChannel<T, R>
        where T : ConversationMessage, R : ConversationMessage {
    return this.getConversationChannel(channel, T::class.java, R::class.java)
}

fun Player.metadata() = Metadata.provideForPlayer(this.uniqueId)
fun Player.asCraft(): CraftPlayerShadow = CraftPlayerShadow.create(this)