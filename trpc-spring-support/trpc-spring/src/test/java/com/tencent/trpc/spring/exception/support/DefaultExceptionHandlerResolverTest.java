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

package com.tencent.trpc.spring.exception.support;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.tencent.trpc.spring.exception.api.ExceptionHandler;
import com.tencent.trpc.spring.exception.api.ExceptionHandlerResolver;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;

public class DefaultExceptionHandlerResolverTest {


    @Mock
    private DefaultExceptionHandlerResolver resolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        resolver = new DefaultExceptionHandlerResolver();
    }

    @Test
    public void testResolveExceptionHandlerWithTargetBean() {
        Object targetBean = new Object();
        Method targetMethod = getMethod(Object.class, "hashCode"); // 选择一个Object类的方法作为测试方法
        Throwable throwable = new Exception();
        ExceptionHandler result = resolver.resolveExceptionHandler(throwable, targetBean, targetMethod);
        assertNull(result);

        AnnotationExceptionHandlerResolver mockResolver = Mockito.mock(AnnotationExceptionHandlerResolver.class);
        ExceptionHandler mockExceptionHandler = Mockito.mock(ExceptionHandler.class);
        Mockito.when(mockResolver.resolveExceptionHandler(throwable, targetBean, targetMethod))
                .thenReturn(mockExceptionHandler);
        ConcurrentHashMap<Class<?>, ExceptionHandlerResolver> specificResolvers = new ConcurrentHashMap<>();
        specificResolvers.put(ClassUtils.getUserClass(targetBean), mockResolver);
        ReflectionTestUtils.setField(resolver, "specificResolvers", specificResolvers);
        result = resolver.resolveExceptionHandler(throwable, targetBean, targetMethod);
        assertSame(mockExceptionHandler, result);
    }

    private Method getMethod(Class<?> clazz, String name) {
        try {
            return clazz.getMethod(name);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
