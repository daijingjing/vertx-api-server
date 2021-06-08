package com.ranqiyun.service.web.common;

import com.google.common.base.Strings;
import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Controller;
import com.ranqiyun.service.web.annotation.Params;
import com.ranqiyun.service.web.annotation.RequestMap;
import com.ranqiyun.service.web.services.LogService;
import com.ranqiyun.service.web.util.ClassUtil;
import com.ranqiyun.service.web.util.DateUtil;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * 预处理HTTP请求的token信息，获取当前令牌的用户信息
 *
 * Created by daijingjing on 2018/5/10.
 */
public class DispatchHandler extends ControllerBase implements Handler<RoutingContext> {
    protected static final Logger logger = LoggerFactory.getLogger(DispatchHandler.class);

    private static final int KB = 1024;
    private static final int MB = 1024 * KB;

    @AutowiredService
    LogService logService;

    private final String uriPrefix;
    private final AtomicBoolean bInitialized = new AtomicBoolean(false);

    private Map<String, RequestEntry> requestMaps = new TreeMap<>();

    public static Router create(Vertx vertx, JsonObject config, String handlersPackageName) {
        logger.info("Initializing router ...");
        Router router = Router.router(vertx);

        router.route().handler(LoggerHandler.create());
        router.route().handler(TimeoutHandler.create(60000));
        router.route().handler(ResponseTimeHandler.create());

        enableCorsSupport(router);

        // body _handler
        router.route().handler(BodyHandler.create(
            config.getString("upload.temp", "./temp"))
            .setBodyLimit(50 * MB)
            .setMergeFormAttributes(true)
            .setDeleteUploadedFilesOnEnd(true));

        // 业务分发接口
        router.route().handler(ServiceManager.autowired(new TokenHandler(vertx, config)));
        router.route().handler(ServiceManager.autowired(new DispatchHandler(vertx, config, handlersPackageName)));

        // static content
        router.route().handler(StaticHandler.create("webroot"));

        return router;
    }

    /**
     * Enable CORS support.
     *
     * @param router router instance
     */
    protected static void enableCorsSupport(Router router) {
        Set<String> allowHeaders = new HashSet<>();
        allowHeaders.add("x-requested-with");
        allowHeaders.add("Access-Control-Allow-Origin");
        allowHeaders.add("origin");
        allowHeaders.add("Content-Type");
        allowHeaders.add("Token");
        allowHeaders.add("accept");
        Set<HttpMethod> allowMethods = new HashSet<>();
        allowMethods.add(HttpMethod.GET);
        allowMethods.add(HttpMethod.PUT);
        allowMethods.add(HttpMethod.OPTIONS);
        allowMethods.add(HttpMethod.POST);
        allowMethods.add(HttpMethod.DELETE);
        allowMethods.add(HttpMethod.PATCH);

        Set<String> exposedHeaders = new HashSet<>();
        exposedHeaders.add("Verifier-Sign");

        router.route().handler(CorsHandler.create("*")
            .allowedHeaders(allowHeaders)
            .allowedMethods(allowMethods)
            .exposedHeaders(exposedHeaders));
    }

    static class RequestEntry {
        public String url;
        public Controller controller;
        public RequestMap requestMap;
        public Object instance;
        public Method method;

        public RequestEntry(String url, Object instance, Method method) {
            this.url = url;
            this.instance = instance;
            this.method = method;
            this.controller = instance.getClass().getAnnotation(Controller.class);
            this.requestMap = method.getAnnotation(RequestMap.class);
        }
    }

