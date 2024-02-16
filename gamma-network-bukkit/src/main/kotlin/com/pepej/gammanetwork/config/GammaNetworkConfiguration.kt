package com.pepej.gammanetwork.config


data class ChatConfiguration(
    val enableSplitting: Boolean,
    val enable: Boolean,
    val localChatRadius: Int,
    val localFormat: Format,
    val globalFormat: Format,
    val adminMessageColor: Format,
) {
    @JvmInline
    value class Format(val format: String)
}

data class RconConfiguration(
    val password: String,
    val port: Int = 25566,
    val whitelist: Boolean = true,
    val whiteListedHosts: List<String> = mutableListOf()

)

data class GammaNetworkConfiguration(
    val chat: ChatConfiguration,
    val rcon: RconConfiguration
)