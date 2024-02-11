package com.pepej.gammanetwork

import com.pepej.gammanetwork.commands.NetworkCommands
import com.pepej.gammanetwork.commands.ProfileArgumentParserRegistry
import com.pepej.gammanetwork.config.ChatConfiguration
import com.pepej.gammanetwork.config.GammaNetworkConfiguration
import com.pepej.gammanetwork.messages.AdminChatMessageSystem
import com.pepej.gammanetwork.messages.GlobalChatMessageSystem
import com.pepej.gammanetwork.messages.PrivateMessageSystem
import com.pepej.gammanetwork.messenger.GammaChatMessengerImpl
import com.pepej.gammanetwork.messenger.redis.Kreds
import com.pepej.gammanetwork.messenger.redis.KredsCredentials
import com.pepej.gammanetwork.messenger.redis.KredsProvider
import com.pepej.gammanetwork.redirect.GammaNetworkRedirectSystem
import com.pepej.gammanetwork.redirect.GammaNetworkRequestHandler
import com.pepej.gammanetwork.redirect.RedirectNetworkMetadataParameterProvider
import com.pepej.gammanetwork.redirect.VelocityPlayerRedirector
import com.pepej.papi.ap.Plugin
import com.pepej.papi.ap.PluginDependency
import com.pepej.papi.command.Commands
import com.pepej.papi.dependency.Dependencies
import com.pepej.papi.dependency.Dependency
import com.pepej.papi.dependency.Repository
import com.pepej.papi.messaging.InstanceData
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.network.Network
import com.pepej.papi.network.modules.DispatchModule
import com.pepej.papi.network.modules.FindCommandModule
import com.pepej.papi.network.modules.NetworkStatusModule
import com.pepej.papi.network.modules.NetworkSummaryModule
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.plugin.PapiJavaPlugin
import kotlinx.coroutines.*
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import org.slf4j.LoggerFactory


@Dependencies(
    Dependency("ch.qos.logback:logback-classic:1.3.5"),
    Dependency("ch.qos.logback:logback-core:1.3.5"),
    Dependency("redis.clients:jedis:3.6.3"),
    Dependency("org.apache.commons:commons-pool2:2.10.0"),
    Dependency("net.jodah:expiringmap:0.5.11"),
    Dependency("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0-RC2"),
//    Dependency("io.netty:netty-handler:4.1.104.Final"),
//    Dependency("io.netty:netty-codec-redis:4.1.104.Final"),
//    Dependency("io.github.microutils:kotlin-logging-jvm:3.0.5"),
    Dependency(
        "io.github.crackthecodeabhi:kreds:squareland-1.9",
        repo = Repository(url = "https://oss.squareland.ru/repository/minecraft")
    ),
)
@Plugin(
    name = "gamma-network",
    version = "1.1.1",
    authors = ["pepej"],
    depends = [
        PluginDependency("papi"),
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
            return GammaChatMessengerImpl.create(credentials)
//        }
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
        configuration = GammaNetworkConfiguration(
            ChatConfiguration(
                enableSplitting = config.getBoolean("chat.enable-splitting"),
                localChatRadius = config.getInt("chat.local-server-chat-radius"),
                localFormat = ChatConfiguration.Format(config.getString("chat.local-chat-format")),
                globalFormat = ChatConfiguration.Format(config.getString("chat.global-chat-format")),
                adminMessageColor = ChatConfiguration.Format(config.getString("chat.admin-message-color")),
            )
        )


    }

    override fun onPluginEnable() {
        CoroutineScope(Dispatchers.Default).launch {
            _globalCredentials = KredsCredentials.fromConfig(config)
            _redis = getKreds(globalCredentials);
            // expose all instances as services.
            provideService(KredsProvider::class.java, this@GammaNetwork)
            provideService(KredsCredentials::class.java, globalCredentials)
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
            ProfileArgumentParserRegistry.register(Commands.parserRegistry(), network)
            bindModule(FindCommandModule(network, arrayOf("find")))
            bindModule(NetworkStatusModule(network))
            bindModule(NetworkSummaryModule(network, this@GammaNetwork, arrayOf("netsum", "online")))
            bindModule(DispatchModule(redis, this@GammaNetwork, arrayOf("dispatch", "exec")))
            bindModule(PrivateMessageSystem)
            bindModule(AdminChatMessageSystem)
            bindModule(GlobalChatMessageSystem)
            bindModule(NetworkCommands)
//            GammaNetworkApi().init()

//        bindModule(Parties)
//        bindModule(PartyCommand)
//        bindModule(TwinksCommand)

        }
    }


    override fun getId(): String {
        return serverId
    }

    override fun getGroups(): MutableSet<String> {
        return mutableSetOf()
    }
}
