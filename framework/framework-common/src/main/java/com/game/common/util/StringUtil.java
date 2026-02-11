package com.game.common.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * 字符串工具类
 * <p>
 * 通用方法委托 Hutool {@link StrUtil}，
 * 游戏业务相关方法 (中文长度计算、名称校验、脱敏) 保持自实现。
 * </p>
 *
 * @author GameServer
 */
public final class StringUtil {

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?\\s]");
    private static final Pattern ALPHA_NUM_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private StringUtil() {
    }

    // ==================== 空判断 (委托 Hutool) ====================

    public static boolean isEmpty(String str) {
        return StrUtil.isEmpty(str);
    }

    public static boolean isNotEmpty(String str) {
        return StrUtil.isNotEmpty(str);
    }

    public static boolean isBlank(String str) {
        return StrUtil.isBlank(str);
    }

    public static boolean isNotBlank(String str) {
        return StrUtil.isNotBlank(str);
    }

    // ==================== 默认值 (委托 Hutool) ====================

    public static String defaultIfEmpty(String str, String defaultValue) {
        return StrUtil.emptyToDefault(str, defaultValue);
    }

    public static String defaultIfBlank(String str, String defaultValue) {
        return StrUtil.blankToDefault(str, defaultValue);
    }

    // ==================== 长度计算 (游戏业务: 中文算2个字符) ====================

    public static int length(String str) {
        if (isEmpty(str)) {
            return 0;
        }
        int length = 0;
        for (char c : str.toCharArray()) {
            length += isChinese(c) ? 2 : 1;
        }
        return length;
    }

    public static boolean isChinese(char c) {
        return c >= '\u4e00' && c <= '\u9fa5';
    }

    public static boolean containsChinese(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return CHINESE_PATTERN.matcher(str).find();
    }

    // ==================== 校验 (游戏业务) ====================

    public static boolean isValidName(String name, int minLen, int maxLen) {
        if (isBlank(name)) {
            return false;
        }
        int len = length(name);
        if (len < minLen || len > maxLen) {
            return false;
        }
        return !SPECIAL_CHAR_PATTERN.matcher(name).find();
    }

    public static boolean isAlphaNumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return ALPHA_NUM_PATTERN.matcher(str).matches();
    }

    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    // ==================== 生成 (委托 Hutool) ====================

    public static String uuid() {
        return IdUtil.fastSimpleUUID();
    }

    public static String randomString(int length) {
        return RandomUtil.randomString(length);
    }

    public static String randomNumeric(int length) {
        return RandomUtil.randomNumbers(length);
    }

    // ==================== 处理 (游戏业务: 中文截取/脱敏) ====================

    public static String truncate(String str, int maxLength) {
        if (isEmpty(str) || maxLength <= 0) {
            return "";
        }
        if (length(str) <= maxLength) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        int len = 0;
        for (char c : str.toCharArray()) {
            int charLen = isChinese(c) ? 2 : 1;
            if (len + charLen > maxLength) {
                break;
            }
            sb.append(c);
            len += charLen;
        }
        return sb.toString();
    }

    public static String maskPhone(String phone) {
        if (isEmpty(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskEmail(String email) {
        if (isEmpty(email) || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    // ==================== 转换 (委托 Hutool) ====================

    public static String capitalize(String str) {
        return StrUtil.upperFirst(str);
    }

    public static String uncapitalize(String str) {
        return StrUtil.lowerFirst(str);
    }

    public static String camelToUnderscore(String str) {
        return StrUtil.toUnderlineCase(str);
    }

    public static String underscoreToCamel(String str) {
        return StrUtil.toCamelCase(str);
    }
}
