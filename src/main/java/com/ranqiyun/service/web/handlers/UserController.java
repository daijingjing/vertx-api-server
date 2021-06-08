package com.ranqiyun.service.web.handlers;

import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Controller;
import com.ranqiyun.service.web.annotation.Params;
import com.ranqiyun.service.web.annotation.RequestMap;
import com.ranqiyun.service.web.common.ControllerBase;
import com.ranqiyun.service.web.services.*;
import com.ranqiyun.service.web.util.ValidateCode;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * @apiDefine User 用户功能服务
 */
@Controller(value = "/user", describe = "用户功能")
public class UserController extends ControllerBase {

    @AutowiredService
    private UserService userService;
    @AutowiredService
    private RoleService roleService;
    @AutowiredService
    private OrgService orgService;

    @AutowiredService
    private LogService logService;

    @AutowiredService
    private ShortMessageService shortMessageService;

    public UserController(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    /**
     * @api {GET} /user/verify_img 获取图形验证码
     *
     * @apiGroup User
     * @apiName verify_img
     * @apiVersion 1.0.0
     *
     * @apiSuccess (Header) {String} Verifier-Sign 验证码签名
     * @apiSuccess {Hex} data 验证码图片数据
     */
    @RequestMap(describe = "获取图形验证码", anonymous = true)
    public void verify_img(RoutingContext context) {
        ValidateCode code = new ValidateCode();
        String codeSign = code.getSign();
        context.addCookie(Cookie.cookie("Verifier-Sign", codeSign));
        context.response()
            .putHeader("Cache-Control", "no-cache")
            .putHeader("Pragma", "no-cache")
            .putHeader("Expires", "0")
            .putHeader("Content-Type", "image/gif")
            .putHeader("Verifier-Sign", codeSign);
        ByteArrayOutputStream steam = new ByteArrayOutputStream();

        try {
            code.write(steam);
        } catch (IOException e) {
            e.printStackTrace();
        }

        context.response().end(Buffer.buffer(steam.toByteArray()));
    }


    /**
     * @api {POST} /user/login_request 发送短信验证码
     *
     * @apiGroup User
     * @apiName login_request
     * @apiPermission anonymous
     * @apiVersion 1.0.0
     *
     * @apiParam {String} mobile  手机号
     * @apiParam {String} verifySign 签名
     * @apiParam {String} verifyCode 验证码
     *
     * @apiSuccess {String} message 发送成功
     */
    @RequestMap(describe = "发送短信验证码", anonymous = true)
    public void login_request(RoutingContext context,
                              @Params(value = "mobile", describe = "手机号") String mobile,
                              @Params(value = "verifySign", describe = "图形验证码签名") String sign,
                              @Params(value = "verifyCode", describe = "图形验证码") String code) {
        if (ValidateCode.validateCode(sign, code)) {
            String sms_code = String.format("%04d", new Random().nextInt(9999));
            succeeded(context,
                userService.getUserByMobile(mobile)
                    .compose(user -> userService.saveVerifyCode(mobile, sms_code).map($ -> user)),
                user -> {
                    logger.info(String.format("[%s]请求短信验证码：%s", mobile, sms_code));
                    shortMessageService.send_verify_code(mobile, sms_code);
                    responseJson(context, 0, "验证码已发送至手机");
                });
        } else {
            internalError(context, 500, "图形验证码错误或过期，请重新获取");
        }
    }

    /**
     * @api {POST} /user/login 手机号验证码登录
     *
     * @apiGroup User
     * @apiName login
     * @apiVersion 1.0.0
     *
     * @apiParam {String} mobile  手机号
     * @apiParam {String} code 短信验证码
     * @apiParam {String} openid 微信OPENID
     *
     * @apiSuccess {String} token 令牌
     * @apiSuccess {String} expires 令牌有效期
     */
    @RequestMap(describe = "手机号验证码登录", anonymous = true)
    public void login(RoutingContext context,
                      @Params("code") String code,
                      @Params("mobile") String mobile,
                      @Params("openid") String openid) {
        String device = getClientAddress(context.request());

        succeeded(context,
            userService.verifyCode(mobile, code)
                .compose($ -> userService.getOrCreateUserByMobile(mobile))
                .compose(u -> userService.bindOpenId(u.getString("id"), openid))
                .compose(user_id -> {
                    logService.log(user_id, "用户登录", user_id, null, "用户通过手机号验证码登录，IP: %s", device);
                    return TokenService.get(user_id);
                }),
            token -> responseJson(context, new JsonObject().put("token", token)));
    }


    /**
     * @api {POST} /user/login_openid 微信登录
     *
     * @apiGroup User
     * @apiName login
     * @apiVersion 1.0.0
     *
     * @apiParam {String} openid 微信OPENID
     *
     * @apiSuccess {String} token 令牌
     * @apiSuccess {String} expires 令牌有效期
     */
    @RequestMap(describe = "微信登录", anonymous = true)
    public void login_openid(RoutingContext context,
                             @Params("openid") String openid) {
        String device = getClientAddress(context.request());

        succeeded(context,
            userService.getUserByOpenId(openid)
                .compose(user -> {
                    logService.log(user.getString("id"), "用户登录", user.getString("id"), null, "用户通过OPENID登录，IP: %s", device);
                    return TokenService.get(user.getString("id"));
                }),
            token -> responseJson(context, new JsonObject().put("token", token)));
    }


    /**
     * @api {POST} /user/login_password 手机号密码登录
     *
     * @apiGroup User
     * @apiName login_password
     * @apiVersion 1.0.0
     *
     * @apiParam {String} mobile 用户手机号
     * @apiParam {String} password 经过MD5加密的密码
     * @apiParam {String} verifySign 验证码签名
     * @apiParam {String} verifyCode 验证码
     *
     * @apiSuccess {String} token 令牌
     */
    @RequestMap(describe = "密码登录", anonymous = true)
    public void login_password(RoutingContext context,
                               @Params(value = "mobile", required = true) String mobile,
                               @Params(value = "password", required = true) String password,
                               @Params(value = "verifySign") String sign,
                               @Params(value = "verifyCode") String code) {
        String device = getClientAddress(context.request());
        if (ValidateCode.validateCode(sign, code)) {
            succeeded(context,
                userService.checkPassword(mobile, password)
                    .compose(user_id -> {
                        logService.log(user_id, "用户登录", user_id, null, "用户通过密码登录，IP: %s", device);
                        return TokenService.get(user_id);
                    }),
                token -> responseJson(context, new JsonObject().put("token", token)));
        } else {
            internalError(context, 500, "图形验证码错误或过期，请重新获取");
        }
    }

    /**
     * @api {POST} /user/info 当前用户个人信息
     *
     * @apiGroup User
     * @apiName info
     * @apiVersion 1.0.0
     *
     * @apiSuccess {String} id 用户ID
     * @apiSuccess {String} nickname 用户昵称
     * @apiSuccess {String} create_date 注册时间
     * @apiSuccess {Object[]} orgs 所属组织机构
     * @apiSuccess {String} orgs.id 企业ID
     * @apiSuccess {String} orgs.name 企业名称
     * @apiSuccess {Object[]} roles 用户角色
     */
    @RequestMap(describe = "当前用户信息")
    public void info(RoutingContext context) {
        succeededResponse(context, userService.getUser(currentUserId(context)).compose(user ->
            roleService.listUserRoles(user.getString("id")).map(roles -> user.put("roles", roles))
                .compose(v -> orgService.listUserOrgs(v.getString("id")).map(orgs -> v.put("orgs", orgs)))));
    }

    /**
     * @api {POST} /user/change_password 修改登录密码
     *
     * @apiGroup User
     * @apiName change_password
     * @apiVersion 1.0.0
     *
     * @apiParam {String} password  密码
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "修改登录密码")
    public void change_password(RoutingContext context,
                                @Params("password") String password) {
        succeeded(context, userService.updatePassword(currentUserId(context), password));
    }

    /**
     * @api {POST} /user/update_name 更新用户昵称
     *
     * @apiGroup User
     * @apiName update
     * @apiVersion 1.0.0
     *
     * @apiParam {String} nickname  用户昵称
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "更新用户昵称")
    public void update_name(RoutingContext context,
                            @Params("nickname") String nickname) {
        succeeded(context, userService.updateName(currentUserId(context), nickname));
    }

    /**
     * @api {POST} /user/unbind_openid 解绑用户OPENID
     *
     * @apiGroup User
     * @apiName update
     * @apiVersion 1.0.0
     *
     * @apiParam {String} openid  用户OPENID
     *
     * @apiSuccess {Number} code 0
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "解绑用户OPENID")
    public void unbind_openid(RoutingContext context,
                              @Params(value = "openid", required = true) String openid) {
        succeeded(context, userService.unbindOpenId(currentUserId(context), openid));
    }

    /**
     * @api {POST} /user/reset_password 重置密码
     *
     * @apiGroup User
     * @apiName reset_password
     * @apiVersion 1.0.0
     *
     * @apiParam {String} mobile  手机号
     * @apiParam {String} code 短信验证码
     * @apiParam {String} new_passwd 新密码
     *
     * @apiSuccess {String} message 操作成功
     */
    @RequestMap(describe = "重置密码", anonymous = true)
    public void reset_password(RoutingContext context,
                               @Params(value = "code", required = true) String code,
                               @Params(value = "mobile", required = true) String mobile,
                               @Params(value = "new_passwd", required = true) String new_passwd) {
        succeeded(context,
            userService.verifyCode(mobile, code)
                .compose($ -> userService.getUserByMobile(mobile))
                .compose(u -> userService.updatePassword(u.getString("id"), new_passwd)));
    }
}
