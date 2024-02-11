package com.pepej.gammanetwork.messenger.redis

import org.bukkit.configuration.ConfigurationSection

/**
 * Represents the credentials for a remote redis instance.
 */
data class KredsCredentials(val address: String, val port: Int, val password: String) {

    companion object {
        fun of(address: String, port: Int, password: String): KredsCredentials {
            return KredsCredentials(address, port, password)
        }

        fun fromConfig(config: ConfigurationSection): KredsCredentials {
            return of(
                config.getString("address", "localhost"),
                config.getInt("port", 6379),
                config.getString("password")
            )
        }
    }
}
