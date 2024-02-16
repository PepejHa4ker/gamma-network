package com.pepej.gammanetwork.rcon

import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.command.RemoteConsoleCommandSender
import org.bukkit.permissions.PermissibleBase
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.permissions.PermissionAttachmentInfo
import org.bukkit.plugin.Plugin

class RconCommandSender(
    private val server: Server
) : RemoteConsoleCommandSender {

    private val buffer = StringBuffer()
    private val base = PermissibleBase(this)

    override fun isPermissionSet(name: String?): Boolean {
        return this.base.isPermissionSet(name)
    }

    override fun isPermissionSet(perm: Permission?): Boolean {
        return this.base.isPermissionSet(perm)
    }

    override fun hasPermission(name: String?): Boolean {
        return this.base.hasPermission(name)
    }

    override fun hasPermission(perm: Permission?): Boolean {
        return this.base.hasPermission(perm)
    }

    override fun addAttachment(plugin: Plugin?, name: String?, value: Boolean): PermissionAttachment {
        return this.base.addAttachment(plugin, name, value)
    }

    override fun addAttachment(plugin: Plugin?): PermissionAttachment {
        return this.base.addAttachment(plugin)
    }

    override fun addAttachment(plugin: Plugin?, name: String?, value: Boolean, ticks: Int): PermissionAttachment {
        return this.base.addAttachment(plugin, name, value, ticks)
    }

    override fun addAttachment(plugin: Plugin?, ticks: Int): PermissionAttachment {
        return this.base.addAttachment(plugin, ticks)
    }

    override fun removeAttachment(attachment: PermissionAttachment?) {
        this.base.removeAttachment(attachment)
    }

    override fun recalculatePermissions() {
        this.base.recalculatePermissions()
    }

    override fun getEffectivePermissions(): Set<PermissionAttachmentInfo> {
        return this.base.effectivePermissions
    }

    override fun isOp(): Boolean {
        return true
    }

    override fun setOp(value: Boolean) {
        throw UnsupportedOperationException("Cannot change operator status of Rcon command sender")
    }

    override fun sendMessage(message: String?) {
        this.buffer.append(message).append("\n")
    }

    override fun sendMessage(strings: Array<String>) {
        strings.forEach(this::sendMessage)
    }

    override fun getServer(): Server {
        return this.server
    }

    override fun getName(): String {
        return "Rcon"
    }

    fun flush(): String {
        val result = buffer.toString()
        buffer.setLength(0)
        return result
    }

    override fun spigot(): CommandSender.Spigot {
        throw UnsupportedOperationException()
    }
}
