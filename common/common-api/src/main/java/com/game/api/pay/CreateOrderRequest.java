package com.game.api.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 创建订单请求
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 角色ID
     */
    private long roleId;
    
    /**
     * 账号ID
     */
    private long accountId;
    
    /**
     * 服务器ID
     */
    private int serverId;
    
    /**
     * 商品ID
     */
    private int productId;
    
    /**
     * 支付渠道 (wechat, alipay, apple, google)
     */
    private String channel;
    
    /**
     * 平台 (ios, android, pc)
     */
    private String platform;
    
    /**
     * 客户端IP
     */
    private String clientIp;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 扩展参数 (JSON)
     */
    private String extra;
}
