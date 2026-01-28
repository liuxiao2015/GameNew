package com.game.mq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息队列 Exchange 定义
 * <p>
 * 定义系统中使用的所有 Exchange 及其类型
 * </p>
 *
 * @author GameServer
 */
@Getter
@RequiredArgsConstructor
public enum MqExchange {

    // ==================== 事件相关 ====================
    
    /**
     * 全局事件广播 (fanout)
     * 替代 Redis Pub/Sub 的全局广播
     */
    EVENT_BROADCAST("event.broadcast", ExchangeType.FANOUT, "全局事件广播"),
    
    /**
     * 服务间定向事件 (topic)
     * routing key 格式: service.{serviceName}.{eventType}
     */
    EVENT_TOPIC("event.topic", ExchangeType.TOPIC, "服务间定向事件"),

    // ==================== 聊天相关 ====================
    
    /**
     * 世界聊天广播 (fanout)
     */
    CHAT_WORLD("chat.world", ExchangeType.FANOUT, "世界聊天广播"),
    
    /**
     * 公会/私聊定向 (direct)
     * routing key 格式: guild.{guildId} 或 private.{roleId}
     */
    CHAT_DIRECT("chat.direct", ExchangeType.DIRECT, "公会/私聊"),

    // ==================== 战斗相关 ====================
    
    /**
     * 战斗事件分发 (direct)
     * routing key 格式: battle.{battleId}
     */
    BATTLE_DIRECT("battle.direct", ExchangeType.DIRECT, "战斗事件分发"),
    
    /**
     * 战斗结算广播 (topic)
     * routing key 格式: battle.result.{battleType}
     */
    BATTLE_RESULT("battle.result", ExchangeType.TOPIC, "战斗结算");

    /**
     * Exchange 名称
     */
    private final String name;
    
    /**
     * Exchange 类型
     */
    private final ExchangeType type;
    
    /**
     * 描述
     */
    private final String description;

    /**
     * Exchange 类型枚举
     */
    public enum ExchangeType {
        /**
         * 广播模式，消息发送到所有绑定的队列
         */
        FANOUT,
        
        /**
         * 直连模式，根据 routing key 精确匹配
         */
        DIRECT,
        
        /**
         * 主题模式，支持 routing key 通配符匹配
         */
        TOPIC,
        
        /**
         * 头部模式，根据消息头匹配
         */
        HEADERS
    }
}
