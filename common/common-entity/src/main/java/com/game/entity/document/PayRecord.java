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

    @Indexed(unique = true)
    private String orderId;

    @Indexed
    private long roleId;

    private long accountId;
    private int serverId;
    private int productId;
    private String productName;
    private BigDecimal amount;
    private String currency;
    private String channel;
    private String payDate;
    private LocalDateTime payTime;
    private boolean firstPay;
    private int dailySeq;
    private int totalSeq;
    private String rewardsJson;
}
