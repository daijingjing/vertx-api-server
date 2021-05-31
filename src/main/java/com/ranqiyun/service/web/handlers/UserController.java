package com.ranqiyun.service.web.handlers;

import com.ranqiyun.service.web.annotation.*;
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
@Controller(value = "/user", describe = "用户功能服务")
public class UserController extends ControllerBase {

    @AutowiredService
    private UserService userService;

    @AutowiredService
    private LogService logService;

    @AutowiredService
    private TokenService tokenService;

    @AutowiredService
    private ShortMessageService shortMessageService;

    @AutowiredService
    private SystemUserService systemUserService;

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
            userService.saveVerifyCode(mobile, sms_code)
                .compose(ar -> userService.getUserByMobile(mobile))
                .onComplete(succeeded(context, ar -> {
                    logger.info(String.format("[%s]请求短信验证码：%s", mobile, sms_code));
                    shortMessageService.send_verify_code(mobile, sms_code);
                    responseJson(context, 0, "验证码已发送至手机");
                }));
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

        userService.verifyCode(mobile, code)
            .compose($ -> userService.getOrCreateUserByMobile(mobile))
            .compose(u -> userService.bindOpenId(u.getString("id"), openid))
            .compose(user_id -> {
                logService.log(null, "用户登录", user_id, "用户通过手机号验证码登录，IP: %s", device);
                return tokenService.get(user_id);
            })
            .onComplete(succeeded(context, t -> responseJson(context, new JsonObject().put("token", t))));
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

        userService.getUserByOpenId(openid)
            .compose(user -> {
                logService.log(null, "用户登录", user.getString("id"),
                    "用户通过OPENID登录，IP: %s", device);

                return tokenService.get(user.getString("id"));
            })
            .onComplete(succeeded(context, t -> responseJson(context, new JsonObject().put("token", t))));
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

            userService.checkPassword(mobile, password)
                .compose(user_id -> {
                    logService.log(null, "用户", user_id, "用户通过密码登录，IP: %s", device);
                    return tokenService.get(user_id);
                })
                .onComplete(succeeded(context, t -> responseJson(context, new JsonObject().put("token", t))));
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
     * @apiSuccess {Object} company 用户企业
     * @apiSuccess {String} company.id 企业ID
     * @apiSuccess {String} company.name 企业名称
     * @apiSuccess {String} company.is_admin 是否企业管理员
     * @apiSuccess {String[]} auth 企业授予的权限
     */
    @RequestMap(describe = "当前用户信息")
    public void info(RoutingContext context) {
        userService.getUser(currentUserId(context))
            .onComplete(succeeded(context, user -> responseJson(context, user)));
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
        userService.updatePassword(currentUserId(context), password)
            .onComplete(succeeded(context, ar -> {
                logService.log(currentUserId(context), "用户", currentUserId(context), "修改登录密码");
                responseSuccessJson(context);
            }));
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
    @RequestMap(describe = "更新用户信息")
    public void update_name(RoutingContext context,
                            @Params("nickname") String nickname) {
        userService.updateName(currentUserId(context), nickname)
            .onComplete(succeeded(context, ar -> {
                logService.log(currentUserId(context), "用户", currentUserId(context), "修改用户昵称");
                responseSuccessJson(context);
            }));
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
        userService.unbindOpenId(currentUserId(context), openid)
            .onComplete(succeeded(context, ar -> {
                logService.log(currentUserId(context), "用户", currentUserId(context), "解绑用户OPENID");
                responseSuccessJson(context);
            }));
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
        userService.verifyCode(mobile, code)
            .compose($ -> userService.getUserByMobile(mobile))
            .compose(u -> userService.updatePassword(u.getString("id"), new_passwd)
                .onComplete(succeeded(context, ar -> {
                    logService.log(currentUserId(context), "用户", u.getString("id"), "重置密码");
                    responseSuccessJson(context);
                }))
            );
    }
}