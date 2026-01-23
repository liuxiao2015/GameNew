package com.game.service.login.handler;

import com.game.api.common.MethodId;
import com.game.api.common.ProtocolConstants;
import com.game.api.login.AccountService;
import com.game.api.login.dto.AccountDTO;
import com.game.api.login.dto.ServerDTO;
import com.game.api.login.RoleBriefDTO;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.core.handler.BaseHandler;
import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;
import com.game.core.net.session.Session;
import com.game.proto.*;
import com.game.service.login.service.AccountServiceImpl;
import com.game.service.login.service.LoginServiceImpl;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 登录协议处理器
 * <p>
 * 完整登录流程:
 * 1. 握手 (Handshake) - 客户端连接后首先发送
 * 2. 账号登录 (AccountLogin) - 支持游客/账号/Google/Facebook
 * 3. 获取服务器列表 (GetServerList)
 * 4. 选择服务器 (SelectServer) - 获取该服务器上的角色列表
 * 5. 创建角色 (CreateRole) - 如果没有角色
 * 6. 进入游戏 (EnterGame) - 选择角色进入
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@ProtocolController(moduleId = ProtocolConstants.PROTOCOL_LOGIN, value = "登录模块")
@RequiredArgsConstructor
public class LoginHandler extends BaseHandler {

    private final AccountServiceImpl accountService;
    private final LoginServiceImpl loginService;

    // ==================== 第一阶段: 握手 ====================
    // 注意: 握手协议由 Gateway 独占处理 (GatewayMessageHandler.handleHandshake)
    // 这里不再重复注册握手处理器，避免协议路由冲突

    // ==================== 第二阶段: 账号登录 ====================

    /**
     * 账号登录 - 支持多种登录方式
     */
    @Protocol(methodId = MethodId.Login.ACCOUNT_LOGIN, desc = "账号登录", requireLogin = false, requireRole = false)
    public Message accountLogin(Session session, C2S_AccountLogin request) {
        log.info("收到账号登录请求: type={}, deviceId={}",
                request.getLoginType(), request.getDeviceId());

        Result<AccountDTO> result;
        
        switch (request.getLoginType()) {
            case LOGIN_TYPE_GUEST:
                // 游客登录
                result = accountService.guestLogin(
                        request.getDeviceId(),
                        request.getPlatform(),
                        request.getChannel()
                );
                break;
                
            case LOGIN_TYPE_ACCOUNT:
                // 账号密码登录
                result = accountService.accountLogin(
                        request.getAccount(),
                        request.getCredential(),
                        request.getDeviceId()
                );
                break;
                
            case LOGIN_TYPE_GOOGLE:
            case LOGIN_TYPE_FACEBOOK:
            case LOGIN_TYPE_APPLE:
                // 第三方登录
                result = accountService.thirdPartyLogin(
                        request.getLoginTypeValue(),
                        request.getCredential(),
                        request.getDeviceId()
                );
                break;
                
            case LOGIN_TYPE_TOKEN:
                // Token 自动登录
                result = accountService.tokenLogin(
                        request.getCredential(),
                        request.getDeviceId()
                );
                break;
                
            default:
                result = Result.fail(ErrorCode.PARAM_ERROR, "不支持的登录类型");
        }

        S2C_AccountLogin.Builder responseBuilder = S2C_AccountLogin.newBuilder();

        if (result.isSuccess()) {
            AccountDTO account = result.getData();
            
            // 绑定账号到 Session
            session.setAccountId(Long.parseLong(account.getAccountId()));
            session.setToken(account.getToken());
            session.setAuthenticated(true);

            responseBuilder.setResult(buildSuccessResult())
                    .setAccountId(account.getAccountId())
                    .setToken(account.getToken())
                    .setTokenExpireTime(account.getTokenExpireTime())
                    .setIsNewAccount(account.isNewAccount());

            log.info("账号登录成功: accountId={}, type={}, isNew={}",
                    account.getAccountId(), request.getLoginType(), account.isNewAccount());
        } else {
            responseBuilder.setResult(buildErrorResult(result.getCode(), result.getMessage()));
            log.warn("账号登录失败: type={}, code={}", request.getLoginType(), result.getCode());
        }

        return responseBuilder.build();
    }

