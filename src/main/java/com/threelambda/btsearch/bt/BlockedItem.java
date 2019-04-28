package com.threelambda.btsearch.bt;

import org.joda.time.DateTime;

/**
 * Created by ym on 2019-04-28
 */
public class BlockedItem {

    private final String ip;
    private final int port;
    private final DateTime createTime;

    public BlockedItem(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.createTime = DateTime.now();
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public DateTime getCreateTime() {
        return createTime;
    }
}
