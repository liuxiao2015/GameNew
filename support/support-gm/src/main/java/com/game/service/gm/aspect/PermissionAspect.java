package com.game.service.gm.aspect;

import com.game.common.enums.ErrorCode;
import com.game.common.exception.GameException;
import com.game.service.gm.annotation.RequirePermission;
import com.game.service.gm.entity.GmAccount;
import com.game.service.gm.service.GmAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限验证切面
 *
 * @author GameServer
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final GmAuthService authService;

    @Around("@annotation(requirePermission)")
    public Object around(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        // 获取请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new GameException(ErrorCode.UNAUTHORIZED);
        }

        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader("Authorization");

        // 验证 Token
        GmAccount account = authService.validateToken(token);
        if (account == null) {
            throw new GameException(ErrorCode.UNAUTHORIZED);
        }

        // 超级管理员跳过权限检查
        if ("SUPER_ADMIN".equals(account.getRole())) {
            return joinPoint.proceed();
        }

        // 检查权限
        String requiredPermission = requirePermission.value();
        if (account.getPermissions() == null || !account.getPermissions().contains(requiredPermission)) {
            log.warn("权限不足: username={}, required={}", account.getUsername(), requiredPermission);
            throw new GameException(ErrorCode.FORBIDDEN);
        }

        return joinPoint.proceed();
    }
}
