package com.game.service.pay.controller;

import com.game.entity.document.PayChannel;
import com.game.service.pay.service.PayCallbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调控制器
 *
 * @author GameServer
 */
@Slf4j
@RestController
@RequestMapping("/pay/callback")
@RequiredArgsConstructor
public class PayCallbackController {
    
    private final PayCallbackService callbackService;
    
    /**
     * 微信支付回调
     */
    @PostMapping(value = "/wechat", produces = MediaType.APPLICATION_XML_VALUE)
    public String wechatCallback(HttpServletRequest request) {
        log.info("收到微信支付回调");
        return handleCallback(PayChannel.WECHAT, request);
    }
    
    /**
     * 支付宝回调
     */
    @PostMapping(value = "/alipay", produces = MediaType.TEXT_PLAIN_VALUE)
    public String alipayCallback(HttpServletRequest request) {
        log.info("收到支付宝回调");
        return handleCallback(PayChannel.ALIPAY, request);
    }
    
    /**
     * 苹果内购验证
     */
    @PostMapping("/apple")
    public String appleCallback(HttpServletRequest request) {
        log.info("收到苹果内购回调");
        return handleCallback(PayChannel.APPLE_IAP, request);
    }
    
    /**
     * Google Play回调
     */
    @PostMapping("/google")
    public String googleCallback(HttpServletRequest request) {
        log.info("收到Google Play回调");
        return handleCallback(PayChannel.GOOGLE_PLAY, request);
    }
    
    /**
     * 模拟支付回调 (测试用)
     */
    @PostMapping("/mock")
    public String mockCallback(@RequestParam Map<String, String> params) {
        log.info("收到模拟支付回调: {}", params);
        return callbackService.handleCallback(PayChannel.MOCK, params, "", new HashMap<>());
    }
    
    /**
     * 处理回调
     */
    private String handleCallback(PayChannel channel, HttpServletRequest request) {
        try {
            Map<String, String> params = getParams(request);
            Map<String, String> headers = getHeaders(request);
            String body = getBody(request);
            
            log.debug("回调参数: channel={}, params={}", channel, params);
            
            return callbackService.handleCallback(channel, params, body, headers);
            
        } catch (Exception e) {
            log.error("处理支付回调异常: channel={}", channel, e);
            return "FAIL";
        }
    }
    
    /**
     * 获取请求参数
     */
    private Map<String, String> getParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        return params;
    }
    
    /**
     * 获取请求头
     */
    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
    
    /**
     * 获取请求body
     */
    private String getBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("读取请求body失败", e);
            return "";
        }
    }
}
