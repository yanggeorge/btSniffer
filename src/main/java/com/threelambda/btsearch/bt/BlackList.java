package com.threelambda.btsearch.bt;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by ym on 2019-04-28
 */
public class BlackList {
    private final ConcurrentHashMap<String, BlockedItem> map = new ConcurrentHashMap<>();
    private final int maxSize;
    private final Duration expireAfter;

    public BlackList(int maxSize) {
        this.maxSize = maxSize;
        this.expireAfter = Duration.standardHours(1);
    }

    public static String genKey(String ip, int port) {
        String key = ip;
        if (port >= 0) {
            key = ip + ":" + port;
        }
        return key;
    }

    public void insert(String ip, int port) {
        synchronized (this.map) {
            if (this.map.size() >= this.maxSize) {
                return;
            }

            this.map.put(genKey(ip, port), new BlockedItem(ip, port));
        }
    }

    public void insert(InetSocketAddress addr) {
        this.insert(addr.getHostString(), addr.getPort());
    }

    public void delete(String ip, int port) {
        synchronized (this.map) {
            this.map.remove(genKey(ip, port));
        }
    }

    public boolean in(String ip, int port) {
        String key = genKey(ip, port);
        DateTime now = DateTime.now();

        synchronized (this.map) {
            BlockedItem item = this.map.getOrDefault(key, null);
            if (item == null) {
                return false;
            }
            if (now.isAfter(item.getCreateTime().plus(this.expireAfter))) {
                this.map.remove(key);
                return false;
            }
            return true;
        }
    }

    public boolean in(InetSocketAddress addr) {
        return this.in(addr.getHostString(), addr.getPort());
    }

    public void clear() {
        List<String> keys = new ArrayList<>();

        DateTime now = DateTime.now();
        for (Map.Entry<String, BlockedItem> entry : map.entrySet()) {
            BlockedItem item = entry.getValue();
            if (now.isAfter(item.getCreateTime().plus(expireAfter))) {
                keys.add(entry.getKey());
            }
        }

        for (String key : keys) {
            synchronized (this.map) {
                this.map.remove(key);
            }
        }
    }


    public static void main(String[] args) {
        Duration duration = Duration.standardSeconds(10);
        DateTime start = DateTime.now();
        DateTime end = start.plus(duration);

        while (true) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            DateTime now = DateTime.now();
            if (!now.isAfter(end)) {
                System.out.println("current now" + now.toString("HH:mm:ss"));
                continue;
            }
            break;
        }
        System.out.println("yes.");
    }



}
