package com.ranqiyun.service.web.services;

import com.google.common.base.Strings;
import com.ranqiyun.service.web.annotation.AutowiredService;
import com.ranqiyun.service.web.annotation.Service;
import com.ranqiyun.service.web.common.ServiceBase;
import com.ranqiyun.service.web.util.DateUtil;
import com.ranqiyun.service.web.util.DesUtil;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.joda.time.DateTime;

@Service
public class TokenService extends ServiceBase {

    // 默认令牌有效期
    private static final int DEFAULT_EXPIRES = 24 * 60 * 1; // 1 天

    public TokenService(Vertx vertx, JsonObject config) {
        super(vertx, config);
    }

    /**
     * 生成新的令牌
     * @param user_id 用户ID
     * @return 令牌
     */
    public Future<String> get(String user_id) {
        return get(user_id, DEFAULT_EXPIRES);
    }

    /**
     * 生成新的令牌
     * @param user_id 用户ID
     * @param expires 过期时间，秒
     * @return 令牌
     */
    public Future<String> get(String user_id, int expires) {
        Token token = new Token(user_id, expires);
        try {
            return Future.succeededFuture(DesUtil.encrypt(token.encode()));
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    /**
     * 解析令牌
     * @param token 令牌
     * @return 用户ID
     */
    public Future<String> parse(String token) {
        return _parse(token)
            .compose(session -> {
                if (session.valid()) {
                    return Future.succeededFuture(session.getUserId());
                } else {
                    return Future.failedFuture("令牌无效或已过期");
                }
            });
    }

    /**
     * 刷新令牌
     * @param token 令牌
     * @return 新的令牌
     */
    public Future<String> refresh(String token) {
        return refresh(token, DEFAULT_EXPIRES);
    }

    /**
     * 刷新令牌
     * @param token 令牌
     * @param expires 过期时间，秒
     * @return 新的令牌
     */
    public Future<String> refresh(String token, int expires) {
        return _parse(token)
            .compose(session -> {
                if (session.valid()) {
                    return get(session.getUserId(), expires);
                } else {
                    return Future.failedFuture("令牌无效或已过期");
                }
            });
    }

    private Future<Token> _parse(String token) {
        try {
            return Future.succeededFuture(new Token(DesUtil.decrypt(token)));
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    public class Token extends JsonObject {
        private static final String USER_ID = "user_id";
        private static final String EXPIRES = "expires";

        public Token(String json) {
            super(json);
        }

        public Token(String userId, int expires) {
            super();
            this.put(USER_ID, userId);
            this.put(EXPIRES, DateUtil.formatDateTime(DateTime.now().plusMinutes(expires)));
        }

        public String getUserId() {
            return this.getString(USER_ID);
        }

        public boolean valid() {
            String expires = this.getString(EXPIRES);
            if (!Strings.isNullOrEmpty(expires)) {
                return DateUtil.parseDateTime(expires).isAfter(DateTime.now());
            }
            return false;
        }
    }

}
