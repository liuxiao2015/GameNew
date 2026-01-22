package com.game.api.login;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 登录结果数据传输对象
 *
 * @author GameServer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 账号 ID
     */
    private long accountId;

    /**
     * 登录 Token
     */
    private String token;

    /**
     * Token 过期时间
     */
    private long expireTime;

    /**
     * 角色列表
     */
    private List<RoleBriefDTO> roles;

    /**
     * 服务器时间
     */
    private long serverTime;
}
