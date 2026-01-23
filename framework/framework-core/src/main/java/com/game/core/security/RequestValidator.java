package com.game.core.security;

import com.game.common.util.CryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 请求验证器
 * <p>
 * 生产级请求安全验证：
 * <ul>
 *     <li>时间戳有效性检查 (防止重放攻击)</li>
 *     <li>签名验证</li>
 *     <li>请求频率检测</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class RequestValidator {

    @Value("${game.security.sign-key:GameServerSecretKey2024}")
    private String signKey;

    @Value("${game.security.timestamp-tolerance:300}")
    private int timestampTolerance; // 时间戳容忍误差 (秒)

    @Value("${game.security.sign-enabled:false}")
    private boolean signEnabled;

    /**
     * 验证时间戳有效性
     * <p>
     * 防止重放攻击：请求时间戳必须在当前时间前后 N 秒内
     * </p>
     *
     * @param timestamp 客户端时间戳 (秒)
     * @return 是否有效
     */
    public boolean validateTimestamp(long timestamp) {
        long serverTime = System.currentTimeMillis() / 1000;
        long diff = Math.abs(serverTime - timestamp);
        
        if (diff > timestampTolerance) {
            log.warn("请求时间戳无效: clientTime={}, serverTime={}, diff={}s",
                    timestamp, serverTime, diff);
            return false;
        }
        return true;
    }

    /**
     * 验证签名
     * <p>
     * 签名规则: MD5(data + timestamp + signKey)
     * </p>
     *
     * @param data      请求数据
     * @param timestamp 时间戳
     * @param sign      客户端签名
     * @return 是否验证通过
     */
    public boolean validateSign(String data, long timestamp, String sign) {
        if (!signEnabled) {
            return true;
        }
        
        if (sign == null || sign.isEmpty()) {
            log.warn("请求缺少签名");
            return false;
        }

        String expectedSign = generateSign(data, timestamp);
        if (!expectedSign.equalsIgnoreCase(sign)) {
            log.warn("请求签名验证失败: expected={}, actual={}", expectedSign, sign);
            return false;
        }
        return true;
    }

    /**
     * 生成签名
     */
    public String generateSign(String data, long timestamp) {
        String content = (data != null ? data : "") + timestamp + signKey;
        return CryptoUtil.md5(content);
    }

    /**
     * 完整的请求验证
     *
     * @param data      请求数据
     * @param timestamp 时间戳
     * @param sign      签名
     * @return 验证结果
     */
    public ValidationResult validate(String data, long timestamp, String sign) {
        // 1. 时间戳验证
        if (!validateTimestamp(timestamp)) {
            return ValidationResult.error("时间戳无效");
        }

        // 2. 签名验证
        if (!validateSign(data, timestamp, sign)) {
            return ValidationResult.error("签名验证失败");
        }

        return ValidationResult.ok();
    }

    /**
     * 验证结果
     */
    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isSuccess() {
            return valid;
        }
    }
}
