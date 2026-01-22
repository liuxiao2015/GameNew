package com.game.service.gm.controller;

import com.game.api.chat.ChatService;
import com.game.common.result.Result;
import com.game.service.gm.annotation.GmLog;
import com.game.service.gm.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * GM 公告管理控制器
 *
 * @author GameServer
 */
@RestController
@RequestMapping("/gm/notice")
@RequiredArgsConstructor
public class GmNoticeController {

    @DubboReference
    private ChatService chatService;

    /**
     * 发送滚动公告
     */
    @PostMapping("/scroll")
    @RequirePermission("notice:send")
    @GmLog(module = "公告管理", operation = "发送滚动公告")
    public Result<Void> sendScrollNotice(
            @RequestParam String title,
            @RequestParam String content) {
        return chatService.sendSystemNotice(1, title, content);
    }

    /**
     * 发送弹窗公告
     */
    @PostMapping("/popup")
    @RequirePermission("notice:send")
    @GmLog(module = "公告管理", operation = "发送弹窗公告")
    public Result<Void> sendPopupNotice(
            @RequestParam String title,
            @RequestParam String content) {
        return chatService.sendSystemNotice(2, title, content);
    }

    /**
     * 发送全服邮件
     */
    @PostMapping("/mail")
    @RequirePermission("mail:broadcast")
    @GmLog(module = "公告管理", operation = "发送全服邮件")
    public Result<Void> sendBroadcastMail(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String items,
            @RequestParam(required = false) Integer minLevel,
            @RequestParam(required = false) Integer maxLevel) {
        // 实际实现: 发送全服邮件
        return Result.success();
    }
}