    /**
     * 绑定账号 - 游客绑定正式账号
     */
    @Protocol(methodId = MethodId.Login.BIND_ACCOUNT, desc = "绑定账号", requireLogin = true, requireRole = false)
    public Message bindAccount(Session session, C2S_BindAccount request) {
        log.info("收到绑定账号请求: accountId={}, type={}",
                session.getAccountId(), request.getBindType());

        Result<Void> result;
        
        if (request.getBindType() == LoginType.LOGIN_TYPE_ACCOUNT) {
            // 绑定账号密码
            String[] parts = request.getCredential().split(":");
            result = accountService.bindAccount(
                    String.valueOf(session.getAccountId()),
                    parts.length > 0 ? parts[0] : "",
                    parts.length > 1 ? parts[1] : "",
                    request.getEmail()
            );
        } else {
            // 绑定第三方账号
            result = accountService.bindThirdParty(
                    String.valueOf(session.getAccountId()),
                    request.getBindTypeValue(),
                    request.getCredential()
            );
        }

        S2C_BindAccount.Builder responseBuilder = S2C_BindAccount.newBuilder();

        if (result.isSuccess()) {
            responseBuilder.setResult(buildSuccessResult())
                    .setAccountId(String.valueOf(session.getAccountId()));
        } else {
            responseBuilder.setResult(buildErrorResult(result.getCode(), result.getMessage()));
        }

        return responseBuilder.build();
    }

    // ==================== 第三阶段: 服务器列表 & 选服 ====================

    /**
     * 获取服务器列表
     */
    @Protocol(methodId = MethodId.Login.GET_SERVER_LIST, desc = "获取服务器列表", requireLogin = true, requireRole = false)
    public Message getServerList(Session session, C2S_GetServerList request) {
        log.info("收到获取服务器列表请求: accountId={}", session.getAccountId());

        Result<List<ServerDTO>> result = accountService.getServerList(
                String.valueOf(session.getAccountId()));

        S2C_GetServerList.Builder responseBuilder = S2C_GetServerList.newBuilder();

        if (result.isSuccess()) {
            List<ServerDTO> servers = result.getData();
            
            // 按分组整理服务器
            java.util.Map<Integer, ServerGroup.Builder> groups = new java.util.HashMap<>();
            
            for (ServerDTO server : servers) {
                int groupId = server.getGroupId();
                if (!groups.containsKey(groupId)) {
                    groups.put(groupId, ServerGroup.newBuilder()
                            .setGroupId(groupId)
                            .setGroupName(getGroupName(groupId)));
                }
                groups.get(groupId).addServers(toServerInfo(server));
            }
            
            groups.values().forEach(g -> responseBuilder.addServerGroups(g.build()));

            // 设置推荐服务器
            Result<ServerDTO> recommendedResult = accountService.getRecommendedServer(
                    String.valueOf(session.getAccountId()));
            if (recommendedResult.isSuccess()) {
                responseBuilder.setRecommendedServerId(recommendedResult.getData().getServerId());
            }

            responseBuilder.setResult(buildSuccessResult());
        } else {
            responseBuilder.setResult(buildErrorResult(result.getCode(), result.getMessage()));
        }

        return responseBuilder.build();
    }

    /**
     * 选择服务器
     */
    @Protocol(methodId = MethodId.Login.SELECT_SERVER, desc = "选择服务器", requireLogin = true, requireRole = false)
    public Message selectServer(Session session, C2S_SelectServer request) {
        log.info("收到选择服务器请求: accountId={}, serverId={}",
                session.getAccountId(), request.getServerId());

        // 保存选择的服务器
        session.setServerId(request.getServerId());

        // 获取该服务器上的角色列表
        Result<List<RoleBriefDTO>> rolesResult = loginService.getRoleList(session.getAccountId());

        S2C_SelectServer.Builder responseBuilder = S2C_SelectServer.newBuilder();
        responseBuilder.setResult(buildSuccessResult());
        
        // TODO: 返回游戏服务器地址（当使用分布式部署时）
        responseBuilder.setGameServerHost("127.0.0.1");
        responseBuilder.setGameServerPort(9000);
        responseBuilder.setEnterToken(session.getToken());

        // 添加角色列表
        if (rolesResult.isSuccess()) {
            for (RoleBriefDTO role : rolesResult.getData()) {
                if (role.getServerId() == request.getServerId()) {
                    responseBuilder.addRoles(toRoleBrief(role));
                }
            }
        }

        return responseBuilder.build();
    }

