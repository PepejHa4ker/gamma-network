package com.pepej.gammanetwork.messages

import com.pepej.papi.metadata.MetadataKey


val CHAT: MetadataKey<ChatType> = MetadataKey.create("chat", ChatType::class.java)

enum class ChatType {
    NOT_PRESENT,
    GLOBAL,
    ADMIN;


}

