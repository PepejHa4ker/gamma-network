package com.pepej.gammanetwork.messenger.redis


interface RedisProvider {
    /**
     * Gets the global redis instance.
     *
     * @return the global redis instance.
     */
    val redis: Redis

    /**
     * Constructs a new redis instance using the given credentials.
    @@ -20,12 +20,12 @@ interface RedisProvider {
     * @param credentials the credentials for the redis instance
     * @return a new redis instance
     */
    fun getRedis(credentials: RedisCredentials): Redis

    /**
     * Gets the global redis credentials being used for the global redis instance.
     *
     * @return the global credentials
     */
    val globalCredentials: RedisCredentials
}