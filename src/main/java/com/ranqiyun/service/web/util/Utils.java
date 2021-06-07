package com.ranqiyun.service.web.util;

import com.google.common.base.Strings;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.joda.time.DateTime;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.ranqiyun.service.web.util.DateUtil.formatDateTime;


/**
 * Created by daijingjing on 2018/4/19.
 * <p>
 * 辅助功能代码
 */
public class Utils {

    public static final char[] ASCII_UPPERCASE = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
    public static final char[] ASCII_LOWERCASE = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    public static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    public static final char[] SPECIAL_CHARS = {'~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '=', '_', '+'};

    public static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=";

    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null)
            throw new NullPointerException(message);
        if (obj instanceof String && Strings.isNullOrEmpty((String) obj))
            throw new RuntimeException(message);
        return obj;
    }

    /**
     * 生成唯一标识ID
     *
     * @return 唯一标识ID
     */
    public static String newId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static String generate_password(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            Random random = new Random();
            sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }

        return sb.toString();
    }

    public static JsonArray buildJsonArray(Object... params) {
        JsonArray p = new JsonArray();
        for (Object param : params) {
            if (Objects.isNull(param)) p.addNull();
            else if (param instanceof String && Strings.isNullOrEmpty((String) param)) p.addNull();
            else if (param instanceof DateTime) p.add(formatDateTime((DateTime) param));
            else if (param instanceof Date) p.add(formatDateTime((Date) param));
            else p.add(param);
        }
        return p;
    }

    public static Object[] buildArray(Object... params) {
        List<Object> p = new ArrayList<>();
        for (Object param : params) {
            if (Objects.isNull(param)) p.add(null);
            else if (param instanceof String && Strings.isNullOrEmpty((String) param)) p.add(null);
            else if (param instanceof DateTime) p.add(formatDateTime((DateTime) param));
            else if (param instanceof Date) p.add(formatDateTime((Date) param));
            else if (param instanceof JsonObject) p.add(((JsonObject) param).encode());
            else p.add(param);
        }
        return p.toArray();
    }

    public static Map.Entry<Object[], String> buildEntrys(Object[] params, String sql) {
        return new AbstractMap.SimpleEntry<>(params, sql);
    }

    public static List<JsonArray> buildListJsonArray(JsonArray... params) {
        return Arrays.stream(params).collect(Collectors.toList());
    }

    public static List<String> buildListString(String... params) {
        return Arrays.stream(params).collect(Collectors.toList());
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, @Nullable Object> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return object -> seen.putIfAbsent(keyExtractor.apply(object), Boolean.TRUE) == null;
    }

    public static <T, R> Future<List<R>> transform(List<T> s, Function<T, Future<R>> mapper) {
        return CompositeFuture.all(s.stream().map(mapper).collect(Collectors.toList()))
            .map(CompositeFuture::list);
    }

    public static <T, R> Future<List<R>> transform(Future<List<T>> future, Function<T, Future<R>> mapper) {
        return future.compose(s -> CompositeFuture.all(s.stream().map(mapper).collect(Collectors.toList()))
            .map(CompositeFuture::list));
    }

}
