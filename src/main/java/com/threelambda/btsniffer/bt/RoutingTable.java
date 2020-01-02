package com.threelambda.btsniffer.bt;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.threelambda.btsniffer.bt.debug.DebugInfo;
import com.threelambda.btsniffer.bt.exception.NodeIdLengthTooBig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by ym on 2019-09-11
 */
@Data
@Slf4j
public class RoutingTable {
    private final BitMap localId;
    private RoutingTableNode root;
    private ConcurrentHashMap<String, Node> cachedNodeMap;
    private ConcurrentHashMap<String, KBucket> cachedKBucketMap;
    private Integer bucketSize;

    private Integer MAX_LENGTH = 160;

    public RoutingTable(BitMap localId) {
        this.localId = localId;
        this.root = new RoutingTableNode(new BitMap(0));
        this.cachedNodeMap = new ConcurrentHashMap<>();
        this.cachedKBucketMap = new ConcurrentHashMap<>();

        this.bucketSize = 8;
        this.cachedKBucketMap.put(this.root.getPrefix(), this.root.getKBucket());
    }

    public RoutingTable(String localId) {
        this(BitMap.fromRawString(localId));
    }

    public RoutingTable insert(Node node) {
        RoutingTableNode rtNode = this.root;
        BitMap nodeId = node.getId();
        if (nodeId.getSize() > MAX_LENGTH) {
            throw new NodeIdLengthTooBig("nodeId size[" + nodeId.getSize() + "] too big");
        }
        Integer commonPrefixLength = BitMap.getCommonPrefixLength(this.localId, nodeId);

        int max = this.localId.getSize();

        int i = 0;
        //如果有子节点，则该节点的kBucket必为null，反之亦然。
        while (i < max && rtNode.getKBucket() == null) {
            rtNode = rtNode.child(nodeId.bit(i));
            i++;
        }

        KBucket kBucket = rtNode.getKBucket();
        if (i >= max) {
            if (kBucket.getSizeOfCandidates() < this.bucketSize) {
                boolean isNew = kBucket.insertCandi(node);
            }
            return this;
        }

        //如果i > commonPrefixLength ，则不在localId的分支上，不能分裂
        if (i > commonPrefixLength) {
            if (ifNotFullThenInsert(node, kBucket)) return this;

            if (kBucket.getSizeOfCandidates() < this.bucketSize) {
                boolean isNew = kBucket.insertCandi(node);
            }
            return this;
        }

        //只有当 i <= commonPrefixLength, 那么 满了可以分裂。
        if (ifNotFullThenInsert(node, kBucket)) return this;

        //满了则分裂
        KBucket rtNodeBucket = rtNode.split();

        boolean remove = cachedKBucketMap.remove(rtNodeBucket.getPrefix().rawString(), rtNodeBucket);
        if (!remove) {
            throw new RuntimeException("remove false");
        }
        RoutingTableNode[] children = rtNode.getChildren();
        for (RoutingTableNode child : children) {
            cachedKBucketMap.put(child.getPrefix(), child.getKBucket());
        }

        RoutingTableNode child = rtNode.child(nodeId.bit(i));
        KBucket childKBucket = child.getKBucket();
        if (ifNotFullThenInsert(node, childKBucket)) return this;

        return this;
    }

    /**
     * 如果没有满，则插入，返回true
     * 如果满了，直接返回false
     *
     * @param node
     * @param childKBucket
     * @return
     */
    private boolean ifNotFullThenInsert(Node node, KBucket childKBucket) {
        if (childKBucket.getSizeOfNodes() < this.bucketSize) {
            boolean isNew = childKBucket.insert(node);
            if (isNew) {
                this.cachedNodeMap.put(node.getAddr().toString(), node);
            }
            return true;
        }
        return false;
    }

    /**
     * 根据nodeId获取对应的bucket
     *
     * @param nodeId
     * @return
     */
    public KBucket getKBucket(BitMap nodeId) {
//        log.info("nodeId={}", nodeId.toString());
        RoutingTableNode rt = this.root;
        Integer commonPrefixLength = BitMap.getCommonPrefixLength(this.localId, nodeId);

        int i = 0;
        while (i < commonPrefixLength + 1) {
            int pos = nodeId.bit(i++);
            RoutingTableNode child = rt.child(pos);
            if (child == null) {
                //说明是叶子节点，只有叶子节点有KBucket
                break;
            }
            rt = child;
        }
        if (rt.getKBucket() == null) {
            log.info("debugInfo={}", new Gson().toJson(this.build(nodeId.toString())));
        }
        return rt.getKBucket();
    }


    public void removeByNodeId(String nodeId) {
        KBucket kBucket = getKBucket(nodeId);
        if (kBucket == null) return;

        Optional<Node> optionalNode = kBucket.getNodeById(nodeId);
        optionalNode.ifPresent(node -> {
            kBucket.replace(node);
            this.cachedNodeMap.remove(node.getAddr().toString());
        });
    }

    public void removeByAddr(String addr) {
        Node node = cachedNodeMap.getOrDefault(addr, null);
        if (node == null) return;
        removeByNodeId(node.getId().rawString());
    }

    public int size() {
        return cachedNodeMap.size();
    }

    public KBucket getKBucket(String id) {
        return getKBucket(BitMap.fromRawString(id));
    }

    public List<Node> getNearest(BitMap targetId) {
        return getNearest(targetId, bucketSize);
    }

