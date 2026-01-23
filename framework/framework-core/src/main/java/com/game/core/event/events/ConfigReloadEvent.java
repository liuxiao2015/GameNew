package com.game.core.event.events;

import com.game.core.event.BaseGameEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 配置重载事件
 * <p>
 * 用于通知所有服务实例重新加载配置
 * </p>
 *
 * @author GameServer
 */
@Getter
@NoArgsConstructor
public class ConfigReloadEvent extends BaseGameEvent {

    /**
     * 配置文件名 (null 表示全部重载)
     */
    private String configFile;

    /**
     * 配置版本号
     */
    private long version;

    /**
     * 操作者
     */
    private String operator;

    public ConfigReloadEvent(String configFile) {
        super();
        this.configFile = configFile;
        this.version = System.currentTimeMillis();
    }

    public ConfigReloadEvent(String configFile, String operator) {
        super();
        this.configFile = configFile;
        this.version = System.currentTimeMillis();
        this.operator = operator;
    }

    /**
     * 创建全量重载事件
     */
    public static ConfigReloadEvent reloadAll(String operator) {
        ConfigReloadEvent event = new ConfigReloadEvent(null, operator);
        return event;
    }

    /**
     * 判断是否全量重载
     */
    public boolean isReloadAll() {
        return configFile == null || configFile.isEmpty();
    }
}
