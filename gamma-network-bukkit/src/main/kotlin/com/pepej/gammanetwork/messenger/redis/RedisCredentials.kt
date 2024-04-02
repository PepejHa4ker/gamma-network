package com.pepej.gammanetwork.messenger.redis

import org.bukkit.configuration.ConfigurationSection

/**
 * Represents the credentials for a remote redis instance.
 */
data class RedisCredentials(val address: String, val port: Int, val password: String) {

    companion object {
        fun of(address: String, port: Int, password: String): RedisCredentials {
            return RedisCredentials(address, port, password)
        }

        fun fromConfig(config: ConfigurationSection): RedisCredentials {
            return of(
                config.getString("address", "localhost"),
                config.getInt("port", 6379),
                config.getString("password")
            )
        }
    }
}
