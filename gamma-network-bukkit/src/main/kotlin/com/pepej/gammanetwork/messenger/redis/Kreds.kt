package com.pepej.gammanetwork.messenger.redis

import com.pepej.papi.messaging.Messenger
import com.pepej.papi.terminable.Terminable
import io.github.crackthecodeabhi.kreds.connection.KredsClient


/**
 * Represents an individual redis instance, created by the library.
 */
interface Kreds : Terminable, Messenger {
    /**
     * Gets the Kreds instance backing the redis instance
     *
     * @return the KredsClient instance
     */
    val kredsClient: KredsClient?
}