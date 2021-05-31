package com.ranqiyun.service.web.annotation;

import java.lang.annotation.*;

/**
 * Created by daijingjing on 2018/4/18.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMap {
    String value() default "";
    String describe() default "";
    boolean anonymous() default false;
    String[] role() default {};
}
