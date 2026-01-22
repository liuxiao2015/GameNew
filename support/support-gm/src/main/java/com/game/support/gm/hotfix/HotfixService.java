package com.game.support.gm.hotfix;

import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.data.redis.RedisService;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 热修复服务
 * <p>
 * 基于 Groovy 的轻量级热更新系统，适合小团队：
 * <ul>
 *     <li>通过 GM 后台注册脚本</li>
 *     <li>支持执行一次性修复脚本</li>
 *     <li>脚本可访问 Spring Bean</li>
 * </ul>
 * </p>
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

    /**
     * 编译后的脚本缓存
     */
    private final Map<String, Script> scriptCache = new ConcurrentHashMap<>();

    /**
     * Groovy Shell
     */
    private GroovyShell groovyShell;

    @PostConstruct
    public void init() {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setSourceEncoding("UTF-8");
        groovyShell = new GroovyShell(getClass().getClassLoader(), new Binding(), config);
        
        // 加载已有脚本
        loadScripts();
        log.info("热修复服务初始化完成");
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
            log.debug("编译热修复脚本: scriptId={}", hotfixScript.getScriptId());
        } catch (Exception e) {
            log.error("编译热修复脚本失败: scriptId={}", hotfixScript.getScriptId(), e);
        }
    }

    /**
     * 注册脚本
     */
    public Result<String> registerScript(HotfixScript script) {
        if (script == null || script.getScriptId() == null) {
            return Result.fail(ErrorCode.PARAM_ERROR, "脚本 ID 不能为空");
        }

        script.setUpdateTime(LocalDateTime.now());
        if (script.getCreateTime() == null) {
            script.setCreateTime(script.getUpdateTime());
        }

        // 保存到 MongoDB
        mongoTemplate.save(script);

        // 编译并缓存
        if (script.isEnabled()) {
            compileAndCache(script);
        } else {
            scriptCache.remove(script.getScriptId());
        }

        log.info("注册热修复脚本: scriptId={}, name={}, creator={}",
                script.getScriptId(), script.getScriptName(), script.getCreator());
        return Result.success(script.getScriptId());
    }

    /**
     * 执行脚本
     *
     * @param scriptId 脚本 ID
     * @param params   参数
     * @return 执行结果
     */
    public Result<Object> execute(String scriptId, Map<String, Object> params) {
        Script script = scriptCache.get(scriptId);
        if (script == null) {
            return Result.fail(ErrorCode.DATA_NOT_FOUND, "脚本不存在: " + scriptId);
        }

        return executeScript(script, params);
    }

    /**
     * 执行一次性脚本 (不缓存)
     *
     * @param scriptContent 脚本内容
     * @param params        参数
     * @return 执行结果
     */
    public Result<Object> executeOnce(String scriptContent, Map<String, Object> params) {
        try {
            Script script = groovyShell.parse(scriptContent);
            return executeScript(script, params);
        } catch (Exception e) {
            log.error("编译一次性脚本失败", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR, "脚本编译失败: " + e.getMessage());
        }
    }

    /**
     * 执行脚本
     */
    private Result<Object> executeScript(Script script, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();

        try {
            Binding binding = new Binding();
            
            // 注入常用服务
            binding.setVariable("ctx", applicationContext);
            binding.setVariable("redis", redisService);
            binding.setVariable("mongo", mongoTemplate);
            binding.setVariable("log", log);
            
            // 注入参数
            if (params != null) {
                params.forEach(binding::setVariable);
            }

            script.setBinding(binding);
            Object result = script.run();

            long cost = System.currentTimeMillis() - startTime;
            log.info("执行热修复脚本: cost={}ms", cost);

            return Result.success(result);

        } catch (Exception e) {
            log.error("执行热修复脚本异常", e);
            return Result.fail(ErrorCode.SYSTEM_ERROR, "脚本执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有脚本
     */
    public List<HotfixScript> getAllScripts() {
        return mongoTemplate.findAll(HotfixScript.class);
    }

    /**
     * 删除脚本
     */
    public void removeScript(String scriptId) {
        mongoTemplate.remove(
                Query.query(Criteria.where("_id").is(scriptId)),
                HotfixScript.class
        );
        scriptCache.remove(scriptId);
        log.info("删除热修复脚本: scriptId={}", scriptId);
    }

    /**
     * 刷新脚本缓存
     */
    public void refreshScripts() {
        scriptCache.clear();
        loadScripts();
    }
}
