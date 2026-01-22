package com.game.support.gm.hotfix;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 热修复脚本实体
 *
 * @author GameServer
 */
@Data
@Document(collection = "hotfix_script")
public class HotfixScript {

    /**
     * 脚本 ID
     */
    @Id
    private String scriptId;

    /**
     * 脚本名称
     */
    private String scriptName;

    /**
     * 脚本描述
     */
    private String description;

    /**
     * Groovy 脚本内容
     */
    private String scriptContent;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
