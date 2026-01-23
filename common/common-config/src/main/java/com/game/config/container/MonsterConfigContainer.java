package com.game.config.container;

import com.game.config.config.MonsterConfig;
import com.game.core.config.game.BaseConfigContainer;
import com.game.core.config.game.ConfigContainer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 怪物配置容器
 *
 * @author GameServer
 */
@Component
@ConfigContainer(file = "monster.json", configClass = MonsterConfig.class)
public class MonsterConfigContainer extends BaseConfigContainer<MonsterConfig> {

    /**
     * 获取所有 Boss
     */
    public List<MonsterConfig> getAllBosses() {
        return getAll().stream()
                .filter(MonsterConfig::isBoss)
                .collect(Collectors.toList());
    }

    /**
     * 根据类型获取怪物
     */
    public List<MonsterConfig> getByType(int type) {
        return getAll().stream()
                .filter(m -> m.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定等级范围的怪物
     */
    public List<MonsterConfig> getByLevelRange(int minLevel, int maxLevel) {
        return getAll().stream()
                .filter(m -> m.getLevel() >= minLevel && m.getLevel() <= maxLevel)
                .collect(Collectors.toList());
    }
}
