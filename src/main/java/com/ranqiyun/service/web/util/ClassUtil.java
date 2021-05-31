package com.ranqiyun.service.web.util;

import com.google.common.base.Strings;
import com.ranqiyun.service.web.MainVerticle;
import com.ranqiyun.service.web.annotation.Controller;
import com.ranqiyun.service.web.annotation.RequestMap;
import com.ranqiyun.service.web.annotation.Service;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;


public class ClassUtil {

    public static Set<Class<?>> getAllClassByAnnotation(Class annotationClass, String packageName) {
        try (ScanResult scanResult = new ClassGraph().enableAllInfo().whitelistPackages(packageName).scan()) {
            return scanResult.getClassesWithAnnotation(annotationClass.getName()).stream().map(v -> v.loadClass()).collect(Collectors.toSet());
        }
    }

    public static void main(String[] args) {
        getAllClassByAnnotation(Service.class, MainVerticle.class.getPackageName()).forEach(v -> {
            System.out.println("found: " + v.getName());
        });

        getAllClassByAnnotation(Controller.class, MainVerticle.class.getPackageName()).forEach(module -> {
            Controller moduleAnnotation = module.getAnnotation(Controller.class);

            String moduleName = moduleAnnotation.value();
            if (Strings.isNullOrEmpty(moduleName)) {
                moduleName = module.getSimpleName();
            }

            while (moduleName.startsWith("/"))
                moduleName = moduleName.substring(1);

            for (Method method : module.getMethods()) {
                RequestMap requestMap = method.getAnnotation(RequestMap.class);
                if (requestMap != null) {
                    String requestName = requestMap.value();
                    if (Strings.isNullOrEmpty(requestName)) {
                        requestName = method.getName();
                    }

                    while (requestName.startsWith("/"))
                        requestName = requestName.substring(1);

                    String uri = "/" + moduleName + "/" + requestName;

                    System.out.println(uri);
                }
            }

        });

    }

    public static Object newInstance(Class<?> clazz, Vertx vertx, JsonObject config) {
        Object instance = null;
        try {
            instance = clazz.getConstructor(Vertx.class, JsonObject.class).newInstance(vertx, config);
        } catch (Exception ignored) {
        }

        if (instance == null) {
            try {
                instance = clazz.getConstructor(Vertx.class).newInstance(vertx);
            } catch (Exception ignored) {
            }
        }

        if (instance == null) {
            try {
                instance = clazz.getConstructor(JsonObject.class).newInstance(config);
            } catch (Exception ignored) {
            }
        }

        if (instance == null) {
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (Exception ignored) {
            }
        }

        return instance;
    }

    public static String getControllerName(Class controller) {
        String controllerName = ((Controller) controller.getAnnotation(Controller.class)).value();
        if (Strings.isNullOrEmpty(controllerName)) {
            controllerName = controller.getSimpleName();
        }

        while (controllerName.startsWith("/"))
            controllerName = controllerName.substring(1);

        return controllerName;
    }

    public static String getRequestName(Method request) {
        RequestMap requestMap = request.getAnnotation(RequestMap.class);

        String requestName = requestMap.value();
        if (Strings.isNullOrEmpty(requestName)) {
            requestName = request.getName();
        }

        while (requestName.startsWith("/"))
            requestName = requestName.substring(1);

        return requestName;
    }
}
