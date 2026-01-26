package com.game.entity.document;

import com.game.data.mongo.BaseDocument;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品配置
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "pay_product")
public class Product extends BaseDocument {

    @Indexed(unique = true)
    private int productId;

    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private int type;
    private String category;
    private String appleProductId;
    private String googleProductId;
    private List<ProductReward> rewards;
    private List<ProductReward> firstBuyRewards;
    private int buyLimit;
    private int dailyLimit;
    private int vipRequired;
    private int levelRequired;
    private boolean enabled;
    private Long startTime;
    private Long endTime;
    private int sortOrder;
    private List<String> tags;
    private String icon;

    @Data
    public static class ProductReward {
        private int type;
        private int itemId;
        private long count;
    }
}
