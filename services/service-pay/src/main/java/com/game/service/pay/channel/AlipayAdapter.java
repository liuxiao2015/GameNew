package com.game.service.pay.channel;

import com.game.api.pay.OrderDTO;
import com.game.api.pay.PayCallbackResult;
import com.game.entity.document.PayChannel;
import com.game.entity.document.PayOrder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 支付宝适配器
 *
 * @author GameServer
 */
@Slf4j
@Component
public class AlipayAdapter extends AbstractPayChannelAdapter {
    
    @Value("${pay.alipay.enabled:false}")
    private boolean enabled;
    
    @Value("${pay.alipay.app-id:}")
    private String appId;
    
    @Value("${pay.alipay.private-key:}")
    private String privateKey;
    
    @Value("${pay.alipay.public-key:}")
    private String publicKey;
    
    @Value("${pay.alipay.notify-url:}")
    private String notifyUrl;
    
    @PostConstruct
    public void init() {
        if (enabled) {
            log.info("支付宝适配器已启用: appId={}", appId);
        }
    }
    
    @Override
    public PayChannel getChannel() {
        return PayChannel.ALIPAY;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public OrderDTO.PayParams createPayRequest(PayOrder order) {
        // TODO: 实际接入时需要使用支付宝SDK
        // 这里是示例代码
        
        Map<String, String> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", order.getOrderId());
        bizContent.put("total_amount", order.getAmount().toString());
        bizContent.put("subject", order.getProductName());
        bizContent.put("product_code", "QUICK_MSECURITY_PAY");
        
        Map<String, String> params = new TreeMap<>();
        params.put("app_id", appId);
        params.put("method", "alipay.trade.app.pay");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", java.time.LocalDateTime.now().toString());
        params.put("version", "1.0");
        params.put("notify_url", notifyUrl);
        params.put("biz_content", toJson(bizContent));
        
        String sign = generateSign(params);
        params.put("sign", sign);
        
        // 构造 orderString
        StringBuilder orderString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            orderString.append(entry.getKey()).append("=")
                    .append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .append("&");
        }
        orderString.deleteCharAt(orderString.length() - 1);
        
        return OrderDTO.PayParams.builder()
                .orderString(orderString.toString())
                .build();
    }
    
    @Override
    public PayCallbackResult parseCallback(Map<String, String> params, String body, Map<String, String> headers) {
        try {
            // 解析 form 表单参数
            Map<String, String> callbackParams = parseFormParams(body);
            
            String tradeStatus = callbackParams.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                return PayCallbackResult.fail("交易未成功: " + tradeStatus);
            }
            
            if (!verifySignature(callbackParams, body)) {
                return PayCallbackResult.fail("签名验证失败");
            }
            
            String orderId = callbackParams.get("out_trade_no");
            String tradeNo = callbackParams.get("trade_no");
            BigDecimal amount = new BigDecimal(callbackParams.get("total_amount"));
            
            return PayCallbackResult.builder()
                    .success(true)
                    .orderId(orderId)
                    .tradeNo(tradeNo)
                    .amount(amount)
                    .rawData(body)
                    .build();
            
        } catch (Exception e) {
            log.error("解析支付宝回调失败", e);
            return PayCallbackResult.fail("解析回调失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean verifySignature(Map<String, String> params, String body) {
        // TODO: 实际需要使用RSA2验签
        // 这里简化处理
        String sign = params.get("sign");
        return sign != null && !sign.isEmpty();
    }
    
    @Override
    protected String generateSign(Map<String, String> params) {
        // TODO: 实际需要使用RSA2签名
        // 这里使用MD5简化
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty() && !"sign".equals(entry.getKey())) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return DigestUtils.md5Hex(sb.toString() + privateKey);
    }
    
    private Map<String, String> parseFormParams(String body) {
        Map<String, String> params = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return params;
        }
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }
    
    private String toJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append("\"").append(entry.getKey()).append("\":\"")
                    .append(entry.getValue()).append("\",");
        }
        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public String successResponse() {
        return "success";
    }
    
    @Override
    public String failResponse(String message) {
        return "fail";
    }
}
