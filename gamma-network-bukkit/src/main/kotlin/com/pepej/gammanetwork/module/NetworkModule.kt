package com.pepej.gammanetwork.module

import com.pepej.gammanetwork.commands.Tabs.enableDisableTab
import com.pepej.gammanetwork.utils.broadcast
import com.pepej.gammanetwork.utils.getChannel
import com.pepej.gammanetwork.utils.getServiceUnchecked
import com.pepej.gammanetwork.utils.parseOrFail
import com.pepej.papi.command.Commands
import com.pepej.papi.command.context.CommandContext
import com.pepej.papi.messaging.Messenger
import com.pepej.papi.network.Network
import com.pepej.papi.terminable.Terminable
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.composite.CompositeTerminable
import com.pepej.papi.terminable.module.TerminableModule
import com.pepej.papi.utils.TabHandlers
import org.bukkit.command.CommandSender
import org.slf4j.LoggerFactory

abstract class NetworkModule(
    val id: String,
) : Terminable {

    protected val network: Network = getServiceUnchecked()
    protected val messenger: Messenger = getServiceUnchecked()
    internal var scope: CompositeTerminable? = CompositeTerminable.create()
    protected var enabled: Boolean = false
    private val log = LoggerFactory.getLogger(javaClass)

    fun enable() {
        if (enabled) {
            log.warn("Module $id is already enabled")
            return
        }
        if (scope == null) {
            scope = CompositeTerminable.create()
        }
        scope?.let {
            log.info("Enabling module {} scope {}", id, scope)
            onEnable(it)
        }

        enabled = true


    }

    fun disable() {
        if (!enabled) {
            log.warn("Module $id is already disabled")
            return
        }
        scope?.let(this::onDisable)
        scope?.close()
        scope = null
        enabled = false
    }

    open fun onDisable(consumer: TerminableConsumer) {}

    open fun onEnable(consumer: TerminableConsumer) {}
    open fun onReload() {}

    fun reload() {
        if (!isReloadable()) {
            throw UnsupportedOperationException("Module $id is not reloadable")
        }
        if (!enabled) {
            throw IllegalStateException("Module $id is not enabled")
        }
        onReload()
        disable()
        enable()
    }

    private fun isReloadable(): Boolean {
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
            toggleModule(name, enabled, fromNetwork = true)


        }
    }

    private fun toggleModule(name: String, enabled: Boolean, fromNetwork: Boolean) {
        val module = findModule(name) ?: return
        if (enabled) {
            module.enable()
        } else {
            module.disable()
        }
        if (fromNetwork) {
            broadcast("Module $name ${if (enabled) "&aenabled" else "&cdisabled"}")
        }
        log.info("Module $name ${if (enabled) "enabled" else "disabled"}")
    }

    override fun setup(consumer: TerminableConsumer) {
        init()
        modules.forEach { it.scope?.let(consumer::bind) }
        Commands.create()
            .assertPermission("network.modules.manage")
            .assertUsage("<module|all> <server|all> <enable|disable>")
            .tabHandler { context ->
                when (context.args().size) {
                    1 -> {
                        TabHandlers.of(context.arg(0).parseOrFail(), "all", *modules.map { it.id }.toTypedArray())
                    }
                    2 -> {
                        TabHandlers.of(context.arg(1).parseOrFail(), "all", *network.servers.values.map { it.id }.toTypedArray())
                    }
                    3 -> {
                        context.arg(2).enableDisableTab()
                    }
                    else -> listOf()
                }
            }
            .handler { context ->
                if (context.args().size < 3) {
                    return@handler context.replyError("Usage: module <module|all> <server|all> <enable|disable>")
                }
                val name = context.arg(0).parseOrFail<String>()
                val server = context.arg(1).parseOrFail<String>()
                val enable = context.arg(2).parseOrFail<String>().equals("enable", true)
                process(context, name, server, enable)

            }
            .registerAndBind(consumer, "module")

    }

    private fun process(context: CommandContext<CommandSender>, name: String, server: String, enabled: Boolean) {
        val allModules = name.equals("all", true)
        val allServers = server.equals("all", true)
        when {
            allModules && allServers -> {
                modules.forEach { channel.sendMessage(ModuleUpdateMessage(it.id, enabled)) }
            }
            allModules -> {
                modules.forEach { toggleModule(it.id, enabled, fromNetwork = false) }
            }
            allServers -> {
                channel.sendMessage(ModuleUpdateMessage(name, enabled))
            }
        }
        return context.replyAnnouncement("Success")

    }

    data class ModuleUpdateMessage(val name: String, val enabled: Boolean)

    fun getModules(): List<NetworkModule> {
        return modules
    }

    private fun init() {
        modules.add(NetworkMalformedProfileModule)
        modules.add(NetworkStatusModule)
        modules.add(NetworkSummaryModule)
        modules.add(NetworkAlertModule)
        modules.add(AdminChatModule)
        modules.add(PrivateMessageSystem)
        modules.add(GlobalChatModule)
        modules.add(NetworkVersionCheckerModule)
        modules.forEach { it.enable() }

    }

    private fun findModule(name: String): NetworkModule? {
        return modules.find { it.id.equals(name, true) }
    }


}