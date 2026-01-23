package com.game.config.config;

import com.game.core.config.game.GameConfig;
import lombok.Data;

/**
 * 全局配置 (键值对形式)
 * <p>
 * 对应配置文件: global.json
 * </p>
 *
 * @author 导表工具生成
 */
@Data
public class GlobalConfig implements GameConfig {

    /**
     * 配置键 (作为 ID)
     */
    private int id;

    /**
     * 配置键名
     */
    private String key;

    /**
     * 配置值 (字符串形式, 需要时转换)
     */
    private String value;

    /**
     * 配置描述
     */
    private String desc;

    @Override
    public int getId() {
        return id;
    }

    /**
     * 获取整数值
     */
    public int getIntValue() {
        return Integer.parseInt(value);
    }

    /**
     * 获取长整数值
     */
    public long getLongValue() {
        return Long.parseLong(value);
    }

    /**
     * 获取浮点值
     */
    public double getDoubleValue() {
        return Double.parseDouble(value);
    }

    /**
     * 获取布尔值
     */
    public boolean getBoolValue() {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
}
