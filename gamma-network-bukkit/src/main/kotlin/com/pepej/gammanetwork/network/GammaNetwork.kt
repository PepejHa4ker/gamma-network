package com.pepej.gammanetwork.network

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.gson.JsonElement
import com.pepej.papi.cooldown.Cooldown
import com.pepej.papi.event.bus.api.EventBus
import com.pepej.papi.event.bus.api.EventSubscriber
import com.pepej.papi.event.bus.api.PostResult
import com.pepej.papi.event.bus.api.SimpleEventBus
import com.pepej.papi.events.Events
import com.pepej.papi.messaging.InstanceData
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.network.Network
import com.pepej.papi.network.Server
import com.pepej.papi.network.event.NetworkEvent
import com.pepej.papi.network.event.ServerConnectEvent
import com.pepej.papi.network.event.ServerDisconnectEvent
import com.pepej.papi.network.event.ServerStatusEvent
import com.pepej.papi.network.metadata.ServerMetadataProvider
import com.pepej.papi.network.metadata.TpsMetadataProvider
import com.pepej.papi.profiles.Profile
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.terminable.composite.CompositeTerminable
import com.pepej.papi.text.Text
import com.pepej.papi.utils.Log
import com.pepej.papi.utils.Players
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.math.abs

open class GammaNetwork(protected val messenger: Messenger, protected val instanceData: InstanceData) : Network {
    private val compositeTerminable: CompositeTerminable = CompositeTerminable.create()
    private val eventBus: EventBus<NetworkEvent> = SimpleEventBus(NetworkEvent::class.java)
    private val log = LoggerFactory.getLogger(GammaNetwork::class.java)
    private val eventsChannel = messenger.getChannel("pnet-events", EventMessage::class.java).apply {
        newAgent { _, message ->
            when (message.type) {
                "connect" -> postEvent(
                    ServerConnectEvent(
                        message.id,
                        handleIncomingStatusMessage(message.status)
                    )
                )

                "disconnect" -> if (instanceData.id != message.id) {
                    postEvent(
                        ServerDisconnectEvent(
                            message.id,
                            message.reason
                        )
                    )
                }
            }
        }.bindWith(compositeTerminable)
    }

    private val statusChannel = messenger.getChannel("pnet-status", StatusMessage::class.java)

    private val metadataProviders: MutableList<ServerMetadataProvider> = CopyOnWriteArrayList()
    private val servers: MutableMap<String, ServerImpl> = ConcurrentHashMap<String, ServerImpl>()

    init {
        val disconnectListener: EventSubscriber<ServerDisconnectEvent> =
            object : EventSubscriber<ServerDisconnectEvent> {
                override fun invoke(event: ServerDisconnectEvent) {
                    log.info("Server disconnected: $event")
                    if (event.id == instanceData.id) {
                        val message = EventMessage(
                            id = event.id,
                            type = "disconnect",
                            reason = event.reason ?: "stopping",
                            status = produceStatusMessage()
                        )

                        eventsChannel.sendMessage(message)
                        eventBus.unregister(this)
                    }
                }
            }
        eventBus.register(ServerDisconnectEvent::class.java, disconnectListener)
        registerMetadataProviders()
        val connectionMessage = EventMessage(
            id = instanceData.id,
            type = "connect",
            status = produceStatusMessage(),
            reason = "init"
        )
        eventsChannel.sendMessage(connectionMessage)
        statusChannel.newAgent { _, message -> handleIncomingStatusMessage(message) }
            .bindWith(this.compositeTerminable)
        Schedulers.builder().async().afterAndEvery(3L, TimeUnit.SECONDS).run {
            val msg = produceStatusMessage()
            statusChannel.sendMessage(msg)
        }.bindWith(this.compositeTerminable)


        Events.merge(PlayerEvent::class.java, PlayerKickEvent::class.java, PlayerQuitEvent::class.java)
            .handler {
                var message = produceStatusMessage()
                message = message.copy(players = message.players.toMutableMap().apply {
                    remove(it.player.uniqueId)
                })
                statusChannel.sendMessage(
                    message
            )
            }
            .bindWith(this.compositeTerminable)

        Events.subscribe(PlayerJoinEvent::class.java)
            .handler {
                var message = produceStatusMessage()
                message = message.copy(players = message.players.toMutableMap().apply {
                    put(it.player.uniqueId, it.player.name)
                })
                statusChannel.sendMessage(message)
            }
            .bindWith(this.compositeTerminable)

    }

    private fun registerMetadataProviders() {
        log.info("Registering metadata providers")
        this.registerMetadataProvider(TpsMetadataProvider.INSTANCE)
    }

     fun postEvent(event: NetworkEvent) {
        try {
            eventBus.post(event).raise()
        } catch (var3: PostResult.CompositeException) {
            throw RuntimeException(var3)
        }
    }

