package com.game.config.config;

import com.game.core.config.game.GameConfig;
import lombok.Data;

/**
 * 等级配置
 * <p>
 * 对应配置文件: level.json
 * </p>
 *
 * @author 导表工具生成
 */
@Data
public class LevelConfig implements GameConfig {

    /**
     * 等级
     */
    private int id;

    /**
     * 升级所需经验
     */
    private long exp;

    /**
     * 总经验 (累计)
     */
    private long totalExp;

    /**
     * 最大体力
     */
    private int maxEnergy;

    /**
     * 属性加成 - 攻击力
     */
    private int attack;

    /**
     * 属性加成 - 防御力
     */
    private int defense;

    /**
     * 属性加成 - 生命值
     */
    private int hp;

    /**
     * 解锁的功能 (逗号分隔)
     */
    private String unlockFunctions;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String validate() {
        if (exp < 0) {
            return "经验值不能为负, level=" + id;
        }
        return null;
    }
}
