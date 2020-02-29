package com.threelambda.btsniffer.bt.routingtable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.threelambda.btsniffer.bt.exception.NodeIdLengthTooBig;
import com.threelambda.btsniffer.bt.util.BitMap;
import com.threelambda.btsniffer.bt.util.DebugInfo;
import com.threelambda.btsniffer.bt.util.Pair;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static com.threelambda.btsniffer.bt.routingtable.KBucket.BUCKET_SIZE;

/**
 * Created by ym on 2019-09-11
 */
@Data
@Slf4j
public class DynamicRoutingTable implements RoutingTable {
    private final BitMap localId;
    private final RoutingTableNode root;
    private final ConcurrentHashMap<String, Pair<Node,KBucket>> addrNodePairMap;
    private final ConcurrentHashMap<String, Pair<Node,KBucket>> idNodePairMap;
    private final ConcurrentHashMap<String, KBucket> prefixKBucketMap;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private final Integer MAX_LENGTH = 160;

    public DynamicRoutingTable(BitMap localId) {
        this.localId = localId;
        this.root = new RoutingTableNode(new BitMap(0));
        this.addrNodePairMap = new ConcurrentHashMap<>();
        this.idNodePairMap = new ConcurrentHashMap<>();
        this.prefixKBucketMap = new ConcurrentHashMap<>();

        this.prefixKBucketMap.put(this.root.getPrefix(), this.root.getKBucket());
    }

    public DynamicRoutingTable(String localId) {
        this(BitMap.fromRawString(localId));
    }

    @Override
    public BitMap getLocalId() {
        return localId;
    }

    @Override
    public int getTableLength() {
        return prefixKBucketMap.size();
    }

    @Override
    public List<Pair<Node, BitMap>> getExpiredNodePairs(int expireMinutes) {
        List<Pair<Node, BitMap>> list = Lists.newArrayList();

        DateTime now = DateTime.now();
        for (Map.Entry<String, KBucket> entry : prefixKBucketMap.entrySet()) {
            KBucket kBucket = entry.getValue();
            BitMap prefix = kBucket.getPrefix();
            DateTime lastChanged = kBucket.getLastChanged();
            if (lastChanged.plus(Duration.standardMinutes(expireMinutes)).isAfter(now)) continue;
            for (Node node : kBucket.getUnmodifiableNodes()) {
                list.add(Pair.create(node, prefix));
            }
        }

        return Collections.unmodifiableList(list);
    }

