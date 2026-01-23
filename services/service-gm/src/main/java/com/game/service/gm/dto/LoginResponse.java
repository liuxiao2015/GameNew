package com.game.service.gm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * 登录响应
 *
 * @author GameServer
 */
@Data
@Builder
public class LoginResponse {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 过期时间 (秒)
     */
    private long expiresIn;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 角色
     */
    private String role;

    /**
     * 权限列表
     */
    private Set<String> permissions;
}
