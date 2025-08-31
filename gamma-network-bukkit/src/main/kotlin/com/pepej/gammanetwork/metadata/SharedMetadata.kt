package com.pepej.gammanetwork.metadata

import com.google.gson.JsonElement
import com.pepej.papi.gson.GsonProvider
import com.pepej.papi.metadata.MetadataKey
import java.util.concurrent.ConcurrentHashMap

data class SharedMetadataEntry<T>(
    val key: MetadataKey<T>,
    val serializer: (T) -> JsonElement,
    val deserializer: (JsonElement) -> T
)

object SharedMetadata {

    private val gson = GsonProvider.standard()
    private val entries = ConcurrentHashMap<String, SharedMetadataEntry<*>>()

    private fun <T> register(
        key: MetadataKey<T>,
        serializer: (T) -> JsonElement,
        deserializer: (JsonElement) -> T
    ) {
        entries[key.id] = SharedMetadataEntry(key, serializer, deserializer)
    }

    fun <T> register(key: MetadataKey<T>, type: Class<T>) {

        register(
            key,
            serializer = { gson.toJsonTree(it) },
            deserializer = { gson.fromJson(it, type) }
        )
    }

    inline fun <reified T> register(key: MetadataKey<T>) {
        register(key, T::class.java)
    }

    fun unregister(key: MetadataKey<*>) {
        entries.remove(key.id)
    }

    fun isRegistered(key: MetadataKey<*>) = entries.containsKey(key.id)

    internal fun all(): Collection<SharedMetadataEntry<*>> = entries.values


    inline fun <reified T> MetadataKey<T>.share() {
        register(this, T::class.java)
    }
}