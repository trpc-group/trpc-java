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

import java.lang.reflect.Method;
import javax.annotation.Nullable;
import org.junit.Assert;
import org.junit.Test;

public class HandleExceptionConfigurerTest {

    private HandleExceptionConfigurer handleExceptionConfigurer;

    @Test
    public void testGetCustomizedResultTransform() {
        handleExceptionConfigurer = new MyHandleExceptionConfigurer();
        ExceptionResultTransformer customizedResultTransform = handleExceptionConfigurer.getCustomizedResultTransform();
        Assert.assertNull(customizedResultTransform);
        handleExceptionConfigurer = new MyHandleExceptionConfigurerWithImpl();

        customizedResultTransform = handleExceptionConfigurer.getCustomizedResultTransform();
        Assert.assertNotNull(customizedResultTransform);
    }

    @Test
    public void testGetCustomizedHandlerResolver() {
        handleExceptionConfigurer = new MyHandleExceptionConfigurer();
        ExceptionHandlerResolver customizedHandlerResolver = handleExceptionConfigurer.getCustomizedHandlerResolver();
        Assert.assertNull(customizedHandlerResolver);

        handleExceptionConfigurer = new MyHandleExceptionConfigurerWithImpl();
        customizedHandlerResolver = handleExceptionConfigurer.getCustomizedHandlerResolver();
        Assert.assertNotNull(customizedHandlerResolver);
    }

    static class MyHandleExceptionConfigurer implements HandleExceptionConfigurer {
    }

    static class MyHandleExceptionConfigurerWithImpl implements HandleExceptionConfigurer {

        @Override
        public ExceptionResultTransformer getCustomizedResultTransform() {
            return (result, targetType) -> new Object();
        }

        @Override
        public ExceptionHandlerResolver getCustomizedHandlerResolver() {
            return new ExceptionHandlerResolver() {
                @Nullable
                @Override
                public ExceptionHandler resolveExceptionHandler(Throwable t, @Nullable Object target,
                        @Nullable Method targetMethod) {
                    return new MyExceptionHandler();
                }
            };
        }

        static class MyExceptionHandler implements ExceptionHandler {

            @Nullable
            @Override
            public Object handle(Throwable t, @Nullable Method targetMethod, @Nullable Object[] arguments) {
                return new MyClass();
            }
        }

        static class MyClass {

        }
    }
}
