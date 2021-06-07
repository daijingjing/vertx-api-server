package com.ranqiyun.service.web.handlers;

import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Controller;
import com.ranqiyun.service.web.annotation.Params;
import com.ranqiyun.service.web.annotation.RequestMap;
import com.ranqiyun.service.web.common.ControllerBase;
import com.ranqiyun.service.web.services.OrgService;
import com.ranqiyun.service.web.services.UserService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static com.ranqiyun.service.web.util.Utils.transform;

/**
 * @apiDefine org 组织机构管理功能
 */
@Controller(value = "/org", describe = "组织机构管理功能")
public class OrgController extends ControllerBase {

    @AutowiredService
    private OrgService orgService;

    @AutowiredService
    private UserService userService;

    public OrgController(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    /**
     * @api {POST} /org/update_name 更新组织机构名称
     *
     * @apiGroup org
     * @apiName update_name
     * @apiVersion 1.0.0
     *
     * @apiParam {String} id  ID
     * @apiParam {String} name  名称
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "更新组织机构名称")
    public void update_name(RoutingContext context,
                            @Params(value = "id", required = true) String id,
                            @Params(value = "name") String name) {
        succeeded(context, orgService.updateName(id, name));
    }

    /**
     * @api {POST} /org/update_remark 更新角色备注
     *
     * @apiGroup org
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
        succeeded(context, orgService.updateRemark(id, remark));
    }

    /**
     * @api {POST} /org/update_remark 更新角色备注
     *
     * @apiGroup org
     * @apiName update_name
     * @apiVersion 1.0.0
     *
     * @apiParam {String} id  ID
     * @apiParam {String} remark  备注
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "更新角色备注")
    public void update_parent(RoutingContext context,
                              @Params(value = "id", required = true) String id,
                              @Params(value = "pid") String pid) {
        succeeded(context, orgService.updateParent(id, pid));
    }


    /**
     * @api {POST} /org/create 创建组织机构
     *
     * @apiGroup org
     * @apiName create
     * @apiVersion 1.0.0
     *
     * @apiParam {String} pid  上级组织机构ID
     * @apiParam {String} name  名称
     * @apiParam {String} remark  备注
     *
     * @apiSuccess {String} data 组织机构ID
     */
    @RequestMap(describe = "创建组织机构")
    public void create(RoutingContext context,
                       @Params(value = "name", required = true) String name,
                       @Params(value = "pid") String pid,
                       @Params(value = "remark") String remark) {
        succeededResponse(context, orgService.create(pid, name, remark));
    }


    /**
     * @api {POST} /org/delete 删除组织机构
     *
     * @apiGroup org
     * @apiName delete
     * @apiVersion 1.0.0
     *
     * @apiParam {String} org_id  组织机构ID
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "删除组织机构")
    public void delete(RoutingContext context,
                       @Params(value = "org_id", required = true) String org_id,
                       @Params(value = "contain_children") Boolean contain_children) {
        succeeded(context, orgService.remove(org_id,contain_children));
    }


    /**
     * @api {POST} /org/add_role_user 添加角色用户
     *
     * @apiGroup org
     * @apiName add_role_user
     * @apiVersion 1.0.0
     *
     * @apiParam {String} org_id  组织机构ID
     * @apiParam {String} user_id  用户ID
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "添加角色用户")
    public void add_role_user(RoutingContext context,
                              @Params(value = "org_id", required = true) String org_id,
                              @Params(value = "user_id", required = true) String user_id) {
        succeeded(context, orgService.addUserOrg(org_id, user_id));
    }

    /**
     * @api {POST} /org/remove_role_user 移除角色用户
     *
     * @apiGroup org
     * @apiName remove_role_user
     * @apiVersion 1.0.0
     *
     * @apiParam {String} org_id  组织机构ID
     * @apiParam {String} user_id  用户ID
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "移除角色用户")
    public void remove_org_user(RoutingContext context,
                                @Params(value = "org_id", required = true) String org_id,
                                @Params(value = "user_id", required = true) String user_id) {
        succeeded(context, orgService.removeUserOrg(org_id, user_id));
    }

    /**
     * @api {POST} /org/org_users 角色用户列表
     *
     * @apiGroup org
     * @apiName org_users
     * @apiVersion 1.0.0
     *
     * @apiParam {String} org_id  组织机构ID
     *
     * @apiSuccess {Object[]} data 用户列表
     */
    @RequestMap(describe = "角色用户列表")
    public void org_users(RoutingContext context,
                          @Params(value = "org_id", required = true) String org_id) {
        succeededResponse(context, transform(orgService.listOrgUsers(org_id),
            user_id -> userService.getUser(user_id)));
    }

    /**
     * @api {POST} /org/list 组织机构列表
     *
     * @apiGroup org
     * @apiName list
     * @apiVersion 1.0.0
     *
     * @apiSuccess {Object[]} data 组织机构列表
     * @apiSuccess {String} data.id ID
     * @apiSuccess {String} data.pid 上级机构ID
     * @apiSuccess {String} data.name 名称
     * @apiSuccess {String} data.remark 备注
     */
    @RequestMap(describe = "组织机构列表")
    public void list(RoutingContext context) {
        succeededResponse(context, orgService.list());
    }
}
