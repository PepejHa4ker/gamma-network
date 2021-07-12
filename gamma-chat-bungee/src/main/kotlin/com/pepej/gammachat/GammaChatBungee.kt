package com.pepej.gammachat

import com.google.common.io.ByteStreams
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import sun.rmi.runtime.Log

class GammaChatBungee : Plugin(), Listener {

    private val knownChannels = mutableSetOf("chat:registerer", "pnet-events", "pnet-status")

    override fun onEnable() {
        proxy.pluginManager.registerListener(this, this)
        knownChannels.forEach { proxy.registerChannel(it) }
    }

    @EventHandler
    fun onPluginMessage(e: PluginMessageEvent) {
        println(e.tag)
        if (e.tag == "chat:registerer") {
            val input = ByteStreams.newDataInput(e.data)
            val type = input.readUTF()
            val channel = input.readUTF()
            when (type) {
                "register" -> {
                    proxy.registerChannel(channel)
                    knownChannels.add(channel)
                }

                "unregister" -> {
                    proxy.unregisterChannel(channel)
                    knownChannels.remove(channel)
                }
            }
        } else {
            for (channel in knownChannels) {
                if (channel == e.tag) {
                    for (server in proxy.servers.values) {
                        server.sendData(e.tag, e.data, false)

                    }
                }
            }
        }
    }
}
