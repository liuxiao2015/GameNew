package com.game.service.gm.controller;

import com.game.api.player.PlayerDTO;
import com.game.api.player.PlayerService;
import com.game.common.result.Result;
import com.game.service.gm.annotation.GmLog;
import com.game.service.gm.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GM 玩家管理控制器
 *
 * @author GameServer
 */
@RestController
@RequestMapping("/gm/player")
@RequiredArgsConstructor
public class GmPlayerController {

    @DubboReference
    private PlayerService playerService;

    /**
     * 查询玩家信息
     */
    @GetMapping("/{roleId}")
    @RequirePermission("player:view")
    public Result<PlayerDTO> getPlayer(@PathVariable long roleId) {
        return playerService.getPlayerInfo(roleId);
    }

    /**
     * 搜索玩家
     */
    @GetMapping("/search")
    @RequirePermission("player:view")
    public Result<?> searchPlayer(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String accountId) {
        // 实际实现: 调用 PlayerService 搜索
        return Result.success();
    }

    /**
     * 修改玩家数据
     */
    @PostMapping("/{roleId}/modify")
    @RequirePermission("player:modify")
    @GmLog(module = "玩家管理", operation = "修改玩家数据")
    public Result<Void> modifyPlayer(@PathVariable long roleId, @RequestBody Map<String, Object> data) {
        // 实际实现: 修改玩家属性
        return Result.success();
    }

    /**
     * 封号
     */
    @PostMapping("/{roleId}/ban")
    @RequirePermission("player:ban")
    @GmLog(module = "玩家管理", operation = "封号")
    public Result<Void> banPlayer(
            @PathVariable long roleId,
            @RequestParam long duration,
            @RequestParam String reason) {
        // 实际实现: 调用封号服务
        return Result.success();
    }

    /**
     * 解封
     */
    @PostMapping("/{roleId}/unban")
    @RequirePermission("player:ban")
    @GmLog(module = "玩家管理", operation = "解封")
    public Result<Void> unbanPlayer(@PathVariable long roleId) {
        // 实际实现: 调用解封服务
        return Result.success();
    }

    /**
     * 发送邮件
     */
    @PostMapping("/{roleId}/mail")
    @RequirePermission("mail:send")
    @GmLog(module = "玩家管理", operation = "发送邮件")
    public Result<Void> sendMail(
            @PathVariable long roleId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String items) {
        // 实际实现: 发送个人邮件
        return Result.success();
    }

    /**
     * 发放物品
     */
    @PostMapping("/{roleId}/giveItem")
    @RequirePermission("item:give")
    @GmLog(module = "玩家管理", operation = "发放物品")
    public Result<Void> giveItem(
            @PathVariable long roleId,
            @RequestParam int itemId,
            @RequestParam int count) {
        // 实际实现: 发放物品
        return Result.success();
    }

    /**
     * 踢下线
     */
    @PostMapping("/{roleId}/kick")
    @RequirePermission("player:kick")
    @GmLog(module = "玩家管理", operation = "踢下线")
    public Result<Void> kickPlayer(@PathVariable long roleId) {
        // 实际实现: 断开玩家连接
        return Result.success();
    }
}
