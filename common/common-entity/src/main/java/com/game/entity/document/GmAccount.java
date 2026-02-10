package com.game.entity.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

/**
 * GM 账号实体
 *
 * @author GameServer
 */
@Data
@Document(collection = "gm_account")
public class GmAccount {

    /**
     * 账号 ID
     */
    @Id
    private String id;

    /**
     * 用户名
     */
    @Indexed(unique = true)
    private String username;

    /**
     * 密码 (加密存储)
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 角色 (SUPER_ADMIN, ADMIN, OPERATOR)
     */
    private String role;

    /**
     * 权限列表
     */
    private Set<String> permissions;

    /**
     * 状态 (0:禁用 1:启用)
     */
    private int status = 1;

    /**
     * 最后登录时间
     */
    private long lastLoginTime;

    /**
     * 最后登录 IP
     */
    private String lastLoginIp;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 更新时间
     */
    private long updateTime;
}
