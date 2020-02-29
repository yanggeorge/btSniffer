package com.threelambda.btsniffer.bt.udp;


import com.threelambda.btsniffer.bt.DHT;
import com.threelambda.btsniffer.bt.exception.BtSnifferException;
import com.threelambda.btsniffer.bt.exception.NodeIdLengthTooBig;
import com.threelambda.btsniffer.bt.metadata.MetadataRequest;
import com.threelambda.btsniffer.bt.routingtable.Node;
import com.threelambda.btsniffer.bt.routingtable.RoutingTable;
import com.threelambda.btsniffer.bt.tran.Transaction;
import com.threelambda.btsniffer.bt.tran.TransactionManager;
import com.threelambda.btsniffer.bt.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.threelambda.btsniffer.bt.tran.TransactionManager.makeResponseDataMap;

/**
 * @author ym
 * @date 2019/10/09
 */
@Data
@Slf4j
public class IncomingPacketHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private RoutingTable rt;
    private TokenManager tokenManager;
    private TransactionManager transactionManager;
    private BlockingQueue<MetadataRequest> metadataRequestQueue;
    private DHT dht;

    public IncomingPacketHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        try {
            InetSocketAddress sender = msg.sender();
            ByteBuf content = msg.content();
            if (content == null || content.readableBytes() == 0) {
                return;
            }
            log.debug("read={}", ByteBufUtil.hexDump(content));
            Map<String, Object> map = null;
            try {
                map = Util.decode(content);
            } catch (Exception e) {
                log.debug("error", e);
                return;
            }
            String tranId = (String) map.get("t");
            String type = (String) map.get("y");
            if ("r".equals(type)) {
                //response
                Map<String, Object> r = (Map<String, Object>) map.get("r");
                if (r == null) return;

                Transaction tran = transactionManager.getByTranId(tranId);
                if (tran == null) return;
                tran.getResponse().getQueue().offer(new Object(), 0, TimeUnit.SECONDS);

                Map<String, Object> dataMap = tran.getQuery().getDataMap();
                String queryType = (String) dataMap.get("q");
                KRpcType kRpcType = KRpcType.getByCode(queryType);
                if (kRpcType == null) return;
                switch (kRpcType) {
                    case PING: {
                        String nodeId = (String) r.get("id");
                        if (!rt.getNodeById(nodeId).isPresent()) {
                            rt.tryInsert(new Node(nodeId, tran.getQuery().getAddr()));
                        }
                        break;
                    }
                    case FIND_NODE: {
                        String nodeId = (String) r.get("id");
                        String nodes = (String) r.get("nodes");

                        if (!rt.getNodeById(nodeId).isPresent()) {
                            rt.tryInsert(new Node(nodeId, sender));
                        }

                        if (StringUtils.isEmpty(nodes)) return;
                        List<Node> nodeList = Node.decodeNodesInfo(nodes);
                        if (nodeList.size() == 0) return;

                        String localId = rt.getLocalId().rawString();
                        for (Node node : nodeList) {
                            String tmpTranId = transactionManager.genTranId();
                            Transaction transaction = transactionManager.buildPingTransaction(localId, tmpTranId, node.getAddr());
                            dht.retrySubmit(transaction);
                        }
                        break;
                    }
                    default:
                        break;
                }
            } else if ("q".equals(type)) {
                //request
                Map<String, Object> a = (Map<String, Object>) map.get("a");
                if (a == null) return;
                String queryType = (String) map.get("q");
                KRpcType kRpcType = KRpcType.getByCode(queryType);
                if (kRpcType == null) return;

                switch (kRpcType) {
                    case PING: {
                        //获取sender的id，address，生成node，插入到路由表
                        String nodeId = (String) a.get("id");

                        if (!rt.getNodeById(nodeId).isPresent()) {
                            rt.tryInsert(new Node(nodeId, sender));
                        }

                        //返回response
                        Map<String, Object> r = new HashMap<>();
                        r.put("id", rt.getLocalId().rawString());
                        Map<String, Object> dataMap = makeResponseDataMap(tranId, r);
                        ByteBuf buf = Unpooled.buffer();
                        Util.encode(buf, dataMap);
                        log.debug("ping|write={}", ByteBufUtil.hexDump(buf));
                        DatagramPacket packet = new DatagramPacket(buf, sender);
                        ctx.writeAndFlush(packet);
                        break;
                    }
                    case FIND_NODE: {
                        String nodeId = (String) a.get("id");
                        String targetId = (String) a.get("target");
                        if (!rt.getNodeById(nodeId).isPresent()) {
                            rt.tryInsert(new Node(nodeId, sender));
                        }
                        //查看targetId是否在路由表里，如果在返回，如果不在返回最相邻的8个节点
                        String nodes = null;
                        Optional<Node> targetNodeOptional = rt.getNodeById(targetId);
                        if (targetNodeOptional.isPresent()) {
                            Node node = targetNodeOptional.get();
                            nodes = Util.toString(node.compactNodeInfo());
                        } else {
                            List<Node> nearest = rt.getNearest(targetId);
                            StringBuilder sb = new StringBuilder();
                            for (Node node : nearest) {
                                sb.append(Util.toString(node.compactNodeInfo()));
                            }
                            nodes = sb.toString();
                        }
                        Map<String, Object> r = new HashMap<>();
                        r.put("id", rt.getLocalId().rawString());
                        r.put("nodes", nodes);
                        Map<String, Object> dataMap = makeResponseDataMap(tranId, r);
                        ByteBuf buf = Unpooled.buffer();
                        Util.encode(buf, dataMap);
                        DatagramPacket packet = new DatagramPacket(buf, sender);
                        ctx.writeAndFlush(packet);
                        break;
                    }
                    case GET_PEERS: {
                        //以下仅仅考虑爬虫模式
                        String infoHash = (String) a.get("info_hash");
                        if (infoHash.length() != 20) {
                            //返回ERROR
                            return;
                        }
                        String token = tokenManager.token(sender.getHostString()).getToken();
                        Map<String, Object> r = new HashMap<>();
                        r.put("id", infoHash);
                        r.put("token", token);
                        r.put("nodes", "");
                        Map<String, Object> dataMap = makeResponseDataMap(tranId, r);
                        ByteBuf buf = Unpooled.buffer();
                        Util.encode(buf, dataMap);
                        DatagramPacket packet = new DatagramPacket(buf, sender);
                        ctx.writeAndFlush(packet);
                        break;
                    }
                    case ANNOUNCE_PEER: {
                        String infoHash = (String) a.get("info_hash");

                        if (infoHash.length() != 20) {
                            return;
                        }

                        Long port = (Long) a.get("port");
                        String token = (String) a.get("token");

                        if (!tokenManager.check(sender.getHostString(), token)) {
                            return;
                        }
                        Long impliedPort = (Long) a.get("implied_port");
                        if (impliedPort != null && impliedPort != 0) {
                            port = (long) sender.getPort();
                        }
                        //如果是标准模式则需要保存peer

                        log.debug("announce_peer|{}:{} info_hash={}", sender.getHostString(), port, Util.hex(infoHash));
                        //提交请求
                        MetadataRequest request = new MetadataRequest();
                        request.setIp(sender.getHostString());
                        request.setPort(port.intValue());
                        request.setInfoHashHex(Util.hex(infoHash));
                        try {
                            metadataRequestQueue.offer(request, 1, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            log.error("error", e);
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        } catch (BtSnifferException e) {
            if (e instanceof NodeIdLengthTooBig) {
                return;
            }
            log.error("BtSniffer error", e);
        } catch (Exception e) {
            log.error("error", e);
        }
    }

}

