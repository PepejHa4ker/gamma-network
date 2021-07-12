package com.pepej.utils

import com.google.common.reflect.TypeToken
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.metadata.Metadata
import com.pepej.papi.metadata.MetadataKey
import org.bukkit.entity.Player

inline fun <reified T> typeTokenOf() = object : TypeToken<T>() {}

fun Player.metadata() = Metadata.provideForPlayer(this)
inline fun <reified T> Messenger.getChannel(channel: String) = this.getChannel(channel, typeTokenOf<T>())
