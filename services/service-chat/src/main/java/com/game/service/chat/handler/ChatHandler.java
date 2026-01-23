package com.game.service.chat.handler;

import com.game.api.common.ProtocolConstants;
import com.game.common.enums.ErrorCode;
import com.game.core.handler.BaseHandler;
import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;
import com.game.core.net.session.Session;
import com.game.proto.*;
import com.game.service.chat.service.ChatBusinessService;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 聊天协议处理器
 * <p>
 * 处理聊天相关的客户端请求
 * </p>
 * 
 * <pre>
 * 框架能力使用：
 * - ProtocolController: 模块级协议处理器声明
 * - Protocol: 方法级协议处理声明
 * - PushService: 通过 ChatBusinessService 广播消息
 * - RedisService: 消息存储和发布
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
@ProtocolController(moduleId = ProtocolConstants.PROTOCOL_CHAT, value = "聊天模块")
@RequiredArgsConstructor
public class ChatHandler extends BaseHandler {

    private final ChatBusinessService chatBusinessService;

    /**
     * 协议方法 ID
     */
    private static final int METHOD_SEND_CHAT = 0x01;
    private static final int METHOD_GET_HISTORY = 0x02;

    /**
     * 发送聊天消息
     */
    @Protocol(methodId = METHOD_SEND_CHAT, desc = "发送聊天消息", requireLogin = true, requireRole = true)
    public Message sendChat(Session session, C2S_SendChat request) {
        long roleId = session.getRoleId();
        int channel = request.getChannel();
        String content = request.getContent();
        long targetId = request.getTargetId();

        log.debug("发送聊天消息: roleId={}, channel={}, content={}", roleId, channel, content);

        // 验证消息内容
        if (content == null || content.isBlank() || content.length() > 500) {
            return S2C_SendChat.newBuilder()
                    .setResult(buildErrorResult(ErrorCode.PARAM_ERROR.getCode(), "消息内容无效"))
                    .build();
        }

        // 发送消息
        ChatBusinessService.SendChatResult result = chatBusinessService.sendChat(
                roleId, channel, content, targetId);

        if (!result.success()) {
            return S2C_SendChat.newBuilder()
                    .setResult(buildErrorResult(ErrorCode.SYSTEM_ERROR.getCode(), result.message()))
                    .build();
        }

        return S2C_SendChat.newBuilder()
                .setResult(buildSuccessResult())
                .setMsgId(result.msgId())
                .build();
    }

    /**
     * 获取聊天记录
     */
    @Protocol(methodId = METHOD_GET_HISTORY, desc = "获取聊天记录", requireLogin = true, requireRole = true)
    public Message getChatHistory(Session session, C2S_GetChatHistory request) {
        long roleId = session.getRoleId();
        int channel = request.getChannel();
        long targetId = request.getTargetId();
        long lastMsgId = request.getLastMsgId();
        int count = Math.min(request.getCount(), 100);

        log.debug("获取聊天记录: roleId={}, channel={}, lastMsgId={}, count={}", 
                roleId, channel, lastMsgId, count);

        List<ChatMessage> messages = chatBusinessService.getChatHistory(
                roleId, channel, targetId, lastMsgId, count);

        return S2C_GetChatHistory.newBuilder()
                .setResult(buildSuccessResult())
                .addAllMessages(messages)
                .build();
    }

    // ==================== 辅助方法 ====================

    private com.game.proto.Result buildSuccessResult() {
        return com.game.proto.Result.newBuilder()
                .setCode(0)
                .setMessage("success")
                .build();
    }

    private com.game.proto.Result buildErrorResult(int code, String message) {
        return com.game.proto.Result.newBuilder()
                .setCode(code)
                .setMessage(message != null ? message : "error")
                .build();
    }
}
