package com.game.api.login;

import com.game.common.result.Result;

import java.util.List;

/**
 * 登录服务接口
 * <p>
 * Dubbo 服务接口定义，由 service-login 模块实现
 * </p>
 *
 * @author GameServer
 */
public interface LoginService {

    /**
     * 账号登录
     *
     * @param account  账号
     * @param password 密码 (MD5)
     * @param deviceId 设备 ID
     * @param platform 平台
     * @param version  客户端版本
     * @param channel  渠道
     * @return 登录结果
     */
    Result<LoginResultDTO> login(String account, String password, String deviceId,
                                  String platform, String version, String channel);

    /**
     * Token 验证
     *
     * @param token Token
     * @return 验证结果 (账号 ID)
     */
    Result<Long> verifyToken(String token);

    /**
     * 刷新 Token
     *
     * @param token 旧 Token
     * @return 新 Token
     */
    Result<String> refreshToken(String token);

    /**
     * 获取角色列表
     *
     * @param accountId 账号 ID
     * @return 角色列表
     */
    Result<List<RoleBriefDTO>> getRoleList(long accountId);

    /**
     * 创建角色
     *
     * @param accountId 账号 ID
     * @param roleName  角色名
     * @param avatarId  头像 ID
     * @param gender    性别
     * @return 创建的角色信息
     */
    Result<RoleBriefDTO> createRole(long accountId, String roleName, int avatarId, int gender);

    /**
     * 进入游戏
     *
     * @param accountId 账号 ID
     * @param roleId    角色 ID
     * @return 操作结果
     */
    Result<Void> enterGame(long accountId, long roleId);

    /**
     * 登出
     *
     * @param roleId 角色 ID
     * @return 操作结果
     */
    Result<Void> logout(long roleId);

    /**
     * 检查角色名是否可用
     *
     * @param roleName 角色名
     * @return true: 可用, false: 不可用
     */
    Result<Boolean> checkRoleName(String roleName);
}
