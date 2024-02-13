package com.pepej.gammanetwork.messenger

import com.google.common.reflect.TypeToken
import com.pepej.gammanetwork.messenger.redis.Kreds
import com.pepej.gammanetwork.messenger.redis.KredsCredentials
import com.pepej.papi.messaging.AbstractMessenger
import com.pepej.papi.messaging.Channel
import com.pepej.papi.utils.Log
import io.github.crackthecodeabhi.kreds.connection.*
import kotlinx.coroutines.*
import java.util.function.BiConsumer
import java.util.function.Consumer


class GammaNetworkMessenger private constructor(
    private val messenger: AbstractMessenger,
    override var kredsClient: KredsClient?,
    private var kredsSubscriberClient: KredsSubscriberClient?,
) : Kreds {


    companion object {

        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private suspend fun KredsClient.outgoingMessagesConsumer() = BiConsumer<String, ByteArray> { channel, message ->
            scope.launch {
                val msg = String(message)
                publish(channel, msg)

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

        suspend fun create(credentials: KredsCredentials): GammaNetworkMessenger = scope.async {
            val endpoint = Endpoint(credentials.address, credentials.port)
            val kredsClient = newClient(endpoint)
            val password = credentials.password
            kredsClient.auth(password)
            val kredsClientConfig =
                KredsClientConfig.Builder(readTimeoutSeconds = KredsClientConfig.NO_READ_TIMEOUT).build()
            val kredsSubscriberClient = newSubscriberClient(endpoint, PubSubListener, kredsClientConfig, null, password)
            kredsSubscriberClient.auth(null, password)
            val messenger = AbstractMessenger(
                kredsClient.outgoingMessagesConsumer(),
                kredsSubscriberClient.subscribeChannel(),
                kredsSubscriberClient.unSubscribeChannel()
            )
            PubSubListener.messenger = messenger
            GammaNetworkMessenger(messenger, kredsClient, kredsSubscriberClient)
        }.await()

    }

    private object PubSubListener : AbstractKredsSubscriber() {
         var messenger: AbstractMessenger? = null

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
        if (kredsSubscriberClient != null) {
            kredsSubscriberClient?.close()
            kredsSubscriberClient = null
        }
        if (kredsClient != null) {
            kredsClient?.close()
            kredsClient = null
        }

    }

    override fun <T> getChannel(name: String, type: TypeToken<T>): Channel<T> {
        return messenger.getChannel(name, type)
    }
}






