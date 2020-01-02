package com.threelambda.btsniffer.bt;

import com.threelambda.btsniffer.bt.tran.Query;
import com.threelambda.btsniffer.bt.tran.TransactionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author ym
 * @date 2019/10/29
 */
@Slf4j
@Component
public class QueryConsumeInit {
    @Autowired
    private TransactionManager transactionManager;
    @Autowired
    private RoutingTable routingTable;
    @Resource(name = "queryExecutor")
    private ExecutorService queryExecutor;
    @Resource(name = "scheduleExecutor")
    private ScheduledExecutorService scheduleExecutor;
    @Resource(name = "udpChannel")
    private Channel channel;

    @PostConstruct
    public void init() {
        startConsumeQueryQueue();
    }

    public void startConsumeQueryQueue() {
        Runnable r = () -> {
            log.info("start consume queryQueue");
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Query query = transactionManager.getQueryQueue().poll();
                    if (query == null || query.getDataMap() == null || query.getAddr() == null) {
                        continue;
                    }
                    ByteBuf buf = Unpooled.buffer();
                    Util.encode(buf, query.getDataMap());
                    DatagramPacket packet = new DatagramPacket(buf, query.getAddr());
                    channel.writeAndFlush(packet);
                }
            } catch (Exception e) {
                log.error("error", e);
            }
        };
        queryExecutor.submit(r);
    }
}
