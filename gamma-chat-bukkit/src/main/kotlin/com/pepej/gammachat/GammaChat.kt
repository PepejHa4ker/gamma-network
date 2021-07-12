package com.pepej.gammachat

import com.pepej.gammachat.messages.AdminChatMessageSystem
import com.pepej.gammachat.messages.PrivateMessageSystem
import com.pepej.gammachat.messenger.GammaChatMessenger
import com.pepej.gammachat.messenger.GammaChatMessengerImpl
import com.pepej.gammachat.messenger.GammaChatNetwork
import com.pepej.papi.ap.Plugin
import com.pepej.papi.ap.PluginDependency
import com.pepej.papi.command.Commands
import com.pepej.papi.messaging.InstanceData
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.network.Network
import com.pepej.papi.network.event.NetworkEvent
import com.pepej.papi.network.event.ServerConnectEvent
import com.pepej.papi.network.modules.FindCommandModule
import com.pepej.papi.network.modules.NetworkStatusModule
import com.pepej.papi.network.modules.NetworkSummaryModule
import com.pepej.papi.plugin.PapiJavaPlugin
import com.pepej.papi.utils.Log

@Plugin(name = "gammachat", version = "1.0.0", authors = ["pepej"], depends = [PluginDependency("papi")])
class GammaChat : PapiJavaPlugin() {

    companion object {
        lateinit var instance: GammaChat
    }

    lateinit var serverId: String
    lateinit var globalMessenger: GammaChatMessenger
    lateinit var network: Network
    lateinit var messageSystem: PrivateMessageSystem
    override fun onPluginLoad() {
        instance = this
        serverId = loadConfig("config.yml").getString("server_id")
    }

    override fun onPluginEnable() {
        globalMessenger = GammaChatMessengerImpl(this)
        network = GammaChatNetwork(globalMessenger)
        provideService(Messenger::class.java, globalMessenger)
        provideService(InstanceData::class.java, globalMessenger)
        provideService(Network::class.java, network)
        messageSystem = PrivateMessageSystem
        provideService(PrivateMessageSystem::class.java, messageSystem)
        bindModule(FindCommandModule(network, arrayOf("find")))
        bindModule(NetworkStatusModule(network))
        bindModule(NetworkSummaryModule(network, globalMessenger, arrayOf("netsum", "online")))
        bindModule(PrivateMessageSystem)
        bindModule(AdminChatMessageSystem)
    }
}
