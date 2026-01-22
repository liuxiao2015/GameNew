package com.game.service.login.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * 账号实体
 *
 * @author GameServer
 */
@Data
@Document(collection = "account")
public class Account {

    /**
     * 账号 ID
     */
    @Id
    private String accountId;

    /**
     * 平台类型 (1:自有 2:微信 3:QQ 4:Apple)
     */
    private int platformType;

    /**
     * 平台用户 ID
     */
    @Indexed(unique = true)
    private String platformUserId;

    /**
     * 用户名 (自有平台)
     */
    @Indexed(unique = true, sparse = true)
    private String username;

    /**
     * 密码 (自有平台, 加密存储)
     */
    private String password;

    /**
     * 手机号
     */
    @Indexed(sparse = true)
    private String phone;

    /**
     * 邮箱
     */
    @Indexed(sparse = true)
    private String email;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * 账号状态 (0:禁用 1:正常)
     */
    private int status = 1;

    /**
     * 封禁结束时间 (0 表示永久)
     */
    private long banEndTime;

    /**
     * 封禁原因
     */
    private String banReason;

    /**
     * 角色 ID 列表
     */
    private List<Long> roleIds;

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

    /**
     * 是否被封禁
     */
    public boolean isBanned() {
        if (status == 0) {
            return true;
        }
        if (banEndTime == 0) {
            return false;
        }
        if (banEndTime == -1) {
            return true; // 永久封禁
        }
        return System.currentTimeMillis() < banEndTime;
    }
}
