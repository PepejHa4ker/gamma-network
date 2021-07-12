package com.pepej.gammachat.messenger

import com.pepej.papi.messaging.InstanceData
import com.pepej.papi.messaging.Messenger
import org.bukkit.plugin.messaging.PluginMessageListener

interface GammaChatMessenger : Messenger, InstanceData, PluginMessageListener