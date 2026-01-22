package com.game.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * 时间工具类
 *
 * @author GameServer
 */
public final class TimeUtil {

    /**
     * 默认时区 (东八区)
     */
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 日期格式
     */
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 时间格式
     */
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 日期时间格式
     */
    public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 紧凑日期时间格式
     */
    public static final DateTimeFormatter COMPACT_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 一天的毫秒数
     */
    public static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;

    /**
     * 一小时的毫秒数
     */
    public static final long MILLIS_PER_HOUR = 60 * 60 * 1000L;

    /**
     * 一分钟的毫秒数
     */
    public static final long MILLIS_PER_MINUTE = 60 * 1000L;

    /**
     * 一秒的毫秒数
     */
    public static final long MILLIS_PER_SECOND = 1000L;

    private TimeUtil() {
        // 禁止实例化
    }

    // ==================== 当前时间 ====================

    /**
     * 获取当前时间戳 (毫秒)
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间戳 (秒)
     */
    public static long currentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * 获取当前日期时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    /**
     * 获取当前日期
     */
    public static LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE);
    }

    // ==================== 时间转换 ====================

    /**
     * 时间戳转 LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), DEFAULT_ZONE);
    }

    /**
     * LocalDateTime 转时间戳
     */
    public static long toTimestamp(LocalDateTime dateTime) {
        return dateTime.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
    }

    /**
     * LocalDate 转时间戳 (当天 00:00:00)
     */
    public static long toTimestamp(LocalDate date) {
        return date.atStartOfDay(DEFAULT_ZONE).toInstant().toEpochMilli();
    }

    /**
     * 时间戳转 LocalDate
     */
    public static LocalDate toLocalDate(long timestamp) {
        return toLocalDateTime(timestamp).toLocalDate();
    }

    // ==================== 格式化 ====================

    /**
     * 格式化日期时间
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime.format(DATETIME_FORMAT);
    }

    /**
     * 格式化日期时间 (自定义格式)
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 格式化时间戳
     */
    public static String format(long timestamp) {
        return format(toLocalDateTime(timestamp));
    }

    /**
     * 格式化日期
     */
    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    /**
     * 格式化时间戳为日期
     */
    public static String formatDate(long timestamp) {
        return formatDate(toLocalDate(timestamp));
    }

    // ==================== 解析 ====================

    /**
     * 解析日期时间字符串
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMAT);
    }

    /**
     * 解析日期字符串
     */
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMAT);
    }

    // ==================== 判断 ====================

    /**
     * 判断是否同一天
     */
    public static boolean isSameDay(long timestamp1, long timestamp2) {
        return toLocalDate(timestamp1).equals(toLocalDate(timestamp2));
    }

    /**
     * 判断是否同一周
     */
    public static boolean isSameWeek(long timestamp1, long timestamp2) {
        LocalDate date1 = toLocalDate(timestamp1);
        LocalDate date2 = toLocalDate(timestamp2);
        // 以周一为一周的开始
        LocalDate weekStart1 = date1.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekStart2 = date2.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return weekStart1.equals(weekStart2);
    }

    /**
     * 判断是否同一月
     */
    public static boolean isSameMonth(long timestamp1, long timestamp2) {
        LocalDate date1 = toLocalDate(timestamp1);
        LocalDate date2 = toLocalDate(timestamp2);
        return date1.getYear() == date2.getYear() && date1.getMonth() == date2.getMonth();
    }

    /**
     * 判断时间戳是否已过期
     */
    public static boolean isExpired(long timestamp) {
        return timestamp > 0 && timestamp < currentTimeMillis();
    }

    /**
     * 判断是否是今天
     */
    public static boolean isToday(long timestamp) {
        return isSameDay(timestamp, currentTimeMillis());
    }

    // ==================== 计算 ====================

    /**
     * 获取今天开始时间戳 (00:00:00)
     */
    public static long getTodayStart() {
        return toTimestamp(today());
    }

    /**
     * 获取今天结束时间戳 (23:59:59.999)
     */
    public static long getTodayEnd() {
        return getTodayStart() + MILLIS_PER_DAY - 1;
    }

    /**
     * 获取指定日期开始时间戳
     */
    public static long getDayStart(long timestamp) {
        return toTimestamp(toLocalDate(timestamp));
    }

    /**
     * 获取指定日期结束时间戳
     */
    public static long getDayEnd(long timestamp) {
        return getDayStart(timestamp) + MILLIS_PER_DAY - 1;
    }

    /**
     * 获取本周开始时间戳 (周一 00:00:00)
     */
    public static long getWeekStart() {
        LocalDate monday = today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return toTimestamp(monday);
    }

    /**
     * 获取本月开始时间戳
     */
    public static long getMonthStart() {
        LocalDate firstDay = today().with(TemporalAdjusters.firstDayOfMonth());
        return toTimestamp(firstDay);
    }

    /**
     * 计算两个时间戳之间的天数
     */
    public static long daysBetween(long timestamp1, long timestamp2) {
        LocalDate date1 = toLocalDate(timestamp1);
        LocalDate date2 = toLocalDate(timestamp2);
        return ChronoUnit.DAYS.between(date1, date2);
    }

    /**
     * 添加天数
     */
    public static long addDays(long timestamp, int days) {
        return timestamp + days * MILLIS_PER_DAY;
    }

    /**
     * 添加小时
     */
    public static long addHours(long timestamp, int hours) {
        return timestamp + hours * MILLIS_PER_HOUR;
    }

    /**
     * 添加分钟
     */
    public static long addMinutes(long timestamp, int minutes) {
        return timestamp + minutes * MILLIS_PER_MINUTE;
    }

    /**
     * 添加秒
     */
    public static long addSeconds(long timestamp, int seconds) {
        return timestamp + seconds * MILLIS_PER_SECOND;
    }

    /**
     * 获取友好的时间描述
     */
    public static String getFriendlyTime(long timestamp) {
        long diff = currentTimeMillis() - timestamp;
        if (diff < 0) {
            return "刚刚";
        }
        if (diff < MILLIS_PER_MINUTE) {
            return "刚刚";
        }
        if (diff < MILLIS_PER_HOUR) {
            return (diff / MILLIS_PER_MINUTE) + "分钟前";
        }
        if (diff < MILLIS_PER_DAY) {
            return (diff / MILLIS_PER_HOUR) + "小时前";
        }
        if (diff < 7 * MILLIS_PER_DAY) {
            return (diff / MILLIS_PER_DAY) + "天前";
        }
        return formatDate(timestamp);
    }
}