    // ==================== 第四阶段: 创建角色 ====================

    /**
     * 检查角色名是否可用
     */
    @Protocol(methodId = MethodId.Login.CHECK_ROLE_NAME, desc = "检查角色名", requireLogin = true, requireRole = false)
    public Message checkRoleName(Session session, C2S_CheckRoleName request) {
        Result<Boolean> result = loginService.checkRoleName(request.getRoleName());

        S2C_CheckRoleName.Builder responseBuilder = S2C_CheckRoleName.newBuilder();

        if (result.isSuccess()) {
            boolean available = result.getData();
            responseBuilder.setResult(buildSuccessResult())
                    .setAvailable(available);
            if (!available) {
                responseBuilder.setSuggestion(generateSuggestedName(request.getRoleName()));
            }
        } else {
            responseBuilder.setResult(buildErrorResult(result.getCode(), result.getMessage()));
        }

        return responseBuilder.build();
    }

    /**
     * 创建角色
     */
    @Protocol(methodId = MethodId.Login.CREATE_ROLE, desc = "创建角色", requireLogin = true, requireRole = false)
    public Message createRole(Session session, C2S_CreateRole request) {
        log.info("收到创建角色请求: accountId={}, roleName={}",
                session.getAccountId(), request.getRoleName());

        Result<RoleBriefDTO> result = loginService.createRole(
                session.getAccountId(),
                request.getRoleName(),
                request.getAvatarId(),
                request.getGender()
        );

        S2C_CreateRole.Builder responseBuilder = S2C_CreateRole.newBuilder();

        if (result.isSuccess()) {
            RoleBriefDTO role = result.getData();
            responseBuilder.setResult(buildSuccessResult())
                    .setRole(toRoleBrief(role));
            log.info("创建角色成功: roleId={}, roleName={}", role.getRoleId(), role.getRoleName());
        } else {
            responseBuilder.setResult(buildErrorResult(result.getCode(), result.getMessage()));
        }

        return responseBuilder.build();
    }

    // ==================== 第五阶段: 进入游戏 ====================

    /**
     * 进入游戏
     */
    @Protocol(methodId = MethodId.Login.ENTER_GAME, desc = "进入游戏", requireLogin = true, requireRole = false)
    public Message enterGame(Session session, C2S_EnterGame request) {
        log.info("收到进入游戏请求: accountId={}, roleId={}",
                session.getAccountId(), request.getRoleId());

        // 验证角色并进入游戏
        Result<Void> result = loginService.enterGame(session.getAccountId(), request.getRoleId());

        S2C_EnterGame.Builder responseBuilder = S2C_EnterGame.newBuilder();

        if (result.isSuccess()) {
            // 绑定角色到 Session
            session.setRoleId(request.getRoleId());

            // 构建玩家信息 (实际应从 Game 服务获取)
            PlayerFullInfo playerInfo = buildPlayerFullInfo(request.getRoleId());

            responseBuilder.setResult(buildSuccessResult())
                    .setPlayer(playerInfo)
                    .setServerTime(System.currentTimeMillis());

            log.info("进入游戏成功: accountId={}, roleId={}", session.getAccountId(), request.getRoleId());
        } else {
            responseBuilder.setResult(buildErrorResult(result.getCode(), result.getMessage()));
        }

        return responseBuilder.build();
    }

    // ==================== 其他: 登出/重连/心跳 ====================

