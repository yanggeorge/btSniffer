package com.threelambda.btsniffer.bt.routingtable;

import com.threelambda.btsniffer.bt.util.BitMap;
import com.threelambda.btsniffer.bt.util.Pair;

import java.util.List;
import java.util.Optional;

/**
 * @author ym
 */
public interface RoutingTable {
    /**
     *  如果不存在则插入。
     *
     * @param node
     */
    void tryInsert(Node node);

    void removeByNodeId(String nodeId);

    void removeByAddr(String addr);

    List<Node> getNearest(String targetId);

    void logMetric();

    int getNodeSize();

    BitMap getLocalId();

    int getTableLength();

    /**
     * 获得过期Node和该node所在的KBucket的prefix
     * @param expireMinutes 过期时间（单位：分钟）
     * @return
     */
    List<Pair<Node, BitMap>> getExpiredNodePairs(int expireMinutes);

    /**
     * 根据nodeId查询node
     * @param nodeId
     * @return
     */
    Optional<Node> getNodeById(String nodeId);

    /**
     * 根据node的地址查询node
     * @param addr
     * @return
     */
    Optional<Node> getNodeByAddr(String addr);
}
