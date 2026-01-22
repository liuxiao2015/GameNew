package com.game.data.mongo;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MongoDB 文档基类
 * <p>
 * 所有 MongoDB 文档实体都应继承此类
 * </p>
 *
 * @author GameServer
 */
@Data
public abstract class BaseDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文档 ID
     */
    @Id
    private String id;

    /**
     * 创建时间
     */
    @CreatedDate
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @LastModifiedDate
    private LocalDateTime updateTime;

    /**
     * 版本号 (乐观锁)
     */
    @Version
    private Long version;
}
