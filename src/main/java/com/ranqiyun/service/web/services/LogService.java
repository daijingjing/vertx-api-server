package com.ranqiyun.service.web.services;

import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Service;
import com.ranqiyun.service.web.common.ServiceBase;
import com.ranqiyun.service.web.util.Utils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * Created by daijingjing on 2018/5/8.
 */
@Service
public class LogService extends ServiceBase {

    private static final String INSERT_BUSINESS_LOG = "INSERT INTO `business_log`(`user_id`, `relation_type`, `relation_id`, `remark`) VALUES (?,?,?,?)";
    private static final String DO_LOG_MESSAGE = LogService.class.getName() + ".do_log";
    public static final String SELECT_LOG = "SELECT `user_id`, `relation_type`, `create_date`, `remark` FROM `business_log` WHERE `relation_id` = ? ORDER BY `id` DESC LIMIT ?,?";
    public static final String SELECT_LOG2 = "SELECT `user_id`, `create_date`, `remark` FROM `business_log` WHERE `relation_type` = ? AND `relation_id` = ? ORDER BY `id` DESC LIMIT ?,?";

    @AutowiredService
    private MySQL mySQL;

    public LogService(Vertx vertx, JsonObject config) {
        super(vertx, config);

        vertx.eventBus().consumer(DO_LOG_MESSAGE, ar -> {
            JsonObject body = (JsonObject) ar.body();
            logger.info(Json.encodePrettily(body));
            mySQL.executeNoResult(Utils.buildArray(
                body.getString("user_id"),
                body.getString("relation_type"),
                body.getString("relation_id"),
                body.getString("remark")), INSERT_BUSINESS_LOG);
        });
    }

    public void log(String user_id, String relation_type, String relation_id, String message, Object... args) {
        log(user_id, relation_type, relation_id, String.format(message, args));
    }

    public void log(String user_id, String relation_type, String relation_id, String remark) {
        vertx.eventBus().send(DO_LOG_MESSAGE, new JsonObject()
            .put("user_id", user_id)
            .put("relation_type", relation_type)
            .put("relation_id", relation_id)
            .put("remark", remark));
    }

    public Future<List<JsonObject>> getLogs(String relation_id) {
        return getLogs(relation_id, 0, 100);
    }

    public Future<List<JsonObject>> getLogs(String relation_id, int offset, int max) {
        return mySQL.retrieveMany(Utils.buildArray(relation_id, offset, max), SELECT_LOG);
    }

    public Future<List<JsonObject>> getLogs(String relation_type, String relation_id) {
        return getLogs(relation_type, relation_id, 0, 100);
    }

    public Future<List<JsonObject>> getLogs(String relation_type, String relation_id, int offset, int max) {
        return mySQL.retrieveMany(Utils.buildArray(relation_type, relation_id, offset, max), SELECT_LOG2);
    }
}
