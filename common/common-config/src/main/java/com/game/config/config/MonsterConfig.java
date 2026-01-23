package com.game.config.config;

import com.game.core.config.game.GameConfig;
import lombok.Data;

/**
 * 怪物配置
 * <p>
 * 对应配置文件: monster.json
 * </p>
 *
 * @author 导表工具生成
 */
@Data
public class MonsterConfig implements GameConfig {

    /**
     * 怪物 ID
     */
    private int id;

    /**
     * 怪物名称
     */
    private String name;

    /**
     * 怪物类型 (1:普通 2:精英 3:Boss)
     */
    private int type;

    /**
     * 怪物等级
     */
    private int level;

    /**
     * 生命值
     */
    private long hp;

    /**
     * 攻击力
     */
    private int attack;

    /**
     * 防御力
     */
    private int defense;

    /**
     * 移动速度
     */
    private int speed;

    /**
     * 攻击间隔 (毫秒)
     */
    private int attackInterval;

    /**
     * 技能 ID 列表 (逗号分隔)
     */
    private String skills;

    /**
     * 掉落配置 ID
     */
    private int dropId;

    /**
     * 击杀经验
     */
    private long killExp;

    /**
     * 怪物模型
     */
    private String model;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String validate() {
        if (hp <= 0) {
            return "怪物生命值必须大于0, id=" + id;
        }
        return null;
    }

    /**
     * 是否是 Boss
     */
    public boolean isBoss() {
        return type == 3;
    }

    /**
     * 是否是精英怪
     */
    public boolean isElite() {
        return type == 2;
    }
}
