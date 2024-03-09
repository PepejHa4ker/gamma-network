package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.utils.broadcast
import com.pepej.gammanetwork.utils.getChannel
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.gammanetwork.utils.parseOrFail
import com.pepej.papi.command.Commands
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.network.Network
import com.pepej.papi.terminable.Terminable
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.composite.CompositeTerminable
import com.pepej.papi.terminable.module.TerminableModule
import com.pepej.papi.utils.TabHandlers
import org.slf4j.LoggerFactory

interface NetworkModule : Terminable {

    val name: String
    var scope: CompositeTerminable?
    var enabled: Boolean

    fun enable() {
        enabled = true
        if (scope == null) {
            scope = CompositeTerminable.create()
        }
        onEnable()
    }
    fun disable() {
        scope?.close()
        scope = null
        enabled = false
        onDisable()
    }

    fun onDisable() {}

    fun onEnable() {}
    fun reload() {
        if (!isReloadable()) {
            throw UnsupportedOperationException("Module $name is not reloadable")
        }
        if (!isEnabled()) {
            throw IllegalStateException("Module $name is not enabled")
        }
        disable()
        enable()
    }
    fun isEnabled(): Boolean {
        return enabled
    }
    fun isDisabled(): Boolean {
        return !enabled
    }
    fun isReloadable(): Boolean {
        return false
    }
    override fun close() {
        scope?.close()
    }

}

object ModuleManager : TerminableModule {

    private val network: Network = getServiceUnchecked()
    private val messenger: Messenger = getServiceUnchecked()
    private val modules = mutableListOf<NetworkModule>()
    private val log = LoggerFactory.getLogger(ModuleManager::class.java)
    private val channel = messenger.getChannel<ModuleUpdateMessage>("network-modules").apply {
        newAgent { _, (name, enabled) ->
            val module = findModule(name) ?: return@newAgent
            if (enabled) {
                module.enable()
            } else {
                module.disable()
            }
            network.broadcast("Module $name ${if (enabled) "&aenabled" else "&cdisabled"}")
            log.info("Module $name ${if (enabled) "enabled" else "disabled"}")


        }
    }
    override fun setup(consumer: TerminableConsumer) {
        init()
        Commands.create()
            .assertPermission("network.modules.manage")
            .tabHandler { context ->
                when (context.args().size) {
                    1 -> {
                        TabHandlers.of(context.arg(0).parseOrFail(), "all", *modules.map { it.name }.toTypedArray())
                    }
                    2 -> {
                        TabHandlers.of(context.arg(1).parseOrFail(), "enable", "disable")
                    }
                    else -> listOf()
                }
            }
            .handler { context ->
                if (context.args().size < 2) {
                    return@handler context.replyError("Usage: module <module|all> <enable|disable>")
                }
                val name = context.arg(0).parseOrFail<String>()
                val enable = context.arg(1).parseOrFail<String>().equals("enable", true)
                if (name.equals("all", true)) {
                    modules.forEach { channel.sendMessage(ModuleUpdateMessage(it.name, enable)) }
                    return@handler context.replyAnnouncement("All modules updated")
                }
                val module = findModule(name) ?: return@handler context.replyError("Module not found")
                channel.sendMessage(ModuleUpdateMessage(module.name, enable))
            }
            .registerAndBind(consumer, "module")

    }

    data class ModuleUpdateMessage(val name: String, val enabled: Boolean)

    fun getModules(): List<NetworkModule> {
        return modules
    }

    private fun init() {
        modules.add(NetworkMalformedProfileModule)
        modules.add(NetworkStatusModule)
        modules.add(NetworkSummaryModule)
    }

    private fun findModule(name: String): NetworkModule? {
        return modules.find { it.name.equals(name, true) }
    }


}