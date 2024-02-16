package com.pepej.gammanetwork

import com.pepej.gammanetwork.commands.NetworkCommands
import com.pepej.gammanetwork.commands.Ping
import com.pepej.gammanetwork.commands.registry.Arguments
import com.pepej.gammanetwork.config.ChatConfiguration
import com.pepej.gammanetwork.config.GammaNetworkConfiguration
import com.pepej.gammanetwork.config.RconConfiguration
import com.pepej.gammanetwork.messages.AdminChatMessageSystem
import com.pepej.gammanetwork.messages.GlobalChatMessageSystem
import com.pepej.gammanetwork.messages.PrivateMessageSystem
import com.pepej.gammanetwork.messenger.GammaNetworkMessenger
import com.pepej.gammanetwork.messenger.redis.Kreds
import com.pepej.gammanetwork.messenger.redis.KredsCredentials
import com.pepej.gammanetwork.messenger.redis.KredsProvider
import com.pepej.gammanetwork.module.NetworkStatusModule
import com.pepej.gammanetwork.module.NetworkSummaryModule
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
import com.pepej.papi.network.modules.DispatchModule
import com.pepej.papi.network.modules.FindCommandModule
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.plugin.PapiJavaPlugin
import kotlinx.coroutines.*
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import org.slf4j.LoggerFactory


@Plugin(
    name = "gamma-network",
    version = "1.1.1",
    authors = ["pepej"],
    hardDepends = [
        "papi",
    ]
)
class GammaNetwork : PapiJavaPlugin(), KredsProvider, InstanceData {


    private lateinit var _redis: Kreds

    private val log = LoggerFactory.getLogger(GammaNetwork::class.java)

    override val redis: Kreds
        get() {
            return _redis
        }

    override suspend fun getKreds(credentials: KredsCredentials): Kreds {
//        return runBlocking {
        return GammaNetworkMessenger.create(credentials)
//
    }

    private lateinit var _globalCredentials: KredsCredentials

    override val globalCredentials: KredsCredentials
        get() {
            return _globalCredentials
        }

    companion object {
        lateinit var instance: GammaNetwork
    }

    lateinit var serverId: String
    lateinit var network: Network
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
            config.getString("rcon.password"),
            config.getInt("rcon.port"),
            config.getBoolean("rcon.whitelist"),
            config.getStringList("rcon.whitelisted")
        )
        configuration = GammaNetworkConfiguration(
            chatConfig,
            rconConfig
        )


    }

    override fun onPluginEnable() {
        CoroutineScope(Dispatchers.Default).launch {
            _globalCredentials = KredsCredentials.fromConfig(config)
            _redis = getKreds(_globalCredentials)

            provideService(KredsProvider::class.java, this@GammaNetwork)
            provideService(KredsCredentials::class.java, _globalCredentials)
            provideService(Messenger::class.java, redis)
            val ensureJoinedViaQueue = config.getBoolean("queue.ensure-joined-via")
            val connectionTimeout = config.getLong("queue.connection-timeout")
            val redirectSystem = GammaNetworkRedirectSystem(
                redis,
                this@GammaNetwork, VelocityPlayerRedirector,
                connectionTimeout
            )
            redirectSystem.addDefaultParameterProvider(RedirectNetworkMetadataParameterProvider)
            redirectSystem.setHandler(GammaNetworkRequestHandler)
            redirectSystem.setEnsure(ensureJoinedViaQueue)
            provideService(
                RedirectSystem::class.java,
                redirectSystem
            )
            provideService(LuckPerms::class.java, LuckPermsProvider.get())
            network = Network.create(redis, this@GammaNetwork)
            provideService(Network::class.java, network)
            redis.bindWith(this@GammaNetwork)
            // initialization
            Arguments.apply {
                profile(Commands.parserRegistry(), network)
                server(Commands.parserRegistry(), network)
            }
            bindModule(FindCommandModule(network, arrayOf("find")))
            bindModule(DispatchModule(redis, this@GammaNetwork, arrayOf("dispatch", "exec")))
            bindModule(PrivateMessageSystem)
            bindModule(AdminChatMessageSystem)
            bindModule(Ping)
            bindModule(GlobalChatMessageSystem)
            bindModule(NetworkCommands)
            bindModule(NetworkSummaryModule)
            bindModule(NetworkStatusModule)
            launch {
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
    }


    override fun getId(): String {
        return serverId
    }

    override fun getGroups(): MutableSet<String> {
        return mutableSetOf()
    }
}
