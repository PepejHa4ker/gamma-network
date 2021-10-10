package com.pepej.gammanetwork

import com.pepej.gammanetwork.commands.ProfileArgumentParserRegistry
import com.pepej.gammanetwork.messages.AdminChatMessageSystem
import com.pepej.gammanetwork.messages.PrivateMessageSystem
import com.pepej.gammanetwork.messenger.GammaChatMessengerImpl
import com.pepej.gammanetwork.messenger.GammaChatNetwork
import com.pepej.gammanetwork.messenger.redis.Redis
import com.pepej.gammanetwork.messenger.redis.RedisCredentials
import com.pepej.gammanetwork.messenger.redis.RedisProvider
import com.pepej.gammanetwork.reqresp.eco.EconomyRequester
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.papi.ap.Plugin
import com.pepej.papi.ap.PluginDependency
import com.pepej.papi.command.Commands
import com.pepej.papi.events.Events.subscribe
import com.pepej.papi.messaging.InstanceData
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.network.Network
import com.pepej.papi.network.modules.FindCommandModule
import com.pepej.papi.network.modules.NetworkStatusModule
import com.pepej.papi.network.modules.NetworkSummaryModule
import com.pepej.papi.plugin.PapiJavaPlugin
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.player.PlayerJoinEvent


@Plugin(
    name = "gammanetwork",
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
    lateinit var config: YamlConfiguration
    lateinit var network: Network
    override fun onPluginLoad() {
        instance = this
        config = loadConfig("config.yml")
        serverId = config.getString("server_id")
    }

    override fun onPluginEnable() {

        _globalCredentials = RedisCredentials.fromConfig(config)
        globalMessenger = GammaChatMessengerImpl(this, globalCredentials )

        this._redis = getRedis(this.globalCredentials);
        // expose all instances as services.
        provideService(RedisProvider::class.java, this)
        provideService(RedisCredentials::class.java, globalCredentials)
        provideService(Redis::class.java, this.redis)
        provideService(Messenger::class.java, this.redis)
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
