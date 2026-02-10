package com.game.service.gm.aspect;

import com.game.common.util.JsonUtil;
import com.game.service.gm.annotation.GmLog;
import com.game.entity.document.GmAccount;
import com.game.entity.document.GmOperationLog;
import com.game.entity.repository.GmOperationLogRepository;
import com.game.service.gm.service.GmAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * GM 操作日志切面
 *
 * @author GameServer
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class GmLogAspect {

    private final GmOperationLogRepository logRepository;
    private final GmAuthService authService;

    @Around("@annotation(gmLog)")
    public Object around(ProceedingJoinPoint joinPoint, GmLog gmLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        GmOperationLog operationLog = new GmOperationLog();
        operationLog.setId(UUID.randomUUID().toString());
        operationLog.setModule(gmLog.module());
        operationLog.setOperationType(gmLog.operation());
        operationLog.setOperateTime(startTime);

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            operationLog.setOperateIp(getClientIp(request));

            // 获取操作者信息
            String token = request.getHeader("Authorization");
            GmAccount account = authService.validateToken(token);
            if (account != null) {
                operationLog.setOperator(account.getUsername());
            }
        }

        // 记录请求参数
        if (gmLog.logParams()) {
            try {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                String[] paramNames = signature.getParameterNames();
                Object[] args = joinPoint.getArgs();
                
                StringBuilder params = new StringBuilder("{");
                for (int i = 0; i < paramNames.length; i++) {
                    if (i > 0) {
                        params.append(", ");
                    }
                    params.append(paramNames[i]).append(": ").append(args[i]);
                }
                params.append("}");
                operationLog.setRequestParams(params.toString());

                // 提取目标角色 ID
                for (int i = 0; i < paramNames.length; i++) {
                    if ("roleId".equals(paramNames[i]) && args[i] instanceof Long) {
                        operationLog.setTargetRoleId((Long) args[i]);
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("记录请求参数异常", e);
            }
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            operationLog.setStatus(1);

            // 记录响应结果
            if (gmLog.logResult() && result != null) {
                operationLog.setResponseResult(JsonUtil.toJson(result));
            }

            return result;
        } catch (Throwable e) {
            operationLog.setStatus(0);
            operationLog.setRemark(e.getMessage());
            throw e;
        } finally {
            // 异步保存日志
            try {
                logRepository.save(operationLog);
            } catch (Exception e) {
                log.error("保存 GM 操作日志失败", e);
            }

            long costTime = System.currentTimeMillis() - startTime;
            log.info("GM 操作: operator={}, module={}, operation={}, status={}, costTime={}ms",
                operationLog.getOperator(), gmLog.module(), gmLog.operation(),
                operationLog.getStatus(), costTime);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
