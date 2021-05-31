package com.ranqiyun.service.web.services;

import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Service;
import com.ranqiyun.service.web.common.ServiceBase;
import com.ranqiyun.service.web.util.Hash;
import com.ranqiyun.service.web.util.Utils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Objects;

/**
 * Created by daijingjing on 2018/4/19.
 */
@Service
public class UserService extends ServiceBase {

    public static final String SELECT_USER = "SELECT `id`, `mobile`, `nickname`, `create_date`, `remark` FROM `employee` ";
    public static final String SELECT_BY_ID = SELECT_USER + "WHERE `id` = ?";
    public static final String SELECT_BY_MOBILE = SELECT_USER + "WHERE mobile = ?";
    public static final String UPDATE_USER = "UPDATE `employee` SET ";
    public static final String UPDATE_REMARK = UPDATE_USER + "`remark`= ? WHERE `id`= ?";
    public static final String UPDATE_NAME = UPDATE_USER + "`nickname`=? WHERE `id`= ?";
    public static final String UPDATE_MOBILE = UPDATE_USER + "`mobile`=? WHERE `id`= ?";
    public static final String UPDATE_PASSWORD = UPDATE_USER + "`password`=? WHERE `id` = ?";
    public static final String INSERT_USER = "INSERT INTO `employee`(`id`, `mobile`, `password`, `nickname`, `remark`) VALUES (?,?,?,?,?)";

    public static final String SELECT_OPENID = "SELECT `openid` FROM `employee_openid` WHERE `employee_id` = ?";
    public static final String DELETE_OPENID = "DELETE FROM `employee_openid` WHERE `employee_id` = ? AND `openid` = ?";
    public static final String INSERT_OPENID = "INSERT INTO `employee_openid`(`employee_id`, `openid`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `openid` = ?";
    public static final String SELECT_USER_ID = "SELECT `employee_id` FROM `employee_openid` WHERE `openid` = ?";
    public static final String SELECT_ID_PASSWORD = "SELECT `id`, `password` FROM `employee` WHERE `mobile`= ?";
    public static final String DELETE_USER = "DELETE FROM `employee` WHERE `id` = ?";

    @AutowiredService
    private MySQL jdbcWrapper;

    @AutowiredService
    private CachedService cachedService;

    public UserService(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    public Future<String> createUser(String mobile, String name, String password, String remark) {
        String user_id = newId();
        return jdbcWrapper.executeNoResult(Utils.buildArray(user_id, mobile, Hash.md5(password), name, remark), INSERT_USER)
            .map(v -> user_id);
    }

    public Future<JsonObject> getUser(String user_id) {
        return jdbcWrapper.retrieveOne(Utils.buildArray(user_id), SELECT_BY_ID)
            .map(v -> v.orElse(null));
    }

    public Future<JsonObject> getOrCreateUserByMobile(String mobile) {
        return jdbcWrapper.retrieveOne(Utils.buildArray(mobile), SELECT_BY_MOBILE)
            .compose(user -> user.map(Future::succeededFuture).orElseGet(() ->
                createUser(mobile, null, null, null)
                    .compose(this::getUser)));
    }

    public Future<JsonObject> getUserByMobile(String mobile) {
        return jdbcWrapper.retrieveOne(Utils.buildArray(mobile), SELECT_BY_MOBILE)
            .compose(user -> user.map(Future::succeededFuture).orElseGet(() -> Future.failedFuture("用户不存在,请联系管理员！")));
    }

    public Future<Void> updateRemark(String user_id, String remark) {
        return jdbcWrapper.executeNoResult(Utils.buildArray(remark, user_id), UPDATE_REMARK);
    }

    public Future<Void> updateName(String user_id, String name) {
        return jdbcWrapper.executeNoResult(Utils.buildArray(name, user_id), UPDATE_NAME);
    }

    public Future<Void> updateMobile(String user_id, String mobile) {
        return jdbcWrapper.executeNoResult(Utils.buildArray(mobile, user_id), UPDATE_MOBILE);
    }

    public Future<String> checkPassword(String mobile, String password) {
        return jdbcWrapper.retrieveOne(Utils.buildArray(mobile), SELECT_ID_PASSWORD)
            .compose(user -> user.map(Future::succeededFuture).orElseGet(() -> Future.failedFuture("用户不存在")))
            .compose(v -> {
                String cur_pass = v.getString("password");
                if (Objects.nonNull(cur_pass) && cur_pass.equalsIgnoreCase(Hash.md5(password))) {
                    return Future.succeededFuture(v.getString("id"));
                } else {
                    return Future.failedFuture("密码错误");
                }
            });
    }

    public Future<Void> updatePassword(String user_id, String password) {
        return jdbcWrapper.executeNoResult(Utils.buildArray(Hash.md5(password).toLowerCase(), user_id), UPDATE_PASSWORD);
    }

    public void remove(String user_id) {
        jdbcWrapper.executeNoResult(Utils.buildArray(user_id), DELETE_USER);
    }

    public Future<Void> saveVerifyCode(String mobile, String sms_code) {
        return cachedService.savePublicData("login_request_sms_code", mobile, sms_code, 90);
    }

    public Future<Void> verifyCode(String mobile, String code) {
        return cachedService.getPublicData("login_request_sms_code", mobile)
            .compose(save_code -> code.equalsIgnoreCase(save_code) ? Future.succeededFuture() : Future.failedFuture("验证码不正确"));
    }

    public Future<String> getUserOpenId(String user_id) {
        return jdbcWrapper.retrieveOne(Utils.buildArray(user_id), SELECT_OPENID)
            .compose(user -> user.map(Future::succeededFuture).orElseGet(() -> Future.failedFuture("用户未绑定OPENID")))
            .map(user -> user.getString("openid"));
    }

    public Future<JsonObject> getUserByOpenId(String openid) {
        return jdbcWrapper.retrieveOne(Utils.buildArray(openid), SELECT_USER_ID)
            .compose(user -> user.map(v -> Future.succeededFuture(v.getString("employee_id"))).orElseGet(() -> Future.failedFuture("用户未绑定OPENID")))
            .compose(this::getUser);
    }

    public Future<String> bindOpenId(String user_id, String openid) {
        return jdbcWrapper.executeNoResult(Utils.buildArray(user_id, openid, openid), INSERT_OPENID)
            .map(v -> user_id);
    }

    public Future<Void> unbindOpenId(String user_id, String openid) {
        return jdbcWrapper.executeNoResult(Utils.buildArray(user_id, openid), DELETE_OPENID);
    }
}