    /**
     * 登出
     */
    @Protocol(methodId = MethodId.Login.LOGOUT, desc = "登出", requireLogin = true, requireRole = false)
    public Message logout(Session session, C2S_Logout request) {
        log.info("收到登出请求: roleId={}", session.getRoleId());

        if (session.getRoleId() > 0) {
            loginService.logout(session.getRoleId());
        }

        // 清理 Session
        session.setRoleId(0);

        return S2C_Logout.newBuilder()
                .setResult(buildSuccessResult())
                .build();
    }

    /**
     * 重连
     */
    @Protocol(methodId = MethodId.Login.RECONNECT, desc = "重连", requireLogin = false, requireRole = false)
    public Message reconnect(Session session, C2S_Reconnect request) {
        log.info("收到重连请求: token={}, roleId={}", request.getToken(), request.getRoleId());

        // 验证 Token
        Result<String> tokenResult = accountService.verifyToken(request.getToken());
        if (!tokenResult.isSuccess()) {
            return S2C_Reconnect.newBuilder()
                    .setResult(buildErrorResult(tokenResult.getCode(), tokenResult.getMessage()))
                    .build();
        }

        // 重新绑定 Session
        session.setAccountId(Long.parseLong(tokenResult.getData()));
        session.setRoleId(request.getRoleId());
        session.setServerId(request.getServerId());
        session.setToken(request.getToken());
        session.setAuthenticated(true);

        // 返回玩家数据
        PlayerFullInfo playerInfo = buildPlayerFullInfo(request.getRoleId());

        log.info("重连成功: accountId={}, roleId={}", tokenResult.getData(), request.getRoleId());

        return S2C_Reconnect.newBuilder()
                .setResult(buildSuccessResult())
                .setPlayer(playerInfo)
                .setServerTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 心跳
     */
    @Protocol(methodId = MethodId.Login.HEARTBEAT, desc = "心跳", requireLogin = true, requireRole = false)
    public Message heartbeat(Session session, C2S_Heartbeat request) {
        session.updateActiveTime();
        return S2C_Heartbeat.newBuilder()
                .setResult(buildSuccessResult())
                .setServerTime(System.currentTimeMillis())
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

    private ServerInfo toServerInfo(ServerDTO dto) {
        return ServerInfo.newBuilder()
                .setServerId(dto.getServerId())
                .setServerName(dto.getServerName())
                .setStatus(dto.getStatus())
                .setOpenTime(dto.getOpenTime())
                .setServerTag(dto.getTag() != null ? dto.getTag() : "")
                .build();
    }

    private RoleBrief toRoleBrief(RoleBriefDTO dto) {
        return RoleBrief.newBuilder()
                .setRoleId(dto.getRoleId())
                .setRoleName(dto.getRoleName())
                .setLevel(dto.getLevel())
                .setAvatarId(dto.getAvatarId())
                .setLastLoginTime(dto.getLastLoginTime())
                .setServerId(dto.getServerId())
                .setCreateTime(dto.getCreateTime())
                .build();
    }

    private PlayerFullInfo buildPlayerFullInfo(long roleId) {
        // TODO: 实际应从 Game 服务获取完整玩家数据
        return PlayerFullInfo.newBuilder()
                .setRoleId(roleId)
                .setRoleName("测试角色")
                .setLevel(1)
                .setExp(0)
                .setGold(1000)
                .setDiamond(100)
                .setBindDiamond(0)
                .setVipLevel(0)
                .setVipExp(0)
                .setAvatarId(1)
                .setFrameId(0)
                .setGender(1)
                .setProfession(0)
                .setEnergy(100)
                .setMaxEnergy(100)
                .setEnergyRecoverTime(0)
                .setCombatPower(100)
                .setCreateTime(System.currentTimeMillis())
                .setGuildId(0)
                .setGuildName("")
                .setGuildPosition(0)
                .setSignature("")
                .build();
    }

    private String getGroupName(int groupId) {
        switch (groupId) {
            case 1: return "推荐服务器";
            case 2: return "最新服务器";
            case 3: return "经典服务器";
            default: return "服务器";
        }
    }

    private String generateSuggestedName(String baseName) {
        return baseName + (int)(Math.random() * 1000);
    }
}
