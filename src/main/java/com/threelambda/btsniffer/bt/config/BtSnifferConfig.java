package com.threelambda.btsniffer.bt.config;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.threelambda.btsniffer.bt.routingtable.BlackList;
import com.threelambda.btsniffer.bt.DHT;
import com.threelambda.btsniffer.bt.routingtable.Node;
import com.threelambda.btsniffer.bt.routingtable.DynamicRoutingTable;
import com.threelambda.btsniffer.bt.routingtable.RoutingTable;
import com.threelambda.btsniffer.bt.util.Util;
import com.threelambda.btsniffer.bt.metadata.Metadata;
import com.threelambda.btsniffer.bt.metadata.MetadataRequest;
import com.threelambda.btsniffer.bt.udp.TokenManager;
import com.threelambda.btsniffer.bt.tran.TransactionManager;
import com.threelambda.btsniffer.bt.udp.IncomingPacketHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * config
 *
 * @author ym
 * @date
 */
@Configuration
@Slf4j
public class BtSnifferConfig {
    @Value("${udp.port}")
    private Integer port;
    @Value("${btsniffer.platform}")
    private String platform;
    @Autowired
    private TransactionManager transactionManager;
    @Autowired
    private DHT dht;

    @Bean(name = "udpChannel")
    Channel rpcChannel() {
        final NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            final Bootstrap b = new Bootstrap();
            final IncomingPacketHandler incomingPacketHandler = new IncomingPacketHandler();
            incomingPacketHandler.setRt(routingTable());
            incomingPacketHandler.setTokenManager(tokenManager());
            incomingPacketHandler.setTransactionManager(transactionManager);
            incomingPacketHandler.setDht(dht);
            incomingPacketHandler.setMetadataRequestQueue(metadataRequestBlockingQueue());
            b.group(group).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        public void initChannel(final NioDatagramChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
//                            p.addLast("logging", new LoggingHandler(LogLevel.INFO));
                            p.addLast("handler", incomingPacketHandler);
                        }
                    });

            // Bind and start to accept incoming connections.
            String inetHost = "0.0.0.0";
            Channel channel = b.bind(inetHost, port).sync().channel();
            log.info("bind to {}:{}", inetHost, port);
            return channel;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {

        }
        return null;
    }

    @Bean("localNode")
    Node localNode() {
        byte[] id = Util.createPeerId().getBytes(Charsets.ISO_8859_1);
        String ip = getIpByPlatform();
        return new Node(id, ip, port);
    }

    @Bean
    RoutingTable routingTable() {
        return new DynamicRoutingTable(localNode().getId());
    }

    @Bean
    TokenManager tokenManager() {
        return new TokenManager();
    }

    @Bean("localIp")
    public String getIpByPlatform() {
        if ("mac".equals(platform)) {
            return Util.getCurrentIpOnMac();
        } else if ("linux".equals(platform)) {
            return Util.getCurrentIp();
        }
        return "";
    }

    @Bean("queryExecutor")
    public ExecutorService queryExecutor() {
        ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("queryExecutor-%d").build();
        return Executors.newSingleThreadExecutor(factory);
    }

    @Bean("scheduleExecutor")
    public ScheduledExecutorService scheduleExecutor() {
        ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("scheduleExecutor-%d").build();
        return Executors.newScheduledThreadPool(4, factory);
    }

    @Bean
    public BlackList blackList() {
        return new BlackList(20000);
    }

    @Bean("metadataQueue")
    public BlockingQueue<Metadata> metadataBlockingQueue() {
        return new ArrayBlockingQueue<Metadata>(10);
    }

    @Bean("metadataRequestQueue")
    public BlockingQueue<MetadataRequest> metadataRequestBlockingQueue() {
        return new ArrayBlockingQueue<MetadataRequest>(10);
    }

    @Bean("metadataRequestExecutor")
    public ExecutorService metadataRequestExecutor() {
        ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("metadataRequestExecutor-%d").build();
        return Executors.newSingleThreadExecutor(factory);
    }

    @Bean("metadataExecutor")
    public ExecutorService metadataExecutor() {
        ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("metadataExecutor-%d").build();
        return Executors.newSingleThreadExecutor(factory);
    }

    @Bean("eventLoopGroup")
    public EventLoopGroup eventLoopGroup() {
        return new NioEventLoopGroup();
    }
}
