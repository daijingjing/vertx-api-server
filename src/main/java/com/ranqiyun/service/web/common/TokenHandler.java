package com.ranqiyun.service.web.common;

import com.google.common.base.Strings;
import com.ranqiyun.service.web.services.TokenService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 *
 * 预处理HTTP请求的token信息，获取当前令牌的用户信息
 *
 * Created by daijingjing on 2018/5/10.
 */
public class TokenHandler extends ControllerBase implements Handler<RoutingContext> {

    public TokenHandler(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    @Override
    public void handle(RoutingContext context) {
        // 从HTTP Header中获取token
        String token = context.request().getHeader("Token");
        if (Strings.isNullOrEmpty(token)) {
            // 尝试从URL中获取token
            token = context.request().getParam("token");
        }

        if (token != null && token.length() > 0) {
            TokenService.parse(token)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        currentUserId(context, ar.result());
                    }
                    context.next();
                });
        } else {
            context.next();
        }
    }
}
