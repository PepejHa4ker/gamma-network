package com.pepej.gammanetwork.messenger.redis

import com.pepej.papi.messaging.Messenger
import com.pepej.papi.terminable.Terminable
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

/**
 * Represents an individual redis instance, created by the library.
 */
interface Redis : Terminable, Messenger {
    /**
     * Gets the JedisPool instance backing the redis instance
     *
     * @return the JedisPool instance
     */
    val jedisPool: JedisPool

    /**
     * Gets a Jedis instance from the JedisPool.
     *
     * @return a jedis instance
     */
    val jedis: Jedis
}