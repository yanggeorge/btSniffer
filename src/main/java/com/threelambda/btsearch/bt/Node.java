package com.threelambda.btsearch.bt;

import org.joda.time.DateTime;

import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * Created by ym on 2019-04-28
 */
public class Node {

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

    public DateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public InetSocketAddress getAddr() {
        return addr;
    }

    public BitMap getId() {
        return id;
    }

    public static void main(String[] args) {
        InetSocketAddress address = new InetSocketAddress("192.168.0.1", 1080);
        byte[] compactAddress = Node.encodeCompactAddress(address);
        String ip = Util.getAddr(Arrays.copyOfRange(compactAddress, 0, 4));
        int port = Util.getPort(Arrays.copyOfRange(compactAddress, 4, 6));
        System.out.printf("%s, %d\n", ip, port);
    }

}
