package com.threelambda.btsearch.bt;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Created by ym on 2019-04-28
 */
@Data
public class RoutingTableNode implements Serializable {

    private RoutingTableNode[] children;
    private KBucket kBucket;

    RoutingTableNode(BitMap prefix) {
        children = new RoutingTableNode[2];
        kBucket = new KBucket(prefix);
    }

    public RoutingTableNode child(Integer index) {
        if (index >= children.length) {
            throw new IndexOutOfBoundsException();
        }
        return children[index];
    }

    public synchronized boolean insert(Node node) {
        return kBucket.insert(node);
    }

    /**
     * 仅仅split不判断是否需要split
     */
    public synchronized void split() {
        BitMap prefix = kBucket.getPrefix();
        //todo maxLen limit

        BitMap left = BitMap.newBitMapFrom(prefix, prefix.getSize() + 1);
        BitMap right = BitMap.newBitMapFrom(prefix, prefix.getSize() + 1);
        right.set(prefix.getSize());

        this.children[0] = new RoutingTableNode(left);
        this.children[1] = new RoutingTableNode(right);

        LinkedList<Node> nodes = this.kBucket.getNodes();
        for (Node node : nodes) {
            this.child(node.getId().bit(prefix.getSize())).kBucket.getNodes().push(node);
        }

        LinkedList<Node> candidates = this.kBucket.getCandidates();
        for (Node candidate : candidates) {
            this.child(candidate.getId().bit(prefix.getSize())).kBucket.getCandidates().push(candidate);
        }

        for (RoutingTableNode child : children) {
            child.kBucket.updateLastChanged();
        }

        this.kBucket = null;
    }

    public static void main(String[] args) {
        BitMap prefix = new BitMap(2);
        prefix.set(0);
        System.out.println(prefix.toString());
        RoutingTableNode node = new RoutingTableNode(prefix);
        System.out.println(node.child(0));
        System.out.println(node);
    }
}
