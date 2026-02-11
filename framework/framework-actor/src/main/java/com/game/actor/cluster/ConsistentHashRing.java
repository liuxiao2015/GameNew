package com.game.actor.cluster;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 一致性哈希环
 * <p>
 * 基于 TreeMap + 虚拟节点实现。用于将 Actor ID 映射到集群中的物理节点。
 * </p>
 * <p>
 * 特点:
 * <ul>
 *     <li>虚拟节点保证负载均匀分布 (默认 160 个虚拟节点/物理节点)</li>
 *     <li>节点增删时只迁移最小量的 Actor</li>
 *     <li>线程安全 (ReadWriteLock)</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public class ConsistentHashRing {

    /**
     * 哈希环: hash值 → 物理节点
     */
    private final TreeMap<Long, ClusterNode> ring = new TreeMap<>();

    /**
     * 所有物理节点
     */
    private final Map<String, ClusterNode> nodes = new LinkedHashMap<>();

    /**
     * 每个物理节点的虚拟节点数
     */
    private final int virtualNodes;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ConsistentHashRing() {
        this(160);
    }

    public ConsistentHashRing(int virtualNodes) {
        this.virtualNodes = virtualNodes;
    }

    // ==================== 节点管理 ====================

    /**
     * 添加节点到哈希环
     *
     * @param node 集群节点
     */
    public void addNode(ClusterNode node) {
        lock.writeLock().lock();
        try {
            if (nodes.containsKey(node.getNodeId())) {
                log.debug("节点已存在，跳过添加: {}", node.getNodeId());
                return;
            }
            nodes.put(node.getNodeId(), node);
            for (int i = 0; i < virtualNodes; i++) {
                long hash = hash(node.getNodeId() + "#" + i);
                ring.put(hash, node);
            }
            log.info("哈希环添加节点: nodeId={}, virtualNodes={}, ringSize={}",
                    node.getNodeId(), virtualNodes, ring.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 从哈希环移除节点
     *
     * @param nodeId 节点 ID
     */
    public void removeNode(String nodeId) {
        lock.writeLock().lock();
        try {
            ClusterNode removed = nodes.remove(nodeId);
            if (removed == null) {
                log.debug("节点不存在，跳过移除: {}", nodeId);
                return;
            }
            for (int i = 0; i < virtualNodes; i++) {
                long hash = hash(nodeId + "#" + i);
                ring.remove(hash);
            }
            log.info("哈希环移除节点: nodeId={}, ringSize={}", nodeId, ring.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 批量重建哈希环 (全量替换)
     *
     * @param newNodes 新的节点集合
     */
    public void rebuild(Collection<ClusterNode> newNodes) {
        lock.writeLock().lock();
        try {
            ring.clear();
            nodes.clear();
            for (ClusterNode node : newNodes) {
                nodes.put(node.getNodeId(), node);
                for (int i = 0; i < virtualNodes; i++) {
                    long hash = hash(node.getNodeId() + "#" + i);
                    ring.put(hash, node);
                }
            }
            log.info("哈希环重建完成: nodes={}, ringSize={}", nodes.size(), ring.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== 路由 ====================

    /**
     * 根据 Actor ID 定位物理节点
     *
     * @param actorId Actor 唯一标识
     * @return 归属的集群节点，若环为空返回 null
     */
    public ClusterNode locate(long actorId) {
        lock.readLock().lock();
        try {
            if (ring.isEmpty()) {
                return null;
            }
            long hash = hash(String.valueOf(actorId));
            // 顺时针查找第一个大于等于 hash 的节点
            Map.Entry<Long, ClusterNode> entry = ring.ceilingEntry(hash);
            if (entry == null) {
                // 绕回环首
                entry = ring.firstEntry();
            }
            return entry.getValue();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 根据字符串 key 定位物理节点
     *
     * @param key 路由 key
     * @return 归属的集群节点
     */
    public ClusterNode locate(String key) {
        lock.readLock().lock();
        try {
            if (ring.isEmpty()) {
                return null;
            }
            long hash = hash(key);
            Map.Entry<Long, ClusterNode> entry = ring.ceilingEntry(hash);
            if (entry == null) {
                entry = ring.firstEntry();
            }
            return entry.getValue();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== 查询 ====================

    /**
     * 获取所有物理节点
     */
    public Collection<ClusterNode> getAllNodes() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(nodes.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取物理节点数
     */
    public int getNodeCount() {
        lock.readLock().lock();
        try {
            return nodes.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取指定节点
     */
    public ClusterNode getNode(String nodeId) {
        lock.readLock().lock();
        try {
            return nodes.get(nodeId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 哈希环是否为空
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return ring.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== 哈希函数 ====================

    /**
     * 使用 MD5 生成 64 位哈希值
     * <p>
     * MD5 提供良好的分布均匀性，与 Dubbo ConsistentHash 使用相同的算法。
     * </p>
     */
    static long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            // 取前 8 字节组成 long
            return ((long) (digest[3] & 0xFF) << 24
                    | (long) (digest[2] & 0xFF) << 16
                    | (long) (digest[1] & 0xFF) << 8
                    | (long) (digest[0] & 0xFF))
                    & 0xFFFFFFFFL;
        } catch (NoSuchAlgorithmException e) {
            // MD5 在所有 JVM 实现中都可用
            throw new RuntimeException("MD5 算法不可用", e);
        }
    }
}
