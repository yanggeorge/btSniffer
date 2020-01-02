package com.threelambda.btsniffer.bt.tran;

import com.threelambda.btsniffer.bt.KRpcType;
import com.threelambda.btsniffer.bt.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author ym
 * @date 2019/10/14
 */
@Data
@Slf4j
@Component
public class TransactionManager implements Serializable {
    ReentrantLock lock = new ReentrantLock();
    ConcurrentHashMap<String, Transaction> transactions = new ConcurrentHashMap<>();
    BlockingQueue<Query> queryQueue = new LinkedBlockingDeque<>(1024);
    Integer cursor = 0;
    final Integer maxCursor = Integer.MAX_VALUE;

    public TransactionManager() {
    }

    public String genTranId() {
        lock.lock();
        try {
            cursor = (cursor + 1) % maxCursor;
            String tranId = Util.intEncodeToString(cursor);
            if (transactions.containsKey(tranId)) {
                return genTranId();
            }
            return tranId;
        } finally {
            lock.unlock();
        }
    }

    public Transaction getPingTransaction(String queryerId, String tranId, InetSocketAddress addr) {
        ByteBuf buf = Unpooled.buffer();
        Map<String, Object> dataMap = makePingQuery(tranId, queryerId);
        Util.encode(buf, dataMap);
        Query query = new Query(addr, dataMap);
        return new Transaction(query, tranId);
    }

    public Transaction getFindNodeTransaction(String localId, String tranId, String targetId, InetSocketAddress addr) {
        ByteBuf buf = Unpooled.buffer();
        Map<String, Object> dataMap = makeFindNodeQuery(tranId, localId, targetId);
        Util.encode(buf, dataMap);
        Query query = new Query(addr, dataMap);
        return new Transaction(query, tranId);
    }

    /**
     * 提交后，需要考虑transaction如何从transactionManager中清除
     * @param tran
     * @return
     */
    public boolean submit(Transaction tran) {
        checkNotNull(tran, "tran was null");
        checkNotNull(tran.getTranId(), "tranId was null");

        if (transactions.containsKey(tran.getTranId())) {
            log.error("tranId duplicate|{}", tran);
            return false;
        }
        lock.lock();
        try {
            queryQueue.offer(tran.getQuery(), 1L, TimeUnit.SECONDS);
            transactions.put(tran.getTranId(), tran);
            return true;
        } catch (Exception e) {
            log.error("fail", e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    public Transaction getByTranId(String tranId) {
        checkNotNull(tranId, "tranId was null");
        return transactions.getOrDefault(tranId, null);
    }

    public boolean remove(String tranId) {
        checkNotNull(tranId, "tranId was null");

        if (!transactions.containsKey(tranId)) {
            return false;
        }

        transactions.remove(tranId);
        return true;
    }

    public int size() {
        return transactions.size();
    }

    public static Map<String, Object> makeQueryDataMap(String tranId, String kRpcType, Map<String, Object> a) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("t", tranId);
        dataMap.put("y", "q");
        dataMap.put("q", kRpcType);
        dataMap.put("a", a);
        return dataMap;
    }

    public static Map<String, Object> makePingQuery(String tranId, String queryerId) {
        Map<String, Object> a = new HashMap<>();
        a.put("id", queryerId);
        return makeQueryDataMap(tranId, KRpcType.PING.getCode(), a);
    }

    public static Map<String, Object> makeFindNodeQuery(String tranId, String queryerId, String targetId) {
        Map<String, Object> a = new HashMap<>();
        a.put("id", queryerId);
        a.put("target", targetId);
        return makeQueryDataMap(tranId, KRpcType.FIND_NODE.getCode(), a);
    }

    public static Map<String, Object> makeResponseDataMap(String tranId, Map<String, Object> r) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("t", tranId);
        dataMap.put("y", "r");
        dataMap.put("r", r);
        return dataMap;
    }

    public static void main(String[] args) {
        TransactionManager transactionManager = new TransactionManager();
        String tranId = transactionManager.genTranId();
        System.out.println(tranId.length());
        System.out.println(transactionManager.getCursor());
        byte[] bytes = tranId.getBytes(Charset.forName("ISO-8859-1"));
        System.out.println(ByteBufUtil.hexDump(bytes));
        ByteBuf buf = Unpooled.copiedBuffer(bytes);
        System.out.println(buf.readInt());
    }
}
