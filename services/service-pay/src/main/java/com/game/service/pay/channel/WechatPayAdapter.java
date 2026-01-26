package com.game.service.pay.channel;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.game.api.pay.OrderDTO;
import com.game.api.pay.PayCallbackResult;
import com.game.entity.document.PayChannel;
import com.game.entity.document.PayOrder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 微信支付适配器
 *
 * @author GameServer
 */
@Slf4j
@Component
public class WechatPayAdapter extends AbstractPayChannelAdapter {
    
    @Value("${pay.wechat.enabled:false}")
    private boolean enabled;
    
    @Value("${pay.wechat.app-id:}")
    private String appId;
    
    @Value("${pay.wechat.mch-id:}")
    private String mchId;
    
    @Value("${pay.wechat.api-key:}")
    private String apiKey;
    
    @Value("${pay.wechat.notify-url:}")
    private String notifyUrl;
    
    private final XmlMapper xmlMapper = new XmlMapper();
    
    @PostConstruct
    public void init() {
        if (enabled) {
            log.info("微信支付适配器已启用: appId={}, mchId={}", appId, mchId);
        }
    }
    
    @Override
    public PayChannel getChannel() {
        return PayChannel.WECHAT;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public OrderDTO.PayParams createPayRequest(PayOrder order) {
        // TODO: 实际接入时需要调用微信统一下单接口
        // 这里是示例代码
        
        Map<String, String> params = new TreeMap<>();
        params.put("appid", appId);
        params.put("mch_id", mchId);
        params.put("nonce_str", UUID.randomUUID().toString().replace("-", ""));
        params.put("body", order.getProductName());
        params.put("out_trade_no", order.getOrderId());
        params.put("total_fee", order.getAmount().multiply(new java.math.BigDecimal("100")).intValue() + "");
        params.put("spbill_create_ip", order.getClientIp());
        params.put("notify_url", notifyUrl);
        params.put("trade_type", "APP");
        
        String sign = generateSign(params);
        params.put("sign", sign);
        
        // 实际应调用 https://api.mch.weixin.qq.com/pay/unifiedorder
        // 获取 prepay_id
        String prepayId = "wx_prepay_" + System.currentTimeMillis();
        
        // 构造APP支付参数
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        
        Map<String, String> appParams = new TreeMap<>();
        appParams.put("appid", appId);
        appParams.put("partnerid", mchId);
        appParams.put("prepayid", prepayId);
        appParams.put("package", "Sign=WXPay");
        appParams.put("noncestr", nonceStr);
        appParams.put("timestamp", timestamp);
        
        String paySign = generateSign(appParams);
        
        return OrderDTO.PayParams.builder()
                .prepayId(prepayId)
                .timestamp(timestamp)
                .nonceStr(nonceStr)
                .sign(paySign)
                .build();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public PayCallbackResult parseCallback(Map<String, String> params, String body, Map<String, String> headers) {
        try {
            Map<String, String> xmlParams = xmlMapper.readValue(body, Map.class);
            
            String returnCode = xmlParams.get("return_code");
            String resultCode = xmlParams.get("result_code");
            
            if (!"SUCCESS".equals(returnCode) || !"SUCCESS".equals(resultCode)) {
                return PayCallbackResult.fail(xmlParams.get("return_msg"));
            }
            
            if (!verifySignature(xmlParams, body)) {
                return PayCallbackResult.fail("签名验证失败");
            }
            
            String orderId = xmlParams.get("out_trade_no");
            String tradeNo = xmlParams.get("transaction_id");
            int totalFee = Integer.parseInt(xmlParams.get("total_fee"));
            
            return PayCallbackResult.builder()
                    .success(true)
                    .orderId(orderId)
                    .tradeNo(tradeNo)
                    .amount(new java.math.BigDecimal(totalFee).divide(new java.math.BigDecimal("100")))
                    .rawData(body)
                    .build();
            
        } catch (Exception e) {
            log.error("解析微信支付回调失败", e);
            return PayCallbackResult.fail("解析回调失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean verifySignature(Map<String, String> params, String body) {
        String sign = params.get("sign");
        if (sign == null) {
            return false;
        }
        
        Map<String, String> checkParams = new TreeMap<>(params);
        checkParams.remove("sign");
        
        String expectedSign = generateSign(checkParams);
        return sign.equals(expectedSign);
    }
    
    @Override
    protected String generateSign(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        sb.append("key=").append(apiKey);
        return DigestUtils.md5Hex(sb.toString()).toUpperCase();
    }
    
    @Override
    public String successResponse() {
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
    }
    
    @Override
    public String failResponse(String message) {
        return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[" + message + "]]></return_msg></xml>";
    }
}
