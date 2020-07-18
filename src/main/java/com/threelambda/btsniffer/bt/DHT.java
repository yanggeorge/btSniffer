package com.threelambda.btsniffer.bt;

import com.google.common.collect.Lists;
import com.threelambda.btsniffer.bt.routingtable.BlackList;
import com.threelambda.btsniffer.bt.routingtable.Node;
import com.threelambda.btsniffer.bt.routingtable.RoutingTable;
import com.threelambda.btsniffer.bt.tran.Query;
import com.threelambda.btsniffer.bt.tran.Response;
import com.threelambda.btsniffer.bt.tran.Transaction;
import com.threelambda.btsniffer.bt.tran.TransactionManager;
import com.threelambda.btsniffer.bt.udp.TokenManager;
import com.threelambda.btsniffer.bt.util.BitMap;
import com.threelambda.btsniffer.bt.util.Pair;
import com.threelambda.btsniffer.bt.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author ym
 * @date 2019/10/22
 */
@Slf4j
@Component
public class DHT implements ApplicationListener<ContextStartedEvent> {
    @Autowired
    private TransactionManager transactionManager;
    @Autowired
    private RoutingTable routingTable;
    @Resource(name = "scheduleExecutor")
    private ScheduledExecutorService scheduleExecutor;
    @Autowired
    private BlackList blackList;
    @Autowired
    private TokenManager tokenManager;
    @Resource(name = "queryExecutor")
    private ExecutorService queryExecutor;
    @Resource(name = "udpChannel")
    private Channel channel;


    @Override
    public void onApplicationEvent(ContextStartedEvent contextStartedEvent) {
        try {
            log.info("context started");

            //启动消费Query队列
            startConsumeQueryQueue();

            //1. 每30秒检查是否活跃
            startActivityCheckScheduler();

            //2. 每10分钟清理BlackList
            startBlackListClearScheduler();

            //3. 每分钟查看routingTable
            startRoutingTableCheckScheduler();

            //4. 每三分钟清除过期token
            startExpiredTokenClearScheduler();
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    private void startExpiredTokenClearScheduler() {
        int checkExpiredTokenPeriod = 3;
        Runnable checkExpiredToken = new Runnable() {
            @Override
            public void run() {
                int beforeSize = tokenManager.size();
                tokenManager.clear();
                int afterSize = tokenManager.size();
                log.info("tokenManager contains beforeSize, afterSize={}, {}", beforeSize, afterSize);
                scheduleExecutor.schedule(this, checkExpiredTokenPeriod, TimeUnit.MINUTES);
            }
        };
        scheduleExecutor.schedule(checkExpiredToken, checkExpiredTokenPeriod, TimeUnit.MINUTES);
    }

    private void startRoutingTableCheckScheduler() {
        int checkRoutingTablePeriod = 1;
        Runnable checkRoutingTable = new Runnable() {
            @Override
            public void run() {
                routingTable.logMetric();
                scheduleExecutor.schedule(this, checkRoutingTablePeriod, TimeUnit.MINUTES);
            }
        };
        scheduleExecutor.schedule(checkRoutingTable, checkRoutingTablePeriod, TimeUnit.MINUTES);
    }

    private void startBlackListClearScheduler() {
        int clearBlackListPeriod = 10;
        Runnable blackListR = new Runnable() {
            @Override
            public void run() {
                blackList.clear();
                scheduleExecutor.schedule(this, clearBlackListPeriod, TimeUnit.MINUTES);
            }
        };
        scheduleExecutor.schedule(blackListR, clearBlackListPeriod * 6, TimeUnit.MINUTES);
    }

    private void startActivityCheckScheduler() {
        int checkKBucketTimePeriod = 30;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (routingTable.getTableLength() <= 1) {
                    DHT.this.join();
                }

                if (transactionManager.size() == 0) {
                    DHT.this.refresh(0);
                }

                scheduleExecutor.schedule(this, checkKBucketTimePeriod, TimeUnit.SECONDS);
            }
        };
        scheduleExecutor.execute(r);
    }

    private void startConsumeQueryQueue() {
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

    private void refresh(int expireMinutes) {
        try {
            log.info("refresh kBucket");

            String localId = routingTable.getLocalId().rawString();

            List<Pair<Node, BitMap>> pairList = routingTable.getExpiredNodePairs(expireMinutes);
            for (Pair<Node, BitMap> pair : pairList) {
                Node expiredNode = pair.left;
                BitMap bucketPrefix = pair.right;
                String targetId = Util.randomChildId(bucketPrefix).rawString();
                String tranId = transactionManager.genTranId();
                Transaction transaction = transactionManager.buildFindNodeTransaction(localId, tranId, targetId, expiredNode.getAddr());
                this.retrySubmit(transaction);
            }

        } catch (Exception e) {
            log.error("error", e);
        }
    }

    private void join() {
        log.info("join bt network");
        List<InetSocketAddress> addressList = Lists.newArrayList();
        addressList.add(new InetSocketAddress("router.bittorrent.com", 6881));
        addressList.add(new InetSocketAddress("router.utorrent.com", 6881));
        addressList.add(new InetSocketAddress("dht.transmissionbt.com", 6881));

        String localId = routingTable.getLocalId().rawString();

        for (InetSocketAddress addr : addressList) {
            String tranId = transactionManager.genTranId();
            Transaction transaction = transactionManager.buildFindNodeTransaction(localId, tranId, localId, addr);
            this.retrySubmit(transaction);
        }
    }

    public boolean retrySubmit(Transaction transaction) {
        return retrySubmit(transaction, Duration.standardSeconds(15));
    }

    public boolean retrySubmit(Transaction transaction, Duration interval) {
        try {
            if (blackList.in(transaction.getQuery().getAddr())) {
                return false;
            }
            if (transactionManager.submit(transaction)) {
                this.retry(transaction, interval);
                return true;
            }
        } catch (Exception e) {
            log.error("error", e);
        }
        return false;
    }


    /**
     * 如果query没有收到response，则重试
     *
     * @param transaction 事务
     * @param interval    等待的间隔
     */
    public void retry(Transaction transaction, Duration interval) {
        if (transaction.getRetryTimes() <= 0) {
            return;
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                Integer retryTimes = transaction.getRetryTimes();
                Response response = transaction.getResponse();
                Object result = null;
                try {
                    result = response.getQueue().poll(0, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                }
                if (result != null) {
                    //成功
                    transactionManager.remove(transaction.getTranId());
                    return;
                }

                if (retryTimes <= 0) {
                    //失败
                    transactionManager.remove(transaction.getTranId());
                    routingTable.removeByAddr(transaction.getQuery().getAddr().toString());
                    blackList.insert(transaction.getQuery().getAddr());
                    return;
                }

                //重试
                transaction.setRetryTimes(retryTimes - 1);
                scheduleExecutor.schedule(this, interval.getStandardSeconds(), TimeUnit.SECONDS);
            }
        };

        scheduleExecutor.schedule(r, interval.getStandardSeconds(), TimeUnit.SECONDS);
    }

}
