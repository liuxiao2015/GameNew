package com.game.service.gm.controller;

import com.game.common.result.Result;
import com.game.service.gm.annotation.GmLog;
import com.game.service.gm.annotation.RequirePermission;
import com.game.support.gm.hotfix.HotfixExecutionLog;
import com.game.support.gm.hotfix.HotfixScript;
import com.game.support.gm.hotfix.HotfixService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * GM 热修复脚本控制器
 * <p>
 * 提供 Groovy 热更新脚本的管理和执行接口
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@RestController
@RequestMapping("/gm/hotfix")
@RequiredArgsConstructor
public class GmHotfixController {

    private final HotfixService hotfixService;

    /**
     * 获取所有脚本列表
     */
    @GetMapping("/list")
    @RequirePermission("hotfix:view")
    public Result<List<HotfixScript>> listScripts() {
        return Result.success(hotfixService.getAllScripts());
    }

    /**
     * 获取脚本详情
     */
    @GetMapping("/{scriptId}")
    @RequirePermission("hotfix:view")
    public Result<HotfixScript> getScript(@PathVariable String scriptId) {
        HotfixScript script = hotfixService.getScript(scriptId);
        if (script == null) {
            return Result.fail(404, "脚本不存在");
        }
        return Result.success(script);
    }

    /**
     * 注册/更新脚本
     */
    @PostMapping("/register")
    @RequirePermission("hotfix:edit")
    @GmLog(module = "热修复", operation = "注册脚本")
    public Result<String> registerScript(
            @RequestBody HotfixScript script,
            @RequestAttribute(name = "operator", required = false) String operator) {
        
        if (operator != null) {
            script.setCreator(operator);
        }
        return hotfixService.registerScript(script);
    }

    /**
     * 执行脚本
     */
    @PostMapping("/execute/{scriptId}")
    @RequirePermission("hotfix:execute")
    @GmLog(module = "热修复", operation = "执行脚本")
    public Result<Object> executeScript(
            @PathVariable String scriptId,
            @RequestBody(required = false) Map<String, Object> params,
            @RequestAttribute(name = "operator", required = false) String operator) {
        
        if (operator == null) {
            operator = "gm";
        }
        return hotfixService.execute(scriptId, params, operator);
    }

    /**
     * 执行一次性脚本
     */
    @PostMapping("/execute-once")
    @RequirePermission("hotfix:execute")
    @GmLog(module = "热修复", operation = "执行一次性脚本")
    public Result<Object> executeOnce(
            @RequestBody ExecuteOnceRequest request,
            @RequestAttribute(name = "operator", required = false) String operator) {
        
        if (operator == null) {
            operator = "gm";
        }
        return hotfixService.executeOnce(request.getScriptContent(), request.getParams(), operator);
    }

    /**
     * 删除脚本
     */
    @DeleteMapping("/{scriptId}")
    @RequirePermission("hotfix:delete")
    @GmLog(module = "热修复", operation = "删除脚本")
    public Result<Void> deleteScript(
            @PathVariable String scriptId,
            @RequestAttribute(name = "operator", required = false) String operator) {
        
        if (operator == null) {
            operator = "gm";
        }
        hotfixService.removeScript(scriptId, operator);
        return Result.success();
    }

    /**
     * 启用/禁用脚本
     */
    @PostMapping("/{scriptId}/toggle")
    @RequirePermission("hotfix:edit")
    @GmLog(module = "热修复", operation = "切换脚本状态")
    public Result<Void> toggleScript(
            @PathVariable String scriptId,
            @RequestParam boolean enabled,
            @RequestAttribute(name = "operator", required = false) String operator) {
        
        if (operator == null) {
            operator = "gm";
        }
        return hotfixService.setScriptEnabled(scriptId, enabled, operator);
    }

    /**
     * 刷新脚本缓存
     */
    @PostMapping("/refresh")
    @RequirePermission("hotfix:edit")
    @GmLog(module = "热修复", operation = "刷新脚本缓存")
    public Result<Void> refreshScripts() {
        hotfixService.refreshScripts();
        return Result.success();
    }

    /**
     * 获取脚本执行历史
     */
    @GetMapping("/history/{scriptId}")
    @RequirePermission("hotfix:view")
    public Result<List<HotfixExecutionLog>> getExecutionHistory(
            @PathVariable String scriptId,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(hotfixService.getExecutionHistory(scriptId, limit));
    }

    /**
     * 获取最近执行历史
     */
    @GetMapping("/history/recent")
    @RequirePermission("hotfix:view")
    public Result<List<HotfixExecutionLog>> getRecentHistory(
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(hotfixService.getRecentExecutionHistory(limit));
    }

    /**
     * 获取热修复统计信息
     */
    @GetMapping("/statistics")
    @RequirePermission("hotfix:view")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(hotfixService.getStatistics());
    }

    /**
     * 一次性脚本执行请求
     */
    @Data
    public static class ExecuteOnceRequest {
        private String scriptContent;
        private Map<String, Object> params;
    }
}
