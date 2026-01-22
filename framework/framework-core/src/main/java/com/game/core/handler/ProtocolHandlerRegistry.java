package com.game.core.handler;

import com.game.core.handler.annotation.Protocol;
import com.game.core.handler.annotation.ProtocolController;
import com.google.protobuf.Message;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一协议处理器注册中心
 * <p>
 * 自动扫描并注册所有带 @ProtocolController 和 @Protocol 注解的处理器。
 * 同时兼容旧的 @ProtocolMapping 注解。
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class ProtocolHandlerRegistry {

    private final ApplicationContext applicationContext;

    /**
     * 协议映射表 (protocolKey -> ProtocolMethod)
     */
    private final Map<Integer, ProtocolMethod> protocolMethods = new ConcurrentHashMap<>();

    /**
     * 协议名称映射 (用于日志)
     */
    private final Map<Integer, String> protocolNames = new ConcurrentHashMap<>();

    public ProtocolHandlerRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        log.info("开始扫描协议处理器...");
        scanProtocolControllers();
        scanLegacyProtocolHandlers();
        log.info("协议处理器扫描完成，共注册 {} 个协议", protocolMethods.size());

        // 打印所有注册的协议
        if (log.isDebugEnabled()) {
            protocolMethods.values().forEach(pm -> log.debug("注册协议: {}", pm));
        }
    }

    /**
     * 扫描 @ProtocolController 注解的控制器
     */
    private void scanProtocolControllers() {
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(ProtocolController.class);
        for (Object controller : controllers.values()) {
            registerController(controller);
        }
    }

    /**
     * 扫描旧的 @Protocol 注解 (不在 @ProtocolController 中的)
     */
    private void scanLegacyProtocolHandlers() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        for (Object bean : beans.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            // 跳过已经处理过的 @ProtocolController
            if (targetClass.isAnnotationPresent(ProtocolController.class)) {
                continue;
            }
            // 扫描带 @Protocol 注解的方法
            for (Method method : targetClass.getDeclaredMethods()) {
                Protocol protocol = method.getAnnotation(Protocol.class);
                if (protocol != null && protocol.value() > 0) {
                    registerMethod(bean, method, protocol, 0);
                }
            }
        }
    }

    /**
     * 注册单个控制器
     */
    private void registerController(Object controller) {
        Class<?> targetClass = AopUtils.getTargetClass(controller);
        ProtocolController controllerAnnotation = targetClass.getAnnotation(ProtocolController.class);

        if (controllerAnnotation == null) {
            return;
        }

        int moduleId = controllerAnnotation.moduleId();
        String moduleName = controllerAnnotation.value();

        log.debug("注册协议控制器: {} (moduleId=0x{}, name={})",
                targetClass.getSimpleName(), Integer.toHexString(moduleId), moduleName);

        // 扫描所有方法
        for (Method method : targetClass.getDeclaredMethods()) {
            // 优先使用新的 @Protocol 注解
            Protocol protocol = method.getAnnotation(Protocol.class);
            if (protocol != null) {
                registerMethod(controller, method, protocol, moduleId);
            }
        }
    }

    /**
     * 注册单个处理方法
     */
    private void registerMethod(Object handler, Method method, Protocol protocol, int moduleId) {
        // 获取请求参数类型
        Class<?> requestType = getRequestType(method);

        // 创建协议方法
        ProtocolMethod protocolMethod = new ProtocolMethod(handler, method, protocol, moduleId, requestType);

        // 检查重复注册
        if (protocolMethods.containsKey(protocolMethod.getProtocolKey())) {
            ProtocolMethod existing = protocolMethods.get(protocolMethod.getProtocolKey());
            log.error("协议重复注册! key={}, existing={}, new={}",
                    protocolMethod.getProtocolKey(), existing, protocolMethod);
            throw new IllegalStateException("协议重复注册: " + protocolMethod.getProtocolKey());
        }

        // 注册
        protocolMethods.put(protocolMethod.getProtocolKey(), protocolMethod);
        protocolNames.put(protocolMethod.getProtocolKey(), protocolMethod.getDescription());

        log.debug("  -> 注册协议方法: {}", protocolMethod);
    }

    /**
     * 获取请求参数类型
     */
    private Class<?> getRequestType(Method method) {
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            Class<?> type = parameter.getType();
            // 找到第一个 Protobuf 消息类型
            if (Message.class.isAssignableFrom(type)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取协议处理方法
     */
    public ProtocolMethod getProtocolMethod(int protocolKey) {
        return protocolMethods.get(protocolKey);
    }

    /**
     * 获取协议处理方法
     */
    public ProtocolMethod getProtocolMethod(int moduleId, int methodId) {
        int protocolKey = (moduleId << 8) | methodId;
        return protocolMethods.get(protocolKey);
    }

    /**
     * 获取协议名称
     */
    public String getProtocolName(int protocolKey) {
        return protocolNames.getOrDefault(protocolKey, "Unknown");
    }

    /**
     * 获取所有注册的协议
     */
    public Collection<ProtocolMethod> getAllProtocols() {
        return protocolMethods.values();
    }

    /**
     * 检查协议是否已注册
     */
    public boolean hasProtocol(int protocolKey) {
        return protocolMethods.containsKey(protocolKey);
    }

    /**
     * 获取注册的协议数量
     */
    public int getProtocolCount() {
        return protocolMethods.size();
    }

    /**
     * 打印协议统计信息
     */
    public void printStats() {
        log.info("========== 协议统计 ==========");
        protocolMethods.values().stream()
                .filter(pm -> pm.getTotalCount().get() > 0)
                .sorted((a, b) -> Long.compare(b.getTotalCount().get(), a.getTotalCount().get()))
                .forEach(pm -> log.info("{}: {}", pm.getName(), pm.getStats()));
        log.info("==============================");
    }
}
