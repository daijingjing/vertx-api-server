package com.ranqiyun.service.web.util;

import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;
import java.util.Locale;

public class DateUtil {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd").withZone(DateTimeZone.getDefault());
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZone(DateTimeZone.getDefault());
    public static final DateTimeFormatter JSON_DATETIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(DateTimeZone.getDefault());
    public static final DateTimeFormatter GMT_DATETIME_FORMATTER = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss zzz").withZone(DateTimeZone.forID("GMT")).withLocale(Locale.ENGLISH);

    public static long currentTimestamp() {
        return new Date().getTime();
    }

    public static DateTime tryParseDateTime(String s) {

        try {
            return parseGMTDate(s);
        } catch (IllegalArgumentException ignored) {
        }

        try {
            return parseJsonDateTime(s);
        } catch (IllegalArgumentException ignored) {
        }

        try {
            return parseDateTime(s);
        } catch (IllegalArgumentException ignored) {
        }

        try {
            return parseDate(s);
        } catch (IllegalArgumentException ignored) {
        }

        return null;
    }

    //
    // Data
    //

    public static DateTime parseDate(String s) {
        return Strings.isNullOrEmpty(s) ? null : DateTime.parse(s, DATE_FORMATTER);
    }

    public static String formatDate(DateTime dt) {
        return dt.toString(DATE_FORMATTER);
    }

    //
    // Date time
    //

    public static DateTime parseDateTime(String s) {
        return Strings.isNullOrEmpty(s) ? null : DateTime.parse(s, DATETIME_FORMATTER);
    }

    public static String formatDateTime(DateTime dt) {
        return dt.toString(DATETIME_FORMATTER);
    }

    public static String formatDateTime(Date dt) {
        return formatDateTime(new DateTime(dt));
    }

    //
    // Json Date
    //

    public static DateTime parseJsonDateTime(String s) {
        return Strings.isNullOrEmpty(s) ? null : DateTime.parse(s, JSON_DATETIME_FORMATTER);
    }

    public static String formatJsonDateTime(DateTime dt) {
        return dt.toString(JSON_DATETIME_FORMATTER);
    }

    //
    // GMT Data
    //

    public static DateTime parseGMTDate(String s) {
        return Strings.isNullOrEmpty(s) ? null : DateTime.parse(s, GMT_DATETIME_FORMATTER);
    }

    public static String formatGMTDate(DateTime dt) {
        return dt.toString(GMT_DATETIME_FORMATTER);
    }

    public static void main(String[] args) {
        System.out.println(formatDateTime(tryParseDateTime("2021-02-18")));
    }
}
