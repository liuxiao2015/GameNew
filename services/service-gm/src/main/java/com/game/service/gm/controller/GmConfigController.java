package com.game.service.gm.controller;

import com.game.common.result.Result;
import com.game.core.config.game.ConfigLoadResult;
import com.game.core.config.game.ConfigLoader;
import com.game.core.config.game.ConfigVersion;
import com.game.service.gm.annotation.GmLog;
import com.game.service.gm.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GM 配置热更新控制器
 * <p>
 * 提供 JSON 配置表的热更新管理接口
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@RestController
@RequestMapping("/gm/config")
@RequiredArgsConstructor
public class GmConfigController {

    private final ConfigLoader configLoader;

    /**
     * 获取所有配置文件列表
     */
    @GetMapping("/list")
    @RequirePermission("config:view")
    public Result<List<String>> listConfigs() {
        return Result.success(configLoader.getAllConfigFiles());
    }

    /**
     * 获取配置状态
     */
    @GetMapping("/status/{file}")
    @RequirePermission("config:view")
    public Result<Map<String, Object>> getConfigStatus(@PathVariable String file) {
        return Result.success(configLoader.getConfigStatus(file));
    }

    /**
     * 获取所有配置状态
     */
    @GetMapping("/status/all")
    @RequirePermission("config:view")
    public Result<Map<String, Map<String, Object>>> getAllConfigStatus() {
        Map<String, Map<String, Object>> allStatus = new HashMap<>();
        for (String file : configLoader.getAllConfigFiles()) {
            allStatus.put(file, configLoader.getConfigStatus(file));
        }
        return Result.success(allStatus);
    }

    /**
     * 获取配置版本历史
     */
    @GetMapping("/history/{file}")
    @RequirePermission("config:view")
    public Result<List<ConfigVersion>> getVersionHistory(@PathVariable String file) {
        return Result.success(configLoader.getVersionHistory(file));
    }

    /**
     * 热更新单个配置
     */
    @PostMapping("/reload/{file}")
    @RequirePermission("config:reload")
    @GmLog(module = "配置管理", operation = "热更新配置")
    public Result<ConfigLoadResult> reloadConfig(
            @PathVariable String file,
            @RequestParam(defaultValue = "local") String source,
            @RequestParam(defaultValue = "false") boolean broadcast,
            @RequestAttribute(name = "operator", required = false) String operator) {
        
        if (operator == null) {
            operator = "gm";
        }

        ConfigLoadResult result;
        if ("remote".equals(source)) {
            result = configLoader.reloadFromRemote(file, operator);
        } else if (broadcast) {
            result = configLoader.reloadAndBroadcast(file, operator);
        } else {
            result = configLoader.reload(file, operator);
        }

        return result.isSuccess() ? Result.success(result) : Result.fail(500, result.getMessage());
    }

    /**
     * 热更新所有配置
     */
    @PostMapping("/reload/all")
    @RequirePermission("config:reload")
    @GmLog(module = "配置管理", operation = "热更新所有配置")
    public Result<Map<String, ConfigLoadResult>> reloadAllConfigs(
            @RequestParam(defaultValue = "false") boolean broadcast,
            @RequestAttribute(name = "operator", required = false) String operator) {
        
        if (operator == null) {
            operator = "gm";
        }

        Map<String, ConfigLoadResult> results;
        if (broadcast) {
            results = configLoader.reloadAllAndBroadcast(operator);
        } else {
            results = configLoader.reloadAll(operator);
        }

        return Result.success(results);
    }

    /**
     * 回滚配置到指定版本
     */
    @PostMapping("/rollback/{file}")
    @RequirePermission("config:rollback")
    @GmLog(module = "配置管理", operation = "回滚配置")
    public Result<ConfigLoadResult> rollbackConfig(
            @PathVariable String file,
            @RequestParam long version,
            @RequestAttribute(name = "operator", required = false) String operator) {
        
        if (operator == null) {
            operator = "gm";
        }

        ConfigLoadResult result = configLoader.rollback(file, version, operator);
        return result.isSuccess() ? Result.success(result) : Result.fail(500, result.getMessage());
    }

    /**
     * 比较两个版本的差异 (简化版)
     */
    @GetMapping("/diff/{file}")
    @RequirePermission("config:view")
    public Result<Map<String, Object>> diffVersions(
            @PathVariable String file,
            @RequestParam long version1,
            @RequestParam long version2) {
        
        List<ConfigVersion> history = configLoader.getVersionHistory(file);
        
        ConfigVersion v1 = null, v2 = null;
        for (ConfigVersion v : history) {
            if (v.getVersion() == version1) v1 = v;
            if (v.getVersion() == version2) v2 = v;
        }

        Map<String, Object> diff = new HashMap<>();
        diff.put("file", file);
        diff.put("version1", version1);
        diff.put("version2", version2);

        if (v1 != null && v2 != null) {
            diff.put("md5Changed", !v1.getMd5().equals(v2.getMd5()));
            diff.put("countDiff", v1.getConfigCount() - v2.getConfigCount());
            diff.put("v1Info", Map.of(
                    "md5", v1.getMd5(),
                    "count", v1.getConfigCount(),
                    "loadTime", v1.getLoadTime(),
                    "operator", v1.getOperator()
            ));
            diff.put("v2Info", Map.of(
                    "md5", v2.getMd5(),
                    "count", v2.getConfigCount(),
                    "loadTime", v2.getLoadTime(),
                    "operator", v2.getOperator()
            ));
        } else {
            diff.put("error", "版本不存在");
        }

        return Result.success(diff);
    }
}
