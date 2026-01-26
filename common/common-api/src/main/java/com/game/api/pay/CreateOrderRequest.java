package com.game.api.pay;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @Positive(message = "角色ID必须为正数")
    private long roleId;
    
    /**
     * 账号ID
     */
    @Positive(message = "账号ID必须为正数")
    private long accountId;
    
    /**
     * 服务器ID
     */
    @Positive(message = "服务器ID必须为正数")
    private int serverId;
    
    /**
     * 商品ID
     */
    @Positive(message = "商品ID必须为正数")
    private int productId;
    
    /**
     * 支付渠道 (wechat, alipay, apple, google)
     */
    @NotBlank(message = "支付渠道不能为空")
    private String channel;
    
    /**
     * 平台 (ios, android, pc)
     */
    @NotBlank(message = "平台不能为空")
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
