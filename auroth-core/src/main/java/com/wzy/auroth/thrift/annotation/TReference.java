package com.wzy.auroth.thrift.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务引用，用于引用thrift发布的服务
 *
 * created by wzy on 24/7/2017
 *
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Inherited
public @interface TReference {

    /**
     * 服务名称
     *
     * @return 服务名
     */
    String serviceId() default "";

    /**
     * 服务提供者ip
     *
     * 用于端到端的调试使用
     *
     * @return ip
     */
    String host() default "";

    /**
     * 服务提供者端口号
     *
     * 用于端到端的调式使用
     *
     * @return 端口号
     */
    int port() default 10010;

}
