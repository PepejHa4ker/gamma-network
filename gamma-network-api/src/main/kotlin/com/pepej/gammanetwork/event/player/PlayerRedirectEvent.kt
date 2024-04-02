package com.pepej.gammanetwork.event.player

import com.google.gson.JsonElement
import com.pepej.papi.network.event.NetworkEvent
import com.pepej.papi.profiles.Profile


data class ProfileRedirectEvent(
    val server: String,
    val profile: Profile,
    val params: Map<String, JsonElement>,
    var allowed: Boolean = true
) : NetworkEvent