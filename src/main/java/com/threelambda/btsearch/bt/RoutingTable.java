package com.threelambda.btsearch.bt;

import lombok.Data;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ym on 2019-09-11
 */
@Data
public class RoutingTable {
    private BitMap localId;
    private RoutingTableNode root;
    private ConcurrentLinkedQueue<Node> cacheNodes;
    private ConcurrentLinkedQueue<Node> cacheCandidates;
    private Integer bucketSize;

    RoutingTable(){}

    RoutingTable(BitMap localId) {
        this.localId = localId;
        this.root = new RoutingTableNode(new BitMap(0));
        this.cacheNodes = new ConcurrentLinkedQueue<>();
        this.bucketSize = 8;
    }

    public void insert(Node node) {
        RoutingTableNode rt = this.root;
        Integer commonPrefixLength = BitMap.getCommonPrefixLength(this.localId, node.getId());

        int max = this.localId.getSize();

        int i = 0;
        //如果有子节点，则该节点的kBucket必为null，反之亦然。
        while(i < max  && rt.getKBucket() == null){
            rt = rt.child(node.getId().bit(i));
            i++;
        }


        KBucket kBucket = rt.getKBucket();
        if(i >= max) {
            if(kBucket.getSizeOfCandidates() < this.bucketSize ){
                boolean isNew = kBucket.insertCandi(node);
                if (isNew) {
                    this.cacheCandidates.add(node);
                }
            }

            return;
        }


        //如果i > commonPrefixLength ，则不在localId的分支上，不能分裂
        if(i > commonPrefixLength){
            if(kBucket.getSizeOfNodes() < this.bucketSize){
                boolean isNew = kBucket.insert(node);
                if(isNew) {
                    this.cacheNodes.add(node);
                }
                return;
            }

            if(kBucket.getSizeOfCandidates() < this.bucketSize ){
                boolean isNew = kBucket.insertCandi(node);
                if (isNew) {
                    this.cacheCandidates.add(node);
                }
            }
            return;
        }

        //i <= commonPrefixLength, 则如果满了可以分裂。
        if (kBucket.getSizeOfNodes() < this.bucketSize) {
            boolean isNew = kBucket.insert(node);
            if(isNew) {
                this.cacheNodes.add(node);
            }
            return;
        }

        //如果满了则分裂
        rt.split();

        RoutingTableNode child = rt.child(node.getId().bit(i));
        KBucket childKBucket = child.getKBucket();
        if (childKBucket.getSizeOfNodes() < this.bucketSize) {
            boolean isNew = childKBucket.insert(node);
            if (isNew) {
                this.cacheNodes.add(node);
            }
            return;
        }

        if (childKBucket.getSizeOfCandidates() < this.bucketSize) {
            boolean isNew = childKBucket.insertCandi(node);
            if (isNew) {
                this.cacheCandidates.add(node);
            }
        }

    }

}
