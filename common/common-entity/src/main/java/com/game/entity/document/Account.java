package com.game.entity.document;

import com.game.data.mongo.BaseDocument;
import com.game.data.mongo.index.MongoIndex;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 账号数据 MongoDB 文档
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "account")
public class Account extends BaseDocument {

    /**
     * 账号 ID
     */
    @MongoIndex(unique = true)
    private long accountId;

    /**
     * 用户名
     */
    @MongoIndex(unique = true)
    private String username;

    /**
     * 密码 (加密后)
     */
    private String password;

    /**
     * 邮箱
     */
    @MongoIndex(sparse = true)
    private String email;

    /**
     * 手机号
     */
    @MongoIndex(sparse = true)
    private String phone;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * 注册 IP
     */
    private String registerIp;

    /**
     * 注册时间
     */
    private long registerTime;

    /**
     * 最后登录时间
     */
    private long lastLoginTime;

    /**
     * 最后登录 IP
     */
    private String lastLoginIp;

    /**
     * 状态 (0:正常 1:封禁)
     */
    private int status = 0;

    /**
     * 封禁结束时间
     */
    private long banEndTime;

    /**
     * 封禁原因
     */
    private String banReason;
}
