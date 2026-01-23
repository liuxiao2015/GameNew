package com.game.service.pay.channel;

import com.game.service.pay.enums.PayChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付渠道工厂
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayChannelFactory {
    
    private final List<PayChannelAdapter> adapters;
    
    private final Map<PayChannel, PayChannelAdapter> adapterMap = new HashMap<>();
    
    @PostConstruct
    public void init() {
        for (PayChannelAdapter adapter : adapters) {
            adapterMap.put(adapter.getChannel(), adapter);
            log.info("注册支付渠道: {} -> {} (enabled={})", 
                    adapter.getChannel(), adapter.getClass().getSimpleName(), adapter.isEnabled());
        }
    }
    
    /**
     * 获取支付渠道适配器
     */
    public PayChannelAdapter getAdapter(PayChannel channel) {
        PayChannelAdapter adapter = adapterMap.get(channel);
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的支付渠道: " + channel);
        }
        if (!adapter.isEnabled()) {
            throw new IllegalStateException("支付渠道未启用: " + channel);
        }
        return adapter;
    }
    
    /**
     * 获取支付渠道适配器 (不检查是否启用)
     */
    public PayChannelAdapter getAdapterUnchecked(PayChannel channel) {
        return adapterMap.get(channel);
    }
    
    /**
     * 检查渠道是否可用
     */
    public boolean isChannelAvailable(PayChannel channel) {
        PayChannelAdapter adapter = adapterMap.get(channel);
        return adapter != null && adapter.isEnabled();
    }
}
