package com.game.config.container;

import com.game.config.config.GlobalConfig;
import com.game.core.config.game.BaseConfigContainer;
import com.game.core.config.game.ConfigContainer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局配置容器
 *
 * @author GameServer
 */
@Component
@ConfigContainer(file = "global.json", configClass = GlobalConfig.class)
public class GlobalConfigContainer extends BaseConfigContainer<GlobalConfig> {

    // 按 key 索引
    private final Map<String, GlobalConfig> keyIndex = new ConcurrentHashMap<>();

    @Override
    protected void afterLoad() {
        keyIndex.clear();
        for (GlobalConfig config : getAll()) {
            keyIndex.put(config.getKey(), config);
        }
    }

    /**
     * 根据 key 获取配置
     */
    public GlobalConfig getByKey(String key) {
        return keyIndex.get(key);
    }

    /**
     * 获取整数配置
     */
    public int getInt(String key, int defaultValue) {
        GlobalConfig config = getByKey(key);
        return config != null ? config.getIntValue() : defaultValue;
    }

    /**
     * 获取长整数配置
     */
    public long getLong(String key, long defaultValue) {
        GlobalConfig config = getByKey(key);
        return config != null ? config.getLongValue() : defaultValue;
    }

    /**
     * 获取字符串配置
     */
    public String getString(String key, String defaultValue) {
        GlobalConfig config = getByKey(key);
        return config != null ? config.getValue() : defaultValue;
    }

    /**
     * 获取布尔配置
     */
    public boolean getBool(String key, boolean defaultValue) {
        GlobalConfig config = getByKey(key);
        return config != null ? config.getBoolValue() : defaultValue;
    }

    // ==================== 常用配置快捷方法 ====================

    public long getInitialGold() {
        return getLong("initial_gold", 1000);
    }

    public long getInitialDiamond() {
        return getLong("initial_diamond", 100);
    }

    public int getInitialEnergy() {
        return getInt("initial_energy", 100);
    }

    public int getEnergyRecoverInterval() {
        return getInt("energy_recover_interval", 360);
    }

    public int getEnergyRecoverAmount() {
        return getInt("energy_recover_amount", 1);
    }

    public int getRoleNameMinLength() {
        return getInt("role_name_min_length", 2);
    }

    public int getRoleNameMaxLength() {
        return getInt("role_name_max_length", 12);
    }

    public int getMaxRoleCount() {
        return getInt("max_role_count", 3);
    }
}
