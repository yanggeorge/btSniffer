package com.threelambda.btsearch.bt;

import com.threelambda.btsearch.bt.metadata.Metadata;
import com.threelambda.btsearch.bt.metadata.MetadataDecoder;
import com.threelambda.btsearch.bt.metadata.MetadataHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by ym on 2019-04-23
 */
@Slf4j
public class RequestMetadataTest {

    public static void main(String[] args) throws Exception {
        String currentIpOnMac = Util.getCurrentIpOnMac();
        log.info("currentIp={}",currentIpOnMac);
        start(currentIpOnMac, 40959);
    }

    private static void start(String ip, int port) throws InterruptedException {

        final String infoHashHex = "75146d87adf9af7d17f1803fcaea9c715c73947f";
        final BlockingQueue<Metadata> queue = new LinkedBlockingQueue<>();
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
                            p.addLast(new MetadataHandler(infoHashHex, ip, port, queue));
                        }
                    });
            ChannelFuture channelFuture = b.connect(new InetSocketAddress(ip, port));
            channelFuture.channel().closeFuture();
            //因为是异步
            Metadata metadata = queue.poll(1, TimeUnit.SECONDS);
            if(metadata!=null) {
                log.info("{}",Util.decode(metadata.getMetadata()));
            }else{
                log.info("metadata is null");
            }
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}
