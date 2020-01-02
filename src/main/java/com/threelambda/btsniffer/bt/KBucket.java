package com.threelambda.btsniffer.bt;

import lombok.Data;
import org.joda.time.DateTime;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ym on 2019-04-28
 */
@Data
public class KBucket {

    private final BitMap prefix;
    private final LinkedList<Node> nodes;
    private final LinkedList<Node> candidates;
    private DateTime lastChanged;

    public KBucket(BitMap prefix) {
        nodes = new LinkedList<>();
        candidates = new LinkedList<>();
        lastChanged = DateTime.now();
        this.prefix = prefix;
    }

    public synchronized DateTime getLastChanged() {
        return lastChanged;
    }

    public synchronized void updateLastChanged() {
        this.lastChanged = DateTime.now();
    }

    public synchronized boolean insert(Node node) {
        boolean isNew = !nodes.contains(node);
        if(isNew) {
            nodes.push(node);
        }
        this.updateLastChanged();
        return isNew;
    }

    public synchronized boolean insertCandi(Node node) {
        boolean isNew = !candidates.contains(node);
        candidates.push(node);
        this.updateLastChanged();
        return isNew;
    }

    public synchronized void replace(Node node) {
        nodes.remove(node);

        if (candidates.size() == 0) {
            return;
        }

        Node last = candidates.removeLast();
        if (nodes.size() == 0) {
            nodes.push(last);
            return;
        }

        boolean inserted = false;
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            long t = n.getLastActiveTime().getMillis();
            if (node.getLastActiveTime().getMillis() < t) {
                nodes.add(i, node);
                inserted = true;
                break;
            }
        }

        if (!inserted) {
            nodes.push(node);
        }
        this.updateLastChanged();
    }

    public synchronized Integer getSizeOfNodes() {
        return nodes.size();
    }

    public synchronized Integer getSizeOfCandidates() {
        return candidates.size();
    }

    /**
     * 获取nodes的id列表
     * @return
     */
    public synchronized Optional<Node> getNodeById(String id) {

        List<Node> list = nodes.stream().filter(node -> {
            return node.getId().rawString().equals(id);
        }).collect(Collectors.toList());
        if (list.size() > 0) {
            return Optional.of(list.get(0));
        }
        return Optional.empty();
    }
}
