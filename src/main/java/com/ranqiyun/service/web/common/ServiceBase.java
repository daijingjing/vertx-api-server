package com.ranqiyun.service.web.common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public abstract class ServiceBase {

    protected final Vertx vertx;
    protected final JsonObject config;
    protected final Logger logger;

    public ServiceBase(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        logger = LoggerFactory.getLogger(this.getClass());
    }

    public void destroy() {
    }


    public static String newId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}