    private fun produceStatusMessage(): StatusMessage {
        log.debug("Producing status message")
        val players = mutableMapOf<UUID, String>().apply {
            Players.forEach { put(it.uniqueId, it.name) }
        }

        val metadata = mutableMapOf<String, JsonElement>().apply {
            metadataProviders.forEach { provider ->
                for (serverMetadata in provider.provide()) {
                    put(serverMetadata.key(), serverMetadata.data())

                }
            }
        }


        val msg = StatusMessage(
            id = instanceData.id,
            groups = ArrayList(instanceData.groups),
            time = System.currentTimeMillis(),
            players = players,
            maxPlayers = Bukkit.getMaxPlayers(),
            whitelisted = Bukkit.hasWhitelist(),
            metadata = metadata

        )

        log.debug("Produced message {}", msg)
        return msg
    }

    private fun handleIncomingStatusMessage(message: StatusMessage): ServerImpl {
        log.debug("Handling incoming status message {}", message)
        val server = servers.computeIfAbsent(message.id) { id: String -> ServerImpl(id) }
        server.loadData(message)
        this.postEvent(ServerStatusEvent(server))
        return server
    }

    override fun getServers(): Map<String, Server> {
        return Collections.unmodifiableMap<String, Server>(this.servers)
    }

    override fun getServer(id: String): Server? {
        return servers[id]
    }

    override fun getOnlinePlayers(): Map<UUID, Profile> {
        val players: MutableMap<UUID?, Profile> = mutableMapOf()
        for (server in servers.values) {
            players.putAll(server.onlinePlayers)
        }
        return Collections.unmodifiableMap(players)
    }

    override fun getOverallPlayerCount(): Int {
        return servers.values.stream().mapToInt { it.onlinePlayers.size }
            .sum()
    }

    override fun registerMetadataProvider(metadataProvider: ServerMetadataProvider) {
        metadataProviders.add(metadataProvider)
    }

    override fun getEventBus(): EventBus<NetworkEvent> {
        return this.eventBus
    }

    override fun close() {
        compositeTerminable.closeAndReportException()
    }
    private data class EventMessage(
        val id: String,
        val type: String,
        val status: StatusMessage,
        val reason: String,
    )

    private data class StatusMessage(
        val id: String,
        val groups: List<String>,
        val time: Long = 0,
        val players: Map<UUID, String>,
        val maxPlayers: Int = 0,
        val whitelisted: Boolean = false,
        val metadata: Map<String, JsonElement>,
    )


    private data class ServerImpl(private val id: String) : Server {
        private var lastPing = 0L
        private var groups: Set<String> = ImmutableSet.of()
        private var players: Map<UUID, Profile> = ImmutableMap.of()
        private var maxPlayers = 0
        private var whitelisted = false
        private var metadata: Map<String, JsonElement>? = null
        private val timeSyncWarningCooldown = Cooldown.of(5L, TimeUnit.SECONDS)

        private fun checkTimeSync(messageTimestamp: Long) {
            val systemTime = System.currentTimeMillis()
            val timeDifference = abs((systemTime - messageTimestamp).toDouble()).toLong()
            if (timeDifference > TIME_SYNC_THRESHOLD && timeSyncWarningCooldown.testAndReset()) {
                Log.warn("[network] Server %s' appears to have a system time difference of %s ms. time now = %s, message timestamp = %s - Check NTP is running? Is network stable?",
                    *arrayOf<Any>(
                        this.id, timeDifference, systemTime, messageTimestamp
                    )
                )
            }
        }

        fun loadData(msg: StatusMessage) {
            this.checkTimeSync(msg.time)
            this.lastPing = msg.time
            this.groups = ImmutableSet.copyOf(msg.groups)
            val players = ImmutableMap.builder<UUID, Profile>()
            for ((k, v) in msg.players.entries) {
                players.put(k, Profile.create(k, v))
            }


            this.players = players.build()
            this.maxPlayers = msg.maxPlayers
            this.whitelisted = msg.whitelisted
            this.metadata = ImmutableMap.copyOf(msg.metadata)
        }

        override fun getId(): String {
            return this.id
        }

        override fun getGroups(): Set<String> {
            return this.groups
        }

        override fun isOnline(): Boolean {
            val diff = System.currentTimeMillis() - this.lastPing
            return diff < TimeUnit.SECONDS.toMillis(5L)
        }

        override fun getLastPing(): Long {
            return this.lastPing
        }

        override fun broadcast(message: String) {
            Players.forEach { p: Player ->
                p.sendMessage(Text.colorize(message))
            }
        }

        override fun getOnlinePlayers(): Map<UUID, Profile> {
            return (if (!this.isOnline) ImmutableMap.of() else this.players)
        }

        override fun getMaxPlayers(): Int {
            return if (!this.isOnline) 0 else this.maxPlayers
        }

        override fun isWhitelisted(): Boolean {
            return this.whitelisted
        }

        override fun getRawMetadata(): Map<String, JsonElement>? {
            return (if (!this.isOnline) ImmutableMap.of() else this.metadata)
        }

        companion object {
            private val TIME_SYNC_THRESHOLD: Long = TimeUnit.SECONDS.toMillis(2L)
        }
    }
}