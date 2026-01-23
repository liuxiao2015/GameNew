package com.game.service.pay.entity;

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
    
    /**
     * 商品ID
     */
    @Indexed(unique = true)
    private int productId;
    
    /**
     * 商品名称
     */
    private String name;
    
    /**
     * 商品描述
     */
    private String description;
    
    /**
     * 商品价格 (元)
     */
    private BigDecimal price;
    
    /**
     * 货币类型
     */
    private String currency;
    
    /**
     * 商品类型 (1:充值 2:礼包 3:月卡 4:首充)
     */
    private int type;
    
    /**
     * 商品分类
     */
    private String category;
    
    /**
     * 苹果商品ID
     */
    private String appleProductId;
    
    /**
     * Google商品ID
     */
    private String googleProductId;
    
    /**
     * 发放的道具列表
     */
    private List<ProductReward> rewards;
    
    /**
     * 首充额外奖励
     */
    private List<ProductReward> firstBuyRewards;
    
    /**
     * 限购次数 (0=不限)
     */
    private int buyLimit;
    
    /**
     * 每日限购次数 (0=不限)
     */
    private int dailyLimit;
    
    /**
     * VIP等级要求
     */
    private int vipRequired;
    
    /**
     * 等级要求
     */
    private int levelRequired;
    
    /**
     * 是否上架
     */
    private boolean enabled;
    
    /**
     * 上架开始时间
     */
    private Long startTime;
    
    /**
     * 下架时间
     */
    private Long endTime;
    
    /**
     * 排序权重
     */
    private int sortOrder;
    
    /**
     * 标签 (如: 热销, 推荐)
     */
    private List<String> tags;
    
    /**
     * 图标
     */
    private String icon;
    
    /**
     * 商品奖励
     */
    @Data
    public static class ProductReward {
        /**
         * 道具类型 (1:货币 2:道具 3:VIP经验)
         */
        private int type;
        
        /**
         * 道具ID
         */
        private int itemId;
        
        /**
         * 数量
         */
        private long count;
    }
}
