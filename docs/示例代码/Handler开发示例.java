package com.game.examples;

import com.game.api.common.MethodId;
import com.game.common.enums.ErrorCode;
import com.game.common.exception.GameException;
import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;
import com.game.core.net.session.Session;
import com.game.core.push.DubboPushService;
import com.game.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 协议处理器开发示例
 * <p>
 * 展示如何开发协议处理器（Handler）
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@ProtocolController(module = "example", desc = "示例模块")
@RequiredArgsConstructor
public class HandlerExample {

    private final DubboPushService pushService;
    private final PlayerService playerService;
    private final BagService bagService;

    // ==================== 基础协议处理 ====================

    /**
     * 获取玩家信息
     * <p>
     * 请求：C2S_GetPlayerInfo
     * 响应：S2C_PlayerInfo
     * </p>
     */
    @Protocol(id = MethodId.PLAYER_GET_INFO)
    public void getPlayerInfo(Session session, C2S_GetPlayerInfo request) {
        long roleId = session.getRoleId();
        
        // 1. 获取玩家数据
        PlayerData player = playerService.getPlayer(roleId);
        if (player == null) {
            throw new GameException(ErrorCode.PLAYER_NOT_FOUND);
        }
        
        // 2. 构建响应
        S2C_PlayerInfo response = S2C_PlayerInfo.newBuilder()
                .setRoleId(player.getRoleId())
                .setName(player.getName())
                .setLevel(player.getLevel())
                .setExp(player.getExp())
                .setGold(player.getGold())
                .setDiamond(player.getDiamond())
                .setVipLevel(player.getVipLevel())
                .setCombatPower(player.getCombatPower())
                .build();
        
        // 3. 发送响应
        session.send(MethodId.PLAYER_GET_INFO, response);
    }

    // ==================== 带参数校验 ====================

    /**
     * 使用物品
     */
    @Protocol(id = MethodId.PLAYER_USE_ITEM)
    public void useItem(Session session, C2S_UseItem request) {
        long roleId = session.getRoleId();
        long itemUid = request.getItemUid();
        int count = request.getCount();
        
        // 1. 参数校验
        if (itemUid <= 0) {
            throw new GameException(ErrorCode.PARAM_ERROR, "物品ID无效");
        }
        if (count <= 0 || count > 99) {
            throw new GameException(ErrorCode.PARAM_ERROR, "使用数量无效");
        }
        
        // 2. 业务处理
        UseItemResult result = bagService.useItem(roleId, itemUid, count);
        
        // 3. 返回结果
        S2C_UseItemResult response = S2C_UseItemResult.newBuilder()
                .setSuccess(result.isSuccess())
                .setItemUid(itemUid)
                .setRemainCount(result.getRemainCount())
                .build();
        
        session.send(MethodId.PLAYER_USE_ITEM, response);
        
        // 4. 推送背包变化（如果有）
        if (result.isSuccess()) {
            pushBagChange(roleId, result);
        }
    }

    // ==================== 异常处理示例 ====================

