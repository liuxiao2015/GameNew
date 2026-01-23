package com.game.service.pay.channel;

import com.game.api.pay.OrderDTO;
import com.game.api.pay.PayCallbackResult;
import com.game.service.pay.entity.PayOrder;
import com.game.service.pay.enums.PayChannel;

import java.util.Map;

/**
 * 支付渠道适配器接口
 *
 * @author GameServer
 */
public interface PayChannelAdapter {
    
    /**
     * 获取支付渠道
     */
    PayChannel getChannel();
    
    /**
     * 是否启用
     */
    boolean isEnabled();
    
    /**
     * 创建支付请求
     *
     * @param order 订单
     * @return 支付参数
     */
    OrderDTO.PayParams createPayRequest(PayOrder order);
    
    /**
     * 解析支付回调
     *
     * @param params  回调参数
     * @param body    回调body
     * @param headers 请求头
     * @return 回调结果
     */
    PayCallbackResult parseCallback(Map<String, String> params, String body, Map<String, String> headers);
    
    /**
     * 验证回调签名
     *
     * @param params 回调参数
     * @param body   回调body
     * @return 是否验证通过
     */
    boolean verifySignature(Map<String, String> params, String body);
    
    /**
     * 查询订单状态
     *
     * @param order 订单
     * @return 支付结果
     */
    PayCallbackResult queryOrder(PayOrder order);
    
    /**
     * 发起退款
     *
     * @param order  订单
     * @param reason 退款原因
     * @return 是否成功
     */
    boolean refund(PayOrder order, String reason);
    
    /**
     * 生成回调成功响应
     */
    String successResponse();
    
    /**
     * 生成回调失败响应
     */
    String failResponse(String message);
}
