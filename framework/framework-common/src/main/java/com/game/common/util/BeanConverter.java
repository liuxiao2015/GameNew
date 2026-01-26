package com.game.common.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Bean 转换工具类
 * <p>
 * 提供对象之间的属性拷贝功能，用于 Entity 和 DTO 之间的转换。
 * 支持：
 * <ul>
 *     <li>同名属性自动拷贝</li>
 *     <li>自定义属性映射</li>
 *     <li>批量转换</li>
 *     <li>链式调用</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 简单转换
 * PlayerDTO dto = BeanConverter.convert(playerData, PlayerDTO.class);
 *
 * // 带自定义映射
 * GuildDTO dto = BeanConverter.of(guildData, GuildDTO::new)
 *     .map(GuildData::getGuildCreateTime, GuildDTO::setCreateTime)
 *     .convert();
 *
 * // 批量转换
 * List<PlayerDTO> dtos = BeanConverter.convertList(playerList, PlayerDTO.class);
 * }
 * </pre>
 *
 * @author GameServer
 */
@Slf4j
public final class BeanConverter {

    /**
     * 字段缓存
     */
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    private BeanConverter() {
        // 工具类私有构造
    }

    // ==================== 静态方法 ====================

    /**
     * 将源对象转换为目标类型
     *
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 目标对象，如果源对象为 null 则返回 null
     */
    public static <S, T> T convert(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            copyProperties(source, target);
            return target;
        } catch (Exception e) {
            log.error("Bean 转换失败: source={}, target={}", source.getClass().getSimpleName(),
                    targetClass.getSimpleName(), e);
            return null;
        }
    }

    /**
     * 将源对象转换为目标类型（使用 Supplier 创建目标对象）
     *
     * @param source         源对象
     * @param targetSupplier 目标对象创建器
     * @param <S>            源类型
     * @param <T>            目标类型
     * @return 目标对象
     */
    public static <S, T> T convert(S source, Supplier<T> targetSupplier) {
        if (source == null) {
            return null;
        }
        T target = targetSupplier.get();
        copyProperties(source, target);
        return target;
    }

    /**
     * 批量转换
     *
     * @param sources     源对象列表
     * @param targetClass 目标类型
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 目标对象列表
     */
    public static <S, T> List<T> convertList(Collection<S> sources, Class<T> targetClass) {
        if (sources == null || sources.isEmpty()) {
            return new ArrayList<>();
        }
        return sources.stream()
                .map(s -> convert(s, targetClass))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换（使用 Supplier）
     *
     * @param sources        源对象列表
     * @param targetSupplier 目标对象创建器
     * @param <S>            源类型
     * @param <T>            目标类型
     * @return 目标对象列表
     */
    public static <S, T> List<T> convertList(Collection<S> sources, Supplier<T> targetSupplier) {
        if (sources == null || sources.isEmpty()) {
            return new ArrayList<>();
        }
        return sources.stream()
                .map(s -> convert(s, targetSupplier))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 复制属性
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }

        Map<String, Field> sourceFields = getFields(source.getClass());
        Map<String, Field> targetFields = getFields(target.getClass());

        for (Map.Entry<String, Field> entry : sourceFields.entrySet()) {
            String fieldName = entry.getKey();
            Field sourceField = entry.getValue();
            Field targetField = targetFields.get(fieldName);

            if (targetField != null && isAssignable(sourceField.getType(), targetField.getType())) {
                try {
                    Object value = sourceField.get(source);
                    if (value != null) {
                        targetField.set(target, value);
                    }
                } catch (IllegalAccessException e) {
                    log.debug("字段复制失败: field={}", fieldName);
                }
            }
        }
    }

    /**
     * 创建链式转换器
     *
     * @param source         源对象
     * @param targetSupplier 目标对象创建器
     * @param <S>            源类型
     * @param <T>            目标类型
     * @return 转换器构建器
     */
    public static <S, T> ConverterBuilder<S, T> of(S source, Supplier<T> targetSupplier) {
        return new ConverterBuilder<>(source, targetSupplier);
    }

    // ==================== 内部方法 ====================

    /**
     * 获取类的所有字段（包含父类）
     */
    private static Map<String, Field> getFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            Map<String, Field> fields = new HashMap<>();
            Class<?> current = c;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    // 跳过静态和 final 字段
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    field.setAccessible(true);
                    fields.putIfAbsent(field.getName(), field);
                }
                current = current.getSuperclass();
            }
            return fields;
        });
    }

    /**
     * 检查类型是否可赋值
     */
    private static boolean isAssignable(Class<?> source, Class<?> target) {
        if (target.isAssignableFrom(source)) {
            return true;
        }
        // 处理基本类型和包装类型
        if (source.isPrimitive() || target.isPrimitive()) {
            return arePrimitiveWrapperPair(source, target);
        }
        return false;
    }

    /**
     * 检查是否为基本类型和包装类型对
     */
    private static boolean arePrimitiveWrapperPair(Class<?> c1, Class<?> c2) {
        Map<Class<?>, Class<?>> primitiveToWrapper = Map.of(
                boolean.class, Boolean.class,
                byte.class, Byte.class,
                char.class, Character.class,
                short.class, Short.class,
                int.class, Integer.class,
                long.class, Long.class,
                float.class, Float.class,
                double.class, Double.class
        );

        if (primitiveToWrapper.containsKey(c1)) {
            return primitiveToWrapper.get(c1).equals(c2);
        }
        if (primitiveToWrapper.containsKey(c2)) {
            return primitiveToWrapper.get(c2).equals(c1);
        }
        return false;
    }

    // ==================== 链式转换器 ====================

    /**
     * 转换器构建器
     */
    public static class ConverterBuilder<S, T> {
        private final S source;
        private final T target;
        private final List<Runnable> mappings = new ArrayList<>();

        private ConverterBuilder(S source, Supplier<T> targetSupplier) {
            this.source = source;
            this.target = targetSupplier.get();
            // 先执行默认属性拷贝
            copyProperties(source, target);
        }

        /**
         * 添加自定义属性映射
         *
         * @param getter 源对象 getter
         * @param setter 目标对象 setter
         * @param <V>    属性类型
         * @return this
         */
        public <V> ConverterBuilder<S, T> map(Function<S, V> getter, BiConsumer<T, V> setter) {
            if (source != null) {
                V value = getter.apply(source);
                if (value != null) {
                    setter.accept(target, value);
                }
            }
            return this;
        }

        /**
         * 添加自定义属性映射（带转换）
         *
         * @param getter    源对象 getter
         * @param setter    目标对象 setter
         * @param converter 值转换器
         * @param <V>       源属性类型
         * @param <R>       目标属性类型
         * @return this
         */
        public <V, R> ConverterBuilder<S, T> map(Function<S, V> getter, BiConsumer<T, R> setter,
                                                  Function<V, R> converter) {
            if (source != null) {
                V value = getter.apply(source);
                if (value != null) {
                    R converted = converter.apply(value);
                    if (converted != null) {
                        setter.accept(target, converted);
                    }
                }
            }
            return this;
        }

        /**
         * 忽略某个属性（设置为 null）
         *
         * @param setter 目标对象 setter
         * @param <V>    属性类型
         * @return this
         */
        public <V> ConverterBuilder<S, T> ignore(BiConsumer<T, V> setter) {
            setter.accept(target, null);
            return this;
        }

        /**
         * 设置固定值
         *
         * @param setter 目标对象 setter
         * @param value  固定值
         * @param <V>    属性类型
         * @return this
         */
        public <V> ConverterBuilder<S, T> set(BiConsumer<T, V> setter, V value) {
            setter.accept(target, value);
            return this;
        }

        /**
         * 执行转换并返回结果
         *
         * @return 目标对象
         */
        public T convert() {
            return target;
        }
    }
}
