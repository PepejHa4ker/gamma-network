package com.pepej.gammachat.messenger

import com.google.common.io.ByteStreams
import com.google.common.reflect.TypeToken
import com.pepej.gammachat.GammaChat
import com.pepej.papi.messaging.AbstractMessenger
import com.pepej.papi.messaging.Channel
import com.pepej.papi.plugin.PapiPlugin
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.utils.Log
import com.pepej.papi.utils.Players
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class GammaChatMessengerImpl(private val plugin: PapiPlugin) : GammaChatMessenger {

    private val messenger: AbstractMessenger
    private val knownChannels = mutableSetOf("chat:registerer")
    private val queuedMessages = ConcurrentHashMap.newKeySet<MessageData>()

    init {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "chat:registerer")
        messenger = AbstractMessenger(
            { channel, message -> trySendMessage(MessageData(channel, message), true) },
            {
                Log.info("Reg $it")
                registerChannel(it, true)
            },
            {
                Log.info("Unreg $it")
                unregisterChannel(it, true)
            }
        )
        Schedulers.builder()
            .sync()
            .afterAndEvery(3, TimeUnit.SECONDS)
            .run { flushQueuedMessages() }
            .bindWith(plugin)

    }


    private fun registerChannel(channel: String, incoming: Boolean = false) {
        knownChannels.add(channel)
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, channel)
        if (incoming) {
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, this)
        }
        val out = ByteStreams.newDataOutput()
        out.writeUTF("register")
        out.writeUTF(channel)

        trySendMessage(MessageData("chat:registerer", out.toByteArray()), true)
    }

    private fun unregisterChannel(channel: String, incoming: Boolean = false) {
        knownChannels.remove(channel)
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, channel)
        if (incoming) {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, channel, this)
        }
        val out = ByteStreams.newDataOutput()
        out.writeUTF("unregister")
        out.writeUTF(channel)
        trySendMessage(MessageData("chat:registerer", out.toByteArray()), true)
    }

    private fun trySendMessage(data: MessageData, queued: Boolean = false) {

        // try to find a player
        val player = Players.all().firstOrNull()
        if (player != null) {
            player.sendPluginMessage(plugin, data.channel, data.message)
        } else {
            if (queued) {
                queuedMessages.add(data)
            }
        }
    }

    private fun flushQueuedMessages() {
        if (queuedMessages.isEmpty()) {
            return
        }
        val p = Players.all().firstOrNull()
        if (p != null) {
            queuedMessages.removeIf {
                Log.info("Flush msg to ${it.channel}")
                p.sendPluginMessage(plugin, it.channel, it.message)
                true
            }
        }
    }


    private data class MessageData(val channel: String, val message: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MessageData

            if (channel != other.channel) return false
            if (!message.contentEquals(other.message)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = channel.hashCode()
            result = 31 * result + message.contentHashCode()
            return result
        }
    }


    override fun <T : Any?> getChannel(name: String, type: TypeToken<T>): Channel<T> {
        return this.messenger.getChannel(name, type)
    }

    override fun getId(): String {
        return GammaChat.instance.serverId
    }

    override fun getGroups(): Set<String> = emptySet()




    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (knownChannels.find { it == channel }?.firstOrNull() != null) {
            messenger.registerIncomingMessage(channel, message)
        }
    }

}