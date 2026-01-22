package com.game.service.login.handler;

import com.game.api.common.ProtocolConstants;
import com.game.core.handler.BaseHandler;
import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;
import com.game.core.net.session.Session;
import com.game.service.login.service.LoginServiceImpl;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录协议处理器
 * <p>
 * 使用 @ProtocolController 和 @Protocol 注解自动注册
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@ProtocolController(moduleId = ProtocolConstants.PROTOCOL_LOGIN, value = "登录模块")
@RequiredArgsConstructor
public class LoginHandler extends BaseHandler {

    private final LoginServiceImpl loginService;

    /**
     * 登录请求
     * <p>
     * 协议号: 0x0100 + 0x01 = 0x0101
     * </p>
     */
    @Protocol(methodId = 0x01, desc = "登录请求", requireLogin = false, requireRole = false)
    public Message login(Session session, byte[] requestData) {
        // 示例：解析请求数据并处理登录
        // LoginRequest request = LoginRequest.parseFrom(requestData);
        // Result<?> result = loginService.login(request.getUsername(), request.getPassword(), ...);

        log.info("收到登录请求: session={}", session.getSessionId());

        // TODO: 实际实现需要解析 Protobuf 请求并调用 loginService
        // 这里返回 null 表示暂不响应
        return null;
    }

    /**
     * 注册请求
     */
    @Protocol(methodId = 0x02, desc = "注册请求", requireLogin = false, requireRole = false)
    public Message register(Session session, byte[] requestData) {
        log.info("收到注册请求: session={}", session.getSessionId());
        // TODO: 实现注册逻辑
        return null;
    }

    /**
     * 选择角色
     */
    @Protocol(methodId = 0x03, desc = "选择角色", requireLogin = true, requireRole = false)
    public Message selectRole(Session session, byte[] requestData) {
        log.info("收到选择角色请求: session={}, accountId={}",
                session.getSessionId(), session.getAccountId());
        // TODO: 实现选角逻辑
        return null;
    }

    /**
     * 创建角色
     */
    @Protocol(methodId = 0x04, desc = "创建角色", requireLogin = true, requireRole = false)
    public Message createRole(Session session, byte[] requestData) {
        log.info("收到创建角色请求: session={}, accountId={}",
                session.getSessionId(), session.getAccountId());
        // TODO: 实现创角逻辑
        return null;
    }

    /**
     * 检查角色名
     */
    @Protocol(methodId = 0x05, desc = "检查角色名", requireLogin = true, requireRole = false)
    public Message checkRoleName(Session session, byte[] requestData) {
        // TODO: 实现角色名检查
        return null;
    }
}
