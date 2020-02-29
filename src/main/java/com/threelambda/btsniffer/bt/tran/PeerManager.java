package com.threelambda.btsniffer.bt.tran;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by ym on 2019-04-28
 */
public class PeerManager {

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Peer>> table;

    public PeerManager() {
        this.table = new ConcurrentHashMap<>();
    }

    public void insert(String infoHash, Peer peer) {
        ConcurrentLinkedDeque<Peer> deque = new ConcurrentLinkedDeque<>();
        synchronized (this.table) {
            if (!table.contains(infoHash)) {
                table.put(infoHash, deque);
            }
        }
        deque = table.get(infoHash);
        deque.addLast(peer);
        int MAX_SIZE = 8;
        if (deque.size() > MAX_SIZE) {
            deque.removeFirst();
        }
    }

    public List<Peer> getPeers(String infoHash) {
        ConcurrentLinkedDeque<Peer> deque = this.table.getOrDefault(infoHash, new ConcurrentLinkedDeque<>());
        return new ArrayList<>(deque);
    }


}
