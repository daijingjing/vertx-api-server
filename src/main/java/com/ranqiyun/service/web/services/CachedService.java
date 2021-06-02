package com.ranqiyun.service.web.services;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.ranqiyun.service.web.annotation.Service;
import com.ranqiyun.service.web.common.ServiceBase;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;

import java.util.Objects;

/**
 * Created by daijingjing on 2018/4/19.
 */
@Service
public class CachedService extends ServiceBase {

    private final Redis redisClient;

    public CachedService(Vertx vertx, JsonObject config) {
        super(vertx, config);
        redisClient = Redis.createClient(vertx, new RedisOptions(config.getJsonObject("redis", new JsonObject())));
    }

    @Override
    public void destroy() {
        redisClient.close();
    }

    private String privateKey(String owner_id, String type, String id) {
        return "private_data." + Strings.nullToEmpty(type) + "." + owner_id + "." + id;
    }

    public Future<Void> savePrivateData(String owner_id, String type, String id, String data) {
        if (Strings.isNullOrEmpty(data)) {
            return del(privateKey(owner_id, type, id));
        } else {
            return set(privateKey(owner_id, type, id), data);
        }
    }

    public Future<String> getPrivateData(String owner_id, String type, String id) {
        return get(privateKey(owner_id, type, id));
    }

    private String publicKey(String type, String id) {
        return "public_data." + Strings.nullToEmpty(type) + "." + id;
    }

    public Future<Void> savePublicData(String type, String id, String data) {
        return set(publicKey(type, id), data);
    }


    public Future<Void> savePublicData(String type, String id, String data, int timeout_s) {
        String key = publicKey(type, id);
        return set(key, data)
            .compose(v -> expire(key, timeout_s));
    }

    public Future<String> getPublicData(String type, String id) {
        return get(publicKey(type, id));
    }

    public Future<String> get(String key) {
        return RedisAPI.api(redisClient).get(key).map(v -> Objects.isNull(v) ? null : v.toString());
    }

    public Future<Void> expire(String key, int timeout) {
        return RedisAPI.api(redisClient).expire(key, String.valueOf(timeout))
            .mapEmpty();
    }

    public Future<Void> set(String key, String value) {
        return RedisAPI.api(redisClient)
            .set(Lists.newArrayList(key, value))
            .mapEmpty();
    }

    public Future<Void> set(String key, String value, int timeout) {
        return set(key, value)
            .compose(v -> expire(key, timeout));
    }

    public Future<Void> del(String key) {
        return RedisAPI.api(redisClient).del(Lists.newArrayList(key))
            .mapEmpty();
    }
}