    public List<Node> getNearest(String targetId) {
        return getNearest(BitMap.fromRawString(targetId), bucketSize);
    }

    /**
     * 找到距离最近的8个节点
     *
     * @param targetId
     * @return
     */
    public List<Node> getNearest(BitMap targetId, Integer topK) {
        try {
            BitMap target = targetId;
            PriorityQueue<Node> maxHeap = new PriorityQueue<Node>(
                    topK,
                    (o1, o2) -> {
                        return -target.xor(o1.getId()).compare(target.xor(o2.getId()), MAX_LENGTH);
                    }
            );

            for (Map.Entry<String, Node> entry : cachedNodeMap.entrySet()) {
                Node cacheNode = entry.getValue();
                if (maxHeap.size() < topK) {
                    maxHeap.add(cacheNode);
                    continue;
                }

                if (maxHeap.size() == topK) {
                    BitMap id = cacheNode.getId();
                    int cmp = id.xor(target).compare(maxHeap.peek().getId().xor(target), MAX_LENGTH);
                    if (cmp < 0) {
                        maxHeap.poll();
                        maxHeap.add(cacheNode);
                    }
                }
            }

            List<Node> list = Lists.newArrayList();
            while (!maxHeap.isEmpty()) {
                list.add(maxHeap.poll());
            }
            Collections.reverse(list);
            return list;
        } catch (Exception e) {
            log.error("debugInfo=" + new Gson().toJson(this.build(targetId.toString())), e);
        }
        return new ArrayList<>();
    }

    public void logMetric() {
        try {
            RoutingTableNode rt = this.root;
            int height = 0;
            int totalNode = 0;
            Stack<RoutingTableNode> stack = new Stack<>();
            Stack<RoutingTableNode> tmp = null;
            stack.push(rt);
            while (!stack.isEmpty()) {
                height += 1;
                tmp = new Stack<>();
                while (!stack.isEmpty()) {
                    RoutingTableNode node = stack.pop();
                    if (node.getKBucket() != null) {
                        totalNode += node.getKBucket().getSizeOfNodes();
                    }
                    RoutingTableNode[] children = node.getChildren();
                    if (children != null) {
                        if (children[0] != null) {
                            tmp.push(children[0]);
                        }
                        if (children[1] != null) {
                            tmp.push(children[1]);
                        }
                    }
                }
                stack = tmp;
                tmp = null;
            }
            log.info("height={},totalNode={}", height, totalNode);
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    public DebugInfo build(String insertNodeId) {
        Collection<Node> nodes = Collections.unmodifiableCollection(cachedNodeMap.values());
        List<DebugInfo.DebugNode> debugNodeList = nodes.stream().map(node -> {
            DebugInfo.DebugNode debugNode = new DebugInfo.DebugNode();
            debugNode.setIp(node.getAddr().getHostString());
            debugNode.setPort(node.getAddr().getPort());
            debugNode.setNodeId(node.getId().toString());
            return debugNode;
        }).collect(Collectors.toList());
        DebugInfo debugInfo = new DebugInfo();
        debugInfo.setNodes(debugNodeList);
        debugInfo.setInsertNodeId(insertNodeId);
        debugInfo.setLocalId(this.localId.toString());
        return debugInfo;
    }

    public static void main(String[] args) {
        BitMap localId = new BitMap(160).set(0).set(2).set(3);
        System.out.println(localId.toString());
        RoutingTable rt = new RoutingTable(localId);
        BitMap id1 = new BitMap(160).set(0).set(2).set(3);
        BitMap id2 = new BitMap(160).set(1).set(2).set(3);
        BitMap id3 = new BitMap(160).set(0).set(2).set(4);
        BitMap id4 = new BitMap(160).set(1).set(2).set(6);
        BitMap id5 = new BitMap(160).set(0).set(2).set(6);
        //与id5相同则不会被添加
        BitMap id6 = new BitMap(160).set(0).set(2).set(6);
        BitMap id7 = new BitMap(160).set(0).set(2).set(7);
        BitMap id8 = new BitMap(160).set(0).set(2).set(8);
        BitMap id9 = new BitMap(160).set(0).set(1).set(9);
        BitMap id10 = new BitMap(160).set(0).set(2).set(10);
        BitMap id11 = new BitMap(160).set(0).set(2).set(11);
        rt.insert(new Node(id1.getData(), "0.0.0.1", 10))
                .insert(new Node(id2, "0.0.0.2", 10))
                .insert(new Node(id3, "0.0.0.3", 10))
                .insert(new Node(id4, "0.0.0.4", 10))
                .insert(new Node(id5, "0.0.0.5", 10))
                .insert(new Node(id6, "0.0.0.6", 10))
                .insert(new Node(id7, "0.0.0.7", 10))
                .insert(new Node(id8, "0.0.0.8", 10))
                .insert(new Node(id9, "0.0.0.9", 10))
                .insert(new Node(id10, "0.0.0.10", 10))
                .insert(new Node(id11, "0.0.0.11", 10))
        ;
        System.out.println("---");
        for (Node cacheNode : rt.cachedNodeMap.values()) {
            System.out.println(cacheNode.getId().toString());
        }
        System.out.println("---");

        BitMap targetId = new BitMap(160);
        List<Node> list = rt.getNearest(targetId, 8);

        for (Node node : list) {
            System.out.println(node.getId().toString());
        }

        KBucket kBucket = rt.getKBucket(id1);
        if (kBucket == null) {
            log.info("is null");
        }
        rt.logMetric();
    }
}
