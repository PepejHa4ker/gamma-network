package com.pepej.gammanetwork

import com.pepej.gammanetwork.commands.NetworkCommands
import com.pepej.gammanetwork.commands.ProfileArgumentParserRegistry
import com.pepej.gammanetwork.config.ChatConfiguration
import com.pepej.gammanetwork.config.GammaNetworkConfiguration
import com.pepej.gammanetwork.messages.AdminChatMessageSystem
import com.pepej.gammanetwork.messages.GlobalChatMessageSystem
import com.pepej.gammanetwork.messages.PrivateMessageSystem
import com.pepej.gammanetwork.messenger.GammaChatMessengerImpl
import com.pepej.gammanetwork.messenger.GammaChatNetwork
import com.pepej.gammanetwork.messenger.redis.Redis
import com.pepej.gammanetwork.messenger.redis.RedisCredentials
import com.pepej.gammanetwork.messenger.redis.RedisProvider
import com.pepej.gammanetwork.redirect.GammaNetworkRedirectSystem
import com.pepej.gammanetwork.redirect.GammaNetworkRequestHandler
import com.pepej.gammanetwork.redirect.RedirectNetworkMetadataParameterProvider
import com.pepej.gammanetwork.redirect.VelocityPlayerRedirector
import com.pepej.papi.ap.Plugin
import com.pepej.papi.ap.PluginDependency
import com.pepej.papi.command.Commands
import com.pepej.papi.messaging.InstanceData
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.network.Network
import com.pepej.papi.network.modules.FindCommandModule
import com.pepej.papi.network.modules.NetworkStatusModule
import com.pepej.papi.network.modules.NetworkSummaryModule
import com.pepej.papi.network.redirect.RedirectSystem
import com.pepej.papi.plugin.PapiJavaPlugin
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider


@Plugin(
    name = "gamma-network",
    version = "1.1.0",
    authors = ["pepej"],
    depends = [
        PluginDependency("papi"),
    ]
)
class GammaNetwork : PapiJavaPlugin(), RedisProvider, InstanceData {


    private lateinit var _redis: Redis

   override val redis: Redis
   get() {
       return _redis
   }

    override fun getRedis(credentials: RedisCredentials): Redis {
        return GammaChatMessengerImpl(this, credentials)
    }

    private lateinit var _globalCredentials: RedisCredentials

    override val globalCredentials: RedisCredentials
    get() {
        return _globalCredentials
    }

    companion object {
        lateinit var instance: GammaNetwork
    }

    lateinit var serverId: String
    lateinit var globalMessenger: GammaChatMessengerImpl
    lateinit var network: Network
    lateinit var configuration: GammaNetworkConfiguration
    override fun onPluginLoad() {
        instance = this

    }

    override fun onPluginEnable() {
        val config = loadConfig("config.yml")
        this.serverId = config.getString("server_id")
        this.configuration = GammaNetworkConfiguration(
            ChatConfiguration(
                enableSplitting = config.getBoolean("chat.enable-splitting"),
                localChatRadius = config.getInt("chat.local-server-chat-radius"),
                localFormat = ChatConfiguration.Format(config.getString("chat.local-chat-format")),
                globalFormat = ChatConfiguration.Format(config.getString("chat.global-chat-format")),
                adminMessageColor = ChatConfiguration.Format(config.getString("chat.admin-message-color")),
            )
        )
        _globalCredentials = RedisCredentials.fromConfig(config)
        globalMessenger = GammaChatMessengerImpl(this, globalCredentials )

        this._redis = getRedis(this.globalCredentials);
        // expose all instances as services.
        provideService(RedisProvider::class.java, this)
        provideService(RedisCredentials::class.java, globalCredentials)
        provideService(Redis::class.java, this.redis)
        provideService(Messenger::class.java, this.redis)
        val redirectSystem = GammaNetworkRedirectSystem(this.redis, this, VelocityPlayerRedirector)
        redirectSystem.addDefaultParameterProvider(RedirectNetworkMetadataParameterProvider)
        redirectSystem.setHandler(GammaNetworkRequestHandler)
        redirectSystem.setEnsure(true)
        provideService(RedirectSystem::class.java,
            redirectSystem
        )
        provideService(LuckPerms::class.java, LuckPermsProvider.get())
        network = GammaChatNetwork(redis, this)
        provideService(Network::class.java, network)
        this.redis.bindWith(this)
         // initialization
        ProfileArgumentParserRegistry.register(Commands.parserRegistry(), network)
        bindModule(FindCommandModule(network, arrayOf("find")))
        bindModule(NetworkStatusModule(network))
        bindModule(NetworkSummaryModule(network, this, arrayOf("netsum", "online")))
        bindModule(PrivateMessageSystem)
        bindModule(AdminChatMessageSystem)
        bindModule(GlobalChatMessageSystem)
        bindModule(NetworkCommands)
        GammaNetworkApi().init()

//        bindModule(Parties)
//        bindModule(PartyCommand)
//        bindModule(TwinksCommand)

    }

    override fun getId(): String {
        return serverId
    }

    override fun getGroups(): MutableSet<String> {
        return mutableSetOf()
    }
}
