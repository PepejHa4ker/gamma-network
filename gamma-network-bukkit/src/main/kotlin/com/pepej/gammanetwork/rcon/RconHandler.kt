package com.pepej.gammanetwork.rcon

import com.pepej.gammanetwork.config.RconConfiguration
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import org.bukkit.Bukkit
import org.bukkit.command.CommandException
import org.bukkit.event.server.RemoteServerCommandEvent
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets


class RconHandler(
    private val rconServer: RconServer,
    private val rconConfig: RconConfiguration
) : SimpleChannelInboundHandler<ByteBuf>() {
    private var loggedIn = false
    private val commandSender = RconCommandSender(rconServer.server)

    private val log = LoggerFactory.getLogger(RconServer::class.java)

    override fun channelRead0(ctx: ChannelHandlerContext, buf: ByteBuf) {
        if (rconConfig.whitelist) {
            val ip = (ctx.channel().remoteAddress() as InetSocketAddress).address.hostAddress
            if (ip !in rconConfig.whiteListedHosts) {
                log.info("Address [{}] is not whitelisted. Connection rejected.", ctx.channel().remoteAddress())
                this.sendResponse(ctx, -1, 2, "")
                return
            }
        }

        if (buf.readableBytes() >= 8) {
            val requestId = buf.readIntLE()
            val type = buf.readIntLE()
            val payloadData = ByteArray(buf.readableBytes() - 2)
            buf.readBytes(payloadData)
            val payload = String(payloadData, StandardCharsets.UTF_8)
            buf.readBytes(2)
            when (type) {
                3 -> {
                    this.handleLogin(ctx, payload, requestId)
                }
                2 -> {
                    this.handleCommand(ctx, payload, requestId)
                }
                else -> {
                    this.sendLargeResponse(ctx, requestId, "Unknown request " + Integer.toHexString(type))
                }
            }
        }
    }

    private fun handleLogin(ctx: ChannelHandlerContext, payload: String, requestId: Int) {
        if (this.rconConfig.password == payload) {
            this.loggedIn = true
            this.sendResponse(ctx, requestId, 2, "")
            log.info("Rcon connection from [{}]", ctx.channel().remoteAddress())
        } else {
            this.loggedIn = false
            this.sendResponse(ctx, -1, 2, "")
        }
    }

    private fun handleCommand(ctx: ChannelHandlerContext, payload: String, requestId: Int) {
        if (!this.loggedIn) {
            this.sendResponse(ctx, -1, 2, "")
        } else {
            try {
                val event = RemoteServerCommandEvent(this.commandSender, payload)
                Bukkit.getServer().pluginManager.callEvent(event)
                rconServer.server.dispatchCommand(this.commandSender, event.command)
                log.info("Executed command from [{}]: {}", ctx.channel().remoteAddress(), event.command)
                val message = commandSender.flush()
                this.sendLargeResponse(ctx, requestId, message)
            } catch (var6: CommandException) {
                this.sendLargeResponse(ctx, requestId, String.format("Error executing: %s (%s)", payload, var6.message))
            }
        }
    }

    private fun sendResponse(ctx: ChannelHandlerContext, requestId: Int, type: Int, payload: String) {
        val buf = ctx.alloc().buffer()
        buf.writeIntLE(requestId)
        buf.writeIntLE(type)
        buf.writeBytes(payload.toByteArray(StandardCharsets.UTF_8))
        buf.writeByte(0)
        buf.writeByte(0)
        ctx.write(buf)
    }

    private fun sendLargeResponse(ctx: ChannelHandlerContext, requestId: Int, payload: String) {
        if (payload.isEmpty()) {
            this.sendResponse(ctx, requestId, 0, "")
        } else {
            var truncated: Int
            var start = 0
            while (start < payload.length) {
                val length = payload.length - start
                truncated = length.coerceAtMost(2048)
                this.sendResponse(ctx, requestId, 0, payload.substring(start, truncated))
                start += truncated
            }
        }
    }
}