package com.game.entity.document;

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

    @Indexed(unique = true)
    private String orderId;

    @Indexed
    private String tradeNo;

    @Indexed
    private long roleId;

    private long accountId;
    private int serverId;
    private int productId;
    private String productName;
    private BigDecimal amount;
    private String currency;
    private PayChannel channel;
    private OrderStatus status;
    private String clientIp;
    private String deviceId;
    private String platform;
    private LocalDateTime payTime;
    private LocalDateTime deliverTime;
    private LocalDateTime completeTime;
    private LocalDateTime expireTime;
    private int deliverRetryCount;
    private String extra;
    private String callbackData;
    private String failReason;
    private String remark;
}
