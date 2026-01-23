package com.game.examples;

import com.game.actor.core.Actor;
import com.game.actor.core.ActorMessage;
import com.game.actor.core.ActorSystem;
import com.game.api.guild.GuildDTO;
import com.game.api.guild.GuildService;
import com.game.common.enums.ErrorCode;
import com.game.common.exception.BizException;
import com.game.common.result.Result;
import com.game.config.container.ItemConfigContainer;
import com.game.config.container.LevelConfigContainer;
import com.game.core.event.EventBus;
import com.game.core.event.EventListener;
import com.game.core.event.GameEvent;
import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;
import com.game.core.net.session.Session;
import com.game.core.push.DubboPushService;
import com.game.data.base.BaseRepository;
import com.game.data.redis.RedisService;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 框架使用示例
 * <p>
 * 本文件展示了框架各核心模块的使用方式
 * </p>
 *
 * @author GameServer
 */
public class FrameworkUsageExample {

    // ==================== 1. 数据层使用 ====================

    /**
     * 1.1 定义 MongoDB 实体
     */
    @Data
    @Document(collection = "player")
    public static class PlayerData {
        @Id
        private Long id;              // 角色ID
        private String name;          // 角色名
        private int level;            // 等级
        private long exp;             // 经验
        private long gold;            // 金币
        private long createTime;      // 创建时间
        private long updateTime;      // 更新时间
    }

    /**
     * 1.2 创建 Repository
     */
    @Repository
    public static class PlayerRepository extends BaseRepository<PlayerData, Long> {
        public PlayerRepository(MongoTemplate mongoTemplate) {
            super(mongoTemplate, PlayerData.class);
        }

        public List<PlayerData> findByLevel(int level) {
            return findByField("level", level);
        }
    }

    /**
     * 1.3 使用 RedisService
     */
    @Service
    @RequiredArgsConstructor
    public static class CacheExample {
        private final RedisService redisService;

        public void example() {
            // String 操作
            redisService.set("key", "value");
            redisService.set("key", "value", Duration.ofHours(1));
            String value = redisService.get("key");

            // 对象操作 (自动JSON序列化)
            redisService.setObject("player:123", new PlayerData());
            PlayerData player = redisService.getObject("player:123", PlayerData.class);

            // Hash 操作
            redisService.hSet("player:123", "name", "张三");
            String name = redisService.hGet("player:123", "name");

            // ZSet 操作 (排行榜)
            redisService.zAdd("rank:combat", "player:123", 10000.0);
            Long rank = redisService.zReverseRank("rank:combat", "player:123");

            // 分布式锁
            boolean locked = redisService.tryLock("lock:key", "requestId", Duration.ofSeconds(10));
            redisService.releaseLock("lock:key", "requestId");
        }
    }

    // ==================== 2. Actor 模型使用 ====================

    /**
     * 2.1 定义 Actor
     */
    @Slf4j
    public static class PlayerActor extends Actor<PlayerData> {
        private final PlayerRepository repository;

        public PlayerActor(long roleId, PlayerRepository repository) {
            super(roleId, "PLAYER", 1000);
            this.repository = repository;
        }

        @Override
        protected PlayerData loadData() {
            return repository.getById(getActorId());
        }

        @Override
        protected void saveData() {
            if (data != null) {
                data.setUpdateTime(System.currentTimeMillis());
                repository.save(data);
            }
        }

        @Override
        protected void handleMessage(ActorMessage message) {
            switch (message.getType()) {
                case "ADD_EXP" -> addExp((Long) message.getData());
                case "ADD_GOLD" -> addGold((Long) message.getData());
            }
        }

        private void addExp(long exp) {
            data.setExp(data.getExp() + exp);
            markDirty();  // 标记需要保存
        }

        private void addGold(long gold) {
            data.setGold(data.getGold() + gold);
            markDirty();
        }

        public int getLevel() {
            return data != null ? data.getLevel() : 0;
        }
    }

    /**
     * 2.2 使用 ActorSystem
     */
    @Service
    @RequiredArgsConstructor
    public static class ActorExample {
        private final ActorSystem<PlayerActor> actorSystem;

        public void example(long roleId) {
            // 获取 Actor (自动创建)
            PlayerActor actor = actorSystem.getActor(roleId);

            // 发送消息
            actorSystem.tell(roleId, ActorMessage.of("ADD_EXP", 100L));
            actorSystem.tell(roleId, ActorMessage.of("ADD_GOLD", 1000L));

            // 直接调用方法 (同步)
            int level = actor.getLevel();

            // 移除 Actor (下线)
            actorSystem.removeActor(roleId);
        }
    }

    // ==================== 3. Dubbo RPC 使用 ====================

