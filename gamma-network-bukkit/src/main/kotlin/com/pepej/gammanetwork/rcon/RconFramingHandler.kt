package com.pepej.gammanetwork.rcon

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec


object RconFramingHandler : ByteToMessageCodec<ByteBuf>() {
    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
        out.writeIntLE(msg.readableBytes())
        out.writeBytes(msg)
    }

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        if (input.readableBytes() >= 4) {
            input.markReaderIndex()
            val length = input.readIntLE()
            if (input.readableBytes() < length) {
                input.resetReaderIndex()
            } else {
                val buf = ctx.alloc().buffer(length)
                input.readBytes(buf, length)
                out.add(buf)
            }
        }
    }
}