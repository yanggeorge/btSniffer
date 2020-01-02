package com.threelambda.btsniffer.bt;

import java.util.Arrays;

/**
 * Created by ym on 2019-04-28
 */
public class Peer {
    private final String ip;
    private final int port;
    private final String token;


    public Peer(String ip, int port, String token) {
        this.ip = ip;
        this.port = port;
        this.token = token;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getToken() {
        return token;
    }

    public static Peer fromCompactInfo(byte[] compactInfo, String token) {
        if (compactInfo.length != 6) {
            throw new RuntimeException("must be 6 byte");
        }
        String ip = Util.getAddr(Arrays.copyOfRange(compactInfo, 0, 4));
        int port = Util.getPort(Arrays.copyOfRange(compactInfo, 4, 6));
        return new Peer(ip, port, token);
    }


}
