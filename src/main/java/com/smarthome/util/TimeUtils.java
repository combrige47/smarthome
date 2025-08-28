package com.smarthome.util;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    // 定义常用日期时间格式
    public static final String PATTERN_DEFAULT = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_WITH_MILLIS = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String PATTERN_DATE = "yyyy-MM-dd";
    public static final String PATTERN_TIME = "HH:mm:ss";

    /**
     * @param timestamp 毫秒级时间戳
     * @param pattern 日期格式
     * @return 格式化后的字符串
     */
    public static String formatTimestamp(long timestamp, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.CHINA);
        return sdf.format(new Date(timestamp));
    }

    /**
     * Java 8+：使用DateTimeFormatter转换时间戳（线程安全）
     * @param timestamp 毫秒级时间戳
     * @param pattern 日期格式
     * @return 格式化后的字符串
     */
    public static String formatTimestampJava8(long timestamp, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.CHINA);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault() // 使用系统默认时区（如Asia/Shanghai）
        );
        return localDateTime.format(formatter);
    }

    // 简化调用：使用默认格式
    public static String formatTimestamp(long timestamp) {
        return formatTimestampJava8(timestamp, PATTERN_DEFAULT);
    }
}