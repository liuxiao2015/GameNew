package com.game.service.gm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * 登录响应
 *
 * @author GameServer
 */
@Data
@Builder
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "访问令牌，用于 API 请求认证", example = "abc123def456...")
    private String accessToken;

    @Schema(description = "刷新令牌，用于获取新的访问令牌", example = "xyz789...")
    private String refreshToken;

    @Schema(description = "访问令牌过期时间（秒）", example = "28800")
    private long expiresIn;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "昵称", example = "管理员")
    private String nickname;

    @Schema(description = "角色", example = "SUPER_ADMIN", allowableValues = {"SUPER_ADMIN", "ADMIN", "OPERATOR"})
    private String role;

    @Schema(description = "权限列表", example = "[\"player:view\", \"player:edit\", \"mail:send\"]")
    private Set<String> permissions;
}
