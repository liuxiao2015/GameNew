package com.game.gateway.router;

import com.game.common.protocol.ProtocolConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 服务路由器
 * <p>
 * 根据协议号路由到对应的后端服务
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class ServiceRouter {

    /**
     * 服务类型枚举
     */
    public enum ServiceType {
        /**
         * 登录服务
         */
        LOGIN("service-login"),
        /**
         * 游戏服务
         */
        GAME("service-game"),
        /**
         * 公会服务
         */
        GUILD("service-guild"),
        /**
         * 聊天服务
         */
        CHAT("service-chat"),
        /**
         * 排行服务
         */
        RANK("service-rank");

        private final String serviceName;

        ServiceType(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getServiceName() {
            return serviceName;
        }
    }

    /**
     * 根据协议号路由到对应服务
     */
    public ServiceType route(int protocolId) {
        // 登录相关协议 (1000-1999)
        if (protocolId >= ProtocolConstants.LOGIN_PROTOCOL_START 
                && protocolId <= ProtocolConstants.LOGIN_PROTOCOL_END) {
            return ServiceType.LOGIN;
        }

        // 玩家相关协议 (2000-2999)
        if (protocolId >= ProtocolConstants.PLAYER_PROTOCOL_START 
                && protocolId <= ProtocolConstants.PLAYER_PROTOCOL_END) {
            return ServiceType.GAME;
        }

        // 背包协议 (3000-3999)
        if (protocolId >= ProtocolConstants.INVENTORY_PROTOCOL_START 
                && protocolId <= ProtocolConstants.INVENTORY_PROTOCOL_END) {
            return ServiceType.GAME;
        }

        // 装备协议 (4000-4999)
        if (protocolId >= ProtocolConstants.EQUIPMENT_PROTOCOL_START 
                && protocolId <= ProtocolConstants.EQUIPMENT_PROTOCOL_END) {
            return ServiceType.GAME;
        }

        // 任务协议 (5000-5999)
        if (protocolId >= ProtocolConstants.QUEST_PROTOCOL_START 
                && protocolId <= ProtocolConstants.QUEST_PROTOCOL_END) {
            return ServiceType.GAME;
        }

        // 公会协议 (6000-6999)
        if (protocolId >= ProtocolConstants.GUILD_PROTOCOL_START 
                && protocolId <= ProtocolConstants.GUILD_PROTOCOL_END) {
            return ServiceType.GUILD;
        }

        // 聊天协议 (7000-7999)
        if (protocolId >= ProtocolConstants.CHAT_PROTOCOL_START 
                && protocolId <= ProtocolConstants.CHAT_PROTOCOL_END) {
            return ServiceType.CHAT;
        }

        // 排行协议 (8000-8999)
        if (protocolId >= ProtocolConstants.RANK_PROTOCOL_START 
                && protocolId <= ProtocolConstants.RANK_PROTOCOL_END) {
            return ServiceType.RANK;
        }

        // 默认路由到游戏服务
        log.warn("未知协议号，默认路由到游戏服务: protocolId={}", protocolId);
        return ServiceType.GAME;
    }

    /**
     * 获取路由的哈希 Key
     * <p>
     * 用于一致性哈希路由，确保同一玩家的请求路由到同一服务实例
     * </p>
     */
    public String getRouteKey(ServiceType serviceType, long roleId) {
        return switch (serviceType) {
            case LOGIN -> "login:" + roleId;
            case GAME -> "game:" + roleId;
            case GUILD -> "guild:" + roleId;
            case CHAT -> "chat:" + roleId;
            case RANK -> "rank:" + roleId;
        };
    }

    /**
     * 判断协议是否需要角色 ID 进行路由
     */
    public boolean needRoleIdRoute(int protocolId) {
        // 登录协议不需要角色 ID 路由
        return !ProtocolConstants.isLoginProtocol(protocolId);
    }
}
