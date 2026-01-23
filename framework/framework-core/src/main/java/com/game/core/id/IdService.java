package com.game.core.id;

import com.game.common.util.IdGenerator;
import com.game.data.redis.RedisService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 分布式 ID 生成服务
 * <p>
 * 支持多机多服务部署：
 * <ul>
 *     <li>通过 Redis 动态分配唯一 workerId</li>
 *     <li>定期续期，防止 workerId 被抢占</li>
 *     <li>服务重启自动重新获取</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * @Autowired
 * private IdService idService;
 *
 * // 生成通用 ID
 * long id = idService.nextId();
 *
 * // 生成指定类型的 ID
 * long playerId = idService.nextPlayerId();
 * long guildId = idService.nextGuildId();
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdService {

    private final RedisService redisService;

    @Value("${game.server.id:1}")
    private int serverId;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${game.id.worker-id-bits:10}")
    private int workerIdBits;

    /**
     * Redis Key 前缀
     */
    private static final String WORKER_ID_KEY_PREFIX = "id:worker:";

    /**
     * WorkerId 过期时间 (秒)
     */
    private static final int WORKER_ID_EXPIRE_SECONDS = 60;

    /**
     * 续期间隔 (秒)
     */
    private static final int RENEW_INTERVAL_SECONDS = 20;

    /**
     * ID 生成器
     */
    private IdGenerator idGenerator;

    /**
     * 分配的 workerId
     */
    private int workerId;

    /**
     * 实例唯一标识
     */
    private String instanceId;

    /**
     * 续期调度器
     */
    private ScheduledExecutorService renewScheduler;

    /**
     * 不同业务类型的 ID 前缀 (高8位)
     */
    public static final int TYPE_PLAYER = 0x01;
    public static final int TYPE_GUILD = 0x02;
    public static final int TYPE_MAIL = 0x03;
    public static final int TYPE_ORDER = 0x04;
    public static final int TYPE_CHAT = 0x05;
    public static final int TYPE_LOG = 0x06;

    @PostConstruct
    public void init() {
        // 生成实例唯一标识
        this.instanceId = applicationName + "-" + getHostInfo() + "-" + System.currentTimeMillis();

        // 动态获取 workerId
        this.workerId = acquireWorkerId();

        // 创建 ID 生成器
        this.idGenerator = new IdGenerator(workerId);

        // 启动续期任务
        startRenewTask();

        log.info("IdService 初始化完成: serverId={}, workerId={}, instanceId={}",
                serverId, workerId, instanceId);
    }

    @PreDestroy
    public void destroy() {
        // 停止续期
        if (renewScheduler != null) {
            renewScheduler.shutdown();
        }

        // 释放 workerId
        releaseWorkerId();

        log.info("IdService 已关闭，释放 workerId={}", workerId);
    }

    /**
     * 动态获取 workerId
     * <p>
     * 从 0 到 maxWorkerId 遍历，找到一个未被占用的 workerId
     * </p>
     */
    private int acquireWorkerId() {
        int maxWorkerId = (1 << workerIdBits) - 1;

        for (int id = 0; id <= maxWorkerId; id++) {
            String key = WORKER_ID_KEY_PREFIX + serverId + ":" + id;

            // 尝试占用
            boolean success = redisService.setIfAbsent(key, instanceId,
                    Duration.ofSeconds(WORKER_ID_EXPIRE_SECONDS));

            if (success) {
                log.info("成功获取 workerId: {}", id);
                return id;
            }
        }

        // 所有 workerId 都被占用，使用随机值 (有冲突风险，但概率低)
        int fallbackId = (int) (System.currentTimeMillis() % maxWorkerId);
        log.warn("无法获取空闲 workerId，使用回退值: {}", fallbackId);
        return fallbackId;
    }

    /**
     * 释放 workerId
     */
    private void releaseWorkerId() {
        String key = WORKER_ID_KEY_PREFIX + serverId + ":" + workerId;
        String value = redisService.get(key);

        // 只释放自己占用的
        if (instanceId.equals(value)) {
            redisService.delete(key);
            log.info("释放 workerId: {}", workerId);
        }
    }

    /**
     * 启动续期任务
     */
    private void startRenewTask() {
        renewScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "id-worker-renew");
            t.setDaemon(true);
            return t;
        });

        renewScheduler.scheduleAtFixedRate(this::renewWorkerId,
                RENEW_INTERVAL_SECONDS, RENEW_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 续期 workerId
     */
    private void renewWorkerId() {
        try {
            String key = WORKER_ID_KEY_PREFIX + serverId + ":" + workerId;
            String value = redisService.get(key);

            if (instanceId.equals(value)) {
                // 续期
                redisService.expire(key, WORKER_ID_EXPIRE_SECONDS);
                log.debug("续期 workerId: {}", workerId);
            } else {
                // 被抢占，重新获取
                log.warn("workerId 被抢占，重新获取...");
                int newWorkerId = acquireWorkerId();
                if (newWorkerId != workerId) {
                    workerId = newWorkerId;
                    idGenerator = new IdGenerator(workerId);
                    log.info("重新获取 workerId: {}", workerId);
                }
            }
        } catch (Exception e) {
            log.error("续期 workerId 异常", e);
        }
    }

    /**
     * 获取主机信息
     */
    private String getHostInfo() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-" + System.nanoTime();
        }
    }

    // ==================== ID 生成方法 ====================

    /**
     * 生成下一个 ID
     */
    public long nextId() {
        return idGenerator.nextId();
    }

    /**
     * 生成带类型前缀的 ID
     */
    public long nextId(int type) {
        long id = idGenerator.nextId();
        return (id & 0x00FFFFFFFFFFFFFFL) | ((long) type << 56);
    }

    /**
     * 生成玩家 ID
     */
    public long nextPlayerId() {
        return nextId(TYPE_PLAYER);
    }

    /**
     * 生成公会 ID
     */
    public long nextGuildId() {
        return nextId(TYPE_GUILD);
    }

    /**
     * 生成邮件 ID
     */
    public long nextMailId() {
        return nextId(TYPE_MAIL);
    }

    /**
     * 生成订单 ID
     */
    public long nextOrderId() {
        return nextId(TYPE_ORDER);
    }

    /**
     * 生成聊天消息 ID
     */
    public long nextChatId() {
        return nextId(TYPE_CHAT);
    }

    /**
     * 生成日志 ID
     */
    public long nextLogId() {
        return nextId(TYPE_LOG);
    }

    /**
     * 解析 ID 类型
     */
    public static int parseType(long id) {
        return (int) (id >>> 56);
    }

    /**
     * 解析 ID 时间戳
     */
    public static long parseTimestamp(long id) {
        return IdGenerator.parseTimestamp(id & 0x00FFFFFFFFFFFFFFL);
    }

    /**
     * 生成字符串 ID
     */
    public String nextStringId() {
        return Long.toString(nextId(), 36).toUpperCase();
    }

    /**
     * 生成带前缀的字符串 ID
     */
    public String nextStringId(String prefix) {
        return prefix + "_" + nextStringId();
    }

    /**
     * 获取当前 workerId
     */
    public int getWorkerId() {
        return workerId;
    }

    /**
     * 获取实例标识
     */
    public String getInstanceId() {
        return instanceId;
    }
}
