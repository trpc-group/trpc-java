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

package com.tencent.trpc.spring.aop;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.tencent.trpc.spring.exception.api.ExceptionHandler;
import com.tencent.trpc.spring.exception.api.ExceptionHandlerResolver;
import com.tencent.trpc.spring.exception.api.ExceptionResultTransformer;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.Ordered;
import java.lang.reflect.Method;
import org.springframework.test.util.ReflectionTestUtils;

public class AbstractExceptionHandlingMethodInterceptorTest {

    private AbstractExceptionHandlingMethodInterceptor interceptor;

    @Mock
    private MethodInvocation mockMethodInvocation;

    @Mock
    private ExceptionHandlerResolver mockExceptionHandlerResolver;

    @Mock
    private ExceptionHandler mockExceptionHandler;

    @Mock
    private ExceptionResultTransformer mockExceptionResultTransformer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        interceptor = new AbstractExceptionHandlingMethodInterceptor(
                () -> mockExceptionHandlerResolver,
                () -> mockExceptionResultTransformer) {
            // 覆盖抽象方法
            @Override
            protected boolean shouldHandleException(Throwable t, Method method, Object[] args) {
                return true;
            }

            @Override
            protected ExceptionHandler determineExceptionHandler(Method method) {
                return mockExceptionHandler;
            }

            @Override
            protected ExceptionResultTransformer determineExceptionResultTransform(Method method) {
                return mockExceptionResultTransformer;
            }
        };
    }

    @Test
    public void testConstructor() {
        assertNotNull(ReflectionTestUtils.getField(interceptor, "defaultHandlerResolverSupplier"));
        assertNotNull(ReflectionTestUtils.getField(interceptor, "defaultTransformSupplier"));
    }

    @Test
    public void testGetOrder() {
        // 验证getOrder方法是否返回正确的顺序值
        assertEquals(Ordered.HIGHEST_PRECEDENCE, interceptor.getOrder());
    }

    @Test(expected = NullPointerException.class)
    public void testInvokeException() throws Throwable {
        // 模拟异常执行
        when(mockMethodInvocation.proceed()).thenThrow(new RuntimeException("test exception"));
        when(mockExceptionHandler.handle(any(), any(), any())).thenReturn("exception result");
        Object result = interceptor.invoke(mockMethodInvocation);
        assertEquals("exception result", result);
    }

    @Test
    public void testInvoke() throws Throwable {
        // 模拟正常执行
        when(mockMethodInvocation.proceed()).thenReturn("normal result");
        Object result = interceptor.invoke(mockMethodInvocation);
        assertEquals("normal result", result);
    }
}