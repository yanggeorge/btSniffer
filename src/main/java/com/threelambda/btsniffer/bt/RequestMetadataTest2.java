package com.threelambda.btsniffer.bt;

import com.threelambda.btsniffer.bt.metadata.Metadata;
import com.threelambda.btsniffer.bt.metadata.MetadataDecoder;
import com.threelambda.btsniffer.bt.metadata.MetadataHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by ym on 2019-04-23
 */
@Slf4j
public class RequestMetadataTest2 {

    public static void main(String[] args) throws Exception {
        String currentIpOnMac = Util.getCurrentIpOnMac();
        log.info("currentIp={}", currentIpOnMac);
        start(currentIpOnMac, 40959);
    }

    private static void start(String ip, int port) throws InterruptedException {

        final String infoHashHex = "75146d87adf9af7d17f1803fcaea9c715c73947f";
        final BlockingQueue<Metadata> queue = new LinkedBlockingQueue<>();
        EventLoopGroup group = new NioEventLoopGroup();

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel sc) throws Exception {
                        ChannelPipeline p = sc.pipeline();
//                        p.addLast(new LoggingHandler(LogLevel.INFO));
                        p.addLast(new ReadTimeoutHandler(3, TimeUnit.SECONDS));
                        p.addLast(new MetadataDecoder());
                        p.addLast(new MetadataHandler(infoHashHex, ip, port, queue));
                    }
                });
        //b.connect(new InetSocketAddress(ip, port));
        Bootstrap b2 = new Bootstrap();
        b2.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel sc) throws Exception {
                        ChannelPipeline p = sc.pipeline();
//                        p.addLast(new LoggingHandler(LogLevel.INFO));
                        p.addLast(new ReadTimeoutHandler(3, TimeUnit.SECONDS));
                        p.addLast(new MetadataDecoder());
                        p.addLast(new MetadataHandler(infoHashHex, ip, port, queue));
                    }
                });
        b2.connect(new InetSocketAddress(ip, port));
        //因为是异步
        //TimeUnit.SECONDS.sleep(5);
        Metadata metadata = queue.poll(1, TimeUnit.SECONDS);
        if (metadata != null) {
            log.info("{}", Util.decode(metadata.getMetadata()));
        } else {
            log.info("metadata is null");
        }
        metadata = queue.poll(1, TimeUnit.SECONDS);
        if (metadata != null) {
            log.info("{}", Util.decode(metadata.getMetadata()));
        } else {
            log.info("metadata is null");
        }
    }
}
