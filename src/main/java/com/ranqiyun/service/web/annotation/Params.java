package com.ranqiyun.service.web.annotation;

import java.lang.annotation.*;

/**
 * Created by daijingjing on 2018/4/18.
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Params {
    String value();
    boolean required() default false;
    String describe() default "";
}
