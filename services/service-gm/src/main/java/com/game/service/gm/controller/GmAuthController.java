package com.game.service.gm.controller;

import com.game.common.result.Result;
import com.game.service.gm.dto.LoginRequest;
import com.game.service.gm.dto.LoginResponse;
import com.game.service.gm.service.GmAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * GM 认证控制器
 *
 * @author GameServer
 */
@Tag(name = "认证管理", description = "GM 后台登录、登出、Token 刷新")
@RestController
@RequestMapping("/gm/auth")
@RequiredArgsConstructor
public class GmAuthController {

    private final GmAuthService authService;

    @Operation(
            summary = "登录",
            description = "使用用户名和密码登录 GM 后台，成功后返回 access_token 和 refresh_token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "403", description = "账号已禁用")
    })
    @PostMapping("/login")
    public Result<LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "登录请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class))
            )
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        return authService.login(request.getUsername(), request.getPassword(), ip);
    }

    @Operation(
            summary = "登出",
            description = "注销当前登录的 Token，使其失效"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登出成功")
    })
    @PostMapping("/logout")
    public Result<Void> logout(
            @Parameter(description = "Bearer Token", required = true, example = "Bearer xxx")
            @RequestHeader("Authorization") String token) {
        return authService.logout(token);
    }

    @Operation(
            summary = "刷新令牌",
            description = "使用 refresh_token 获取新的 access_token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "刷新成功",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token 无效或已过期")
    })
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(
            @Parameter(description = "Refresh Token", required = true)
            @RequestHeader("Authorization") String token) {
        return authService.refreshToken(token);
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
