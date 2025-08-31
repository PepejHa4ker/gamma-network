package com.pepej.gammanetwork.metadata

import com.pepej.gammanetwork.messages.CHAT
import com.pepej.gammanetwork.module.LAST_PM
import com.pepej.gammanetwork.module.SPY

object SharedMetadataRegistrar {

    fun register() {
        SharedMetadata.register(CHAT)
        SharedMetadata.register(SPY)
        SharedMetadata.register(LAST_PM)
    }
}