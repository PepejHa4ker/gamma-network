package com.pepej.gammanetwork.messenger

import com.google.common.reflect.TypeToken
import com.pepej.gammanetwork.messenger.redis.Redis
import com.pepej.gammanetwork.messenger.redis.RedisCredentials
import com.pepej.papi.messaging.AbstractMessenger
import com.pepej.papi.messaging.Channel
import com.pepej.papi.scheduler.Schedulers
import com.pepej.papi.terminable.composite.CompositeTerminable
import com.pepej.papi.utils.Log
import redis.clients.jedis.BinaryJedisPubSub
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

class GammaNetworkMessenger(credentials: RedisCredentials) : Redis {
    private var _jedisPool: JedisPool
    override val jedisPool: JedisPool
        get() {
            return _jedisPool
        }
    override val jedis: Jedis
        get() {
            return _jedisPool.resource
        }
    private var messenger: AbstractMessenger
    private val channels = mutableSetOf<String>()
    private val registry = CompositeTerminable.create()
    private var listener: PubSubListener? = null


    init {
        val config = JedisPoolConfig()
        config.maxTotal = 128
        _jedisPool = if (credentials.password.trim { it <= ' ' }.isEmpty()) {
            JedisPool(config, credentials.address, credentials.port)
        } else {
            JedisPool(config, credentials.address, credentials.port, 2000, credentials.password)
        }
        jedisPool.resource.use { jedis -> jedis.ping() }

        Schedulers.async().run(object : Runnable {
            private var broken = false
            override fun run() {
                if (broken) {
                    Log.info("[network] Retrying subscription...")
                    broken = false
                }
                jedis.use { jedis ->
                    try {
                        listener = PubSubListener()
                        jedis.subscribe(
                            listener,
                            "network-redis-dummy".toByteArray(StandardCharsets.UTF_8)
                        )
                    } catch (e: Exception) {
                        // Attempt to unsubscribe this instance and try again.
                        RuntimeException("Error subscribing to listener", e).printStackTrace()

                        listener?.unsubscribe()

                        listener = null
                        broken = true
                    }
                }
                if (broken) {
                    // reschedule the runnable
                    Schedulers.async().runLater(this, 1L)
                }
            }
        })

        Schedulers.async().runRepeating(Runnable {


            if (listener == null || !(listener?.isSubscribed)!!) {
                return@Runnable
            }
            for (channel in channels) {
                listener?.subscribe(channel.toByteArray(StandardCharsets.UTF_8))
            }
        }, 2L, 2L).bindWith(registry)

        messenger = AbstractMessenger(
            { channel: String, message: ByteArray? ->
                jedis.use { jedis ->
                    jedis.publish(
                        channel.toByteArray(StandardCharsets.UTF_8),
                        message
                    )
                }
            },
            { channel: String ->
                Log.info("[network] Subscribing to channel: $channel")
                channels.add(channel)
                listener?.subscribe(channel.toByteArray(StandardCharsets.UTF_8))
            }
        ) { channel: String ->
            Log.info("[gamma-redis] Unsubscribing from channel: $channel")
            channels.remove(channel)
            listener?.unsubscribe(channel.toByteArray(StandardCharsets.UTF_8))
        }
    }




    override fun close() {
        if (listener != null) {
            listener?.unsubscribe()
            listener = null
        }
        jedisPool.close()
        registry.close()
    }

    override fun <T> getChannel(
        name: String,
        type: TypeToken<T>,
    ): Channel<T> {
        return messenger.getChannel(name, type)
    }

    private inner class PubSubListener : BinaryJedisPubSub() {
        private val lock = ReentrantLock()
        private val subscribed: MutableSet<String> = ConcurrentHashMap.newKeySet()
        override fun subscribe(vararg channels: ByteArray) {
            lock.lock()
            try {
                for (channel in channels) {
                    val channelName = String(channel, StandardCharsets.UTF_8)
                    if (subscribed.add(channelName)) {
                        super.subscribe(channel)
                    }
                }
            } finally {
                lock.unlock()
            }
        }

        override fun unsubscribe(vararg channels: ByteArray) {
            lock.lock()
            try {
                super.unsubscribe(*channels)
            } finally {
                lock.unlock()
            }
        }

        override fun onSubscribe(channel: ByteArray, subscribedChannels: Int) {
            Log.info("[network] Subscribed to channel: " + String(channel, StandardCharsets.UTF_8))
        }

        override fun onUnsubscribe(channel: ByteArray, subscribedChannels: Int) {
            val channelName = String(channel, StandardCharsets.UTF_8)
            Log.info("[network] Unsubscribed from channel: $channelName")
            subscribed.remove(channelName)
        }

        override fun onMessage(channel: ByteArray, message: ByteArray) {
            val channelName = String(channel, StandardCharsets.UTF_8)
            try {
                messenger.registerIncomingMessage(channelName, message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
