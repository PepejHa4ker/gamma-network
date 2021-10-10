package com.pepej.gammanetwork.utils

import com.pepej.papi.command.argument.Argument
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.messaging.conversation.ConversationChannel
import com.pepej.papi.messaging.conversation.ConversationMessage
import com.pepej.papi.services.Services


inline fun <reified T> Messenger.getChannel(channel: String) = this.getChannel(channel, T::class.java)
inline fun <reified T, reified R> Messenger.getConversationChannel(channel: String): ConversationChannel<T, R>
        where T : ConversationMessage, R : ConversationMessage {
            return this.getConversationChannel(channel, T::class.java, R::class.java)
}


inline fun <reified T> getServiceUnchecked(): T {
    return Services.getNullable(T::class.java)!!
}



