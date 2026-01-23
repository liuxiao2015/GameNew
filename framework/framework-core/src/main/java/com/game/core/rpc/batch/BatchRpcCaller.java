package com.game.core.rpc.batch;

import com.game.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 批量 RPC 调用器
 * <p>
 * 优化批量调用场景，避免循环调用导致的性能问题。
 * 支持并行调用、失败隔离、超时控制等能力。
 * </p>
 *
 * <pre>
 * 使用示例:
 * {@code
 * // 批量查询玩家信息
 * List<Long> roleIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
 * Map<Long, PlayerDTO> players = batchCaller.batchCall(
 *     roleIds,
 *     playerService::getPlayerInfo,
 *     PlayerDTO::getRoleId
 * );
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class BatchRpcCaller {

    /**
     * 并行执行器
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 默认超时时间(毫秒)
     */
    private static final long DEFAULT_TIMEOUT_MS = 5000;

    /**
     * 批量调用 - 并行执行
     *
     * @param ids          ID 列表
     * @param caller       单个调用函数
     * @param keyExtractor 从结果中提取 Key 的函数
     * @param <K>          Key 类型
     * @param <V>          Value 类型
     * @return ID -> 结果的映射
     */
    public <K, V> Map<K, V> batchCall(List<K> ids,
                                       Function<K, Result<V>> caller,
                                       Function<V, K> keyExtractor) {
        return batchCall(ids, caller, keyExtractor, DEFAULT_TIMEOUT_MS);
    }

    /**
     * 批量调用 - 并行执行（带超时）
     *
     * @param ids          ID 列表
     * @param caller       单个调用函数
     * @param keyExtractor 从结果中提取 Key 的函数
     * @param timeoutMs    超时时间(毫秒)
     * @param <K>          Key 类型
     * @param <V>          Value 类型
     * @return ID -> 结果的映射
     */
    public <K, V> Map<K, V> batchCall(List<K> ids,
                                       Function<K, Result<V>> caller,
                                       Function<V, K> keyExtractor,
                                       long timeoutMs) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        // 去重
        List<K> uniqueIds = ids.stream().distinct().toList();
        
        // 并行调用
        Map<K, CompletableFuture<V>> futures = new HashMap<>();
        for (K id : uniqueIds) {
            CompletableFuture<V> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Result<V> result = caller.apply(id);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        return result.getData();
                    }
                    return null;
                } catch (Exception e) {
                    log.warn("批量调用单个请求失败: id={}, error={}", id, e.getMessage());
                    return null;
                }
            }, executor);
            futures.put(id, future);
        }

        // 等待所有结果
        Map<K, V> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        for (Map.Entry<K, CompletableFuture<V>> entry : futures.entrySet()) {
            K id = entry.getKey();
            CompletableFuture<Void> f = entry.getValue()
                    .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .thenAccept(v -> {
                        if (v != null) {
                            K key = keyExtractor.apply(v);
                            results.put(key, v);
                        }
                    })
                    .exceptionally(e -> {
                        log.warn("批量调用超时或异常: id={}, error={}", id, e.getMessage());
                        return null;
                    });
            allFutures.add(f);
        }

        // 等待所有完成
        try {
            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                    .get(timeoutMs + 1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("批量调用整体等待异常: {}", e.getMessage());
        }

        log.debug("批量调用完成: 请求={}, 成功={}", uniqueIds.size(), results.size());
        return results;
    }

    /**
     * 批量调用 - 返回列表
     *
     * @param ids    ID 列表
     * @param caller 单个调用函数
     * @param <K>    Key 类型
     * @param <V>    Value 类型
     * @return 结果列表（过滤 null）
     */
    public <K, V> List<V> batchCallList(List<K> ids, Function<K, Result<V>> caller) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<V>> futures = ids.stream()
                .distinct()
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Result<V> result = caller.apply(id);
                        return result != null && result.isSuccess() ? result.getData() : null;
                    } catch (Exception e) {
                        log.warn("批量调用失败: id={}, error={}", id, e.getMessage());
                        return null;
                    }
                }, executor))
                .toList();

        return futures.stream()
                .map(f -> {
                    try {
                        return f.get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 分片批量调用
     * <p>
     * 当 ID 列表过大时，分片处理避免一次性创建太多并发任务。
     * </p>
     *
     * @param ids          ID 列表
     * @param batchSize    每批大小
     * @param caller       单个调用函数
     * @param keyExtractor Key 提取器
     * @param <K>          Key 类型
     * @param <V>          Value 类型
     * @return 结果映射
     */
    public <K, V> Map<K, V> batchCallPartitioned(List<K> ids, int batchSize,
                                                   Function<K, Result<V>> caller,
                                                   Function<V, K> keyExtractor) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<K, V> results = new ConcurrentHashMap<>();
        List<K> uniqueIds = ids.stream().distinct().toList();

        // 分片处理
        for (int i = 0; i < uniqueIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, uniqueIds.size());
            List<K> batch = uniqueIds.subList(i, end);
            
            Map<K, V> batchResult = batchCall(batch, caller, keyExtractor);
            results.putAll(batchResult);
        }

        return results;
    }

    /**
     * 带重试的批量调用
     *
     * @param ids          ID 列表
     * @param caller       调用函数
     * @param keyExtractor Key 提取器
     * @param maxRetries   最大重试次数
     * @param <K>          Key 类型
     * @param <V>          Value 类型
     * @return 结果映射
     */
    public <K, V> Map<K, V> batchCallWithRetry(List<K> ids,
                                                 Function<K, Result<V>> caller,
                                                 Function<V, K> keyExtractor,
                                                 int maxRetries) {
        Map<K, V> results = new ConcurrentHashMap<>();
        List<K> pending = new ArrayList<>(ids.stream().distinct().toList());

        for (int retry = 0; retry <= maxRetries && !pending.isEmpty(); retry++) {
            if (retry > 0) {
                log.debug("批量调用重试 {}/{}: 剩余 {} 个", retry, maxRetries, pending.size());
            }

            Map<K, V> batchResult = batchCall(pending, caller, keyExtractor);
            results.putAll(batchResult);

            // 移除成功的
            pending.removeAll(batchResult.keySet());
        }

        if (!pending.isEmpty()) {
            log.warn("批量调用仍有 {} 个失败", pending.size());
        }

        return results;
    }
}
