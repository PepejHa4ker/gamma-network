package com.pepej.gammachat.messenger

import com.pepej.papi.events.Events
import com.pepej.papi.messaging.bungee.BungeeCord
import com.pepej.papi.messaging.util.ChannelReceiver
import com.pepej.papi.network.AbstractNetwork
import com.pepej.papi.network.Server
import com.pepej.papi.network.event.ServerDisconnectEvent
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.services.Services
import org.bukkit.event.server.PluginDisableEvent
import java.util.concurrent.TimeUnit

class GammaChatNetwork(messenger: GammaChatMessenger) : AbstractNetwork(messenger, messenger)
