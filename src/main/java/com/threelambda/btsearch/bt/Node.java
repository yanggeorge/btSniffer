package com.threelambda.btsearch.bt;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.threelambda.btsearch.bt.exception.BtSearchException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Byte.toUnsignedInt;

/**
 * Created by ym on 2019-04-28
 */
@Data
public class Node implements Serializable, Comparable<Node> {

    private final InetSocketAddress addr;
    private final BitMap id;
    private DateTime lastActiveTime;

    public Node(byte[] id, String ip, int port) {
        if (id.length != 20) {
            throw new BtSearchException("must 20 byte");
        }

        this.id = BitMap.fromBytes(id);
        this.addr = new InetSocketAddress(ip, port);
        this.lastActiveTime = DateTime.now();
    }

    public Node(BitMap id, String ip, int port) {
        this.id = id;
        this.addr = new InetSocketAddress(ip, port);
        this.lastActiveTime = DateTime.now();
    }

    public Node(String id, String ip, int port) {
        this.id = BitMap.fromRawString(id);
        this.addr = new InetSocketAddress(ip, port);
        this.lastActiveTime = DateTime.now();
    }

    public Node(byte[] id, InetSocketAddress addr) {
        if (id.length != 20) {
            throw new BtSearchException("must 20 byte");
        }

        this.id = BitMap.fromBytes(id);
        this.addr = addr;
        this.lastActiveTime = DateTime.now();
    }

    public Node(String id, InetSocketAddress addr) {
        this.id = BitMap.fromRawString(id);
        this.addr = addr;
        this.lastActiveTime = DateTime.now();
    }

    public static Node fromCompactInfo(byte[] compactInfo) {
        if (compactInfo.length != 26) {
            throw new BtSearchException("must 26 byte");
        }

        byte[] id = Arrays.copyOfRange(compactInfo, 0, 20);
        String ip = Util.getAddr(Arrays.copyOfRange(compactInfo, 20, 24));
        int port = Util.getPort(Arrays.copyOfRange(compactInfo, 24, 26));
        return new Node(id, ip, port);
    }

    public byte[] compactNodeInfo() {
        return encodeCompactNodeInfo(this);
    }

    public static byte[] encodeCompactNodeInfo(Node node) {
        ByteBuf buf = Unpooled.buffer(26);
        buf.writeBytes(node.getId().getData());
        buf.writeBytes(encodeCompactAddress(node.getAddr()));
        return buf.array();
    }

    public static Node decodeCompactNodeInfo(byte[] compactNodeInfo) {
        byte[] id = Arrays.copyOfRange(compactNodeInfo, 0, 20);
        byte[] compactAddr = Arrays.copyOfRange(compactNodeInfo, 20, 26);
        return new Node(id, decodeCompactAddress(compactAddr));
    }

    public static List<Node> decodeNodesInfo(byte[] nodes) {
        List<Node> list = Lists.newArrayList();
        int i = 0;
        while (i < nodes.length - 26) {
            list.add(decodeCompactNodeInfo(Arrays.copyOfRange(nodes, i, i + 26)));
            i += 26;
        }
        return list;
    }

    public static List<Node> decodeNodesInfo(String nodes) {
       return decodeNodesInfo(Util.getBytes(nodes));
    }

    public static byte[] encodeCompactAddress(String ip, int port) {
        ByteBuf buf = Unpooled.buffer(6);
        String[] arr = ip.split("\\.");
        for (int i = 0; i < arr.length; i++) {
            buf.writeByte(Integer.parseInt(arr[i]));
        }
        buf.writeShort(port);
        return buf.array();
    }

    public static InetSocketAddress decodeCompactAddress(byte[] compactAddr) {
        if (compactAddr.length != 6) {
            throw new BtSearchException("length must be 6");
        }
        ByteBuf buf = Unpooled.copiedBuffer(compactAddr);
        int[] arr = new int[]{toUnsignedInt(buf.readByte()),
                toUnsignedInt(buf.readByte()), toUnsignedInt(buf.readByte()), toUnsignedInt(buf.readByte())};
        String ip = String.format("%d.%d.%d.%d", arr[0], arr[1], arr[2], arr[3]);
        int port = buf.readUnsignedShort();
        return new InetSocketAddress(ip, port);
    }

    public static byte[] encodeCompactAddress(InetSocketAddress addr) {
        String ip = addr.getHostString();
        int port = addr.getPort();
        return encodeCompactAddress(ip, port);
    }

    /**
     * 只用id作为节点相等的判断。因为相同的地址可以有多个节点。
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(Node o) {
        return this.id.rawString().compareTo(o.getId().rawString());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Node && this.id.rawString().equals(((Node) obj).getId().rawString());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id.rawString());
    }

    public static void main(String[] args) {
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
