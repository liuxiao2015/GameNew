package com.game.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 加密签名工具类
 * <p>
 * 提供常用的加密、解密、签名功能：
 * <ul>
 *     <li>MD5/SHA 哈希</li>
 *     <li>AES 加密/解密</li>
 *     <li>HMAC 签名</li>
 *     <li>Base64 编解码</li>
 *     <li>参数签名验证</li>
 * </ul>
 * </p>
 *
 * @author GameServer
 */
@Slf4j
public final class CryptoUtil {

    private CryptoUtil() {
        // 禁止实例化
    }

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String AES_KEY_ALGORITHM = "AES";
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    // ==================== MD5 ====================

    /**
     * MD5 哈希
     */
    public static String md5(String input) {
        return hash(input, "MD5");
    }

    /**
     * MD5 哈希 (带盐)
     */
    public static String md5(String input, String salt) {
        return md5(input + salt);
    }

    /**
     * MD5 哈希 (字节数组)
     */
    public static String md5(byte[] input) {
        return hash(input, "MD5");
    }

    // ==================== SHA ====================

    /**
     * SHA-1 哈希
     */
    public static String sha1(String input) {
        return hash(input, "SHA-1");
    }

    /**
     * SHA-256 哈希
     */
    public static String sha256(String input) {
        return hash(input, "SHA-256");
    }

    /**
     * SHA-512 哈希
     */
    public static String sha512(String input) {
        return hash(input, "SHA-512");
    }

    /**
     * 通用哈希方法
     */
    public static String hash(String input, String algorithm) {
        if (input == null) {
            return null;
        }
        return hash(input.getBytes(StandardCharsets.UTF_8), algorithm);
    }

    /**
     * 通用哈希方法 (字节数组)
     */
    public static String hash(byte[] input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(input);
            return bytesToHex(digest);
        } catch (Exception e) {
            log.error("哈希计算失败: algorithm={}", algorithm, e);
            throw new RuntimeException("哈希计算失败", e);
        }
    }

    // ==================== HMAC ====================

    /**
     * HMAC-SHA256 签名
     */
    public static String hmacSha256(String data, String key) {
        return hmac(data, key, "HmacSHA256");
    }

    /**
     * HMAC-SHA1 签名
     */
    public static String hmacSha1(String data, String key) {
        return hmac(data, key, "HmacSHA1");
    }

    /**
     * HMAC-MD5 签名
     */
    public static String hmacMd5(String data, String key) {
        return hmac(data, key, "HmacMD5");
    }

    /**
     * 通用 HMAC 签名
     */
    public static String hmac(String data, String key, String algorithm) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(secretKey);
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception e) {
            log.error("HMAC 签名失败: algorithm={}", algorithm, e);
            throw new RuntimeException("HMAC 签名失败", e);
        }
    }

    // ==================== AES ====================

    /**
     * AES 加密
     *
     * @param data 明文
     * @param key  密钥 (16/24/32 字节)
     * @param iv   初始化向量 (16 字节)
     * @return Base64 编码的密文
     */
    public static String aesEncrypt(String data, String key, String iv) {
        try {
            SecretKey secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES_KEY_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES 加密失败", e);
            throw new RuntimeException("AES 加密失败", e);
        }
    }

    /**
     * AES 解密
     *
     * @param encryptedData Base64 编码的密文
     * @param key           密钥 (16/24/32 字节)
     * @param iv            初始化向量 (16 字节)
     * @return 明文
     */
    public static String aesDecrypt(String encryptedData, String key, String iv) {
        try {
            SecretKey secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES_KEY_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES 解密失败", e);
            throw new RuntimeException("AES 解密失败", e);
        }
    }

    /**
     * AES 加密 (使用密钥派生 IV)
     */
    public static String aesEncrypt(String data, String key) {
        String iv = md5(key).substring(0, 16);
        return aesEncrypt(data, normalizeKey(key), iv);
    }

    /**
     * AES 解密 (使用密钥派生 IV)
     */
    public static String aesDecrypt(String encryptedData, String key) {
        String iv = md5(key).substring(0, 16);
        return aesDecrypt(encryptedData, normalizeKey(key), iv);
    }

    /**
     * 标准化密钥长度为 16 字节
     */
    private static String normalizeKey(String key) {
        if (key.length() >= 16) {
            return key.substring(0, 16);
        }
        StringBuilder sb = new StringBuilder(key);
        while (sb.length() < 16) {
            sb.append("0");
        }
        return sb.toString();
    }

    // ==================== Base64 ====================

    /**
     * Base64 编码
     */
    public static String base64Encode(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 编码 (字节数组)
     */
    public static String base64Encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Base64 解码
     */
    public static String base64Decode(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    /**
     * Base64 解码 (返回字节数组)
     */
    public static byte[] base64DecodeBytes(String encoded) {
        return Base64.getDecoder().decode(encoded);
    }

    /**
     * URL 安全的 Base64 编码
     */
    public static String base64UrlEncode(String data) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * URL 安全的 Base64 解码
     */
    public static String base64UrlDecode(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    // ==================== 签名验证 ====================

    /**
     * 生成参数签名
     * <p>
     * 按参数名排序后拼接，使用 HMAC-SHA256 签名
     * </p>
     *
     * @param params    参数 Map
     * @param secretKey 密钥
     * @return 签名
     */
    public static String signParams(Map<String, String> params, String secretKey) {
        // 按 key 排序
        String sortedParams = new TreeMap<>(params).entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .filter(e -> !"sign".equalsIgnoreCase(e.getKey()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        return hmacSha256(sortedParams, secretKey);
    }

    /**
     * 验证参数签名
     */
    public static boolean verifySign(Map<String, String> params, String sign, String secretKey) {
        String expectedSign = signParams(params, secretKey);
        return expectedSign.equalsIgnoreCase(sign);
    }

    /**
     * 生成时间戳签名 (防重放)
     */
    public static String signWithTimestamp(String data, String secretKey, long timestamp) {
        return hmacSha256(data + timestamp, secretKey);
    }

    /**
     * 验证时间戳签名
     *
     * @param data       数据
     * @param sign       签名
     * @param secretKey  密钥
     * @param timestamp  时间戳
     * @param expireMs   有效期 (毫秒)
     * @return 是否有效
     */
    public static boolean verifySignWithTimestamp(String data, String sign, String secretKey,
                                                   long timestamp, long expireMs) {
        // 检查时间戳有效性
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > expireMs) {
            return false;
        }

        String expectedSign = signWithTimestamp(data, secretKey, timestamp);
        return expectedSign.equalsIgnoreCase(sign);
    }

    // ==================== 辅助方法 ====================

    /**
     * 字节数组转十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_DIGITS[(b >> 4) & 0x0f]);
            sb.append(HEX_DIGITS[b & 0x0f]);
        }
        return sb.toString();
    }

    /**
     * 十六进制字符串转字节数组
     */
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }
}
