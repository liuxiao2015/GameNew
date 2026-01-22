package com.game.service.game.config;

import com.game.common.cross.CrossServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 跨服配置自动装配
 *
 * @author GameServer
 */
@Configuration
public class CrossServerAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "cross-server")
    public CrossServerConfig crossServerConfig() {
        return new CrossServerConfig();
    }
}
