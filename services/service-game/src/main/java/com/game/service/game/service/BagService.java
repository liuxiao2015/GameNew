package com.game.service.game.service;

import com.game.api.common.ProtocolConstants;
import com.game.config.container.ItemConfigContainer;
import com.game.config.config.ItemConfig;
import com.game.core.event.EventBus;
import com.game.core.push.PushService;
import com.game.entity.player.PlayerData;
import com.game.proto.ItemInstance;
import com.game.proto.S2C_ItemChange;
import com.game.service.game.actor.PlayerActor;
import com.game.service.game.actor.PlayerActorSystem;
import com.game.service.game.event.PlayerEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 背包服务
 * <p>
 * 提供背包相关的业务逻辑，包括：
 * <ul>
 *     <li>物品添加/扣除</li>
 *     <li>物品使用</li>
 *     <li>物品出售</li>
 *     <li>背包查询</li>
 * </ul>
 * </p>
 * 
 * <pre>
 * 框架能力使用：
 * - Actor: 线程安全的玩家数据操作
 * - EventBus: 发布物品变化事件
 * - PushService: 推送物品变化给客户端
 * - ConfigContainer: 读取物品配置
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BagService {

    private final PlayerActorSystem playerActorSystem;
    private final ItemConfigContainer itemConfigContainer;
    private final EventBus eventBus;
    private final PushService pushService;

    /**
     * 推送协议号：物品变化
     */
    private static final int PUSH_ITEM_CHANGE = (ProtocolConstants.PROTOCOL_PUSH << 8) | 0x04;

    // ==================== 物品操作 ====================

    /**
     * 添加物品
     *
     * @param roleId 角色 ID
     * @param itemId 物品 ID
     * @param count  数量
     * @param reason 原因
     * @return 是否成功
     */
    public boolean addItem(long roleId, int itemId, long count, String reason) {
        if (count <= 0) {
            return false;
        }

        // 检查物品配置
        ItemConfig config = itemConfigContainer.get(itemId);
        if (config == null) {
            log.warn("物品配置不存在: itemId={}", itemId);
            return false;
        }

        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            log.warn("玩家不在线: roleId={}", roleId);
            return false;
        }

        // 通过 Actor 消息更新背包
        actor.fire("ADD_ITEM", new AddItemData(itemId, count, reason));

        // 发布物品获得事件
        eventBus.publish(new PlayerEvents.ItemObtainEvent(roleId, itemId, count, reason));

        // 推送物品变化给客户端
        pushItemChange(roleId, itemId, count, 1, reason);

        log.info("添加物品: roleId={}, itemId={}, count={}, reason={}", roleId, itemId, count, reason);
        return true;
    }

    /**
     * 批量添加物品
     */
    public boolean addItems(long roleId, Map<Integer, Long> items, String reason) {
        for (Map.Entry<Integer, Long> entry : items.entrySet()) {
            if (!addItem(roleId, entry.getKey(), entry.getValue(), reason)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 扣除物品
     */
    public boolean removeItem(long roleId, int itemId, long count, String reason) {
        if (count <= 0) {
            return false;
        }

        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return false;
        }

        PlayerData data = actor.getData();
        Long currentCount = data.getBagItems().get(itemId);
        if (currentCount == null || currentCount < count) {
            log.debug("物品数量不足: roleId={}, itemId={}, need={}, have={}", 
                    roleId, itemId, count, currentCount);
            return false;
        }

        actor.fire("REMOVE_ITEM", new RemoveItemData(itemId, count, reason));

        // 发布物品消耗事件
        eventBus.publish(new PlayerEvents.ItemConsumeEvent(roleId, itemId, count, reason));

        // 推送物品变化
        pushItemChange(roleId, itemId, -count, 2, reason);

        log.info("扣除物品: roleId={}, itemId={}, count={}, reason={}", roleId, itemId, count, reason);
        return true;
    }

    /**
     * 检查物品数量
     */
    public boolean hasEnoughItem(long roleId, int itemId, long count) {
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return false;
        }

        Long currentCount = actor.getData().getBagItems().get(itemId);
        return currentCount != null && currentCount >= count;
    }

    /**
     * 获取物品数量
     */
    public long getItemCount(long roleId, int itemId) {
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return 0;
        }

        return actor.getData().getBagItems().getOrDefault(itemId, 0L);
    }

    // ==================== 使用物品 ====================

    /**
     * 使用物品
     */
    public UseItemResult useItem(long roleId, int itemId, int count) {
        if (count <= 0) {
            return UseItemResult.failure("数量无效");
        }

        ItemConfig config = itemConfigContainer.get(itemId);
        if (config == null) {
            return UseItemResult.failure("物品不存在");
        }

        if (!config.isUsable()) {
            return UseItemResult.failure("物品不可使用");
        }

        if (!hasEnoughItem(roleId, itemId, count)) {
            return UseItemResult.failure("物品数量不足");
        }

        // 扣除物品
        if (!removeItem(roleId, itemId, count, "使用物品")) {
            return UseItemResult.failure("扣除物品失败");
        }

        // 执行物品效果 (根据物品类型处理)
        executeItemEffect(roleId, config, count);

        return UseItemResult.ok();
    }

    /**
     * 执行物品效果
     */
    private void executeItemEffect(long roleId, ItemConfig config, int count) {
        // TODO: 根据物品类型执行不同效果
        // 这里是简化示例
        log.info("执行物品效果: roleId={}, itemId={}, count={}", roleId, config.getId(), count);
    }

    // ==================== 出售物品 ====================

    /**
     * 出售物品
     */
    public SellItemResult sellItem(long roleId, int itemId, int count) {
        if (count <= 0) {
            return SellItemResult.fail("数量无效");
        }

        ItemConfig config = itemConfigContainer.get(itemId);
        if (config == null) {
            return SellItemResult.fail("物品不存在");
        }

        if (config.getSellPrice() <= 0) {
            return SellItemResult.fail("物品不可出售");
        }

        if (!hasEnoughItem(roleId, itemId, count)) {
            return SellItemResult.fail("物品数量不足");
        }

        // 扣除物品
        if (!removeItem(roleId, itemId, count, "出售物品")) {
            return SellItemResult.fail("扣除物品失败");
        }

        // 计算并添加金币
        long gold = (long) config.getSellPrice() * count;
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor != null) {
            actor.fire("ADD_GOLD", new PlayerActor.AddGoldData(gold, "出售物品"));
        }

        return SellItemResult.success(gold);
    }

    // ==================== 背包查询 ====================

    /**
     * 获取背包物品列表
     */
    public List<ItemInstance> getBagItems(long roleId) {
        PlayerActor actor = playerActorSystem.getActorIfPresent(roleId);
        if (actor == null) {
            return List.of();
        }

        List<ItemInstance> items = new ArrayList<>();
        Map<Integer, Long> bagItems = actor.getData().getBagItems();

        for (Map.Entry<Integer, Long> entry : bagItems.entrySet()) {
            items.add(ItemInstance.newBuilder()
                    .setItemId(entry.getKey())
                    .setCount(entry.getValue())
                    .build());
        }

        return items;
    }

    // ==================== 私有方法 ====================

    /**
     * 推送物品变化
     */
    private void pushItemChange(long roleId, int itemId, long count, int changeType, String reason) {
        S2C_ItemChange push = S2C_ItemChange.newBuilder()
                .setChangeType(changeType)
                .setItem(ItemInstance.newBuilder()
                        .setItemId(itemId)
                        .setCount(count)
                        .build())
                .setReason(reason)
                .build();

        pushService.push(roleId, PUSH_ITEM_CHANGE, push);
    }

    // ==================== 数据类 ====================

    public record AddItemData(int itemId, long count, String reason) {}
    public record RemoveItemData(int itemId, long count, String reason) {}

    public record UseItemResult(boolean isSuccess, String message) {
        public static UseItemResult ok() {
            return new UseItemResult(true, null);
        }
        public static UseItemResult failure(String message) {
            return new UseItemResult(false, message);
        }
    }

    public record SellItemResult(boolean success, long gold, String message) {
        public static SellItemResult success(long gold) {
            return new SellItemResult(true, gold, null);
        }
        public static SellItemResult fail(String message) {
            return new SellItemResult(false, 0, message);
        }
    }
}
