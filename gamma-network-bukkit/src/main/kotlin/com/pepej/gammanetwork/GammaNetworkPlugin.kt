package com.pepej.gammanetwork

import com.pepej.gammanetwork.commands.NetworkCommands
import com.pepej.gammanetwork.commands.registry.Arguments
import com.pepej.gammanetwork.config.ChatConfiguration
import com.pepej.gammanetwork.config.GammaNetworkConfiguration
import com.pepej.gammanetwork.config.RconConfiguration
import com.pepej.gammanetwork.messenger.GammaNetworkMessenger
import com.pepej.gammanetwork.messenger.redis.Redis
import com.pepej.gammanetwork.messenger.redis.RedisCredentials
import com.pepej.gammanetwork.messenger.redis.RedisProvider
import com.pepej.gammanetwork.module.ModuleManager
import com.pepej.gammanetwork.network.GammaNetwork
import com.pepej.gammanetwork.rcon.RconServer
import com.pepej.gammanetwork.redirect.GammaNetworkRedirectSystem
import com.pepej.gammanetwork.redirect.GammaNetworkRequestHandler
import com.pepej.gammanetwork.redirect.RedirectNetworkMetadataParameterProvider
import com.pepej.gammanetwork.redirect.VelocityPlayerRedirector
import com.pepej.papi.ap.Plugin
import com.pepej.papi.command.Commands
import com.pepej.papi.messaging.InstanceData
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.network.Network
import com.pepej.papi.network.event.ServerDisconnectEvent
import com.pepej.papi.network.modules.DispatchModule
import com.pepej.papi.network.modules.FindCommandModule
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.plugin.PapiJavaPlugin
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import org.slf4j.LoggerFactory

const val VERSION: Int = 8

@Plugin(
    name = "gamma-network",
    version = "1.1.1",
    authors = ["pepej"],
    hardDepends = [
        "papi",
    ]
)
class GammaNetworkPlugin : PapiJavaPlugin(), RedisProvider, InstanceData {


    private lateinit var _redis: Redis

    private val log = LoggerFactory.getLogger(GammaNetwork::class.java)

    override val redis: Redis
        get() {
            return _redis
        }

    override fun getRedis(credentials: RedisCredentials): Redis {
        return GammaNetworkMessenger(credentials)
    }

    private lateinit var _globalCredentials: RedisCredentials

    override val globalCredentials: RedisCredentials
        get() {
            return _globalCredentials
        }

    companion object {
        lateinit var instance: GammaNetworkPlugin
    }

    lateinit var serverId: String
    lateinit var network: GammaNetwork
    lateinit var configuration: GammaNetworkConfiguration
    override fun onPluginLoad() {
        instance = this
        val config = loadConfig("config.yml")
        serverId = config.getString("server_id")
        val chatConfig = ChatConfiguration(
            enable = config.getBoolean("chat.enable"),
            enableSplitting = config.getBoolean("chat.enable-splitting"),
            localChatRadius = config.getInt("chat.local-server-chat-radius"),
            localFormat = ChatConfiguration.Format(config.getString("chat.local-chat-format")),
            globalFormat = ChatConfiguration.Format(config.getString("chat.global-chat-format")),
            adminMessageColor = ChatConfiguration.Format(config.getString("chat.admin-message-color")),
        )
        val rconConfig = RconConfiguration(
            config.getBoolean("rcon.enabled"),
            config.getString("rcon.password"),
            config.getInt("rcon.port"),
            config.getBoolean("rcon.whitelist"),
            config.getStringList("rcon.whitelisted")
        )
        configuration = GammaNetworkConfiguration(
            chatConfig,
            rconConfig
        )
        saveResource("logback.xml", true)


    }

    override fun onPluginEnable() {

        _globalCredentials = RedisCredentials.fromConfig(config)
        _redis = getRedis(_globalCredentials)

        provideService(RedisProvider::class.java, this@GammaNetworkPlugin)
        provideService(RedisCredentials::class.java, _globalCredentials)
        provideService(Messenger::class.java, redis)
        provideService(LuckPerms::class.java, LuckPermsProvider.get())
        network = GammaNetwork(redis, this@GammaNetworkPlugin)
        provideService(Network::class.java, network)
        val ensureJoinedViaQueue = config.getBoolean("queue.ensure-joined-via")
        val connectionTimeout = config.getLong("queue.connection-timeout")
        val redirectSystem = GammaNetworkRedirectSystem(
            redis,
            this@GammaNetworkPlugin, VelocityPlayerRedirector,
            connectionTimeout
        )

        redirectSystem.addDefaultParameterProvider(RedirectNetworkMetadataParameterProvider)
        redirectSystem.setHandler(GammaNetworkRequestHandler)
        redirectSystem.setEnsure(ensureJoinedViaQueue)
        provideService(
            RedirectSystem::class.java,
            redirectSystem
        )

        redis.bindWith(this@GammaNetworkPlugin)
        // initialization
        Arguments.apply {
            profile(Commands.parserRegistry(), network)
            server(Commands.parserRegistry(), network)
        }
        bindModule(FindCommandModule(network, arrayOf("find")))
        bindModule(DispatchModule(redis, this@GammaNetworkPlugin, arrayOf("dispatch", "exec")))
        bindModule(NetworkCommands)
        bindModule(ModuleManager)
        if (configuration.rcon.enable) {
                val rconServer = RconServer(server, configuration.rcon)
                val channelFuture = rconServer.bind(configuration.rcon.port)
                val channel = channelFuture.awaitUninterruptibly().channel()
                bindModule(rconServer)
                if (!channel.isActive) {
                    log.warn("Failed to bind Rcon. Address already in use? ({})", configuration.rcon.port)
                } else {
                    log.info("Rcon server started!")
                }

        }

    }

    override fun onPluginDisable() {
        network.postEvent(
            ServerDisconnectEvent(
                serverId, "stopping"
            )
        )
    }

    override fun getId(): String {
        return serverId
    }

    override fun getGroups(): MutableSet<String> {
        return mutableSetOf()
    }
}
