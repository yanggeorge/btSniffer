package com.threelambda.btsearch.bt;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;

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
            throw new RuntimeException("must 20 byte");
        }

        this.id = BitMap.fromBytes(id);
        this.addr = new InetSocketAddress(ip, port);
        this.lastActiveTime = DateTime.now();
    }

    public static Node fromCompactInfo(byte[] compactInfo) {
        if (compactInfo.length != 26) {
            throw new RuntimeException("must 26 byte");
        }

        byte[] id = Arrays.copyOfRange(compactInfo, 0, 20);
        String ip = Util.getAddr(Arrays.copyOfRange(compactInfo, 20, 24));
        int port = Util.getPort(Arrays.copyOfRange(compactInfo, 24, 26));
        return new Node(id, ip, port);
    }

    public byte[] compactNodeInfo() {
        ByteBuf buf = Unpooled.buffer(26);
        buf.writeBytes(this.id.getData());
        buf.writeBytes(encodeCompactAddress(this.addr));
        return buf.array();
    }

    public static byte[] encodeCompactAddress(String ip, int port) {
        byte[] info = new byte[6];
        String[] arr = ip.split("\\.");
        for (int i = 0; i < arr.length; i++) {
            info[i] = (byte) (Integer.parseInt(arr[i]));
        }
        info[4] = (byte) (port / 256);
        info[5] = (byte) (port % 256);
        return info;
    }

    public static byte[] encodeCompactAddress(InetSocketAddress addr) {
        String ip = addr.getHostString();
        int port = addr.getPort();
        return encodeCompactAddress(ip, port);
    }

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
        String ip = Util.getAddr(Arrays.copyOfRange(compactAddress, 0, 4));
        int port = Util.getPort(Arrays.copyOfRange(compactAddress, 4, 6));
        System.out.printf("%s, %d\n", ip, port);

        byte[] a = new byte[]{(byte) 0xC0, (byte) 0xa8, (byte) 0x00, (byte) 0x01, (byte) 0x04, (byte) 0x38};
        String s = new String(a, Charset.forName("ISO-8859-1"));
        System.out.println(s.length());
        System.out.println(ByteBufUtil.hexDump(a));
        System.out.println(ByteBufUtil.hexDump(s.getBytes(Charset.forName("ISO-8859-1"))));


        LinkedList<Node> list = Lists.newLinkedList();
        byte[] peerId = Util.createPeerId().getBytes();
        Node node1 = new Node(peerId, ip, port);
        list.add(node1);
        Node node2 = new Node(peerId, ip, 25);
        assert list.contains(node2) ;
        list.remove(node2);
        assert list.size() == 0;
    }
}
