package com.game.config.container;

import com.game.config.config.ItemConfig;
import com.game.core.config.game.BaseConfigContainer;
import com.game.core.config.game.ConfigContainer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 物品配置容器
 *
 * @author GameServer
 */
@Component
@ConfigContainer(file = "item.json", configClass = ItemConfig.class)
public class ItemConfigContainer extends BaseConfigContainer<ItemConfig> {

    /**
     * 根据类型获取物品列表
     */
    public List<ItemConfig> getByType(int type) {
        return getAll().stream()
                .filter(item -> item.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * 根据品质获取物品列表
     */
    public List<ItemConfig> getByQuality(int quality) {
        return getAll().stream()
                .filter(item -> item.getQuality() == quality)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有装备配置
     */
    public List<ItemConfig> getAllEquipments() {
        return getByType(2);
    }

    /**
     * 获取所有消耗品配置
     */
    public List<ItemConfig> getAllConsumables() {
        return getByType(1);
    }
}
