package com.ranqiyun.service.web.common;

import io.vertx.core.json.JsonObject;

import java.util.UUID;

public class ModelBase extends JsonObject {

    @Override
    public String toString() {
        return this.encodePrettily();
    }

    public static String newId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}
