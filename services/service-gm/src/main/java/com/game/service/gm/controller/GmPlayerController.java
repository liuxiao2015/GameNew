package com.game.service.gm.controller;

import com.game.api.player.PlayerDTO;
import com.game.api.player.PlayerService;
import com.game.common.result.Result;
import com.game.service.gm.annotation.GmLog;
import com.game.service.gm.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GM 玩家管理控制器
 *
 * @author GameServer
 */
@Tag(name = "玩家管理", description = "玩家查询、封禁、邮件、物品发放等操作")
@RestController
@RequestMapping("/gm/player")
@RequiredArgsConstructor
public class GmPlayerController {

    @DubboReference
    private PlayerService playerService;

    @Operation(summary = "查询玩家信息", description = "根据角色ID查询玩家详细信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = PlayerDTO.class)))
    @GetMapping("/{roleId}")
    @RequirePermission("player:view")
    public Result<PlayerDTO> getPlayer(
            @Parameter(description = "角色ID", required = true, example = "10001")
            @PathVariable long roleId) {
        return playerService.getPlayerInfo(roleId);
    }

    @Operation(summary = "搜索玩家", description = "根据角色名、角色ID或账号ID搜索玩家")
    @GetMapping("/search")
    @RequirePermission("player:view")
    public Result<?> searchPlayer(
            @Parameter(description = "角色名（模糊匹配）") @RequestParam(required = false) String roleName,
            @Parameter(description = "角色ID（精确匹配）") @RequestParam(required = false) Long roleId,
            @Parameter(description = "账号ID（精确匹配）") @RequestParam(required = false) String accountId) {
        // 实际实现: 调用 PlayerService 搜索
        return Result.success();
    }

    @Operation(summary = "修改玩家数据", description = "修改玩家的属性数据，如等级、金币等")
    @PostMapping("/{roleId}/modify")
    @RequirePermission("player:modify")
    @GmLog(module = "玩家管理", operation = "修改玩家数据")
    public Result<Void> modifyPlayer(
            @Parameter(description = "角色ID", required = true) @PathVariable long roleId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "要修改的属性，如 {\"level\": 50, \"gold\": 10000}",
                    content = @Content(schema = @Schema(type = "object", example = "{\"level\": 50, \"gold\": 10000}"))
            )
            @RequestBody Map<String, Object> data) {
        // 实际实现: 修改玩家属性
        return Result.success();
    }

    @Operation(summary = "封禁玩家", description = "封禁指定玩家账号")
    @PostMapping("/{roleId}/ban")
    @RequirePermission("player:ban")
    @GmLog(module = "玩家管理", operation = "封号")
    public Result<Void> banPlayer(
            @Parameter(description = "角色ID", required = true) @PathVariable long roleId,
            @Parameter(description = "封禁时长（秒），-1 表示永久", required = true, example = "86400") @RequestParam long duration,
            @Parameter(description = "封禁原因", required = true, example = "使用外挂") @RequestParam String reason) {
        // 实际实现: 调用封号服务
        return Result.success();
    }

    @Operation(summary = "解封玩家", description = "解除玩家的封禁状态")
    @PostMapping("/{roleId}/unban")
    @RequirePermission("player:ban")
    @GmLog(module = "玩家管理", operation = "解封")
    public Result<Void> unbanPlayer(
            @Parameter(description = "角色ID", required = true) @PathVariable long roleId) {
        // 实际实现: 调用解封服务
        return Result.success();
    }

    @Operation(summary = "发送邮件", description = "向指定玩家发送游戏内邮件，可附带物品")
    @PostMapping("/{roleId}/mail")
    @RequirePermission("mail:send")
    @GmLog(module = "玩家管理", operation = "发送邮件")
    public Result<Void> sendMail(
            @Parameter(description = "角色ID", required = true) @PathVariable long roleId,
            @Parameter(description = "邮件标题", required = true, example = "系统补偿") @RequestParam String title,
            @Parameter(description = "邮件内容", required = true, example = "亲爱的玩家，这是系统补偿...") @RequestParam String content,
            @Parameter(description = "附件物品 (JSON格式: [{\"itemId\":1001,\"count\":10}])", example = "[{\"itemId\":1001,\"count\":10}]") @RequestParam(required = false) String items) {
        // 实际实现: 发送个人邮件
        return Result.success();
    }

    @Operation(summary = "发放物品", description = "直接向玩家背包发放物品")
    @PostMapping("/{roleId}/giveItem")
    @RequirePermission("item:give")
    @GmLog(module = "玩家管理", operation = "发放物品")
    public Result<Void> giveItem(
            @Parameter(description = "角色ID", required = true) @PathVariable long roleId,
            @Parameter(description = "物品ID", required = true, example = "1001") @RequestParam int itemId,
            @Parameter(description = "数量", required = true, example = "100") @RequestParam int count) {
        // 实际实现: 发放物品
        return Result.success();
    }

    @Operation(summary = "踢玩家下线", description = "强制断开玩家的游戏连接")
    @PostMapping("/{roleId}/kick")
    @RequirePermission("player:kick")
    @GmLog(module = "玩家管理", operation = "踢下线")
    public Result<Void> kickPlayer(
            @Parameter(description = "角色ID", required = true) @PathVariable long roleId) {
        // 实际实现: 断开玩家连接
        return Result.success();
    }
}
