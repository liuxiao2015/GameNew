package com.game.support.gm.hotfix;

import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.core.event.DistributedEventBus;
import com.game.core.event.EventBus;
import com.game.core.event.EventListener;
import com.game.data.redis.RedisService;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 热修复服务 (增强版)
 * <p>
 * 基于 Groovy 的轻量级热更新系统，适合小团队：
 * <ul>
 *     <li>通过 GM 后台注册脚本</li>
 *     <li>支持执行一次性修复脚本</li>
 *     <li>脚本可访问 Spring Bean</li>
 *     <li>支持脚本版本管理</li>
 *     <li>支持执行历史记录</li>
 *     <li>支持集群同步</li>
 *     <li>支持执行超时保护</li>
 * </ul>
 * </p>
 *
 * <pre>
 * Groovy 脚本示例：
 * {@code
 * // 1. 获取 Spring Bean
 * def playerService = ctx.getBean("playerService")
 *
 * // 2. 访问 MongoDB
 * def player = mongo.findById(roleId, PlayerDocument.class)
 *
 * // 3. 访问 Redis
 * redis.set("key", "value")
 *
 * // 4. 返回结果
 * return [success: true, message: "修复完成"]
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotfixService {

    private final ApplicationContext applicationContext;
    private final MongoTemplate mongoTemplate;
    private final RedisService redisService;
    private final EventBus eventBus;
    private final DistributedEventBus distributedEventBus;

    /**
     * 脚本执行超时时间 (秒)
     */
    @Value("${game.hotfix.timeout-seconds:30}")
    private int timeoutSeconds;

    /**
     * 服务实例 ID
     */
    @Value("${spring.application.name:unknown}-${random.uuid:default}")
    private String serverInstanceId;

    /**
     * 编译后的脚本缓存
     */
    private final Map<String, Script> scriptCache = new ConcurrentHashMap<>();

    /**
     * 脚本版本缓存
     */
    private final Map<String, Integer> scriptVersionCache = new ConcurrentHashMap<>();

    /**
     * Groovy Shell
     */
    private GroovyShell groovyShell;

    /**
     * 脚本执行线程池
     */
    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setSourceEncoding("UTF-8");
        groovyShell = new GroovyShell(getClass().getClassLoader(), new Binding(), config);
        
        // 使用虚拟线程执行器
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        
        // 加载已有脚本
        loadScripts();
        
        log.info("热修复服务初始化完成: timeoutSeconds={}", timeoutSeconds);
    }

    /**
     * 加载所有启用的脚本
     */
    private void loadScripts() {
        try {
            List<HotfixScript> scripts = mongoTemplate.find(
                    Query.query(Criteria.where("enabled").is(true)),
                    HotfixScript.class
            );
            for (HotfixScript script : scripts) {
                compileAndCache(script);
            }
            log.info("加载热修复脚本: count={}", scriptCache.size());
        } catch (Exception e) {
            log.error("加载热修复脚本异常", e);
        }
    }

    /**
     * 编译并缓存脚本
     */
    private void compileAndCache(HotfixScript hotfixScript) {
        try {
            Script script = groovyShell.parse(hotfixScript.getScriptContent());
            scriptCache.put(hotfixScript.getScriptId(), script);
            scriptVersionCache.put(hotfixScript.getScriptId(), hotfixScript.getVersion());
            log.debug("编译热修复脚本: scriptId={}, version={}", 
                    hotfixScript.getScriptId(), hotfixScript.getVersion());
        } catch (Exception e) {
            log.error("编译热修复脚本失败: scriptId={}", hotfixScript.getScriptId(), e);
        }
    }

    /**
     * 注册脚本
     */
    public Result<String> registerScript(HotfixScript script) {
        return registerScript(script, true);
    }

    /**
     * 注册脚本
     *
     * @param script    脚本对象
     * @param broadcast 是否广播到集群
     */
    public Result<String> registerScript(HotfixScript script, boolean broadcast) {
        if (script == null || script.getScriptId() == null) {
            return Result.fail(ErrorCode.PARAM_ERROR, "脚本 ID 不能为空");
        }

        // 预编译验证
        try {
            groovyShell.parse(script.getScriptContent());
        } catch (Exception e) {
            log.error("脚本编译失败: scriptId={}", script.getScriptId(), e);
            return Result.fail(ErrorCode.PARAM_ERROR, "脚本编译失败: " + e.getMessage());
        }

        script.setUpdateTime(LocalDateTime.now());
        if (script.getCreateTime() == null) {
            script.setCreateTime(script.getUpdateTime());
        }
        
        // 版本号递增
        HotfixScript existing = mongoTemplate.findById(script.getScriptId(), HotfixScript.class);
        script.setVersion(existing != null ? existing.getVersion() + 1 : 1);

        // 保存到 MongoDB
        mongoTemplate.save(script);

        // 编译并缓存
        if (script.isEnabled()) {
            compileAndCache(script);
        } else {
            scriptCache.remove(script.getScriptId());
            scriptVersionCache.remove(script.getScriptId());
        }

        log.info("注册热修复脚本: scriptId={}, name={}, version={}, creator={}",
                script.getScriptId(), script.getScriptName(), script.getVersion(), script.getCreator());

        // 广播到集群
        if (broadcast) {
            distributedEventBus.broadcast(
                    HotfixSyncEvent.register(script.getScriptId(), script.getScriptContent(), 
                            script.isEnabled(), script.getCreator())
            );
        }

        return Result.success(script.getScriptId());
    }

    /**
     * 执行脚本
     *
     * @param scriptId 脚本 ID
     * @param params   参数
     * @param executor 执行者
     * @return 执行结果
     */
    public Result<Object> execute(String scriptId, Map<String, Object> params, String executor) {
        Script script = scriptCache.get(scriptId);
        if (script == null) {
            return Result.fail(ErrorCode.DATA_NOT_FOUND, "脚本不存在或未启用: " + scriptId);
        }

        HotfixScript hotfixScript = mongoTemplate.findById(scriptId, HotfixScript.class);
        String scriptName = hotfixScript != null ? hotfixScript.getScriptName() : scriptId;

        return executeScriptWithLog(script, scriptId, scriptName, params, executor);
    }

    /**
     * 执行一次性脚本 (不缓存)
     *
     * @param scriptContent 脚本内容
     * @param params        参数
     * @param executor      执行者
     * @return 执行结果
     */
    public Result<Object> executeOnce(String scriptContent, Map<String, Object> params, String executor) {
        try {
            Script script = groovyShell.parse(scriptContent);
            return executeScriptWithLog(script, "once-" + System.currentTimeMillis(), 
                    "一次性脚本", params, executor);
        } catch (Exception e) {
            log.error("编译一次性脚本失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR, "脚本编译失败: " + e.getMessage());
        }
    }

    /**
     * 执行脚本并记录日志
     */
    private Result<Object> executeScriptWithLog(Script script, String scriptId, String scriptName,
                                                 Map<String, Object> params, String executor) {
        long startTime = System.currentTimeMillis();
        HotfixExecutionLog logEntry = new HotfixExecutionLog();
        logEntry.setLogId(UUID.randomUUID().toString());
        logEntry.setScriptId(scriptId);
        logEntry.setScriptName(scriptName);
        logEntry.setExecutor(executor);
        logEntry.setParams(params);
        logEntry.setServerInstanceId(serverInstanceId);
        logEntry.setExecuteTime(LocalDateTime.now());

        try {
            // 带超时执行
            Future<Object> future = executorService.submit(() -> executeScriptInternal(script, params));
            Object result = future.get(timeoutSeconds, TimeUnit.SECONDS);

            long cost = System.currentTimeMillis() - startTime;
            logEntry.setSuccess(true);
            logEntry.setResult(result);
            logEntry.setCostMs(cost);

            log.info("执行热修复脚本成功: scriptId={}, executor={}, cost={}ms", scriptId, executor, cost);
            
            // 保存执行日志
            saveExecutionLog(logEntry);

            return Result.success(result);

        } catch (TimeoutException e) {
            long cost = System.currentTimeMillis() - startTime;
            logEntry.setSuccess(false);
            logEntry.setErrorMessage("执行超时 (超过 " + timeoutSeconds + " 秒)");
            logEntry.setCostMs(cost);
            
            log.error("执行热修复脚本超时: scriptId={}, timeout={}s", scriptId, timeoutSeconds);
            saveExecutionLog(logEntry);
            
            return Result.fail(ErrorCode.SYSTEM_ERROR, "脚本执行超时");

        } catch (ExecutionException e) {
            long cost = System.currentTimeMillis() - startTime;
            logEntry.setSuccess(false);
            logEntry.setErrorMessage(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            logEntry.setCostMs(cost);
            
            log.error("执行热修复脚本异常: scriptId={}", scriptId, e.getCause());
            saveExecutionLog(logEntry);
            
            return Result.fail(ErrorCode.SYSTEM_ERROR, "脚本执行失败: " + logEntry.getErrorMessage());

        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            logEntry.setSuccess(false);
            logEntry.setErrorMessage(e.getMessage());
            logEntry.setCostMs(cost);
            
            log.error("执行热修复脚本异常: scriptId={}", scriptId, e);
            saveExecutionLog(logEntry);
            
            return Result.fail(ErrorCode.SYSTEM_ERROR, "脚本执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行脚本内部逻辑
     */
    private Object executeScriptInternal(Script script, Map<String, Object> params) {
        Binding binding = new Binding();
        
        // 注入常用服务
        binding.setVariable("ctx", applicationContext);
        binding.setVariable("redis", redisService);
        binding.setVariable("mongo", mongoTemplate);
        binding.setVariable("log", log);
        binding.setVariable("eventBus", eventBus);
        
        // 注入参数
        if (params != null) {
            params.forEach(binding::setVariable);
        }

        script.setBinding(binding);
        return script.run();
    }

    /**
     * 保存执行日志
     */
    private void saveExecutionLog(HotfixExecutionLog logEntry) {
        try {
            mongoTemplate.save(logEntry);
        } catch (Exception e) {
            log.error("保存热修复执行日志失败", e);
        }
    }

    /**
     * 获取执行历史
     */
    public List<HotfixExecutionLog> getExecutionHistory(String scriptId, int limit) {
        Query query = Query.query(Criteria.where("scriptId").is(scriptId))
                .limit(limit)
                .with(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "executeTime"));
        return mongoTemplate.find(query, HotfixExecutionLog.class);
    }

    /**
     * 获取最近执行历史
     */
    public List<HotfixExecutionLog> getRecentExecutionHistory(int limit) {
        Query query = new Query()
                .limit(limit)
                .with(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "executeTime"));
        return mongoTemplate.find(query, HotfixExecutionLog.class);
    }

    /**
     * 获取所有脚本
     */
    public List<HotfixScript> getAllScripts() {
        return mongoTemplate.findAll(HotfixScript.class);
    }

    /**
     * 获取脚本详情
     */
    public HotfixScript getScript(String scriptId) {
        return mongoTemplate.findById(scriptId, HotfixScript.class);
    }

    /**
     * 删除脚本
     */
    public void removeScript(String scriptId, String operator) {
        removeScript(scriptId, operator, true);
    }

    /**
     * 删除脚本
     */
    public void removeScript(String scriptId, String operator, boolean broadcast) {
        mongoTemplate.remove(
                Query.query(Criteria.where("_id").is(scriptId)),
                HotfixScript.class
        );
        scriptCache.remove(scriptId);
        scriptVersionCache.remove(scriptId);
        
        log.info("删除热修复脚本: scriptId={}, operator={}", scriptId, operator);

        // 广播到集群
        if (broadcast) {
            distributedEventBus.broadcast(HotfixSyncEvent.delete(scriptId, operator));
        }
    }

    /**
     * 刷新脚本缓存
     */
    public void refreshScripts() {
        refreshScripts(true);
    }

    /**
     * 刷新脚本缓存
     */
    public void refreshScripts(boolean broadcast) {
        scriptCache.clear();
        scriptVersionCache.clear();
        loadScripts();
        
        log.info("刷新热修复脚本缓存完成");

        if (broadcast) {
            distributedEventBus.broadcast(HotfixSyncEvent.refresh("system"));
        }
    }

    /**
     * 启用/禁用脚本
     */
    public Result<Void> setScriptEnabled(String scriptId, boolean enabled, String operator) {
        HotfixScript script = mongoTemplate.findById(scriptId, HotfixScript.class);
        if (script == null) {
            return Result.fail(ErrorCode.DATA_NOT_FOUND, "脚本不存在");
        }

        script.setEnabled(enabled);
        script.setUpdateTime(LocalDateTime.now());
        mongoTemplate.save(script);

        if (enabled) {
            compileAndCache(script);
        } else {
            scriptCache.remove(scriptId);
            scriptVersionCache.remove(scriptId);
        }

        log.info("设置脚本状态: scriptId={}, enabled={}, operator={}", scriptId, enabled, operator);

        // 广播到集群
        distributedEventBus.broadcast(
                HotfixSyncEvent.register(scriptId, script.getScriptContent(), enabled, operator)
        );

        return Result.success();
    }

    /**
     * 监听脚本同步事件
     */
    @EventListener(desc = "热修复脚本同步事件监听")
    public void onHotfixSync(HotfixSyncEvent event) {
        log.info("收到脚本同步事件: type={}, scriptId={}, operator={}",
                event.getOperationType(), event.getScriptId(), event.getOperator());

        switch (event.getOperationType()) {
            case REGISTER -> {
                if (event.isEnabled()) {
                    try {
                        Script script = groovyShell.parse(event.getScriptContent());
                        scriptCache.put(event.getScriptId(), script);
                        log.debug("同步注册脚本: scriptId={}", event.getScriptId());
                    } catch (Exception e) {
                        log.error("同步注册脚本编译失败: scriptId={}", event.getScriptId(), e);
                    }
                } else {
                    scriptCache.remove(event.getScriptId());
                    log.debug("同步禁用脚本: scriptId={}", event.getScriptId());
                }
            }
            case DELETE -> {
                scriptCache.remove(event.getScriptId());
                scriptVersionCache.remove(event.getScriptId());
                log.debug("同步删除脚本: scriptId={}", event.getScriptId());
            }
            case REFRESH -> {
                refreshScripts(false);
            }
        }
    }

    /**
     * 获取脚本统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedScriptCount", scriptCache.size());
        stats.put("totalScriptCount", mongoTemplate.count(new Query(), HotfixScript.class));
        stats.put("enabledScriptCount", mongoTemplate.count(
                Query.query(Criteria.where("enabled").is(true)), HotfixScript.class));
        stats.put("totalExecutionCount", mongoTemplate.count(new Query(), HotfixExecutionLog.class));
        stats.put("successExecutionCount", mongoTemplate.count(
                Query.query(Criteria.where("success").is(true)), HotfixExecutionLog.class));
        return stats;
    }
}
