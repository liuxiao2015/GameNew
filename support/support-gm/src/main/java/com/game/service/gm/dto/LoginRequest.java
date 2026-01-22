package com.game.service.gm.dto;

import lombok.Data;

/**
 * 登录请求
 *
 * @author GameServer
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
