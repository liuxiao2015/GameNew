package com.game.service.gm.controller;

import com.game.common.result.Result;
import com.game.core.config.game.ConfigLoader;
import com.game.data.redis.RedisService;
import com.game.service.gm.annotation.GmLog;
import com.game.service.gm.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * GM 服务器管理控制器
 *
 * @author GameServer
 */
@RestController
@RequestMapping("/gm/server")
@RequiredArgsConstructor
public class GmServerController {

    private final RedisService redisService;
    private final ConfigLoader configLoader;

    /**
     * 获取服务器状态
     */
    @GetMapping("/status")
    @RequirePermission("server:view")
    public Result<Map<String, Object>> getServerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 获取在线人数
        Long onlineCount = redisService.sCard("online:players");
        status.put("onlineCount", onlineCount != null ? onlineCount : 0);
        
        // 服务器状态
        status.put("serverStatus", "running");
        
        // 当前时间
        status.put("serverTime", System.currentTimeMillis());
        
        return Result.success(status);
    }

    /**
     * 设置服务器开关状态
     */
    @PostMapping("/switch")
    @RequirePermission("server:control")
    @GmLog(module = "服务器管理", operation = "设置服务器开关")
    public Result<Void> setServerSwitch(
            @RequestParam String key,
            @RequestParam boolean enabled) {
        // 设置各种功能开关
        redisService.set("server:switch:" + key, enabled ? "1" : "0");
        return Result.success();
    }

    /**
     * 获取所有功能开关
     */
    @GetMapping("/switches")
    @RequirePermission("server:view")
    public Result<Map<String, Boolean>> getServerSwitches() {
        Map<String, Boolean> switches = new HashMap<>();
        
        // 常用开关列表
        String[] keys = {"login", "register", "pay", "chat", "guild", "arena", "trade"};
        for (String key : keys) {
            String value = redisService.get("server:switch:" + key);
            switches.put(key, !"0".equals(value));
        }
        
        return Result.success(switches);
    }

    /**
     * 维护模式
     */
    @PostMapping("/maintenance")
    @RequirePermission("server:control")
    @GmLog(module = "服务器管理", operation = "维护模式")
    public Result<Void> setMaintenance(
            @RequestParam boolean enabled,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) Long endTime) {
        if (enabled) {
            Map<String, String> maintenanceInfo = new HashMap<>();
            maintenanceInfo.put("enabled", "1");
            maintenanceInfo.put("message", message != null ? message : "服务器维护中");
            maintenanceInfo.put("endTime", endTime != null ? String.valueOf(endTime) : "0");
            redisService.hSetAll("server:maintenance", maintenanceInfo);
        } else {
            redisService.delete("server:maintenance");
        }
        return Result.success();
    }

    /**
     * 热更新配置 (简化接口，详细操作请使用 /gm/config)
     */
    @PostMapping("/reload")
    @RequirePermission("server:reload")
    @GmLog(module = "服务器管理", operation = "热更新配置")
    public Result<Void> reloadConfig(@RequestParam String configType) {
        if ("all".equalsIgnoreCase(configType)) {
            configLoader.reloadAllAndBroadcast("gm");
        } else {
            configLoader.reloadAndBroadcast(configType, "gm");
        }
        return Result.success();
    }

    /**
     * 广播消息到所有服务
     */
    @PostMapping("/broadcast")
    @RequirePermission("server:control")
    @GmLog(module = "服务器管理", operation = "服务器广播")
    public Result<Void> broadcast(@RequestParam String message) {
        redisService.publish("server:broadcast", message);
        return Result.success();
    }
}
