package com.game.service.pay.entity;

import com.game.data.mongo.BaseDocument;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录 (成功的订单记录，用于统计)
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "pay_record")
@CompoundIndexes({
        @CompoundIndex(name = "idx_role_product", def = "{'roleId': 1, 'productId': 1}"),
        @CompoundIndex(name = "idx_role_date", def = "{'roleId': 1, 'payDate': 1}")
})
public class PayRecord extends BaseDocument {
    
    /**
     * 订单号
     */
    @Indexed(unique = true)
    private String orderId;
    
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
     * 支付金额
     */
    private BigDecimal amount;
    
    /**
     * 货币类型
     */
    private String currency;
    
    /**
     * 支付渠道
     */
    private String channel;
    
    /**
     * 支付日期 (yyyy-MM-dd)
     */
    private String payDate;
    
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    
    /**
     * 是否首充
     */
    private boolean firstPay;
    
    /**
     * 当日第几笔
     */
    private int dailySeq;
    
    /**
     * 累计第几笔
     */
    private int totalSeq;
    
    /**
     * 发放的道具 (JSON)
     */
    private String rewardsJson;
}
