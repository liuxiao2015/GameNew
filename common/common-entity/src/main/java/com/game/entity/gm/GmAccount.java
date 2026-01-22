package com.game.entity.gm;

import com.game.data.mongo.BaseDocument;
import com.game.data.mongo.index.MongoIndex;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * GM 账号 MongoDB 文档
 *
 * @author GameServer
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "gm_account")
public class GmAccount extends BaseDocument {

    /**
     * GM 账号名
     */
    @MongoIndex(unique = true)
    private String username;

    /**
     * 密码 (加密后)
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 角色 (admin/operator/viewer)
     */
    private String role;

    /**
     * 权限列表
     */
    private List<String> permissions = new ArrayList<>();

    /**
     * 状态 (0:正常 1:禁用)
     */
    private int status = 0;

    /**
     * 最后登录时间
     */
    private long lastLoginTime;

    /**
     * 最后登录 IP
     */
    private String lastLoginIp;
}
