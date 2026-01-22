package com.game.core.config;

import com.game.core.net.session.SessionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 核心模块自动配置
 *
 * @author GameServer
 */
@Configuration
@ComponentScan("com.game.core")
public class CoreAutoConfiguration {

    @Bean
    public SessionManager sessionManager() {
        return new SessionManager();
    }
}
