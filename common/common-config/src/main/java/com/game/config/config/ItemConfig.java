package com.game.config.config;

import com.game.core.config.game.GameConfig;
import lombok.Data;

/**
 * 物品配置
 * <p>
 * 对应配置文件: item.json
 * </p>
 *
 * @author 导表工具生成
 */
@Data
public class ItemConfig implements GameConfig {

    /**
     * 物品 ID
     */
    private int id;

    /**
     * 物品名称
     */
    private String name;

    /**
     * 物品类型 (1:消耗品 2:装备 3:材料 4:宝石 5:礼包)
     */
    private int type;

    /**
     * 物品品质 (1:白 2:绿 3:蓝 4:紫 5:橙 6:红)
     */
    private int quality;

    /**
     * 堆叠上限
     */
    private int stackLimit;

    /**
     * 出售价格 (金币)
     */
    private int sellPrice;

    /**
     * 物品图标
     */
    private String icon;

    /**
     * 物品描述
     */
    private String desc;

    /**
     * 使用等级限制
     */
    private int levelLimit;

    /**
     * 使用效果参数 (JSON 格式, 根据类型解析)
     */
    private String effectParams;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void afterLoad() {
        if (stackLimit <= 0) {
            stackLimit = 1;
        }
        if (quality <= 0) {
            quality = 1;
        }
    }

    @Override
    public String validate() {
        if (name == null || name.isEmpty()) {
            return "物品名称不能为空, id=" + id;
        }
        if (type <= 0 || type > 5) {
            return "物品类型无效, id=" + id + ", type=" + type;
        }
        return null;
    }

    /**
     * 是否可堆叠
     */
    public boolean isStackable() {
        return stackLimit > 1;
    }

    /**
     * 是否是装备
     */
    public boolean isEquipment() {
        return type == 2;
    }

    /**
     * 是否是消耗品
     */
    public boolean isConsumable() {
        return type == 1;
    }

    /**
     * 是否可使用
     */
    public boolean isUsable() {
        // 消耗品和礼包可使用
        return type == 1 || type == 5;
    }
}
