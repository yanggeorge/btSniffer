package com.threelambda.btsearch.bt;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

/**
 * Created by ym on 2019-04-23
 */
public class RequestMetadata {

    public static void main(String[] args) throws Exception {
        String currentIpOnMac = Util.getCurrentIpOnMac();
        System.out.println(currentIpOnMac);

        start(currentIpOnMac, 40959);
//        start("127.0.0.1", 8108);
    }

    private static void start(String addr, int port) throws InterruptedException {

        final String infoHash = "e84213a794f3ccd890382a54a64ca68b7e925433";
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();

            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel sc) throws Exception {
                            ChannelPipeline p = sc.pipeline();
//                            p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new HandshakeDecoder());
                            p.addLast(new HandshakeHandler(infoHash));
                        }
                    });
            ChannelFuture channelFuture = b.connect(new InetSocketAddress(addr, port)).sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}
