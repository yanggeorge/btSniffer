package com.threelambda.btsearch.bt;

import com.google.common.collect.Lists;
import com.threelambda.btsearch.bt.tran.Response;
import com.threelambda.btsearch.bt.tran.Transaction;
import com.threelambda.btsearch.bt.tran.TransactionManager;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    @Override
    public void onApplicationEvent(ContextStartedEvent contextStartedEvent) {
        try {
            log.info("context started");
            //1. 每30秒检查是否活跃
            int checkKBucketTimePeriod = 30;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (routingTable.size() == 0) {
                        DHT.this.join();
                    } else if (transactionManager.size() == 0) {
                        DHT.this.refresh(0);
                    }
                    scheduleExecutor.schedule(this, checkKBucketTimePeriod, TimeUnit.SECONDS);
                }
            };
            scheduleExecutor.execute(r);

            //2. 每10分钟清理BlackList
            int clearBlackListPeriod = 10;
            Runnable blackListR = new Runnable() {
                @Override
                public void run() {
                    blackList.clear();
                    scheduleExecutor.schedule(this, clearBlackListPeriod, TimeUnit.MINUTES);
                }
            };
            scheduleExecutor.schedule(blackListR, clearBlackListPeriod*6, TimeUnit.MINUTES);

            //3. 每分钟查看routingTable
            int checkRoutingTablePeriod = 1;
            Runnable checkRoutingTable = new Runnable() {
                @Override
                public void run() {
                    routingTable.logMetric();
                    scheduleExecutor.schedule(this, checkRoutingTablePeriod, TimeUnit.MINUTES);
                }
            };
            scheduleExecutor.schedule(checkRoutingTable, checkRoutingTablePeriod, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    private void refresh(int expireTime) {
        try {
            log.info("refresh kBucket");
            //15分钟
            //int expireTime = 15;
            String localId = Util.toString(routingTable.getLocalId().getData());
            //如果cachedKBucket过期，则find_node
            DateTime now = DateTime.now();
            ConcurrentHashMap<String, KBucket> cachedKBucketMap = routingTable.getCachedKBucketMap();
            for (Map.Entry<String, KBucket> entry : cachedKBucketMap.entrySet()) {
                KBucket kBucket = entry.getValue();
                DateTime lastChanged = kBucket.getLastChanged();
                if(lastChanged.plus(Duration.standardMinutes(expireTime)).isAfter(now)) continue;

                String targetId = Util.randomChildId(kBucket.getPrefix()).rawString();
                List<Node> nodes = Collections.unmodifiableList(kBucket.getNodes());
                for (Node node : nodes) {
                    String tranId = transactionManager.genTranId();
                    Transaction transaction = transactionManager.getFindNodeTransaction(localId, tranId, targetId, node.getAddr());
                    this.retrySubmit(transaction);
                }
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

        String localId = Util.toString(routingTable.getLocalId().getData());

        for (InetSocketAddress addr : addressList) {
            String tranId = transactionManager.genTranId();
            Transaction transaction = transactionManager.getFindNodeTransaction(localId, tranId, localId, addr);
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
