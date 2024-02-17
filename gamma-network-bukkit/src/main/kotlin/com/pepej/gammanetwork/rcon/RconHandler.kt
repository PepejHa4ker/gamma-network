import com.pepej.gammanetwork.config.RconConfiguration
import com.pepej.gammanetwork.rcon.RconCommandSender
import com.pepej.papi.scheduler.Schedulers
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import org.bukkit.Server
import org.bukkit.command.CommandException
import org.bukkit.event.server.RemoteServerCommandEvent
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

class RconHandler(private val server: Server, private val config: RconConfiguration) : SimpleChannelInboundHandler<ByteBuf>() {
    private val commandSender = RconCommandSender(server)
    private var loggedIn = false

    private val log = LoggerFactory.getLogger(RconHandler::class.java)


    private fun handleLogin(ctx: ChannelHandlerContext, payload: String, requestId: Int) {
        if (config.password == payload) {
            loggedIn = true
            sendResponse(ctx, requestId, TYPE_COMMAND, "")
            log.info("Rcon connection from [{}]", ctx.channel().remoteAddress())
        } else {
            loggedIn = false
            sendResponse(ctx, -1, TYPE_COMMAND, "")
        }
    }

    private fun handleCommand(ctx: ChannelHandlerContext, payload: String, requestId: Int) {
        if (!loggedIn) {
            sendResponse(ctx, -1, TYPE_COMMAND, "")
        } else {
            try {
                val event = RemoteServerCommandEvent(this.commandSender, payload)
                server.pluginManager.callEvent(event)
                Schedulers.sync().run {
                    server.dispatchCommand(this.commandSender, event.command)
                }
                log.info("Executed command from [{}]: {}",ctx.channel().remoteAddress(), event.command)
                val message = commandSender.flush()
                sendLargeResponse(ctx, requestId, message)
            } catch (ex: CommandException) {
                sendLargeResponse(ctx, requestId, String.format("Error executing: %s (%s)", payload, ex.message))
            }
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, buf: ByteBuf) {

        val requestId = buf.readIntLE()
        val type = buf.readIntLE()
        val payload = readPayload(buf)

        when (type.toByte()) {
            TYPE_LOGIN -> handleLogin(ctx, payload, requestId)
            TYPE_COMMAND -> handleCommand(ctx, payload, requestId)
            else -> sendLargeResponse(ctx, requestId, "Unknown request " + Integer.toHexString(type));
        }
    }

    private fun sendResponse(ctx: ChannelHandlerContext, requestId: Int, type: Byte, payload: String) {
        val buf = ctx.alloc().buffer()
        buf.writeIntLE(requestId)
        buf.writeIntLE(type.toInt())
        buf.writeBytes(payload.toByteArray(StandardCharsets.UTF_8))
        buf.writeByte(0)
        buf.writeByte(0)
        ctx.write(buf)
    }

    private fun sendLargeResponse(ctx: ChannelHandlerContext, requestId: Int, payload: String) {
        if (payload.isEmpty()) {
            sendResponse(ctx, requestId, TYPE_RESPONSE, "")
        } else {
            var truncated: Int
            var start = 0
            while (start < payload.length) {
                val length = payload.length - start
                truncated = length.coerceAtMost(2048)
                this.sendResponse(ctx, requestId, TYPE_RESPONSE, payload.substring(start, truncated))
                start += truncated
            }
        }
    }

    private fun readPayload(buf: ByteBuf): String {
        val payloadBytes = ByteArray(buf.readableBytes() - 2)
        buf.readBytes(payloadBytes)
        buf.skipBytes(2) //two byte padding
        return String(payloadBytes, StandardCharsets.UTF_8)
    }

    companion object {
        private const val FAILURE: Byte = -1
        private const val TYPE_RESPONSE: Byte = 0
        private const val TYPE_COMMAND: Byte = 2
        private const val TYPE_LOGIN: Byte = 3
    }
}