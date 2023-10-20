/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company. 
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.spring.exception.annotation;

import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.spring.exception.api.ExceptionHandler;
import com.tencent.trpc.spring.exception.api.ExceptionHandlerResolver;
import com.tencent.trpc.spring.exception.api.ExceptionResultTransformer;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that exception thrown by annotated method(or methods of annotated class)
 * should be handled by {@link ExceptionHandler}s(or {@link TRpcExceptionHandler} annotated methods).
 * <p>Should be used on {@link TRpcService} classes or {@link TRpcMethod} methods.
 *
 * Example:
 * <pre class="code">
 * &#064;Service
 * &#064;TRpcHandleException
 * public class TRpcServiceApiImpl implements TRpcServiceApi {
 *
 *     &#064;Override
 *     public Response test(RpcContext context, Request request) {
 *          // do something
 *     }
 *
 *     &#064;Override
 *     &#064;TRpcHandleException(exclude = MyException.class)
 *     public Response doNotCatchMyException(RpcContext context, Request request) {
 *         // do something
 *     }
 * }</pre>
 *
 * @see TRpcService
 * @see TRpcMethod
 * @see ExceptionHandlerResolver
 * @see ExceptionHandler
 * @see ExceptionResultTransformer
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TRpcHandleException {

    /**
     * Exception types to be excluded.
     */
    Class<? extends Throwable>[] exclude() default {};

    /**
     * Set the explicit {@link ExceptionHandler} bean to be used.
     */
    String handler() default "";

    /**
     * Set the explicit {@link ExceptionResultTransformer} bean to be used.
     */
    String transform() default "";

}
