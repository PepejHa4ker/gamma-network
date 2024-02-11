package com.pepej.gammanetwork.messenger

import com.google.common.reflect.TypeToken
import com.pepej.gammanetwork.messenger.redis.Kreds
import com.pepej.gammanetwork.messenger.redis.KredsCredentials
import com.pepej.papi.messaging.AbstractMessenger
import com.pepej.papi.messaging.Channel
import com.pepej.papi.terminable.composite.CompositeTerminable
import com.pepej.papi.utils.Log
import io.github.crackthecodeabhi.kreds.connection.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds


class GammaChatMessengerImpl private constructor(
    private val messenger: AbstractMessenger,
    override var kredsClient: KredsClient?,
    private var kredsSubscriberClient: KredsSubscriberClient?,
    private val channels: MutableSet<String> = mutableSetOf()
) : Kreds {

    private val registry = CompositeTerminable.create()
    private val log = LoggerFactory.getLogger(GammaChatMessengerImpl::class.java)


    companion object {

        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private suspend fun KredsClient.outgoingMessagesConsumer() = BiConsumer<String, ByteArray> { channel, message ->
            scope.launch {
                JedisPool
                this@outgoingMessagesConsumer.use {
                    it.publish(channel, String(message, Charsets.UTF_8))
                }
            }
        }


        private suspend fun KredsSubscriberClient.subscribeChannel(
            channels: MutableSet<String>,
        ) = Consumer<String> { channel ->
            scope.launch {
                this@subscribeChannel.use {
                    channels.add(channel)
                    it.subscribe(channel)
                }
            }
        }

        private suspend fun KredsSubscriberClient.unSubscribeChannel(
            channels: MutableSet<String>,
        ) = Consumer<String> { channel ->
            scope.launch {
                this@unSubscribeChannel.use {
                    channels.remove(channel)
                    it.unsubscribe(channel)
                }
            }
        }


        suspend fun create(credentials: KredsCredentials): GammaChatMessengerImpl {
            val listener = PubSubListener()
            val kredsClient = newClient(Endpoint(credentials.address, credentials.port))
            kredsClient.auth(credentials.password)

            val kredsSubscriberClient = scope.newSubscriberClient(Endpoint(credentials.address, credentials.port), listener)
            kredsSubscriberClient.auth(credentials.password)
            val channels = mutableSetOf<String>()
            val messenger = AbstractMessenger(
                kredsClient.outgoingMessagesConsumer(),
                kredsSubscriberClient.subscribeChannel(channels),
                kredsSubscriberClient.unSubscribeChannel(channels)
            )
            listener.messenger = messenger

            val gammaChatMessengerImpl = GammaChatMessengerImpl(messenger, kredsClient, kredsSubscriberClient)
            scope.launch {
                while (gammaChatMessengerImpl.kredsSubscriberClient != null) {
                    delay(2000)
//                    gammaChatMessengerImpl.channels.forEach { channel ->
//                        gammaChatMessengerImpl.kredsSubscriberClient?.subscribe(channel)
//
//                    }
                }
            }
            return gammaChatMessengerImpl

        }
    }

    class PubSubListener(var messenger: AbstractMessenger? = null) : AbstractKredsSubscriber() {

        override fun onSubscribe(channel: String, subscribedChannels: Long) {
            Log.info("Subscribed to channel: $channel")
        }

        override fun onUnsubscribe(channel: String, subscribedChannels: Long) {
            Log.info("Unsubscribed from channel: $channel")
        }

        override fun onMessage(channel: String, message: String) {
            try {
                messenger?.registerIncomingMessage(channel, message.toByteArray())
            } catch (e: Exception) {
                Log.severe("Exception during processing message {} on channel {}", message, channel, e)
            }
        }

        override fun onException(ex: Throwable) {
            Log.severe("Exception during listening redis", ex)
        }
    }

    override fun close() {
        if (kredsClient != null) {
            kredsClient?.close()
            kredsClient = null
        }
        if (kredsSubscriberClient != null) {
            kredsSubscriberClient?.close()
            kredsSubscriberClient = null
        }

        this.registry.close();
    }

    override fun <T> getChannel(name: String, type: TypeToken<T>): Channel<T> {
        return messenger.getChannel(name, type)
    }
}






