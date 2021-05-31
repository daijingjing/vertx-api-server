package com.ranqiyun.service.web.services;

import com.ranqiyun.service.web.annotation.Service;
import com.ranqiyun.service.web.common.ServiceBase;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

/**
 * Created by daijingjing on 2018/5/3.
 */
@Service
public class ShortMessageService extends ServiceBase {

    private final String service_url;
    private final String api_key;
    private final Long verify_code_tpl;

    public ShortMessageService(Vertx vertx, JsonObject config) {
        super(vertx, config);
        JsonObject conf = config.getJsonObject("sms", new JsonObject());

        service_url = conf.getString("service_url");
        api_key = conf.getString("api_key");
        verify_code_tpl = conf.getLong("verify_code_tpl", 2281860L);
    }

    @Override
    public void destroy() {

    }

    /**
     * 发送短信验证码
     *
     * @param mobile 接收手机号
     * @param code 短信验证码
     */
    public Future<Void> send_verify_code(String mobile, String code) {
        return send(mobile, verify_code_tpl, new JsonObject().put("#code#", code));
    }

    /**
     * 根据模板ID和参数，执行发送单条短信
     *
     * @param mobile
     * @param tpl_id
     * @param params
     *
     */
    protected Future<Void> send(String mobile, Long tpl_id, JsonObject params) {
        Promise<Void> promise = Promise.promise();

        QueryStringEncoder ps = new QueryStringEncoder("");
        params.forEach(k -> ps.addParam(k.getKey(), String.valueOf(k.getValue())));

        MultiMap form = MultiMap.caseInsensitiveMultiMap();
        form.set("apikey", api_key);
        form.set("mobile", mobile);
        form.set("tpl_id", String.valueOf(tpl_id));
        form.set("tpl_value", ps.toString().substring(1));

        WebClient.create(vertx)
            .postAbs(service_url)
            .sendForm(form)
            .onSuccess(response -> {
                if (response.statusCode() == 200) {
                    promise.complete();
                } else {
                    promise.fail(new RuntimeException(response.bodyAsString()));
                    logger.error("发送短信息失败", new RuntimeException(response.bodyAsString()));
                }
            })
            .onFailure(err -> {
                promise.fail(err);
                logger.error("发送短信息失败", err);
            });

        return promise.future();
    }
}
