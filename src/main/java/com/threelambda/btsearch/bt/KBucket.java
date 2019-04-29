package com.threelambda.btsearch.bt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.joda.time.DateTime;

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by ym on 2019-04-28
 */
public class KBucket {

    private final BitMap prefix;
    private final ConcurrentLinkedDeque<Node> nodes;
    private final ConcurrentLinkedDeque<Node> candidates;
    private DateTime lastChanged;

    public KBucket(BitMap prefix) {
        nodes = new ConcurrentLinkedDeque<>();
        candidates = new ConcurrentLinkedDeque<>();
        lastChanged = DateTime.now();
        this.prefix = prefix;
    }

    public synchronized DateTime getLastChanged() {
        return lastChanged;
    }

    public synchronized void updateLastChanged() {
        this.lastChanged = DateTime.now();
    }
}
