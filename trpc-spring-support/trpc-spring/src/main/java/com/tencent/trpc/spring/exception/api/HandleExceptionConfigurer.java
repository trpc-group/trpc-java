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

package com.tencent.trpc.spring.exception.api;

/**
 * Implement this interface to provide customize {@link ExceptionResultTransformer}
 * and {@link ExceptionHandlerResolver}
 *
 * <p>Example:
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTRpcHandleException
 * public class MyHandleTRpcExceptionConfiguration implements HandleExceptionConfigurer {
 *
 *     &#064;Override
 *     public ExceptionResultTransform getCustomizedResultTransform() {
 *          return new MyExceptionResultTransform();
 *     }
 *
 *     &#064;Override
 *     public ExceptionHandlerResolver getCustomizedHandlerResolver() {
 *         return new MyServiceExceptionHandlerResolver();
 *     }
 * }</pre>
 */
public interface HandleExceptionConfigurer {

    /**
     * Provides custom {@link ExceptionResultTransformer}
     *
     * @return {@link ExceptionResultTransformer}
     */
    default ExceptionResultTransformer getCustomizedResultTransform() {
        return null;
    }

    /**
     * Provides custom {@link ExceptionHandlerResolver}
     *
     * @return {@link ExceptionHandlerResolver}
     */
    default ExceptionHandlerResolver getCustomizedHandlerResolver() {
        return null;
    }

}