    public DispatchHandler(Vertx vertx, JsonObject config, String packageName) {
        super(vertx, config);
        uriPrefix = config.getString("prefix", "/api");

        vertx.executeBlocking(r -> {
                ClassUtil.getAllClassByAnnotation(Controller.class, packageName).forEach(controller -> {
                    logger.info(String.format("Add Controller %s ...", controller.getName()));
                    Object instance = ClassUtil.newInstance(controller, vertx, config);

                    if (instance != null) {
                        ServiceManager.autowired(instance);

                        String controllerName = ClassUtil.getControllerName(controller);

                        Arrays.stream(controller.getMethods())
                            .filter(request -> request.getAnnotation(RequestMap.class) != null)
                            .forEach(request -> {
                                String requestName = ClassUtil.getRequestName(request);

                                String uri = "/" + controllerName + "/" + requestName;

                                if (requestMaps.containsKey(uri)) {
                                    r.fail(String.format("路由地址重复: [%s%s] [%s.%s] ...", this.uriPrefix, uri, instance.getClass().getName(), request.getName()));
                                    return;
                                }

                                requestMaps.put(uri, new RequestEntry(uri, instance, request));

                                logger.info(String.format("Register Request [%s%s] [%s.%s] ...", this.uriPrefix, uri, instance.getClass().getName(), request.getName()));
                            });
                    }

                });

                // 标记完成
                r.complete();
            },
            ar -> {
                if (ar.succeeded()) {
                    logger.info("Initialize Dispatcher Sucessed!");
                    bInitialized.set(true);
                } else {
                    logger.error("初始化路由调用器失败", ar.cause());
                    vertx.close();
                }
            });
    }

    @Override
    public void handle(RoutingContext context) {
        if (bInitialized.get()) {
            String path = context.request().path().substring(uriPrefix.length());
            while (path.startsWith("//"))
                path = path.substring(1);

            String finalPath = path;
            String map = requestMaps.keySet().stream().filter(finalPath::equals).findFirst().orElse(null);

            if (!Strings.isNullOrEmpty(map)) {
                RequestEntry x = requestMaps.get(map);
                if (!x.requestMap.anonymous() && Strings.isNullOrEmpty(currentUserId(context))) {
                    // 未登录
                    responseJson(context, 404, "用户未登录", 403);
                } else if (checkModulePermission(context, x)) {
                    logService.log(currentUserId(context), x.controller.describe(), null, x.requestMap.describe(), getParams(context).encodePrettily());
                    // 有权限
                    vertx.executeBlocking(r -> {
                        try {
                            process_request(context, x);
                            r.complete();
                        } catch (Exception exception) {
                            logger.error(exception.getMessage(), exception);
                            r.fail(exception);
                        }
                    }, ar -> {
                        if (!ar.succeeded()) {
                            logger.error(ar.cause().getMessage(), ar.cause());
                            internalError(context, ar.cause());
                        }
                    });
                } else {
                    // 无权限
                    serviceUnavailable(context, 403, 403, "用户无操作权限，请与管理员联系");
                }
            } else {
                // 不存在映射功能
                context.next();
            }
        } else {
            internalError(context, "分发程序未完成初始化");
        }
    }

    private boolean checkModulePermission(RoutingContext context, RequestEntry requestEntry) {
        return true;
    }

