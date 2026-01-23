package com.game.service.pay.entity;

import com.game.data.mongo.BaseDocument;
import com.game.service.pay.enums.OrderStatus;
import com.game.service.pay.enums.PayChannel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "pay_order")
@CompoundIndexes({
        @CompoundIndex(name = "idx_role_status", def = "{'roleId': 1, 'status': 1}"),
        @CompoundIndex(name = "idx_channel_status", def = "{'channel': 1, 'status': 1}"),
        @CompoundIndex(name = "idx_create_time", def = "{'createTime': -1}")
})
public class PayOrder extends BaseDocument {
    
    /**
     * 订单号 (业务订单号)
     */
    @Indexed(unique = true)
    private String orderId;
    
    /**
     * 第三方订单号 (微信/支付宝等)
     */
    @Indexed
    private String tradeNo;
    
    /**
     * 角色ID
     */
    @Indexed
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
     * 商品名称
     */
    private String productName;
    
    /**
     * 订单金额 (元)
     */
    private BigDecimal amount;
    
    /**
     * 货币类型 (CNY, USD, etc.)
     */
    private String currency;
    
    /**
     * 支付渠道
     */
    private PayChannel channel;
    
    /**
     * 订单状态
     */
    private OrderStatus status;
    
    /**
     * 客户端IP
     */
    private String clientIp;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 平台 (ios, android, pc)
     */
    private String platform;
    
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    
    /**
     * 发放时间
     */
    private LocalDateTime deliverTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
    
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 发放重试次数
     */
    private int deliverRetryCount;
    
    /**
     * 扩展参数 (JSON格式)
     */
    private String extra;
    
    /**
     * 渠道回调原始数据
     */
    private String callbackData;
    
    /**
     * 失败原因
     */
    private String failReason;
    
    /**
     * 备注
     */
    private String remark;
}
