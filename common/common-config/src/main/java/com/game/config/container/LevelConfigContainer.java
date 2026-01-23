package com.game.config.container;

import com.game.config.config.LevelConfig;
import com.game.core.config.game.BaseConfigContainer;
import com.game.core.config.game.ConfigContainer;
import org.springframework.stereotype.Component;

/**
 * 等级配置容器
 *
 * @author GameServer
 */
@Component
@ConfigContainer(file = "level.json", configClass = LevelConfig.class)
public class LevelConfigContainer extends BaseConfigContainer<LevelConfig> {

    /**
     * 获取最大等级
     */
    public int getMaxLevel() {
        return getAll().stream()
                .mapToInt(LevelConfig::getId)
                .max()
                .orElse(1);
    }

    /**
     * 获取升级所需经验
     */
    public long getExpRequired(int level) {
        LevelConfig config = get(level);
        return config != null ? config.getExp() : 0;
    }

    /**
     * 根据当前经验计算等级
     */
    public int calculateLevel(long totalExp) {
        int level = 1;
        for (LevelConfig config : getAll()) {
            if (totalExp >= config.getTotalExp()) {
                level = config.getId();
            } else {
                break;
            }
        }
        return level;
    }

    /**
     * 获取当前等级的最大体力
     */
    public int getMaxEnergy(int level) {
        LevelConfig config = get(level);
        return config != null ? config.getMaxEnergy() : 100;
    }
}
