package com.ranqiyun.service.web.common;

import com.ranqiyun.service.web.annotation.AutowiredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceManager {

    private static final Logger logger = LoggerFactory.getLogger(ServiceManager.class);
    private static final Map<String, Object> _services = new ConcurrentHashMap<>();

    public static <T> T getService(Class clazz) {
        return (T) getService(clazz.getName());
    }

    private static <T> T getService(String name) {
        if (_services.containsKey(name)) {
            return (T) _services.get(name);
        } else {
            logger.error(String.format("Don't Found Service %s ...", name));
            throw new RuntimeException(String.format("Don't Found Service %s ...", name));
        }
    }

    public static void registerService(Object service) {
        registerService(service.getClass(), service);
    }

    public static void registerService(Class clazz, Object service) {
        registerService(clazz.getName(), service);
    }

    public static synchronized void registerService(String name, Object service) {
        if (_services.containsKey(name)) {
            return;
        }
        logger.info(String.format("Register Service %s ...", name));
        _services.put(name, service);
    }

    public static synchronized void serviceAutowired() {
        _services.values().forEach(s -> {
            autowired(s);
        });
    }

    private static List<Field> getDeclaredFields(Class clazz) {
        List<Field> fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        if (clazz.getSuperclass() != null) {
            List<Field> superFields = getDeclaredFields(clazz.getSuperclass());
            fields.addAll(superFields);
        }

        return fields;
    }

    public static synchronized <T> T autowired(Object obj) {
        List<Field> fields = getDeclaredFields(obj.getClass());

        fields.stream().filter(f -> f.getAnnotation(AutowiredService.class) != null)
            .forEach(field -> {
                logger.info(String.format("Auto wired %s.%s ...", obj.getClass().getName(), field.getName()));

                Object service = getService(field.getType());
                if (service != null) {
                    field.setAccessible(true);
                    try {
                        field.set(obj, service);
                    } catch (IllegalAccessException e) {
                        logger.error(String.format("Auto wired %s.%s[%s] failed, Illegal Access Exception!",
                            obj.getClass().getName(), field.getName(), field.getType().getName()));
                    }
                } else {
                    logger.error(String.format("Auto wired %s.%s[%s] failed, service don't found!",
                        obj.getClass().getName(), field.getName(), field.getType().getName()));
                    throw new RuntimeException(String.format("Auto wired %s.%s[%s] failed, service don't found!",
                        obj.getClass().getName(), field.getName(), field.getType().getName()));
                }
            });

        return (T) obj;
    }

    public static void shutdown() {
        _services.values().forEach(s -> {
            try {
                Method m = s.getClass().getMethod("destroy");
                m.invoke(s);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }
}
