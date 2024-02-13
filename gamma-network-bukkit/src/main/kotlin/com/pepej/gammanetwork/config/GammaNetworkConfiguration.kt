package com.pepej.gammanetwork.config


data class ChatConfiguration(
    val enableSplitting: Boolean,
    val enable: Boolean,
    val localChatRadius: Int,
    val localFormat: Format,
    val globalFormat: Format,
//    val genericFormat: Format,
    val adminMessageColor: Format,
) {
    @JvmInline
    value class Format(val format: String)
}
data class GammaNetworkConfiguration(
    val chat: ChatConfiguration
)