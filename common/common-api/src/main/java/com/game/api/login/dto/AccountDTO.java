package com.game.api.login.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 账号信息 DTO
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账号 ID
     */
    private String accountId;

    /**
     * 登录 Token
     */
    private String token;

    /**
     * Token 过期时间
     */
    private long tokenExpireTime;

    /**
     * 是否新账号
     */
    private boolean newAccount;

    /**
     * 登录类型
     */
    private int loginType;

    /**
     * 已绑定的第三方账号列表
     */
    private List<String> bindPlatforms;

    /**
     * 最后登录服务器 ID
     */
    private int lastLoginServerId;
}
