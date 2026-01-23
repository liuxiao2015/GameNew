package com.game.core.security;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏工具
 * <p>
 * 用于日志输出时对敏感信息进行脱敏处理
 * </p>
 *
 * @author GameServer
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {}

    /**
     * 敏感字段名称 (全小写)
     */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "pwd", "secret", "token", "credential",
            "access_token", "refresh_token", "api_key", "apikey",
            "private_key", "privatekey", "id_card", "idcard",
            "phone", "mobile", "email", "bank_card", "bankcard"
    );

    /**
     * 手机号正则
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    /**
     * 邮箱正则
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}");

    /**
     * 身份证正则
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[\\dXx]");

    /**
     * 银行卡正则
     */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\d{16,19}");

    // ==================== 字段脱敏 ====================

    /**
     * 根据字段名判断是否敏感字段
     */
    public static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        return SENSITIVE_FIELDS.contains(fieldName.toLowerCase());
    }

    /**
     * 脱敏密码
     */
    public static String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return password;
        }
        return "******";
    }

    /**
     * 脱敏手机号 (保留前3后4)
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 脱敏邮箱 (保留@前2位和域名)
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return "*" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    /**
     * 脱敏身份证 (保留前4后4)
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 脱敏银行卡 (保留前4后4)
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + " **** **** " + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 脱敏 Token (只显示前8位)
     */
    public static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8) + "...";
    }

    /**
     * 通用脱敏 (保留首尾，中间用*代替)
     */
    public static String mask(String value, int keepStart, int keepEnd) {
        if (value == null || value.length() <= keepStart + keepEnd) {
            return "***";
        }
        int maskLen = value.length() - keepStart - keepEnd;
        return value.substring(0, keepStart) + "*".repeat(Math.max(3, maskLen)) + value.substring(value.length() - keepEnd);
    }

    // ==================== 文本脱敏 ====================

    /**
     * 自动脱敏文本中的敏感信息
     */
    public static String maskSensitiveData(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        
        // 脱敏手机号
        result = maskPattern(result, PHONE_PATTERN, SensitiveDataMasker::maskPhone);
        
        // 脱敏邮箱
        result = maskPattern(result, EMAIL_PATTERN, SensitiveDataMasker::maskEmail);
        
        // 脱敏身份证
        result = maskPattern(result, ID_CARD_PATTERN, SensitiveDataMasker::maskIdCard);
        
        return result;
    }

    /**
     * 使用正则脱敏
     */
    private static String maskPattern(String text, Pattern pattern, java.util.function.Function<String, String> masker) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, masker.apply(matcher.group()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 判断并脱敏敏感字段值
     */
    public static String maskFieldValue(String fieldName, String value) {
        if (value == null || fieldName == null) {
            return value;
        }
        
        String lowerField = fieldName.toLowerCase();
        
        if (lowerField.contains("password") || lowerField.contains("pwd") || lowerField.contains("secret")) {
            return maskPassword(value);
        }
        if (lowerField.contains("token") || lowerField.contains("key")) {
            return maskToken(value);
        }
        if (lowerField.contains("phone") || lowerField.contains("mobile")) {
            return maskPhone(value);
        }
        if (lowerField.contains("email")) {
            return maskEmail(value);
        }
        if (lowerField.contains("idcard") || lowerField.contains("id_card")) {
            return maskIdCard(value);
        }
        if (lowerField.contains("bankcard") || lowerField.contains("bank_card")) {
            return maskBankCard(value);
        }
        
        return value;
    }
}
