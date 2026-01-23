package com.game.core.config.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置版本信息
 * <p>
 * 用于追踪配置的版本历史，支持回滚
 * </p>
 *
 * @author GameServer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigVersion {
    
    /**
     * 配置文件名
     */
    private String configFile;
    
    /**
     * 版本号
     */
    private long version;
    
    /**
     * MD5 校验值
     */
    private String md5;
    
    /**
     * 配置内容 (用于回滚)
     */
    private String content;
    
    /**
     * 加载时间
     */
    private long loadTime;
    
    /**
     * 操作者
     */
    private String operator;
    
    /**
     * 来源 (local/remote/backup)
     */
    private String source;
    
    /**
     * 配置数量
     */
    private int configCount;
}
