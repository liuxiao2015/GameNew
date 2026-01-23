package com.game.core.sync;

import com.game.core.push.PushService;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 数据同步服务
 * <p>
 * 提供数据变更自动同步给客户端的能力：
 * <ul>
 *     <li>收集数据变更</li>
 *     <li>批量同步 (减少网络请求)</li>
 *     <li>支持全量/增量同步</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 1. 记录数据变更
 * dataSyncService.markChanged(roleId, SyncData.update("player", "gold", newGold));
 * dataSyncService.markChanged(roleId, SyncData.update("player", "level", newLevel));
 *
 * // 2. 在请求结束时自动推送 (可在拦截器中调用)
 * dataSyncService.flush(roleId);
 *
 * // 或者手动立即推送
 * dataSyncService.syncNow(roleId, SyncData.full("bag", bagData));
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSyncService {

    private final PushService pushService;

    /**
     * 待同步数据 (roleId -> List<SyncData>)
     */
    private final Map<Long, List<SyncData>> pendingSyncData = new ConcurrentHashMap<>();

    /**
     * 同步数据转换器 (module -> 转换函数)
     */
    private final Map<String, Function<List<SyncData>, Message>> syncConverters = new ConcurrentHashMap<>();

    /**
     * 同步协议号映射 (module -> protocolId)
     */
    private final Map<String, Integer> syncProtocols = new ConcurrentHashMap<>();

    /**
     * 注册同步模块
     *
     * @param module     模块名
     * @param protocolId 同步协议号
     * @param converter  数据转换器
     */
    public void registerModule(String module, int protocolId, Function<List<SyncData>, Message> converter) {
        syncProtocols.put(module, protocolId);
        syncConverters.put(module, converter);
        log.info("注册数据同步模块: module={}, protocolId=0x{}", module, Integer.toHexString(protocolId));
    }

    /**
     * 标记数据变更
     */
    public void markChanged(long roleId, SyncData syncData) {
        if (roleId <= 0 || syncData == null) {
            return;
        }

        pendingSyncData.computeIfAbsent(roleId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(syncData);
    }

    /**
     * 标记多个数据变更
     */
    public void markChanged(long roleId, List<SyncData> syncDataList) {
        if (roleId <= 0 || syncDataList == null || syncDataList.isEmpty()) {
            return;
        }

        pendingSyncData.computeIfAbsent(roleId, k -> Collections.synchronizedList(new ArrayList<>()))
                .addAll(syncDataList);
    }

    /**
     * 标记字段变更 (便捷方法)
     */
    public void markFieldChanged(long roleId, String module, String field, Object value) {
        markChanged(roleId, SyncData.update(module, field, value));
    }

    /**
     * 立即同步单个数据
     */
    public void syncNow(long roleId, SyncData syncData) {
        if (roleId <= 0 || syncData == null) {
            return;
        }

        String module = syncData.getModule();
        Integer protocolId = syncProtocols.get(module);
        Function<List<SyncData>, Message> converter = syncConverters.get(module);

        if (protocolId == null || converter == null) {
            log.warn("未注册的同步模块: module={}", module);
            return;
        }

        Message message = converter.apply(Collections.singletonList(syncData));
        if (message != null) {
            pushService.push(roleId, protocolId, message);
        }
    }

    /**
     * 刷新并推送所有待同步数据
     */
    public void flush(long roleId) {
        List<SyncData> dataList = pendingSyncData.remove(roleId);
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        // 按模块分组
        Map<String, List<SyncData>> byModule = new HashMap<>();
        for (SyncData data : dataList) {
            byModule.computeIfAbsent(data.getModule(), k -> new ArrayList<>()).add(data);
        }

        // 按模块推送
        for (Map.Entry<String, List<SyncData>> entry : byModule.entrySet()) {
            String module = entry.getKey();
            List<SyncData> moduleData = entry.getValue();

            Integer protocolId = syncProtocols.get(module);
            Function<List<SyncData>, Message> converter = syncConverters.get(module);

            if (protocolId == null || converter == null) {
                log.warn("未注册的同步模块: module={}", module);
                continue;
            }

            try {
                Message message = converter.apply(moduleData);
                if (message != null) {
                    pushService.push(roleId, protocolId, message);
                    log.debug("同步数据: roleId={}, module={}, count={}",
                            roleId, module, moduleData.size());
                }
            } catch (Exception e) {
                log.error("数据同步异常: roleId={}, module={}", roleId, module, e);
            }
        }
    }

    /**
     * 清除待同步数据 (玩家下线时)
     */
    public void clear(long roleId) {
        pendingSyncData.remove(roleId);
    }

    /**
     * 获取待同步数据数量
     */
    public int getPendingCount(long roleId) {
        List<SyncData> list = pendingSyncData.get(roleId);
        return list != null ? list.size() : 0;
    }
}
