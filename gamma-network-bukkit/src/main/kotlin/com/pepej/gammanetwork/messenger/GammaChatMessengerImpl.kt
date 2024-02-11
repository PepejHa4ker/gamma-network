package com.pepej.gammanetwork.messenger

import com.google.common.reflect.TypeToken
import com.pepej.gammanetwork.messenger.redis.Kreds
import com.pepej.gammanetwork.messenger.redis.KredsCredentials
import com.pepej.papi.messaging.AbstractMessenger
import com.pepej.papi.messaging.Channel
import com.pepej.papi.terminable.composite.CompositeTerminable
import com.pepej.papi.utils.Log
import io.github.crackthecodeabhi.kreds.connection.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.function.BiConsumer
import java.util.function.Consumer


class GammaChatMessengerImpl private constructor(
    private val messenger: AbstractMessenger,
    override var kredsClient: KredsClient?,
    private var kredsSubscriberClient: KredsSubscriberClient?,
) : Kreds {


    companion object {

        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private suspend fun KredsClient.outgoingMessagesConsumer() = BiConsumer<String, ByteArray> { channel, message ->
            scope.launch {
                publish(channel, String(message))

            }
        }

        private suspend fun KredsSubscriberClient.subscribeChannel() = Consumer<String> { channel ->
            scope.launch {
                subscribe(channel)
            }
        }

        private suspend fun KredsSubscriberClient.unSubscribeChannel() = Consumer<String> { channel ->
            scope.launch {
                unsubscribe(channel)
            }
        }

        suspend fun create(credentials: KredsCredentials): GammaChatMessengerImpl {
            val listener = PubSubListener()
            val kredsClient = newClient(Endpoint(credentials.address, credentials.port))
            val kredsSubscriberClient = scope.newSubscriberClient(Endpoint(credentials.address, credentials.port), listener)
            val messenger = AbstractMessenger(
                kredsClient.outgoingMessagesConsumer(),
                kredsSubscriberClient.subscribeChannel(),
                kredsSubscriberClient.unSubscribeChannel()
            )
            listener.messenger = messenger
            return GammaChatMessengerImpl(messenger, kredsClient, kredsSubscriberClient)
        }
    }

    class PubSubListener(var messenger: AbstractMessenger? = null) : AbstractKredsSubscriber() {

        override fun onSubscribe(channel: String, subscribedChannels: Long) {
            Log.info("Subscribed to channel: $channel with channels $subscribedChannels")
        }

        override fun onUnsubscribe(channel: String, subscribedChannels: Long) {
            Log.info("Unsubscribed from channel: $channel with channels $subscribedChannels")
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
    }

    override fun <T> getChannel(name: String, type: TypeToken<T>): Channel<T> {
        return messenger.getChannel(name, type)
    }
}






