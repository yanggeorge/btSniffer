package com.threelambda.btsearch.bt.config;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.threelambda.btsearch.bt.BlackList;
import com.threelambda.btsearch.bt.DHT;
import com.threelambda.btsearch.bt.Node;
import com.threelambda.btsearch.bt.RoutingTable;
import com.threelambda.btsearch.bt.Util;
import com.threelambda.btsearch.bt.token.TokenManager;
import com.threelambda.btsearch.bt.tran.TransactionManager;
import com.threelambda.btsearch.bt.udp.IncomingPacketHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
public class BtSearchConfig {
    @Value("${udp.port}")
    private Integer port;
    @Value("${btsearch.platform}")
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
            b.group(group).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        public void initChannel(final NioDatagramChannel ch) throws Exception {

                            ChannelPipeline p = ch.pipeline();
                            p.addLast("logging", new LoggingHandler(LogLevel.INFO));
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
        return new RoutingTable(localNode().getId());
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
        return Executors.newScheduledThreadPool(4,factory);
    }

    @Bean
    public BlackList blackList() {
        return new BlackList(20000);
    }
}