    /**
     * 3.1 服务提供者
     */
    @DubboService(version = "1.0.0", group = "GAME_SERVER", timeout = 5000)
    @RequiredArgsConstructor
    public static class GuildServiceImpl implements GuildService {
        @Override
        public Result<GuildDTO> getPlayerGuild(long roleId) {
            // 业务逻辑
            return Result.success(new GuildDTO());
        }

        // 其他方法实现...
        @Override
        public Result<GuildDTO> createGuild(long roleId, String guildName, String declaration, int iconId) {
            return Result.success(new GuildDTO());
        }

        @Override
        public Result<Void> joinGuild(long roleId, long guildId) {
            return Result.success();
        }

        @Override
        public Result<Void> leaveGuild(long roleId) {
            return Result.success();
        }

        @Override
        public Result<Void> dissolveGuild(long roleId, long guildId) {
            return Result.success();
        }

        @Override
        public Result<Void> dailyReset() {
            return Result.success();
        }
    }

    /**
     * 3.2 服务消费者
     */
    @Service
    public static class RpcConsumerExample {
        @DubboReference(
                version = "1.0.0",
                group = "GAME_SERVER",
                timeout = 5000,
                loadbalance = "consistenthash",
                check = false
        )
        private GuildService guildService;

        public GuildDTO getGuild(long roleId) {
            Result<GuildDTO> result = guildService.getPlayerGuild(roleId);
            if (!result.isSuccess()) {
                throw new BizException(ErrorCode.of(result.getCode()));
            }
            return result.getData();
        }
    }

    // ==================== 4. 事件总线使用 ====================

    /**
     * 4.1 定义事件
     */
    @Getter
    public static class PlayerLevelUpEvent extends GameEvent {
        private final long roleId;
        private final int oldLevel;
        private final int newLevel;

        public PlayerLevelUpEvent(long roleId, int oldLevel, int newLevel) {
            super("PLAYER_LEVEL_UP");
            this.roleId = roleId;
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
        }
    }

    /**
     * 4.2 发布事件
     */
    @Service
    @RequiredArgsConstructor
    public static class EventPublisherExample {
        private final EventBus eventBus;

        public void levelUp(long roleId, int oldLevel, int newLevel) {
            // 同步发布
            eventBus.publish(new PlayerLevelUpEvent(roleId, oldLevel, newLevel));

            // 异步发布
            eventBus.publishAsync(new PlayerLevelUpEvent(roleId, oldLevel, newLevel));
        }
    }

    /**
     * 4.3 监听事件
     */
    @Component
    @RequiredArgsConstructor
    public static class EventListenerExample {
        private final DubboPushService pushService;

        @EventListener
        public void onLevelUp(PlayerLevelUpEvent event) {
            // 同步处理
            System.out.println("玩家升级: " + event.getRoleId());
        }

        @EventListener(async = true)
        public void onLevelUpAsync(PlayerLevelUpEvent event) {
            // 异步处理
            System.out.println("异步处理升级: " + event.getRoleId());
        }

        @EventListener(priority = 100)
        public void onLevelUpHighPriority(PlayerLevelUpEvent event) {
            // 高优先级，先执行
            System.out.println("高优先级处理: " + event.getRoleId());
        }
    }

    // ==================== 5. 协议处理器使用 ====================

    /**
     * 5.1 协议处理器
     */
    @Slf4j
    @ProtocolController(module = "player", desc = "玩家模块")
    @RequiredArgsConstructor
    public static class ProtocolHandlerExample {
        private final ItemConfigContainer itemConfig;
        private final LevelConfigContainer levelConfig;

        @Protocol(id = 0x0101)
        public void getPlayerInfo(Session session, Object request) {
            long roleId = session.getRoleId();
            // 处理请求
            // session.send(MethodId.xxx, response);
        }

        @Protocol(id = 0x0102)
        public void useItem(Session session, Object request) {
            // 检查物品
            // if (!itemConfig.exists(itemId)) {
            //     throw new BizException(ErrorCode.ITEM_NOT_EXIST);
            // }
        }
    }

    // ==================== 6. 配置加载使用 ====================

    /**
     * 6.1 使用配置容器
     */
    @Service
    @RequiredArgsConstructor
    public static class ConfigUsageExample {
        private final ItemConfigContainer itemConfig;
        private final LevelConfigContainer levelConfig;

        public void example() {
            // 获取配置
            var item = itemConfig.getItem(1001);
            var level = levelConfig.getLevel(50);

            // 获取升级经验
            long expRequired = levelConfig.getExpRequired(51);

            // 获取最大等级
            int maxLevel = levelConfig.getMaxLevel();

            // 按类型获取
            var equipments = itemConfig.getAllEquipments();
        }
    }
}
