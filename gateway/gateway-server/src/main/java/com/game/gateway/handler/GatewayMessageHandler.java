package com.game.gateway.handler;

import com.game.api.login.LoginService;
import com.game.common.enums.ErrorCode;
import com.game.common.protocol.Protocol;
import com.game.common.protocol.ProtocolConstants;
import com.game.common.result.Result;
import com.game.core.net.codec.GameMessage;
import com.game.core.net.session.Session;
import com.game.core.net.session.SessionManager;
import com.game.gateway.router.ServiceRouter;
import com.game.proto.LoginProto.*;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 网关消息处理器
 * <p>
 * 处理网关层协议，转发其他协议到后端服务
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayMessageHandler {

    private final SessionManager sessionManager;
    private final ServiceRouter serviceRouter;

    @DubboReference(check = false, timeout = 5000)
    private LoginService loginService;

    /**
     * 处理心跳请求
     */
    @Protocol(value = ProtocolConstants.HEARTBEAT_REQ, desc = "心跳", requireLogin = false)
    public HeartbeatResponse handleHeartbeat(Session session, GameMessage message) 
            throws InvalidProtocolBufferException {
        HeartbeatRequest request = HeartbeatRequest.parseFrom(message.getBody());
        
        session.updateActiveTime();

        return HeartbeatResponse.newBuilder()
                .setResult(com.game.proto.CommonProto.Result.newBuilder()
                        .setCode(0)
                        .setMessage("success")
                        .build())
                .setServerTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 处理登录请求
     */
    @Protocol(value = 1001, desc = "登录", requireLogin = false)
    public LoginResponse handleLogin(Session session, GameMessage message) 
            throws InvalidProtocolBufferException {
        LoginRequest request = LoginRequest.parseFrom(message.getBody());

        log.info("收到登录请求: sessionId={}, account={}, platform={}", 
                session.getSessionId(), request.getAccount(), request.getPlatform());

        // 调用登录服务
        Result<com.game.api.login.LoginResultDTO> result = loginService.login(
                request.getAccount(),
                request.getPassword(),
                request.getDeviceId(),
                request.getPlatform(),
                request.getVersion(),
                request.getChannel()
        );

        if (!result.isSuccess()) {
            return LoginResponse.newBuilder()
                    .setResult(com.game.proto.CommonProto.Result.newBuilder()
                            .setCode(result.getCode())
                            .setMessage(result.getMessage())
                            .build())
                    .build();
        }

        var loginResult = result.getData();

        // 绑定账号
        session.setAccountId(loginResult.getAccountId());

        // 构建响应
        LoginResponse.Builder builder = LoginResponse.newBuilder()
                .setResult(com.game.proto.CommonProto.Result.newBuilder()
                        .setCode(0)
                        .setMessage("success")
                        .build())
                .setToken(loginResult.getToken())
                .setExpireTime(loginResult.getExpireTime());

        // 添加角色列表
        if (loginResult.getRoles() != null) {
            for (var role : loginResult.getRoles()) {
                builder.addRoles(RoleBrief.newBuilder()
                        .setRoleId(role.getRoleId())
                        .setRoleName(role.getRoleName())
                        .setLevel(role.getLevel())
                        .setAvatarId(role.getAvatarId())
                        .setLastLogin(role.getLastLoginTime())
                        .setServerId(role.getServerId())
                        .build());
            }
        }

        log.info("登录成功: sessionId={}, accountId={}", 
                session.getSessionId(), loginResult.getAccountId());

        return builder.build();
    }

    /**
     * 处理创建角色请求
     */
    @Protocol(value = 1002, desc = "创建角色", requireLogin = false)
    public CreateRoleResponse handleCreateRole(Session session, GameMessage message) 
            throws InvalidProtocolBufferException {
        CreateRoleRequest request = CreateRoleRequest.parseFrom(message.getBody());

        if (session.getAccountId() <= 0) {
            return CreateRoleResponse.newBuilder()
                    .setResult(com.game.proto.CommonProto.Result.newBuilder()
                            .setCode(ErrorCode.TOKEN_INVALID.getCode())
                            .setMessage("请先登录")
                            .build())
                    .build();
        }

        log.info("创建角色: sessionId={}, accountId={}, roleName={}", 
                session.getSessionId(), session.getAccountId(), request.getRoleName());

        // 调用登录服务创建角色
        Result<com.game.api.login.RoleBriefDTO> result = loginService.createRole(
                session.getAccountId(),
                request.getRoleName(),
                request.getAvatarId(),
                request.getGender()
        );

        if (!result.isSuccess()) {
            return CreateRoleResponse.newBuilder()
                    .setResult(com.game.proto.CommonProto.Result.newBuilder()
                            .setCode(result.getCode())
                            .setMessage(result.getMessage())
                            .build())
                    .build();
        }

        var role = result.getData();

        return CreateRoleResponse.newBuilder()
                .setResult(com.game.proto.CommonProto.Result.newBuilder()
                        .setCode(0)
                        .setMessage("success")
                        .build())
                .setRole(RoleBrief.newBuilder()
                        .setRoleId(role.getRoleId())
                        .setRoleName(role.getRoleName())
                        .setLevel(role.getLevel())
                        .setAvatarId(role.getAvatarId())
                        .setLastLogin(role.getLastLoginTime())
                        .setServerId(role.getServerId())
                        .build())
                .build();
    }

    /**
     * 处理进入游戏请求
     */
    @Protocol(value = 1003, desc = "进入游戏", requireLogin = false)
    public EnterGameResponse handleEnterGame(Session session, GameMessage message) 
            throws InvalidProtocolBufferException {
        EnterGameRequest request = EnterGameRequest.parseFrom(message.getBody());

        if (session.getAccountId() <= 0) {
            return EnterGameResponse.newBuilder()
                    .setResult(com.game.proto.CommonProto.Result.newBuilder()
                            .setCode(ErrorCode.TOKEN_INVALID.getCode())
                            .setMessage("请先登录")
                            .build())
                    .build();
        }

        long roleId = request.getRoleId();

        log.info("进入游戏: sessionId={}, accountId={}, roleId={}", 
                session.getSessionId(), session.getAccountId(), roleId);

        // 调用登录服务
        Result<Void> enterResult = loginService.enterGame(session.getAccountId(), roleId);
        if (!enterResult.isSuccess()) {
            return EnterGameResponse.newBuilder()
                    .setResult(com.game.proto.CommonProto.Result.newBuilder()
                            .setCode(enterResult.getCode())
                            .setMessage(enterResult.getMessage())
                            .build())
                    .build();
        }

        // TODO: 从游戏服务获取完整玩家信息
        // 绑定角色到 Session
        sessionManager.bindRole(session, roleId, "Player_" + roleId);

        // 构建响应 (简化版，实际应从游戏服务获取)
        return EnterGameResponse.newBuilder()
                .setResult(com.game.proto.CommonProto.Result.newBuilder()
                        .setCode(0)
                        .setMessage("success")
                        .build())
                .setPlayer(PlayerFullInfo.newBuilder()
                        .setRoleId(roleId)
                        .setRoleName(session.getRoleName())
                        .setLevel(1)
                        .setExp(0)
                        .setGold(0)
                        .setDiamond(0)
                        .setVipLevel(0)
                        .setEnergy(100)
                        .setMaxEnergy(100)
                        .build())
                .build();
    }

    /**
     * 处理登出请求
     */
    @Protocol(value = 1004, desc = "登出")
    public LogoutResponse handleLogout(Session session, GameMessage message) {
        long roleId = session.getRoleId();

        log.info("玩家登出: sessionId={}, roleId={}", session.getSessionId(), roleId);

        // 调用登录服务
        if (roleId > 0) {
            loginService.logout(roleId);
        }

        // 解绑角色
        sessionManager.unbindRole(session);

        return LogoutResponse.newBuilder()
                .setResult(com.game.proto.CommonProto.Result.newBuilder()
                        .setCode(0)
                        .setMessage("success")
                        .build())
                .build();
    }
}
