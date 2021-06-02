package com.ranqiyun.service.web.handlers;

import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Controller;
import com.ranqiyun.service.web.annotation.Params;
import com.ranqiyun.service.web.annotation.RequestMap;
import com.ranqiyun.service.web.common.ControllerBase;
import com.ranqiyun.service.web.services.LogService;
import com.ranqiyun.service.web.services.UserService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * @apiDefine user_manage 用户功能服务
 */
@Controller(value = "/user_manage", describe = "用户管理功能")
public class UserManageController extends ControllerBase {

    @AutowiredService
    private UserService userService;

    @AutowiredService
    private LogService logService;

    public UserManageController(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    @RequestMap(describe = "重置登录密码")
    public void reset_password(RoutingContext context,
                               @Params(value = "user_id", required = true) String user_id,
                               @Params(value = "password", required = true) String password) {
        userService.updatePassword(user_id, password)
            .onComplete(succeeded(context, ar -> {
                responseSuccessJson(context);
            }));
    }

    /**
     * @api {POST} /user_manage/update_mobile 更新用户手机号
     *
     * @apiGroup user_manage
     * @apiName update
     * @apiVersion 1.0.0
     *
     * @apiParam {String} user_id  用户ID
     * @apiParam {String} mobile  用户手机号
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "更新用户昵称")
    public void update_mobile(RoutingContext context,
                              @Params(value = "user_id", required = true) String user_id,
                              @Params(value = "mobile", required = true) String mobile) {
        userService.updateMobile(user_id, mobile)
            .onComplete(succeeded(context, ar -> {
                responseSuccessJson(context);
            }));
    }

    /**
     * @api {POST} /user_manage/update_name 更新用户昵称
     *
     * @apiGroup user_manage
     * @apiName update_name
     * @apiVersion 1.0.0
     *
     * @apiParam {String} user_id  用户ID
     * @apiParam {String} nickname  用户昵称
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "更新用户昵称")
    public void update_name(RoutingContext context,
                            @Params(value = "user_id", required = true) String user_id,
                            @Params(value = "nickname") String nickname) {
        userService.updateName(user_id, nickname)
            .onComplete(succeeded(context, ar -> {
                responseSuccessJson(context);
            }));
    }


    /**
     * @api {POST} /user_manage/create_user 创建用户
     *
     * @apiGroup user_manage
     * @apiName update_name
     * @apiVersion 1.0.0
     *
     * @apiParam {String} user_id  用户ID
     * @apiParam {String} nickname  用户昵称
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "更新用户昵称")
    public void create_user(RoutingContext context,
                            @Params(value = "mobile", required = true) String mobile,
                            @Params(value = "nickname") String nickname,
                            @Params(value = "password") String password,
                            @Params(value = "remark") String remark) {
        userService.createUser(mobile, nickname, password, remark)
            .onComplete(succeeded(context, user_id -> {
                responseSuccessJson(context);
            }));
    }
}
