/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 Tencent.
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.spring.exception.annotation;

import com.tencent.trpc.spring.exception.support.DefaultExceptionHandlerResolver;
import com.tencent.trpc.spring.exception.support.DefaultExceptionResultTransformer;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.tencent.trpc.spring.exception.support.HandleExceptionConfiguration;
import com.tencent.trpc.spring.exception.support.HandleExceptionInterceptor;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Enables tRPC exception handling mechanism. Used on @{@link Configuration} classes as follows:
 *
 * <pre class="code">
 * &#064;Slf4j
 * &#064;Configuration
 * &#064;EnableTRpcHandleException
 * public class MyHandleExceptionConfiguration {
 *
 *     &#064;TRpcExceptionHandler
 *     public MyResponse handleTRpcException(TRpcException e, Method method, Message request) {
 *         log.error("service encountered TRpcException method={} request={}", method, request, e);
 *         int code = e.isFrameException() ? e.getCode() : e.getBizCode();
 *         return MyResponse.of(code, e.getMessage());
 *     }
 *
 *     &#064;TRpcExceptionHandler
 *     public MyResponse handleException(Exception e, Method method, Message request) {
 *         log.error("service encountered Exception method={} request={}", method, request, e);
 *         return MyResponse.of(999, e.getMessage());
 *     }
 *
 * }</pre>
 *
 * @see HandleExceptionInterceptor
 * @see DefaultExceptionHandlerResolver
 * @see DefaultExceptionResultTransformer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(HandleExceptionConfiguration.class)
public @interface EnableTRpcHandleException {

    /**
     * Whether to use CGLIB instead of JDK dynamic proxy
     */
    boolean proxyTargetClass() default false;

    /**
     * Whether to expose proxy instance by storing it in {@link ThreadLocal}.
     * If true, proxy instance can be acquired via {@link AopContext#currentProxy()}.
     */
    boolean exposeProxy() default false;

}
