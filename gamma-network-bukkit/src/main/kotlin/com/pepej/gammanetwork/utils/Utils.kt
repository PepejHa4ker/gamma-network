package com.pepej.gammanetwork.utils

import com.google.common.reflect.TypeToken
import com.pepej.papi.command.argument.Argument
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.network.Network
import com.pepej.papi.profiles.Profile
import com.pepej.papi.services.Services
import com.pepej.papi.shadow.bukkit.player.CraftPlayerShadow
import com.pepej.papi.utils.Players
import org.bukkit.entity.Player


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
fun Player.metadata() = Metadata.provideForPlayer(this)
fun Player.asCraft(): CraftPlayerShadow = CraftPlayerShadow.create(this)