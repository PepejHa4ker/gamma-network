package com.pepej.gammanetwork.rcon

import com.pepej.gammanetwork.config.RconConfiguration
import com.pepej.papi.terminable.TerminableConsumer
import com.pepej.papi.terminable.composite.CompositeTerminable
import com.pepej.papi.terminable.module.TerminableModule
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.bukkit.Server


class RconServer(val server: Server, rconConfig: RconConfiguration) : TerminableModule {
    private val bootstrap = ServerBootstrap()
    private val bossGroup: EventLoopGroup = NioEventLoopGroup()
    private val workerGroup: EventLoopGroup = NioEventLoopGroup()
    private val compositeTerminable = CompositeTerminable.create()

    init {
        val serverBootstrap = bootstrap
            .group(this.bossGroup, this.workerGroup)
            .channel(NioServerSocketChannel::class.java) as ServerBootstrap
        serverBootstrap.childHandler(object : ChannelInitializer<SocketChannel>() {
            public override fun initChannel(ch: SocketChannel) {
                ch.pipeline()
                    .addLast(RconFramingHandler)
                    .addLast(RconHandler(this@RconServer, rconConfig))
            }
        })
    }

    fun bind(port: Int): ChannelFuture {
        return bootstrap.bind(port)
    }

    override fun setup(consumer: TerminableConsumer) {
        compositeTerminable
            .with { workerGroup.shutdownGracefully() }
            .with { bossGroup.shutdownGracefully() }
            .bindWith(consumer)
    }


}
