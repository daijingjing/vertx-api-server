package com.ranqiyun.service.web.handlers;

import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Controller;
import com.ranqiyun.service.web.annotation.Params;
import com.ranqiyun.service.web.annotation.RequestMap;
import com.ranqiyun.service.web.common.ControllerBase;
import com.ranqiyun.service.web.services.LogService;
import com.ranqiyun.service.web.services.RoleService;
import com.ranqiyun.service.web.services.UserService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Objects;

import static com.ranqiyun.service.web.util.Utils.transform;

/**
 * @apiDefine user_manage 用户功能服务
 */
@Controller(value = "/user_manage", describe = "用户管理功能")
public class UserManageController extends ControllerBase {

    @AutowiredService
    private UserService userService;
    @AutowiredService
    private RoleService roleService;

    public UserManageController(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    /**
     * @api {POST} /user_manage/reset_password 重置用户登录密码
     *
     * @apiGroup user_manage
     * @apiName reset_password
     * @apiVersion 1.0.0
     *
     * @apiParam {String} user_id  用户ID
     * @apiParam {String} password  登录密码
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "重置用户登录密码")
    public void reset_password(RoutingContext context,
                               @Params(value = "user_id", required = true) String user_id,
                               @Params(value = "password", required = true) String password) {
        succeeded(context, userService.updatePassword(user_id, password));
    }

    /**
     * @api {POST} /user_manage/update_mobile 更新用户手机号
     *
     * @apiGroup user_manage
     * @apiName update_mobile
     * @apiVersion 1.0.0
     *
     * @apiParam {String} user_id  用户ID
     * @apiParam {String} mobile  用户手机号
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "更新用户手机号")
    public void update_mobile(RoutingContext context,
                              @Params(value = "user_id", required = true) String user_id,
                              @Params(value = "mobile", required = true) String mobile) {
        succeeded(context, userService.updateMobile(user_id, mobile));
    }

    /**
     * @api {POST} /user_manage/update_name 更新用户昵称
     *
     * @apiGroup user_manage
     * @apiName update_name
     * @apiVersion 1.0.0
     *
     * @apiParam {String} nickname  用户昵称
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "更新用户昵称")
    public void update_name(RoutingContext context,
                            @Params(value = "user_id", required = true) String user_id,
                            @Params(value = "nickname") String nickname) {
        succeeded(context, userService.updateName(user_id, nickname));
    }


    /**
     * @api {POST} /user_manage/create 创建用户
     *
     * @apiGroup user_manage
     * @apiName create
     * @apiVersion 1.0.0
     *
     * @apiParam {String} mobile  手机号
     * @apiParam {String} nickname  昵称
     * @apiParam {String} password  登录密码
     * @apiParam {String} remark  备注
     *
     * @apiSuccess {String} data 用户ID
     */
    @RequestMap(describe = "更新用户昵称")
    public void create(RoutingContext context,
                       @Params(value = "mobile", required = true) String mobile,
                       @Params(value = "nickname") String nickname,
                       @Params(value = "password") String password,
                       @Params(value = "remark") String remark) {
        succeededResponse(context, userService.createUser(mobile, nickname, password, remark));
    }


    /**
     * @api {POST} /user_manage/delete_user 删除用户
     *
     * @apiGroup user_manage
     * @apiName update_name
     * @apiVersion 1.0.0
     *
     * @apiParam {String} user_id  用户ID
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "更新用户昵称")
    public void delete_user(RoutingContext context,
                            @Params(value = "user_id", required = true) String user_id) {
        succeeded(context, userService.remove(user_id));
    }

    /**
     * @api {POST} /user_manage/list_user 用户列表
     *
     * @apiGroup user_manage
     * @apiName list_user
     * @apiVersion 1.0.0
     *
     * @apiParam {Number} offset  offset
     * @apiParam {Number} page_size  pageSize
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "用户列表")
    public void list_user(RoutingContext context,
                          @Params(value = "offset") Integer offset,
                          @Params(value = "page_size") Integer pageSize) {
        succeededResponse(context,
            transform(userService.list(Objects.nonNull(offset) ? offset : 0, Objects.nonNull(pageSize) ? pageSize : 20),
                user -> roleService.listUserRoles(user.getString("id")).map(roles -> user.put("roles", roles))));
    }
}
