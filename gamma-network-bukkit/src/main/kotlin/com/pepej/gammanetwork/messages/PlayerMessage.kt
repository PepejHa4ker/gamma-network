package com.pepej.gammanetwork.messages

import java.util.*


sealed interface Message
data class ServerMessage(val uuid: UUID, val displayName: String, val message: String) : Message
data class PlayerMessage(val uuid: UUID, val displayName: String, val message: String, val server: String) : Message
