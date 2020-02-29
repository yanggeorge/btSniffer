package com.threelambda.btsniffer.bt;

import com.threelambda.btsniffer.bt.routingtable.Node;
import com.threelambda.btsniffer.bt.util.Util;
import io.netty.buffer.ByteBufUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static com.threelambda.btsniffer.bt.routingtable.Node.decodeCompactNodeInfo;
import static com.threelambda.btsniffer.bt.routingtable.Node.encodeCompactNodeInfo;

/**
 * @author ym
 */
@Slf4j
public class NodeTest {

    @Test
    public void test() {
        InetSocketAddress address = new InetSocketAddress("192.168.0.1", 1080);
        byte[] compactAddress = Node.encodeCompactAddress(address);

        InetSocketAddress addr = Node.decodeCompactAddress(Util.getBytes(Util.toString(compactAddress)));
        System.out.println(addr);
        byte[] id = ByteBufUtil.decodeHexDump("6cc44b3cd76ddd4920f4f6c1e03ae122cef398ee");
        String ip = "10.22.62.31";
        int port = 40959;
        Node node = new Node(id, ip, port);
        byte[] compactNodeInfo = encodeCompactNodeInfo(node);

        StringBuilder sb = new StringBuilder();
        sb.append(Util.toString(compactNodeInfo));
        String nodeInfoString = sb.toString();
        Map<String, Object> map = new HashMap<>();
        map.put("nodes", nodeInfoString);
        Map<String, Object> parse = Util.decode(Util.encode(map));
        Node node2 = decodeCompactNodeInfo(Util.getBytes((String) parse.get("nodes")));
        System.out.println(ByteBufUtil.hexDump(node2.getId().getData()));
        System.out.println(node2.getAddr());
    }
}