    /**
     * 购买物品
     */
    @Protocol(id = MethodId.SHOP_BUY_ITEM)
    public void buyItem(Session session, C2S_BuyItem request) {
        long roleId = session.getRoleId();
        
        try {
            // 1. 检查商品是否存在
            ShopItem shopItem = shopService.getShopItem(request.getShopItemId());
            if (shopItem == null) {
                throw new GameException(ErrorCode.SHOP_ITEM_NOT_EXIST);
            }
            
            // 2. 检查购买次数
            if (shopService.getBuyCount(roleId, request.getShopItemId()) >= shopItem.getMaxBuyCount()) {
                throw new GameException(ErrorCode.SHOP_BUY_LIMIT);
            }
            
            // 3. 检查货币是否足够
            if (!playerService.hasEnoughGold(roleId, shopItem.getPrice())) {
                throw new GameException(ErrorCode.GOLD_NOT_ENOUGH);
            }
            
            // 4. 扣除货币
            playerService.deductGold(roleId, shopItem.getPrice(), "shop_buy");
            
            // 5. 发放物品
            bagService.addItem(roleId, shopItem.getItemId(), shopItem.getItemCount(), "shop_buy");
            
            // 6. 返回成功
            session.send(MethodId.SHOP_BUY_ITEM, S2C_BuyItemResult.newBuilder()
                    .setSuccess(true)
                    .setItemId(shopItem.getItemId())
                    .setCount(shopItem.getItemCount())
                    .build());
            
        } catch (GameException e) {
            // 业务异常直接抛出，框架会统一处理
            throw e;
        } catch (Exception e) {
            // 未知异常记录日志
            log.error("购买物品异常: roleId={}, shopItemId={}", roleId, request.getShopItemId(), e);
            throw new GameException(ErrorCode.SYSTEM_ERROR);
        }
    }

    // ==================== 无返回值协议 ====================

    /**
     * 客户端心跳
     */
    @Protocol(id = MethodId.HEARTBEAT)
    public void heartbeat(Session session, C2S_Heartbeat request) {
        // 更新活跃时间（框架自动处理）
        // 可以在这里做一些额外的处理，如检测作弊
        
        // 返回心跳响应
        session.send(MethodId.HEARTBEAT, S2C_Heartbeat.newBuilder()
                .setServerTime(System.currentTimeMillis())
                .build());
    }

    // ==================== 推送消息 ====================

    /**
     * 推送背包变化
     */
    private void pushBagChange(long roleId, UseItemResult result) {
        S2C_BagUpdate update = S2C_BagUpdate.newBuilder()
                .addItems(BagItemInfo.newBuilder()
                        .setItemUid(result.getItemUid())
                        .setCount(result.getRemainCount())
                        .build())
                .build();
        
        pushService.pushToPlayer(roleId, MethodId.PUSH_BAG_UPDATE, update);
    }

    /**
     * 全服广播
     */
    private void broadcastAnnouncement(String content) {
        S2C_Announcement announcement = S2C_Announcement.newBuilder()
                .setContent(content)
                .setType(1)
                .build();
        
        pushService.broadcast(MethodId.PUSH_ANNOUNCEMENT, announcement);
    }

    // ==================== 内部类（模拟依赖） ====================

    interface PlayerService {
        PlayerData getPlayer(long roleId);
        boolean hasEnoughGold(long roleId, int amount);
        void deductGold(long roleId, int amount, String reason);
    }

    interface BagService {
        UseItemResult useItem(long roleId, long itemUid, int count);
        void addItem(long roleId, int itemId, int count, String source);
    }

    interface ShopService {
        ShopItem getShopItem(int shopItemId);
        int getBuyCount(long roleId, int shopItemId);
    }

    static class PlayerData {
        long roleId;
        String name;
        int level;
        long exp;
        long gold;
        long diamond;
        int vipLevel;
        long combatPower;

        // getters...
        public long getRoleId() { return roleId; }
        public String getName() { return name; }
        public int getLevel() { return level; }
        public long getExp() { return exp; }
        public long getGold() { return gold; }
        public long getDiamond() { return diamond; }
        public int getVipLevel() { return vipLevel; }
        public long getCombatPower() { return combatPower; }
    }

    static class UseItemResult {
        boolean success;
        long itemUid;
        int remainCount;

        public boolean isSuccess() { return success; }
        public long getItemUid() { return itemUid; }
        public int getRemainCount() { return remainCount; }
    }

    static class ShopItem {
        int itemId;
        int itemCount;
        int price;
        int maxBuyCount;

        public int getItemId() { return itemId; }
        public int getItemCount() { return itemCount; }
        public int getPrice() { return price; }
        public int getMaxBuyCount() { return maxBuyCount; }
    }

    // 模拟依赖注入
    private final ShopService shopService = null;
}
