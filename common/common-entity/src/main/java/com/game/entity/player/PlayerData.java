package com.game.entity.player;

import com.game.data.mongo.BaseDocument;
import com.game.data.mongo.index.CompoundIndex;
import com.game.data.mongo.index.MongoIndex;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家数据 MongoDB 文档
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "player")
@CompoundIndex(name = "idx_account_server", def = "{\"accountId\": 1, \"serverId\": 1}", unique = true)
@CompoundIndex(name = "idx_guild_level", def = "{\"guildId\": 1, \"level\": -1}")
public class PlayerData extends BaseDocument {

    // ==================== 基础信息 ====================

    /**
     * 角色 ID (唯一索引)
     */
    @MongoIndex(unique = true)
    private long roleId;

    /**
     * 账号 ID
     */
    @MongoIndex
    private long accountId;

    /**
     * 服务器 ID
     */
    @MongoIndex
    private int serverId;

    /**
     * 角色名 (唯一索引)
     */
    @MongoIndex(unique = true)
    private String roleName;

    /**
     * 等级
     */
    @MongoIndex(order = -1)
    private int level = 1;

    /**
     * 经验
     */
    private long exp;

    /**
     * VIP 等级
     */
    private int vipLevel;

    /**
     * VIP 经验
     */
    private long vipExp;

    /**
     * 头像 ID
     */
    private int avatarId = 1;

    /**
     * 头像框 ID
     */
    private int frameId;

    // ==================== 货币 ====================

    /**
     * 金币
     */
    private long gold;

    /**
     * 钻石
     */
    private long diamond;

    /**
     * 绑定钻石
     */
    private long bindDiamond;

    // ==================== 属性 ====================

    /**
     * 战斗力
     */
    @MongoIndex(order = -1)
    private long combatPower;

    /**
     * 体力
     */
    private int energy = 100;

    /**
     * 体力恢复时间
     */
    private long energyRecoverTime;

    /**
     * 最大体力
     */
    private int maxEnergy = 100;

    /**
     * 个性签名
     */
    private String signature;

    // ==================== 公会信息 ====================

    /**
     * 公会 ID
     */
    @MongoIndex
    private long guildId;

    /**
     * 公会名称
     */
    private String guildName;

    /**
     * 公会职位
     */
    private int guildPosition;

    // ==================== 状态 ====================

    /**
     * 封禁结束时间 (0 表示未封禁, -1 表示永久)
     */
    @MongoIndex(name = "idx_ban_time", sparse = true)
    private long banEndTime;

    /**
     * 封禁原因
     */
    private String banReason;

    /**
     * 最后登录时间
     */
    @MongoIndex
    private long lastLoginTime;

    /**
     * 最后登出时间
     */
    private long lastLogoutTime;

    /**
     * 累计在线时长 (秒)
     */
    private long totalOnlineTime;

    // ==================== 扩展数据 ====================

    /**
     * 背包数据 (物品ID -> 数量)
     */
    private Map<Integer, Long> bagItems = new HashMap<>();

    /**
     * 装备数据 (槽位 -> 装备唯一ID)
     */
    private Map<Integer, Long> equipments = new HashMap<>();

    /**
     * 已完成任务 ID 列表
     */
    private List<Integer> completedQuests = new ArrayList<>();

    /**
     * 进行中任务
     */
    private Map<Integer, QuestProgress> questProgress = new HashMap<>();

    /**
     * 解锁的功能列表
     */
    private List<Integer> unlockedFunctions = new ArrayList<>();

    /**
     * 引导进度
     */
    private Map<Integer, Integer> guideProgress = new HashMap<>();

    /**
     * 每日数据 (每日重置)
     */
    private DailyData dailyData = new DailyData();

    // ==================== 方法 ====================

    /**
     * 判断是否被封禁
     */
    public boolean isBanned() {
        if (banEndTime == 0) {
            return false;
        }
        if (banEndTime < 0) {
            return true; // 永久封禁
        }
        return System.currentTimeMillis() < banEndTime;
    }

    // ==================== 内嵌文档 ====================

    /**
     * 任务进度
     */
    @Data
    public static class QuestProgress {
        private int questId;
        private int progress;
        private long acceptTime;
    }

    /**
     * 每日数据
     */
    @Data
    public static class DailyData {
        /**
         * 数据日期
         */
        private String date;

        /**
         * 每日签到
         */
        private boolean signed;

        /**
         * 竞技场次数
         */
        private int arenaCount;

        /**
         * 副本次数
         */
        private Map<Integer, Integer> dungeonCounts = new HashMap<>();

        /**
         * 购买次数
         */
        private Map<Integer, Integer> buyCounts = new HashMap<>();
    }
}
