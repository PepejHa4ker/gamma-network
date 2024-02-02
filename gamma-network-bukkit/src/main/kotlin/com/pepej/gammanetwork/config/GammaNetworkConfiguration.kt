package com.pepej.gammanetwork.config

import org.bukkit.configuration.ConfigurationSection


data class ChatConfiguration(
    val enableSplitting: Boolean,
    val localChatRadius: Int,
    val localFormat: Format,
    val globalFormat: Format,
    val adminMessageColor: Format,
) {
    @JvmInline
    value class Format(val format: String)
}
data class GammaNetworkConfiguration(
    val chat: ChatConfiguration
)