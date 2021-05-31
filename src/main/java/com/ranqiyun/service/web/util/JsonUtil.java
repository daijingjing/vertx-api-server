package com.ranqiyun.service.web.util;

import com.google.common.base.Strings;
import io.vertx.core.json.JsonArray;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.ranqiyun.service.web.util.DateUtil.formatDateTime;


/**
 * Created by daijingjing on 2018/4/19.
 *
 * Json辅助功能代码
 */
public class JsonUtil {

    public static JsonArray buildJsonArray(Object... params) {
        JsonArray p = new JsonArray();

        for (Object param : params) {
            if (Objects.isNull(param)) {
                p.addNull();
            } else if (param instanceof String && Strings.isNullOrEmpty((String) param)) {
                p.addNull();
            } else if (param instanceof DateTime) {
                p.add(formatDateTime((DateTime) param));
            } else if (param instanceof Date) {
                p.add(formatDateTime(new DateTime(param)));
            } else {
                p.add(param);
            }
        }

        return p;
    }

    public static List<JsonArray> buildJsonArrayList(JsonArray... params) {
        return Arrays.stream(params).collect(Collectors.toList());
    }

}
