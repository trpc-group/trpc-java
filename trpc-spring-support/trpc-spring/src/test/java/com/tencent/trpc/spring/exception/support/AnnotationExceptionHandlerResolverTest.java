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

import com.tencent.trpc.spring.exception.api.ExceptionHandler;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for AnnotationExceptionHandlerResolver
 */
public class AnnotationExceptionHandlerResolverTest {

    /**
     * Ambiguous ExceptionHandlers
     */
    @Test
    public void testDetectExceptionHandlers() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            MyAnnotationExceptionHandlerResolver resolver = new MyAnnotationExceptionHandlerResolver();
            resolver.detectExceptionHandlers(new MyHandlerBean());
            Assertions.fail();
        });
    }

    /**
     * Test handle NullPointerException
     */
    @Test
    public void testNullPointerException() throws Throwable {
        MyAnnotationExceptionHandlerResolver resolver = new MyAnnotationExceptionHandlerResolver();
        resolver.setAllowOverrideExceptionHandler(true);
        resolver.detectExceptionHandlers(new MyHandlerBean());
        ExceptionHandler handler = resolver.resolveExceptionHandler(new NullPointerException(), null, null);
        Object result = handler.handle(new NullPointerException(), null, null);
        Assertions.assertEquals(true, result.toString().contains("handle"));
    }

    /**
     * Test handle IllegalArgumentException
     */
    @Test
    public void testIllegalArgumentException() throws Throwable {
        MyAnnotationExceptionHandlerResolver resolver = new MyAnnotationExceptionHandlerResolver();
        resolver.setAllowOverrideExceptionHandler(true);
        resolver.detectExceptionHandlers(new MyHandlerBean());
        ExceptionHandler handler = resolver.resolveExceptionHandler(new IllegalArgumentException(), null, null);
        Object result = handler.handle(new IllegalArgumentException(), null, null);
        Assertions.assertEquals(true, result.toString().contains("handle"));
    }

    /**
     * Test handle IllegalStateException
     */
    @Test
    public void testIllegalStateException() throws Throwable {
        MyAnnotationExceptionHandlerResolver resolver = new MyAnnotationExceptionHandlerResolver();
        resolver.setAllowOverrideExceptionHandler(true);
        resolver.detectExceptionHandlers(new MyHandlerBean());
        ExceptionHandler handler = resolver.resolveExceptionHandler(new IllegalStateException(), null, null);
        Object result = handler.handle(new IllegalStateException(), null, null);
        Assertions.assertEquals("handle3", result);
    }

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public static @interface MyHandler {

        Class<? extends Throwable>[] value() default {};
    }

    public static class MyAnnotationExceptionHandlerResolver extends
            AbstractAnnotationExceptionHandlerResolver<MyHandler> {

        @Override
        protected Set<Class<? extends Throwable>> doDetectExceptionMappings(Method method, MyHandler annotation) {
            if (ArrayUtils.isEmpty(annotation.value())) {
                return Stream.of(NullPointerException.class, IllegalArgumentException.class)
                        .collect(Collectors.toSet());
            }
            return Stream.of(annotation.value()).collect(Collectors.toSet());
        }
    }

    public static class MyHandlerBean {

        @MyHandler
        public String handle1() {
            return "handle1";
        }

        @MyHandler
        public String handle2() {
            return "handle2";
        }

        @MyHandler(IllegalStateException.class)
        public String handle3() {
            return "handle3";
        }
    }

}