    @Override
    public Optional<Node> getNodeById(String nodeId) {
        Pair<Node,KBucket> pair = idNodePairMap.get(nodeId);
        if(pair != null) {
            return Optional.of(pair.left);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Node> getNodeByAddr(String addr) {
        Pair<Node,KBucket> pair = addrNodePairMap.get(addr);
        if(pair != null) {
            return Optional.of(pair.left);
        }
        return Optional.empty();
    }

    @Override
    public void tryInsert(Node node) {
        Lock lock = rwLock.writeLock();
        try {
            if (lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                try {
                    String nodeId = node.getId().rawString();
                    String addr = node.getAddr().toString();
                    if (!idNodePairMap.containsKey(nodeId) && !addrNodePairMap.containsKey(addr)) {
                        Optional<KBucket> optionalKBucket = insert(node);
                        if (optionalKBucket.isPresent()) {
                            //如果插入成功，则同步到map中
                            Pair<Node, KBucket> pair = Pair.create(node, optionalKBucket.get());
                            idNodePairMap.put(nodeId, pair);
                            addrNodePairMap.put(addr, pair);
                        }
                    }

                    if (!idNodePairMap.containsKey(nodeId) && addrNodePairMap.containsKey(addr)) {
                        log.info("two nodes have same addr and different nodeId");
                        Pair<Node,KBucket> conflictAddrNodePair = addrNodePairMap.get(addr);
                        removeByAddr(conflictAddrNodePair.left.getAddr().toString());
                        tryInsert(node);
                    }

                    //这种情况应该非常少。
                    if (idNodePairMap.containsKey(nodeId) && !addrNodePairMap.containsKey(addr)) {
                        log.info("two nodes have same nodeId and different addr");
                        Pair<Node,KBucket> conflictIdNodePair = idNodePairMap.get(nodeId);
                        removeByNodeId(conflictIdNodePair.left.getId().rawString());
                        tryInsert(node);
                    }
                } catch (Exception e) {
                    log.error("error", e);
                }finally {
                    lock.unlock();
                }
            }
        }catch (InterruptedException e) {
            log.info("interrupted:{}", e.getMessage());
        }
    }

    /**
     * 插入node到routing table中
     * 如果成功，则返回插入到的kBucket
     * 如果失败，则返回空
     * @param node
     * @return
     */
    private Optional<KBucket> insert(Node node) {
        Lock lock = rwLock.writeLock();
        try {

            if(lock.tryLock(1,TimeUnit.SECONDS)) {
                try {
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

                        return Optional.empty();
                    }

                    //如果i > commonPrefixLength ，则不在localId的分支上，不能分裂
                    if (i > commonPrefixLength) {
                        if (ifNotFullThenInsert(node, kBucket)) return Optional.of(kBucket);
                        kBucket.tryInsertCandi(node);
                        return Optional.empty();
                    }

                    //只有当 i <= commonPrefixLength, 那么 满了可以分裂。
                    if (ifNotFullThenInsert(node, kBucket)) return Optional.of(kBucket);

                    //满了则分裂
                    KBucket rtNodeBucket = rtNode.split();
                    //todo 更新 nodeId->(node,kBucket), addr->(node,kBucket)

                    boolean remove = prefixKBucketMap.remove(rtNodeBucket.getPrefix().rawString(), rtNodeBucket);
                    if (!remove) {
                        throw new RuntimeException("remove false");
                    }
                    RoutingTableNode[] children = rtNode.getChildren();
                    for (RoutingTableNode child : children) {
                        prefixKBucketMap.put(child.getPrefix(), child.getKBucket());
                    }

                    RoutingTableNode child = rtNode.child(nodeId.bit(i));
                    KBucket childKBucket = child.getKBucket();
                    if(ifNotFullThenInsert(node, childKBucket)) return Optional.of(childKBucket);
                    return Optional.empty();
                }finally {
                    lock.unlock();
                }

            }
        }catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 如果没有满，则插入，返回true
     * 如果满了，直接返回false
     *
     * @param node
     * @param kBucket
     * @return
     */
    private boolean ifNotFullThenInsert(Node node, KBucket kBucket) {
        return kBucket.tryInsert(node);
    }

    /**
     * 根据nodeId获取对应的bucket
     *
     * @param nodeId
     * @return
     */
    public KBucket getKBucket(BitMap nodeId) {
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
            log.info("debugInfo={}", new Gson().toJson(this.build(nodeId.toHumanString())));
        }
        return rt.getKBucket();
    }

    /**
     * 同时删除以下三个中的引用 <br/>
     *    {@link DynamicRoutingTable#root}  <br/>
     *    {@link DynamicRoutingTable#idNodePairMap} <br/>
     *    {@link DynamicRoutingTable#addrNodePairMap} <br/>
     * @param nodeId
     */
    @Override
    public void removeByNodeId(String nodeId) {
        //todo removeByNodeId
    }

    /**
     * 同时删除以下三个中的引用 <br/>
     *    {@link DynamicRoutingTable#root}  <br/>
     *    {@link DynamicRoutingTable#idNodePairMap} <br/>
     *    {@link DynamicRoutingTable#addrNodePairMap} <br/>
     * @param addr
     */
    @Override
    public void removeByAddr(String addr) {
        //todo removeByAddr
        Pair<Node, KBucket> nodePair = addrNodePairMap.get(addr);
        if (nodePair == null) return;
        Node node = nodePair.left;
        KBucket kBucket = nodePair.right;

    }

    @Override
    public int getNodeSize() {
        return addrNodePairMap.size();
    }

    public KBucket getKBucket(String id) {
        return getKBucket(BitMap.fromRawString(id));
    }

    public List<Node> getNearest(BitMap targetId) {
        return getNearest(targetId, BUCKET_SIZE);
    }

    @Override
    public List<Node> getNearest(String targetId) {
        return getNearest(BitMap.fromRawString(targetId), BUCKET_SIZE);
    }

    /**
     * 找到距离最近的8个节点
     *
     * @param targetId
     * @return
     */
    public List<Node> getNearest(BitMap targetId, Integer topK) {
        try {
            PriorityQueue<Node> maxHeap = new PriorityQueue<Node>(
                    topK,
                    (o1, o2) -> -targetId.xor(o1.getId()).compare(targetId.xor(o2.getId()), MAX_LENGTH)
            );

            for (Map.Entry<String, Pair<Node,KBucket>> entry : addrNodePairMap.entrySet()) {
                Node node = entry.getValue().left;
                if (maxHeap.size() < topK) {
                    maxHeap.add(node);
                    continue;
                }

                if (maxHeap.size() == topK) {
                    BitMap id = node.getId();
                    int cmp = id.xor(targetId).compare(Objects.requireNonNull(maxHeap.peek()).getId().xor(targetId), MAX_LENGTH);
                    if (cmp < 0) {
                        maxHeap.poll();
                        maxHeap.add(node);
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
            log.error("debugInfo=" + new Gson().toJson(this.build(targetId.toHumanString())), e);
        }
        return new ArrayList<>();
    }

    @Override
    public void logMetric() {
        try {
            RoutingTableNode rt = this.root;
            int height = 0;
            int totalNode = 0;
            Map<String, Integer> addrCountMap = Maps.newHashMap();
            Stack<RoutingTableNode> stack = new Stack<>();
            Stack<RoutingTableNode> tmp = null;
            stack.push(rt);
            while (!stack.isEmpty()) {
                height += 1;
                tmp = new Stack<>();
                while (!stack.isEmpty()) {
                    RoutingTableNode node = stack.pop();
                    KBucket kBucket = node.getKBucket();
                    if (kBucket != null) {
                        totalNode += kBucket.getSizeOfNodes();
                        //检查是否有相同address的node
                        kBucket.getNodes().forEach(n -> {
                            String addr = n.getAddr().toString();
                            addrCountMap.put(addr, 1 + addrCountMap.getOrDefault(addr, 0));
                        });
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
            log.info("cachedNodeMap.size={},cachedKBucketMap.size={}", this.addrNodePairMap.size(), this.prefixKBucketMap.size());

            int dupCount = 0;
            for (Map.Entry<String, Integer> entry : addrCountMap.entrySet()) {
                if (entry.getValue() > 1) {
                    dupCount += 1;
                }
            }
            log.info("dupCount={}", dupCount);

        } catch (Exception e) {
            log.error("error", e);
        }
    }

    private DebugInfo build(String insertNodeId) {
        List<Node> collect = addrNodePairMap.values().stream().map(pair -> pair.left).collect(Collectors.toList());
        Collection<Node> nodes = Collections.unmodifiableCollection(collect);
        List<DebugInfo.DebugNode> debugNodeList = nodes.stream()
                .map(node -> DebugInfo.DebugNode.builder()
                        .ip(node.getAddr().getHostString())
                        .port(node.getAddr().getPort())
                        .nodeId(node.getId().toHumanString())
                        .build()).collect(Collectors.toList());
        return DebugInfo.builder()
                .nodes(debugNodeList)
                .insertNodeId(insertNodeId)
                .localId(this.localId.toHumanString())
                .build();
    }
}
