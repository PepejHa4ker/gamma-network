package com.pepej.gammanetwork.messenger.redis

import javax.annotation.Nonnull

interface RedisProvider {
    /**
     * Gets the global redis instance.
     *
     * @return the global redis instance.
     */
    val redis: Redis

    /**
     * Constructs a new redis instance using the given credentials.
     *
     *
     * These instances are not cached, and a new redis instance is created each
     * time this method is called.
     *
     * @param credentials the credentials for the redis instance
     * @return a new redis instance
     */
    fun getRedis(@Nonnull credentials: RedisCredentials): Redis

    /**
     * Gets the global redis credentials being used for the global redis instance.
     *
     * @return the global credentials
     */
    val globalCredentials: RedisCredentials
}