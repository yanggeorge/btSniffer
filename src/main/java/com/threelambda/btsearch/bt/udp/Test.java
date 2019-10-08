package com.threelambda.btsearch.bt.udp;

import com.threelambda.btsearch.bt.DHT;
import com.threelambda.btsearch.bt.RoutingTable;
import com.threelambda.btsearch.bt.Util;
import com.threelambda.btsearch.bt.tran.Transaction;
import com.threelambda.btsearch.bt.tran.TransactionManager;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetSocketAddress;

/**
 * Created by ym on 2019-10-08
 */
@Slf4j
@Component
public class Test {

    @Resource(name = "udpChannel")
    private Channel channel;
    @Autowired
    private TransactionManager transactionManager;
    @Autowired
    private RoutingTable routingTable;
    @Resource(name = "localIp")
    private String localIp;
    @Autowired
    private DHT dht;

    //@Scheduled(fixedRateString = "200000", initialDelayString = "1000")
    public void test1() {
        String localId = Util.toString(routingTable.getLocalId().getData());
        String tranId = transactionManager.genTranId();
        log.info("gen tranId={}",Util.stringDecodeToInt(tranId));
        InetSocketAddress addr = new InetSocketAddress(localIp, 40959);

        Transaction transaction = transactionManager.getPingTransaction(localId, tranId, addr);
        dht.retrySubmit(transaction);
    }



    //@Scheduled(fixedRateString = "200000", initialDelayString = "1000")
    public void test2() {
        log.info("localIp={}", localIp);
        String localId = Util.toString(routingTable.getLocalId().getData());
        String tranId = transactionManager.genTranId();
        InetSocketAddress addr = new InetSocketAddress(localIp, 40959);
        String targetId = Util.toString(ByteBufUtil.decodeHexDump("6cc44b3cd76ddd4920f4f6c1e03ae122cef398ee"));
        Transaction transaction = transactionManager.getFindNodeTransaction(localId, tranId, targetId, addr);
        dht.retrySubmit(transaction);
    }


}
