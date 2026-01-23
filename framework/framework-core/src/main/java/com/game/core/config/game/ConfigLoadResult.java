package com.game.core.config.game;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 配置加载结果
 *
 * @author GameServer
 */
@Data
@Builder
public class ConfigLoadResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 加载的配置文件
     */
    private String configFile;
    
    /**
     * 当前版本
     */
    private long version;
    
    /**
     * MD5 校验值
     */
    private String md5;
    
    /**
     * 配置数量
     */
    private int configCount;
    
    /**
     * 加载耗时 (ms)
     */
    private long costMs;
    
    /**
     * 验证错误列表
     */
    private List<String> validationErrors;
    
    /**
     * 创建成功结果
     */
    public static ConfigLoadResult success(String configFile, long version, String md5, int count, long costMs) {
        return ConfigLoadResult.builder()
                .success(true)
                .message("配置加载成功")
                .configFile(configFile)
                .version(version)
                .md5(md5)
                .configCount(count)
                .costMs(costMs)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static ConfigLoadResult fail(String configFile, String message) {
        return ConfigLoadResult.builder()
                .success(false)
                .message(message)
                .configFile(configFile)
                .build();
    }
    
    /**
     * 创建验证失败结果
     */
    public static ConfigLoadResult validationFail(String configFile, List<String> errors) {
        return ConfigLoadResult.builder()
                .success(false)
                .message("配置验证失败")
                .configFile(configFile)
                .validationErrors(errors)
                .build();
    }
}