    private void process_request(RoutingContext context, RequestEntry requestEntry) throws Exception {
        Parameter[] method_params = requestEntry.method.getParameters();

        if (method_params.length == 1 && method_params[0].getType().equals(RoutingContext.class)) {
            requestEntry.method.invoke(requestEntry.instance, context);
        } else if (method_params.length == 2
            && method_params[0].getType().equals(RoutingContext.class)
            && method_params[1].getType().equals(JsonObject.class)) {
            requestEntry.method.invoke(requestEntry.instance, context, getParams(context));
        } else if (method_params.length >= 2
            && method_params[0].getType().equals(RoutingContext.class)) {
            Object[] ppp = new Object[method_params.length];
            ppp[0] = context;

            JsonObject params = getParams(context);

            for (int i = 1; i < ppp.length; i++) {

                if (method_params[i].getType().equals(String.class)) {
                    ppp[i] = params.getValue(method_params[i].getAnnotation(Params.class).value());
                    if (Objects.nonNull(ppp[i])) {
                        ppp[i] = Strings.isNullOrEmpty(ppp[i].toString()) ? null : ppp[i].toString();
                    }

                } else if (method_params[i].getType().equals(Integer.class)) {
                    ppp[i] = params.getValue(method_params[i].getAnnotation(Params.class).value());
                    if (Objects.nonNull(ppp[i])) {
                        ppp[i] = Strings.isNullOrEmpty(ppp[i].toString()) ? null : Integer.valueOf(ppp[i].toString());
                    }

                } else if (method_params[i].getType().equals(Boolean.class)) {
                    ppp[i] = params.getValue(method_params[i].getAnnotation(Params.class).value());
                    if (Objects.nonNull(ppp[i])) {
                        ppp[i] = Strings.isNullOrEmpty(ppp[i].toString()) ? null : Boolean.valueOf(ppp[i].toString());
                    }

                } else if (method_params[i].getType().equals(Long.class)) {
                    ppp[i] = params.getValue(method_params[i].getAnnotation(Params.class).value());
                    if (Objects.nonNull(ppp[i])) {
                        ppp[i] = Strings.isNullOrEmpty(ppp[i].toString()) ? null : Long.valueOf(ppp[i].toString());
                    }

                } else if (method_params[i].getType().equals(JsonArray.class)) {
                    ppp[i] = params.getJsonArray(method_params[i].getAnnotation(Params.class).value());

                } else if (method_params[i].getType().equals(JsonObject.class)) {
                    ppp[i] = params.getJsonObject(method_params[i].getAnnotation(Params.class).value());

                } else if (method_params[i].getType().equals(Date.class)) {
                    DateTime dt = DateUtil.tryParseDateTime(params.getValue(method_params[i].getAnnotation(Params.class).value()).toString());
                    ppp[i] = Objects.nonNull(dt) ? dt.toDate() : null;

                } else if (method_params[i].getType().equals(DateTime.class)) {
                    ppp[i] = DateUtil.tryParseDateTime(params.getValue(method_params[i].getAnnotation(Params.class).value()).toString());

                } else if (method_params[i].getType().equals(Double.class)) {
                    ppp[i] = params.getValue(method_params[i].getAnnotation(Params.class).value());
                    if (Objects.nonNull(ppp[i])) {
                        ppp[i] = Strings.isNullOrEmpty(ppp[i].toString()) ? null : Double.valueOf(ppp[i].toString());
                    }
                } else if (method_params[i].getType().equals(BigDecimal.class)) {
                    ppp[i] = params.getValue(method_params[i].getAnnotation(Params.class).value());
                    if (Objects.nonNull(ppp[i])) {
                        ppp[i] = Strings.isNullOrEmpty(ppp[i].toString()) ? null : new BigDecimal(ppp[i].toString());
                    }
                } else {
                    throw new Exception(String.format("接口不支持的参数定义类型：%s", method_params[i].getType().getName()));
                }

                if (method_params[i].getAnnotation(Params.class).required()) {
                    requireNonNull(ppp[i], method_params[i].getAnnotation(Params.class).value() + "参数不能为空");
                }
            }

            requestEntry.method.invoke(requestEntry.instance, ppp);
        } else {
            throw new Exception("服务接口定义类型错误");
        }
    }

    private JsonObject getParams(RoutingContext context) {
        Buffer body = context.getBody();
        if (Objects.isNull(body) || body.length() == 0) {
            JsonObject p = new JsonObject();
            context.request().params().forEach(entry -> {
                if (!entry.getKey().equals("token"))
                    p.put(entry.getKey(), entry.getValue());
            });
            return p;
        } else {
            if (context.request().getHeader(HttpHeaders.CONTENT_TYPE).contains("application/json")) {
                return body.toJsonObject();
            } else {
                JsonObject p = new JsonObject();
                context.request().params().forEach(entry -> {
                    if (!entry.getKey().equals("token"))
                        p.put(entry.getKey(), entry.getValue());
                });
                return p;
            }
        }
    }
}
