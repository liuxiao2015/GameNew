package com.game.api.login;

import com.game.api.login.dto.AccountDTO;
import com.game.api.login.dto.ServerDTO;
import com.game.common.result.Result;

import java.util.List;

/**
 * 账号服务接口
 * <p>
 * 处理账号登录、注册、绑定等
 * </p>
 *
 * @author GameServer
 */
public interface AccountService {

    // ==================== 登录类型常量 ====================

    int LOGIN_TYPE_GUEST = 0;     // 游客登录
    int LOGIN_TYPE_ACCOUNT = 1;   // 账号密码登录
    int LOGIN_TYPE_GOOGLE = 2;    // Google 登录
    int LOGIN_TYPE_FACEBOOK = 3;  // Facebook 登录
    int LOGIN_TYPE_APPLE = 4;     // Apple 登录
    int LOGIN_TYPE_TOKEN = 5;     // Token 自动登录

    // ==================== 账号登录/注册 ====================

    /**
     * 游客登录 (自动注册)
     *
     * @param deviceId 设备 ID
     * @param platform 平台
     * @param channel  渠道
     * @return 账号信息
     */
    Result<AccountDTO> guestLogin(String deviceId, String platform, String channel);

    /**
     * 账号密码登录
     *
     * @param account  账号
     * @param password 密码 (MD5)
     * @param deviceId 设备 ID
     * @return 账号信息
     */
    Result<AccountDTO> accountLogin(String account, String password, String deviceId);

    /**
     * 账号密码注册
     *
     * @param account  账号
     * @param password 密码 (MD5)
     * @param deviceId 设备 ID
     * @param email    邮箱 (可选)
     * @return 账号信息
     */
    Result<AccountDTO> accountRegister(String account, String password, String deviceId, String email);

    /**
     * 第三方平台登录 (Google/Facebook/Apple)
     *
     * @param loginType   登录类型
     * @param accessToken 第三方平台 Token
     * @param deviceId    设备 ID
     * @return 账号信息
     */
    Result<AccountDTO> thirdPartyLogin(int loginType, String accessToken, String deviceId);

    /**
     * Token 自动登录
     *
     * @param token    登录 Token
     * @param deviceId 设备 ID
     * @return 账号信息
     */
    Result<AccountDTO> tokenLogin(String token, String deviceId);

    // ==================== 账号绑定 ====================

    /**
     * 绑定账号密码 (游客绑定)
     *
     * @param accountId 账号 ID
     * @param account   账号
     * @param password  密码
     * @param email     邮箱
     * @return 操作结果
     */
    Result<Void> bindAccount(String accountId, String account, String password, String email);

    /**
     * 绑定第三方账号
     *
     * @param accountId   账号 ID
     * @param loginType   第三方类型
     * @param accessToken 第三方 Token
     * @return 操作结果
     */
    Result<Void> bindThirdParty(String accountId, int loginType, String accessToken);

    // ==================== Token 验证 ====================

    /**
     * 验证 Token
     *
     * @param token Token
     * @return 账号 ID
     */
    Result<String> verifyToken(String token);

    /**
     * 刷新 Token
     *
     * @param token 旧 Token
     * @return 新 Token
     */
    Result<String> refreshToken(String token);

    // ==================== 服务器列表 ====================

    /**
     * 获取服务器列表
     *
     * @param accountId 账号 ID
     * @return 服务器列表
     */
    Result<List<ServerDTO>> getServerList(String accountId);

    /**
     * 获取推荐服务器
     *
     * @param accountId 账号 ID
     * @return 推荐服务器
     */
    Result<ServerDTO> getRecommendedServer(String accountId);
}
