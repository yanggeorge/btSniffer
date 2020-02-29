package com.threelambda.btsniffer.bt;

import com.threelambda.btsniffer.bt.metadata.Metadata;
import com.threelambda.btsniffer.bt.metadata.MetadataDecoder;
import com.threelambda.btsniffer.bt.metadata.MetadataHandler;
import com.threelambda.btsniffer.bt.util.Util;
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
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author ym
 */
@Slf4j
public class RequestMetadataTest {

    /**
     * 从本地的uTorrent软件测试metadata的获取
     */
    @Test
    public void requestMetadata() throws InterruptedException {
        String currentIpOnMac = Util.getCurrentIpOnMac();
        log.info("currentIp={}",currentIpOnMac);
        //uTorrent在本地的端口
        int port = 40959;
        start(currentIpOnMac, port);
    }

    private static void start(String ip, int port) throws InterruptedException {
        //2019-09-26-raspbian-buster.zip 的infohash
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
                            p.addLast(new LoggingHandler(LogLevel.INFO));
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
