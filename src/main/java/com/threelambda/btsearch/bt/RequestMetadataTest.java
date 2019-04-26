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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by ym on 2019-04-23
 */
public class RequestMetadataTest {

    public static void main(String[] args) throws Exception {
        String currentIpOnMac = Util.getCurrentIpOnMac();
        System.out.println(currentIpOnMac);
        start(currentIpOnMac, 40959);
    }

    private static void start(String addr, int port) throws InterruptedException {

        final String infoHash = "e84213a794f3ccd890382a54a64ca68b7e925433";
        final BlockingQueue<Metadata> queue = new LinkedBlockingQueue();
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel sc) throws Exception {
                            ChannelPipeline p = sc.pipeline();
//                            p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new MetadataDecoder());
                            p.addLast(new MetadataHandler(infoHash, addr, port, queue));
                        }
                    });
            ChannelFuture channelFuture = b.connect(new InetSocketAddress(addr, port)).sync();
            channelFuture.channel().closeFuture().sync();
            Metadata metadata = queue.poll(1, TimeUnit.SECONDS);
            System.out.println(Util.parse(metadata.getMetadata()));
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}
