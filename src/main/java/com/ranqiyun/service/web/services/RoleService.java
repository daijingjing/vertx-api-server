package com.ranqiyun.service.web.services;

import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Service;
import com.ranqiyun.service.web.common.ServiceBase;
import com.ranqiyun.service.web.util.Hash;
import com.ranqiyun.service.web.util.Utils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by daijingjing on 2018/4/19.
 */
@Service
public class RoleService extends ServiceBase {

    public static final String SELECT_ROLE = "SELECT `id`, `name`, `remark` FROM `role` ";
    public static final String SELECT_ALL = SELECT_ROLE + "ORDER BY name";
    public static final String SELECT_BY_ID = SELECT_ROLE + "WHERE `id` = ?";
    public static final String SELECT_BY_NAME = SELECT_ROLE + "WHERE name = ?";
    public static final String UPDATE_USER = "UPDATE `role` SET ";
    public static final String UPDATE_REMARK = UPDATE_USER + "`remark` = ? WHERE `id`= ?";
    public static final String UPDATE_NAME = UPDATE_USER + "`name` = ? WHERE `id`= ?";
    public static final String INSERT_ROLE = "INSERT INTO `role` (`id`, `name`, `remark`) VALUES (?,?,?)";
    public static final String DELETE_ROLE = "DELETE FROM `role` WHERE `id` = ?";

    public static final String SELECT_USER_ROLE = "SELECT  b.`id`, b.`name` FROM `role_user` a INNER JOIN `role` b ON a.`role_id` = b.`id` WHERE a.`user_id` = ?";
    public static final String SELECT_ROLE_USER = "SELECT  `user_id` FROM `role_user` WHERE `role_id` = ?";
    public static final String INSERT_ROLE_USER = "INSERT INTO `role_user` (`role_id`, `user_id`) VALUES (?,?)";
    public static final String REMOVE_ROLE_USER = "DELETE FROM `role_user` WHERE `role_id` = ? AND `user_id` = ?";

    @AutowiredService
    private MySQL mySQL;

    public RoleService(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    public Future<String> create(String name, String remark) {
        String id = newId();
        return mySQL.executeNoResult(Utils.buildArray(id, name, remark), INSERT_ROLE)
            .map(v -> id);
    }

    public Future<JsonObject> getById(String id) {
        return mySQL.retrieveOne(Utils.buildArray(id), SELECT_BY_ID)
            .map(v -> v.orElse(null));
    }

    public Future<JsonObject> getByName(String name) {
        return mySQL.retrieveOne(Utils.buildArray(name), SELECT_BY_NAME)
            .map(v -> v.orElse(null));
    }

    public Future<Void> updateRemark(String id, String remark) {
        return mySQL.executeNoResult(Utils.buildArray(remark, id), UPDATE_REMARK);
    }

    public Future<Void> updateName(String user_id, String name) {
        return mySQL.executeNoResult(Utils.buildArray(name, user_id), UPDATE_NAME);
    }

    public Future<Void> remove(String id) {
        return mySQL.executeNoResult(Utils.buildArray(id), DELETE_ROLE);
    }

    public Future<List<JsonObject>> list() {
        return mySQL.retrieveMany(SELECT_ALL);
    }

    public Future<Void> addUserRole(String role_id, String user_id) {
        return mySQL.executeNoResult(Utils.buildArray(role_id, user_id), INSERT_ROLE_USER);
    }

    public Future<Void> removeUserRole(String role_id, String user_id) {
        return mySQL.executeNoResult(Utils.buildArray(role_id, user_id), REMOVE_ROLE_USER);
    }

    public Future<List<JsonObject>> listUserRoles(String user_id) {
        return mySQL.retrieveMany(Utils.buildArray(user_id), SELECT_USER_ROLE);
    }

    public Future<List<String>> listRoleUsers(String role_id) {
        return mySQL.retrieveMany(Utils.buildArray(role_id), SELECT_ROLE_USER)
            .map(v -> v.stream().map(row -> row.getString("user_id")).collect(Collectors.toList()));
    }

}
