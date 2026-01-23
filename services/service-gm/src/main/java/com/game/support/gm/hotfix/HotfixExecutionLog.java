package com.game.support.gm.hotfix;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 热修复脚本执行日志
 *
 * @author GameServer
 */
@Data
@Document(collection = "hotfix_execution_log")
public class HotfixExecutionLog {

    /**
     * 日志 ID
     */
    @Id
    private String logId;

    /**
     * 脚本 ID
     */
    private String scriptId;

    /**
     * 脚本名称
     */
    private String scriptName;

    /**
     * 执行者
     */
    private String executor;

    /**
     * 执行参数
     */
    private Map<String, Object> params;

    /**
     * 执行结果
     */
    private Object result;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时 (ms)
     */
    private long costMs;

    /**
     * 服务器实例 ID
     */
    private String serverInstanceId;

    /**
     * 执行时间
     */
    private LocalDateTime executeTime;
}
