package com.ranqiyun.service.web.services;

import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Service;
import com.ranqiyun.service.web.common.ServiceBase;
import com.ranqiyun.service.web.util.Utils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * Created by daijingjing on 2018/5/8.
 */
@Service
public class LogService extends ServiceBase {

    private static final String INSERT_BUSINESS_LOG = "INSERT INTO `log` (`user_id`, `relation_type`, `relation_id`, `resource`, `remark`) VALUES (?,?,?,?,?)";
    private static final String SELECT_LOG = "SELECT `user_id`, `relation_type`, `relation_id`, `resource`, `remark`, `create_date` FROM `log` WHERE `relation_id` = ? ORDER BY `id` DESC LIMIT ?,?";
    private static final String SELECT_LOG2 = "SELECT `user_id`, `relation_type`, `relation_id`, `resource`, `remark`, `create_date` FROM `log` WHERE `relation_type` = ? AND `relation_id` = ? ORDER BY `id` DESC LIMIT ?,?";
    private static final String SELECT_LOG3 = "SELECT `user_id`, `relation_type`, `relation_id`, `resource`, `remark`, `create_date` FROM `log` WHERE `user_id` = ? ORDER BY `id` DESC LIMIT ?,?";
    private static final String SELECT_LOG4 = "SELECT `user_id`, `relation_type`, `relation_id`, `resource`, `remark`, `create_date` FROM `log` ORDER BY `id` DESC LIMIT ?,?";

    @AutowiredService
    private MySQL mySQL;

    public LogService(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    public void log(String user_id, String relation_type, String relation_id, String resource, String message, Object... args) {
        log(user_id, relation_type, relation_id, resource, String.format(message, args));
    }

    public void log(String user_id, String relation_type, String relation_id, String resource, String remark) {
        mySQL.executeNoResult(Utils.buildArray(
            user_id,
            relation_type,
            relation_id,
            resource,
            remark), INSERT_BUSINESS_LOG)
            .onFailure(err -> logger.error("保存日志失败", err));
    }

    public Future<List<JsonObject>> getLogsByRelation(String relation_id, int offset, int max) {
        return mySQL.retrieveMany(Utils.buildArray(relation_id, offset, max), SELECT_LOG);
    }

    public Future<List<JsonObject>> getLogsByRelation(String relation_type, String relation_id, int offset, int max) {
        return mySQL.retrieveMany(Utils.buildArray(relation_type, relation_id, offset, max), SELECT_LOG2);
    }

    public Future<List<JsonObject>> getLogsByUser(String user_id, int offset, int max) {
        return mySQL.retrieveMany(Utils.buildArray(user_id, offset, max), SELECT_LOG3);
    }

    public Future<List<JsonObject>> getLogs(int offset, int max) {
        return mySQL.retrieveMany(Utils.buildArray(offset, max), SELECT_LOG4);
    }
}
