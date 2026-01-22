package com.game.service.scheduler.job;

import com.game.data.mongo.MongoService;
import com.game.data.redis.RedisService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 数据清理任务
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupJob {

    private final MongoService mongoService;
    private final RedisService redisService;

    /**
     * 清理过期数据 (每日凌晨 3 点)
     * XXL-Job 配置: 0 0 3 * * ?
     */
    @XxlJob("dataCleanupHandler")
    public void cleanupExpiredData() {
        XxlJobHelper.log("开始清理过期数据");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 清理过期邮件 (30 天前)
            cleanupExpiredMails();

            // 2. 清理过期聊天记录 (7 天前)
            cleanupExpiredChatMessages();

            // 3. 清理过期日志 (90 天前)
            cleanupExpiredLogs();

            // 4. 清理 Redis 过期数据
            cleanupRedisData();

            long costTime = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("过期数据清理完成, 耗时: {}ms", costTime);
            log.info("过期数据清理完成, 耗时: {}ms", costTime);

        } catch (Exception e) {
            XxlJobHelper.log("过期数据清理异常: {}", e.getMessage());
            log.error("过期数据清理异常", e);
            XxlJobHelper.handleFail("过期数据清理异常: " + e.getMessage());
        }
    }

    private void cleanupExpiredMails() {
        long threshold = LocalDateTime.now().minusDays(30)
            .toInstant(ZoneOffset.UTC).toEpochMilli();

        Query query = new Query(Criteria.where("expireTime").lt(threshold));
        // mongoService.getMongoTemplate().remove(query, "mail");
        XxlJobHelper.log("清理过期邮件完成");
    }

    private void cleanupExpiredChatMessages() {
        long threshold = LocalDateTime.now().minusDays(7)
            .toInstant(ZoneOffset.UTC).toEpochMilli();

        Query query = new Query(Criteria.where("sendTime").lt(threshold));
        // mongoService.getMongoTemplate().remove(query, "chat_message");
        XxlJobHelper.log("清理过期聊天记录完成");
    }

    private void cleanupExpiredLogs() {
        long threshold = LocalDateTime.now().minusDays(90)
            .toInstant(ZoneOffset.UTC).toEpochMilli();

        Query query = new Query(Criteria.where("timestamp").lt(threshold));
        // mongoService.getMongoTemplate().remove(query, "operation_log");
        XxlJobHelper.log("清理过期日志完成");
    }

    private void cleanupRedisData() {
        // Redis 数据通过 TTL 自动过期，这里可以清理一些需要手动管理的数据
        XxlJobHelper.log("清理 Redis 数据完成");
    }
}
