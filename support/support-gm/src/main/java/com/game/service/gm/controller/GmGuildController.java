package com.game.service.gm.controller;

import com.game.api.guild.GuildDTO;
import com.game.api.guild.GuildService;
import com.game.common.result.Result;
import com.game.service.gm.annotation.GmLog;
import com.game.service.gm.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * GM 公会管理控制器
 *
 * @author GameServer
 */
@RestController
@RequestMapping("/gm/guild")
@RequiredArgsConstructor
public class GmGuildController {

    @DubboReference
    private GuildService guildService;

    /**
     * 查询公会信息
     */
    @GetMapping("/{guildId}")
    @RequirePermission("guild:view")
    public Result<GuildDTO> getGuild(@PathVariable long guildId) {
        return guildService.getGuildInfo(guildId);
    }

    /**
     * 搜索公会
     */
    @GetMapping("/search")
    @RequirePermission("guild:view")
    public Result<?> searchGuild(
            @RequestParam(required = false) String guildName,
            @RequestParam(required = false) Long guildId) {
        // 实际实现: 搜索公会
        return Result.success();
    }

    /**
     * 解散公会
     */
    @PostMapping("/{guildId}/dissolve")
    @RequirePermission("guild:dissolve")
    @GmLog(module = "公会管理", operation = "解散公会")
    public Result<Void> dissolveGuild(@PathVariable long guildId, @RequestParam String reason) {
        // 实际实现: 强制解散公会
        return Result.success();
    }

    /**
     * 修改公会名称
     */
    @PostMapping("/{guildId}/rename")
    @RequirePermission("guild:modify")
    @GmLog(module = "公会管理", operation = "修改公会名称")
    public Result<Void> renameGuild(@PathVariable long guildId, @RequestParam String newName) {
        // 实际实现: 修改公会名称
        return Result.success();
    }

    /**
     * 转移会长
     */
    @PostMapping("/{guildId}/transferLeader")
    @RequirePermission("guild:modify")
    @GmLog(module = "公会管理", operation = "转移会长")
    public Result<Void> transferLeader(
            @PathVariable long guildId,
            @RequestParam long newLeaderId) {
        // 实际实现: 强制转移会长
        return Result.success();
    }
}
