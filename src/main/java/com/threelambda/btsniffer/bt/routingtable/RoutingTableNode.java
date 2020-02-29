package com.threelambda.btsniffer.bt.routingtable;

import com.threelambda.btsniffer.bt.util.BitMap;
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

    public RoutingTableNode(BitMap prefix) {
        children = new RoutingTableNode[2];
        kBucket = new KBucket(prefix);
    }

    public RoutingTableNode child(Integer index) {
        if (index >= children.length) {
            throw new IndexOutOfBoundsException();
        }
        return children[index];
    }


    /**
     * 仅仅split不判断是否需要split
     * <p>
     * 当前节点的KBucket会在分裂后返回
     *
     * @return
     */
    public synchronized KBucket split() {
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

        KBucket kBucket = this.kBucket;
        this.kBucket = null;
        return kBucket;
    }

    public String getPrefix() {
        return this.kBucket.getPrefix().rawString();
    }

}
