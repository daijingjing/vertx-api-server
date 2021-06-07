package com.ranqiyun.service.web.common;

import com.google.common.base.Strings;
import com.ranqiyun.service.web.util.DateUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ControllerBase {

    protected final Logger logger;
    protected final Vertx vertx;
    protected final JsonObject config;

    public ControllerBase(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        logger = LoggerFactory.getLogger(this.getClass());
    }

    public static String currentUserId(RoutingContext context) {
        return context.get("user_id");
    }

    public static void currentUserId(RoutingContext context, String userId) {
        context.put("user_id", userId);
    }

    public static String getClientAddress(HttpServerRequest request) {
        String real_ip = request.headers().get("x-real-ip");
        if (real_ip != null && real_ip.length() > 0)
            return real_ip;

        if (request.remoteAddress() == null) {
            return null;
        }
        return request.remoteAddress().host();
    }

    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null)
            throw new NullPointerException(message);
        if (obj instanceof String && Strings.isNullOrEmpty((String) obj))
            throw new RuntimeException(message);
        return obj;
    }

    // helper method response json
    public static void responseSuccessJson(RoutingContext context) {
        responseJson(context, new JsonObject().put("code", 0).put("message", "success"), 200);
    }

    public static void responseJson(RoutingContext context, Object o) {
        responseJson(context, new JsonObject().put("code", 0).put("message", "success").put("data", o), 200);
    }

    public static void responseJson(RoutingContext context, int code, String message) {
        responseJson(context, code, message, 200);
    }

    public static void responseJson(RoutingContext context, int code, String message, int status_code) {
        responseJson(context, new JsonObject().put("code", code).put("message", message), status_code);
    }

    public static void responseJson(RoutingContext context, Object o, int status_code) {
        String response = Json.encode(o);
        context.response()
            .setStatusCode(status_code)
            .putHeader("Content-Type", "application/json")
            .putHeader("Date", DateUtil.formatGMTDate(DateTime.now()))
            .putHeader("Server", "IoT Gateway Server")
            .putHeader("X-Version", "1.0")
            .end(response);
    }

    // helper method dealing with failure
    public static void internalError(RoutingContext context, int code, String message) {
        serviceUnavailable(context, 500, code, message);
    }

    public static void internalError(RoutingContext context, String ex) {
        serviceUnavailable(context, 500, -1, ex);
    }

    public static void internalError(RoutingContext context, Throwable ex) {
        serviceUnavailable(context, 500, -1, ex.getMessage());
    }

    public static void notImplemented(RoutingContext context) {
        serviceUnavailable(context, 501, -1, "not_implemented");
    }

    public static void serviceUnavailable(RoutingContext context, String cause) {
        serviceUnavailable(context, 503, -1, cause);
    }

    public static void serviceUnavailable(RoutingContext context, int status, int code, String message) {
        responseJson(context, new JsonObject().put("code", code).put("message", message), status);
    }

    public static <T> Handler<AsyncResult<T>> succeeded(RoutingContext context, Handler<T> h2) {
        return ar -> {
            if (ar.succeeded()) {
                h2.handle(ar.result());
            } else {
                internalError(context, ar.cause());
            }
        };
    }

    public static <T> void succeeded(RoutingContext context, Future<T> future, Handler<T> h2) {
        future.onComplete(succeeded(context, h2));
    }

    public static <T> void succeeded(RoutingContext context, Future<T> future) {
        future.onComplete(succeeded(context, $ -> responseSuccessJson(context)));
    }

    public static <T> void succeededResponse(RoutingContext context, Future<T> future) {
        future.onComplete(succeeded(context, d -> responseJson(context, d)));
    }
}
