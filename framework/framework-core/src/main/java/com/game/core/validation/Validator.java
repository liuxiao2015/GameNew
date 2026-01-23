package com.game.core.validation;

import com.game.common.enums.ErrorCode;
import com.game.common.exception.BizException;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;

/**
 * 参数验证器
 * <p>
 * 提供链式验证能力，简化参数校验代码
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 链式验证
 * Validator.of(request)
 *     .notEmpty("roleName", ErrorCode.PARAM_ERROR)
 *     .range("level", 1, 100, ErrorCode.PARAM_ERROR)
 *     .positive("gold", ErrorCode.PARAM_ERROR)
 *     .validate();
 *
 * // 简单验证
 * Validator.notNull(player, ErrorCode.ROLE_NOT_FOUND);
 * Validator.notEmpty(name, ErrorCode.PARAM_ERROR, "名称不能为空");
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
public class Validator {

    private final Object target;
    private boolean hasError = false;
    private ErrorCode firstError;
    private String firstMessage;

    private Validator(Object target) {
        this.target = target;
    }

    // ==================== 静态方法（简单验证）====================

    /**
     * 验证不为 null
     */
    public static void notNull(Object value, ErrorCode errorCode) {
        if (value == null) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 验证不为 null (带消息)
     */
    public static void notNull(Object value, ErrorCode errorCode, String message) {
        if (value == null) {
            throw new BizException(errorCode, message);
        }
    }

    /**
     * 验证字符串不为空
     */
    public static void notEmpty(String value, ErrorCode errorCode) {
        if (value == null || value.isEmpty()) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 验证字符串不为空 (带消息)
     */
    public static void notEmpty(String value, ErrorCode errorCode, String message) {
        if (value == null || value.isEmpty()) {
            throw new BizException(errorCode, message);
        }
    }

    /**
     * 验证集合不为空
     */
    public static void notEmpty(Collection<?> collection, ErrorCode errorCode) {
        if (collection == null || collection.isEmpty()) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 验证正数
     */
    public static void positive(long value, ErrorCode errorCode) {
        if (value <= 0) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 验证正数 (带消息)
     */
    public static void positive(long value, ErrorCode errorCode, String message) {
        if (value <= 0) {
            throw new BizException(errorCode, message);
        }
    }

    /**
     * 验证非负数
     */
    public static void nonNegative(long value, ErrorCode errorCode) {
        if (value < 0) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 验证范围
     */
    public static void range(long value, long min, long max, ErrorCode errorCode) {
        if (value < min || value > max) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 验证条件为真
     */
    public static void isTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 验证条件为真 (带消息)
     */
    public static void isTrue(boolean condition, ErrorCode errorCode, String message) {
        if (!condition) {
            throw new BizException(errorCode, message);
        }
    }

    /**
     * 验证条件为假
     */
    public static void isFalse(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new BizException(errorCode);
        }
    }

    // ==================== 链式验证（Protobuf 消息）====================

    /**
     * 创建验证器
     */
    public static Validator of(Object target) {
        return new Validator(target);
    }

    /**
     * 验证 Protobuf 字段不为空
     */
    public Validator fieldNotEmpty(String fieldName, ErrorCode errorCode) {
        if (hasError) return this;

        try {
            Object value = getFieldValue(fieldName);
            if (value == null || (value instanceof String s && s.isEmpty())) {
                recordError(errorCode, fieldName + " 不能为空");
            }
        } catch (Exception e) {
            log.warn("验证字段失败: {}", fieldName, e);
        }
        return this;
    }

    /**
     * 验证字段为正数
     */
    public Validator positive(String fieldName, ErrorCode errorCode) {
        if (hasError) return this;

        try {
            Object value = getFieldValue(fieldName);
            if (value instanceof Number num && num.longValue() <= 0) {
                recordError(errorCode, fieldName + " 必须为正数");
            }
        } catch (Exception e) {
            log.warn("验证字段失败: {}", fieldName, e);
        }
        return this;
    }

    /**
     * 验证字段范围
     */
    public Validator range(String fieldName, long min, long max, ErrorCode errorCode) {
        if (hasError) return this;

        try {
            Object value = getFieldValue(fieldName);
            if (value instanceof Number num) {
                long v = num.longValue();
                if (v < min || v > max) {
                    recordError(errorCode, fieldName + " 必须在 " + min + " 到 " + max + " 之间");
                }
            }
        } catch (Exception e) {
            log.warn("验证字段失败: {}", fieldName, e);
        }
        return this;
    }

    /**
     * 验证字符串长度
     */
    public Validator length(String fieldName, int min, int max, ErrorCode errorCode) {
        if (hasError) return this;

        try {
            Object value = getFieldValue(fieldName);
            if (value instanceof String s) {
                if (s.length() < min || s.length() > max) {
                    recordError(errorCode, fieldName + " 长度必须在 " + min + " 到 " + max + " 之间");
                }
            }
        } catch (Exception e) {
            log.warn("验证字段失败: {}", fieldName, e);
        }
        return this;
    }

    /**
     * 验证匹配正则
     */
    public Validator matches(String fieldName, String regex, ErrorCode errorCode) {
        if (hasError) return this;

        try {
            Object value = getFieldValue(fieldName);
            if (value instanceof String s && !s.matches(regex)) {
                recordError(errorCode, fieldName + " 格式不正确");
            }
        } catch (Exception e) {
            log.warn("验证字段失败: {}", fieldName, e);
        }
        return this;
    }

    /**
     * 执行验证（如果有错误则抛出异常）
     */
    public void validate() {
        if (hasError) {
            throw new BizException(firstError, firstMessage);
        }
    }

    /**
     * 获取字段值
     */
    private Object getFieldValue(String fieldName) {
        if (target instanceof Message message) {
            Descriptors.FieldDescriptor field = message.getDescriptorForType()
                    .findFieldByName(toSnakeCase(fieldName));
            if (field != null) {
                return message.getField(field);
            }
        } else if (target instanceof Map<?, ?> map) {
            return map.get(fieldName);
        }
        return null;
    }

    /**
     * 驼峰转下划线
     */
    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * 记录错误
     */
    private void recordError(ErrorCode errorCode, String message) {
        if (!hasError) {
            hasError = true;
            firstError = errorCode;
            firstMessage = message;
        }
    }
}
