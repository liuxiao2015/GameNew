package com.game.service.activity.handler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动处理器工厂
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityHandlerFactory {

    private final List<ActivityHandler> handlers;
    private final Map<String, ActivityHandler> handlerMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (ActivityHandler handler : handlers) {
            handlerMap.put(handler.getTemplateId(), handler);
            log.info("注册活动处理器: templateId={} -> {}", 
                    handler.getTemplateId(), handler.getClass().getSimpleName());
        }
    }

    /**
     * 获取活动处理器
     */
    public ActivityHandler getHandler(String templateId) {
        ActivityHandler handler = handlerMap.get(templateId);
        if (handler == null) {
            log.warn("未找到活动处理器: templateId={}", templateId);
        }
        return handler;
    }

    /**
     * 检查处理器是否存在
     */
    public boolean hasHandler(String templateId) {
        return handlerMap.containsKey(templateId);
    }
}
