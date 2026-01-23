package com.game.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 安全过滤器
 * <p>
 * 生产级安全防护：
 * <ul>
 *     <li>IP 黑名单</li>
 *     <li>敏感词过滤</li>
 *     <li>XSS 防护</li>
 *     <li>输入长度限制</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
public class SecurityFilter {

    /**
     * IP 黑名单
     */
    private final Set<String> ipBlacklist = ConcurrentHashMap.newKeySet();

    /**
     * IP 白名单 (优先级高于黑名单)
     */
    private final Set<String> ipWhitelist = ConcurrentHashMap.newKeySet();

    /**
     * 敏感词列表
     */
    private final Set<String> sensitiveWords = ConcurrentHashMap.newKeySet();

    /**
     * XSS 过滤正则
     */
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<script[^>]*>.*?</script>|<[^>]+on\\w+\\s*=|javascript:|vbscript:|expression\\s*\\(",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    /**
     * SQL 注入检测正则
     */
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "('|\")|(--)|(;)|(%27)|(%22)|(\\bunion\\b)|(\\bselect\\b)|(\\binsert\\b)|(\\bdelete\\b)|(\\bdrop\\b)",
            Pattern.CASE_INSENSITIVE
    );

    @Value("${game.security.max-input-length:1000}")
    private int maxInputLength;

    // ==================== IP 过滤 ====================

    /**
     * 检查 IP 是否被封禁
     */
    public boolean isIpBlocked(String ip) {
        if (ip == null || ip.isEmpty()) {
            return true;
        }
        // 白名单优先
        if (ipWhitelist.contains(ip)) {
            return false;
        }
        return ipBlacklist.contains(ip);
    }

    /**
     * 添加 IP 到黑名单
     */
    public void blockIp(String ip, String reason) {
        ipBlacklist.add(ip);
        log.warn("IP 加入黑名单: ip={}, reason={}", ip, reason);
    }

    /**
     * 从黑名单移除 IP
     */
    public void unblockIp(String ip) {
        ipBlacklist.remove(ip);
        log.info("IP 从黑名单移除: ip={}", ip);
    }

    /**
     * 添加 IP 到白名单
     */
    public void addToWhitelist(String ip) {
        ipWhitelist.add(ip);
    }

    // ==================== 内容过滤 ====================

    /**
     * 过滤 XSS
     */
    public String filterXss(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return XSS_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * 检测 SQL 注入
     */
    public boolean hasSqlInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * 过滤敏感词
     */
    public String filterSensitiveWords(String input) {
        if (input == null || input.isEmpty() || sensitiveWords.isEmpty()) {
            return input;
        }
        String result = input;
        for (String word : sensitiveWords) {
            if (result.contains(word)) {
                result = result.replace(word, "*".repeat(word.length()));
            }
        }
        return result;
    }

    /**
     * 添加敏感词
     */
    public void addSensitiveWord(String word) {
        sensitiveWords.add(word.toLowerCase());
    }

    /**
     * 批量添加敏感词
     */
    public void addSensitiveWords(Set<String> words) {
        words.forEach(w -> sensitiveWords.add(w.toLowerCase()));
    }

    // ==================== 输入验证 ====================

    /**
     * 检查输入长度
     */
    public boolean isInputTooLong(String input) {
        return input != null && input.length() > maxInputLength;
    }

    /**
     * 安全过滤输入
     *
     * @param input 原始输入
     * @return 过滤后的输入
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // 1. 长度限制
        if (input.length() > maxInputLength) {
            input = input.substring(0, maxInputLength);
        }
        // 2. XSS 过滤
        input = filterXss(input);
        // 3. 敏感词过滤
        input = filterSensitiveWords(input);
        // 4. 去除首尾空白
        input = input.trim();
        return input;
    }

    /**
     * 验证用户名格式
     */
    public boolean isValidUsername(String username) {
        if (username == null || username.length() < 2 || username.length() > 20) {
            return false;
        }
        // 只允许字母、数字、下划线、中文
        return username.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$");
    }

    /**
     * 验证昵称格式
     */
    public boolean isValidNickname(String nickname) {
        if (nickname == null || nickname.length() < 1 || nickname.length() > 12) {
            return false;
        }
        // 不允许纯数字、特殊字符开头
        return !nickname.matches("^\\d+$") && !nickname.matches("^[\\s!@#$%^&*()]+.*");
    }

    // ==================== 统计 ====================

    /**
     * 获取黑名单大小
     */
    public int getBlacklistSize() {
        return ipBlacklist.size();
    }

    /**
     * 获取敏感词数量
     */
    public int getSensitiveWordCount() {
        return sensitiveWords.size();
    }
}
