package com.threelambda.btsearch.bt.example;

import com.threelambda.btsearch.bt.example.echo.EchoServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ym on 2019-04-24
 */
public class ServerExample {

    private Logger logger = LoggerFactory.getLogger(ServerExample.class);

    public static void main(String[] args) {
        try {
            new ServerExample().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void start() {
        int PORT = 8108;
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        final SocksServerExampleHandler socksServerExampleHandler = new SocksServerExampleHandler();
//        final EchoServerHandler echoServerHandler = new EchoServerHandler();
        ServerBootstrap b = new ServerBootstrap();
        try {
            b.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO), socksServerExampleHandler);
                        }
                    });

            logger.info("[127.0.0.1:{}] bind.", PORT);
            ChannelFuture future = b.bind(PORT).sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }

    }
}
