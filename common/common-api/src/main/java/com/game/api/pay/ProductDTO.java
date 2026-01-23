package com.game.api.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 商品ID
     */
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
     * 商品价格
     */
    private BigDecimal price;
    
    /**
     * 货币类型
     */
    private String currency;
    
    /**
     * 商品类型
     */
    private int type;
    
    /**
     * 图标
     */
    private String icon;
    
    /**
     * 标签
     */
    private List<String> tags;
    
    /**
     * 苹果商品ID
     */
    private String appleProductId;
    
    /**
     * Google商品ID
     */
    private String googleProductId;
    
    /**
     * 道具奖励
     */
    private List<RewardDTO> rewards;
    
    /**
     * 首充额外奖励
     */
    private List<RewardDTO> firstBuyRewards;
    
    /**
     * 剩余购买次数 (-1=无限)
     */
    private int remainBuyCount;
    
    /**
     * 今日剩余购买次数 (-1=无限)
     */
    private int remainDailyBuyCount;
    
    /**
     * 奖励DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /**
         * 类型 (1:货币 2:道具)
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
        
        /**
         * 道具名称
         */
        private String itemName;
        
        /**
         * 道具图标
         */
        private String itemIcon;
    }
}
