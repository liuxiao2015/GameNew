package com.game.service.pay.channel;

import com.game.api.pay.OrderDTO;
import com.game.api.pay.PayCallbackResult;
import com.game.service.pay.entity.PayOrder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 支付渠道适配器抽象基类
 *
 * @author GameServer
 */
@Slf4j
public abstract class AbstractPayChannelAdapter implements PayChannelAdapter {
    
    @Override
    public PayCallbackResult queryOrder(PayOrder order) {
        log.warn("渠道[{}]未实现订单查询功能", getChannel());
        return PayCallbackResult.fail("Not implemented");
    }
    
    @Override
    public boolean refund(PayOrder order, String reason) {
        log.warn("渠道[{}]未实现退款功能", getChannel());
        return false;
    }
    
    /**
     * 生成签名
     */
    protected abstract String generateSign(Map<String, String> params);
}
