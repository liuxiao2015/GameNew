package com.game.service.gm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 配置
 * <p>
 * 访问地址：
 * <ul>
 *     <li>Swagger UI: http://localhost:8090/swagger-ui.html</li>
 *     <li>OpenAPI JSON: http://localhost:8090/v3/api-docs</li>
 *     <li>OpenAPI YAML: http://localhost:8090/v3/api-docs.yaml</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:service-gm}")
    private String applicationName;

    @Value("${server.port:8090}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // API 基本信息
                .info(new Info()
                        .title("游戏服务器 GM 后台 API")
                        .description("""
                                ## 游戏服务器运营管理后台 API 文档
                                
                                ### 功能模块
                                - **认证模块** - 登录、登出、Token 刷新
                                - **玩家管理** - 查询、封禁、解封、发放道具
                                - **公会管理** - 查询、解散、修改
                                - **邮件系统** - 发送邮件、群发邮件
                                - **配置管理** - 热更新配置、Hotfix 脚本
                                - **服务器管理** - 服务状态、公告管理
                                
                                ### 认证方式
                                所有接口（除登录外）需要在请求头中携带 Token：
                                ```
                                Authorization: Bearer <access_token>
                                ```
                                
                                ### 响应格式
                                ```json
                                {
                                    "code": 0,
                                    "message": "success",
                                    "data": {}
                                }
                                ```
                                
                                ### 错误码
                                - 0: 成功
                                - 1000-1999: 系统错误
                                - 2000-2999: 认证错误
                                - 3000-3999: 业务错误
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("GameServer Team")
                                .email("admin@gameserver.com")
                                .url("https://github.com/gameserver"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                // 服务器列表
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("本地开发环境"),
                        new Server()
                                .url("http://gm.gameserver.com")
                                .description("生产环境")))
                // 安全认证配置
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("在登录成功后获取的 access_token")))
                // 全局安全要求
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
