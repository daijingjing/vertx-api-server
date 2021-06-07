package com.ranqiyun.service.web.handlers;

import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Controller;
import com.ranqiyun.service.web.annotation.Params;
import com.ranqiyun.service.web.annotation.RequestMap;
import com.ranqiyun.service.web.common.ControllerBase;
import com.ranqiyun.service.web.services.RoleService;
import com.ranqiyun.service.web.services.UserService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static com.ranqiyun.service.web.util.Utils.transform;

/**
 * @apiDefine role 用户功能服务
 */
@Controller(value = "/role", describe = "角色管理功能")
public class RoleController extends ControllerBase {

    @AutowiredService
    private RoleService roleService;

    @AutowiredService
    private UserService userService;

    public RoleController(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    /**
     * @api {POST} /role/update_name 更新角色名称
     *
     * @apiGroup role
     * @apiName update_name
     * @apiVersion 1.0.0
     *
     * @apiParam {String} id  ID
     * @apiParam {String} name  名称
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "更新角色名称")
    public void update_name(RoutingContext context,
                            @Params(value = "id", required = true) String id,
                            @Params(value = "name") String name) {
        succeeded(context, roleService.updateName(id, name));
    }

    /**
     * @api {POST} /role/update_remark 更新角色备注
     *
     * @apiGroup role
     * @apiName update_name
     * @apiVersion 1.0.0
     *
     * @apiParam {String} id  ID
     * @apiParam {String} remark  备注
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "更新角色备注")
    public void update_remark(RoutingContext context,
                              @Params(value = "id", required = true) String id,
                              @Params(value = "remark") String remark) {
        succeeded(context, roleService.updateRemark(id, remark));
    }


    /**
     * @api {POST} /role/create 创建角色
     *
     * @apiGroup role
     * @apiName update_name
     * @apiVersion 1.0.0
     *
     * @apiParam {String} name  名称
     * @apiParam {String} remark  备注
     *
     * @apiSuccess {String} data 角色ID
     */
    @RequestMap(describe = "创建角色")
    public void create(RoutingContext context,
                       @Params(value = "name", required = true) String name,
                       @Params(value = "remark") String remark) {
        succeededResponse(context, roleService.create(name, remark));
    }


    /**
     * @api {POST} /role/delete_user 删除用户
     *
     * @apiGroup role
     * @apiName update_name
     * @apiVersion 1.0.0
     *
     * @apiParam {String} user_id  用户ID
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "更新用户昵称")
    public void delete(RoutingContext context,
                       @Params(value = "id", required = true) String id) {
        succeeded(context, roleService.remove(id));
    }


    /**
     * @api {POST} /role/add_role_user 添加角色用户
     *
     * @apiGroup role
     * @apiName add_role_user
     * @apiVersion 1.0.0
     *
     * @apiParam {String} role_id  角色ID
     * @apiParam {String} user_id  用户ID
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "添加角色用户")
    public void add_role_user(RoutingContext context,
                              @Params(value = "role_id", required = true) String role_id,
                              @Params(value = "user_id", required = true) String user_id) {
        succeeded(context, roleService.addUserRole(role_id, user_id));
    }

    /**
     * @api {POST} /role/remove_role_user 移除角色用户
     *
     * @apiGroup role
     * @apiName remove_role_user
     * @apiVersion 1.0.0
     *
     * @apiParam {String} role_id  角色ID
     * @apiParam {String} user_id  用户ID
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "移除角色用户")
    public void remove_role_user(RoutingContext context,
                                 @Params(value = "role_id", required = true) String role_id,
                                 @Params(value = "user_id", required = true) String user_id) {
        succeeded(context, roleService.removeUserRole(role_id, user_id));
    }

    /**
     * @api {POST} /role/role_users 角色用户列表
     *
     * @apiGroup role
     * @apiName role_users
     * @apiVersion 1.0.0
     *
     * @apiParam {String} role_id  角色ID
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "角色用户列表")
    public void role_users(RoutingContext context,
                               @Params(value = "role_id", required = true) String role_id) {
        succeededResponse(context, transform(roleService.listRoleUsers(role_id),
            user_id -> userService.getUser(user_id)));
    }

    /**
     * @api {POST} /role/list 角色列表
     *
     * @apiGroup role
     * @apiName list_user
     * @apiVersion 1.0.0
     *
     * @apiSuccess {Object[]} data 角色列表
     * @apiSuccess {String} data.id ID
     * @apiSuccess {String} data.name 名称
     * @apiSuccess {String} data.remark 备注
     */
    @RequestMap(describe = "角色列表")
    public void list(RoutingContext context) {
        succeededResponse(context, roleService.list());
    }
}
