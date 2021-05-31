package com.ranqiyun.service.web.annotation;

import java.lang.annotation.*;

/**
 * Created by daijingjing on 2018/4/18.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Service {
    String describe() default "";
}
